# Mobile Protection Gap Analysis & Safe Migration Plan

**Document Version:** 1.0.0  
**Date:** 2026-10-09  
**Auditor:** Independent Principal Systems & Security Engineer  
**Status:** Pre-Migration Architecture Specification  

---

## 1. Executive Summary: The Core Architectural Mismatch

The forensic audit confirms that ContextGuard operates exclusively as an **on-demand Android Sharesheet receiver** (`ACTION_SEND` and `ACTION_SEND_MULTIPLE`).

While its perception, redaction, URL classification, and policy gating modules are fully verified and functional, **it currently provides zero real-time, event-driven, or background mobile protection**.

### Sharesheet vs. Event-Driven Mobile Protection

```
CURRENT (Sharesheet Pull Architecture):
[User Action in App X] -> [User explicitly taps Share] -> [User selects ContextGuard] -> [Full-screen Activity opens] -> [User selects action chip] -> [User taps Analyze] -> [Result displayed in ContextGuard UI]
* Defect: Relies on user suspicion prior to taking action. Zero protection against active phishing, credential submission, or malicious clipboard paste if the user does not manually trigger share.

TARGET (Event-Driven Mobile Protection Architecture):
[User interacts with App X] 
       │
       ▼ (Accessibility Event: Click / Window Change / Text Input)
[ContextGuard AccessibilityService] ──► [Allowlist Filter]
       │
       ▼ (Extract Window Nodes / Active URL / Input Text)
[On-Device PII & URL Gating] ──► [Policy Engine Evaluates Risk]
       │
       ▼ (If Risk >= ASK / WARN / STOP)
[Floating System Alert Overlay over App X] (Blocks or warns user BEFORE action commits)
```

---

## 2. Forensic Evaluation of the Nine Gap Criteria

| # | Protection Criterion | Current State | Evidence from Source Code & Manifest | Gap Status |
| :-: | :--- | :--- | :--- | :---: |
| **1** | **Configured AccessibilityService** | Completely Absent | Zero `<service>` tags in `AndroidManifest.xml`. No class extending `AccessibilityService`. No `accessibility_service_config.xml`. | **CRITICAL GAP** |
| **2** | **Monitoring Permission & In-App Consent** | Completely Absent | Manifest contains only `INTERNET` and `ACCESS_NETWORK_STATE`. No consent flow or rationale dialog explaining accessibility data access. | **CRITICAL GAP** |
| **3** | **Selected-App Allowlist** | Completely Absent | `SettingsScreen.kt` manages only backend host/port. No package name allowlist or denylist exists in `core/` or `ui/`. | **GAP** |
| **4** | **UI-Event Processing** | Completely Absent | Zero event handlers for `AccessibilityEvent.TYPE_VIEW_CLICKED`, `TYPE_VIEW_TEXT_CHANGED`, or `TYPE_WINDOW_STATE_CHANGED`. | **CRITICAL GAP** |
| **5** | **Accessible-Window Inspection** | Completely Absent | Zero usage of `AccessibilityNodeInfo`, `rootInActiveWindow`, or view hierarchy traversal. Cannot read browser address bars or form fields. | **CRITICAL GAP** |
| **6** | **Working Intervention Overlay** | Completely Absent | Interventions render only inside ContextGuard's own `ResultScreen.kt`. No `SYSTEM_ALERT_WINDOW` permission, no `WindowManager.addView()`. | **CRITICAL GAP** |
| **7** | **NotificationListenerService** | Completely Absent | No service bound to `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`. Incoming SMS, OTPs, or banking notifications cannot be intercepted. | **GAP** |
| **8** | **Persistent Monitoring Status** | Completely Absent | When the user exits `MainActivity`, the process is backgrounded or killed. No ongoing foreground notification or status bar icon. | **GAP** |
| **9** | **Immediate Disable / Kill Switch** | Completely Absent | No persistent quick tile, notification action, or global toggle to immediately pause event capture and release accessibility interception. | **GAP** |

---

## 3. Deep Dive into the Gap Criteria

### 3.1. Configured AccessibilityService
- **Android Requirement:** In order for an application to observe actions across other apps, it must declare a `<service>` in `AndroidManifest.xml` protected by `android.permission.BIND_ACCESSIBILITY_SERVICE` and point to an XML meta-data configuration file defining event types (`typeViewClicked`, `typeWindowStateChanged`, `typeViewFocused`).
- **Codebase Reality:** `android/app/src/main/AndroidManifest.xml` only defines `MainActivity`. There are no background services of any kind.
- **Consequence:** The app cannot receive callbacks when a user is about to click "Transfer", "Submit", or "Send" in WhatsApp, Chrome, or Gmail.

