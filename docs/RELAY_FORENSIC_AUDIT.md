# ContextGuard Relay Forensic Audit & Integration Gap Analysis

**Document Version:** 1.0.0  
**Audit Date:** Phase 6 Real-Time Integration Milestone  
**Scope:** Android Mobile Application (`android/`), Supervisor Web Dashboard (`supervisor-dashboard/`), FastAPI Backend (`backend/app/`), Persistence & Relay Contracts  
**Auditor:** Principal Backend Architect, Android Networking Engineer & Security Lead  

---

## 1. Executive Summary & Forensic Statement

ContextGuard was designed as an action-conditioned digital safety system spanning an Android client and a supervisor examination dashboard. Prior to this milestone, an in-depth forensic inspection revealed that the **Android mobile application** and the **Supervisor Web Dashboard** operated as **completely disconnected products**:

1. **Dashboard Illusion of Connectivity:** The Supervisor Dashboard (`supervisor-dashboard/`) displayed an active Android device status card showing `"CONNECTED"` and `"Android Emulator (API 34)"`. In reality, this data was **completely hardcoded** in `backend/app/api/v1/supervisor.py` (lines 67–73). The dashboard simply polled this static JSON fixture; it had no knowledge of whether a phone was connected, running, or turned off.
2. **No Live Device-to-Dashboard Relay:** There was **zero WebSocket relay infrastructure** in either the FastAPI backend or the Android app. No real mobile risk triggers, notification flags, or intervention events were ever broadcast to the web dashboard.
3. **Android Client Isolation:** The Android application's only network communication was an HTTP client (`ContextGuardApiClient.kt`) targeting `POST /api/v1/analyze`. It had no pairing flow, no device token, no heartbeat, no telemetry outbox, and no command listener.
4. **No Remote Command Execution:** The supervisor dashboard had no mechanism to send commands (such as diagnostic requests or running synthetic demos) back to the device, nor did Android have an inbound command processor.

This forensic audit catalogues the exact origin of every value currently displayed in the dashboard, the network requests issued by Android, and the exact architectural gaps that must be resolved.

---

## 2. Supervisor Web Dashboard Data Provenance Breakdown

We traced every component in `supervisor-dashboard/src/components/sections/` and `src/services/api.ts` to determine the source of its data:

| Section / Component | Source File | Data Provenance | Real Live Data or Fixture? |
| :--- | :--- | :--- | :--- |
| **System Overview - Android Status** | `SystemOverview.tsx` (lines 36–60) | Polled via `fetchSystemOverview()` -> `/api/v1/supervisor/overview` | **STATIC MOCK.** `supervisor.py` lines 67–73 returned a hardcoded dictionary: `{"status": "CONNECTED", "device": "Android Emulator (API 34)", "active_mode": "REDACTED_LOCAL_BACKEND"}`. Completely simulated. |
| **System Overview - ML Kit / VLM** | `SystemOverview.tsx` (lines 62–99) | Polled via `/api/v1/supervisor/overview` | **HYBRID.** ML Kit string is static; Ollama / VLM status queries the real local Ollama server health probe. |
| **Live Analysis** | `LiveAnalysis.tsx` | User clicks "Run Live Pre-Action Analysis" -> `POST /api/v1/analyze` | **REAL API (Manual Browser Only).** Executes the real 6-stage pipeline in FastAPI, but uses static browser test artifacts (`ART-FIN-001`, etc.), **NOT live mobile events**. |
| **Pipeline Trace** | `PipelineTrace.tsx` | Prop `latestResult` passed from `LiveAnalysis` | **REAL API (Browser Triggered).** Reflects the last manual analysis run in the browser tab. |
| **EARB Benchmark** | `EarbBenchmark.tsx` | `fetchEarbData()` -> `/api/v1/supervisor/earb` | **GENERATED BENCHMARK DATA.** Serves the frozen evaluation dataset from `benchmark/data/earb_v1.jsonl`. |
| **Model Performance** | `ModelPerformance.tsx` | `fetchEvaluationResults()` -> `/api/v1/supervisor/results` | **GENERATED BENCHMARK DATA.** Serves precomputed metrics from Phase 5 evaluation runs (`results/evaluation_summary.json`). |
| **Baseline Comparison** | `BaselineComparison.tsx` | `fetchEvaluationResults()` | **GENERATED BENCHMARK DATA.** Evaluated baselines (B1, B3, B4, B5) vs ContextGuard. |
| **Ablation Results** | `AblationResults.tsx` | `fetchEvaluationResults()` | **GENERATED BENCHMARK DATA.** Precomputed ablation metrics (A1–A5). |
| **Privacy-Utility Tradeoff** | `PrivacyUtility.tsx` | `fetchEvaluationResults()` | **GENERATED BENCHMARK DATA.** Mathematical $\Delta \rho$ trade-off tables. |
| **Failure Analysis** | `FailureAnalysis.tsx` | `fetchFailureAnalysis()` -> `/api/v1/supervisor/failure-analysis` | **GENERATED BENCHMARK DATA.** Analyzes false ACTs and false STOPs from the test set. |
| **Network Activity** | `NetworkActivity.tsx` | `fetchNetworkActivity()` -> `/api/v1/supervisor/network-activity` | **IN-MEMORY FIXTURE.** Returns volatile request log from backend memory; not connected to Android outbox. |
| **Model Health** | `ModelHealth.tsx` | `fetchModelHealth()` -> `/api/v1/health/models` | **REAL API.** Actual HTTP probe to local Ollama daemon and local XGBoost model file check. |

