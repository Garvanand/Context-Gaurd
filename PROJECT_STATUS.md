# ContextGuard: Project Status Report

**Capstone Title:** CONTEXTGUARD: A Multimodal AI System for Pre-Action Risk Detection in Everyday Digital Tasks  
**Institution:** Final-Year B.Tech Capstone Project  
**Status Date:** Phase 6 (Real-Time Mobile ↔ Web Relay Integration) Completed  
**Current Milestone:** Real-Time Mobile ↔ Web Relay Integration **COMPLETED**  
**Next Milestone:** Final Capstone Presentation & Live Viva Demonstration

---

## 1. System Environment & Subsystems Audit

| Component | Target / Required | Detected System Environment | Build & Test Status |
| :--- | :--- | :--- | :---: |
| **Operating System** | Windows 10/11 64-bit | Windows 11 Home Single Language (10.0.26200) | **PASS** |
| **Python** | 3.11+ | Python 3.12.0 (`C:\Users\GARV ANAND\AppData\Local\Programs\Python\Python312\python.exe`) | **PASS** |
| **Java JDK** | JDK 17 LTS | OpenJDK 17.0.6 (`C:\Program Files\Android\Android Studio\jbr`) | **PASS** |
| **Android SDK** | API 34+ | Android SDK at `C:\Users\GARV ANAND\AppData\Local\Android\Sdk` (API 34, Build-tools 34.0.0) | **PASS** |
| **Gradle** | 8.x | Gradle 8.3 (Wrapper generated in `android/gradlew.bat`) | **PASS** |
| **FastAPI Backend** | 0.110+ | FastAPI 0.141.1, Uvicorn 0.52.0, Pydantic 2.13.4 | **PASS** (100% tests pass) |
| **Android App** | Jetpack Compose | CompileSdk 34, MinSdk 26, Material 3, Kotlin 1.9.22, AGP 8.2.2, OkHttp 4.12.0 | **PASS** (100% tests pass, APK built) |
| **ML Component** | Trained Classifier | XGBoost 3.2.0 trained on PhiUSIIL (UCI ID: 967) | **PASS** (Test F1: 0.9951, AUC: 0.9990) |
| **Multimodal Reasoner** | Qwen2.5-VL-3B / Ollama | `backend/models/qwen_vision.py`, `backend/prompts/*` | **PASS** (20/20 tests pass, safe fallback active) |
| **Six-Stage Pipeline** | Full Reasoning Pipeline | `backend/pipeline/pipeline.py`, `backend/policy/*` | **PASS** (18/18 pipeline & policy tests pass) |
| **Relay Backend** | Bidirectional Transport | `backend/app/relay/`, SQLite WAL Ledger, In-Memory Broker | **PASS** (8/8 relay tests, 1/1 e2e test pass) |
| **Evaluation Framework** | Baselines & Ablations | `evaluation/`, `results/`, `results/figures/`, `results/tables/` | **PASS** (99/99 Python tests pass) |

---

## 2. Deliverable Verification Matrix

