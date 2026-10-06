# ContextGuard: Development & Operations Guide

This guide details local engineering workflows, build instructions, test commands, and architectural validation procedures for ContextGuard.

---

## 1. Environment & Prerequisites

Ensure the following runtimes and toolchains are available:
- **Operating System:** Windows 11 64-bit / Linux / macOS
- **Python:** 3.11+ (Detected: 3.12.0)
- **Java JDK:** JDK 17 LTS (Detected: OpenJDK 17.0.6 at `C:\Program Files\Android\Android Studio\jbr`)
- **Android SDK:** API 34+ (Located at `C:\Users\GARV ANAND\AppData\Local\Android\Sdk`)
- **Gradle:** Gradle 8.3 (Wrapper included via `android/gradlew.bat`)

---

## 2. Backend Development (FastAPI)

### Setup & Dependencies
```powershell
# From the repository root
cd backend
python -m pip install -r requirements.txt
```

### Running Local Development Server
```powershell
python -m uvicorn backend.app.main:app --host 0.0.0.0 --port 8000 --reload
```

### Testing Endpoints
- **Root Status:** `GET http://localhost:8000/`
- **Health Check:** `GET http://localhost:8000/health` or `GET http://localhost:8000/api/v1/health`
- **ML Subsystems Health:** `GET http://localhost:8000/health/models`
- **Version Telemetry:** `GET http://localhost:8000/health/version`
- **OpenAPI Interactive Documentation:** `http://localhost:8000/docs`

---

## 3. Automated Test Execution

### Backend, ML Policy & Benchmark Tests
Run all pytest test suites from the repository root:
```powershell
python -m pytest tests/ -v
```

### Validating EARB Dataset Files
Validate any benchmark JSON file against the Pydantic schema:
```powershell
python -m benchmark.schema.validator benchmark/data/sample_earb_pairs.json
```

---

## 4. Android Client Development (Kotlin + Jetpack Compose)

The native Android client is located in the `android/` directory and utilizes a Single-Activity architecture with Jetpack Compose Material 3.

### Setting `JAVA_HOME`
Ensure `JAVA_HOME` points to JDK 17 prior to Gradle execution:
```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
```

### Running Android Unit Tests
```powershell
cd android
.\gradlew.bat testDebugUnitTest
```

### Compiling and Assembling Debug APK
```powershell
cd android
.\gradlew.bat assembleDebug
```
Output artifact generated at:
`android/app/build/outputs/apk/debug/app-debug.apk`

### Installing on Connected Device or Emulator
```powershell
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
```

---

## 5. Navigation & Screen Map

The Android application implements 8 dedicated screens managed by `NavGraph.kt`:
1. **WelcomeScreen (`welcome`):** Project title, research hypothesis, and system entry point.
2. **HomeScreen (`home`):** Dashboard overview, active inference mode badge, and workspace shortcuts.
3. **AnalyzeScreen (`analyze`):** Artifact preview, local PII masking toggle, intended action selector, and policy evaluation trigger.
4. **ResultScreen (`result`):** Intervention banner (ACT, ASK, WARN, STOP), risk score $\rho$ trace, evidence cards, and emergency user override.
5. **PrivacyScreen (`privacy`):** Zero raw persistence guarantee, edge perception details, and network audit log table.
6. **SettingsScreen (`settings`):** Host/port configuration, inference mode selector (OFFLINE, REDACTED_LOCAL_BACKEND, RAW_EVALUATION_ONLY).
7. **DemoScreen (`demo`):** Central Viva Demo walkthrough (Bank Statement evaluated across 3 actions: ACT, WARN, STOP).
8. **SupervisorScreen (`supervisor`):** Real-time examiner telemetry panel with intermediate ML activations and mathematical gating trace.

---

## 6. Monorepo Structure & File Organization

```text
Context-Gaurd/
├── android/            # Jetpack Compose Android Client
│   ├── app/            # Application module, UI, ViewModel, components
│   └── gradlew.bat     # Official Gradle 8.3 wrapper
├── backend/            # FastAPI Backend
│   ├── app/            # Core, Config, API v1, Logging
│   └── requirements.txt
├── ml/                 # Machine Learning Subsystems
│   ├── datasets/       # Dataset storage & pipelines
│   ├── features/       # 35+ URL feature extraction
│   ├── training/       # Model training scripts
│   ├── inference/      # Abstract interfaces (URLRiskModel, VisionReasoner, PolicyEngine)
│   ├── artifacts/      # Serialized models and weights
│   └── evaluation/     # Metric computation scripts
├── benchmark/          # Everyday Action Risk Benchmark (EARB)
│   ├── schema/         # Pydantic EARB schema & validator
│   ├── data/           # Seed datasets (sample_earb_pairs.json)
│   ├── annotations/    # Annotator agreement records
│   ├── scripts/        # Benchmarking runner scripts
│   └── evaluation/     # Baseline evaluator implementations
├── tests/              # End-to-end test suites
│   ├── backend/        # Health and config tests
│   ├── ml/             # Policy engine unit tests
│   └── benchmark/      # EARB schema validation tests
├── docs/               # Research & architecture documentation
└── artifacts/          # Project build outputs and models
```
