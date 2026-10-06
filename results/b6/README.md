# Evaluation Run Report: B6: ContextGuard (Inferred Intent)

**System Identifier:** `b6`  
**Category:** `BASELINE`  
**Dataset Split:** `all` (60 samples)  
**Execution Timestamp:** `2026-10-06T13:54:47.053501+00:00`  
**Platform:** `Windows 11`  
**Python Runtime:** `3.12.0`  

---

## 1. Primary Academic Benchmark Metrics

| Metric | Empirical Score | 95% Confidence Interval (Clustered) |
| :--- | :---: | :---: |
| **Multi-Class Accuracy** | **55.00%** | [43.33%, 66.67%] |
| **Macro Precision** | **0.4820** | — |
| **Macro Recall** | **0.4667** | — |
| **Macro F1 Score** | **0.4476** | [0.2978, 0.5969] |
| **STOP Recall** (Safety Critical) | **95.00%** | [85.00%, 100.00%] |
| **ACT False Alarm Rate** | **50.00%** | [30.00%, 70.00%] |
| **Expected Calibration Error (ECE)** | **0.1950** | — |

---

## 2. Per-Class F1 Breakdown

| Safety Intervention | Ground Truth Count | Per-Class F1 Score |
| :--- | :---: | :---: |
| **ACT** (Benign Routine Task) | 20 | `0.5714` |
| **ASK** (Clarification / Ambiguity) | 8 | `0.2667` |
| **WARN** (Cautionary Hazard) | 12 | `0.2353` |
| **STOP** (High-Consequence Hazard) | 20 | `0.7170` |

---

## 3. Empirical Confusion Matrix

| Ground Truth \ Prediction | ACT | ASK | WARN | STOP | Total |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **ACT** | **10** | 0 | 2 | 8 | 20 |
| **ASK** | 1 | **2** | 1 | 4 | 8 |
| **WARN** | 4 | 4 | **2** | 2 | 12 |
| **STOP** | 0 | 1 | 0 | **19** | 20 |

---

## 4. Latency Distribution (Wall-Clock ms)

- **Mean Latency:** `2582.18 ms`
- **Median Latency:** `2578.84 ms`
- **P90 Latency:** `2654.37 ms`
- **P95 Latency:** `2677.14 ms`
- **P99 Latency:** `2733.27 ms`
- **Min / Max:** `2477.70 ms` / `2748.42 ms`
- **Standard Deviation:** `58.87 ms`

---

## 5. Artifact Verification & Integrity

- **Configuration:** [`configuration.json`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\b6\configuration.json)
- **Normalized Predictions:** [`predictions.jsonl`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\b6\predictions.jsonl)
- **Metrics JSON:** [`metrics.json`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\b6\metrics.json)
- **Total Valid Pairs:** `60`
- **Failures / Anomalies:** `0` (0.0%)
