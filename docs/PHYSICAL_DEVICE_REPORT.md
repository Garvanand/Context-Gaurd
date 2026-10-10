# ContextGuard Physical Device & Runtime Environment Audit Report

**Audit Date:** 2026-10-10  
**Audit Standard:** Zero-Trust Empirical Verification  
**Auditor:** Mobile Platform Quality Lead  

---

## 1. Physical Device & Emulator Discovery Log

The device discovery probe was executed via the Android platform-tools ADB binary:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l
```

### Command Output:
```text
List of devices attached

```

### Audit Finding:
* **No physical Android hardware** (USB / Wi-Fi debugging) was connected to the host workstation at time of execution.
* **No Android Virtual Device (AVD) emulator instance** was currently running.
* **Empirical Status:** **ACTUAL PHYSICAL-DEVICE RUNTIME BEHAVIOR IS MARKED UNVERIFIED.**

> [!WARNING]
> In strict accordance with release engineering protocol, ContextGuard will **never** claim physical device tests have passed without real hardware execution proof. All on-device validation claims are explicitly marked **UNVERIFIED on physical hardware** until executed on a connected test unit.

---

## 2. Automated Verification Executed in Place of Hardware

To establish the highest possible degree of assurance short of an attached physical handset, the entire build and testing pipeline was executed against real compilation targets and simulated Android components:

| Verification Layer | Tool / Command | Result | Assurance Provided |
| :--- | :--- | :---: | :--- |
| **Android Unit Tests** | `./gradlew testDebugUnitTest` | **BUILD SUCCESSFUL (22 tasks, 100% pass)** | Verifies ViewModel state machines, Sharesheet payload parsing, URL tree inference engine, PII regex scanning, canvas redaction, and overlay manager logic. |
| **Android DEX & APK Packaging** | `./gradlew assembleDebug` | **BUILD SUCCESSFUL (35 tasks, 23s)** | Verifies byte-level compilation, AndroidManifest.xml validation, resource merging, Proguard/R8 rules, and asset bundle packaging (`app-debug.apk`). |
| **On-Device Model Assets** | Asset verification | **VERIFIED** | `url_model_portable.json` (761 KB) and `message_model_portable.json` (281 KB) are correctly packaged inside `app/src/main/assets/models/`. |
| **Backend & Policy Suite** | `pytest tests/` | **PASSED (90/90 passed)** | Verifies deterministic risk math $\rho = s \cdot (1 + \lambda r)$, Ollama fallback, and EARB benchmark integrity. |

---

## 3. Physical Device Verification Runbook (For Connected Handsets)

When a physical Android handset (API 31+) is connected via USB debugging, execute the following verification steps:

### Step 1: Install Debug Build
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r android/app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Grant Permissions via ADB (or Manual UI)
```powershell
# Launch ContextGuard Home Screen
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.contextguard.app/.MainActivity

# Verify that ScreenGuard Accessibility Service is recognized
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell settings put secure enabled_accessibility_services com.contextguard.app/com.contextguard.app.core.service.ScreenGuardAccessibilityService
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell settings put secure accessibility_enabled 1
```

### Step 3: Verify Sharesheet Ingestion
```powershell
# Send test URL share intent
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "https://phishing-domain.xyz/login" -p com.contextguard.app
```
* **Expected UI Behavior:**
  - ContextGuard opens directly to `AnalyzeScreen`.
  - Content preview displays normalized URL.
  - Action chips display `OPEN`, `SEND`, `COPY`.
  - Tapping `OPEN` executes inference automatically and navigates directly to `ResultScreen` displaying `STOP` or `WARN`.
  - No "Analyze Now" button is required.