| Area | Deliverables Required | Implementation Details | Test & Verification Result |
| :--- | :--- | :--- | :--- | :---: |
| **Canonical Input/Output Contract** | - Canonical `AnalysisRequest`<br>- Explicit missing fields representation<br>- Canonical `AnalysisResponse` | - `backend/app/models/schemas.py`<br>- Fields: artifact, artifact_type, ocr_text, detected_faces, detected_pii, redaction_metadata, url, selected_action, recipient, destination, source_app, network_state, user_context<br>- Output: intervention, risk_score, severity, reversibility, confidence, evidence, reason, recommended_alternative, can_override | **PASS**<br>Schema validation tested; handles null and omitted fields cleanly. |
| **Stage 1 — Context** | - Multi-source context aggregation<br>- No invented missing fields | - `ContextGuardPipeline.stage_1_context`<br>- Integrates OCR, face counts, PII, source_app, recipient, destination, network_state | **PASS**<br>Tested across multiple channels (public feed, vault, unverified chat). |
| **Stage 2 — Intent** | - Explicit selected actions<br>- Inferred intent for research<br>- Strict precedence rule | - `ContextGuardPipeline.stage_2_intent`<br>- Actions: SEND, UPLOAD, POST, SIGN, LOGIN, APPROVE, SAVE, OPEN<br>- Inferred intent NEVER silently overrides explicit selected intent | **PASS**<br>Precedence verified via unit tests. |
| **Stage 3 — Evidence** | - Action-relevant extraction<br>- Explicit evidence sources | - `ContextGuardPipeline.stage_3_evidence`<br>- Detects OTP, credentials, financial records, faces, phishing URLs, public destinations, unverified recipients<br>- Strict sources: `OCR`, `ML_KIT`, `URL_MODEL`, `VLM`, `CONTEXT`, `RULE` | **PASS**<br>Every evidence item verified against valid enum sources. |
| **Stage 4 — Consequence** | - Separate severity & reversibility<br>- Action-conditioned divergence | - `ContextGuardPipeline.stage_4_consequence`<br>- Financial + SAVE = severity 0.05, reversibility 0.0<br>- Financial + POST = severity 0.90, reversibility 1.0<br>- Financial + SEND unverified = severity 0.40, reversibility 0.50 | **PASS**<br>Central viva bank statement divergence verified. |
| **Stage 5 — Uncertainty** | - Epistemic confidence calculation<br>- Uncertainty is not safety | - `ContextGuardPipeline.stage_5_uncertainty`<br>- Calculates confidence $c \in [0.0, 1.0]$<br>- Missing OCR / unverified recipient penalizes confidence<br>- Moderate risk + low confidence mandates ASK | **PASS**<br>Uncertainty gating verified. |
| **Stage 6 — Intervention** | - Deterministic policy engine<br>- Freezeable thresholds<br>- Grounded explanation | - `backend/policy/thresholds.py`: $\rho = s \times (1 + \lambda \times r)$, frozen evaluation lock<br>- `backend/policy/explain.py`: Non-hallucinatory deterministic reason and alternative<br>- `backend/policy/policy.py`: STOP, ASK, WARN, ACT gating | **PASS**<br>All mathematical thresholds and frozen lock verified. |
| **Model Failure Safety Rule** | - No silent ACT on failure<br>- Safe fallback | - `DeterministicPolicyEngine.safe_fallback`<br>- Invoked on VLM failure, URL model failure, schema anomaly, or out-of-bounds metrics<br>- Emits ASK with safe default parameters | **PASS**<br>Verified under pipeline monkeypatching and corrupt metrics. |
| **Multimodal Vision Reasoner** | - `VisionReasoner.analyze(...)`<br>- `qwen2.5vl:3b` Ollama runtime<br>- Strict JSON schema<br>- Safe unavailable fallback | - `backend/models/base.py`<br>- `backend/models/qwen_vision.py`<br>- `ImageNormalizer` (JPEG, PNG, WebP)<br>- Bounded $[0.0, 1.0]$ metrics (`severity`, `reversibility`, `confidence`, `importance`) | **PASS**<br>Tested with mock VLM, codeblock wrappers, malformed JSON, and offline fallback across all 8 archetypes. |
| **Prompt Engineering** | - 6 immutable versioned prompts<br>- Grounded evidence & uncertainty | - `backend/prompts/context_v1.txt`<br>- `backend/prompts/intent_v1.txt`<br>- `backend/prompts/evidence_v1.txt`<br>- `backend/prompts/consequence_v1.txt`<br>- `backend/prompts/uncertainty_v1.txt`<br>- `backend/prompts/reasoning_v1.txt` | **PASS**<br>All prompts present, tested for string substitution and JSON constraints. |
| **Model Health & Telemetry** | - `GET /health/models`<br>- Never fake health<br>- Real test inference probe | - `backend/app/api/v1/health.py`<br>- Returns `ollama_reachable`, `model_installed`, `model_name`, `startup_latency`, `test_inference_status`, `vlm_healthy` | **PASS**<br>Authentic telemetry returned. |
| **Dataset Acquisition** | - Reproducible script<br>- PhiUSIIL (UCI ID 967) | - `ml/datasets/acquire_phiusiil.py`<br>- 235,795 rows extracted | **PASS** |
| **Feature Extraction** | - Deterministic features<br>- 30+ properties | - `ml/features/url_features.py` (36 features) | **PASS** |
| **Model Training & Comparison** | - Model comparison<br>- XGBoost champion | - `ml/training/train_phishing.py`<br>- XGBoost test F1: 0.9951, AUC: 0.9990 | **PASS** |
| **Android Client** | - 8 Compose screens<br>- Single Activity | - Material 3 Dark theme, single activity architecture, edge perception integration | **PASS**<br>`testDebugUnitTest`: passed.<br>`assembleDebug`: `app-debug.apk` built. |
| **Android Perception Layer** | - Google ML Kit Text Recognition<br>- Google ML Kit Face Detection<br>- Native `PdfRenderer` (first 3 pages)<br>- Local PII Detector (9 types, Luhn check, OTP context)<br>- Strongly typed `LocalPerceptionResult` | - `MlKitPerceptionEngine.kt`<br>- `PiiDetector.kt`<br>- `PdfPerceptionRenderer.kt`<br>- `ImagePreprocessor.kt`<br>- Coroutines off-UI dispatchers (`Dispatchers.Default`, `Dispatchers.IO`) | **PASS**<br>15 dedicated unit tests passed.<br>APK packaged with real native ML Kit runtime (`libface_detector_v2_jni.so`, `libmlkit_google_ocr_pipeline.so`). |
| **Privacy Architecture** | - 9-stage Serial Privacy Pipeline<br>- In-memory Redaction Engine (`BLACKOUT` & `BLUR`)<br>- 3 Network Modes (`OFFLINE`, `LOCAL_BACKEND`, `RESTRICTED_EVALUATION`)<br>- Metadata-only Network Audit Logger<br>- SHA-256 Correlation Fingerprinting<br>- Interactive Privacy Center UI<br>- Before vs After Redaction Preview<br>- User Intentional Override on STOP | - `PrivacyPipeline.kt`<br>- `RedactionEngine.kt`<br>- `NetworkAuditLogger.kt`<br>- `ContextGuardApiClient.kt`<br>- `PrivacyScreen.kt`<br>- `AnalyzeScreen.kt`<br>- `ResultScreen.kt`<br>- `docs/THREAT_MODEL.md` | **PASS**<br>6/6 privacy invariant unit tests passed.<br>Debug APK assembled successfully. |
| **Android Sharesheet Integration** | - System-level Share Target (`ACTION_SEND` & `ACTION_SEND_MULTIPLE`)<br>- Supported MIME types: `image/*`, `text/plain`, `application/pdf`, `*/*`<br>- ContentResolver extraction (zero persistent disk writes)<br>- Safe in-memory decoding with bounded 1600px downsampling<br>- Multi-page PDF rasterization via `PdfRenderer`<br>- URL pattern extraction from shared browser text<br>- Non-crashing error handling across 8 edge cases<br>- 3-Iteration identical artifact triad verification | - `AndroidManifest.xml`<br>- `SharesheetPayloadResolver.kt`<br>- `SharePayload.kt`<br>- `PdfPerceptionRenderer.kt`<br>- `MainViewModel.kt`<br>- `MainActivity.kt`<br>- `SharesheetIntegrationTest.kt`<br>- `DEMO_RUNBOOK.md` (Section 5) | **PASS**<br>6/6 sharesheet integration unit tests pass.<br>Debug APK assembled (94.9 MB). |
| **Everyday Action Risk Benchmark (EARB)** | - Exactly 20 programmatically generated synthetic base artifacts<br>- Exactly 60 action-conditioned pairs across 4 categories<br>- 3 candidate actions per base artifact with dynamic shifts<br>- 20 negative controls (ACT) & 8 ambiguous cases (ASK)<br>- Zero-leakage grouping by base artifact (dev: 42, test: 18)<br>- Interactive CLI annotator & local Web UI annotator<br>- Automated Pydantic validation & pytest test suite | - `benchmark/data/earb_v1.jsonl`<br>- `benchmark/data/earb_v1.csv`<br>- `benchmark/schema/earb_schema.json`<br>- `benchmark/scripts/generate_synthetic_artifacts.py`<br>- `benchmark/scripts/build_earb_dataset.py`<br>- `benchmark/scripts/annotate_cli.py`<br>- `benchmark/scripts/validate_benchmark.py`<br>- `benchmark/annotator/index.html`<br>- `tests/benchmark/test_earb_benchmark.py` | **PASS**<br>60/60 pairs valid.<br>7/7 benchmark pytest tests pass. |
| **Spectral Signal Artifact-Analysis Journey** | - Step 1: Spacious Artifact Intake Surface with animated aperture & before/after inspection<br>- Step 2: 8-Action Selector (SAVE, SEND, UPLOAD, POST, SIGN, LOGIN, APPROVE, OPEN) with dynamic animated signal connector bus into Context Field & app-capability disclosures<br>- Step 3: Progressive 6-sector illuminated reasoning pipeline with TalkBack liveRegion announcements and sub-400ms transitions<br>- Step 4: Controlled 4-phase result reveal without superficial fireworks<br>- Step 5: Grounded evidence inspection with uncertainty calibration & model provenance<br>- Step 6: Change-Context Comparison simulating identical artifact under SAVE vs SEND vs POST with genuine math $\rho = s \cdot (1 + \lambda r)$ | - `ArtifactIntakeSurface.kt`<br>- `ActionSelectorWithConnector.kt`<br>- `SixStageProgressOverlay.kt`<br>- `ContextComparisonMode.kt`<br>- `AnalyzeScreen.kt`<br>- `ResultScreen.kt`<br>- `EvidenceCard.kt` | **PASS**<br>Clean build and unit test execution. |
| **Signal Intercept: JIT Intervention Overlay** | - Genuine `AccessibilityService` overlay using `TYPE_ACCESSIBILITY_OVERLAY`<br>- Compact floating card layout (`WRAP_CONTENT` height for all classes, never full-screen modal)<br>- Thin spectral accent leading edge (Electric Violet -> Ion Cyan -> Intervention Accent)<br>- Small Aperture Signal logo & evidence-category indicators (`PHISHING LINK`, `SENSITIVE PII`, `PAYMENT AUTHORIZATION`, `CREDENTIAL ENTRY`)<br>- One strong intervention heading & concise grounded evidence sentence<br>- Prominent "Review Evidence" CTA & restrained "Continue once" dismissal without click injection<br>- 4-Phase signature animation (< 300ms) with full Reduced Motion mode support<br>- Rapid app switching auto-dismissal & anti-fatigue fingerprint cooldowns | - `InterventionOverlayManager.kt`<br>- `ScreenGuardAccessibilityService.kt`<br>- `OverlayTelemetry.kt`<br>- `ScreenRiskTriggerEngine.kt`<br>- `InterventionOverlayTest.kt` | **PASS**<br>Zero click injection verified.<br>RAPID app switch & latency telemetry verified. |
| **Real-Time Mobile ↔ Web Relay** | - Authenticated 6-char pairing handshake<br>- Scoped cryptographically hashed tokens (`devtok_*`, `dshtok_*`)<br>- Durable SQLite WAL ledger for events, commands, presence, audit logs<br>- Real-time WebSocket multiplexer with live fan-out<br>- Zero raw persistence privacy enforcement<br>- Bidirectional command queue with mobile ACK loop<br>- Interactive Live Device Relay Panel & Command Center in Dashboard | - `backend/app/relay/*`<br>- `backend/app/api/v1/relay.py`<br>- `android/app/.../network/Relay*`<br>- `supervisor-dashboard/src/services/relay.ts`<br>- `supervisor-dashboard/src/components/LiveDeviceRelayPanel.tsx`<br>- `tests/backend/test_relay.py`<br>- `tests/integration/test_relay_e2e.py` | **PASS**<br>8/8 relay pytest tests pass.<br>1/1 E2E round-trip test passes.<br>Android unit tests pass.<br>Production dashboard builds cleanly. |

