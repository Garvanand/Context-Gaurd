"""
Unit and Integration Tests for ContextGuard Real-Time Device Relay.
Validates:
- Pairing state machine, single-use codes, approval/denial, revocation
- Device heartbeat & live presence
- Privacy-safe event ingestion & idempotent deduplication
- Zero-leakage privacy invariants (raw screenshots & credentials rejection)
- Bidirectional command channel lifecycle & acknowledgements
- Real-time WebSocket authentication & message passing
"""

import pytest
from fastapi.testclient import TestClient
from backend.app.main import app
from backend.app.api.v1.relay import relay_db

client = TestClient(app)


@pytest.fixture(autouse=True)
def clean_relay_db():
    """Ensure clean database before each test run."""
    with relay_db._lock, relay_db._get_connection() as conn:
        conn.execute("DELETE FROM audit_logs")
        conn.execute("DELETE FROM commands")
        conn.execute("DELETE FROM events")
        conn.execute("DELETE FROM pairing_sessions")
        conn.execute("DELETE FROM devices")
        conn.commit()


# =============================================================================
# 1. PAIRING TESTS
# =============================================================================

def test_successful_pairing_lifecycle():
    """Tests the full mutual approval pairing flow."""
    # Step 1: Phone starts pairing
    res = client.post("/api/v1/relay/pairing/start", json={
        "device_name": "Test Pixel 8",
        "platform": "Android",
        "model": "Pixel 8 Pro",
        "app_version": "1.0.0"
    })
    assert res.status_code == 200
    data = res.json()
    session_id = data["session_id"]
    code = data["pairing_code"]
    assert len(code) == 6

    # Step 2: Dashboard claims pairing session
    claim_res = client.post("/api/v1/relay/pairing/claim", json={
        "pairing_code": code,
        "supervisor_label": "Examiner Workstation"
    })
    assert claim_res.status_code == 200
    claim_data = claim_res.json()
    assert claim_data["status"] == "CLAIMED"
    assert claim_data["device_name"] == "Test Pixel 8"

    # Step 3: Phone approves pairing
    approve_res = client.post("/api/v1/relay/pairing/approve", json={
        "session_id": session_id,
        "approved": True
    })
    assert approve_res.status_code == 200
    approve_data = approve_res.json()
    assert approve_data["status"] == "APPROVED"
    assert approve_data["device_token"].startswith("devtok_")
    assert approve_data["dashboard_token"].startswith("dshtok_")

    # Step 4: Verify device is now registered and ONLINE
    status_res = client.get("/api/v1/relay/device/primary")
    assert status_res.status_code == 200
    primary = status_res.json()
    assert primary["status"] == "ONLINE"
    assert primary["device"]["device_name"] == "Test Pixel 8"


def test_pairing_wrong_code_and_replay_protection():
    """Tests rejection of wrong pairing codes and replay attempts."""
    # Start valid pairing
    res = client.post("/api/v1/relay/pairing/start", json={
        "device_name": "Phone", "platform": "Android", "model": "Pixel", "app_version": "1.0"
    })
    valid_code = res.json()["pairing_code"]
    session_id = res.json()["session_id"]

    # Claim with wrong code
    bad_res = client.post("/api/v1/relay/pairing/claim", json={
        "pairing_code": "WRONG9", "supervisor_label": "Dashboard"
    })
    assert bad_res.status_code == 400

    # Claim with correct code
    good_res = client.post("/api/v1/relay/pairing/claim", json={
        "pairing_code": valid_code, "supervisor_label": "Dashboard"
    })
    assert good_res.status_code == 200

    # Replay attempt with same code must fail
    replay_res = client.post("/api/v1/relay/pairing/claim", json={
        "pairing_code": valid_code, "supervisor_label": "Hacker"
    })
    assert replay_res.status_code == 400


