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
- [x] Programmatically generated exactly 20 high-fidelity, synthetic base digital artifacts across 4 categories (`benchmark/scripts/generate_synthetic_artifacts.py`):
  - [x] Financial (5 base artifacts: bank statement, UPI request, KYC form, payslip, investment advice).
  - [x] Digital Security (5 base artifacts: bank login portal, password reset alert, auth QR code, credential request email, browser warning).
  - [x] Privacy Disclosure (5 base artifacts: OTP SMS notification, family photo with address, private medical alert, clinical prescription with phone number, GPS live route tracking).
  - [x] Communication (5 base artifacts: draft executive M&A email, NDA legal contract draft, internal strategy memo, reply-all critique draft, whiteboard architecture diagram).
- [x] Zero real personal or financial data: 100% synthetic generation.
- [x] Constructed exactly 60 action-conditioned pairs adhering to the full EARB schema (3 distinct actions per artifact):
  - [x] Demonstrated dynamic action shifts (`ACT` -> `WARN`, `ACT` -> `STOP`, `ASK` -> `WARN`, `WARN` -> `STOP`).
  - [x] Negative controls included (20 benign safe tasks correctly labeled `ACT`).
  - [x] Realistic ambiguous cases included (8 epistemic uncertainty cases correctly labeled `ASK`).
- [x] Cross-partition leakage control: strict grouping by base artifact (dev: 14 base artifacts / 42 pairs, test: 6 base artifacts / 18 pairs, 0% leakage).
- [x] Exported benchmark datasets:
  - [x] `benchmark/data/earb_v1.jsonl` (60 lines)
  - [x] `benchmark/data/earb_v1.csv` (60 rows)
  - [x] `benchmark/schema/earb_schema.json` (Pydantic model JSON Schema export)
- [x] Interactive annotation tools:
  - [x] CLI annotator: `benchmark/scripts/annotate_cli.py`
  - [x] Local Web UI annotator: `benchmark/annotator/index.html`
- [x] Comprehensive validation suite (`benchmark/scripts/validate_benchmark.py` & `tests/benchmark/test_earb_benchmark.py`): 100% pass across all 60 pairs and 20 artifacts.

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
- [x] Implement baseline evaluators:
  - [x] B1: Artifact-only baseline (`evaluation/baselines/b1_artifact_only.py`).
  - [x] B3: Multimodal context-free baseline (`evaluation/baselines/b3_no_intent.py`).
  - [x] B4: Intent-aware fixed threshold baseline ($\lambda = 0$) (`evaluation/baselines/b4_fixed_threshold.py`).
  - [x] B5: Full ContextGuard system (`evaluation/baselines/b5_full_contextguard.py`).
- [x] Implement component ablations:
  - [x] A1: No intent (`evaluation/ablations/a1_no_intent.py`).
  - [x] A2: No multimodality (`evaluation/ablations/a2_no_multimodal.py`).
  - [x] A3: Fixed policy ($\lambda = 0$) (`evaluation/ablations/a3_fixed_policy.py`).
  - [x] A4: Adaptive policy ($\lambda = 0.75$) (`evaluation/ablations/a4_adaptive_policy.py`).
  - [x] A5: Warn everything (`evaluation/ablations/a5_warn_everything.py`).
- [x] Common Evaluation Contract enforced across all runs (`evaluation/schemas.py`).
- [x] Execute automated benchmark evaluation across all 60 EARB pairs (`python -m evaluation.run --all`).
- [x] Compute empirical metrics: Macro F1, STOP Recall, ACT False Alarm Rate, ECE, Mean Latency, and clustered bootstrap 95% CIs (`evaluation/metrics.py`).
- [x] Generate confusion matrices, safety tradeoff curves, and calibration plots in `results/figures/` (`python -m evaluation.report`).
- [x] Generate comparative tables in `results/tables/` (`python -m evaluation.compare`).
- [x] Update `EXPERIMENTS.md`, `results/RESEARCH_REPORT.md`, and `PROJECT_STATUS.md` with true empirical numbers.

---

## 6. Phase 6: System Integration, End-to-End Testing & Polish
- [ ] End-to-end integration test connecting Android client, FastAPI backend, and ML models.
- [ ] Central Demo verification (Bank Statement -> Save privately = ACT, Send to unknown = WARN/ASK, Post publicly = STOP).
- [ ] Stress and fault-injection testing (server outage, malformed JSON, network timeout).
- [ ] Final viva presentation assets and documentation sign-off.

---

## 7. Phase 7: Three-Mode Privacy-Utility Evaluation
- [x] Implement the three operational modes:
  - [x] Mode 1: `ON_DEVICE` (zero outbound egress, ML Kit heuristics, 18.8 ms).
  - [x] Mode 2: `REDACTED_LOCAL_BACKEND` (ContextGuard proposed, in-memory redaction, 135.5 ms).
  - [x] Mode 3: `RAW_CLOUD_EVALUATION` (synthetic benchmark unredacted cloud baseline, 375.5 ms).
