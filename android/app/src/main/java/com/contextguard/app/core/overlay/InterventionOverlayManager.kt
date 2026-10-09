package com.contextguard.app.core.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.contextguard.app.MainActivity
import com.contextguard.app.core.accessibility.AllowlistManager
import com.contextguard.app.core.accessibility.ScreenContextMetadata
import com.contextguard.app.core.accessibility.ScreenGuardStateManager
import com.contextguard.app.core.accessibility.ScreenRiskTriggerEngine
import com.contextguard.app.core.logging.AppLogger
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.SafetyResult

/**
 * Genuine Just-in-Time Intervention Overlay Manager.
 *
 * ARCHITECTURAL INVARIANTS:
 * - Uses native Accessibility Overlay capability (WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY).
 * - Avoids requesting SYSTEM_ALERT_WINDOW as a shortcut.
 * - Shows over active third-party apps only when ScreenRiskTriggerEngine emits actionable risk (ASK, WARN, STOP).
 * - ACT is completely silent (no overlay).
 * - Underlying third-party app remains visible behind the overlay at all times.
 * - "Continue once" dismisses the overlay WITHOUT injecting clicks or gestures into the underlying app.
 * - Suppresses immediate repeated alerts for unchanged screen state via fingerprint tracking.
 * - Never records raw screen text, passwords, or message drafts in telemetry.
 * - Accessible to screen readers (TalkBack) with explicit content descriptions and accessibility focus.
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

    // Theme Color Tokens
    private const val COLOR_BG_CARD = 0xFF111722.toInt() // SurfaceDark
    private const val COLOR_SCRIM_STOP = 0xB3090D14.toInt() // ~70% dark translucent scrim
    private const val COLOR_BORDER_DEFAULT = 0xFF26334A.toInt() // SurfaceBorder
    private const val COLOR_STOP = 0xFFFF1744.toInt() // StopRed
    private const val COLOR_WARN = 0xFFFF9100.toInt() // WarnOrange
    private const val COLOR_ASK = 0xFFFFD600.toInt() // AskYellow
    private const val COLOR_ACT = 0xFF00E676.toInt() // ActGreen
    private const val COLOR_CYAN = 0xFF00E5FF.toInt() // CyanAccent
    private const val COLOR_TEXT_PRIMARY = 0xFFF8FAFC.toInt() // TextPrimary
    private const val COLOR_TEXT_SECONDARY = 0xFF94A3B8.toInt() // TextSecondary
    private const val COLOR_BTN_SECONDARY = 0xFF1E293B.toInt() // Slate 800
    private const val COLOR_TEXT_DANGER = 0xFFFF8A80.toInt() // Danger light red

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

        // Verify monitoring is active
        if (!ScreenGuardStateManager.canProcessEvents()) {
            return
        }

        mainHandler.post {
            try {
                // Dismiss existing overlay if any before showing a new one
                dismissActiveOverlay()

                val context = service.applicationContext
                val windowManager = try {
                    service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                } catch (e: Exception) {
                    null
                }

                val now = System.currentTimeMillis()
                val riskDecisionTimestamp = now
                val overlayShownTimestamp = System.currentTimeMillis()

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

                // Construct UI Hierarchy
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
                    AppLogger.i("Intervention overlay displayed for ${metadata.packageName} [${safetyResult.intervention}]")
                }
            } catch (e: Exception) {
                AppLogger.e("Failed to display intervention overlay: ${e.message}")
            }
        }
    }

    /**
     * Builds the overlay view hierarchy and window layout parameters.
     */
    fun buildOverlayView(
        service: AccessibilityService,
        safetyResult: SafetyResult,
        metadata: ScreenContextMetadata,
        recordId: String
    ): Pair<View, WindowManager.LayoutParams> {
        val context = service.applicationContext
        val intervention = safetyResult.intervention

        // Window Layout Parameters using Accessibility Overlay type
        val wmLayoutParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT

            if (intervention == InterventionType.STOP) {
                // High-risk state: temporarily covers screen with semi-transparent scrim
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.CENTER
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            } else {
                // ASK / WARN: Compact anchored card
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dpToPx(context, 36)
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
        }

        // Root container: Scrim for STOP, transparent for ASK/WARN (Third-party app remains visible behind)
        val root = FrameLayout(context).apply {
            if (intervention == InterventionType.STOP) {
                setBackgroundColor(COLOR_SCRIM_STOP)
            } else {
                setBackgroundColor(Color.TRANSPARENT)
            }
            fitsSystemWindows = false
        }

        // Inner Card Container
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val cardBg = GradientDrawable().apply {
                setColor(COLOR_BG_CARD)
                cornerRadius = dpToPx(context, 16).toFloat()
                val borderColor = when (intervention) {
                    InterventionType.STOP -> COLOR_STOP
                    InterventionType.WARN -> COLOR_WARN
                    InterventionType.ASK -> COLOR_ASK
                    InterventionType.ACT -> COLOR_ACT
                }
                setStroke(dpToPx(context, 2), borderColor)
            }
            background = cardBg
            elevation = dpToPx(context, 12).toFloat()
            setPadding(
                dpToPx(context, 18),
                dpToPx(context, 16),
                dpToPx(context, 18),
                dpToPx(context, 16)
            )

            // Screen reader accessibility: Set explicit description and focusability
            isFocusable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = "ContextGuard Alert: ${safetyResult.intervention}. " +
                    "${safetyResult.rationale}. Target: ${metadata.candidateAction.name} in ${metadata.packageName}."
        }

        val cardLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = if (intervention == InterventionType.STOP) Gravity.CENTER else Gravity.TOP
            val marginHoriz = dpToPx(context, 18)
            setMargins(marginHoriz, dpToPx(context, 12), marginHoriz, dpToPx(context, 12))
        }
        root.addView(card, cardLayoutParams)

        // 1. Header Layout: Badge + Title + Confidence indicator
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Badge Pill
        val badgeColor = when (intervention) {
            InterventionType.STOP -> COLOR_STOP
            InterventionType.WARN -> COLOR_WARN
            InterventionType.ASK -> COLOR_ASK
            InterventionType.ACT -> COLOR_ACT
        }
        val badge = TextView(context).apply {
            text = intervention.name
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(badgeColor)
                cornerRadius = dpToPx(context, 6).toFloat()
            }
            setPadding(dpToPx(context, 8), dpToPx(context, 3), dpToPx(context, 8), dpToPx(context, 3))
        }
        headerLayout.addView(badge)

        // Title
        val title = TextView(context).apply {
            text = "ContextGuard Intervention"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(COLOR_TEXT_PRIMARY)
            setPadding(dpToPx(context, 10), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerLayout.addView(title)

        // Confidence / Uncertainty Pill
        val confText = TextView(context).apply {
            val confPct = (safetyResult.confidence * 100).toInt()
            text = "Conf: $confPct%"
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(COLOR_CYAN)
            gravity = Gravity.END
        }
        headerLayout.addView(confText)
        card.addView(headerLayout)

        // 2. Target Context Row
        val contextRow = TextView(context).apply {
            text = "Context: ${metadata.candidateAction.name} in ${metadata.packageName}"
            textSize = 11f
            setTextColor(COLOR_TEXT_SECONDARY)
            setPadding(0, dpToPx(context, 8), 0, 0)
        }
        card.addView(contextRow)

        // 3. Short, Evidence-Grounded Reason
        val reasonText = TextView(context).apply {
            val reason = safetyResult.rationale.ifBlank {
                safetyResult.evidence.firstOrNull() ?: "Pre-action risk threshold exceeded."
            }
            text = reason
            textSize = 13f
            setTextColor(COLOR_TEXT_PRIMARY)
            maxLines = 4
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(dpToPx(context, 2).toFloat(), 1f)
            setPadding(0, dpToPx(context, 8), 0, dpToPx(context, 14))
        }
        card.addView(reasonText)

        // Divider
        val divider = View(context).apply {
            setBackgroundColor(COLOR_BORDER_DEFAULT)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 1)
            ).apply {
                setMargins(0, 0, 0, dpToPx(context, 12))
            }
        }
        card.addView(divider)

        // 4. Action Controls (Row 1: Continue Once & Review Risk)
        val row1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(context, 8))
            }
        }

        // Action: "Continue once"
        // INVARIANT: Dismisses overlay ONLY. Strictly NEVER clicks underlying button or injects gestures.
        val btnContinue = Button(context).apply {
            text = "Continue once"
            textSize = 12f
            isAllCaps = false
            setTextColor(COLOR_TEXT_PRIMARY)
            background = GradientDrawable().apply {
                setColor(COLOR_BTN_SECONDARY)
                cornerRadius = dpToPx(context, 8).toFloat()
                setStroke(dpToPx(context, 1), COLOR_BORDER_DEFAULT)
            }
            contentDescription = "Continue once without safety intervention"
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(context, 40), 1f).apply {
                setMargins(0, 0, dpToPx(context, 6), 0)
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
        row1.addView(btnContinue)

        // Action: "Review risk"
        val btnReview = Button(context).apply {
            text = "Review risk"
            textSize = 12f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(COLOR_BG_CARD)
            background = GradientDrawable().apply {
                setColor(COLOR_CYAN)
                cornerRadius = dpToPx(context, 8).toFloat()
            }
            contentDescription = "Review detailed risk analysis in ContextGuard"
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(context, 40), 1f).apply {
                setMargins(dpToPx(context, 6), 0, 0, 0)
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
        row1.addView(btnReview)
        card.addView(row1)

        // Action Controls (Row 2: Dismiss & Disable for this app)
        val row2 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Action: "Dismiss"
        val btnDismiss = Button(context).apply {
            text = "Dismiss"
            textSize = 11f
            isAllCaps = false
            setTextColor(COLOR_TEXT_SECONDARY)
            background = null
            contentDescription = "Dismiss safety alert"
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(context, 36), 1f)
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
        row2.addView(btnDismiss)

        // Action: "Disable for this app"
        val btnDisable = Button(context).apply {
            text = "Disable for this app"
            textSize = 11f
            isAllCaps = false
            setTextColor(COLOR_TEXT_DANGER)
            background = null
            contentDescription = "Disable ContextGuard monitoring for ${metadata.packageName}"
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(context, 36), 1f)
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
        row2.addView(btnDisable)
        card.addView(row2)

        return Pair(root, wmLayoutParams)
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
