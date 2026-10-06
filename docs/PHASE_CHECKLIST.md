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
- [x] Initialize Android project with Gradle 8.3 and AGP 8.x target API 34.
- [x] Implement edge perception modules:
  - [x] ML Kit Text Recognition (OCR).
  - [x] ML Kit Face Detection.
  - [x] Local regex / pattern sensitive data detector (PII: 9 types, Luhn, OTP context).
  - [x] Native `PdfRenderer` for safe first-3-pages multi-page ingestion.
  - [x] Multi-format artifact analysis (`IMAGE`, `SCREENSHOT`, `TEXT`, `URL`, `PDF`).
- [x] Implement local redaction engine (in-memory canvas pixel masking + text masking).
- [x] Implement network client with offline fallback:
  - [x] `OFFLINE` mode (zero network bytes, local heuristics).
  - [x] `LOCAL_BACKEND` mode (sends masked bitmap + metadata).
  - [x] `RESTRICTED_EVALUATION` mode (academic ablation testing).
- [x] Presentation-Grade UI Architecture:
  - [x] Dark graphite & glassmorphic design system (`BackgroundDark = #090D14`, `SurfaceGlass`, `CyanAccent`).
  - [x] Home Screen with exact Hero ("Before you act, ContextGuard."), supporting text, and 3 CTAs ("Analyze something", "Demo scenarios", "Privacy center").
  - [x] 4-Step Analysis Flow: Step 1 Artifact preview (BEFORE/AFTER toggle, blackout/blur), Step 2 "What are you about to do?" (8 chips: `SAVE`, `SEND`, `UPLOAD`, `POST`, `SIGN`, `LOGIN`, `APPROVE`, `OPEN`), Step 3 Context (Recipient, Destination, Source app), Step 4 Analyze CTA with animated `SixStageProgressOverlay` (Context -> Intent -> Evidence -> Consequence -> Uncertainty -> Intervention).
  - [x] Dominant Result Screen with exact user copy: ACT ("Looks safe to proceed."), ASK ("Before you continue, we need to clarify something."), WARN ("This action carries a meaningful risk."), STOP ("We strongly recommend not proceeding."). Displays risk score, confidence, severity, reversibility, evidence, alternative action, and exact buttons (Continue, Review evidence, Change action / Continue anyway, Review evidence).
  - [x] Expandable 8-node visual Decision Trace Pipeline (Artifact ↓ Context ↓ Intent ↓ Evidence ↓ Consequence ↓ Uncertainty ↓ Policy ↓ Intervention).
  - [x] Grounded Evidence Card: image spatial coordinates, XGBoost URL risk score $P(\text{phishing})$, OCR document excerpts.
  - [x] Demo Mode with 8 ready-to-run cards (Bank statement, Fake KYC link, OTP screenshot, Unknown recipient document, Routine family photo, Routine news URL, Contract lock-in, Payment request).
  - [x] Supervisor Viva Mode: 5 sections (`MODEL`, `PERCEPTION`, `VLM`, `URL MODEL`, `POLICY`) + live HUD metrics (Latency, Confidence, Risk, Evidence count, Redactions, Network state).
- [x] Android System Sharesheet Integration:
  - [x] Declared system intent filters (`ACTION_SEND` and `ACTION_SEND_MULTIPLE`) for `image/*`, `text/plain`, `application/pdf`, and `*/*`.
  - [x] Strongly-typed `SharePayload` sealed model hierarchy with zero persistent storage writing.
  - [x] Non-crashing `SharesheetPayloadResolver` using `ContentResolver`, bounded `ImagePreprocessor` downsampling, and regex URL parsing.
  - [x] Safe in-memory PDF rendering via `PdfPerceptionRenderer.renderPdfFromPfd(...)`.
  - [x] Direct navigation pipeline in `MainViewModel` and `MainActivity` for seamless auto-routing to `AnalyzeScreen`.
  - [x] Robust handling across 8 edge cases (missing stream, inaccessible URI, unsupported MIME, corrupted PDF, cancelled share).
  - [x] Dedicated automated test suite in `SharesheetIntegrationTest.kt` verifying model parsing, error resilience, and the 3-iteration identical artifact triad (`SAVE` -> `ACT`, `SEND` -> `WARN/ASK`, `POST` -> `STOP`).
- [x] Android unit tests passing (`28 passed`) and presentation-ready `app-debug.apk` (94.9 MB) successfully assembled.

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
