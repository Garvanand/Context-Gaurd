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
| **B1: Artifact-Only** | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | Pending run |
| **B2: Warn-Everything** | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | Pending run |
| **B3: Multimodal (No Intent)** | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | Pending run |
| **B4: Intent-Aware ($\lambda = 0$)** | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | Pending run |
| **B5: ContextGuard (Full)** | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | *Not evaluated* | Pending run |

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
