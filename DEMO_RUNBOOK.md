# ContextGuard Viva & Demonstration Runbook

This runbook guides examiners, researchers, and engineers through the complete live demonstration of ContextGuard during project presentations and oral viva examinations.

---

## 1. Quick Verification Checklist Before Demo

1. **Backend Service Active:**
   ```powershell
   cd backend
   python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
   ```
2. **Verify Health Endpoint:**
   ```powershell
   curl http://localhost:8000/api/v1/health
   # Expected response: {"status": "healthy", "xgboost_url_model": "loaded", "policy_engine": "ready"}
   ```
3. **Android Client Launched:**
   - Run on emulator (`Android API 34`) or connected physical device.
   - Verify backend connection status indicator in top bar displays green dot ("Connected").

---

## 2. The Core Viva Demo: Action-Conditioned Safety

> **Core Objective:** Prove that artifact risk is fundamentally dependent on the intended action and recipient, not just the pixels or text alone.

### Base Artifact: `synthetic_bank_statement.png`
A realistic, synthetic monthly bank statement containing sample account balances, mock transaction histories, and simulated PII.

```text
┌─────────────────────────────────────────────────────────────┐
│                 CENTRAL DEMO SCENARIO MATRIX                │
├──────────────────┬───────────────────────┬──────────────────┤
│ Action 1         │ Action 2              │ Action 3         │
│ Save Privately   │ Send to Unknown User  │ Post Publicly    │
│ Vault / Drive    │ Telegram / WhatsApp   │ Twitter / Reddit │
├──────────────────┼───────────────────────┼──────────────────┤
│ Outcome: ACT     │ Outcome: WARN / ASK   │ Outcome: STOP    │
│ (Green Banner)   │ (Orange/Yellow Banner)│ (Red Banner)     │
└──────────────────┴───────────────────────┴──────────────────┘
```

### Demonstration Steps:
1. Open ContextGuard on Android and navigate to **Demo Scenarios**.
2. Select **Scenario 1: Personal Bank Statement**.
3. **Run Action 1 (Save to Encrypted Vault):**
   - Intended Action: `Archive Document`
   - Destination: `Personal Google Drive / Encrypted Vault`
   - Observe Result: **ACT (Green)**
   - Rationale: Storing private records in personal storage carries negligible exposure hazard ($\rho \approx 0.05$).
4. **Run Action 2 (Send to Unknown Recipient):**
   - Intended Action: `Direct Message Transfer`
   - Destination: `Unknown / Unverified Contact via Chat App`
   - Observe Result: **WARN / ASK (Orange / Yellow)**
   - Rationale: High hazard of account harvesting; partially reversible; system warns user or prompts for recipient verification ($\rho \approx 0.62$).
5. **Run Action 3 (Post to Public Feed):**
   - Intended Action: `Public Post / Broadcast`
   - Destination: `Social Media Feed (Twitter / Instagram)`
   - Observe Result: **STOP (Red)**
   - Rationale: Irreversible public disclosure of financial account details and identity markers ($\rho = 1.57 \ge 0.65$). Action blocked.

---

## 3. Viva Supervisor Mode (Examiner Deep Dive)

Toggle **Supervisor Mode** in the top navigation bar or Settings.

### Examiner Inspection Panel:
- **Perception Telemetry:**
  - Extracted ML Kit OCR tokens & confidence
  - Face count & bounding box coordinates
  - Detected PII entities (e.g. Account Number: `1234-****-5678`)
- **Redaction Verification:**
  - Real-time side-by-side view showing the raw artifact vs. the masked transmission payload.
- **Model Subsystem Outputs:**
  - URL Classifier: Prediction score, extracted lexical features.
  - VLM Reasoning: Severity ($s$), Irreversibility ($r$), Epistemic Confidence ($c$).
- **Policy Engine Trace:**
  $$\rho = s \times (1 + \lambda \times r) = 0.90 \times (1 + 0.75 \times 1.0) = 1.575$$
  - Threshold rule evaluated: $\rho \ge 0.65 \implies \text{STOP}$.
- **Network Audit:**
  - SHA-256 payload digest, payload size in bytes, and execution latency.

---

## 4. Failure Mode & Robustness Demonstrations

### Test Case A: Backend Disconnected (Graceful Offline Degradation)
1. Terminate the backend server (`Ctrl+C`).
2. Trigger an analysis on Android.
3. Observe:
   - App does not crash.
   - Status badge transitions to "Offline Mode".
   - Local on-device heuristics evaluate the artifact.
   - Unresolved ambiguities safely trigger **ASK**.

### Test Case B: Model Failure Safety Rule
1. Send a corrupted or unparseable payload to the policy engine.
2. Invariant Check: The system **NEVER** silently falls back to `ACT`.
3. System immediately returns **ASK** with a clear explanation: "Inference uncertainty: Policy fell back to safe verification".

