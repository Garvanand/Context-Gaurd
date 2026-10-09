package com.contextguard.app.core.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.contextguard.app.core.accessibility.AllowlistManager
import com.contextguard.app.core.accessibility.ScreenContextExtractor
import com.contextguard.app.core.accessibility.ScreenGuardStateManager
import com.contextguard.app.core.accessibility.ScreenRiskTriggerEngine
import com.contextguard.app.core.logging.AppLogger
import com.contextguard.app.core.overlay.InterventionOverlayManager
import com.contextguard.app.ui.viewmodel.InterventionType

/**
 * Real Android Screen-Context Monitoring Service for ContextGuard.
 *
 * ARCHITECTURAL INVARIANTS:
 * - Subscribes strictly to window state, window content, focus, and click events.
 * - Processes events only from user-allowlisted packages.
 * - Safely inspects rootInActiveWindow without ANRs.
 * - Strictly ignores password inputs.
 * - Dispatches real Just-in-Time accessibility overlays via InterventionOverlayManager.
 * - ACT is completely silent (no overlay).
 * - Tracks live connection state in ScreenGuardStateManager.
 */
class ScreenGuardAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppLogger.i("ScreenGuardAccessibilityService connected to Android accessibility manager.")
        try {
            if (com.contextguard.app.core.engine.OnDeviceDecisionEngine.getUrlEngine() == null) {
                val urlEngine = com.contextguard.app.core.engine.UrlTreeInferenceEngine.getInstance(applicationContext)
                com.contextguard.app.core.engine.OnDeviceDecisionEngine.setUrlEngine(urlEngine)
            }
        } catch (e: Exception) {
            AppLogger.e("Could not attach UrlTreeInferenceEngine in ScreenGuardAccessibilityService: ${e.message}")
        }
        ScreenGuardStateManager.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. Verify global monitoring is active and not paused
        if (!ScreenGuardStateManager.canProcessEvents()) {
            return
        }

        // 2. Package filtering: Only allowlisted third-party packages are inspected
        val packageName = event.packageName?.toString() ?: return
        if (!AllowlistManager.isPackageAllowed(packageName)) {
            return
        }

        try {
            // 3. Inspect active accessible window content
            val root = rootInActiveWindow

            // 4. Extract structured metadata (strictly bypassing password controls)
            val metadata = ScreenContextExtractor.extract(root, event)

            // 5. Evaluate pre-action risk via deterministic trigger engine
            val safetyResult = ScreenRiskTriggerEngine.evaluate(metadata)

            if (safetyResult != null) {
                AppLogger.i("ScreenGuard evaluation: [${safetyResult.intervention}] for $packageName (Score: ${safetyResult.riskScore})")

                // Handle intervention overlay: ACT is silent; ASK, WARN, STOP trigger JIT overlay card
                if (safetyResult.intervention != InterventionType.ACT) {
                    mainHandler.post {
                        InterventionOverlayManager.showIntervention(
                            service = this,
                            safetyResult = safetyResult,
                            metadata = metadata
                        )
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error processing accessibility event for $packageName: ${e.message}")
        }
    }

    override fun onInterrupt() {
        AppLogger.w("ScreenGuardAccessibilityService interrupted by system.")
        mainHandler.post {
            InterventionOverlayManager.dismissActiveOverlay()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        AppLogger.i("ScreenGuardAccessibilityService unbound.")
        mainHandler.post {
            InterventionOverlayManager.dismissActiveOverlay()
        }
        ScreenGuardStateManager.onServiceDisconnected()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.i("ScreenGuardAccessibilityService destroyed.")
        mainHandler.post {
            InterventionOverlayManager.dismissActiveOverlay()
        }
        ScreenGuardStateManager.onServiceDisconnected()
    }
}
