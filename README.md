<div align="center">

# ContextGuard

**A Multimodal AI System for Action-Conditioned Pre-Action Digital Risk Detection in Everyday Mobile Tasks**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Python 3.11 | 3.12](https://img.shields.io/badge/Python-3.11%20%7C%203.12-3776AB.svg?logo=python&logoColor=white)](https://www.python.org/)
[![Android API 34](https://img.shields.io/badge/Android-API%2034%20%28Compose%29-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110%2B-009688.svg?logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![React 19 & TypeScript](https://img.shields.io/badge/React%2019-TypeScript%205-61DAFB.svg?logo=react&logoColor=white)](https://react.dev/)
[![Tests Passing](https://img.shields.io/badge/Tests-99%2F99%20Passing-brightgreen.svg)]()
[![Privacy Invariants](https://img.shields.io/badge/Privacy-Zero%20Raw%20Persistence-9C27B0.svg)]()

[Research Overview](#-the-research-hypothesis) •
[Architecture](#-system-architecture) •
[Quickstart](#-quickstart--installation) •
[Live Relay](#-real-time-mobile--web-relay) •
[Testing](#-testing--verification) •
[Contributing](#-contributing)

</div>

---

## 🎯 The Research Hypothesis

Traditional security mechanisms (anti-malware scans, spam filters, static heuristics) evaluate safety as an isolated property of the digital artifact:

$$\text{Risk}_{\text{traditional}} = f(\text{Artifact})$$

In everyday digital workflows, however, **risk is fundamentally inseparable from the user's intended action and destination context**. An artifact that is completely safe to archive locally may be catastrophic to publish publicly. ContextGuard formulates safety as an action-conditioned decision:

$$\text{Risk}_{\text{ContextGuard}} = \mathcal{P}(\text{Artifact} + \text{Context} + \text{Intended Action} + \text{Evidence} + \text{Consequence} + \text{Uncertainty})$$

### The Central Viva Demonstration: Bank Statement Triad
To prove this hypothesis empirically, ContextGuard evaluates an identical synthetic bank statement across three distinct contexts:

| Action Scenario | Target Recipient / Channel | Severity ($s$) | Irreversibility ($r$) | Risk ($\rho$) | Resulting Intervention |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **1. Save Privately** | Encrypted Cloud Vault / Device Storage | $0.05$ | $0.00$ | $0.05$ | **ACT (Green)** |
| **2. Send via Chat** | Unverified Contact via Messaging App | $0.45$ | $0.50$ | $0.62$ | **WARN / ASK (Orange/Yellow)** |
| **3. Post Publicly** | Public Social Media Broadcast Feed | $0.90$ | $1.00$ | $1.57$ | **STOP (Red)** |

> **Key Takeaway:** The artifact remains constant, but the intervention adapts dynamically to action, destination, and irreversibility.

---

## 🛡️ The 4 Safety Interventions

ContextGuard deterministically computes risk $\rho = s \cdot (1 + \lambda \cdot r)$ with frozen calibration ($\lambda = 0.75$) and maps it to one definitive intervention:

* <span style="color:#4CAF50">**ACT (Green):**</span> Safe to proceed. Negligible or well-bounded risk ($\rho < 0.35$ with adequate confidence).
* <span style="color:#FFEB3B">**ASK (Yellow):**</span> Ambiguous context, unverified recipient, or elevated uncertainty ($c < 0.70$). Clarifying confirmation required.
* <span style="color:#FF9800">**WARN (Orange):**</span> Detectable risk with reversible consequence ($0.35 \le \rho < 0.65$). Explicit hazard warning presented before proceeding.
* <span style="color:#F44336">**STOP (Red):**</span> Severe, critical, or irreversible harm detected ($\rho \ge 0.65$ or $s \ge 0.80$). Action blocked by default with emergency user override.

---

## 🏗️ System Architecture

```mermaid
flowchart TD
    subgraph Mobile ["Android Native Client (Kotlin + Jetpack Compose)"]
        Artifact[Inbound Artifact: Sharesheet / ACTION_SEND / Image] --> EdgePerception[Edge Perception Engine]
        EdgePerception --> OCR[Google ML Kit Text Recognition]
        EdgePerception --> Face[Google ML Kit Face Detection]
        EdgePerception --> PII[On-Device PII & Regex Engine]
        EdgePerception --> PDF[Native PdfRenderer]
        
        OCR & Face & PII & PDF --> Redaction[Volatile Memory Redaction Engine]
        Redaction --> Overlay[JIT Accessibility Overlay]
        Redaction --> RelayMgr[Relay Manager & Durable Outbox]
    end

    subgraph Backend ["FastAPI Relay & Inference Backend (Python 3.12)"]
        RelayMgr <-->|WebSocket ws://.../ws/device| ConnBroker[Relay Connection Broker]
        RelayMgr -->|REST Ingest| RelayRouter["/api/v1/relay/*"]
        
        RelayRouter --> PrivacyFilter[Zero-Leakage Privacy Engine]
        PrivacyFilter --> SQLiteLedger[(SQLite WAL Ledger: relay.db)]
        
        RelayRouter --> Pipeline[6-Stage Reasoning Pipeline]
        subgraph Models ["3-Tier AI / ML Subsystems"]
            Pipeline --> Tier1[1. On-Device Perception & Lexical Features]
            Pipeline --> Tier2[2. XGBoost Phishing Classifier (PhiUSIIL)]
            Pipeline --> Tier3[3. Qwen2.5-VL-3B Multimodal Vision Reasoner]
        end
        Models --> PolicyEngine[Deterministic Policy Engine]
    end

    subgraph Dashboard ["Supervisor Web Dashboard (React 19 + TypeScript + Vite)"]
        ConnBroker <-->|WebSocket ws://.../ws/dashboard| DashWS[Dashboard WebSocket Client]
        DashWS --> LivePanel[Live Mobile Relay Panel]
        DashWS --> CmdCenter[Interactive Command Center]
        LivePanel --> EventFeed[Real-Time Telemetry Feed]
        CmdCenter -->|Command Dispatch| ConnBroker
    end
```

---

## ⚡ Key Features

- **📱 Edge Perception & JIT Interventions:**
  - Google ML Kit on-device OCR and Face Detection running completely offline.
  - Native `PdfRenderer` rasterizing first 3 pages with memory-safe buffer recycling.
  - Just-In-Time intervention overlay using Android `AccessibilityService` (`TYPE_ACCESSIBILITY_OVERLAY`), floating non-intrusively above third-party applications without synthetic click injection.
  - System-level Android Sharesheet integration (`ACTION_SEND` and `ACTION_SEND_MULTIPLE`).
- **🧠 6-Stage Reasoning Pipeline:**
  1. *Context Aggregation* $\to$ 2. *Intent Disambiguation* $\to$ 3. *Action-Relevant Evidence Extraction* $\to$ 4. *Consequence Analysis ($s, r$)* $\to$ 5. *Epistemic Uncertainty Calibration ($c$)* $\to$ 6. *Deterministic Policy Enforcement*.
- **🔬 3-Tier AI / ML Subsystems:**
  - **Tier 1:** Deterministic local heuristics & ML Kit on-device perception.
  - **Tier 2:** 36-feature XGBoost URL classifier trained on PhiUSIIL (UCI ID 967) achieving **Test F1: 0.9951, AUC: 0.9990**.
  - **Tier 3:** `Qwen2.5-VL-3B` vision-language reasoning via local Ollama runtime with strict JSON schema constraints.
- **🔄 Real-Time Bidirectional Relay:**
  - Authenticated 6-character shortcode pairing handshake with 5-minute timeout.
  - Scoped cryptographically salted SHA-256 tokens (`devtok_*` for phone, `dshtok_*` for dashboard).
  - Sub-10ms WebSocket fan-out for live telemetry streaming.
  - Durable SQLite WAL outbox with auto-flush on reconnect.
  - Controlled Command Center dispatching synthetic benchmarks and diagnostics.
- **🔒 Privacy & Zero Raw Persistence:**
  - Strict privacy enforcement filter (`enforce_privacy_or_raise`) rejects any payload containing raw screenshots, unredacted passwords, credit cards, or OTPs.
  - SHA-256 fingerprinting for telemetry logs.

---

## 📂 Repository Structure

```text
Context-Gaurd/
├── android/                      # Native Kotlin + Jetpack Compose Android Client
│   ├── app/src/main/             # Compose UI, Edge Perception, Relay, Accessibility
│   ├── app/src/test/             # Android Unit Tests (Relay, Overlay, Perception)
│   ├── build.gradle.kts          # Dependencies (ML Kit, OkHttp 4.12, Compose)
│   └── gradlew.bat               # Gradle 8.3 Wrapper
├── backend/                      # Python 3.12+ FastAPI Server
│   ├── app/api/v1/               # API Routers (/analyze, /relay, /supervisor, /health)
│   ├── app/relay/                # Persistence (SQLite WAL), WebSocket Broker, Privacy
│   ├── app/core/                 # Pydantic Settings & Config
│   ├── models/                   # Qwen2.5-VL Vision Reasoner
│   ├── pipeline/                 # Six-Stage Reasoning Pipeline
│   ├── policy/                   # Deterministic Policy Engine & Thresholds
│   └── requirements.txt          # Python Dependencies
├── supervisor-dashboard/         # Supervisor Web Application
│   ├── src/components/sections/  # Live Relay Panel, Pairing Modal, Benchmark Views
│   ├── src/services/             # WebSocket Client & Relay REST Client
│   ├── src/types/                # Shared TypeScript Data Contracts
│   └── vite.config.ts            # Vite Build Configuration
├── ml/                           # Machine Learning Training & Datasets
│   ├── datasets/                 # PhiUSIIL URL Dataset Acquisition
│   ├── features/                 # 36 Lexical & Structural URL Features
│   └── training/                 # XGBoost Classifier Training Pipeline
├── benchmark/                    # Everyday Action Risk Benchmark (EARB)
│   ├── data/                     # 60 Action-Conditioned Artifact Pairs (earb_v1.jsonl)
│   ├── schema/                   # Pydantic Benchmark Schema & CLI Validator
│   └── annotator/                # Local Annotation Web Interface
├── tests/                        # Comprehensive Automated Test Suites
│   ├── backend/                  # Relay, Health, Config, Pipeline & Policy tests
│   ├── integration/              # Full Round-Trip Relay E2E Integration tests
│   ├── ml/                       # XGBoost & URL Risk tests
│   └── benchmark/                # EARB validation & CAC metric tests
├── docs/                         # In-Depth Engineering & Research Documentation
│   ├── RELAY_ARCHITECTURE.md     # Relay Transport Architecture
│   ├── RELAY_PROTOCOL.md         # Wire Protocol & WebSocket Specifications
│   ├── PAIRING_FLOW.md           # Device Pairing Sequence & State Machine
│   ├── CONNECTION_SETUP.md       # LAN IPv4 & Loopback Configuration Guide
│   ├── RELAY_SECURITY.md         # Threat Model & Cryptographic Guarantees
│   ├── END_TO_END_RELAY_TESTS.md # Test Strategy & Verification Reproduction
│   └── RELAY_RELEASE_REPORT.md   # Milestone Completion & Latency Metrics
├── CONTRIBUTING.md               # Contribution Guidelines & Workflow
├── PROJECT_STATUS.md             # System Deliverables Audit & Test Logs
├── ARCHITECTURE.md               # Complete System Specification
├── DEMO_RUNBOOK.md               # Viva & Live Demonstration Script
└── LICENSE                       # MIT License
```

---

## ⚡ Quickstart & Installation

### Prerequisites
- **Python:** 3.11+ (Python 3.12 tested)
- **Node.js:** 18+ (Node 20+ recommended)
- **Java JDK:** JDK 17 LTS (e.g. OpenJDK 17)
- **Android SDK:** API 34+ (Build-tools 34.0.0)

---

### 1. Backend Setup (FastAPI & Relay)
```powershell
# 1. Clone repository
git clone https://github.com/Garvanand/Context-Gaurd.git
cd Context-Gaurd

# 2. Setup Python environment
python -m venv .venv
.venv\Scripts\Activate.ps1   # On Linux/macOS: source .venv/bin/activate

# 3. Install dependencies
pip install -r backend/requirements.txt

# 4. Launch FastAPI Server
python -m uvicorn backend.app.main:app --host 0.0.0.0 --port 8000 --reload
```
API Documentation will be live at: `http://localhost:8000/docs`

---

### 2. Supervisor Web Dashboard Setup
```powershell
# Open a new terminal
cd supervisor-dashboard

# Install packages
npm install

# Start Vite development server
npm run dev
```
Supervisor Interface will be accessible at: `http://localhost:5173`

---

### 3. Android Client Setup
```powershell
# Open a new terminal
cd android

# Set Java 17 home (if not configured globally)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Run native unit tests
.\gradlew.bat testDebugUnitTest

# Assemble debug APK
.\gradlew.bat assembleDebug
```
The compiled debug APK will be generated at: `android/app/build/outputs/apk/debug/app-debug.apk`.  
Install on an attached Android device or emulator via `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔄 Real-Time Mobile ↔ Web Relay

ContextGuard connects the mobile handset and web supervisor through an authenticated, real-time bidirectional relay:

1. **Pairing:**
   - Tap **"Generate Pairing Code"** in the Android app's **Supervisor** screen (e.g. `ABC123`).
   - In the Supervisor Web Dashboard, click **"Live Mobile Relay"** $\to$ **"Pair Mobile Device"** $\to$ enter `ABC123`.
   - On the phone, tap **"Approve Connection"**. Both interfaces instantly transition to **CONNECTED**.
2. **Zero-Refresh Event Streaming:**
   - Any pre-action intervention triggered on the handset automatically streams to the dashboard within $<10\text{ms}$ over WebSocket.
3. **Supervisor Command Center:**
   - Queue actions (`RUN_SYNTHETIC_DEMO`, `TRIGGER_DIAGNOSTIC_TRACE`) from the web dashboard.
   - The device executes the scenario on-device and streams real-time execution ACKs back to the dashboard.

For complete network connection topologies (Local Emulator loopback `10.0.2.2`, LAN Wi-Fi `192.168.x.x`, or Cloud server), consult [`docs/CONNECTION_SETUP.md`](docs/CONNECTION_SETUP.md).

---

## 🧪 Testing & Verification

ContextGuard maintains automated test coverage across Python, Kotlin, and TypeScript:

```powershell
# 1. Run all Backend, ML, Relay, and Integration Pytest tests (99/99 passed)
python -m pytest tests/ -v

# 2. Run Relay End-to-End integration test specifically
python -m pytest tests/integration/test_relay_e2e.py -v

# 3. Run Android native unit tests
cd android && .\gradlew.bat testDebugUnitTest && cd ..

# 4. Typecheck & build Web Dashboard production bundle
cd supervisor-dashboard && npm run build && cd ..
```

### Current Verification Status:
- **Backend & ML Suite:** 99 / 99 Passed (100%)
- **Android Unit Suite:** 100% Passed
- **Web Dashboard Build:** Clean (0 TypeScript errors)
- **Physical Device:** Software & network protocol 100% verified via automated integration tests; physical hardware verification is transparently documented as **UNVERIFIED** pending connection of a physical handset.

---

## 🤝 Contributing

We welcome contributions from the community! Whether you want to improve model accuracy, add support for new Android accessibility features, or expand the EARB benchmark, please review our contribution guidelines:

- See [`CONTRIBUTING.md`](CONTRIBUTING.md) for branch naming conventions, development setup, code standards, and PR workflows.
- Please adhere to our **Model Failure Safety Rule** and **Zero Raw Persistence** privacy invariants.

---

## 📜 Research & Documentation

- [`docs/RELAY_ARCHITECTURE.md`](docs/RELAY_ARCHITECTURE.md): System architecture and data flow.
- [`docs/RELAY_PROTOCOL.md`](docs/RELAY_PROTOCOL.md): Wire format, REST routes, and WebSocket message schemas.
- [`docs/PAIRING_FLOW.md`](docs/PAIRING_FLOW.md): Sequence diagrams and token security.
- [`docs/CONNECTION_SETUP.md`](docs/CONNECTION_SETUP.md): Local LAN, Android emulator, and firewall setup.
- [`docs/RELAY_SECURITY.md`](docs/RELAY_SECURITY.md): Threat model and cryptographic invariants.
- [`docs/END_TO_END_RELAY_TESTS.md`](docs/END_TO_END_RELAY_TESTS.md): Detailed test execution report.
- [`docs/RELAY_RELEASE_REPORT.md`](docs/RELAY_RELEASE_REPORT.md): Milestone performance and latency analysis.
- [`DEMO_RUNBOOK.md`](DEMO_RUNBOOK.md): Step-by-step viva presentation script.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
