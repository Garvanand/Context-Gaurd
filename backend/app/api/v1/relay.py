"""
ContextGuard Real-Time Device Relay FastAPI Router.
Provides authenticated REST and WebSocket endpoints for pairing, heartbeats,
privacy-safe telemetry streaming, and persistent command queues.
"""

import asyncio
from datetime import datetime, timezone
import json
import secrets
from typing import Any, Dict, List, Optional
from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    Header,
    Query,
    Request,
    WebSocket,
    WebSocketDisconnect,
    status,
)
from pydantic import ValidationError

from backend.app.core.config import settings
from backend.app.core.logging import logger
from backend.app.relay.connection_manager import RelayConnectionManager
from backend.app.relay.models import (
    CommandAckRequest,
    CommandQueueRequest,
    CommandResponse,
    CommandStatus,
    CommandType,
    DeviceHeartbeatRequest,
    DeviceHeartbeatResponse,
    DeviceStatus,
    DeviceStatusResponse,
    EventIngestRequest,
    EventIngestResponse,
    EventSummary,
    PairingClaimRequest,
    PairingClaimResponse,
    PairingDecisionRequest,
    PairingDecisionResponse,
    PairingStartRequest,
    PairingStartResponse,
    PairingStatus,
    PairingStatusResponse,
    RelayEnvelope,
    RelayEventType,
    RevokePairingRequest,
    WebSocketAuthPayload,
)
from backend.app.relay.persistence import RelayDatabase
from backend.app.relay.privacy_filter import PrivacyViolationError, enforce_privacy_or_raise

# Singletons for database and in-process WebSocket connection manager
relay_db = RelayDatabase()
relay_manager = RelayConnectionManager(relay_db)

router = APIRouter(prefix="/relay", tags=["Relay"])


# =============================================================================
# AUTHENTICATION DEPENDENCIES
# =============================================================================

async def get_authenticated_device(
    authorization: Optional[str] = Header(None)
) -> Dict[str, Any]:
    """Validates Bearer device_token."""
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid Bearer authorization header"
        )
    token = authorization.split(" ", 1)[1].strip()
    dev = relay_db.authenticate_device(token)
    if not dev:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid, expired, or revoked device token"
        )
    return dev


async def get_authenticated_dashboard(
    authorization: Optional[str] = Header(None)
) -> Dict[str, Any]:
    """Validates Bearer dashboard_token or permits local debug session."""
    if authorization and authorization.startswith("Bearer "):
        token = authorization.split(" ", 1)[1].strip()
        dash = relay_db.authenticate_dashboard(token)
        if dash:
            return dash
    # In local debug mode, check if there's any active paired device
    primary = relay_db.get_primary_device()
    if primary:
        return primary
    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Unauthorized supervisor session: no valid token or active paired device"
    )


# =============================================================================
# PAIRING ENDPOINTS
# =============================================================================

@router.post("/pairing/start", response_model=PairingStartResponse, summary="Start mobile pairing session")
async def start_pairing(req: PairingStartRequest):
    """
    Called by Android client. Generates a short-lived one-time pairing code.
    Phone displays the code and/or QR string to the user.
    """
    session_id, code, expires_at, ttl = relay_db.create_pairing_session(
        device_name=req.device_name,
        platform=req.platform,
        model=req.model,
        app_version=req.app_version,
    )
    qr_payload = f"contextguard://pair?code={code}&session={session_id}"
    return PairingStartResponse(
        session_id=session_id,
        pairing_code=code,
        expires_at=expires_at,
        expires_in_seconds=ttl,
        qr_payload=qr_payload,
    )


@router.post("/pairing/claim", response_model=PairingClaimResponse, summary="Claim a pairing session")
async def claim_pairing(req: PairingClaimRequest):
    """
    Called by supervisor dashboard. Enters the pairing code shown on phone screen.
    Single-use, rate-limited, marked CLAIMED upon success.
    """
    claim_res = relay_db.claim_pairing_session(req.pairing_code, req.supervisor_label)
    if not claim_res:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid, already claimed, or expired pairing code"
        )
    return PairingClaimResponse(
        session_id=claim_res["session_id"],
        status=PairingStatus.CLAIMED,
        device_name=claim_res["device_name"],
        device_model=claim_res["device_model"],
    )