def test_pairing_denial_and_revocation():
    """Tests phone user denying connection and supervisor revoking device."""
    # Start and claim
    res = client.post("/api/v1/relay/pairing/start", json={"device_name": "Phone", "platform": "Android", "model": "Pixel", "app_version": "1.0"})
    code = res.json()["pairing_code"]
    session_id = res.json()["session_id"]
    client.post("/api/v1/relay/pairing/claim", json={"pairing_code": code, "supervisor_label": "Dashboard"})

    # Deny
    deny_res = client.post("/api/v1/relay/pairing/deny", json={"session_id": session_id, "approved": False})
    assert deny_res.status_code == 200
    assert deny_res.json()["status"] == "DENIED"

    # Start fresh pairing and approve
    res2 = client.post("/api/v1/relay/pairing/start", json={"device_name": "Phone", "platform": "Android", "model": "Pixel", "app_version": "1.0"})
    session2 = res2.json()["session_id"]
    client.post("/api/v1/relay/pairing/claim", json={"pairing_code": res2.json()["pairing_code"], "supervisor_label": "Dashboard"})
    app_res = client.post("/api/v1/relay/pairing/approve", json={"session_id": session2, "approved": True})
    dash_token = app_res.json()["dashboard_token"]

    # Retrieve device_id
    primary = client.get("/api/v1/relay/device/primary").json()
    dev_id = primary["device"]["device_id"]

    # Revoke pairing
    revoke_res = client.post(
        "/api/v1/relay/pairing/revoke",
        headers={"Authorization": f"Bearer {dash_token}"},
        json={"device_id": dev_id}
    )
    assert revoke_res.status_code == 200

    # Check device is now UNPAIRED
    after = client.get("/api/v1/relay/device/primary").json()
    assert after["status"] == "UNPAIRED"


# =============================================================================
# 2. HEARTBEAT & LIVE PRESENCE TESTS
# =============================================================================

def test_heartbeat_and_presence_tracking():
    """Tests device heartbeat recording and presence state."""
    # Setup paired device
    res = client.post("/api/v1/relay/pairing/start", json={"device_name": "Pixel", "platform": "Android", "model": "Pixel", "app_version": "1.0"})
    session_id = res.json()["session_id"]
    client.post("/api/v1/relay/pairing/claim", json={"pairing_code": res.json()["pairing_code"], "supervisor_label": "Dashboard"})
    app_res = client.post("/api/v1/relay/pairing/approve", json={"session_id": session_id, "approved": True})
    dev_token = app_res.json()["device_token"]
    primary = client.get("/api/v1/relay/device/primary").json()
    dev_id = primary["device"]["device_id"]

    # Send heartbeat
    hb_res = client.post(
        "/api/v1/relay/device/heartbeat",
        headers={"Authorization": f"Bearer {dev_token}"},
        json={
            "device_id": dev_id,
            "monitoring_active": True,
            "notification_active": True,
            "selected_apps_count": 5,
            "local_model_healthy": True,
            "network_mode": "LOCAL_BACKEND",
            "telemetry_enabled": True
        }
    )
    assert hb_res.status_code == 200
    assert hb_res.json()["device_status"] == "ONLINE"

    # Verify supervisor overview endpoint reflects this honest live status
    ov_res = client.get("/api/v1/supervisor/overview")
    assert ov_res.status_code == 200
    conn = ov_res.json()["android_connection"]
    assert conn["status"] == "ONLINE"
    assert conn["selected_apps_count"] == 5
    assert conn["monitoring_active"] is True


# =============================================================================
# 3. EVENT INGESTION & PRIVACY INVARIANT TESTS
# =============================================================================