---

## 5. Android System Sharesheet Integration & Live Flows

ContextGuard is registered directly into the Android system as a primary share target via `ACTION_SEND` and `ACTION_SEND_MULTIPLE`. It seamlessly intercepts incoming streams, text, and URLs before users complete downstream digital actions.

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                       ANDROID SHARESHEET PIPELINE                           │
├─────────────────────────────────────────────────────────────────────────────┤
│  External App (Gallery, Browser, Files, Chat)                               │
│       │                                                                     │
│       ▼ Intent (ACTION_SEND / ACTION_SEND_MULTIPLE)                         │
│  [Android OS Sharesheet Dialog] ──► Select "ContextGuard"                   │
│       │                                                                     │
│       ▼ content:// URI / EXTRA_TEXT / ClipData                              │
│  SharesheetPayloadResolver                                                  │
│  ├── ContentResolver.openInputStream() (Zero disk persistence / volatile RAM)│
│  ├── ImagePreprocessor: Bounded 1600px subsampling (prevents OOM on 48MP)    │
│  ├── PdfPerceptionRenderer: In-memory page bitmap rasterization             │
│  └── URL Regex Extractor: Isolates destination links from message bodies     │
│       │                                                                     │
│       ▼                                                                     │
│  MainViewModel.processSharesheetPayload() ──► Auto-navigate to AnalyzeScreen│
│  ├── Live Artifact Preview (Side-by-side Before / After Redaction)          │
│  ├── User Selects Intended Action (SAVE, SEND, POST, OPEN, APPROVE)         │
│  ├── 6-Stage Reasoning Pipeline (Context ➔ Intent ➔ Evidence ➔ ... )        │
│  └── Pre-Action Intervention Modal (ACT / WARN / ASK / STOP)                │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.1 Real-World Flow 1: Gallery / Photos ➔ Sharesheet ➔ ContextGuard

1. Open Android **Gallery** or **Google Photos**.
2. Select a sensitive artifact (e.g., `synthetic_bank_statement.png` or `tax_document.pdf`).
3. Tap **Share** ➔ select **ContextGuard**.
4. ContextGuard launches directly into `AnalyzeScreen`:
   - Decodes content stream in volatile memory via `ContentResolver`.
   - Runs on-device ML Kit OCR & sensitive entity detection.
   - Displays artifact preview with the **Redaction Toggle** (`Blackout` or `Gaussian Blur`).
5. Select intended action:
   - Tap **SAVE** ➔ Destination: `Personal Encrypted Drive`.
   - Tap **Analyze Risk**.
   - Result: **ACT (Green)** — risk score $< 0.35$.
6. Repeat with **SEND** to `Unverified Telegram Contact`:
   - Result: **WARN / ASK (Amber/Yellow)** — risk score $\approx 0.62$.
7. Repeat with **POST** to `Public Twitter/X Feed`:
   - Result: **STOP (Red)** — irreversible exposure of financial credentials blocked ($\rho \ge 0.65$).
   - *Key Thesis Invariant: The artifact remains 100% identical across all three runs.*

---

### 5.2 Real-World Flow 2: Browser / Messaging ➔ Share URL ➔ ContextGuard

1. Open **Chrome**, **Firefox**, or **SMS/WhatsApp**.
2. Select or share a suspicious message:
   ```text
   Urgent KYC Update required: http://kyc-update-sbi-portal-verify.support-desk91.net/auth
   ```
3. Tap **Share** ➔ select **ContextGuard**.
4. ContextGuard receives `EXTRA_TEXT` / `ClipData`:
   - `SharesheetPayloadResolver.extractUrl` automatically detects the phishing link.
   - Sets source app to `Browser / Web Client` and default intended action to `OPEN`.
   - Evaluates link against the trained PhiUSIIL XGBoost lexical model.
   - Model detects:
     - IP/hex patterns, multiple subdomains, brand token spoofing (`sbi-portal`), suspicious TLD (`.net`).
     - $P(\text{phishing}) = 0.974$.
5. Policy Engine outputs **STOP (Red banner)**:
   - "High-hazard credential harvesting domain detected. Opening this link exposes authentication tokens."

---

### 5.3 Deep Links & Stream Security Protocol