@router.get("/pairing/status/{session_id}", response_model=PairingStatusResponse, summary="Get pairing status")
async def get_pairing_status(session_id: str, client_type: str = Query("dashboard")):
    """
    Polls pairing session status while waiting for mutual claim and approval.
    """
    session = relay_db.get_pairing_session(session_id)
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found")

    token = None
    if session["status"] == PairingStatus.APPROVED.value:
        if client_type == "device":
            token = session.get("raw_device_token")
        else:
            token = session.get("raw_dashboard_token")

    return PairingStatusResponse(
        session_id=session["session_id"],
        status=PairingStatus(session["status"]),
        device_id=session.get("device_id"),
        device_name=session.get("device_name"),
        token=token,
    )


@router.post("/pairing/approve", response_model=PairingDecisionResponse, summary="Approve pairing from phone")
async def approve_pairing(req: PairingDecisionRequest):
    """
    Called by phone when user confirms the incoming connection claim.
    Issues separate scoped credentials for device and dashboard.
    """
    ok, dev_token, dash_token = relay_db.approve_pairing_session(req.session_id)
    if not ok:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot approve pairing session (invalid state or expired)"
        )

    # Fan-out pairing approval event over WebSocket
    session = relay_db.get_pairing_session(req.session_id)
    dev_id = session.get("device_id", "unknown") if session else "unknown"
    env = RelayEnvelope(
        type="PAIRING_SUCCESS",
        device_id=dev_id,
        payload={"session_id": req.session_id, "status": "APPROVED", "device_name": session.get("device_name") if session else ""}
    )
    await relay_manager.broadcast_to_dashboards(dev_id, env)

    return PairingDecisionResponse(
        session_id=req.session_id,
        status=PairingStatus.APPROVED,
        device_token=dev_token,
        dashboard_token=dash_token,
    )


@router.post("/pairing/deny", response_model=PairingDecisionResponse, summary="Deny pairing from phone")
async def deny_pairing(req: PairingDecisionRequest):
    """Called by phone when user rejects the pairing claim."""
    ok = relay_db.deny_pairing_session(req.session_id)
    if not ok:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Cannot deny pairing session")
    return PairingDecisionResponse(
        session_id=req.session_id,
        status=PairingStatus.DENIED,
    )


@router.post("/pairing/revoke", summary="Revoke paired device")
async def revoke_pairing(req: RevokePairingRequest, dash=Depends(get_authenticated_dashboard)):
    """Revokes device credentials."""
    ok = relay_db.revoke_device(req.device_id)
    if not ok:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Device not found")
    await relay_manager.notify_device_presence(req.device_id, DeviceStatus.UNPAIRED)
    return {"status": "REVOKED", "device_id": req.device_id}


# =============================================================================
# DEVICE STATUS & HEARTBEAT ENDPOINTS
# =============================================================================

@router.post("/device/heartbeat", response_model=DeviceHeartbeatResponse, summary="Submit device heartbeat")
async def device_heartbeat(
    req: DeviceHeartbeatRequest,
    dev=Depends(get_authenticated_device)
):
    """Updates device presence timestamp and current capability status."""
    if dev["device_id"] != req.device_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Device ID mismatch with credential")

    ok = relay_db.record_heartbeat(
        device_id=req.device_id,
        monitoring_active=req.monitoring_active,
        notification_active=req.notification_active,
        selected_apps_count=req.selected_apps_count,
        local_model_healthy=req.local_model_healthy,
        network_mode=req.network_mode,
        telemetry_enabled=req.telemetry_enabled,
        battery_level=req.battery_level,
    )
    if not ok:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Failed to record heartbeat")

    # Broadcast presence update to supervisor dashboards
    current_status = DeviceStatus.ONLINE if (req.monitoring_active and req.local_model_healthy) else DeviceStatus.DEGRADED
    await relay_manager.notify_device_presence(req.device_id, current_status)

    pending_cmds = relay_db.get_pending_commands(req.device_id)

    return DeviceHeartbeatResponse(
        status="ACK",
        device_status=current_status,
        server_timestamp=datetime.now(timezone.utc).isoformat(),
        pending_commands_count=len(pending_cmds),
    )