- [x] Enforce Mode 3 benchmark safety invariant (only synthetic EARB artifacts permitted; never expose actual private user data).
- [x] Calculate all required metrics across each mode:
  - [x] Intervention Macro-F1 (Mode 1: 0.5389, Mode 2: 0.5044, Mode 3: 0.5455).
  - [x] Multi-class Accuracy (Mode 1: 63.3%, Mode 2: 65.0%, Mode 3: 68.3%).
  - [x] STOP Recall (Mode 1: 70.0%, Mode 2: 90.0%, Mode 3: 95.0%).
  - [x] ACT False Alarm Rate (Mode 1: 5.0%, Mode 2: 10.0%, Mode 3: 10.0%).
  - [x] Mean Confidence & Latency distributions.
  - [x] Transmitted sensitive regions (Mode 1: 0, Mode 2: 0, Mode 3: 117 leaked).
  - [x] Redaction count (Mode 1: 0, Mode 2: 117 masked, Mode 3: 0).
  - [x] Redaction ratio (Mode 1: 100%, Mode 2: 100%, Mode 3: 0%).
  - [x] Payload size (Mode 1: 0 KB, Mode 2: 11.53 KB, Mode 3: 28.35 KB, -59.3% reduction).
- [x] Measure and report the genuine utility cost of redaction ($\Delta \text{Acc} = -3.3\%$, $\Delta \text{F1} = -0.0411$).
- [x] Output `privacy_utility_results.json` and publication-quality plots:
  - [x] `results/figures/accuracy_vs_redaction.png`
  - [x] `results/figures/f1_vs_payload_size.png`
  - [x] `results/figures/latency_vs_accuracy.png`
- [x] Update UI Supervisor dashboards (Android `SupervisorScreen.kt` and web `index.html`) with explicit 3-mode selector and prominent hazard warning on RAW mode.

---

## 8. Phase 8: Supervisor Control Room (AI Safety Console)
- [x] Architect and implement Supervisor Control Room (`supervisor-dashboard/`):
  - [x] Tech stack: React 19 + TypeScript + Vite v5.4 (pure CSS design system, zero unnecessary frameworks).
  - [x] Dark defense-grade AI evaluation console aesthetic (Inter & JetBrains Mono typography, status badges, neon telemetry accents).
- [x] Implement all 11 dedicated console panels:
  - [x] 1. System Overview (8 real-time status cards: ContextGuard, Android Connection, Backend Status, Ollama, Qwen VLM, XGBoost URL, ML Kit, EARB Size).
  - [x] 2. Live Analysis (Interactive selector: 20 synthetic base artifacts, pre-action candidates, recipient, destination channel -> executes live 6-stage pipeline).
  - [x] 3. Pipeline Trace (Interactive 6-stage flow diagram: Context -> Intent -> Evidence -> Consequence -> Uncertainty -> Intervention with JSON inspector).
  - [x] 4. EARB Benchmark (60 pairs, 20 base artifacts, 4 categories, intervention and ambiguity distributions, search/filter table).
  - [x] 5. Model Performance (Qwen 2.5-VL 3B, XGBoost PhiUSIIL model, ML Kit, calibrated policy parameters).
  - [x] 6. Baseline Comparison (Strict empirical baseline matrix B1-B6; missing variants strictly display "Not evaluated").
  - [x] 7. Ablation Results (Component ablations A1-A5; missing variants strictly display "Not evaluated").
  - [x] 8. Privacy-Utility (3 modes: On-Device, Redacted Local Backend, Raw Cloud Benchmark with leakage & payload reduction metrics).
  - [x] 9. Failure Analysis (False STOP, False ACT/Hazard, False WARN, False ASK, tail latency outliers > P90).
  - [x] 10. Network Activity (Cryptographic wire audit ledger with SHA-256 fingerprints, wire size, network mode).
  - [x] 11. Model Health (Active health check probes with roundtrip latency, watchdog status, and "Ping Live Probes" trigger).
- [x] Backend Integration (`backend/app/api/v1/supervisor.py` & `backend/services/pii_detector.py`):
  - [x] Fast, memory-efficient endpoints consuming real result files and live ML pipeline.
  - [x] Zero raw persistence guard enforced.
- [x] Browser Verification & Documentation:
  - [x] Verified via browser tooling across all 11 panels, live execution, and probe triggers.
  - [x] Screenshots captured: `section1_overview`, `section2_live_analysis`, `section4_earb_benchmark`, `section5_model_performance`, `model_health_panel`, `live_analysis_panel`, `full_view_mode`.
  - [x] Browser session recording saved: `supervisor_room_final_verify_1791563258486.webp`.