def test_privacy_safe_event_ingestion_and_idempotency():
    """Tests ingestion of valid privacy-safe events and idempotent deduplication."""
    # Pair device
    res = client.post("/api/v1/relay/pairing/start", json={"device_name": "Pixel", "platform": "Android", "model": "Pixel", "app_version": "1.0"})
    session_id = res.json()["session_id"]
    client.post("/api/v1/relay/pairing/claim", json={"pairing_code": res.json()["pairing_code"], "supervisor_label": "Dashboard"})
    app_res = client.post("/api/v1/relay/pairing/approve", json={"session_id": session_id, "approved": True})
    dev_token = app_res.json()["device_token"]
    dash_token = app_res.json()["dashboard_token"]
    primary = client.get("/api/v1/relay/device/primary").json()
    dev_id = primary["device"]["device_id"]

    event_payload = {
        "event_id": "evt-test-101",
        "device_id": dev_id,
        "event_type": "PRE_ACTION_INTERVENTION",
        "source_app": "com.whatsapp",
        "risk_category": "CREDENTIAL_HARVESTING",
        "intervention": "STOP",
        "risk_score": 0.85,
        "severity": 0.90,
        "reversibility": 1.0,
        "confidence": 0.95,
        "evidence_summary": [
            "Detected credential harvesting domain 'verify-bank-security.net'",
            "Masked 2 account tokens [ACCOUNT_REDACTED]"
        ],
        "model_version": "1.0.0",
        "latency_ms": 112,
        "redaction_count": 2,
        "correlation_id": "sha256_mock_hash",
        "network_mode": "LOCAL_BACKEND"
    }

    # Ingest event
    ingest_res = client.post(
        "/api/v1/relay/events/ingest",
        headers={"Authorization": f"Bearer {dev_token}"},
        json=event_payload
    )
    assert ingest_res.status_code == 200
    assert ingest_res.json()["status"] == "INSERTED"

    # Retry same event ID (idempotency check)
    retry_res = client.post(
        "/api/v1/relay/events/ingest",
        headers={"Authorization": f"Bearer {dev_token}"},
        json=event_payload
    )
    assert retry_res.status_code == 200
    assert retry_res.json()["status"] == "DUPLICATE_IGNORED"

    # Verify event is retrievable by dashboard
    get_res = client.get(
        f"/api/v1/relay/events/{dev_id}",
        headers={"Authorization": f"Bearer {dash_token}"}
    )
    assert get_res.status_code == 200
    events = get_res.json()
    assert len(events) == 1
    assert events[0]["event_id"] == "evt-test-101"
    assert events[0]["intervention"] == "STOP"


def test_privacy_invariant_rejection():
    """Validates that payloads containing raw screenshots or credentials are strictly rejected."""
    # Pair device
    res = client.post("/api/v1/relay/pairing/start", json={"device_name": "Pixel", "platform": "Android", "model": "Pixel", "app_version": "1.0"})
    session_id = res.json()["session_id"]
    client.post("/api/v1/relay/pairing/claim", json={"pairing_code": res.json()["pairing_code"], "supervisor_label": "Dashboard"})
    app_res = client.post("/api/v1/relay/pairing/approve", json={"session_id": session_id, "approved": True})
    dev_token = app_res.json()["device_token"]
    primary = client.get("/api/v1/relay/device/primary").json()
    dev_id = primary["device"]["device_id"]

    # 1. Reject raw base64 artifact
    bad_payload_1 = {
        "event_id": "evt-leak-1",
        "device_id": dev_id,
        "event_type": "PRE_ACTION_INTERVENTION",
        "risk_category": "PII",
        "intervention": "STOP",
        "screenshot": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
    }
    res_1 = client.post("/api/v1/relay/events/ingest", headers={"Authorization": f"Bearer {dev_token}"}, json=bad_payload_1)
    assert res_1.status_code == 400
    assert "Privacy Invariant Breached" in res_1.json()["detail"]

    # 2. Reject unmasked 16-digit credit card in evidence
    bad_payload_2 = {
        "event_id": "evt-leak-2",
        "device_id": dev_id,
        "event_type": "PRE_ACTION_INTERVENTION",
        "risk_category": "FINANCIAL",
        "intervention": "STOP",
        "evidence_summary": ["User entered card 4111 2222 3333 4444 on form"]
    }
    res_2 = client.post("/api/v1/relay/events/ingest", headers={"Authorization": f"Bearer {dev_token}"}, json=bad_payload_2)
    assert res_2.status_code == 400
    assert "Payment card number" in res_2.json()["detail"] or "Privacy Invariant" in res_2.json()["detail"]