---

## 3. Test Suite Execution Summary

```text
============================= test session starts =============================
platform win32 -- Python 3.12.0, pytest-9.0.3, pluggy-1.5.0
rootdir: C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd

tests/backend/test_action_decision_engine.py ........                    [  8%]
tests/backend/test_config.py ...                                         [ 11%]
tests/backend/test_health.py .....                                       [ 16%]
tests/backend/test_pipeline.py ..........                                [ 26%]
tests/backend/test_policy_engine.py ........                             [ 34%]
tests/backend/test_relay.py ........                                     [ 42%]
tests/backend/test_vision_reasoner.py ....................               [ 62%]
tests/benchmark/test_cac_metric.py .....                                 [ 67%]
tests/benchmark/test_earb_benchmark.py ....                              [ 71%]
tests/benchmark/test_earb_schema.py ...                                  [ 74%]
tests/benchmark/test_evaluation.py ........                              [ 82%]
tests/integration/test_relay_e2e.py .                                    [ 83%]
tests/ml/test_policy.py ......                                           [ 89%]
tests/ml/test_url_risk.py ..........                                     [100%]

======================= 99 passed, 2 warnings in 42.10s =======================

============================= Android Gradle Build =============================
> Task :app:compileDebugKotlin
> Task :app:compileDebugUnitTestKotlin
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 18s (Unit tests pass, including RelayModelsUnitTest)

> Task :app:assembleDebug
BUILD SUCCESSFUL in 46s
Output APK: android/app/build/outputs/apk/debug/app-debug.apk

============================= Supervisor Dashboard =============================
> tsc -b && vite build
✓ 1874 modules transformed.
dist/index.html                   0.85 kB │ gzip:  0.43 kB
dist/assets/index-D7xVpE2L.css   24.12 kB │ gzip:  5.23 kB
dist/assets/index-CF7v9q1m.js   384.62 kB │ gzip: 112.44 kB
✓ built in 7.15s
```

