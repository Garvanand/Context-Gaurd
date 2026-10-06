# Evaluation Run Report: B1: Artifact-Only Static Baseline

**System Identifier:** `b1`  
**Category:** `BASELINE`  
**Dataset Split:** `all` (60 samples)  
**Execution Timestamp:** `2026-10-06T08:50:13.284813+00:00`  
**Platform:** `Windows 11`  
**Python Runtime:** `3.12.0`  

---

## 1. Primary Academic Benchmark Metrics

| Metric | Empirical Score | 95% Confidence Interval (Clustered) |
| :--- | :---: | :---: |
| **Multi-Class Accuracy** | **33.33%** | [33.33%, 33.33%] |
| **Macro Precision** | **0.2500** | — |
| **Macro Recall** | **0.2583** | — |
| **Macro F1 Score** | **0.1767** | [0.1250, 0.2183] |
| **STOP Recall** (Safety Critical) | **90.00%** | [75.00%, 100.00%] |
| **ACT False Alarm Rate** | **95.00%** | [85.00%, 100.00%] |
| **Expected Calibration Error (ECE)** | **0.5667** | — |

---

## 2. Per-Class F1 Breakdown

| Safety Intervention | Ground Truth Count | Per-Class F1 Score |
| :--- | :---: | :---: |
| **ACT** (Benign Routine Task) | 20 | `0.0870` |
| **ASK** (Clarification / Ambiguity) | 8 | `0.0000` |
| **WARN** (Cautionary Hazard) | 12 | `0.1333` |
| **STOP** (High-Consequence Hazard) | 20 | `0.4865` |

---

## 3. Empirical Confusion Matrix

| Ground Truth \ Prediction | ACT | ASK | WARN | STOP | Total |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **ACT** | **1** | 0 | 1 | 18 | 20 |
| **ASK** | 0 | **0** | 0 | 8 | 8 |
| **WARN** | 1 | 0 | **1** | 10 | 12 |
| **STOP** | 1 | 0 | 1 | **18** | 20 |

---

## 4. Latency Distribution (Wall-Clock ms)

- **Mean Latency:** `0.15 ms`
- **Median Latency:** `0.01 ms`
- **P90 Latency:** `0.67 ms`
- **P95 Latency:** `0.94 ms`
- **P99 Latency:** `1.37 ms`
- **Min / Max:** `0.01 ms` / `1.81 ms`
- **Standard Deviation:** `0.36 ms`

---

## 5. Artifact Verification & Integrity

- **Configuration:** [`configuration.json`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\b1\configuration.json)
- **Normalized Predictions:** [`predictions.jsonl`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\b1\predictions.jsonl)
- **Metrics JSON:** [`metrics.json`](file://C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd\results\b1\metrics.json)
- **Total Valid Pairs:** `60`
- **Failures / Anomalies:** `0` (0.0%)
