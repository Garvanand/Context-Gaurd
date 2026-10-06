# ContextGuard Research & Experimental Evaluation Protocol

## 1. Research Questions & Hypotheses

- **RQ1 (Action-Conditioning):** Does conditioning safety interventions on the intended action and recipient significantly reduce false-alarm rates compared to static artifact-only baselines?
  - *Hypothesis:* Artifact-only detectors over-intervene on benign tasks (e.g. archiving personal documents) and fail to distinguish benign internal storage from dangerous public disclosure.
- **RQ2 (Deterministic Policy vs. Direct Generative Prompting):** Does mathematical policy gating ($\rho = s \times (1 + \lambda \times r)$) provide superior calibration and safety guarantees compared to unconstrained LLM/VLM textual recommendations?
  - *Hypothesis:* Generative LLMs suffer from prompt-drift and hallucination under edge cases; mathematical policy enforcement guarantees zero silent `ACT` failures.
- **RQ3 (Privacy-Utility Tradeoff):** How does pre-transmission on-device redaction (masking faces and PII tokens) affect downstream multimodal reasoning accuracy?
  - *Hypothesis:* Coarse redaction preserves semantic hazard context (document type, layout, recipient hazard) while achieving zero raw PII disclosure over the wire.

---

## 2. Experimental Baselines & Systems Under Test

| System ID | Name | Description | Multimodal? | Action-Aware? | Policy Engine |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **B1** | Artifact-Only Static Baseline | Evaluates artifact text/image without any context, intended action, or recipient information. | Yes | No | Fixed Classifier |
| **B2** | Warn-Everything Baseline | Trivial conservative baseline that emits `WARN` on every non-blank artifact. | No | No | Heuristic |
| **B3** | Multimodal Context-Free | Multimodal VLM provided with image and text, but prompted without explicit action or destination tokens. | Yes | No | Direct Generation |
| **B4** | Intent-Aware Fixed Threshold | Multimodal VLM with intent, but using a naive single risk score without reversibility penalty ($\lambda = 0$). | Yes | Yes | Fixed Cutoffs |
| **B5** | **ContextGuard (Full Proposed System)** | Multi-tier perception (ML Kit + XGBoost + VLM) with action-conditioned deterministic policy ($\rho = s \times (1 + \lambda \times r)$). | **Yes** | **Yes** | **Deterministic Policy** |

---

## 3. Evaluation Metrics

1. **Overall Performance:**
   - Multi-class Accuracy
   - Macro Precision, Macro Recall, Macro F1
2. **Safety-Critical Focus:**
   - **STOP Recall:** $\frac{\text{True STOP}}{\text{True STOP} + \text{False Negative STOP}}$ (Target: $\ge 98\%$, zero tolerated misses on catastrophic risk)
   - **ACT False-Alarm Rate:** Benign actions unnecessarily flagged as `STOP` or `WARN` (Target: $\le 5\%$)
3. **Uncertainty & Calibration:**
   - Expected Calibration Error (ECE)
   - Accuracy vs. Confidence calibration curves
4. **Efficiency:**
   - On-device edge perception latency (ms)
   - Backend policy inference latency (ms)
   - End-to-end roundtrip latency

---

## 4. Benchmark Dataset: Everyday Action Risk Benchmark (EARB)

- **Total Test Pairs:** 60 curated artifact-action pairs.
- **Base Artifacts:** 20 distinct synthetic artifacts.
- **Categories:**
  1. **Financial:** Bank statements, invoices, payment QR codes, credit card forms, tax summaries.
  2. **Digital Security:** Password recovery prompts, 2FA backup codes, SSH keys, session tokens, suspicious login alerts.
  3. **Privacy Disclosure:** Medical discharge papers, personal ID cards, private travel itineraries, personal selfies, employment contracts.
  4. **Communication:** Private chat logs, confidential team emails, legal non-disclosure drafts, customer support tickets.
- **Action Triad:** Each base artifact is paired with 3 distinct intended actions:
  - *Action A (Low Exposure):* Local save / personal vault.
  - *Action B (Moderate Exposure):* Internal team share / known recipient.
  - *Action C (High Exposure):* Public upload / untrusted recipient / public post.

---

## 5. Experimental Results Table

> **INTEGRITY RULE:** In accordance with `PROJECT_RULES.md` Rule 7, no synthetic or fabricated benchmark figures are reported below. All entries remain marked as **"Not evaluated"** until the formal evaluation suite is executed.

| Model / System | Macro F1 | STOP Recall | ACT False Alarm | ECE | Latency (ms) | Status |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **B1: Artifact-Only** | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | Pending run |
| **B2: Warn-Everything** | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | Pending run |
| **B3: Multimodal (No Intent)** | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | Pending run |
| **B4: Intent-Aware ($\lambda = 0$)** | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | Pending run |
| **B5: ContextGuard (Full)** | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | Pending run |