---

## 4. Phase 6 Real-Time Integration Milestone Completed

* **Operating Mode A (Normal Launch):** Direct navigation to `HomeScreen`. Dynamic telemetry for Accessibility, Notification access, Allowlisted apps (6 apps enabled), and On-device tree models.
* **Operating Mode B (Background Protection):** `ScreenGuardAccessibilityService` and `NotificationGuardService` wired with deterministic pre-action trigger engine and `InterventionOverlayManager` (`TYPE_ACCESSIBILITY_OVERLAY`).
* **Operating Mode C (Explicit Sharing):** `SharesheetPayloadResolver` ingests actual stream bytes/text. Local preprocessing executes in volatile RAM. Selecting an action automatically triggers inference without an unnecessary manual "Analyze Now" button.
* **Operating Mode D (Real-Time Relay & Pairing):** Android client pairs via 6-character code with Supervisor Web Dashboard. Streams live telemetry events over authenticated WebSocket or durable REST outbox with zero-leakage privacy enforcement. Supervisor queues action commands executed by mobile with real-time ACKs.
* **Physical Device Acceptance Status:** Honest assessment recorded — ADB device list empty, therefore physical hardware verification is **UNVERIFIED**, while APK packaging, unit tests, backend test suite, and web frontend are 100% verified.