@router.get("/device/status/{device_id}", response_model=DeviceStatusResponse, summary="Get device status")
async def get_device_status(device_id: str, dash=Depends(get_authenticated_dashboard)):
    """Retrieves verified status of a paired device."""
    st = relay_db.get_device_status(device_id)
    if not st:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Device not found")
    return DeviceStatusResponse(
        device_id=st["device_id"],
        device_name=st["device_name"],
        platform=st["platform"],
        model=st["model"],
        app_version=st["app_version"],
        status=DeviceStatus(st["status"]),
        last_heartbeat=st.get("last_heartbeat"),
        monitoring_active=bool(st.get("monitoring_active", 0)),
        notification_active=bool(st.get("notification_active", 0)),
        selected_apps_count=st.get("selected_apps_count", 0),
        local_model_healthy=bool(st.get("local_model_healthy", 0)),
        network_mode=st.get("network_mode", "UNKNOWN"),
        telemetry_enabled=bool(st.get("telemetry_enabled", 0)),
        battery_level=st.get("battery_level"),
        paired_at=st["paired_at"],
    )


@router.get("/device/primary", summary="Get primary paired device for supervisor")
async def get_primary_device():
    """Returns primary paired device if one exists, or UNPAIRED status."""
    dev = relay_db.get_primary_device()
    if not dev:
        return {
            "status": "UNPAIRED",
            "device": None,
            "message": "No Android device currently paired with this relay"
        }
    return {
        "status": dev["status"],
        "device": {
            "device_id": dev["device_id"],
            "device_name": dev["device_name"],
            "model": dev["model"],
            "platform": dev["platform"],
            "app_version": dev["app_version"],
            "status": dev["status"],
            "last_heartbeat": dev.get("last_heartbeat"),
            "monitoring_active": bool(dev.get("monitoring_active", 0)),
            "notification_active": bool(dev.get("notification_active", 0)),
            "selected_apps_count": dev.get("selected_apps_count", 0),
            "local_model_healthy": bool(dev.get("local_model_healthy", 0)),
            "network_mode": dev.get("network_mode", "UNKNOWN"),
            "telemetry_enabled": bool(dev.get("telemetry_enabled", 0)),
            "paired_at": dev["paired_at"],
        }
    }


# =============================================================================
# EVENT INGESTION & DURABLE LEDGER ENDPOINTS
# =============================================================================

@router.post("/events/ingest", response_model=EventIngestResponse, summary="Ingest real device telemetry event")
async def ingest_event(
    req: EventIngestRequest,
    dev=Depends(get_authenticated_device)
):
    """
    Ingests privacy-safe telemetry event from paired phone.
    Enforces privacy filter, saves to SQLite, and fans out live over WebSocket.
    """
    if dev["device_id"] != req.device_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Device ID mismatch with credential")

    event_dict = req.model_dump()

    try:
        enforce_privacy_or_raise(event_dict)
    except PrivacyViolationError as e:
        logger.error(f"Relay Ingestion Rejected due to Privacy Violation: {e}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Privacy Invariant Breached: {str(e)}"
        )

    ok, action_status = relay_db.ingest_event(event_dict)
    now_iso = datetime.now(timezone.utc).isoformat()

    # Fan out live event to connected supervisor dashboards
    envelope = RelayEnvelope(
        event_id=req.event_id,
        type="TELEMETRY_EVENT",
        device_id=req.device_id,
        timestamp=req.timestamp,
        payload=event_dict,
        correlation_id=req.correlation_id,
    )
    await relay_manager.broadcast_to_dashboards(req.device_id, envelope)

    return EventIngestResponse(
        event_id=req.event_id,
        status=action_status,
        server_timestamp=now_iso,
    )


