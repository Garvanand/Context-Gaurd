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
│   ├── app/            # Domain, UI, Edge Perception (ML Kit), Local Redactor
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── backend/            # FastAPI Python 3.12+ Backend
│   ├── app/
│   │   ├── api/v1/     # REST Endpoints (/analyze, /health, /policy, /audit)
│   │   ├── core/       # Config, Logging, Hash Verification
│   │   ├── models/     # Pydantic Schemas for Requests, Responses, Policy
│   │   ├── policy/     # Deterministic Policy Engine & Fallbacks
│   │   └── services/   # Inference Orchestrator, XGBoost Client, VLM Client
│   └── requirements.txt
├── ml/                 # Genuine Machine Learning Pipelines
│   ├── data/           # Dataset Download & Preprocessing (PhiUSIIL URL)
│   ├── features/       # 35+ URL Lexical & Structural Feature Extractor
│   ├── models/         # Serialized Model Artifacts
│   ├── training/       # XGBoost Training, Validation, and Calibration
│   └── evaluation/     # Metrics, Confusion Matrices, ROC/PR Curves
├── benchmark/          # Everyday Action Risk Benchmark (EARB)
│   ├── earb/           # 60 Action-Conditioned Pairs across 4 Categories
│   ├── evaluators/     # Baselines (B1, B3, B4, B5) & Ablation Framework
│   └── metrics/        # Evaluation Metric Computations (F1, ECE, STOP Recall)
├── dashboard/          # Supervisor & Evaluation Web Dashboard
├── docs/               # In-Depth Documentation
├── scripts/            # Automation & Run Scripts
├── artifacts/          # Generated Model Weights, Figures, and Logs
├── tests/              # End-to-End Automated Test Suites
├── PROJECT_RULES.md    # Engineering Rules & Integrity Guidelines
├── PROJECT_STATUS.md   # System Status and Environment Audit
├── ARCHITECTURE.md     # Full Architectural Specification
├── THREAT_MODEL.md     # Threat Analysis & Security Boundaries
├── PRIVACY.md          # Data Privacy Guarantees & Redaction Protocol
├── DEMO_RUNBOOK.md     # Step-by-Step Viva Demonstration Guide
└── EXPERIMENTS.md      # Research Experiment Protocols & Baselines
```

---

## ⚡ Quickstart

### Prerequisites
- **Python 3.11+** (Detected: 3.12.0)
- **Java 17 LTS** (Detected: OpenJDK 17.0.6)
- **Android SDK API 34** (Installed at `C:\Users\GARV ANAND\AppData\Local\Android\Sdk`)
- **Gradle 8.3** (Cached locally)

### 1. Backend Setup
```powershell
# Navigate to backend
cd backend
python -m pip install -r requirements.txt

# Run backend service
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 2. Train the XGBoost Phishing Model
```powershell
python -m ml.training.train_phishing
```

### 3. Run Benchmark Suite
```powershell
python -m benchmark.evaluators.run_benchmark
```

### 4. Build Android Application
```powershell
cd android
./gradlew assembleDebug
```

---

## 🛡️ The 3 AI/ML Layers

1. **Google ML Kit:** On-device OCR (Text Recognition) and Face Detection running completely locally before network transmission.
2. **Trained XGBoost Classifier:** Real machine learning model trained on the public **PhiUSIIL Phishing URL Dataset** using 35+ derived lexical and structural features.
3. **Qwen2.5-VL-3B-Instruct:** Multimodal vision-language reasoning for contextual hazard understanding and consequence estimation.

---

## 🔬 Central Demo: Action-Conditioning
The core demonstration uses a **single synthetic bank statement**:
1. **Save privately:** $\rho = 0.05 \implies$ **ACT**
2. **Send to unknown recipient:** $\rho = 0.62 \implies$ **WARN / ASK**
3. **Post publicly:** $\rho = 1.57 \implies$ **STOP**
