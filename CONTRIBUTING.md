# Contributing to ContextGuard

Thank you for your interest in contributing to **ContextGuard**!  
ContextGuard is an open-source, action-conditioned multimodal AI safety system designed to detect and prevent digital risks on mobile devices before harmful actions are executed.

We welcome contributions from researchers, software engineers, machine learning practitioners, and designers.

---

## Code of Conduct

We are committed to providing a welcoming, inclusive, and harassment-free environment for all contributors. Please treat everyone with respect, professionalism, and constructive feedback.

---

## Core System Invariants (Must Read Before Contributing)

Before modifying code in this repository, you must be aware of the core engineering and security invariants:

1. **The Model Failure Safety Rule:**
   - AI models and parsers *will* fail. In ContextGuard, a model failure, timeout, or schema anomaly must **NEVER** silently yield `ACT` (safe).
   - Incomplete evidence or model unavailability must safely degrade to `ASK` with conservative fallback defaults.
2. **Action-Conditioned Risk Precedence:**
   - An artifact's intrinsic content (e.g. text, image) alone cannot dictate safety. The risk is dynamically conditioned on the intended action ($\text{SAVE}$ vs $\text{SEND}$ vs $\text{POST}$) and destination.
3. **Zero Raw Persistence / Privacy Filter:**
   - Raw user credentials, OTPs, full credit card numbers, and unredacted sensitive screenshots must **NEVER** be persisted to disk or emitted in unredacted network logs.
   - Any telemetry sent to the relay backend must pass through `enforce_privacy_or_raise` in `backend/app/relay/privacy_filter.py`.
4. **Honest Presence & Metrics:**
   - Never simulate or fake connected hardware statuses or benchmark results. Physical device status is only reported as connected when verified through genuine ADB/network sessions.

---

## Development Workflow

### 1. Fork and Clone
```bash
git clone https://github.com/Garvanand/Context-Gaurd.git
cd Context-Gaurd
```

### 2. Create a Feature Branch
Use descriptive branch names following our convention:
- `feature/description` for new capabilities
- `fix/description` for bug fixes
- `refactor/description` for code refactoring
- `docs/description` for documentation improvements
- `benchmark/description` for datasets and evaluations

```bash
git checkout -b feature/your-feature-name
```

### 3. Setup the Local Environment

#### Backend (Python 3.11+)
```powershell
python -m venv .venv
# Windows:
.venv\Scripts\Activate.ps1
# Linux/macOS:
# source .venv/bin/activate

pip install -r backend/requirements.txt
```

#### Web Dashboard (Node.js 18+)
```powershell
cd supervisor-dashboard
npm install
npm run dev
```

#### Android Client (Android Studio & JDK 17)
```powershell
cd android
# Set JAVA_HOME if not configured globally:
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
```

---

## Coding Standards

### Python (`backend/`, `ml/`, `benchmark/`, `tests/`)
- Follow **PEP 8** style guidelines.
- Use explicit type hints for all function arguments and return values.
- Validate all incoming API request payloads with **Pydantic V2** schemas.
- Write docstrings for classes and core mathematical algorithms.
- Ensure all tests pass with `python -m pytest tests/ -v`.

### Kotlin & Android (`android/`)
- Follow the official **Kotlin Style Guide** and Jetpack Compose best practices.
- State management belongs in `ViewModel`s; keep Compose screens declarative.
- Heavy operations (OCR, PDF rendering, encryption, network calls) must run off the main thread using Kotlin Coroutines (`Dispatchers.IO` or `Dispatchers.Default`).
- Ensure unit tests pass with `.\gradlew.bat testDebugUnitTest`.

### TypeScript & React (`supervisor-dashboard/`)
- Strict TypeScript (`"strict": true` in `tsconfig.json`).
- Prefer functional components with React Hooks.
- Ensure production build passes with `npm run build` (`tsc -b && vite build`) without any type warnings or errors.

---

## Submitting Pull Requests

1. **Run the Full Test Suite Locally:**
   ```powershell
   # 1. Run all backend & integration tests
   python -m pytest tests/ -v

   # 2. Run Android unit tests
   cd android && .\gradlew.bat testDebugUnitTest && cd ..

   # 3. Build Supervisor Dashboard
   cd supervisor-dashboard && npm run build && cd ..
   ```
2. **Commit Your Changes:**
   Write clear, concise commit messages using [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat(relay): add auto-reconnect backoff to WebSocket client`
   - `fix(pipeline): handle missing OCR text in Stage 1 gracefully`
   - `test(backend): add coverage for pairing code expiration`
   - `docs(readme): clarify local setup prerequisites`
3. **Push to Your Fork & Open a Pull Request:**
   - Provide a concise description of the change, why it's needed, and how it was tested.
   - Reference any related issue numbers (e.g., `Closes #12`).
   - Include test outputs or screenshots for UI modifications.

---

## Reporting Issues

If you discover a bug, security vulnerability, or unexpected behavior:
1. Search existing issues to verify it hasn't been reported yet.
2. File a new GitHub Issue with:
   - Operating system and version (e.g. Windows 11, Ubuntu 22.04).
   - Component affected (`Android Client`, `FastAPI Backend`, `Supervisor Web`, `ML Pipeline`).
   - Clear reproduction steps.
   - Expected vs actual behavior.
   - Relevant error logs or stack traces.

---

## License

By contributing to ContextGuard, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).