To maintain maximum privacy and prevent side-channel leaks:
1. **Zero Filesystem Footprint:** The application never writes incoming `content://` streams into persistent application storage or cache directories (`/data/data/...` or external storage).
2. **Volatile RAM Processing:** All Bitmaps and OCR tokens exist strictly in transient memory structures and are garbage collected upon activity destruction.
3. **Bounded Image Preprocessing:** Images shared from modern phone cameras (e.g. 48MP/108MP) are downsampled to a maximum bounding box of $1600 \times 1600$ px via `BitmapFactory.Options.inSampleSize` before decoding, preventing `OutOfMemoryError`.
4. **Sandboxed PDF Rasterization:** Android-native `PdfRenderer` renders at most the first 3 pages into in-memory bitmaps with automatic resource descriptor cleanup (`ParcelFileDescriptor.close()`).

---

### 5.4 Defensive Error Handling Matrix

ContextGuard enforces a strict **Never Crash** design contract on corrupted, malformed, or malicious share intents:

| Edge Case | Sharesheet Handler Behavior | User Experience |
| :--- | :--- | :--- |
| **Missing Stream / Empty Text** | `SharePayload.ErrorPayload` returned | Informative toast/banner; returns to Home safely without crashing. |
| **Inaccessible URI (`SecurityException`)** | Caught in `SharesheetPayloadResolver` | Displays "Inaccessible content URI (permission revoked)"; prompts re-selection. |
| **Unsupported MIME Type** | Sniffs magic bytes (JPEG/PNG/PDF/Text) | If unrecognized, safely treats as text or displays friendly fallback message. |
| **Multiple Attachments (`ACTION_SEND_MULTIPLE`)** | Ingests primary item; displays batch warning | Displays: *"Received N attachments. ContextGuard is analyzing primary attachment."* |
| **Corrupted PDF Document** | Catches `IOException` in `PdfPerceptionRenderer` | Falls back to generic document perception or asks user for alternative format. |
| **Malformed / Null Text** | Null-safe string coercion | Handled as empty note without throwing `NullPointerException`. |
| **Cancelled Share Flow** | `SharePayload.EmptyOrCancelled` | Activity remains calm; no state corruption or unintended background triggers. |

---

### 5.5 CLI & Automated Verification Commands

#### A. Automated Unit Testing
Execute the complete test suite verifying `SharePayload` resolution, URL parsing, and the 3-iteration identical artifact triad:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd android
.\gradlew.bat testDebugUnitTest
```

#### B. Manual Android Sharesheet Verification via ADB

1. **Simulate Browser Shared Phishing URL:**
```bash
adb shell am start -a android.intent.action.SEND \
  -t "text/plain" \
  --es android.intent.extra.TEXT "Urgent: Complete your KYC here http://kyc-update-sbi-portal-verify.support-desk91.net/auth" \
  -n com.contextguard.app.debug/com.contextguard.app.MainActivity
```

2. **Simulate Gallery Shared Bank Statement Image:**
```bash
# Push test image to emulator/device
adb push test_statement.png /sdcard/Download/test_statement.png

# Broadcast SEND intent with content URI
adb shell am start -a android.intent.action.SEND \
  -t "image/png" \
  --eu android.intent.extra.STREAM "content://media/external/images/media/1" \
  -n com.contextguard.app.debug/com.contextguard.app.MainActivity
```

3. **Simulate Documents App Shared PDF:**
```bash
adb shell am start -a android.intent.action.SEND \
  -t "application/pdf" \
  --eu android.intent.extra.STREAM "content://media/external/file/10" \
  -n com.contextguard.app.debug/com.contextguard.app.MainActivity
```

4. **Verify App Robustness Against Malformed Share Intent (Never Crash):**
```bash
adb shell am start -a android.intent.action.SEND \
  -t "application/octet-stream" \
  -n com.contextguard.app.debug/com.contextguard.app.MainActivity