# =============================================================================
# 4. COMMAND CHANNEL & ACK LIFECYCLE TESTS
# =============================================================================

def test_command_channel_lifecycle():
    """Tests enqueuing, polling, executing, and acknowledging commands."""
    # Pair device
    res = client.post("/api/v1/relay/pairing/start", json={"device_name": "Pixel", "platform": "Android", "model": "Pixel", "app_version": "1.0"})
    session_id = res.json()["session_id"]
    client.post("/api/v1/relay/pairing/claim", json={"pairing_code": res.json()["pairing_code"], "supervisor_label": "Dashboard"})
    app_res = client.post("/api/v1/relay/pairing/approve", json={"session_id": session_id, "approved": True})
    dev_token = app_res.json()["device_token"]
    dash_token = app_res.json()["dashboard_token"]
    primary = client.get("/api/v1/relay/device/primary").json()
    dev_id = primary["device"]["device_id"]

    # Supervisor enqueues REQUEST_STATUS
    cmd_res = client.post(
        "/api/v1/relay/commands/queue",
        headers={"Authorization": f"Bearer {dash_token}"},
        json={
            "device_id": dev_id,
            "command_type": "REQUEST_STATUS",
            "payload": {"include_battery": True}
        }
    )
    assert cmd_res.status_code == 200
    cmd_data = cmd_res.json()
    cmd_id = cmd_data["command_id"]
    assert cmd_data["status"] == "QUEUED"

    # Device polls pending commands
    pending_res = client.get(
        f"/api/v1/relay/commands/pending/{dev_id}",
        headers={"Authorization": f"Bearer {dev_token}"}
    )
    assert pending_res.status_code == 200
    pending_list = pending_res.json()
    assert len(pending_list) == 1
    assert pending_list[0]["command_id"] == cmd_id

    # Device acknowledges successful execution
    ack_res = client.post(
        "/api/v1/relay/commands/ack",
        headers={"Authorization": f"Bearer {dev_token}"},
        json={
            "command_id": cmd_id,
            "device_id": dev_id,
            "status": "SUCCEEDED",
            "result": {"battery": 0.88, "service": "ACTIVE", "memory_mb": 142}
        }
    )
    assert ack_res.status_code == 200

    # Supervisor verifies updated command status
    recent_res = client.get(
        f"/api/v1/relay/commands/recent/{dev_id}",
        headers={"Authorization": f"Bearer {dash_token}"}
    )
    assert recent_res.status_code == 200
    recent_cmds = recent_res.json()
    assert recent_cmds[0]["status"] == "SUCCEEDED"
    assert recent_cmds[0]["result"]["battery"] == 0.88


# =============================================================================
# 5. WEBSOCKET AUTHENTICATION & MESSAGING TESTS
# =============================================================================

def test_websocket_device_authentication():
    """Tests WebSocket device connection authentication handshake."""
    # Pair device
    res = client.post("/api/v1/relay/pairing/start", json={"device_name": "Pixel", "platform": "Android", "model": "Pixel", "app_version": "1.0"})
    session_id = res.json()["session_id"]
    client.post("/api/v1/relay/pairing/claim", json={"pairing_code": res.json()["pairing_code"], "supervisor_label": "Dashboard"})
    app_res = client.post("/api/v1/relay/pairing/approve", json={"session_id": session_id, "approved": True})
    dev_token = app_res.json()["device_token"]
    primary = client.get("/api/v1/relay/device/primary").json()
    dev_id = primary["device"]["device_id"]

    # Connect over WebSocket with valid auth
    with client.websocket_connect("/api/v1/relay/ws/device") as websocket:
        websocket.send_text(f'{{"token": "{dev_token}", "device_id": "{dev_id}"}}')
        auth_ack = websocket.receive_json()
        assert auth_ack["type"] == "AUTH_SUCCESS"
        assert auth_ack["device_id"] == dev_id

        # Send PING
        websocket.send_text('{"type": "PING"}')
        pong = websocket.receive_json()
        assert pong["type"] == "PONG"

        # Clean close
        websocket.send_text('{"type": "CLOSE"}')
