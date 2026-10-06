# Evaluation Run Report: A4: Adaptive Policy (Full ContextGuard Policy)

**System Identifier:** `a4`  
**Category:** `ABLATION`  
**Dataset Split:** `all` (60 samples)  
**Execution Timestamp:** `2026-10-06T08:50:28.917583+00:00`  
**Platform:** `Windows 11`  
**Python Runtime:** `3.12.0`  

---

## 1. Primary Academic Benchmark Metrics

| Metric | Empirical Score | 95% Confidence Interval (Clustered) |
| :--- | :---: | :---: |
| **Multi-Class Accuracy** | **68.33%** | [60.00%, 76.67%] |
| **Macro Precision** | **0.5729** | — |
| **Macro Recall** | **0.5667** | — |
| **Macro F1 Score** | **0.5455** | [0.4271, 0.6621] |
| **STOP Recall** (Safety Critical) | **95.00%** | [85.00%, 100.00%] |
| **ACT False Alarm Rate** | **10.00%** | [0.00%, 25.00%] |
| **Expected Calibration Error (ECE)** | **0.1942** | — |

---

## 2. Per-Class F1 Breakdown

| Safety Intervention | Ground Truth Count | Per-Class F1 Score |
| :--- | :---: | :---: |
| **ACT** (Benign Routine Task) | 20 | `0.8182` |
| **ASK** (Clarification / Ambiguity) | 8 | `0.2500` |
| **WARN** (Cautionary Hazard) | 12 | `0.2500` |
| **STOP** (High-Consequence Hazard) | 20 | `0.8636` |

---

## 3. Empirical Confusion Matrix

| Ground Truth \ Prediction | ACT | ASK | WARN | STOP | Total |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **ACT** | **18** | 1 | 1 | 0 | 20 |
| **ASK** | 2 | **2** | 1 | 3 | 8 |
| **WARN** | 4 | 4 | **2** | 2 | 12 |
| **STOP** | 0 | 1 | 0 | **19** | 20 |

---

## 4. Latency Distribution (Wall-Clock ms)

- **Mean Latency:** `0.29 ms`
- **Median Latency:** `0.13 ms`
- **P90 Latency:** `0.92 ms`
- **P95 Latency:** `0.95 ms`
- **P99 Latency:** `1.51 ms`
- **Min / Max:** `0.09 ms` / `1.66 ms`
- **Standard Deviation:** `0.35 ms`

---

## 5. Artifact Verification & Integrity

- **Configuration:** [`configuration.json`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\ablations\a4\configuration.json)
- **Normalized Predictions:** [`predictions.jsonl`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\ablations\a4\predictions.jsonl)
- **Metrics JSON:** [`metrics.json`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\ablations\a4\metrics.json)
- **Total Valid Pairs:** `60`
- **Failures / Anomalies:** `0` (0.0%)
