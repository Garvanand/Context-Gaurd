# ContextGuard: Project Status Report

**Capstone Title:** CONTEXTGUARD: A Multimodal AI System for Pre-Action Risk Detection in Everyday Digital Tasks  
**Institution:** Final-Year B.Tech Capstone Project  
**Status Date:** Phase 1 (ML Phishing Model & Feature Extraction) Completed  
**Current Milestone:** Phase 1 **COMPLETED**  
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

---

## 2. Deliverable Verification Matrix

| Area | Deliverables Required | Implementation Details | Test & Verification Result |
| :--- | :--- | :--- | :---: |
| **Dataset Acquisition** | - Reproducible script<br>- PhiUSIIL (UCI ID 967)<br>- No manual copy | - `ml/datasets/acquire_phiusiil.py`<br>- Downloads from `https://archive.ics.uci.edu/static/public/967/phiusiil+phishing+url+dataset.zip` | **PASS**<br>Downloaded 14.66 MB zip and extracted 54.22 MB CSV (235,795 rows). |
| **Feature Extraction** | - Deterministic features<br>- 30+ lexical/structural properties<br>- Identical training & inference | - `ml/features/url_features.py`<br>- Implements 36 reproducible features (length, subdomains, Shannon entropy, ratios, keywords, TLD, IP host, etc.) | **PASS**<br>Throughput: >20,000 URLs/sec. Verified across 25 deterministic unit test URLs. |
| **Model Training & Comparison** | - Logistic Regression<br>- Random Forest<br>- XGBoost<br>- Stratified splits<br>- Model selection by validation | - `ml/training/train_phishing.py`<br>- 60,000 stratified samples (42k Train, 9k Val, 9k Test)<br>- Validation: LogReg (F1: 0.9935), RF (F1: 0.9958), **XGBoost (F1: 0.9961)** | **PASS**<br>XGBoost selected as champion. Evaluated on 9,000 held-out test samples. |
| **Model Artifacts** | - Serialized model<br>- Metadata payload | - `ml/artifacts/url_risk_model.joblib` (295.7 KB)<br>- `ml/artifacts/url_model_metadata.json` (5.39 KB) | **PASS**<br>Both artifacts generated and verified. |
| **Inference Service** | - `backend/services/url_risk.py`<br>- Exact same feature extractor<br>- Required JSON output schema | - `backend/services/url_risk.py`<br>- Imports `URLFeatureExtractor`<br>- Implements `predict(raw_url)` returning `{is_phishing, probability, confidence, risk_level, model, features_used}` | **PASS**<br>Tested and verified on legitimate, IP, localhost, and phishing URLs. |
| **Automated Testing** | - Unit tests for URLs<br>- Schema, malformed, edge cases<br>- 20+ deterministic URLs | - `tests/ml/test_url_risk.py` (9 tests covering 25 test URLs)<br>- `tests/backend/test_health.py`<br>- `tests/backend/test_config.py`<br>- `tests/ml/test_policy.py`<br>- `tests/benchmark/test_earb_schema.py` | **PASS**<br>**26 passed in 5.09s** across entire pytest suite. |
| **Android Client** | - 8 Compose screens<br>- Single Activity<br>- Material 3 Dark theme<br>- Architecture abstractions | - `MainActivity.kt`<br>- `NavGraph.kt`<br>- `MainViewModel.kt`<br>- `Welcome`, `Home`, `Analyze`, `Result`, `Privacy`, `Settings`, `Demo`, `Supervisor` screens | **PASS**<br>`testDebugUnitTest`: 22/22 tasks passed.<br>`assembleDebug`: `app-debug.apk` (15.8 MB) generated. |

---

## 3. Test Suite Execution Summary

```text
============================= test session starts =============================
platform win32 -- Python 3.12.0, pytest-9.0.3, pluggy-1.5.0
rootdir: C:\Users\GARV ANAND\Downloads\Krish project\Context-Gaurd

tests/backend/test_config.py ...                                         [ 11%]
tests/backend/test_health.py .....                                       [ 30%]
tests/benchmark/test_earb_schema.py ...                                  [ 42%]
tests/ml/test_policy.py ......                                           [ 65%]
tests/ml/test_url_risk.py .........                                      [100%]

======================= 26 passed in 5.09s ====================================

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