### 3.2. Permission and In-App Consent Flow
- **Android Requirement:** Google Play policy and Android 13+ (API 33+) require explicit user education and consent before directing the user to `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
- **Codebase Reality:** `WelcomeScreen.kt` and `HomeScreen.kt` do not mention accessibility permissions or background monitoring consent.
- **Consequence:** Even if the service is declared, users are never guided through granting accessibility access.

### 3.3. Selected-App Allowlist
- **Android Requirement:** Inspecting every application on a user's phone induces massive CPU/battery drain and severe privacy violations. An event-driven security engine must target high-risk application packages (e.g., browsers: `com.android.chrome`, `org.mozilla.firefox`; messaging: `com.whatsapp`, `org.telegram.messenger`).
- **Codebase Reality:** The app lacks any package manager filter or user-configurable allowlist.

### 3.4. UI-Event Processing & Window Inspection
- **Android Requirement:** The service must override `onAccessibilityEvent(event: AccessibilityEvent)`. When an event occurs, it must inspect `rootInActiveWindow` or `event.source`, traverse the `AccessibilityNodeInfo` tree to find:
  - Editable text fields (detecting PII / credentials being entered).
  - Web address bars (detecting phishing URLs before navigation).
  - Action buttons labeled "Send", "Transfer", "Login", "Approve", or "Post".
- **Codebase Reality:** Neither `AccessibilityEvent` nor `AccessibilityNodeInfo` is referenced anywhere in the repository.

### 3.5. Working Intervention Overlay
- **Android Requirement:** When high risk is detected ($\rho \ge 0.65$ or $\text{STOP}$), the application must display an overlay on top of the target app via `WindowManager` using `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
- **Codebase Reality:** Interventions are rendered inside Compose `ResultScreen.kt`, which only shows after the user manually initiates analysis from `AnalyzeScreen.kt`. If the user is in Chrome, ContextGuard cannot overlay a warning.

### 3.6. Persistent Status & Kill Switch
- **Android Requirement:** Users must always know when background monitoring is active. Best practices require a foreground service with a persistent notification (e.g., "ContextGuard Active: Protecting Chrome & WhatsApp") and an immediate action button ("Pause Protection" / "Disable").
- **Codebase Reality:** Zero foreground service implementation exists.

---

## 4. Safe Migration Sequence

### Preservation Invariant
The migration must strictly preserve existing, verified working components:
1. **Preserve Sharesheet Receiver:** Keep `ACTION_SEND` and `SharesheetPayloadResolver` intact as an alternative on-demand ingestion channel.
2. **Preserve Perception Engine:** Reuse `MlKitPerceptionEngine`, `PiiDetector`, and `RedactionEngine`.
3. **Preserve Policy & URL Models:** Reuse `URLRiskService`, `DeterministicPolicyEngine`, and `ContextGuardPipeline`.
4. **Preserve Benchmark v1:** Zero modifications to `benchmark/data/earb_v1.jsonl` or evaluation scripts.
5. **Preserve Dashboard:** Maintain compatibility with `supervisor-dashboard/` and existing REST endpoints.

---

### Migration Roadmap: The Smallest Safe Path

```
                    ┌────────────────────────────────────────┐
                    │    Phase M0: Audit & Architecture      │  <-- CURRENT STAGE
                    │   (Forensic Audit & Gap Docs Complete) │
                    └──────────────────┬─────────────────────┘
                                       │
                                       ▼
                    ┌────────────────────────────────────────┐
                    │ Phase M1: Lifecycle, Status & Consent  │
                    │ • Foreground Service + Persistent HUD  │
                    │ • Kill-Switch Toggle in Notification   │
                    │ • In-App Consent & Permission Flow     │
                    └──────────────────┬─────────────────────┘
                                       │
                                       ▼
                    ┌────────────────────────────────────────┐
                    │ Phase M2: Minimal AccessibilityService │
                    │ • Package Allowlist (Chrome, WhatsApp) │
                    │ • Event Filter (CLICK, WINDOW_CHANGE)  │
                    │ • Debounced Event Dispatcher           │
                    └──────────────────┬─────────────────────┘
                                       │
                                       ▼
                    ┌────────────────────────────────────────┐
                    │ Phase M3: Window Node Extraction       │
                    │ • Browser URL Bar Extraction           │
                    │ • Input Field & Button Action Resolver │
                    │ • Feed into ContextGuardApiClient      │
                    └──────────────────┬─────────────────────┘
                                       │
                                       ▼
                    ┌────────────────────────────────────────┐
                    │ Phase M4: Non-Intrusive Overlay System │
                    │ • SYSTEM_ALERT_WINDOW floating badge   │
                    │ • Compact ACT/ASK/WARN/STOP pill       │
                    │ • Tap to expand full Decision Trace    │
                    └──────────────────┬─────────────────────┘
                                       │
                                       ▼
                    ┌────────────────────────────────────────┐
                    │ Phase M5: End-to-End Validation        │
                    │ • Live Phishing URL Interception Test  │
                    │ • Demo Scenarios 1-10 on Device/AVD    │
                    │ • Latency & Battery Impact Benchmark   │
                    └────────────────────────────────────────┘
```

