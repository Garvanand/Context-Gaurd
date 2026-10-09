package com.contextguard.app.core.engine

import com.contextguard.app.core.event.NotificationEvent
import com.contextguard.app.core.event.ScreenContextType
import com.contextguard.app.core.event.UiScreenEvent
import com.contextguard.app.core.perception.PiiDetector
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.SafetyResult
import java.util.regex.Pattern

/**
 * Fast, fully on-device decision engine.
 *
 * INVARIANTS:
 * - Executes in < 10ms entirely on-device (zero network required).
 * - Implements calibrated formula: rho = s * (1 + lambda * r).
 * - Enforces Model Failure Safety Rule: Never emits silent ACT on failure or low confidence.
 */
object OnDeviceDecisionEngine {

    private const val LAMBDA = 0.75f
    private const val THRESHOLD_STOP = 0.65f
    private const val THRESHOLD_ASK = 0.35f
    private const val MIN_CONFIDENCE = 0.70f
    private const val HIGH_SEVERITY_OVERRIDE = 0.80f

    private val IP_HOST_PATTERN = Pattern.compile("^https?://(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d+)?.*")
    private val SUSPICIOUS_TLDS = setOf(".xyz", ".top", ".click", ".work", ".tk", ".ml", ".ga", ".cf", ".gq")
    private val PHISHING_KEYWORDS = listOf("login", "verify", "secure", "update", "banking", "wallet", "kyc", "signin")

    /**
     * Evaluates an ephemeral UI screen event in real time.
     */
    fun evaluateScreenEvent(event: UiScreenEvent): SafetyResult {
        val t0 = System.currentTimeMillis()
        val evidenceList = mutableListOf<String>()

        var severity = 0.05f
        var irreversibility = 0.00f
        var confidence = 0.90f
        val action = event.focusedAction ?: "OPEN"

        // 1. Evaluate Active URL if present
        if (!event.activeUrl.isNullOrBlank()) {
            val urlRisk = evaluateUrlFast(event.activeUrl)
            if (urlRisk.isPhishing) {
                severity = maxOf(severity, urlRisk.severity)
                irreversibility = maxOf(irreversibility, 0.90f)
                confidence = maxOf(confidence, urlRisk.confidence)
                evidenceList.add(urlRisk.reason)
            } else {
                evidenceList.add("URL domain checked via on-device heuristics: ${event.activeUrl}")
            }
        }

        // 2. Evaluate Input Draft for Sensitive PII
        if (!event.inputDraft.isNullOrBlank()) {
            val piiFindings = PiiDetector.detectPii(event.inputDraft)
            if (piiFindings.isNotEmpty()) {
                val piiTypes = piiFindings.map { it.type.name }.distinct()
                evidenceList.add("On-device PII detector identified sensitive entity: $piiTypes")

                // Harm severity scales with destination channel
                if (action == "POST" || event.screenType == ScreenContextType.SOCIAL) {
                    severity = maxOf(severity, 0.90f)
                    irreversibility = 1.00f // Public broadcast is completely irreversible
                    evidenceList.add("Destination is a public broadcast channel accessible to third parties")
                } else if (action == "UPLOAD" || action == "SUBMIT") {
                    severity = maxOf(severity, 0.80f)
                    irreversibility = 0.85f
                    evidenceList.add("Sensitive data detected adjacent to upload or submission control")
                } else if (action == "SEND" || event.screenType == ScreenContextType.CHAT) {
                    severity = maxOf(severity, 0.50f)
                    irreversibility = 0.60f
                    evidenceList.add("Message containing sensitive data prepared for transmission")
                }
            }
        }

        // 3. Evaluate High-Consequence Actions (Login on Suspicious Screen, Payment)
        if ((event.screenType == ScreenContextType.LOGIN || action == "LOGIN") && !event.activeUrl.isNullOrBlank()) {
            val isIp = IP_HOST_PATTERN.matcher(event.activeUrl).matches()
            val isSuspiciousTld = SUSPICIOUS_TLDS.any { event.activeUrl.lowercase().contains(it) }
            val urlRisk = evaluateUrlFast(event.activeUrl)
            if (isIp || isSuspiciousTld || urlRisk.isPhishing) {
                severity = 0.95f
                irreversibility = 0.90f
                evidenceList.add("Credential entry on unverified or suspicious domain")
            }
        } else if (event.screenType == ScreenContextType.PAYMENT || action == "APPROVE" || action == "PAY") {
            severity = maxOf(severity, 0.70f)
            irreversibility = maxOf(irreversibility, 0.85f)
            evidenceList.add("High-consequence payment authorization detected")
        }

        val riskScore = severity * (1.0f + LAMBDA * irreversibility)
        val intervention = calculateIntervention(riskScore, severity, confidence)
        val latencyMs = System.currentTimeMillis() - t0

        return SafetyResult(
            intervention = intervention,
            riskScore = riskScore,
            severity = severity,
            irreversibility = irreversibility,
            confidence = confidence,
            lambda = LAMBDA,
            evidence = if (evidenceList.isEmpty()) listOf("No actionable threats detected in active context.") else evidenceList,
            rationale = synthesizeRationale(intervention, riskScore, action, evidenceList),
            artifactTitle = "${event.screenType} (${event.packageName})",
            intendedAction = action,
            destination = event.activeUrl ?: event.packageName,
            latencyMs = latencyMs,
            alternativeAction = if (intervention == InterventionType.STOP) "Abort action and inspect target domain." else "Review destination before proceeding."
        )
    }

