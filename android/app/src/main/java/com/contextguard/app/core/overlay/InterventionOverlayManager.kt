package com.contextguard.app.core.overlay

import android.accessibilityservice.AccessibilityService
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.contextguard.app.MainActivity
import com.contextguard.app.R
import com.contextguard.app.core.accessibility.AllowlistManager
import com.contextguard.app.core.accessibility.ScreenContextMetadata
import com.contextguard.app.core.accessibility.ScreenGuardStateManager
import com.contextguard.app.core.accessibility.ScreenRiskTriggerEngine
import com.contextguard.app.core.logging.AppLogger
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.SafetyResult

/**
 * Signature Just-in-Time Intervention Overlay: "Signal Intercept"
 *
 * ARCHITECTURAL INVARIANTS:
 * - Uses native Accessibility Overlay capability (WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY).
 * - Compact floating card that leaves third-party applications and system navigation visible and unblocked.
 * - Shows over active third-party apps only when ScreenRiskTriggerEngine emits actionable risk (ASK, WARN, STOP).
 * - ACT is completely silent (no overlay).
 * - Underlying third-party app remains visible behind the overlay at all times.
 * - "Continue once" dismisses the overlay WITHOUT injecting clicks or gestures into the underlying app.
 * - Suppresses immediate repeated alerts for unchanged screen state via fingerprint tracking.
 * - Never records raw screen text, passwords, or message drafts in telemetry.
 * - Accessible to screen readers (TalkBack) with explicit content descriptions and accessibility focus.
 * - Supports Reduced Motion: skips or collapses animations to 0ms when system animator scale is 0.
 */
object InterventionOverlayManager {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var activeOverlayView: View? = null
    private var activeWindowManager: WindowManager? = null
    private var currentRecordId: String? = null
    private var currentMetadata: ScreenContextMetadata? = null
    private var activeInterventionType: InterventionType? = null
    private var activePackage: String? = null
    private var lastLayoutParams: WindowManager.LayoutParams? = null

    // Spectral Signal Color Tokens
    private const val COLOR_BG_CARD = 0xFF0C101A.toInt() // DeepSurface
    private const val COLOR_BG_ELEVATED = 0xFF141926.toInt() // ElevatedSurface
    private const val COLOR_BORDER_CONTOUR = 0xFF242C3D.toInt() // ContourBorder
    private const val COLOR_BORDER_ACTIVE = 0xFF3D4B66.toInt() // ContourBorderActive
    private const val COLOR_VIOLET = 0xFF8B70FF.toInt() // ElectricViolet
    private const val COLOR_CYAN = 0xFF45E4FF.toInt() // IonCyan
    private const val COLOR_LIME = 0xFFD8FF63.toInt() // SignalLime / ActLime
    private const val COLOR_AMBER = 0xFFFFC837.toInt() // AskAmber
    private const val COLOR_ORANGE = 0xFFFF8C38.toInt() // WarnOrange
    private const val COLOR_CORAL = 0xFFFF4D6A.toInt() // StopCoral
    private const val COLOR_TEXT_PRIMARY = 0xFFF4F6FF.toInt() // SoftWhite
    private const val COLOR_TEXT_MUTED = 0xFF8E9BAE.toInt() // MutedText
    private const val COLOR_TEXT_SUBTLE = 0xFF586579.toInt() // SubtleText
    private const val COLOR_SCRIM_STOP = 0xB3090D14.toInt() // ~70% translucent scrim

