# Evaluation Run Report: A1: No Intent (Intent-Ablated)

**System Identifier:** `a1`  
**Category:** `ABLATION`  
**Dataset Split:** `all` (60 samples)  
**Execution Timestamp:** `2026-10-06T08:50:28.410214+00:00`  
**Platform:** `Windows 11`  
**Python Runtime:** `3.12.0`  

---

## 1. Primary Academic Benchmark Metrics

| Metric | Empirical Score | 95% Confidence Interval (Clustered) |
| :--- | :---: | :---: |
| **Multi-Class Accuracy** | **20.00%** | [13.33%, 26.67%] |
| **Macro Precision** | **0.1111** | — |
| **Macro Recall** | **0.2250** | — |
| **Macro F1 Score** | **0.1364** | [0.0914, 0.1732] |
| **STOP Recall** (Safety Critical) | **40.00%** | [20.00%, 60.00%] |
| **ACT False Alarm Rate** | **100.00%** | [100.00%, 100.00%] |
| **Expected Calibration Error (ECE)** | **0.2800** | — |

---

## 2. Per-Class F1 Breakdown

| Safety Intervention | Ground Truth Count | Per-Class F1 Score |
| :--- | :---: | :---: |
| **ACT** (Benign Routine Task) | 20 | `0.0000` |
| **ASK** (Clarification / Ambiguity) | 8 | `0.1818` |
| **WARN** (Cautionary Hazard) | 12 | `0.0000` |
| **STOP** (High-Consequence Hazard) | 20 | `0.3636` |

---

## 3. Empirical Confusion Matrix

| Ground Truth \ Prediction | ACT | ASK | WARN | STOP | Total |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **ACT** | **0** | 12 | 0 | 8 | 20 |
| **ASK** | 0 | **4** | 0 | 4 | 8 |
| **WARN** | 0 | 8 | **0** | 4 | 12 |
| **STOP** | 0 | 12 | 0 | **8** | 20 |

---

## 4. Latency Distribution (Wall-Clock ms)

- **Mean Latency:** `0.30 ms`
- **Median Latency:** `0.13 ms`
- **P90 Latency:** `1.00 ms`
- **P95 Latency:** `1.21 ms`
- **P99 Latency:** `1.66 ms`
- **Min / Max:** `0.08 ms` / `2.07 ms`
- **Standard Deviation:** `0.41 ms`

---

## 5. Artifact Verification & Integrity

- **Configuration:** [`configuration.json`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\ablations\a1\configuration.json)
- **Normalized Predictions:** [`predictions.jsonl`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\ablations\a1\predictions.jsonl)
- **Metrics JSON:** [`metrics.json`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\ablations\a1\metrics.json)
- **Total Valid Pairs:** `60`
- **Failures / Anomalies:** `0` (0.0%)
