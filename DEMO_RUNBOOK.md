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