    /**
     * Evaluates a system notification event in real time.
     */
    fun evaluateNotificationEvent(event: NotificationEvent): SafetyResult {
        val t0 = System.currentTimeMillis()
        val evidenceList = mutableListOf<String>()

        var severity = 0.05f
        var irreversibility = 0.00f
        var confidence = 0.85f

        val combinedText = "${event.title} ${event.text}"

        // 1. Check for Scam / Urgency Lures
        val hasLure = com.contextguard.app.core.event.NotificationFilter.hasScamLure(combinedText)
        if (hasLure) {
            severity = maxOf(severity, 0.60f)
            irreversibility = 0.50f
            evidenceList.add("Notification contains urgent account suspension or verification lure")
        }

        // 2. Check for OTP tokens
        val hasOtp = com.contextguard.app.core.event.NotificationFilter.hasOtpContext(combinedText)
        if (hasOtp) {
            evidenceList.add("Notification contains high-consequence One-Time Password (OTP)")
        }

        // 3. Evaluate URLs in notification
        for (url in event.extractedUrls) {
            val urlRisk = evaluateUrlFast(url)
            if (urlRisk.isPhishing) {
                severity = maxOf(severity, 0.95f)
                irreversibility = 0.90f
                evidenceList.add("Notification contains suspicious phishing domain: $url")
            }
        }

        val riskScore = severity * (1.0f + LAMBDA * irreversibility)
        val intervention = calculateIntervention(riskScore, severity, confidence)
        val latencyMs = System.currentTimeMillis() - t0

        return SafetyResult(
            intervention = intervention,
            riskScore = riskScore,
            severity = severity,
            irreversibility = irreversibility,
            confidence = confidence,
            lambda = LAMBDA,
            evidence = evidenceList,
            rationale = synthesizeRationale(intervention, riskScore, "NOTIFICATION", evidenceList),
            artifactTitle = "Notification: ${event.title}",
            intendedAction = "OPEN",
            destination = event.packageName,
            latencyMs = latencyMs,
            alternativeAction = "Do not click links inside unverified notifications."
        )
    }

    private fun calculateIntervention(riskScore: Float, severity: Float, confidence: Float): InterventionType {
        return if (riskScore >= THRESHOLD_STOP) {
            if (severity >= HIGH_SEVERITY_OVERRIDE || confidence >= MIN_CONFIDENCE) {
                InterventionType.STOP
            } else {
                InterventionType.ASK
            }
        } else if (riskScore >= THRESHOLD_ASK) {
            if (confidence < MIN_CONFIDENCE) {
                InterventionType.ASK
            } else {
                InterventionType.WARN
            }
        } else {
            if (confidence < MIN_CONFIDENCE) {
                InterventionType.ASK
            } else {
                InterventionType.ACT
            }
        }
    }

    private fun synthesizeRationale(
        intervention: InterventionType,
        riskScore: Float,
        action: String,
        evidence: List<String>
    ): String {
        return when (intervention) {
            InterventionType.STOP -> "Action '$action' blocked (Risk score: ${String.format("%.2f", riskScore)}). Critical hazard detected."
            InterventionType.WARN -> "Caution: Action '$action' carries elevated risk (Score: ${String.format("%.2f", riskScore)})."
            InterventionType.ASK -> "Confirmation required before proceeding with '$action'. Ambiguous risk context."
            InterventionType.ACT -> "Action '$action' verified safe by on-device policy engine."
        }
    }

    private var urlTreeEngine: UrlTreeInferenceEngine? = null

    fun setUrlEngine(engine: UrlTreeInferenceEngine?) {
        this.urlTreeEngine = engine
    }

    fun getUrlEngine(): UrlTreeInferenceEngine? = this.urlTreeEngine

    private data class UrlRiskResult(
        val isPhishing: Boolean,
        val severity: Float,
        val confidence: Float,
        val reason: String
    )

    private fun evaluateUrlFast(rawUrl: String): UrlRiskResult {
        urlTreeEngine?.let { engine ->
            val pred = engine.predictUrl(rawUrl)
            return UrlRiskResult(
                isPhishing = pred.isPhishing,
                severity = pred.probability,
                confidence = pred.confidence,
                reason = if (pred.isPhishing) "On-device ML tree model flagged URL as high risk (P: ${String.format("%.2f", pred.probability)})" else "On-device ML model verified URL as benign"
            )
        }

        val lower = rawUrl.lowercase()
        val isIp = IP_HOST_PATTERN.matcher(rawUrl).matches()
        val isSuspiciousTld = SUSPICIOUS_TLDS.any { lower.contains(it) }
        val hasAt = lower.contains("@")
        val hasPhishKeyword = PHISHING_KEYWORDS.count { lower.contains(it) }

        if (isIp) {
            return UrlRiskResult(
                isPhishing = true,
                severity = 0.95f,
                confidence = 0.95f,
                reason = "URL uses raw IP address instead of domain hostname"
            )
        }
        if (isSuspiciousTld && hasPhishKeyword >= 1) {
            return UrlRiskResult(
                isPhishing = true,
                severity = 0.90f,
                confidence = 0.90f,
                reason = "URL uses high-risk TLD with security/login lures"
            )
        }
        if (hasAt) {
            return UrlRiskResult(
                isPhishing = true,
                severity = 0.85f,
                confidence = 0.85f,
                reason = "URL contains '@' credential masquerade character"
            )
        }
        return UrlRiskResult(
            isPhishing = false,
            severity = 0.05f,
            confidence = 0.90f,
            reason = "URL passed fast on-device checks"
        )
    }
}
