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

