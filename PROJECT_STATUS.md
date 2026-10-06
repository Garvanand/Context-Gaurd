# ContextGuard: Project Status Report

**Capstone Title:** CONTEXTGUARD: A Multimodal AI System for Pre-Action Risk Detection in Everyday Digital Tasks  
**Institution:** Final-Year B.Tech Capstone Project  
**Status Date:** Multimodal AI Reasoning Layer (Qwen2.5-VL-3B & Health Telemetry) Completed  
**Current Milestone:** Multimodal Vision Reasoner & Safe Fallback **COMPLETED**  
**Next Milestone:** Phase 2 (Everyday Action Risk Benchmark - EARB Dataset & Central Demo Generation)

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

---

## 2. Deliverable Verification Matrix

| Area | Deliverables Required | Implementation Details | Test & Verification Result |
| :--- | :--- | :--- | :--- | :---: |
| **Multimodal Vision Reasoner** | - `VisionReasoner.analyze(...)`<br>- `qwen2.5vl:3b` Ollama runtime<br>- Strict JSON schema<br>- Safe unavailable fallback | - `backend/models/base.py`<br>- `backend/models/qwen_vision.py`<br>- `ImageNormalizer` (JPEG, PNG, WebP)<br>- Bounded $[0.0, 1.0]$ metrics (`severity`, `reversibility`, `confidence`, `importance`)<br>- Model Failure Safety Rule enforced (never silent `ACT`) | **PASS**<br>Tested with mock VLM, codeblock wrappers, malformed JSON, and offline fallback across all 8 archetypes. |
| **Prompt Engineering** | - 6 immutable versioned prompts<br>- Grounded evidence & uncertainty | - `backend/prompts/context_v1.txt`<br>- `backend/prompts/intent_v1.txt`<br>- `backend/prompts/evidence_v1.txt`<br>- `backend/prompts/consequence_v1.txt`<br>- `backend/prompts/uncertainty_v1.txt`<br>- `backend/prompts/reasoning_v1.txt` | **PASS**<br>All prompts present, tested for string substitution and JSON constraints. |
| **Model Health & Telemetry** | - `GET /health/models`<br>- Never fake health<br>- Real test inference probe | - `backend/app/api/v1/health.py`<br>- Returns `ollama_reachable`, `model_installed`, `model_name`, `startup_latency`, `test_inference_status`, `vlm_healthy`<br>- Performs live ping test when daemon is reachable | **PASS**<br>Returns authentic status (`service_unavailable` / `model_not_pulled` / `passed`). |
| **Synthetic Test Artifacts** | - 8 threat archetypes<br>- Zero raw image persistence<br>- Hashes and structured outputs | - `tests/ml/generate_synthetic_artifacts.py`<br>- Evaluates: benign photo, bank statement, phishing screenshot, OTP, KYC message, ordinary doc, sensitive doc, QR payment<br>- `artifacts/evaluations/synthetic_test_outputs.json` | **PASS**<br>8/8 evaluated; outputs and SHA-256 digests recorded without persisting raw images to disk. |
| **Dataset Acquisition** | - Reproducible script<br>- PhiUSIIL (UCI ID 967)<br>- No manual copy | - `ml/datasets/acquire_phiusiil.py`<br>- Downloads from `https://archive.ics.uci.edu/static/public/967/phiusiil+phishing+url+dataset.zip` | **PASS**<br>Downloaded 14.66 MB zip and extracted 54.22 MB CSV (235,795 rows). |
| **Feature Extraction** | - Deterministic features<br>- 30+ lexical/structural properties<br>- Identical training & inference | - `ml/features/url_features.py`<br>- Implements 36 reproducible features (length, subdomains, Shannon entropy, ratios, keywords, TLD, IP host, etc.) | **PASS**<br>Throughput: >20,000 URLs/sec. Verified across 25 deterministic unit test URLs. |
| **Model Training & Comparison** | - Logistic Regression<br>- Random Forest<br>- XGBoost<br>- Stratified splits<br>- Model selection by validation | - `ml/training/train_phishing.py`<br>- 60,000 stratified samples (42k Train, 9k Val, 9k Test)<br>- Validation: LogReg (F1: 0.9935), RF (F1: 0.9958), **XGBoost (F1: 0.9961)** | **PASS**<br>XGBoost selected as champion. Evaluated on 9,000 held-out test samples. |
| **Model Artifacts** | - Serialized model<br>- Metadata payload | - `ml/artifacts/url_risk_model.joblib` (295.7 KB)<br>- `ml/artifacts/url_model_metadata.json` (5.39 KB) | **PASS**<br>Both artifacts generated and verified. |
| **Inference Service** | - `backend/services/url_risk.py`<br>- Exact same feature extractor<br>- Required JSON output schema | - `backend/services/url_risk.py`<br>- Imports `URLFeatureExtractor`<br>- Implements `predict(raw_url)` returning `{is_phishing, probability, confidence, risk_level, model, features_used}` | **PASS**<br>Tested and verified on legitimate, IP, localhost, and phishing URLs. |
| **Automated Testing** | - Unit tests for URLs<br>- Schema, malformed, edge cases<br>- 20+ deterministic URLs<br>- Vision reasoner & health tests | - `tests/ml/test_url_risk.py` (9 tests covering 25 test URLs)<br>- `tests/backend/test_vision_reasoner.py` (20 tests)<br>- `tests/backend/test_health.py` (5 tests)<br>- `tests/backend/test_config.py` (3 tests)<br>- `tests/ml/test_policy.py` (6 tests)<br>- `tests/benchmark/test_earb_schema.py` (3 tests) | **PASS**<br>**46 passed in 19.27s** across entire pytest suite. |
| **Android Client** | - 8 Compose screens<br>- Single Activity<br>- Material 3 Dark theme<br>- Architecture abstractions | - `MainActivity.kt`<br>- `NavGraph.kt`<br>- `MainViewModel.kt`<br>- `Welcome`, `Home`, `Analyze`, `Result`, `Privacy`, `Settings`, `Demo`, `Supervisor` screens | **PASS**<br>`testDebugUnitTest`: 22/22 tasks passed.<br>`assembleDebug`: `app-debug.apk` (15.8 MB) generated. |

