# ContextGuard Engineering Rules & Operating Principles

ContextGuard is a rigorous final-year B.Tech Capstone research engineering system. These rules govern every phase of planning, development, evaluation, and documentation.

## Core Rules

1. **Inspect before editing.** Always inspect existing files, dependencies, and environment status before modifying or creating code.
2. **Never overwrite working code blindly.** When refactoring or adding features, preserve existing functional guarantees and non-conflicting logic.
3. **Prefer small composable modules.** Keep architectural layers decoupled: UI, domain logic, policy engine, perception, feature extraction, and inference.
4. **No unnecessary frameworks.** Use minimal, robust libraries. Keep overhead lean and deterministic.
5. **No LangChain unless there is a compelling technical reason.** Direct HTTP/SDK client calls to Ollama/VLM and structured JSON parsing are preferred for determinism, testability, and speed.
6. **No fake AI.** All ML components must execute real models (ML Kit OCR/face detection, trained XGBoost model, real local VLM via Ollama/Transformers).
7. **No fake benchmark numbers.** All benchmark metrics reported must be computed from actual evaluation runs over test sets and EARB pairs. If an experiment has not run, report "Not evaluated".
8. **No fake network activity.** Network calls, telemetry, and request logs must reflect real requests between the Android client, local backend, and inference providers.
9. **No real sensitive data.** NEVER include real Aadhaar numbers, real PAN cards, real passwords, real OTPs, real financial accounts, or private persons' photos. All artifacts must be synthetic or consented academic fixtures.
10. **No hardcoded absolute local paths.** Use workspace-relative paths, environment variables, or standard Android/FastAPI asset loaders.
11. **Every environment-specific configuration must be documented.** Any dependency on JDK, Android SDK, Python versions, or hardware acceleration must be documented in `README.md` and `PROJECT_STATUS.md`.
12. **Add automated tests for core decision logic.** The deterministic policy engine, risk calculation, and fallback rules must be backed by thorough unit tests.
13. **Add API tests.** All FastAPI endpoints (health, analyze, policy, feedback, metrics) must have automated pytest test suites.
14. **Add Android unit tests for policy/domain behavior.** Ensure Android offline perception, redaction heuristics, and domain models have unit tests running via Gradle.
15. **Run builds after major phases.** Every phase must conclude with real build and test validation (`pytest`, `./gradlew test` / compilation).
16. **Fix build errors instead of reporting them.** When an error occurs, diagnose the root cause and resolve it directly.
17. **Maintain PROJECT_STATUS.md after every phase.** Document exact progress, completed deliverables, component statuses, and known issues after every milestone.
18. **Maintain ARCHITECTURE.md whenever architecture changes.** Architectural diagrams, data flows, and schemas must remain synchronized with the implementation.
19. **Use official/current documentation when package APIs have changed.** Verify breaking changes across libraries (FastAPI, Pydantic v2, XGBoost 3.x, AGP 8.x, Compose BOM).
20. **Prefer stable dependency versions compatible with the installed environment.** Lock versions to verified system runtimes (Python 3.12, Java 17 LTS, Android API 34).
21. **Do not tell me "this should work" without actually testing it.** Always execute tests, verify terminal output, and confirm exit codes.
22. **If a dependency cannot be installed, find a compatible alternative and document why.** If an external tool (e.g. Ollama) is unavailable, implement resilient, graceful degradation with deterministic fallbacks as mandated by the research contract.

## Research Contract Constraints

- **The Research Hypothesis:** Risk cannot be determined from the artifact alone; intended action and context dictate the appropriate safety intervention:
  $$\rho = s \times (1 + \lambda \times r)$$
  mapping to exactly one of $\{ \text{ACT}, \text{ASK}, \text{WARN}, \text{STOP} \}$.
- **Not a Generic Tool:** ContextGuard is NOT a generic chatbot, generic phishing scanner, or LLM wrapper. It is an **Action-Conditioned Pre-Action Digital Safety System**.
- **Model Failure Safety Rule:** If any AI model output is missing, malformed, unparseable, or inconsistent, the system must NEVER silently return `ACT`. It must return `ASK` or a safe fallback.
- **Privacy Assurance:** On-device redaction and hashing are standard. Raw artifacts must never leave the device unless explicitly in raw evaluation mode.