```
Result: ContextGuard launches gracefully, catches the empty stream, informs the user, and remains stable.

---

## 6. End-to-End Mobile Product Modes (Post-Recovery Demonstration)

### Mode A: Normal Application Launch
1. Tap the ContextGuard app icon from the Android system launcher.
2. The application opens directly to the **Real Protection Home** (`HomeScreen`).
3. Notice:
   - **Protection Status:** Clearly displays "Protection active", "Protection paused", or "Setup required".
   - **Satellite Capabilities:** Screen Guard (window tree access), Notification Guard (listener access), On-Device Tree Inference (120 XGBoost trees loaded in RAM).
   - **Monitored Applications:** Displays count of enabled packages (e.g., "6 enabled: Google Chrome, Firefox, WhatsApp, Telegram, Google Messages, Gmail").
   - **Network Privacy Mode:** Clearly shows "Offline Local (Zero egress)" or "Backend: host:port".
   - **Missing Permissions:** Clearly displayed; tapping "Configure protection" opens `ConsentScreen` to guide the user directly to system settings.
   - Returning from Android Settings dynamically updates permission states via the lifecycle resume observer.

### Mode B: Event-Driven Background Protection
1. Grant Accessibility and Notification listener permissions.
2. Select target applications to protect in Settings or Consent Screen.
3. Switch to third-party app (e.g., Chrome or WhatsApp).
4. When a suspicious link or credential form is interacted with, `ScreenGuardAccessibilityService` intercepts the window context.
5. In-memory `UrlTreeInferenceEngine` or `ScamMessageClassifier` evaluates risk in < 0.5ms.
6. If risk is elevated (WARN/STOP/ASK):
   - A floating, compact JIT intervention card appears directly over the third-party application via `TYPE_ACCESSIBILITY_OVERLAY`.
   - ACT is silent.
   - Tapping "Review" launches ContextGuard `ResultScreen` displaying grounded evidence.
   - Tapping "Continue once" dismisses the overlay cleanly without clicking through or submitting the underlying app.
   - Immediate pause/kill-switch in ContextGuard immediately removes the overlay and suspends monitoring.

### Mode C: Explicit Artifact Sharing
1. Share an image, document, URL, or text snippet from any Android application using the system Sharesheet into ContextGuard.
2. ContextGuard resolves the actual content via `SharesheetPayloadResolver` (not just filename).
3. The actual content is opened and local preprocessing begins immediately in volatile RAM.
4. Preview and extracted evidence appear on `AnalyzeScreen`.
5. Prompts the user: "What are you about to do with this?" with tailored action chips (e.g., `SAVE`, `SEND`, `POST`, `SIGN`).
6. **Automatic Analysis:** Tapping an action chip immediately triggers risk analysis without requiring an additional "Analyze Now" button click.
7. Result renders automatically upon inference completion in `ResultScreen`.
8. User can tap "Cancel" at any time to abort and return cleanly to Home.

---

## 7. Real-Time Mobile ↔ Web Relay & Supervisor Command Center Demonstration

This walkthrough demonstrates the real-time bidirectional bridge connecting the Android handset to the Supervisor Web Dashboard.

### Step 1: Start Services & Open Interfaces
1. **Launch FastAPI Relay Backend:**
   ```powershell
   python -m uvicorn backend.app.main:app --host 0.0.0.0 --port 8000
   ```
2. **Launch Supervisor Web Dashboard:**
   ```powershell
   cd supervisor-dashboard
   npm run dev
   ```
   Open `http://localhost:5173` in your browser.
3. **Launch Android App:**
   Open ContextGuard on an Android emulator or device with backend profile configured (`LOCAL_LAN_DEMO` or `10.0.2.2:8000`).

---

### Step 2: Live Device Pairing Handshake
1. **On Android:**
   - Navigate to the **Supervisor** screen.
   - Locate the **"SUPERVISOR WEB RELAY & PAIRING"** card.
   - Tap **"Generate Pairing Code"**.
   - A 6-character code appears (e.g. `ABC123`) with a 5-minute countdown.
2. **On Supervisor Web Dashboard:**
   - Click **"Live Mobile Relay"** in the sidebar navigation.
   - If unpaired, click **"Pair Mobile Device"**.
   - Enter the 6-character code into the modal input and click **"Claim Code"**.
3. **Approve on Device:**
   - The Android handset immediately displays: *"Supervisor Workstation requested connection"*.
   - Tap **"Approve Connection"**.
   - Scoped cryptographic tokens (`devtok_*` and `dshtok_*`) are issued and securely stored.
   - Both phone and web dashboard instantly transition to **CONNECTED** with live heartbeat telemetry.

---

### Step 3: Zero-Refresh Real-Time Event Streaming
1. Ensure **"Enable Telemetry Sync"** toggle is enabled on the Android Supervisor screen.
2. Trigger an analysis on Android (e.g., share an image or run an in-app scenario).
3. **Observe Web Dashboard:**
   - Without refreshing the browser, the **"Real-Time Telemetry Feed"** automatically animates a new incoming event card within ~10ms via WebSocket fan-out.
   - The badge highlights the intervention (`STOP`, `WARN`, `ASK`, `ACT`).
   - Click **"Inspect Event"** to view grounded evidence, correlation ID, and privacy verification (zero raw screenshots stored).

---

### Step 4: Bidirectional Command Center Dispatch
1. In the Web Dashboard's **"Command Center"**:
   - Locate the **"Run Synthetic Demo"** action card.
   - Select the target scenario (e.g., `synthetic_bank_statement_v1`).
   - Click **"Dispatch Command"**.
2. **Observe Execution:**
   - Command state transitions: `QUEUED` $\to$ `DELIVERED` $\to$ `EXECUTING`.
   - The Android client executes the scenario on-device in the background.
   - Mobile sends an execution ACK back to the relay.
   - Dashboard command card dynamically updates to `SUCCEEDED` with the executed intervention (`STOP`) and risk score.

