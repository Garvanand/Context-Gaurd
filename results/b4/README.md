# Evaluation Run Report: B4: Multimodal + Intent + Fixed Threshold

**System Identifier:** `b4`  
**Category:** `BASELINE`  
**Dataset Split:** `all` (60 samples)  
**Execution Timestamp:** `2026-10-06T08:50:13.639769+00:00`  
**Platform:** `Windows 11`  
**Python Runtime:** `3.12.0`  

---

## 1. Primary Academic Benchmark Metrics

| Metric | Empirical Score | 95% Confidence Interval (Clustered) |
| :--- | :---: | :---: |
| **Multi-Class Accuracy** | **71.67%** | [63.33%, 80.00%] |
| **Macro Precision** | **0.5104** | — |
| **Macro Recall** | **0.5875** | — |
| **Macro F1 Score** | **0.5455** | [0.4578, 0.6203] |
| **STOP Recall** (Safety Critical) | **95.00%** | [85.00%, 100.00%] |
| **ACT False Alarm Rate** | **10.00%** | [0.00%, 25.00%] |
| **Expected Calibration Error (ECE)** | **0.1775** | — |

---

## 2. Per-Class F1 Breakdown

| Safety Intervention | Ground Truth Count | Per-Class F1 Score |
| :--- | :---: | :---: |
| **ACT** (Benign Routine Task) | 20 | `0.8182` |
| **ASK** (Clarification / Ambiguity) | 8 | `0.0000` |
| **WARN** (Cautionary Hazard) | 12 | `0.5000` |
| **STOP** (High-Consequence Hazard) | 20 | `0.8636` |

---

## 3. Empirical Confusion Matrix

| Ground Truth \ Prediction | ACT | ASK | WARN | STOP | Total |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **ACT** | **18** | 0 | 2 | 0 | 20 |
| **ASK** | 2 | **0** | 3 | 3 | 8 |
| **WARN** | 4 | 0 | **6** | 2 | 12 |
| **STOP** | 0 | 0 | 1 | **19** | 20 |

---

## 4. Latency Distribution (Wall-Clock ms)

- **Mean Latency:** `0.30 ms`
- **Median Latency:** `0.16 ms`
- **P90 Latency:** `0.97 ms`
- **P95 Latency:** `1.09 ms`
- **P99 Latency:** `1.43 ms`
- **Min / Max:** `0.09 ms` / `1.84 ms`
- **Standard Deviation:** `0.36 ms`

---

## 5. Artifact Verification & Integrity

- **Configuration:** [`configuration.json`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\b4\configuration.json)
- **Normalized Predictions:** [`predictions.jsonl`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\b4\predictions.jsonl)
- **Metrics JSON:** [`metrics.json`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\b4\metrics.json)
- **Total Valid Pairs:** `60`
- **Failures / Anomalies:** `0` (0.0%)