    /**
     * Checks if the user or device has requested reduced motion / disabled animations.
     */
    fun isReducedMotionEnabled(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Shows a contextual intervention card over the currently active supported app.
     * Must be invoked only when ScreenRiskTriggerEngine detects actionable risk.
     */
    fun showIntervention(
        service: AccessibilityService,
        safetyResult: SafetyResult,
        metadata: ScreenContextMetadata
    ) {
        // UX Constraint: ACT is silent. Never show overlay for ACT.
        if (safetyResult.intervention == InterventionType.ACT) {
            return
        }

        // Verify monitoring is active and not paused
        if (!ScreenGuardStateManager.canProcessEvents()) {
            return
        }

        mainHandler.post {
            try {
                // Dismiss existing overlay if any before showing a new one
                dismissActiveOverlay()

                val windowManager = try {
                    service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                } catch (e: Exception) {
                    null
                }

                val now = System.currentTimeMillis()
                val riskDecisionTimestamp = now
                val overlayShownTimestamp = System.currentTimeMillis()

                // Guard against post-action false interception claims:
                // If event timestamp is stale (> 3000ms old), do not claim real-time pre-action interception
                if (now - metadata.timestampMs > 3000L) {
                    AppLogger.w("Event timestamp is stale (${now - metadata.timestampMs}ms ago); skipping overlay to avoid false post-action claim.")
                    return@post
                }

                // Record privacy-safe telemetry (NEVER records raw screen text)
                val recordId = OverlayTelemetryManager.recordOverlayShown(
                    eventTimestamp = metadata.timestampMs,
                    riskDecisionTimestamp = riskDecisionTimestamp,
                    overlayShownTimestamp = overlayShownTimestamp,
                    interventionType = safetyResult.intervention.name,
                    targetPackage = metadata.packageName,
                    candidateAction = metadata.candidateAction.name
                )

                currentRecordId = recordId
                currentMetadata = metadata
                activeInterventionType = safetyResult.intervention
                activePackage = metadata.packageName
                activeWindowManager = windowManager

                // Construct UI Hierarchy with Signal Intercept design
                val (rootView, wmLayoutParams) = buildOverlayView(
                    service = service,
                    safetyResult = safetyResult,
                    metadata = metadata,
                    recordId = recordId
                )

                lastLayoutParams = wmLayoutParams
                activeOverlayView = rootView

                if (windowManager != null) {
                    windowManager.addView(rootView, wmLayoutParams)
                    AppLogger.i("Signal Intercept overlay displayed for ${metadata.packageName} [${safetyResult.intervention}] (Decision->Overlay: ${overlayShownTimestamp - riskDecisionTimestamp}ms)")
                }
            } catch (e: Exception) {
                AppLogger.e("Failed to display intervention overlay: ${e.message}")
            }
        }
    }

    /**
     * Builds the overlay view hierarchy and window layout parameters.
     * Implements the "Signal Intercept" floating card with layered surfaces
     * and a thin spectral accent at the leading edge.
     */
    fun buildOverlayView(
        service: AccessibilityService,
        safetyResult: SafetyResult,
        metadata: ScreenContextMetadata,
        recordId: String
    ): Pair<View, WindowManager.LayoutParams> {
        val context = service.applicationContext
        val intervention = safetyResult.intervention

        // Window Layout Parameters: Compact anchored floating card
        // Does NOT cover bottom system navigation or Android gesture bar
        val wmLayoutParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(context, 48) // Safe position below status bar
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        // Intervention accent color
        val accentColor = when (intervention) {
            InterventionType.STOP -> COLOR_CORAL
            InterventionType.WARN -> COLOR_ORANGE
            InterventionType.ASK -> COLOR_AMBER
            InterventionType.ACT -> COLOR_LIME
        }

        // Evidence category tag
        val categoryLabel = resolveEvidenceCategory(safetyResult, metadata)

        // Root container: Transparent frame so third-party app is visible
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            fitsSystemWindows = false
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Inner Card Container: Layered precision-engineered floating card
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val cardBg = GradientDrawable().apply {
                setColor(COLOR_BG_CARD)
                cornerRadius = dpToPx(context, 16).toFloat()
                setStroke(dpToPx(context, 1), COLOR_BORDER_CONTOUR)
            }
            background = cardBg
            elevation = dpToPx(context, 16).toFloat()
            setPadding(0, 0, 0, dpToPx(context, 16))

            // Screen reader accessibility: Complete TalkBack description
            isFocusable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = "ContextGuard Signal Intercept: ${intervention.name}. $categoryLabel. " +
                    "${safetyResult.rationale}. Target: ${metadata.candidateAction.name} in ${metadata.packageName}."
        }

        val cardLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
            val marginHoriz = dpToPx(context, 16)
            setMargins(marginHoriz, dpToPx(context, 8), marginHoriz, dpToPx(context, 8))
        }
        root.addView(card, cardLayoutParams)