---

## 3. Test Suite Execution Summary

```text
============================= test session starts =============================
platform win32 -- Python 3.12.0, pytest-9.0.3, pluggy-1.5.0
rootdir: C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd

tests/backend/test_config.py ...                                         [  6%]
tests/backend/test_health.py .....                                       [ 17%]
tests/backend/test_vision_reasoner.py ....................               [ 60%]
tests/benchmark/test_earb_schema.py ...                                  [ 67%]
tests/ml/test_policy.py ......                                           [ 80%]
tests/ml/test_url_risk.py .........                                      [100%]

======================= 46 passed in 19.27s ====================================

============================= Android Gradle Build =============================
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:compileDebugUnitTestKotlin UP-TO-DATE
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 1m 36s (22 actionable tasks executed)

> Task :app:assembleDebug
BUILD SUCCESSFUL in 1m 41s (33 actionable tasks executed)
Output APK: android/app/build/outputs/apk/debug/app-debug.apk (15.8 MB)
```

---

## 4. Next Immediate Phase
**Phase 2: Everyday Action Risk Benchmark (EARB) Dataset & Central Demo Generation**
1. Generate 20 high-fidelity synthetic base digital artifacts across 4 categories:
   - Financial (Bank statement, invoice, payment QR, credit card form, tax summary).
   - Digital Security (Password reset email, 2FA backup codes, SSH key, session token, login alert).
   - Privacy Disclosure (Medical discharge summary, national ID card, flight itinerary, selfie, offer letter).
   - Communication (Confidential Slack DM, strategy memo, NDA draft, support ticket, executive calendar).
2. Generate 60 artifact-action pairs conforming to the EARB schema.
3. Validate dataset via `benchmark.schema.validator`.
4. Ensure Central Demo bank statement is rendered with realistic layout.
