# ContextGuard Real-Time Relay Protocol Specification

**Specification Version:** 1.0.0  
**Target Milestone:** Phase 6 Real-Time Integration Milestone  
**Classification:** Wire Protocol & API Contract Standard  

---

## 1. Documented Message & Event Envelope

All real-time messages exchanged across the WebSocket relay and persisted in the event ledger conform to the canonical `RelayEnvelope` structure:

```json
{
  "schema_version": "1.0.0",
  "message_id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "event_id": "evt-77a83d-992a",
  "type": "TELEMETRY_EVENT",
  "device_id": "dev_39a8fbc7102e",
  "timestamp": "2026-10-10T12:00:00.000000Z",
  "payload": { ... },
  "correlation_id": "sha256_b3f9472e9a0c..."
}
```

### Field Definitions

| Field | Type | Required | Description |
| :--- | :--- | :---: | :--- |
| `schema_version` | String | **Yes** | Semantic envelope version. Always `"1.0.0"`. |
| `message_id` | String (UUIDv4) | **Yes** | Unpredictable unique message identifier for transmission tracing. |
| `event_id` | String | *Optional* | Persisted event identifier for idempotent deduplication. |
| `type` | String (Enum) | **Yes** | Classification frame type (e.g. `TELEMETRY_EVENT`, `COMMAND_DELIVER`). |
| `device_id` | String | **Yes** | Scoped paired device identity (`dev_<hex16>`). |
| `timestamp` | String (ISO-8601) | **Yes** | ISO-8601 UTC timestamp of creation. |
| `payload` | Object | **Yes** | Bounded, strictly typed payload dictionary. |
| `correlation_id` | String | *Optional* | Cryptographic SHA-256 fingerprint linking to on-device artifact. |

---

## 2. REST API Routes (`/api/v1/relay`)

### 2.1. Pairing Endpoints

| Method | Endpoint | Authorization | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/relay/pairing/start` | Public (Phone) | Initiates a short-lived pairing session; returns 6-character code and QR string. |
| `POST` | `/api/v1/relay/pairing/claim` | Public (Web) | Claims a session with a one-time code; transitions status to `CLAIMED`. |
| `GET` | `/api/v1/relay/pairing/status/{session_id}` | Public | Polls pairing status during mutual handshake. |
| `POST` | `/api/v1/relay/pairing/approve` | Public (Phone) | Phone user confirms connection claim; issues scoped tokens and activates device. |
| `POST` | `/api/v1/relay/pairing/deny` | Public (Phone) | Phone user rejects connection claim; cancels session. |
| `POST` | `/api/v1/relay/pairing/revoke` | Bearer (Dashboard) | Revokes paired device credentials and marks device `UNPAIRED`. |

### 2.2. Device Status & Heartbeat Endpoints

| Method | Endpoint | Authorization | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/relay/device/heartbeat` | Bearer (Device) | Submits device capability heartbeat; refreshes online presence. |
| `GET` | `/api/v1/relay/device/status/{device_id}` | Bearer (Dashboard) | Retrieves verified device status and capabilities. |
| `GET` | `/api/v1/relay/device/primary` | Public / Session | Auto-discovers primary paired device for dashboard connection panel. |
| `POST` | `/api/v1/relay/device/unpair` | Bearer (Device) | Device self-unpairs and clears stored token. |

### 2.3. Event Telemetry Endpoints

| Method | Endpoint | Authorization | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/relay/events/ingest` | Bearer (Device) | Durable ingestion of privacy-safe telemetry; fans out to WebSockets. |
| `GET` | `/api/v1/relay/events/{device_id}` | Bearer (Dashboard) | Retrieves persisted telemetry events for device (supports pagination). |
| `GET` | `/api/v1/relay/events/detail/{event_id}` | Bearer (Dashboard) | Retrieves detailed telemetry record for single event. |

### 2.4. Command Channel Endpoints

| Method | Endpoint | Authorization | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/relay/commands/queue` | Bearer (Dashboard) | Enqueues authorized command (`REQUEST_STATUS`, `REQUEST_DIAGNOSTICS`, `RUN_SYNTHETIC_DEMO`, `REQUEST_CONFIG_REFRESH`). |
| `GET` | `/api/v1/relay/commands/pending/{device_id}`| Bearer (Device) | Device polls queued commands upon reconnect; marks them `DELIVERED`. |
| `POST` | `/api/v1/relay/commands/ack` | Bearer (Device) | Device submits execution outcome; marks command `SUCCEEDED` / `FAILED`. |
| `GET` | `/api/v1/relay/commands/recent/{device_id}` | Bearer (Dashboard) | Retrieves recent command execution history and results. |

