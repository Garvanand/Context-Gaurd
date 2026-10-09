# ContextGuard: Android Capability & Limitation Matrix

**Document Version:** 1.0.0  
**Date:** 2026-10-09  
**Role:** Staff Mobile-Security Architect  
**Classification:** Technical Capability Matrix & Boundary Constraints  

---

## 1. Executive Capability Matrix

This matrix establishes the strict operational boundaries of ContextGuard under standard Android user-space security constraints. It explicitly enumerates what can be observed, what can trigger an intervention, what remains unsupported, and the underlying OS security barrier.

| Application / UI Surface | Observable Elements | Can Trigger Overlay / Alert? | Unsupported / Blind Spots | Technical / OS Barrier |
| :--- | :--- | :---: | :--- | :--- |
| **Standard Web Browsers** (Chrome, Firefox, Edge, Samsung Internet) | • Address bar URL (`url_bar`, `toolbar`)<br>• Page title & domain<br>• Visible webpage accessibility text nodes<br>• Link click events | **YES**<br>(`WARN` / `STOP` overlay on malicious domain; `ACT` on benign) | • Private/Incognito URL (on some OEMs)<br>• WebGL/Canvas rendered graphics<br>• Obfuscated link redirects before network commit | Android Accessibility exposes native browser UI hierarchy; DOM text exposed via accessibility node tree. |
| **Messaging & Chat Apps** (WhatsApp, Telegram, Signal, SMS) | • Unsent message draft in `EditText`<br>• Incoming message notification text<br>• Send button click event<br>• Recipient contact name/group header | **YES**<br>(`WARN` overlay on public PII; `ASK` on unverified recipient) | • End-to-end encrypted VoIP audio/video<br>• Disappearing media before render<br>• Custom sticker/image payloads in draft | Accessibility text change events expose draft input; NotificationListener exposes incoming message alerts. |
| **Email Clients** (Gmail, Outlook) | • Email subject & sender header<br>• Email body text nodes<br>• Embedded link text & targets<br>• Send / Reply button click | **YES**<br>(`WARN` on urgency lure; `STOP` on phishing link) | • Encrypted S/MIME body before decrypt<br>• Raw binary attachment content prior to download | Native view hierarchy exposes text fields and clickable spans. |
| **Social Media & Public Feeds** (X/Twitter, Reddit, Forums) | • Post composition draft text<br>• Tweet/Post submit button<br>• Destination channel identity (Public) | **YES**<br>(`STOP` on Aadhaar/PAN/OTP to public; `WARN` on phone/email) | • Video stream contents<br>• Non-standard animated canvas stickers | Public channel heuristics classify destination as irreversible broadcast ($r = 1.0$). |
| **Banking & Payment Apps with `FLAG_SECURE`** (GPay, PhonePe, Banking Apps) | • Package identity & app launch event<br>• System notification text (transaction alerts, debit SMS) | **NOTIFICATION ONLY**<br>(No window overlay over `FLAG_SECURE` windows on Android 12+) | • **Window hierarchy is completely invisible**<br>• Screen pixels blocked by OS<br>• PIN / UPI keypad entries | **Android `FLAG_SECURE` barrier:** OS blocks window inspection, screenshots, and overlays to prevent tapjacking. |
| **Password & Credential Input Fields** | • Presence of an input field<br>• Input type hint (`TYPE_TEXT_VARIATION_PASSWORD`) | **YES**<br>(Can warn about deceptive domain context) | • **Password characters are NEVER read**<br>• Text returned as null or mask bullets | **Android Security & ContextGuard Privacy Invariant:** `isPassword() == true` nodes are strictly skipped. |
| **System Notifications** (All allowlisted packages) | • Notification title & body text<br>• Action buttons<br>• Associated PendingIntent URLs<br>• Post timestamp | **YES**<br>(High-priority advisory notification with `WARN` / `STOP`) | • Custom remote views without text extras<br>• Media player progress & ongoing downloads | `NotificationListenerService` receives `StatusBarNotification` bundle. |
| **Explicit Sharesheet Receiver** | • Bitmaps (PNG, JPEG, WebP)<br>• Multi-page PDFs (first 3 pages)<br>• Raw shared text & URLs | **YES**<br>(Full-screen ContextGuard UI with 8-node trace) | • Unsupported proprietary formats (.docx, .zip) | Ingests via Android `ContentResolver` and in-memory streams. |
| **Flutter / React Native / Custom Canvas Apps** | • Text nodes **IF** app enables accessibility semantics | **PARTIAL**<br>(If accessibility tree populated) | • Apps with disabled semantics tree<br>• Custom OpenGL / Unity games | Custom rendering engines that do not populate `AccessibilityNodeInfo` appear empty. |
| **System Lock Screen & Keyguard** | • Lock screen notifications | **NO OVERLAY**<br>(Advisory notification only) | • Active unlock PIN / pattern / biometric | OS keyguard security restricts third-party overlays. |

