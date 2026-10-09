package com.contextguard.app.core.accessibility

import com.contextguard.app.core.engine.OnDeviceDecisionEngine
import com.contextguard.app.core.event.AccessibilityEventFilter
import com.contextguard.app.core.event.ScreenContextType
import com.contextguard.app.core.event.UiScreenEvent
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.SafetyResult
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Deterministic pre-action risk trigger engine.
 *
 * DEFENSIVE INVARIANTS:
 * - Debounces rapid events (300ms window).
 * - Uses short-lived content fingerprints to prevent duplicate inference on identical screens.
 * - Implements per-package cooldowns and dismissal tracking to eliminate warning fatigue.
 * - Does not trigger inference on minor scrolls or keystrokes unless pre-action cues are met.
 */
object ScreenRiskTriggerEngine {

    private const val COOLDOWN_MS = 5000L // 5 seconds per app for identical hazard level
    private val lastInferenceTimestamp = ConcurrentHashMap<String, Long>()
    private val lastFingerprintPerPackage = ConcurrentHashMap<String, String>()
    private val dismissedFingerprints = ConcurrentHashMap.newKeySet<String>()

    /**
     * Evaluates whether a screen state warrants risk inference.
     * Returns SafetyResult if inference was run, or null if skipped/debounced/cooldown.
     */
    fun evaluate(metadata: ScreenContextMetadata): SafetyResult? {
        val packageName = metadata.packageName
        val now = System.currentTimeMillis()

        // 1. If screen is unavailable (FLAG_SECURE or null root), return null (never silent fake safety)
        if (metadata.inspectionUnavailable) {
            return null
        }

        // 2. Debounce check
        if (AccessibilityEventFilter.shouldDebounce(packageName, now)) {
            return null
        }

        // 3. Compute short-lived content fingerprint
        val fingerprint = computeFingerprint(metadata)

        // 4. Check if this exact screen state was already evaluated or dismissed
        if (dismissedFingerprints.contains(fingerprint)) {
            return null
        }

        val lastFingerprint = lastFingerprintPerPackage[packageName]
        val lastTime = lastInferenceTimestamp[packageName] ?: 0L

        // If identical content within cooldown window, skip to prevent warning fatigue
        if (fingerprint == lastFingerprint && (now - lastTime < COOLDOWN_MS)) {
            return null
        }

        // 5. Pre-action relevance check: Only trigger inference if actionable cues exist
        val hasUrl = !metadata.activeUrl.isNullOrBlank()
        val hasPreActionControl = metadata.candidateAction != ScreenActionType.OPEN
        val hasText = metadata.visibleTextFragments.isNotEmpty()

        if (!hasUrl && !hasPreActionControl && !hasText) {
            return null
        }

        // 6. Map to UiScreenEvent for OnDeviceDecisionEngine
        val screenType = when {
            metadata.candidateAction == ScreenActionType.LOGIN -> ScreenContextType.LOGIN
            metadata.candidateAction == ScreenActionType.PAY || metadata.candidateAction == ScreenActionType.APPROVE -> ScreenContextType.PAYMENT
            metadata.candidateAction == ScreenActionType.POST -> ScreenContextType.SOCIAL
            metadata.candidateAction == ScreenActionType.SEND -> ScreenContextType.CHAT
            hasUrl -> ScreenContextType.BROWSER
            else -> ScreenContextType.UNKNOWN
        }

        val draftText = metadata.visibleTextFragments.joinToString(" ")
        val uiEvent = UiScreenEvent(
            packageName = packageName,
            screenType = screenType,
            activeUrl = metadata.activeUrl,
            inputDraft = draftText.take(1000), // Bounded sample for PII inspection
            focusedAction = metadata.candidateAction.name,
            isPasswordContext = false,
            timestampMs = now
        )

        // 7. Execute Fast On-Device Risk Inference (< 10ms)
        val safetyResult = OnDeviceDecisionEngine.evaluateScreenEvent(uiEvent)

        // Update tracking state
        lastInferenceTimestamp[packageName] = now
        lastFingerprintPerPackage[packageName] = fingerprint

        // 8. Record privacy-safe audit entry (NEVER stores raw text or messages)
        ScreenGuardStateManager.recordAuditEntry(
            AuditLogEntry(
                timestampMs = now,
                packageName = packageName,
                screenIdentifier = metadata.screenIdentifier,
                candidateAction = metadata.candidateAction.name,
                intervention = safetyResult.intervention.name,
                riskScore = safetyResult.riskScore,
                latencyMs = safetyResult.latencyMs,
                evidenceSummary = safetyResult.evidence.firstOrNull() ?: "Evaluated on-device"
            )
        )

        return safetyResult
    }

    /**
     * Records a user dismissal for a specific screen state fingerprint to prevent warning fatigue.
     */
    fun recordDismissal(metadata: ScreenContextMetadata) {
        val fingerprint = computeFingerprint(metadata)
        dismissedFingerprints.add(fingerprint)
    }

    /**
     * Computes a SHA-256 fingerprint from package, candidate action, URL, and normalized text.
     */
    fun computeFingerprint(metadata: ScreenContextMetadata): String {
        val raw = StringBuilder()
            .append(metadata.packageName).append("|")
            .append(metadata.candidateAction.name).append("|")
            .append(metadata.activeUrl ?: "").append("|")
            .append(metadata.buttonLabels.sorted().joinToString(",")).append("|")
            .append(metadata.visibleTextFragments.take(10).joinToString(";"))
            .toString()

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }.take(16)
        } catch (e: Exception) {
            raw.hashCode().toString()
        }
    }

    fun clearState() {
        lastInferenceTimestamp.clear()
        lastFingerprintPerPackage.clear()
        dismissedFingerprints.clear()
    }
}