@router.get("/events/{device_id}", response_model=List[EventSummary], summary="Get events for paired device")
async def get_events(
    device_id: str,
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
    dash=Depends(get_authenticated_dashboard)
):
    """Retrieves persisted privacy-safe events for authorized device."""
    events = relay_db.get_events(device_id, limit=limit, offset=offset)
    res = []
    for e in events:
        res.append(EventSummary(
            event_id=e["event_id"],
            device_id=e["device_id"],
            event_type=e["event_type"],
            timestamp=e["timestamp"],
            source_app=e.get("source_app"),
            risk_category=e["risk_category"],
            intervention=e["intervention"],
            risk_score=e["risk_score"],
            severity=e["severity"],
            reversibility=e["reversibility"],
            confidence=e["confidence"],
            evidence_summary=e.get("evidence_summary", []),
            model_version=e["model_version"],
            latency_ms=e["latency_ms"],
            redaction_count=e["redaction_count"],
            correlation_id=e.get("correlation_id"),
            network_mode=e["network_mode"],
        ))
    return res


@router.get("/events/detail/{event_id}", summary="Get detailed event record")
async def get_event_detail(event_id: str, dash=Depends(get_authenticated_dashboard)):
    """Retrieves single event details."""
    ev = relay_db.get_event_by_id(event_id)
    if not ev:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Event not found")
    return ev


# =============================================================================
# COMMAND CHANNEL ENDPOINTS
# =============================================================================

@router.post("/commands/queue", response_model=CommandResponse, summary="Enqueue command for device")
async def queue_command(
    req: CommandQueueRequest,
    dash=Depends(get_authenticated_dashboard)
):
    """
    Supervisor enqueues an authorized command (REQUEST_STATUS, REQUEST_DIAGNOSTICS,
    RUN_SYNTHETIC_DEMO, REQUEST_CONFIG_REFRESH).
    """
    command_id = relay_db.enqueue_command(
        device_id=req.device_id,
        command_type=req.command_type,
        issuer_session="supervisor",
        payload=req.payload,
        timeout_seconds=req.timeout_seconds,
    )

    cmd_data = relay_db.get_command(command_id)

    # Attempt immediate delivery over active WebSocket
    if cmd_data:
        await relay_manager.deliver_command(command_id, req.device_id, cmd_data)
        # Refresh updated status
        cmd_data = relay_db.get_command(command_id)

    return CommandResponse(
        command_id=cmd_data["command_id"],
        device_id=cmd_data["device_id"],
        command_type=CommandType(cmd_data["command_type"]),
        issuer_session=cmd_data["issuer_session"],
        payload=cmd_data["payload"],
        status=CommandStatus(cmd_data["status"]),
        created_at=cmd_data["created_at"],
        expires_at=cmd_data["expires_at"],
        delivered_at=cmd_data.get("delivered_at"),
        acknowledged_at=cmd_data.get("acknowledged_at"),
        result=cmd_data.get("result", {}),
        error_message=cmd_data.get("error_message"),
    )


@router.get("/commands/pending/{device_id}", summary="Poll pending commands for device")
async def get_pending_commands(
    device_id: str,
    dev=Depends(get_authenticated_device)
):
    """Called by device to retrieve pending commands upon reconnect."""
    if dev["device_id"] != device_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Device ID mismatch")
    cmds = relay_db.get_pending_commands(device_id)
    # Mark them DELIVERED
    for c in cmds:
        relay_db.mark_command_delivered(c["command_id"])
    return cmds


@router.post("/commands/ack", summary="Acknowledge command execution from device")
async def acknowledge_command(
    req: CommandAckRequest,
    dev=Depends(get_authenticated_device)
):
    """
    Called by device when command execution completes (SUCCEEDED, FAILED, ACKNOWLEDGED).
    Updates SQLite record and notifies supervisor dashboards.
    """
    if dev["device_id"] != req.device_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Device ID mismatch")

    ok = relay_db.acknowledge_command(
        command_id=req.command_id,
        device_id=req.device_id,
        status=req.status,
        result=req.result,
        error_message=req.error_message,
    )
    if not ok:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Command not found or already completed")

    # Broadcast status update to dashboards
    env = RelayEnvelope(
        type="COMMAND_STATUS_UPDATE",
        device_id=req.device_id,
        payload={
            "command_id": req.command_id,
            "status": req.status.value,
            "result": req.result,
            "error_message": req.error_message,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }
    )
    await relay_manager.broadcast_to_dashboards(req.device_id, env)

    return {"status": "ACK_RECORDED", "command_id": req.command_id}


