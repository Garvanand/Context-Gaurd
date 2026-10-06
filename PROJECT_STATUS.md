# ContextGuard: Project Status Report

**Capstone Title:** CONTEXTGUARD: A Multimodal AI System for Pre-Action Risk Detection in Everyday Digital Tasks  
**Institution:** Final-Year B.Tech Capstone Project  
**Status Date:** Android Privacy Architecture (Pipeline, Redaction Engine, Network Modes, Audit Logger, Threat Model & Privacy Center) Completed  
**Current Milestone:** Privacy Architecture & Invariants **COMPLETED**  
**Next Milestone:** Phase 2 (Everyday Action Risk Benchmark - EARB Dataset & 60 Action-Conditioned Pairs)

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
| **Android App** | Jetpack Compose | CompileSdk 34, MinSdk 26, Material 3, Kotlin 1.9.22, AGP 8.2.2 | **PASS** (100% tests pass, APK built) |
| **ML Component** | Trained Classifier | XGBoost 3.2.0 trained on PhiUSIIL (UCI ID: 967) | **PASS** (Test F1: 0.9951, AUC: 0.9990) |
| **Multimodal Reasoner** | Qwen2.5-VL-3B / Ollama | `backend/models/qwen_vision.py`, `backend/prompts/*` | **PASS** (20/20 tests pass, safe fallback active) |
| **Six-Stage Pipeline** | Full Reasoning Pipeline | `backend/pipeline/pipeline.py`, `backend/policy/*` | **PASS** (18/18 pipeline & policy tests pass) |

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
| **Automated Testing** | - Unit & integration tests<br>- Full test suite | - 64 automated test cases across monorepo | **PASS**<br>**64 passed in 19.25s**. |
| **Android Client** | - 8 Compose screens<br>- Single Activity | - Material 3 Dark theme, single activity architecture, edge perception integration | **PASS**<br>`testDebugUnitTest`: 37/37 passed.<br>`assembleDebug`: `app-debug.apk` built. |
| **Android Perception Layer** | - Google ML Kit Text Recognition<br>- Google ML Kit Face Detection<br>- Native `PdfRenderer` (first 3 pages)<br>- Local PII Detector (9 types, Luhn check, OTP context)<br>- Strongly typed `LocalPerceptionResult` | - `MlKitPerceptionEngine.kt`<br>- `PiiDetector.kt`<br>- `PdfPerceptionRenderer.kt`<br>- `ImagePreprocessor.kt`<br>- Coroutines off-UI dispatchers (`Dispatchers.Default`, `Dispatchers.IO`) | **PASS**<br>15 dedicated unit tests passed.<br>APK packaged with real native ML Kit runtime (`libface_detector_v2_jni.so`, `libmlkit_google_ocr_pipeline.so`). |
| **Privacy Architecture** | - 9-stage Serial Privacy Pipeline<br>- In-memory Redaction Engine (`BLACKOUT` & `BLUR`)<br>- 3 Network Modes (`OFFLINE`, `LOCAL_BACKEND`, `RESTRICTED_EVALUATION`)<br>- Metadata-only Network Audit Logger<br>- SHA-256 Correlation Fingerprinting<br>- Interactive Privacy Center UI<br>- Before vs After Redaction Preview<br>- User Intentional Override on STOP | - `PrivacyPipeline.kt`<br>- `RedactionEngine.kt`<br>- `NetworkAuditLogger.kt`<br>- `ContextGuardApiClient.kt`<br>- `PrivacyScreen.kt`<br>- `AnalyzeScreen.kt`<br>- `ResultScreen.kt`<br>- `docs/THREAT_MODEL.md` | **PASS**<br>6/6 privacy invariant unit tests passed.<br>43 total Android unit tests passed.<br>Debug APK assembled successfully. |

---

## 3. Test Suite Execution Summary

```text
============================= test session starts =============================
platform win32 -- Python 3.12.0, pytest-9.0.3, pluggy-1.5.0
rootdir: C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd

tests/backend/test_config.py ...                                         [  4%]
tests/backend/test_health.py .....                                       [ 12%]
tests/backend/test_pipeline.py ..........                                [ 28%]
tests/backend/test_policy_engine.py ........                             [ 40%]
tests/backend/test_vision_reasoner.py ....................               [ 71%]
tests/benchmark/test_earb_schema.py ...                                  [ 76%]
tests/ml/test_policy.py ......                                           [ 85%]
tests/ml/test_url_risk.py .........                                      [100%]

======================= 64 passed, 2 warnings in 21.48s =======================

============================= Android Gradle Build =============================
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:compileDebugUnitTestKotlin UP-TO-DATE
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 24s (22 actionable tasks executed: 43 total unit tests pass)
- ViewModel & State Tests: 22 passed
- Perception & ML Kit & PII Tests: 15 passed
- Privacy Architecture & Invariant Tests: 6 passed

> Task :app:assembleDebug
BUILD SUCCESSFUL in 22s (35 actionable tasks executed)
Output APK: android/app/build/outputs/apk/debug/app-debug.apk (94.8 MB)
```

---

## 4. Next Immediate Phase
**Phase 2: Everyday Action Risk Benchmark (EARB) Dataset & 60 Action-Conditioned Pairs**
1. Generate 20 high-fidelity synthetic base digital artifacts across 4 categories:
   - Financial (Bank statement, invoice, payment QR, credit card form, tax summary).
   - Digital Security (Password reset email, 2FA backup codes, SSH key, session token, login alert).
   - Privacy Disclosure (Medical discharge summary, national ID card, flight itinerary, selfie, offer letter).
   - Communication (Confidential Slack DM, strategy memo, NDA draft, support ticket, executive calendar).
2. Generate 60 artifact-action pairs conforming to the EARB schema.
3. Validate dataset via `benchmark.schema.validator`.
4. Run ablation and benchmark evaluation.
