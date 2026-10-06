# ContextGuard: Project Status Report

**Capstone Title:** CONTEXTGUARD: A Multimodal AI System for Pre-Action Risk Detection in Everyday Digital Tasks  
**Institution:** Final-Year B.Tech Capstone Project  
**Status Date:** Phase 0 (Environment Audit & Architectural Foundation) Completed  
**Current Phase:** Transitioning to Phase 1 (Data Acquisition, ML Feature Extraction & XGBoost URL Model Training)

---

## 1. System Environment Audit

| Component | Target / Required | Detected System Environment | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Operating System** | Windows 10/11 64-bit | Windows 11 Home Single Language (10.0.26200) | **PASS** | 64-bit architecture |
| **System RAM** | 8+ GB | 12.0 GB Physical RAM (~1.6 GB free at idle) | **PASS** | Sufficient for local Python services & XGBoost; VLM requires quantization or Ollama |
| **Available Storage** | 20+ GB | 51.15 GB Free on C: Drive | **PASS** | Adequate for Android SDK, ML weights, datasets |
| **Git** | 2.x | Git 2.42.0.windows.2 | **PASS** | Functional |
| **Python** | 3.11+ | Python 3.12.0 (`C:\Users\GARV ANAND\AppData\Local\Programs\Python\Python312\python.exe`) | **PASS** | Python 3.12 verified |
| **Node.js / npm** | Node 18+, npm 9+ | Node v20.14.0, npm 10.9.0 | **PASS** | Available for dashboard/tooling |
| **Java JDK** | JDK 17 LTS | - OpenJDK 17.0.6 (Android Studio JBR at `C:\Program Files\Android\Android Studio\jbr`)<br>- JDK 22.0.2 at `C:\Program Files\Java\jdk-22` | **PASS** | JAVA_HOME set to JDK 17 for AGP/Gradle |
| **Android SDK** | API 34+ | Android SDK located at `C:\Users\GARV ANAND\AppData\Local\Android\Sdk`<br>- Platforms: `android-34`, `android-36`<br>- Build-tools: `34.0.0`, `36.1.0`, `37.0.0`<br>- Platform-tools: `adb 1.0.41 (37.0.1)` | **PASS** | Target API 34 supported out-of-the-box |
| **Gradle** | 8.x | Gradle 8.3 (Cached at `C:\Users\GARV ANAND\.gradle\wrapper\dists\gradle-8.3-bin\...`) | **PASS** | Verified working with Java 17 and Kotlin 1.9 |
| **Ollama** | Local LLM/VLM runtime | Not found in PATH / Port 11434 inactive | **FALLBACK READY** | System architecture includes resilient offline mode, heuristic fallback, and API abstraction |
| **Internet Access** | PyPI, HuggingFace, UCI | Tested and Verified (HTTP 200 to httpbin, UCI PhiUSIIL archive endpoint accessible) | **PASS** | Package & dataset downloads enabled |

### ML Environment Status (Python 3.12)
- **FastAPI**: `0.141.1` (INSTALLED)
- **Pydantic**: `2.13.4` (INSTALLED)
- **Uvicorn**: `0.52.0` (INSTALLED)
- **XGBoost**: `3.2.0` (INSTALLED)
- **Scikit-learn**: `1.4.2` (INSTALLED)
- **PyTorch**: `2.13.0+cpu` (INSTALLED)
- **Transformers**: `5.17.0` (INSTALLED)
- **Pandas**: `3.0.3` (INSTALLED)
- **NumPy**: `1.26.4` (INSTALLED)
- **Pillow**: `12.2.0` (INSTALLED)
- **OpenCV (headless)**: `5.0.0.93` (INSTALLED)
- **Pytest**: `9.0.3` (INSTALLED)

---

## 2. Component Implementation Status

| Component | Status | Description |
| :--- | :--- | :--- |
| **Repository Structure** | **COMPLETED** | Monorepo structured with `android`, `backend`, `ml`, `benchmark`, `dashboard`, `artifacts`, `docs`, `scripts`, `tests`. |
| **Documentation & Rules** | **COMPLETED** | `PROJECT_RULES.md`, `ARCHITECTURE.md`, `PROJECT_STATUS.md`, `THREAT_MODEL.md`, `PRIVACY.md`, `DEMO_RUNBOOK.md`, `EXPERIMENTS.md` initialized. |
| **Backend Service** | **PLANNED** | FastAPI server with deterministic policy engine, PII redaction pipeline, XGBoost inference service, VLM client, and health endpoints. |
| **ML Phishing Model** | **PLANNED** | Feature extraction pipeline (35+ lexical/structural features), training on PhiUSIIL dataset, evaluation (AUC, F1, latency), model export. |
| **EARB Benchmark** | **PLANNED** | 60 synthetic/consented artifact-action pairs across 4 categories with rigorous schema, negative controls, and evaluation harness. |
| **Android Application** | **PLANNED** | Jetpack Compose app with Sharesheet interception, ML Kit OCR & Face detection, local PII redaction, Supervisor mode, and Central Demo. |
| **Evaluation Suite** | **PLANNED** | Baselines B1–B5, ablations, confusion matrices, STOP recall, false-alarm analysis, latency benchmarking. |

---

## 3. Immediate Next Milestones
1. **Phase 1: ML Model & Feature Extraction Engine**
   - Acquire/stream PhiUSIIL dataset or balanced subset.
   - Implement deterministic URL lexical/structural feature extractor (exact parity between training and inference).
   - Train XGBoost phishing classifier and evaluate metrics.
   - Save model artifact (`artifacts/models/xgboost_phishing_v1.json`).
2. **Phase 2: EARB Benchmark Creation & Validation**
   - Create 20 synthetic base artifacts across 4 categories (Financial, Digital Security, Privacy Disclosure, Communication).
   - Generate 60 artifact-action pairs adhering to EARB schema.
   - Provide Central Demo bank statement artifact with 3 distinct action outcomes (ACT, WARN, STOP).
3. **Phase 3: FastAPI Backend & Deterministic Policy Engine**
   - Implement risk calculation: $\rho = s \times (1 + \lambda \times r)$.
   - Implement policy logic ($STOP, ASK, WARN, ACT$) with strict Model Failure Safety Rule.
   - Connect XGBoost inference, local VLM client, and health endpoints.
4. **Phase 4: Android Application (Compose + ML Kit)**
   - Build Android app with ML Kit OCR & Face Detection.
   - Implement on-device PII masking and network request auditing.
   - Integrate ACTION_SEND Sharesheet handler and Supervisor viva mode.