#### Detailed Phase Breakdown

#### Phase M1: Monitoring Lifecycle, Consent Flow & Kill Switch
* **Goal:** Establish user transparency, Android foreground service lifecycle, and immediate user control without touching core logic.
* **Additions:**
  - `android/app/src/main/java/com/contextguard/app/core/service/ProtectionForegroundService.kt`:
    - Persistent notification with icon and text: "ContextGuard Active: Monitoring Protected Apps".
    - Two notification action buttons: `[Pause Protection]` (kill-switch) and `[Open Console]`.
  - Permission declarations in `AndroidManifest.xml`:
    - `FOREGROUND_SERVICE` and `POST_NOTIFICATIONS`.
  - In-app consent card in `HomeScreen.kt` / `SettingsScreen.kt` explaining why monitoring is used and offering an instant toggle switch.

#### Phase M2: Minimal AccessibilityService with Target Allowlist
* **Goal:** Intercept UI events only from explicitly approved applications without degrading device performance.
* **Additions:**
  - `android/app/src/main/res/xml/accessibility_service_config.xml`:
    - Event types: `typeWindowStateChanged`, `typeViewClicked`.
    - Feedback type: `feedbackGeneric`.
    - Flags: `flagReportViewIds`, `flagRetrieveInteractiveWindows`.
  - `android/app/src/main/java/com/contextguard/app/core/service/ContextGuardAccessibilityService.kt`:
    - Maintains a hardcoded default allowlist:
      - `com.android.chrome` (Google Chrome)
      - `org.mozilla.firefox` (Firefox)
      - `com.whatsapp` (WhatsApp)
      - `com.google.android.gm` (Gmail)
    - Ignores all events from non-allowlisted packages immediately.
    - Debounce timer (300ms) to prevent event flooding.

#### Phase M3: Accessible Window Inspector & Context Resolver
* **Goal:** Convert live accessibility node hierarchies into existing `AnalysisRequestPayload` objects.
* **Additions:**
  - `android/app/src/main/java/com/contextguard/app/core/accessibility/WindowNodeInspector.kt`:
    - Extracts URL from browser address bar (e.g., node with id `com.android.chrome:id/url_bar`).
    - Extracts text from focused edit texts.
    - Identifies clicked button labels (e.g., "Send", "Pay", "Submit", "Sign").
  - Maps extracted window context directly to `selected_action`, `destination`, `recipient`, and `url`.
  - Invokes existing `ContextGuardApiClient.analyze()` or local `PiiDetector` + `URLRiskService`.

#### Phase M4: System Alert Intervention Overlay
* **Goal:** Provide immediate visual feedback directly over the active application.
* **Additions:**
  - Permission: `SYSTEM_ALERT_WINDOW`.
  - `android/app/src/main/java/com/contextguard/app/ui/overlay/InterventionOverlayManager.kt`:
    - Displays a floating glassmorphic badge in the top-right corner of the screen using `WindowManager`.
    - Colors and states match existing theme:
      - Green pill: `ACT` ("Safe")
      - Blue pill: `ASK` ("Confirm recipient")
      - Amber pill: `WARN` ("Caution: Unverified domain")
      - Red full-width banner: `STOP` ("High Risk: Phishing / Data Leakage Detected")
    - Clicking the pill opens a compact modal with evidence and "Change Action" / "Proceed Anyway" options.

#### Phase M5: End-to-End Validation & Hardening
* **Goal:** Verify that background interception works without regressions.
* **Execution & Verification:**
  - Test browser navigation: Open phishing test URL in Chrome $\rightarrow$ Verify overlay appears with `STOP`.
  - Test safe browsing: Open routine news URL $\rightarrow$ Verify overlay displays `ACT` or disappears cleanly.
  - Test kill-switch: Tap "Pause Protection" in notification $\rightarrow$ Verify zero events processed and overlay immediately removed.
  - Verify all 28 existing Android unit tests and 76 backend tests continue to pass with zero modifications.

---

## 5. Auditor Sign-Off & Constraints

1. **Strict Ordering:** No code changes for Phase M1–M5 shall begin until the forensic audit (`docs/FORENSIC_AUDIT.md`) and gap specification (`docs/MOBILE_PROTECTION_GAP.md`) are finalized and reviewed.
2. **Benchmark Integrity:** The existing EARB v1 benchmark and published evaluation tables in `results/` shall remain untouched.
3. **User Experience:** The existing Sharesheet receiver flow will remain active as a fallback mode.
