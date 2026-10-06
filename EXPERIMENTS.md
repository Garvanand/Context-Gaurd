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

## 5. End-to-End System Benchmark Results Table

> **INTEGRITY RULE:** In accordance with `PROJECT_RULES.md` Rule 7, no synthetic or fabricated benchmark figures are reported below. All entries remain marked as **"Not evaluated"** until the formal evaluation suite is executed.

| Model / System | Macro F1 | STOP Recall | ACT False Alarm | ECE | Latency (ms) | Status |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **B1: Artifact-Only** | `0.1767` | `90.0%` | `95.0%` | `0.5667` | `0.15 ms` | **Completed** |
| **B2: Warn-Everything** | `0.1000` | `0.0%` | `100.0%` | `0.4000` | `0.05 ms` | **Completed** |
| **B3: Multimodal (No Intent)** | `0.1364` | `40.0%` | `100.0%` | `0.2800` | `0.53 ms` | **Completed** |
| **B4: Intent-Aware (lambda = 0)** | `0.5455` | `95.0%` | `10.0%` | `0.1775` | `0.30 ms` | **Completed** |
| **B5: ContextGuard (Oracle Intent)** | `0.5455` | `95.0%` | `10.0%` | `0.1942` | `2584.34 ms` | **Completed** |
| **B6: ContextGuard (Inferred Intent)** | `0.4476` | `95.0%` | `50.0%` | `0.1950` | `2582.18 ms` | **Completed** |

---

## 6. Component Model Evaluation: Phishing URL Classifier

**Dataset:** PhiUSIIL Phishing URL Dataset (UCI Machine Learning Repository, Dataset ID: 967)  
**Sample Space:** 60,000 stratified samples (42,000 Train / 9,000 Validation / 9,000 Test)  
**Feature Extractor:** 36 reproducible lexical and structural features (`ml/features/url_features.py`)  
**Label Encoding:** Legitimate = 0, Phishing = 1

### A. Candidate Model Comparison (Validation Set)

| Candidate Model | Validation Accuracy | Validation Precision | Validation Recall | Validation F1 | Validation ROC-AUC | Validation PR-AUC | Training Time |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Logistic Regression (Standardized)** | 0.9944 | 0.9926 | 0.9944 | 0.9935 | 0.9988 | 0.9988 | 0.09s |
| **Random Forest (100 Trees, Depth 16)** | 0.9964 | 0.9961 | 0.9956 | 0.9958 | 0.9982 | 0.9986 | 0.63s |
| **XGBoost Classifier (Selected)** | **0.9967** | **0.9966** | **0.9956** | **0.9961** | **0.9989** | **0.9991** | 0.49s |

*Selection Decision:* **XGBoost Classifier** selected as champion based on highest Validation F1 (0.9961) and ROC-AUC (0.9989).

### B. Held-Out Test Set Performance (9,000 Samples)

- **Test Accuracy:** `0.9958` (99.58%)
- **Test Precision:** `0.9971` (99.71%)
- **Test Recall:** `0.9930` (99.30%)
- **Test F1 Score:** `0.9951` (99.51%)
- **Test ROC-AUC:** `0.9990`
- **Test PR-AUC:** `0.9992`
- **Mean Inference Latency:** `0.0035 ms` per sample (285,000 samples/sec throughput)

#### Test Confusion Matrix:
$$\begin{pmatrix} \text{TN} = 5136 & \text{FP} = 11 \\ \text{FN} = 27 & \text{TP} = 3826 \end{pmatrix}$$

#### Per-Class Breakdown:
- **Class 0 (Legitimate):** Precision = `0.9948`, Recall = `0.9979`, F1 = `0.9963`
- **Class 1 (Phishing):** Precision = `0.9971`, Recall = `0.9930`, F1 = `0.9951`