---

## 3. Android Mobile Application Networking Audit

We inspected all network-related classes in `android/app/src/main/java/com/contextguard/app/`:

### 3.1. Identified Network Endpoints Called
1. **`POST /api/v1/analyze`** (in `ContextGuardApiClient.kt`):
   - Called from `MainViewModel.kt` during manual or sharesheet analysis if `networkMode != OFFLINE`.
   - Sends redacted PII metadata, face counts, and action context.
   - **Does NOT** register device, does NOT send heartbeats, does NOT subscribe to commands.
2. **`GET /api/v1/health`** (in `HealthCheckClient.kt` / `MainViewModel.kt`):
   - Probes basic backend reachability to toggle the green/grey connection dot in the top app bar.

### 3.2. Missing Mobile Networking Capabilities
- **No Pairing Flow:** The Android app has no screen or client method to initiate a pairing handshake, display a pairing PIN / QR code, or approve a web dashboard claim.
- **No Device Identity:** No persistent `device_id` or cryptographic credentials stored in private Android storage.
- **No WebSocket Client:** OkHttp is imported in Gradle, but no `WebSocketListener` or persistent socket connection exists.
- **No Event Outbox / WorkManager Ingestion:** Subsystem outputs (Accessibility risk triggers, Notification guardian triage, local OCR findings) remain isolated on-device; no durable outbox queue exists to stream privacy-safe summaries to the supervisor.
- **No Command Ingestion:** The app has no listener for `REQUEST_STATUS`, `REQUEST_DIAGNOSTICS`, `RUN_SYNTHETIC_DEMO`, or `REQUEST_CONFIG_REFRESH`.

---

## 4. Root Causes of the Disconnected Architecture

1. **Independent Subsystem Development:** The Android application and the FastAPI backend were developed with clean REST boundaries (`POST /analyze`), but without an event-driven relay bus to bridge mobile events to the web supervisor.
2. **Dashboard Prototype Mocking:** To allow early UI development of the supervisor dashboard without an active phone connected via USB/ADB, `supervisor.py` supplied a hardcoded `"android_connection"` status object. This temporary placeholder was never replaced with a live presence registry.
3. **Absence of Shared Event Ledger:** There was no database table or in-memory bus for storing mobile telemetry events or command queues.
4. **Single-Direction Assumption:** The initial API was designed strictly as a synchronous client-to-server RPC (`analyze(request) -> response`), rather than a bidirectional control channel.