---

## 2. Technical Deep Dive: What Can Be Observed

### 2.1. Browser Address Bar Extraction
- **Chrome (`com.android.chrome`):**
  - View ID: `com.android.chrome:id/url_bar`
  - Node text contains the active URL or display domain.
  - Event: `TYPE_WINDOW_STATE_CHANGED` or `TYPE_VIEW_TEXT_CHANGED`.
- **Firefox (`org.mozilla.firefox`):**
  - View ID: `org.mozilla.firefox:id/url_bar_title` or `toolbar`.
  - Node text contains current web address.
- **Extraction Guarantees:**
  - ContextGuard extracts only the active scheme, host, port, and path.
  - Passes extracted URL directly to the on-device `URLRiskService` (XGBoost 36-feature classifier).

### 2.2. Composition & Input Field Inspection
- **Target Nodes:** `android.widget.EditText` or nodes with `isEditable() == true`.
- **Exclusion Invariant:**
  ```kotlin
  if (node.isPassword || node.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0) {
      // STRICT PRIVACY BARRIER: Zero extraction, zero caching
      return null
  }
  ```
- **Observed Data:** Uncommitted draft text.
- **Engine Processing:** Evaluates text using on-device `PiiDetector` (regex + Luhn card validation + Aadhaar format + OTP patterns).

### 2.3. Action Button & Intent Resolution
- **Target Nodes:** `android.widget.Button`, `android.widget.ImageButton`, or clickable elements with content descriptions.
- **Labels Observed:** "Send", "Share", "Post", "Tweet", "Transfer", "Pay", "Submit", "Sign", "Login", "Confirm".
- **Pre-Action Mapping:** Clicking an action button establishes the `intended_action` field in the risk engine:
  - "Post" / "Tweet" $\implies \text{POST}$ (Public broadcast, $r = 1.0$)
  - "Send" $\implies \text{SEND}$ (Direct communication, $r = 0.5$)
  - "Pay" / "Transfer" $\implies \text{APPROVE}$ (Financial irreversibility, $r = 0.9$)

---

## 3. Technical Blind Spots & Defensive Invariants

### 3.1. The `FLAG_SECURE` Boundary
- **Description:** Banking applications (e.g., Chase, HDFC, SBI YONO), UPI apps (Google Pay, PhonePe), and password managers set `WindowManager.LayoutParams.FLAG_SECURE` on their window.
- **Impact on ContextGuard:**
  - The accessibility tree inside the window cannot be traversed.
  - Screen capture returns a black canvas.
  - Overlays cannot obscure the window on Android 12+ due to tapjacking prevention (`hideNonSystemOverlayWindows`).
- **ContextGuard Handling:**
  - ContextGuard **never claims to monitor transactions inside `FLAG_SECURE` windows**.
  - Protection shifts to **pre-app entry** (detecting phishing links that mimic the bank) and **post-app entry** (intercepting debit SMS alerts to detect unauthorized transfers).

### 3.2. Password Fields (`isPassword == true`)
- **Impact on ContextGuard:** Zero character visibility.
- **ContextGuard Handling:**
  - The security decision evaluates the **domain and context**, not the password text.
  - If a user enters credentials on an unverified domain (e.g. `http://bank-login-secure.xyz`), ContextGuard triggers a `STOP` overlay based solely on the domain risk, without ever needing to see what password was typed.

### 3.3. Custom Canvas & Uninstrumented Frameworks
- **Impact on ContextGuard:** If an app is built using Unity, raw OpenGL, or Flutter without semantic accessibility enabled, `AccessibilityNodeInfo` child count is zero.
- **ContextGuard Handling:**
  - Adheres to the **Model Failure Safety Rule**: When the UI tree is obstructed, ContextGuard reports "Screen context unavailable" and does not issue a false `ACT` ("Safe").
  - The user can always fall back to Ingestion Path C (taking a screenshot and sharing via Sharesheet for ML Kit OCR analysis).

---

## 4. Genuine Just-in-Time Accessibility Overlay Mechanics

### 4.1. Overlay Window Architecture
- **Window Type:** `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY` (2032).
  - Binds directly to the enabled `ScreenGuardAccessibilityService`.
  - Avoids requesting `SYSTEM_ALERT_WINDOW` as an unnecessary shortcut for accessibility-driven interventions.