        // ==========================================
        // 1. LEADING EDGE: THIN SPECTRAL ACCENT LINE
        // ==========================================
        val leadingAccentLine = View(context).apply {
            val lineDrawable = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(COLOR_VIOLET, COLOR_CYAN, accentColor)
            ).apply {
                cornerRadii = floatArrayOf(
                    dpToPx(context, 16).toFloat(), dpToPx(context, 16).toFloat(),
                    dpToPx(context, 16).toFloat(), dpToPx(context, 16).toFloat(),
                    0f, 0f, 0f, 0f
                )
            }
            background = lineDrawable
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 3)
            )
        }
        card.addView(leadingAccentLine)

        // Card Content Wrapper (with inner margins)
        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dpToPx(context, 18),
                dpToPx(context, 14),
                dpToPx(context, 18),
                0
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        card.addView(contentLayout)

        // ==========================================
        // 2. HEADER: APERTURE LOGO + BRAND + CATEGORY + METRICS
        // ==========================================
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(context, 10)
            }
        }

        // Small Aperture Signal Brand Logo
        val apertureLogo = ImageView(context).apply {
            setImageResource(R.drawable.ic_aperture_signal_symbol)
            val iconSize = dpToPx(context, 20)
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                rightMargin = dpToPx(context, 8)
            }
            contentDescription = "Aperture Signal"
        }
        headerLayout.addView(apertureLogo)

        // Brand Sub-label
        val brandWordmark = TextView(context).apply {
            text = "CONTEXTGUARD"
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setTextColor(COLOR_CYAN)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dpToPx(context, 8)
            }
        }
        headerLayout.addView(brandWordmark)

        // Category Indicator Tag
        val categoryPill = TextView(context).apply {
            text = categoryLabel
            textSize = 9.5f
            typeface = Typeface.MONOSPACE
            setTextColor(accentColor)
            background = GradientDrawable().apply {
                setColor(accentColor and 0x00FFFFFF or 0x24000000) // ~14% alpha
                cornerRadius = dpToPx(context, 4).toFloat()
                setStroke(dpToPx(context, 1), accentColor and 0x00FFFFFF or 0x66000000)
            }
            setPadding(dpToPx(context, 6), dpToPx(context, 2), dpToPx(context, 6), dpToPx(context, 2))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerLayout.addView(categoryPill)

        // Risk Metric / Uncertainty
        val metricPill = TextView(context).apply {
            text = "rho=${String.format("%.2f", safetyResult.riskScore)}"
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setTextColor(COLOR_TEXT_MUTED)
            gravity = Gravity.END
        }
        headerLayout.addView(metricPill)
        contentLayout.addView(headerLayout)

        // ==========================================
        // 3. STRONG INTERVENTION HEADING
        // ==========================================
        val headingText = TextView(context).apply {
            text = resolveInterventionHeading(intervention, metadata)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(COLOR_TEXT_PRIMARY)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(context, 6)
            }
        }
        contentLayout.addView(headingText)

        // ==========================================
        // 4. ONE SENTENCE EXPLAINING THE EVIDENCE
        // ==========================================
        val evidenceSentenceText = TextView(context).apply {
            text = resolveEvidenceSentence(safetyResult, metadata)
            textSize = 12.5f
            setTextColor(COLOR_TEXT_PRIMARY)
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(dpToPx(context, 2).toFloat(), 1f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(context, 8)
            }
        }
        contentLayout.addView(evidenceSentenceText)

        // ==========================================
        // 5. GUIDANCE / SAFER ALTERNATIVE / PROMPT
        // ==========================================
        val guidanceRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(COLOR_BG_ELEVATED)
                cornerRadius = dpToPx(context, 6).toFloat()
            }
            setPadding(dpToPx(context, 8), dpToPx(context, 6), dpToPx(context, 8), dpToPx(context, 6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(context, 12)
            }
        }

        val guidanceBullet = View(context).apply {
            background = GradientDrawable().apply {
                setColor(accentColor)
                shape = GradientDrawable.OVAL
            }
            val bulletSize = dpToPx(context, 5)
            layoutParams = LinearLayout.LayoutParams(bulletSize, bulletSize).apply {
                rightMargin = dpToPx(context, 8)
            }
        }
        guidanceRow.addView(guidanceBullet)

        val guidanceText = TextView(context).apply {
            text = resolveGuidanceText(intervention, safetyResult)
            textSize = 11f
            setTextColor(COLOR_TEXT_MUTED)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        guidanceRow.addView(guidanceText)
        contentLayout.addView(guidanceRow)

        // Divider
        val divider = View(context).apply {
            setBackgroundColor(COLOR_BORDER_CONTOUR)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 1)
            ).apply {
                bottomMargin = dpToPx(context, 12)
            }
        }
        contentLayout.addView(divider)

        // ==========================================
        // 6. ACTION CONTROLS: REVIEW & CONTINUE ONCE
        // ==========================================
        val controlsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Primary Row: Continue once & Review Evidence
        val primaryRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(context, 6)
            }
        }

        // Restrained "Continue once" button
        // INVARIANT: Dismisses overlay ONLY. Strictly NEVER clicks underlying button or injects gestures.
        val btnContinue = Button(context).apply {
            text = "Continue once"
            textSize = 12f
            isAllCaps = false
            setTextColor(COLOR_TEXT_PRIMARY)
            background = GradientDrawable().apply {
                setColor(COLOR_BG_ELEVATED)
                cornerRadius = dpToPx(context, 8).toFloat()
                setStroke(dpToPx(context, 1), COLOR_BORDER_CONTOUR)
            }
            contentDescription = "Continue once without safety intervention"
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(context, 42), 1f).apply {
                rightMargin = dpToPx(context, 6)
            }
            setOnClickListener {
                handleUserChoice(
                    actionName = "CONTINUE_ONCE",
                    service = service,
                    metadata = metadata,
                    recordId = recordId,
                    safetyResult = safetyResult
                )
            }
        }
        primaryRow.addView(btnContinue)

        // Prominent "Review Evidence" button
        val btnReview = Button(context).apply {
            text = "Review Evidence"
            textSize = 12f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(COLOR_BG_CARD)
            background = GradientDrawable().apply {
                setColor(COLOR_CYAN)
                cornerRadius = dpToPx(context, 8).toFloat()
            }
            contentDescription = "Review detailed evidence and provenance in ContextGuard"
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(context, 42), 1f).apply {
                leftMargin = dpToPx(context, 6)
            }
            setOnClickListener {
                handleUserChoice(
                    actionName = "REVIEW_RISK",
                    service = service,
                    metadata = metadata,
                    recordId = recordId,
                    safetyResult = safetyResult
                )
            }
        }
        primaryRow.addView(btnReview)
        controlsLayout.addView(primaryRow)

        // Secondary Utility Row: Dismiss & Disable for this app
        val secondaryRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val btnDismiss = Button(context).apply {
            text = "Dismiss"
            textSize = 11f
            isAllCaps = false
            setTextColor(COLOR_TEXT_SUBTLE)
            background = null
            contentDescription = "Dismiss safety alert"
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(context, 34), 1f)
            setOnClickListener {
                handleUserChoice(
                    actionName = "DISMISS",
                    service = service,
                    metadata = metadata,
                    recordId = recordId,
                    safetyResult = safetyResult
                )
            }
        }
        secondaryRow.addView(btnDismiss)

        val btnDisable = Button(context).apply {
            text = "Disable for this app"
            textSize = 11f
            isAllCaps = false
            setTextColor(COLOR_TEXT_MUTED)
            background = null
            contentDescription = "Disable ContextGuard monitoring for ${metadata.packageName}"
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(context, 34), 1f)
            setOnClickListener {
                handleUserChoice(
                    actionName = "DISABLE_APP",
                    service = service,
                    metadata = metadata,
                    recordId = recordId,
                    safetyResult = safetyResult
                )
            }
        }
        secondaryRow.addView(btnDisable)
        controlsLayout.addView(secondaryRow)

        contentLayout.addView(controlsLayout)

        // ==========================================
        // 7. SIGNATURE ENTRANCE ANIMATION
        // ==========================================
        applySignatureAnimation(
            context = context,
            apertureLogo = apertureLogo,
            leadingAccentLine = leadingAccentLine,
            messageView = evidenceSentenceText,
            controlsView = controlsLayout
        )

        return Pair(root, wmLayoutParams)
    }

    /**
     * Applies the signature entrance animation sequence:
     * 1. Aperture mark resolves into view (0-90ms)
     * 2. Signal line traces leading edge (60-180ms)
     * 3. Explanatory message fades/slides into place (120-240ms)
     * 4. Action controls settle into final positions (180-300ms)
     *
     * Respects Reduced Motion: if enabled, sets final properties immediately (0ms).
     */
    fun applySignatureAnimation(
        context: Context,
        apertureLogo: View,
        leadingAccentLine: View,
        messageView: View,
        controlsView: View
    ) {
        if (isReducedMotionEnabled(context)) {
            // Instant presentation for accessibility reduced-motion mode
            apertureLogo.alpha = 1f
            apertureLogo.scaleX = 1f
            apertureLogo.scaleY = 1f
            leadingAccentLine.scaleX = 1f
            messageView.alpha = 1f
            messageView.translationY = 0f
            controlsView.alpha = 1f
            controlsView.translationY = 0f
            return
        }

        // Initial states
        apertureLogo.alpha = 0.3f
        apertureLogo.scaleX = 0.75f
        apertureLogo.scaleY = 0.75f

        leadingAccentLine.pivotX = 0f
        leadingAccentLine.scaleX = 0.1f

        messageView.alpha = 0.4f
        messageView.translationY = -dpToPx(context, 8).toFloat()

        controlsView.alpha = 0.4f
        controlsView.translationY = dpToPx(context, 6).toFloat()

        val interpolator = DecelerateInterpolator(1.4f)

        // 1. Aperture mark resolves (90ms)
        val logoAlpha = ObjectAnimator.ofFloat(apertureLogo, "alpha", 0.3f, 1f).apply { duration = 90 }
        val logoScaleX = ObjectAnimator.ofFloat(apertureLogo, "scaleX", 0.75f, 1f).apply { duration = 90 }
        val logoScaleY = ObjectAnimator.ofFloat(apertureLogo, "scaleY", 0.75f, 1f).apply { duration = 90 }

        // 2. Leading edge traces (120ms, starts at 50ms)
        val lineTrace = ObjectAnimator.ofFloat(leadingAccentLine, "scaleX", 0.1f, 1f).apply {
            startDelay = 50
            duration = 120
            this.interpolator = interpolator
        }

        // 3. Explanatory message fades & slides (120ms, starts at 100ms)
        val msgAlpha = ObjectAnimator.ofFloat(messageView, "alpha", 0.4f, 1f).apply {
            startDelay = 100
            duration = 120
        }
        val msgTransY = ObjectAnimator.ofFloat(messageView, "translationY", -dpToPx(context, 8).toFloat(), 0f).apply {
            startDelay = 100
            duration = 120
            this.interpolator = interpolator
        }

        // 4. Action controls settle (110ms, starts at 160ms)
        val ctrlAlpha = ObjectAnimator.ofFloat(controlsView, "alpha", 0.4f, 1f).apply {
            startDelay = 160
            duration = 110
        }
        val ctrlTransY = ObjectAnimator.ofFloat(controlsView, "translationY", dpToPx(context, 6).toFloat(), 0f).apply {
            startDelay = 160
            duration = 110
            this.interpolator = interpolator
        }

        AnimatorSet().apply {
            playTogether(
                logoAlpha, logoScaleX, logoScaleY,
                lineTrace,
                msgAlpha, msgTransY,
                ctrlAlpha, ctrlTransY
            )
            start()
        }
    }

    /**
     * Resolves a concise, accurate evidence category indicator.
     */
    fun resolveEvidenceCategory(safetyResult: SafetyResult, metadata: ScreenContextMetadata): String {
        val evidenceText = safetyResult.evidence.joinToString(" ").lowercase()
        val rationale = safetyResult.rationale.lowercase()
        val text = metadata.visibleTextFragments.joinToString(" ").lowercase()

        return when {
            evidenceText.contains("phish") || metadata.activeUrl?.contains("http") == true || rationale.contains("url") || text.contains("http") ->
                "PHISHING LINK"
            evidenceText.contains("payment") || evidenceText.contains("upi") || metadata.candidateAction.name.contains("APPROVE") || text.contains("upi") ->
                "PAYMENT AUTHORIZATION"
            evidenceText.contains("credential") || evidenceText.contains("password") || metadata.candidateAction.name.contains("LOGIN") || text.contains("password") ->
                "CREDENTIAL ENTRY"
            evidenceText.contains("aadhaar") || evidenceText.contains("card") || evidenceText.contains("pii") || text.contains("aadhaar") ->
                "SENSITIVE PII"
            evidenceText.contains("upload") || metadata.candidateAction.name.contains("UPLOAD") || text.contains("upload") ->
                "DOCUMENT SUBMISSION"
            evidenceText.contains("telegram") || evidenceText.contains("unverified") || text.contains("telegram") ->
                "UNVERIFIED RECIPIENT"
            else ->
                "IRREVERSIBLE DISCLOSURE"
        }
    }

    /**
     * Resolves one strong intervention heading.
     */
    fun resolveInterventionHeading(intervention: InterventionType, metadata: ScreenContextMetadata): String {
        return when (intervention) {
            InterventionType.STOP -> when (metadata.candidateAction.name) {
                "LOGIN" -> "Stop: Unverified Credential Gateway"
                "APPROVE", "PAY" -> "Stop: High-Consequence Payment Transfer"
                "SEND", "POST" -> "Stop: Irreversible Disclosure Hazard"
                else -> "Stop: Irreversible Risk Detected"
            }
            InterventionType.WARN -> when (metadata.candidateAction.name) {
                "SEND" -> "Warn: Sensitive Data on External Channel"
                "UPLOAD" -> "Warn: Unencrypted Document Upload"
                else -> "Warn: Elevated Exposure Hazard"
            }
            InterventionType.ASK -> "Clarification: Confirm Action Intent"
            InterventionType.ACT -> "Clear: Safe to Proceed"
        }
    }

    /**
     * Resolves one clear sentence explaining the grounded evidence.
     */
    fun resolveEvidenceSentence(safetyResult: SafetyResult, metadata: ScreenContextMetadata): String {
        val firstEvidence = safetyResult.evidence.firstOrNull { !it.contains("Zero Network", ignoreCase = true) }
        return if (!firstEvidence.isNullOrBlank()) {
            firstEvidence
        } else if (safetyResult.rationale.isNotBlank()) {
            safetyResult.rationale
        } else {
            "Pre-action risk conditions detected for ${metadata.candidateAction.name} in ${metadata.packageName}."
        }
    }

    /**
     * Resolves prompt / alternative / override text.
     */
    fun resolveGuidanceText(intervention: InterventionType, safetyResult: SafetyResult): String {
        return when (intervention) {
            InterventionType.STOP ->
                "Overriding will bypass safety protections and execute this operation."
            InterventionType.WARN ->
                if (safetyResult.alternativeAction.isNotBlank()) "Alternative: ${safetyResult.alternativeAction}"
                else "Alternative: Mask sensitive account fields or store in private vault."
            InterventionType.ASK ->
                "High uncertainty in recipient identity. Verify counterparty before proceeding."
            InterventionType.ACT ->
                "Safe action bounded in local hardware perimeter."
        }
    }

    /**
     * Handles user selection on the intervention overlay.
     */
    fun handleUserChoice(
        actionName: String,
        service: AccessibilityService,
        metadata: ScreenContextMetadata,
        recordId: String,
        safetyResult: SafetyResult
    ) {
        // Record telemetry choice (NO raw text)
        OverlayTelemetryManager.recordUserChoice(recordId, actionName)

        when (actionName) {
            "CONTINUE_ONCE" -> {
                // Dismiss overlay and suppress immediate repeat on unchanged screen
                ScreenRiskTriggerEngine.recordDismissal(metadata)
                dismissActiveOverlay()
                // CRITICAL INVARIANT: NEVER click or dispatch gestures to third-party app
            }
            "REVIEW_RISK" -> {
                // Launch ContextGuard app with risk analysis intent
                try {
                    val intent = Intent(service, MainActivity::class.java).apply {
                        action = "com.contextguard.app.ACTION_REVIEW_RISK"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("EXTRA_REVIEW_RISK", true)
                        putExtra("EXTRA_TARGET_PACKAGE", metadata.packageName)
                        putExtra("EXTRA_RATIONALE", safetyResult.rationale)
                        putExtra("EXTRA_RISK_SCORE", safetyResult.riskScore)
                        putExtra("EXTRA_INTERVENTION", safetyResult.intervention.name)
                        putExtra("EXTRA_CANDIDATE_ACTION", metadata.candidateAction.name)
                    }
                    service.startActivity(intent)
                } catch (e: Exception) {
                    AppLogger.e("Failed to launch MainActivity for review risk: ${e.message}")
                }
                dismissActiveOverlay()
            }
            "DISMISS" -> {
                // Suppress repeated warnings for unchanged screen
                ScreenRiskTriggerEngine.recordDismissal(metadata)
                dismissActiveOverlay()
            }
            "DISABLE_APP" -> {
                // Toggle app monitoring off in AllowlistManager
                AllowlistManager.toggleApp(metadata.packageName, false)
                dismissActiveOverlay()
            }
            else -> {
                dismissActiveOverlay()
            }
        }
    }

    /**
     * Safely dismisses and detaches the active overlay from WindowManager.
     */
    fun dismissActiveOverlay() {
        val view = activeOverlayView
        val wm = activeWindowManager

        activeOverlayView = null
        activeWindowManager = null
        activeInterventionType = null
        activePackage = null
        currentRecordId = null
        currentMetadata = null

        if (view != null && wm != null) {
            try {
                wm.removeView(view)
                AppLogger.i("Intervention overlay dismissed and removed from window.")
            } catch (e: Exception) {
                // Window or view might already be removed
            }
        }
    }

    fun isOverlayShowing(): Boolean = activeOverlayView != null

    fun getActiveInterventionType(): InterventionType? = activeInterventionType

    fun getActivePackage(): String? = activePackage

    fun getLastLayoutParams(): WindowManager.LayoutParams? = lastLayoutParams

    fun resetForTesting() {
        dismissActiveOverlay()
        lastLayoutParams = null
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        val density = try {
            context.resources.displayMetrics.density
        } catch (e: Exception) {
            1f
        }
        val d = if (density <= 0f) 1f else density
        return (dp * d).toInt()
    }
}