---

## 5. Architectural Gap Analysis & Remediation Requirements

To achieve a true, authenticated, bidirectional device relay, the following gaps must be closed:

```
┌─────────────────────────┐                                 ┌─────────────────────────┐
│       Android App       │                                 │   Supervisor Web UI     │
│                         │                                 │                         │
│  - Device Pairing UI    │                                 │  - Pairing Modal        │
│  - Persistent Device ID │                                 │  - Live Status Panel    │
│  - Secure Token Storage │                                 │  - Live Event Feed      │
│  - Telemetry Outbox     │                                 │  - Command Console      │
│  - WebSocket Client     │                                 │  - WebSocket Client     │
│  - Command Handler      │                                 │                         │
└────────────▲────────────┘                                 └────────────▲────────────┘
             │                                                           │
             │ HTTPS / WSS (Auth: Bearer DeviceToken)                    │ WSS / REST (Auth: DashboardToken)
             │                                                           │
             ▼───────────────────────────────────────────────────────────▼
                                 FASTAPI RELAY LAYER
             ┌───────────────────────────────────────────────────────────┐
             │ - Pairing Session Manager (6-char PIN, 5-min TTL, Claim)  │
             │ - Token Auth & Origin Validation                          │
             │ - Device Presence Tracker (Heartbeat timeout 30s)         │
             │ - Durable SQLite Persistence (Devices, Events, Commands)  │
             │ - WebSocket Pub/Sub Hub (In-process fan-out)              │
             │ - Command Queue & ACK State Machine                       │
             │ - Privacy Filter (Zero raw pixels/passwords invariant)    │
             └───────────────────────────────────────────────────────────┘
```

### Specific Technical Tasks Required:
1. **FastAPI Relay Backend (`backend/app/relay/` & `backend/app/api/v1/relay.py`):**
   - SQLite persistence layer for `devices`, `pairing_sessions`, `events`, `commands`, and `audit_log`.
   - Pydantic models for pairing, heartbeat, privacy-safe event ingestion, and command dispatch.
   - Device WebSocket `/api/v1/relay/ws/device` and Dashboard WebSocket `/api/v1/relay/ws/dashboard`.
   - Update `backend/app/api/v1/supervisor.py` to source actual device status from the live registry rather than returning the hardcoded mock.
2. **Device Pairing Security:**
   - Single-use 6-character random alphanumeric pairing codes, rate-limited, 5-minute expiry.
   - Dual-approval handshake: Phone generates -> Web claims -> Phone user approves -> Scoped tokens issued (`device_token`, `dashboard_token`).
   - Tokens stored with SHA-256 password hashing in backend SQLite.
3. **Android Client Integration (`com.contextguard.app.core.relay`):**
   - Implement `RelayManager` and `RelayWebSocketClient` using OkHttp WebSocket.
   - Secure token storage using Android `SharedPreferences`.
   - Implement privacy-safe event outbox with idempotency and retry.
   - Implement command receiver supporting `REQUEST_STATUS`, `REQUEST_DIAGNOSTICS`, `RUN_SYNTHETIC_DEMO`, and `REQUEST_CONFIG_REFRESH`.
   - Add "Supervisor Relay & Pairing" section in `SupervisorScreen.kt` and an explicit opt-in toggle in `SettingsScreen.kt`.
4. **Supervisor Web Dashboard Integration (`supervisor-dashboard/`):**
   - Implement `relay.ts` WebSocket client with auto-reconnect, exponential backoff, and heartbeat.
   - Replace hardcoded connection cards in `SystemOverview.tsx` with live data from relay state.
   - Add real-time event feed and interactive Command Center.
5. **Connection Profiles & Documentation:**
   - Support `LOCAL_LAN_DEMO` (`0.0.0.0` binding, `10.0.2.2` emulator, local LAN IP for phone) and `REMOTE_SERVER` (HTTPS/WSS).
   - Write comprehensive protocol and runbook documentation.
   - Verify with automated pytest suite and Android unit tests.
