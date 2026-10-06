# Evaluation Run Report: A5: Warn Everything (Conservative Baseline)

**System Identifier:** `a5`  
**Category:** `ABLATION`  
**Dataset Split:** `all` (60 samples)  
**Execution Timestamp:** `2026-10-06T08:50:29.038770+00:00`  
**Platform:** `Windows 11`  
**Python Runtime:** `3.12.0`  

---

## 1. Primary Academic Benchmark Metrics

| Metric | Empirical Score | 95% Confidence Interval (Clustered) |
| :--- | :---: | :---: |
| **Multi-Class Accuracy** | **20.00%** | [13.33%, 26.67%] |
| **Macro Precision** | **0.0500** | — |
| **Macro Recall** | **0.2500** | — |
| **Macro F1 Score** | **0.0833** | [0.0588, 0.1053] |
| **STOP Recall** (Safety Critical) | **0.00%** | [0.00%, 0.00%] |
| **ACT False Alarm Rate** | **100.00%** | [100.00%, 100.00%] |
| **Expected Calibration Error (ECE)** | **0.3000** | — |

---

## 2. Per-Class F1 Breakdown

| Safety Intervention | Ground Truth Count | Per-Class F1 Score |
| :--- | :---: | :---: |
| **ACT** (Benign Routine Task) | 20 | `0.0000` |
| **ASK** (Clarification / Ambiguity) | 8 | `0.0000` |
| **WARN** (Cautionary Hazard) | 12 | `0.3333` |
| **STOP** (High-Consequence Hazard) | 20 | `0.0000` |

---

## 3. Empirical Confusion Matrix

| Ground Truth \ Prediction | ACT | ASK | WARN | STOP | Total |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **ACT** | **0** | 0 | 20 | 0 | 20 |
| **ASK** | 0 | **0** | 8 | 0 | 8 |
| **WARN** | 0 | 0 | **12** | 0 | 12 |
| **STOP** | 0 | 0 | 20 | **0** | 20 |

---

## 4. Latency Distribution (Wall-Clock ms)

- **Mean Latency:** `0.00 ms`
- **Median Latency:** `0.00 ms`
- **P90 Latency:** `0.00 ms`
- **P95 Latency:** `0.00 ms`
- **P99 Latency:** `0.00 ms`
- **Min / Max:** `0.00 ms` / `0.00 ms`
- **Standard Deviation:** `0.00 ms`

---

## 5. Artifact Verification & Integrity

- **Configuration:** [`configuration.json`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\ablations\a5\configuration.json)
- **Normalized Predictions:** [`predictions.jsonl`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\ablations\a5\predictions.jsonl)
- **Metrics JSON:** [`metrics.json`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\ablations\a5\metrics.json)
- **Total Valid Pairs:** `60`
- **Failures / Anomalies:** `0` (0.0%)