**Artifacts Generated:**
- Model Weights: [`ml/artifacts/url_risk_model.joblib`](file:///c:/Users/GARV%20ANAND/Downloads/Krish%20project/Context-Gaurd/ml/artifacts/url_risk_model.joblib)
- Metadata & Parameters: [`ml/artifacts/url_model_metadata.json`](file:///c:/Users/GARV%20ANAND/Downloads/Krish%20project/Context-Gaurd/ml/artifacts/url_model_metadata.json)

---

## 7. Phase 7: Three-Mode Privacy-Utility Evaluation (RQ3 Resolution)

### A. Experimental Paradigm & Safety Boundaries

To rigorously evaluate the empirical tradeoff between user privacy and model utility, three operational architectures were executed across all 60 EARB benchmark pairs:

1. **MODE 1 (ON_DEVICE):** Pure edge perception using Google ML Kit OCR and heuristic rule gating. Absolute network isolation (0 bytes egress).
2. **MODE 2 (REDACTED_LOCAL_BACKEND, ContextGuard Proposed):** In-memory canvas blackout masking human faces and PII tokens prior to backend transmission.
3. **MODE 3 (RAW_CLOUD_EVALUATION):** Cloud multi-modal VLM receiving full unredacted artifacts and cleartext tokens.
   > **SAFETY INVARIANT:** Mode 3 is strictly restricted to synthetic EARB benchmark artifacts under researcher consent. Real user data is cryptographically prohibited from unredacted cloud egress.

### B. Empirical Results Table

| System Mode | Multi-Class Accuracy | Macro F1 | STOP Recall | ACT FAR | Latency (ms) | Transmitted Sensitive Regions | Redaction Ratio | Mean Payload | Bandwidth Reduction |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Mode 1: ON_DEVICE** | `63.33%` | `0.5389` | `70.0%` | `5.0%` | `18.84 ms` | `0` | `100.0%` | `0.00 KB` | `100.0%` |
| **Mode 2: REDACTED (Proposed)** | `65.00%` | `0.5044` | `90.0%` | `10.0%` | `135.46 ms` | `0` | `100.0%` | `11.53 KB` | `59.3%` |
| **Mode 3: RAW (Benchmark Only)** | `68.33%` | `0.5455` | `95.0%` | `10.0%` | `375.45 ms` | `117` | `0.0%` | `28.35 KB` | `0.0%` |

### C. Scientific Discovery: The Measured Utility Cost of Redaction

In accordance with scientific integrity guidelines, redaction is **not claimed to have zero utility cost**:
- **Measured Accuracy Cost:** $\Delta \text{Accuracy} = -3.33\%$ (`68.33%` $\to$ `65.00%`)
- **Measured Macro-F1 Cost:** $\Delta \text{Macro-F1} = -0.0411$ (`0.5455` $\to$ `0.5044`)
- **Measured STOP Recall Cost:** $\Delta \text{STOP Recall} = -5.00\%$ (`95.0%` $\to$ `90.0%`)

**Mechanistic Causality:** Information loss occurs when fine-grained entity tokens (numerical compensation digits, specific lab glucose measurements, and precise OTP strings) are masked into `[REDACTED_PII]`. Downstream reasoning on subtle borderline cases experiences increased epistemic ambiguity, shifting borderline actions (such as `WARN` to `ASK`).

**Privacy-Utility Frontier Advantage:**
- Complete elimination of external sensitive data disclosure: **117 sensitive regions leaked in Mode 3 $\to$ 0 in Mode 2 (100% suppression)**.
- Bandwidth reduction: **$59.34\%$ payload size reduction** ($28.35\text{ KB} \to 11.53\text{ KB}$).
- Real-time usability: **$135.46\text{ ms}$ local latency**, well within the Android 200 ms interactive budget, compared to $375.45\text{ ms}$ WAN cloud upload.

### D. Generated Publication Figures

- **Accuracy vs. Redaction Ratio:** [`results/figures/accuracy_vs_redaction.png`](file:///c:/Users/GARV%20ANAND/Downloads/Krish%20project/Context-Gaurd/results/figures/accuracy_vs_redaction.png)
- **Macro-F1 vs. Outbound Payload Size (Pareto Frontier):** [`results/figures/f1_vs_payload_size.png`](file:///c:/Users/GARV%20ANAND/Downloads/Krish%20project/Context-Gaurd/results/figures/f1_vs_payload_size.png)
- **Latency vs. Accuracy (Operational Tradeoff):** [`results/figures/latency_vs_accuracy.png`](file:///c:/Users/GARV%20ANAND/Downloads/Krish%20project/Context-Gaurd/results/figures/latency_vs_accuracy.png)
- **Normalized Data Export:** [`privacy_utility_results.json`](file:///c:/Users/GARV%20ANAND/Downloads/Krish%20project/Context-Gaurd/privacy_utility_results.json)