- **Window Layout & Background Invariant:**
  - **ASK / WARN:** Anchored top card (`MATCH_PARENT` width with horizontal margins, `WRAP_CONTENT` height, `FLAG_NOT_TOUCH_MODAL or FLAG_NOT_FOCUSABLE`). The root background is `Color.TRANSPARENT`. The underlying third-party app remains 100% visible and interactive outside the card bounds.
  - **STOP:** Emergency intervention modal with translucent dark scrim (`0xB3090D14`, ~70% opacity). The underlying third-party app remains clearly visible behind the overlay, while protecting the user from accidental taps before an explicit choice is made.
  - **ACT:** Strictly silent. Never displays an overlay.

### 4.2. Action Conditioning & Click Non-Injection Invariant
The overlay provides 4 explicit user actions:
1. **"Continue once":** Dismisses the overlay, suppresses immediate duplicate alerts for unchanged screen state via SHA-256 fingerprint tracking, and logs telemetry choice.
   - **CRITICAL INVARIANT:** Never injects gestures or clicks into the third-party app. The user retains sole discretion to manually tap underlying buttons when ready.
2. **"Review risk":** Launches ContextGuard `MainActivity` with intent action `ACTION_REVIEW_RISK`, populating the full risk analysis and breakdown screen.
3. **"Dismiss":** Dismisses the overlay and adds the screen fingerprint to the dismissal ledger to prevent warning fatigue.
4. **"Disable for this app":** Invokes `AllowlistManager.toggleApp(packageName, false)`, removing the target app from active monitoring.

### 4.3. Ephemeral Telemetry (Zero Raw Text Invariant)
Measures:
- `eventTimestamp`
- `riskDecisionTimestamp`
- `overlayShownTimestamp`
- `decisionToOverlayLatencyMs`
- `interventionType` (`ASK` / `WARN` / `STOP`)
- `targetPackage`
- `candidateAction`
- `userChoice` (`CONTINUE_ONCE`, `REVIEW_RISK`, `DISMISS`, `DISABLE_APP`)
- **Strict Privacy Barrier:** Telemetry data classes contain strictly ZERO raw screen text, passwords, or draft messages.

---

## 5. Security, Anti-Abuse Defenses & Tested Verification Status

### 5.1. Anti-Abuse Defenses
1. **Anti-Fatigue Policy:** Short-lived SHA-256 content fingerprints suppress duplicate alerts for unchanged screens.
2. **Click Non-Injection:** Absolute prohibition against simulated touch dispatch or automated confirmation.
3. **Hardware / Software Kill-Switch:** Pausing monitoring via `ScreenGuardStateManager.pauseMonitoring()` immediately removes any active overlay within 5ms.
4. **Service Unbind Cleanup:** `onInterrupt()`, `onUnbind()`, and `onDestroy()` cleanly remove overlay views from `WindowManager` without window leaks.

### 5.2. Device & OS Testing Verification Status

| Surface / Invariant | Verified By | Test Result | Limitations & Platform Caveats |
| :--- | :--- | :---: | :--- |
| **Real App Visibility Behind Overlay** | `InterventionOverlayTest` | **PASSED** | Translucent scrim (`0xB3090D14`) and `TRANSPARENT` root preserve visibility of background apps. |
| **Triggered from Real ScreenRiskTriggerEngine** | `InterventionOverlayTest` | **PASSED** | 4 trigger classes (Phishing Login, Sharing PII, High-Risk Payment, Risky Submission) verified. |
| **Click Non-Injection on Continue** | `InterventionOverlayTest` | **PASSED** | Verified zero clicks or touch events injected on "Continue once". |
| **Dismissal Anti-Fatigue Suppression** | `InterventionOverlayTest` | **PASSED** | Verified immediate unchanged events suppressed by fingerprint tracking. |
| **Turn Off Monitoring Removes Overlays** | `InterventionOverlayTest` | **PASSED** | `pauseMonitoring()` immediately tears down active window attachment. |
| **Permission Unbind / Revocation Cleanup** | `InterventionOverlayTest` | **PASSED** | Verified clean unbind without crashes or leaked window tokens. |
| **Ordinary Browsing Remains Silent** | `InterventionOverlayTest` | **PASSED** | Verified benign browsing emits silent `ACT` without overlay interruption. |
| **Physical Device Instrumentation** | `adb devices` (0 attached) | **UNVERIFIED** | Physical device run marked UNVERIFIED due to no connected hardware. Tested via 62 unit tests and debug APK compilation. |
| **`FLAG_SECURE` Screens** | Android OS specification | **LIMITATION** | Banking apps and payment PIN screens with `FLAG_SECURE` block overlay display and window hierarchy inspection. ContextGuard reports explicit inspection unavailability. |