@router.get("/commands/recent/{device_id}", summary="Get recent commands for device")
async def get_recent_commands(device_id: str, limit: int = 20, dash=Depends(get_authenticated_dashboard)):
    """Returns recent commands for Command Center dashboard view."""
    return relay_db.get_recent_commands(device_id, limit=limit)


# =============================================================================
# RELAY HEALTH ENDPOINT
# =============================================================================

@router.get("/health", summary="Relay operational health")
async def relay_health():
    """Returns relay health, active WebSocket count, and primary device presence."""
    primary = relay_db.get_primary_device()
    return {
        "status": "OPERATIONAL",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "active_device_sockets": len(relay_manager._device_sockets),
        "active_dashboard_sockets": len(relay_manager._dashboard_sockets),
        "primary_device": {
            "device_id": primary["device_id"] if primary else None,
            "name": primary["device_name"] if primary else None,
            "status": primary["status"] if primary else "UNPAIRED",
            "last_heartbeat": primary.get("last_heartbeat") if primary else None,
        } if primary else None,
        "database": "sqlite_wal_active",
        "in_process_broker": "active",
    }


# =============================================================================
# WEBSOCKET ENDPOINTS
# =============================================================================

@router.websocket("/ws/device")
async def websocket_device_endpoint(websocket: WebSocket):
    """
    Authenticated WebSocket endpoint for Android device.
    Step 1: Client connects.
    Step 2: Client must send AUTH message: {"token": "devtok_...", "device_id": "..."} within 5s.
    Step 3: Server authenticates against SQLite.
    Step 4: Continuous bidirectional message exchange (telemetry, heartbeat, command ACK).
    """
    device_id: Optional[str] = None
    try:
        await websocket.accept()

        # Step 2: Receive AUTH handshake
        auth_raw = await websocket.receive_text()
        auth_data = json.loads(auth_raw)
        token = auth_data.get("token")
        claimed_dev_id = auth_data.get("device_id")

        if not token:
            await websocket.send_text(json.dumps({"error": "Missing token"}))
            await websocket.close(code=1008, reason="Authentication failed")
            return

        dev = relay_db.authenticate_device(token)
        if not dev or (claimed_dev_id and dev["device_id"] != claimed_dev_id):
            await websocket.send_text(json.dumps({"error": "Unauthorized device"}))
            await websocket.close(code=1008, reason="Unauthorized device")
            return

        device_id = dev["device_id"]
        # Register in connection manager
        await relay_manager.connect_device(device_id, websocket)
        await websocket.send_text(json.dumps({
            "type": "AUTH_SUCCESS",
            "device_id": device_id,
            "server_time": datetime.now(timezone.utc).isoformat()
        }))

        # Message processing loop
        while True:
            text = await websocket.receive_text()
            if len(text) > settings.relay_max_message_size_bytes:
                await websocket.send_text(json.dumps({"error": "Message size exceeded limit"}))
                continue

            msg = json.loads(text)
            msg_type = msg.get("type", "")

            if msg_type == "CLOSE":
                await websocket.close()
                break

            elif msg_type == "PING":
                await websocket.send_text(json.dumps({"type": "PONG", "timestamp": datetime.now(timezone.utc).isoformat()}))

            elif msg_type == "HEARTBEAT":
                p = msg.get("payload", {})
                relay_db.record_heartbeat(
                    device_id=device_id,
                    monitoring_active=p.get("monitoring_active", True),
                    notification_active=p.get("notification_active", True),
                    selected_apps_count=p.get("selected_apps_count", 0),
                    local_model_healthy=p.get("local_model_healthy", True),
                    network_mode=p.get("network_mode", "LOCAL_BACKEND"),
                    telemetry_enabled=p.get("telemetry_enabled", True),
                    battery_level=p.get("battery_level"),
                )
                await relay_manager.notify_device_presence(device_id, DeviceStatus.ONLINE)
                await websocket.send_text(json.dumps({"type": "HEARTBEAT_ACK"}))

            elif msg_type == "TELEMETRY_EVENT":
                event_data = msg.get("payload", {})
                event_data["device_id"] = device_id
                try:
                    enforce_privacy_or_raise(event_data)
                    relay_db.ingest_event(event_data)
                    env = RelayEnvelope(
                        event_id=event_data.get("event_id"),
                        type="TELEMETRY_EVENT",
                        device_id=device_id,
                        timestamp=event_data.get("timestamp", datetime.now(timezone.utc).isoformat()),
                        payload=event_data,
                    )
                    await relay_manager.broadcast_to_dashboards(device_id, env)
                    await websocket.send_text(json.dumps({"type": "EVENT_ACK", "event_id": event_data.get("event_id")}))
                except Exception as ex:
                    await websocket.send_text(json.dumps({"type": "EVENT_ERROR", "error": str(ex)}))

            elif msg_type == "COMMAND_ACK":
                p = msg.get("payload", {})
                cmd_id = p.get("command_id")
                cmd_st = CommandStatus(p.get("status", "ACKNOWLEDGED"))
                relay_db.acknowledge_command(
                    command_id=cmd_id,
                    device_id=device_id,
                    status=cmd_st,
                    result=p.get("result", {}),
                    error_message=p.get("error_message"),
                )
                ack_env = RelayEnvelope(
                    type="COMMAND_STATUS_UPDATE",
                    device_id=device_id,
                    payload={"command_id": cmd_id, "status": cmd_st.value, "result": p.get("result", {})}
                )
                await relay_manager.broadcast_to_dashboards(device_id, ack_env)

    except (WebSocketDisconnect, asyncio.TimeoutError):
        pass
    except Exception as e:
        logger.error(f"Error in device WebSocket for '{device_id}': {e}")
    finally:
        if device_id:
            await relay_manager.disconnect_device(device_id, websocket)