---

## 3. Real-Time WebSocket Protocols

### 3.1. Device Channel (`/api/v1/relay/ws/device`)

- **Step 1 (Client Handshake):** Client opens socket.
- **Step 2 (Auth Frame):** Client transmits:
  ```json
  {"token": "devtok_...", "device_id": "dev_..."}
  ```
- **Step 3 (Auth Ack):** Server returns:
  ```json
  {"type": "AUTH_SUCCESS", "device_id": "dev_...", "server_time": "..."}
  ```
- **Step 4 (Keepalive / Heartbeat):** Client periodically sends:
  ```json
  {"type": "HEARTBEAT", "payload": { "monitoring_active": true, ... }}
  ```
  Server returns: `{"type": "HEARTBEAT_ACK"}`.
- **Step 5 (Inbound Commands):** Server pushes:
  ```json
  {
    "type": "COMMAND_DELIVER",
    "payload": {
      "command_id": "cmd_...",
      "command_type": "REQUEST_STATUS",
      "payload": { ... }
    }
  }
  ```
- **Step 6 (Command Ack):** Client executes action and replies:
  ```json
  {
    "type": "COMMAND_ACK",
    "payload": {
      "command_id": "cmd_...",
      "status": "SUCCEEDED",
      "result": { "battery": 0.88, "monitoring": "ACTIVE" }
    }
  }
  ```

### 3.2. Dashboard Channel (`/api/v1/relay/ws/dashboard`)

- **Step 1 (Client Handshake):** Dashboard opens socket.
- **Step 2 (Auth Frame):**
  ```json
  {"token": "dshtok_...", "device_id": "*"}
  ```
- **Step 3 (Auth Ack & Initial Snapshot):** Server returns:
  ```json
  {"type": "AUTH_SUCCESS", "socket_id": "dash_...", "server_time": "..."}
  {"type": "DEVICE_PRESENCE", "device_id": "dev_...", "payload": { "status": "ONLINE", ... }}
  ```
- **Step 4 (Live Telemetry Broadcast):** Whenever a phone ingests an event, server immediately broadcasts:
  ```json
  {
    "type": "TELEMETRY_EVENT",
    "device_id": "dev_...",
    "payload": {
      "event_id": "evt-123",
      "source_app": "com.whatsapp",
      "intervention": "STOP",
      "risk_score": 0.88,
      "severity": 0.90,
      "reversibility": 1.0,
      "confidence": 0.95,
      "evidence_summary": ["Masked account [ACCOUNT_REDACTED]"]
    }
  }
  ```
- **Step 5 (Command Status Broadcast):** When device acks a command, server broadcasts:
  ```json
  {
    "type": "COMMAND_STATUS_UPDATE",
    "device_id": "dev_...",
    "payload": {
      "command_id": "cmd_...",
      "status": "SUCCEEDED",
      "result": { ... }
    }
  }
  ```

---

## 4. Command State Machine

```
   ┌──────────┐
   │  QUEUED  │ (Enqueued by Supervisor)
   └────┬─────┘
        │ Device Connected / Polled
        ▼
 ┌──────────────┐
 │  DELIVERED   │ (Received by Android client)
 └──────┬───────┘
        ├─────────────────────────────┬─────────────────────────────┐
        ▼                             ▼                             ▼
┌───────────────┐             ┌───────────────┐             ┌───────────────┐
│   SUCCEEDED   │             │    FAILED     │             │    EXPIRED    │
│ (Real Result) │             │ (Error Rpt)   │             │ (TTL Elapsed) │
└───────────────┘             └───────────────┘             └───────────────┘
```
