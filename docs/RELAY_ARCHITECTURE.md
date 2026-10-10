# ContextGuard Real-Time Mobile ↔ Web Relay Architecture

**Document Version:** 1.0.0  
**Target Milestone:** Phase 6 Real-Time Integration Milestone  
**Classification:** Core System Architecture Specification  

---

## 1. Architectural Mission & Overview

ContextGuard is an action-conditioned digital safety assistant. Its real-time architecture connects the **Android mobile client** (edge perception, risk detection, user interaction) with the **Supervisor Web Dashboard** (examiner viva inspection, telemetry stream, command center) through an **authenticated FastAPI relay layer**.

```
┌────────────────────────────────────────────────────────┐
│                   ANDROID CLIENT                       │
│  - ML Kit Text Recognition & Face Contour Mapping      │
│  - In-Memory Redaction Engine (Blackout & Blur)        │
│  - Local PII Extraction & Gating                       │
│  - Device Identity Storage (UUID + SharedPreferences)  │
│  - Telemetry Outbox & Retry Engine                     │
│  - OkHttp Authenticated WebSocket Client               │
│  - Inbound Command Executor (Allowlisted types only)   │
└───────────────────────────▲────────────────────────────┘
                            │
                            │ Authenticated HTTPS / REST
                            │ Authenticated WSS / WebSocket
                            │
┌───────────────────────────▼────────────────────────────┐
│              CONTEXTGUARD FASTAPI RELAY                │
│             (Local Workstation / Server)               │
│                                                        │
│  +-- 1. Pairing Handshake & Identity Manager           │
│  +-- 2. Device Presence Tracker (Heartbeat timeout 30s)│
│  +-- 3. Privacy-Safe Invariant Filter & Sanitizer      │
│  +-- 4. Durable SQLite Event Ledger & Command Queue    │
│  +-- 5. In-Process WebSocket Hub & Fan-Out Broker      │
│  +-- 6. Scoped Bearer Token Authorization              │
│  +-- 7. Audit Logging Engine                           │
└───────────────────────────▲────────────────────────────┘
                            │
                            │ Authenticated REST & WebSocket
                            │
┌───────────────────────────▼────────────────────────────┐
│             SUPERVISOR WEB DASHBOARD                   │
│  - Real-Time Device Presence & Capability Inspection   │
│  - Live Telemetry Stream (Zero-refresh live updates)   │
│  - Interactive Command Center (Status, Diag, Demo)     │
│  - Pairing Wizard Modal                                │
│  - Historical Research Panels (EARB, Ablation, Models) │
└────────────────────────────────────────────────────────┘
```

Both clients share the **same backend instance, device identity, event store, and command protocol**. There is **no direct peer-to-peer connection** between phone and browser; the FastAPI relay serves as the authenticated point of synchronization.

---

## 2. Core Architectural Subsystems

### 2.1. Backend Relay Layer (`backend/app/relay/`)

1. **Persistence Store (`persistence.py`):**
   - Implemented via high-speed SQLite in WAL mode.
   - Tables:
     - `devices`: Tracks device ID, hardware details, current presence state (`ONLINE`, `DEGRADED`, `OFFLINE`, `UNPAIRED`), SHA-256 hashed credentials, and monitoring capabilities.
     - `pairing_sessions`: Short-lived sessions with 6-character single-use alphanumeric codes, 5-minute TTL, and single-claim constraints.
     - `events`: Durable event ledger recording privacy-safe telemetry events. Enforces idempotency on `event_id`.
     - `commands`: Persistent command queue tracking lifecycle states (`QUEUED`, `DELIVERED`, `ACKNOWLEDGED`, `SUCCEEDED`, `FAILED`, `EXPIRED`, `CANCELLED`).
     - `audit_logs`: Tamper-evident record of all pairing, command, and connection events.
2. **WebSocket Hub & Fan-Out Broker (`connection_manager.py`):**
   - In-process connection registry tracking active device WebSockets and subscribed supervisor dashboards.
   - Immediately fans out inbound mobile telemetry events to connected supervisor dashboards.
   - Attempts instantaneous command delivery over active device WebSockets upon enqueueing.
3. **Privacy Invariant Filter (`privacy_filter.py`):**
   - Deep inspection of all inbound payloads.
   - Rejects payloads containing raw image base64, raw passwords, unmasked credit cards, or raw notification bodies.

### 2.2. Distributed Scaling & Multi-Worker Extension Point

> [!NOTE]
> The current in-process WebSocket connection manager is designed for single-process workstation deployments (such as the examiner viva laptop demonstration). If ContextGuard is deployed across multiple Uvicorn worker processes or clustered horizontally, the in-process registry must be backed by an external distributed Pub/Sub mechanism (such as Redis Pub/Sub or RabbitMQ).

The relay architecture isolates all broadcast logic inside `RelayConnectionManager.broadcast_to_dashboards(...)` and `deliver_command(...)`, allowing a drop-in replacement with a Redis message bus adapter without altering client contracts.

---

## 3. Topologies & Data Flows

### 3.1. Telemetry Ingestion Flow
```
1. Mobile Safety Trigger (Accessibility risk or Notification triage)
   ↓
2. Edge Perception & In-Memory Redaction (Zero raw disk writes)
   ↓
3. Telemetry Gating Check (Is telemetry sync enabled? Is network mode != OFFLINE?)
   ↓ (If permitted)
4. Outbox Enqueue & Immediate WebSocket Transmission
   ↓
5. FastAPI Relay receives frame -> Privacy Filter inspects payload
   ↓
6. Durable SQLite insertion (Idempotent by event_id)
   ↓
7. WebSocket Fan-Out to Subscribed Supervisor Dashboards
   ↓
8. Dashboard UI updates dynamically without page reload
```

### 3.2. Bidirectional Command Channel Flow
```
1. Supervisor selects permitted command (e.g. REQUEST_STATUS)
   ↓
2. POST /api/v1/relay/commands/queue with Bearer DashboardToken
   ↓
3. Relay enqueues command in SQLite (Status: QUEUED)
   ↓
4. If Device WebSocket is active:
      - Transmit COMMAND_DELIVER frame immediately
      - Mark status: DELIVERED
   Else:
      - Command waits in durable queue for reconnection
   ↓
5. Android app receives frame -> Validates command type against allowlist
   ↓
6. Android executes action (collects genuine runtime telemetry)
   ↓
7. Android sends COMMAND_ACK frame back over WebSocket
   ↓
8. Relay updates SQLite (Status: SUCCEEDED / FAILED)
   ↓
9. Relay broadcasts COMMAND_STATUS_UPDATE to Dashboard
   ↓
10. Dashboard Command Center displays real returned result
```

---

## 4. Privacy & Security Invariants

1. **Zero Raw Screen Capture:** The supervisor dashboard cannot request a raw screen upload or stream raw video of the phone.
2. **No Remote Code Execution:** The command channel only permits 4 explicit pre-defined commands (`REQUEST_STATUS`, `REQUEST_DIAGNOSTICS`, `RUN_SYNTHETIC_DEMO`, `REQUEST_CONFIG_REFRESH`).
3. **Zero Egress when Opted-Out:** If the mobile user disables "Supervisor Telemetry Sync" or toggles `OFFLINE` mode, zero bytes are transmitted to the relay.
4. **Hashed Credentials:** Device and dashboard tokens are stored only as SHA-256 hashes on the backend.
