# ContextGuard Secure Device Pairing Workflow

**Document Version:** 1.0.0  
**Target Milestone:** Phase 6 Real-Time Integration Milestone  
**Classification:** Security Architecture & Handshake Specification  

---

## 1. Handshake Overview & Threat Model

A device must never appear connected merely because the supervisor dashboard displays a simulated placeholder row. Device pairing in ContextGuard establishes **mutual cryptographic consent**:

1. The mobile device requests a pairing session from the relay.
2. The relay generates an unpredictable, short-lived 6-character code (5-minute TTL).
3. The supervisor inputs this code into the dashboard to claim the session.
4. The mobile user receives an interactive prompt asking for approval.
5. Only upon explicit mobile approval are scoped credentials issued: `device_token` and `dashboard_token`.

```mermaid
sequenceDiagram
    autonumber
    actor User as Mobile Phone User
    participant Phone as Android App
    participant Relay as FastAPI Relay
    participant DB as SQLite DB
    participant Dash as Supervisor Web Dashboard
    actor Supervisor as Examiner Supervisor

    User->>Phone: Selects "Connect Supervisor Dashboard"
    Phone->>Relay: POST /api/v1/relay/pairing/start
    Relay->>DB: Create pairing_session (code: "7K2M9X", TTL: 300s, status: PENDING)
    Relay-->>Phone: { session_id, pairing_code: "7K2M9X", expires_at, qr_payload }
    Phone->>User: Displays "7K2M9X" & waiting indicator

    Supervisor->>Dash: Selects "Connect a Device"
    Supervisor->>Dash: Enters "7K2M9X"
    Dash->>Relay: POST /api/v1/relay/pairing/claim { code: "7K2M9X" }
    Relay->>DB: Validate code & expiry; mark status: CLAIMED
    Relay-->>Dash: { session_id, status: CLAIMED, device_name, device_model }
    Dash->>Supervisor: Displays "Approval Pending on Mobile Device..."

    Phone->>Relay: GET /pairing/status/{session_id} (Polling)
    Relay-->>Phone: { status: "CLAIMED" }
    Phone->>User: Shows "⚠️ Dashboard Connection Request. Approve?"
    
    alt User Confirms Connection
        User->>Phone: Taps "Approve"
        Phone->>Relay: POST /api/v1/relay/pairing/approve { session_id, approved: true }
        Relay->>Relay: Generate devtok_... and dshtok_...
        Relay->>DB: Store SHA-256 hashes; insert into devices (status: ONLINE)
        Relay-->>Phone: { status: APPROVED, device_token: "devtok_..." }
        Relay--)Dash: WS Broadcast: PAIRING_SUCCESS
        Phone->>Phone: Store token in SharedPreferences; connect WebSocket
        Dash->>Dash: Store token in localStorage; connect WebSocket
        Dash->>Supervisor: Live Device Status: ONLINE
    else User Rejects Connection
        User->>Phone: Taps "Deny"
        Phone->>Relay: POST /api/v1/relay/pairing/deny { session_id, approved: false }
        Relay->>DB: Update pairing_sessions (status: DENIED)
        Dash->>Supervisor: Displays "Connection Claim Denied"
    end
```

---

## 2. Security Invariants & Defensive Controls

1. **Unpredictable, Single-Use Codes:**
   - Codes are 6 characters chosen from an unambiguous alphanumeric alphabet (`23456789ABCDEFGHJKLMNPQRSTUVWXYZ`), eliminating `O/0` and `I/1` visual confusion.
   - Once claimed, the code cannot be reused by any other client. Replay attempts return HTTP 400.
2. **Short-Lived Expiration (300 seconds):**
   - Unclaimed sessions expire automatically after 5 minutes.
3. **Dual-Consent Authorization:**
   - Entering the code alone does NOT authorize the supervisor. The mobile user must physically tap "Approve" on their phone screen.
4. **Credential Scoping & Hashing:**
   - Separate tokens are issued: `device_token` (scoped strictly to device operations: heartbeats, outbox events, command ACKs) and `dashboard_token` (scoped to reading events and queueing commands).
   - Only the cryptographic SHA-256 hash of each token is stored in the database. Raw token material is never persisted on disk.
5. **Revocation:**
   - The supervisor or mobile user can revoke the pairing at any time. Revocation invalidates all tokens immediately and updates device status to `UNPAIRED`.
