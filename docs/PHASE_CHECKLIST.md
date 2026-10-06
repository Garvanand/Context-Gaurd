# ContextGuard Implementation Phase Checklist

This checklist tracks the end-to-end execution of the ContextGuard research capstone across all phases.

---

## Phase 0: Environment Audit & Architecture Foundation
- [x] Comprehensive environment audit (OS, RAM, Disk, Java, Android SDK, Gradle, Python, Node, Git, Ollama, Internet).
- [x] Monorepo directory structure established (`android`, `backend`, `ml`, `benchmark`, `dashboard`, `docs`, `scripts`, `artifacts`, `tests`).
- [x] Repository configuration: `.gitignore`, `.env.example`.
- [x] Foundation documentation: `PROJECT_RULES.md`, `PROJECT_STATUS.md`, `ARCHITECTURE.md`, `THREAT_MODEL.md`, `PRIVACY.md`, `DEMO_RUNBOOK.md`, `EXPERIMENTS.md`.
- [x] Phase checklist and verification protocol established.

---

## Phase 1: Machine Learning & Feature Engineering
- [ ] Implement URL feature extraction pipeline (`ml/features/url_features.py`) with 35+ lexical and structural features.
- [ ] Acquire PhiUSIIL Phishing URL dataset from UCI Machine Learning repository (`ml/data/acquire_phiusiil.py`).
- [ ] Build preprocessing, feature derivation, and train/test split pipeline (`ml/data/preprocess.py`).
- [ ] Train XGBoost phishing classifier with hyperparameter tuning and cross-validation (`ml/training/train_phishing.py`).
- [ ] Calibrate probability estimates using Platt scaling / isotonic regression.
- [ ] Evaluate model on held-out test split: Accuracy, ROC-AUC, PR-AUC, F1, Latency (`ml/evaluation/eval_phishing.py`).
- [ ] Export production model artifact (`artifacts/models/xgboost_phishing_v1.json`) and feature metadata (`artifacts/models/url_features_v1.json`).
- [ ] Unit tests for feature extraction and model inference consistency (`ml/tests/test_features.py`, `ml/tests/test_model.py`).

---

## Phase 2: Everyday Action Risk Benchmark (EARB)
- [ ] Generate 20 high-fidelity, synthetic base digital artifacts across 4 categories:
  - Financial (5 base artifacts: bank statement, invoice, payment QR, credit card receipt, tax summary).
  - Digital Security (5 base artifacts: password reset email, 2FA backup codes, SSH private key header, session bearer token, suspicious login prompt).
  - Privacy Disclosure (5 base artifacts: medical summary, national ID mockup, flight booking itinerary, personal selfie, employment offer letter).
  - Communication (5 base artifacts: confidential Slack DM, internal strategy memo, NDA draft, customer dispute email, executive calendar screenshot).
- [ ] Ensure the Central Demo base artifact (`synthetic_bank_statement.png`) is created with photorealistic typography and layout.
- [ ] Construct the 60 action-conditioned pairs adhering to the full EARB schema.
- [ ] Include negative controls, ambiguous pairs, and calibrated severity/reversibility values.
- [ ] Validate EARB JSON schema with Pydantic validator (`benchmark/earb/validate_benchmark.py`).

---

## 3. Phase 3: Backend & Deterministic Policy Engine
- [ ] Implement core domain schemas (`backend/app/models/schemas.py`).
- [ ] Implement deterministic policy engine (`backend/app/policy/engine.py`):
  - Risk formula: $\rho = s \times (1 + \lambda \times r)$.
  - Threshold gating: $\{ \text{ACT}, \text{ASK}, \text{WARN}, \text{STOP} \}$.
  - Strict Model Failure Safety Rule (never silently emit `ACT`).
- [ ] Implement inference orchestrator service (`backend/app/services/orchestrator.py`):
  - Integrates ML Kit feature ingestion.
  - Integrates XGBoost URL classifier service.
  - Integrates Qwen2.5-VL-3B VLM reasoning client (Ollama HTTP / fallback).
- [ ] Build REST API routes (`backend/app/api/v1/`):
  - `POST /api/v1/analyze`: Full action-conditioned multimodal risk assessment.
  - `POST /api/v1/policy/evaluate`: Pure policy simulation endpoint.
  - `GET /api/v1/health`: Subsystem health (XGBoost, VLM, Policy Engine).
  - `GET /api/v1/audit/logs`: Privacy-safe request log and SHA-256 telemetry.
- [ ] Comprehensive unit and integration test suite (`tests/backend/test_api.py`, `tests/backend/test_policy.py`).

---

## 4. Phase 4: Android Application (Kotlin + Jetpack Compose)
- [ ] Initialize Android project with Gradle 8.3 and AGP 8.x target API 34.
- [ ] Implement edge perception modules:
  - ML Kit Text Recognition (OCR).
  - ML Kit Face Detection.
  - Local regex / pattern sensitive data detector (PII).
- [ ] Implement local redaction engine (in-memory canvas pixel masking + text masking).
- [ ] Implement network client with offline fallback:
  - `OFFLINE` mode (zero network bytes, local heuristics).
  - `REDACTED_LOCAL_BACKEND` mode (sends masked bitmap + metadata).
- [ ] Implement Jetpack Compose UI:
  - Welcome & Setup Screen.
  - Home / Analyze Screen (Image picker, Camera capture, Sharesheet receiver).
  - Artifact Preview & Local Redaction Toggle.
  - Action & Destination Selection (Save privately, Direct message, Public post).
  - Analysis Progress with real-time stage indicators.
  - Result Screen with Intervention Banner (ACT / ASK / WARN / STOP).
  - Evidence Card & Decision Trace Breakdown.
  - Supervisor / Viva Mode (real-time telemetry and score calculation).
  - Network Audit Log Screen.
- [ ] Android unit tests for domain and redaction logic (`android/app/src/test/`).

---

## 5. Phase 5: Empirical Benchmark & Ablation Study
- [ ] Implement baseline evaluators:
  - B1: Artifact-only baseline.
  - B2: Warn-everything baseline.
  - B3: Multimodal context-free baseline.
  - B4: Intent-aware fixed threshold baseline ($\lambda = 0$).
  - B5: Full ContextGuard system.
- [ ] Execute automated benchmark evaluation across all 60 EARB pairs (`scripts/run_evaluations.py`).
- [ ] Compute empirical metrics: Macro F1, STOP Recall, ACT False Alarm Rate, ECE, Latency.
- [ ] Generate confusion matrices and calibration plots.
- [ ] Update `EXPERIMENTS.md` and `PROJECT_STATUS.md` with true empirical numbers.

---

## 6. Phase 6: System Integration, End-to-End Testing & Polish
- [ ] End-to-end integration test connecting Android client, FastAPI backend, and ML models.
- [ ] Central Demo verification (Bank Statement -> Save privately = ACT, Send to unknown = WARN/ASK, Post publicly = STOP).
- [ ] Stress and fault-injection testing (server outage, malformed JSON, network timeout).
- [ ] Final viva presentation assets and documentation sign-off.
