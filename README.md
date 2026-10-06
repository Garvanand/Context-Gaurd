# ContextGuard
**A Multimodal AI System for Pre-Action Risk Detection in Everyday Digital Tasks**

*Final-Year B.Tech Capstone Project*

---

## 🎯 The Research Hypothesis

> **The risk of a digital artifact cannot always be determined from the artifact alone.**  
> **The intended action and surrounding context can change the appropriate safety intervention.**

ContextGuard evaluates:
$$\text{Artifact} + \text{Context} + \text{Intended Action} + \text{Evidence} + \text{Consequence} + \text{Uncertainty}$$

And maps the resulting risk score to exactly one definitive safety intervention:
- **ACT** (Green): Low-risk, proceed immediately.
- **ASK** (Yellow): Ambiguous context or low confidence, confirm with user.
- **WARN** (Orange): Elevated risk, display clear consequence warning.
- **STOP** (Red): Critical, irreversible risk, block action (user overridable).

---

## 🏗️ Repository Architecture

```text
/
├── android/            # Native Kotlin + Jetpack Compose Android Client
│   ├── app/            # Domain, UI (8 Screens), Components, ViewModel
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradlew.bat     # Official Gradle 8.3 Wrapper
├── backend/            # FastAPI Python 3.12+ Backend
│   ├── app/
│   │   ├── api/v1/     # REST Endpoints (/health, /health/models, /health/version)
│   │   ├── core/       # Pydantic Settings, Structured JSON Logging
│   │   └── main.py     # Application entry point with CORS & Lifespan
│   └── requirements.txt
├── ml/                 # Genuine Machine Learning Architecture
│   ├── datasets/       # Dataset pipelines (PhiUSIIL Phishing URL)
│   ├── features/       # 35+ URL Lexical & Structural Feature Extractor
│   ├── training/       # XGBoost Training & Validation
│   ├── inference/      # Abstract Interfaces: URLRiskModel, VisionReasoner, ArtifactAnalyzer, PolicyEngine
│   ├── artifacts/      # Exported weights & metadata
│   └── evaluation/     # Metrics, ROC/PR Curves
├── benchmark/          # Everyday Action Risk Benchmark (EARB)
│   ├── schema/         # Pydantic EARB Schema & CLI Validator
│   ├── data/           # Seed datasets (sample_earb_pairs.json)
│   ├── annotations/    # Annotator agreement records
│   ├── scripts/        # Benchmark runners
│   └── evaluation/     # Baseline evaluators
├── tests/              # End-to-End Automated Test Suites
│   ├── backend/        # Health & Config tests
│   ├── ml/             # Deterministic Policy Engine tests
│   └── benchmark/      # Schema validation tests
├── docs/               # Research, API, and System Documentation
├── PROJECT_RULES.md    # 22 Engineering Rules & Integrity Guidelines
├── PROJECT_STATUS.md   # System Status, Test Logs & Deliverables
├── ARCHITECTURE.md     # Full Architectural Specification & Mermaid Dataflow
├── DEVELOPMENT.md      # Comprehensive Local Operations & Run Guide
├── THREAT_MODEL.md     # Threat Analysis & STRIDE Boundaries
├── PRIVACY.md          # On-Device Redaction Protocol & Guarantees
├── DEMO_RUNBOOK.md     # Viva Demonstration Guide (Central Bank Statement Demo)
└── EXPERIMENTS.md      # Research Experiment Protocols & Baselines
```

---

## ⚡ Quickstart & Setup Instructions

### Prerequisites
- **Python 3.11+** (Detected: Python 3.12.0)
- **Java JDK 17 LTS** (Detected: OpenJDK 17.0.6 at `C:\Program Files\Android\Android Studio\jbr`)
- **Android SDK API 34** (Installed at `C:\Users\GARV ANAND\AppData\Local\Android\Sdk`)
- **Gradle 8.3** (Bundled via `android/gradlew.bat`)

---

### 1. Backend Service Setup & Execution
```powershell
# Navigate to backend directory and install dependencies
cd backend
python -m pip install -r requirements.txt

# Start the local FastAPI server
python -m uvicorn backend.app.main:app --host 0.0.0.0 --port 8000 --reload
```

Endpoints available:
- Health Check: `http://localhost:8000/health`
- ML Subsystems Health: `http://localhost:8000/health/models`
- Version Telemetry: `http://localhost:8000/health/version`
- OpenAPI Documentation: `http://localhost:8000/docs`

---

### 2. Running Automated Tests & Benchmark Validation
```powershell
# Run backend, ML policy, and schema unit tests (17 passed)
python -m pytest tests/ -v

# Run the EARB dataset schema validator
python -m benchmark.schema.validator benchmark/data/sample_earb_pairs.json
```

---

### 3. Android Application Build & Test
```powershell
# Set Java 17 for Gradle execution
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'

# Navigate to android directory
cd android

# Run Android unit tests (Passed)
.\gradlew.bat testDebugUnitTest

# Assemble debug APK (15.8 MB APK generated in build/outputs/apk/debug/)
.\gradlew.bat assembleDebug
```

---

## 🛡️ Core Architectural Features

1. **Native Jetpack Compose Client:** Single-Activity architecture with Material 3 dark graphite theme, ViewModel state management, and 8 dedicated screens:
   - `WelcomeScreen`: System hypothesis and entry point.
   - `HomeScreen`: Dashboard overview and workspace launcher.
   - `AnalyzeScreen`: Artifact inspection, local PII masking toggle, and action selection.
   - `ResultScreen`: Intervention banner, risk score breakdown, evidence cards, and user override.
   - `PrivacyScreen`: Zero raw persistence guarantees and network audit log preview.
   - `SettingsScreen`: Backend endpoint configuration and inference mode selection.
   - `DemoScreen`: Central Viva Demo walkthrough (Bank Statement across ACT, WARN, STOP).
   - `SupervisorScreen`: Examiner live telemetry panel exposing all 19 viva inspector metrics.
2. **Deterministic Mathematical Policy:**
   $$\rho = s \times (1 + \lambda \times r)$$
   Strictly enforces the **Model Failure Safety Rule**: malformed AI outputs never silently yield `ACT`; they safely fall back to `ASK`.
3. **EARB Benchmark Harness:** Formal Pydantic schema and CLI validator for 60 action-conditioned artifact pairs across Financial, Digital Security, Privacy Disclosure, and Communication categories.