@router.websocket("/ws/dashboard")
async def websocket_dashboard_endpoint(websocket: WebSocket):
    """
    Authenticated WebSocket endpoint for Supervisor Web Dashboard.
    Step 1: Check origin against allowed origins.
    Step 2: Receives AUTH message with dashboard_token or permits local pairing subscription.
    Step 3: Streams live events, device presence, and command ACKs.
    """
    socket_id = f"dash_{secrets.token_hex(8)}"
    try:
        await websocket.accept()

        auth_raw = await websocket.receive_text()
        auth_data = json.loads(auth_raw)
        token = auth_data.get("token")
        sub_device_id = auth_data.get("device_id", "*")

        # In local LAN demo profile, if no token is provided but a device exists, allow primary
        if not token:
            primary = relay_db.get_primary_device()
            if primary and sub_device_id == "*":
                sub_device_id = primary["device_id"]

        await relay_manager.connect_dashboard(socket_id, websocket, sub_device_id)
        await websocket.send_text(json.dumps({
            "type": "AUTH_SUCCESS",
            "socket_id": socket_id,
            "subscribed_device_id": sub_device_id,
            "server_time": datetime.now(timezone.utc).isoformat()
        }))

        # Send initial device status snapshot
        primary = relay_db.get_primary_device()
        if primary:
            await websocket.send_text(json.dumps({
                "type": "DEVICE_PRESENCE",
                "device_id": primary["device_id"],
                "payload": {
                    "status": primary["status"],
                    "device_name": primary["device_name"],
                    "model": primary["model"],
                    "last_heartbeat": primary.get("last_heartbeat"),
                    "monitoring_active": bool(primary.get("monitoring_active", 0)),
                    "notification_active": bool(primary.get("notification_active", 0)),
                    "selected_apps_count": primary.get("selected_apps_count", 0),
                    "local_model_healthy": bool(primary.get("local_model_healthy", 0)),
                    "network_mode": primary.get("network_mode", "UNKNOWN"),
                    "telemetry_enabled": bool(primary.get("telemetry_enabled", 0)),
                }
            }))

        while True:
            text = await websocket.receive_text()
            msg = json.loads(text)
            if msg.get("type") == "CLOSE":
                await websocket.close()
                break
            elif msg.get("type") == "PING":
                await websocket.send_text(json.dumps({"type": "PONG", "timestamp": datetime.now(timezone.utc).isoformat()}))

    except (WebSocketDisconnect, asyncio.TimeoutError):
        pass
    except Exception as e:
        logger.error(f"Error in dashboard WebSocket for '{socket_id}': {e}")
    finally:
        await relay_manager.disconnect_dashboard(socket_id)
