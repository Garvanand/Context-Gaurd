package com.contextguard.app.core.notification

import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.SafetyResult
import java.net.URI
import java.util.regex.Pattern

/**
 * High-performance, in-memory Notification Threat Triage Analyzer.
 *
 * ARCHITECTURAL INVARIANTS:
 * - Cached in-memory singleton; zero model loading per event.
 * - Processing time bounded to < 10ms.
 * - Extracts only transient features; NEVER stores the raw notification payload.
 * - CRITICAL RULE: The mere presence of urgent language is NOT treated as proof of fraud.
 * - Uses cautious wording (ASK/WARN) when evidence is ambiguous rather than false STOP.
 * - Explains the strongest available evidence in generated alerts.
 */
object NotificationThreatAnalyzer {

    private const val LAMBDA = 0.75f
    private const val THRESHOLD_STOP = 0.65f
    private const val THRESHOLD_ASK = 0.35f
    private const val MIN_CONFIDENCE = 0.70f

    private var urlTreeEngine: com.contextguard.app.core.engine.UrlTreeInferenceEngine? = null
    private var messageClassifier: com.contextguard.app.core.engine.ScamMessageClassifier? = null

    fun setModels(
        urlEngine: com.contextguard.app.core.engine.UrlTreeInferenceEngine?,
        msgClassifier: com.contextguard.app.core.engine.ScamMessageClassifier?
    ) {
        this.urlTreeEngine = urlEngine
        this.messageClassifier = msgClassifier
    }

    // URL Extraction Regex
    private val URL_PATTERN = Pattern.compile(
        "(https?://(?:[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})+|(?:\\d{1,3}\\.){3}\\d{1,3})(?::\\d+)?(?:/[^\\s]*)?)",
        Pattern.CASE_INSENSITIVE
    )

    private val IP_HOST_PATTERN = Pattern.compile("^https?://(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d+)?.*")
    private val SUSPICIOUS_TLDS = setOf(".xyz", ".top", ".click", ".work", ".tk", ".ml", ".ga", ".cf", ".gq", ".live", ".icu")
    private val PHISHING_PATH_KEYWORDS = listOf("login", "verify", "secure", "update", "banking", "wallet", "kyc", "signin", "otp", "bill", "pay")

    // Urgency indicators
    private val URGENCY_KEYWORDS = listOf(
        "immediate action required", "immediately", "within 24 hours", "account will be blocked",
        "service suspended", "electricity disconnected tonight", "power cut", "urgent notice",
        "final warning", "arrest warrant", "legal notice", "penalty overdue", "act now", "last chance"
    )

    // Credential & OTP indicators
    private val CREDENTIAL_KEYWORDS = listOf(
        "enter password", "provide your password", "login credentials", "verify pin", "atm pin",
        "secret pin", "netbanking password", "submit login"
    )

    private val OTP_KEYWORDS = listOf(
        "share otp", "send otp", "forward otp", "share your 6-digit", "share 4-digit", "provide otp",
        "verification code to agent", "otp required to cancel", "one-time password", "otp is"
    )

    // Coercive Payment indicators
    private val PAYMENT_KEYWORDS = listOf(
        "pay immediately", "transfer to upi", "settle overdue", "pay fine", "avoid disconnection pay",
        "send money to", "pay bill immediately", "transfer penalty"
    )

    // Known Impersonated Entities
    private val ENTITY_LEXICON = listOf(
        OrganizationEntity(
            name = "HDFC Bank",
            keywords = listOf("hdfc", "hdfc bank", "hdfcbank"),
            officialDomains = listOf("hdfcbank.com", "hdfc.com"),
            category = "BANK"
        ),
        OrganizationEntity(
            name = "State Bank of India (SBI)",
            keywords = listOf("sbi", "state bank of india", "onlinesbi", "yono"),
            officialDomains = listOf("sbi.co.in", "onlinesbi.sbi", "onlinesbi.com", "sbi.bank"),
            category = "BANK"
        ),
        OrganizationEntity(
            name = "ICICI Bank",
            keywords = listOf("icici", "icici bank", "imobile"),
            officialDomains = listOf("icicibank.com"),
            category = "BANK"
        ),
        OrganizationEntity(
            name = "Axis Bank",
            keywords = listOf("axis bank", "axisbank"),
            officialDomains = listOf("axisbank.com"),
            category = "BANK"
        ),
        OrganizationEntity(
            name = "Electricity Department",
            keywords = listOf("electricity bill", "power department", "bijli", "electricity board", "discom", "power disconnection"),
            officialDomains = listOf("bescom.co.in", "tneb.gov.in", "mahadiscom.in", "uppcl.org"),
            category = "UTILITY"
        ),
        OrganizationEntity(
            name = "Income Tax Department",
            keywords = listOf("income tax", "itr refund", "tax department", "cbdh"),
            officialDomains = listOf("incometax.gov.in", "incometaxindia.gov.in"),
            category = "GOV"
        ),
        OrganizationEntity(
            name = "India Post",
            keywords = listOf("india post", "indiapost", "parcel delivery failed", "postal delivery"),
            officialDomains = listOf("indiapost.gov.in"),
            category = "GOV"
        ),
        OrganizationEntity(
            name = "Netflix",
            keywords = listOf("netflix", "netflix subscription", "netflix payment"),
            officialDomains = listOf("netflix.com"),
            category = "TECH"
        ),
        OrganizationEntity(
            name = "WhatsApp Security",
            keywords = listOf("whatsapp support", "whatsapp verification", "whatsapp security"),
            officialDomains = listOf("whatsapp.com"),
            category = "TECH"
        )
    )

    /**
     * Extracts transient features from raw notification texts.
     * PRIVACY: Never buffers or persists rawText.
     */
    fun extractFeatures(
        packageName: String,
        category: String?,
        title: String?,
        text: String?
    ): NotificationFeatures {
        val safeTitle = title ?: ""
        val safeText = text ?: ""
        val combined = "$safeTitle $safeText"
        val lower = combined.lowercase()

        val length = combined.length

        // 1. URL candidates extraction
        val extractedUrls = mutableListOf<String>()
        val matcher = URL_PATTERN.matcher(combined)
        while (matcher.find()) {
            matcher.group(1)?.let { extractedUrls.add(it) }
        }

        // 2. Local URL risk analysis
        val suspiciousUrls = mutableListOf<String>()
        for (url in extractedUrls) {
            val urlAnalysis = analyzeUrl(url)
            if (urlAnalysis.isSuspicious) {
                suspiciousUrls.add(url)
            }
        }

        // 3. Impersonation & Domain Mismatch Detection
        var detectedEntity: OrganizationEntity? = null
        for (entity in ENTITY_LEXICON) {
            if (entity.keywords.any { lower.contains(it) }) {
                detectedEntity = entity
                break
            }
        }

        val domainMismatches = mutableListOf<String>()
        if (detectedEntity != null && extractedUrls.isNotEmpty()) {
            for (url in extractedUrls) {
                val host = extractDomain(url).lowercase()
                val isOfficial = detectedEntity.officialDomains.any { official ->
                    host == official || host.endsWith(".$official")
                }
                if (!isOfficial) {
                    domainMismatches.add(
                        "Claims to be ${detectedEntity.name}, but destination domain is '$host' (unverified)"
                    )
                }
            }
        }

        // 4. Credential & OTP indicators
        val hasCredential = CREDENTIAL_KEYWORDS.any { lower.contains(it) }
        val hasOtp = OTP_KEYWORDS.any { lower.contains(it) }
        val requestsCredentialsOrOtp = hasCredential || hasOtp

        // 5. Coercive Payment indicators
        val requestsImmediatePayment = PAYMENT_KEYWORDS.any { lower.contains(it) }

        // 6. Urgency scoring
        val urgencyMatches = URGENCY_KEYWORDS.count { lower.contains(it) }
        val urgencyScore = (urgencyMatches * 0.35f).coerceAtMost(1.0f)

        // 7. Linguistic risk score
        var linguisticScore = 0.0f
        if (urgencyMatches > 0) linguisticScore += 0.3f
        if (requestsCredentialsOrOtp) linguisticScore += 0.4f
        if (requestsImmediatePayment) linguisticScore += 0.3f

        messageClassifier?.let { classifier ->
            val mlPred = classifier.predict(combined)
            if (mlPred.isPhishing) {
                linguisticScore = maxOf(linguisticScore, mlPred.probability)
            }
        }
        linguisticScore = linguisticScore.coerceAtMost(1.0f)

        // 8. Truncated / Incomplete state
        val isTruncated = safeText.endsWith("...") ||
                safeText.endsWith("\u2026") ||
                (safeText.length < 25 && extractedUrls.isNotEmpty())

        return NotificationFeatures(
            sourcePackage = packageName,
            notificationCategory = category,
            messageLength = length,
            extractedUrls = extractedUrls,
            suspiciousUrls = suspiciousUrls,
            urgencyScore = urgencyScore,
            impersonationEntity = detectedEntity?.name,
            requestsCredentialsOrOtp = requestsCredentialsOrOtp,
            requestsImmediatePayment = requestsImmediatePayment,
            domainMismatches = domainMismatches,
            linguisticRiskScore = linguisticScore,
            isTruncated = isTruncated
        )
    }

    /**
     * Executes fast on-device threat triage over extracted features.
     */
    fun analyze(features: NotificationFeatures): SafetyResult {
        val t0 = System.currentTimeMillis()
        val evidenceList = mutableListOf<String>()

        var severity = 0.05f
        var irreversibility = 0.00f
        var confidence = 0.90f

        // 1. Evidence Grounding: Domain Mismatch / Impersonation (High Confidence Fraud)
        if (features.domainMismatches.isNotEmpty()) {
            severity = maxOf(severity, 0.95f)
            irreversibility = maxOf(irreversibility, 0.90f)
            val mismatch = features.domainMismatches.first()
            evidenceList.add("Potential impersonation: the message requests an action through a domain that does not match the claimed organization ($mismatch).")
        }

        // 2. Evidence Grounding: OTP or Credential Theft
        if (features.requestsCredentialsOrOtp) {
            severity = maxOf(severity, 0.90f)
            irreversibility = maxOf(irreversibility, 0.95f)
            evidenceList.add("Critical security hazard: message explicitly solicits a confidential authentication password or OTP.")
        }

        // 3. Evidence Grounding: Suspicious Phishing URL Candidates
        if (features.suspiciousUrls.isNotEmpty()) {
            severity = maxOf(severity, 0.85f)
            irreversibility = maxOf(irreversibility, 0.85f)
            evidenceList.add("Suspicious destination URL detected with high-risk lexical indicators: ${features.suspiciousUrls.first()}.")
        }

        // 4. Evidence Grounding: Coercive Urgent Payment Demands
        if (features.requestsImmediatePayment && features.urgencyScore > 0f) {
            severity = maxOf(severity, 0.75f)
            irreversibility = maxOf(irreversibility, 0.85f)
            evidenceList.add("Coercive payment demand with threat of immediate service disruption or penalties.")
        }

        // 5. CRITICAL RULE: "Do not treat the mere presence of urgent language as proof of fraud"
        // If message has urgent language but NO domain mismatch, NO credential solicitation, NO coercive payment, NO suspicious URL:
        if (features.domainMismatches.isEmpty() &&
            !features.requestsCredentialsOrOtp &&
            !features.requestsImmediatePayment &&
            features.suspiciousUrls.isEmpty()
        ) {
            if (features.extractedUrls.isNotEmpty()) {
                // Urgent language + unverified link with unknown destination = Cautious ambiguity
                severity = 0.40f
                irreversibility = 0.45f
                confidence = 0.65f // Ambiguous evidence
                evidenceList.add("Caution: message conveys urgency with an unverified web link. Confirm sender identity before tapping.")
            } else {
                // Routine urgency (e.g. calendar reminder, flight update, benign chat urgency)
                severity = 0.10f
                irreversibility = 0.00f
                confidence = 0.95f
                evidenceList.add("Routine advisory notification with time-sensitive language. No fraudulent threat signals detected.")
            }
        }

        // 6. Incomplete / Truncated Adjustment
        if (features.isTruncated) {
            confidence = (confidence * 0.85f).coerceAtLeast(0.50f)
            evidenceList.add("Note: notification text appears truncated. Context evaluated conservatively.")
        }

        // Action-Conditioned Calibrated Risk Formula: rho = s * (1 + lambda * r)
        val riskScore = severity * (1.0f + LAMBDA * irreversibility)

        // Calibrated Intervention Thresholds
        val intervention = when {
            riskScore >= THRESHOLD_STOP && confidence >= MIN_CONFIDENCE -> InterventionType.STOP
            riskScore >= THRESHOLD_STOP -> InterventionType.WARN
            riskScore >= THRESHOLD_ASK && confidence < MIN_CONFIDENCE -> InterventionType.ASK
            riskScore >= THRESHOLD_ASK -> InterventionType.WARN
            else -> InterventionType.ACT
        }

        val latencyMs = System.currentTimeMillis() - t0

        val primaryEvidence = evidenceList.firstOrNull() ?: "Evaluated on-device by ContextGuard."

        return SafetyResult(
            intervention = intervention,
            riskScore = riskScore,
            severity = severity,
            irreversibility = irreversibility,
            confidence = confidence,
            lambda = LAMBDA,
            evidence = evidenceList,
            rationale = primaryEvidence,
            artifactTitle = "Notification (${features.sourcePackage})",
            intendedAction = "OPEN_NOTIFICATION",
            destination = features.sourcePackage,
            latencyMs = latencyMs,
            alternativeAction = when (intervention) {
                InterventionType.STOP -> "Do not open links or share credentials requested in this notification."
                InterventionType.WARN -> "Verify sender identity directly through official channels before responding."
                InterventionType.ASK -> "Review destination carefully before proceeding."
                InterventionType.ACT -> "Safe to view."
            }
        )
    }

    /**
     * Fast analyzer for extracted URL candidates using on-device ML tree engine when available.
     */
    fun analyzeUrl(url: String): UrlAnalysisResult {
        val domain = extractDomain(url)

        urlTreeEngine?.let { engine ->
            val pred = engine.predictUrl(url)
            return UrlAnalysisResult(
                url = url,
                domain = domain,
                isSuspicious = pred.isPhishing,
                severity = pred.probability,
                reason = if (pred.isPhishing) "On-device ML model flagged URL (Probability: ${String.format("%.2f", pred.probability)})" else "On-device ML model verified URL domain"
            )
        }

        val lower = url.lowercase()
        val isIp = IP_HOST_PATTERN.matcher(url).matches()
        val isSuspiciousTld = SUSPICIOUS_TLDS.any { domain.endsWith(it) || lower.contains(it) }
        val hasPhishKeyword = PHISHING_PATH_KEYWORDS.count { lower.contains(it) }
        val hasAtSymbol = lower.contains("@")

        if (isIp) {
            return UrlAnalysisResult(url, domain, isSuspicious = true, severity = 0.95f, "URL uses raw IP host address")
        }
        if (isSuspiciousTld && hasPhishKeyword >= 1) {
            return UrlAnalysisResult(url, domain, isSuspicious = true, severity = 0.90f, "High-risk TLD associated with credential harvesting")
        }
        if (hasAtSymbol) {
            return UrlAnalysisResult(url, domain, isSuspicious = true, severity = 0.85f, "URL contains masquerade '@' separator")
        }
        if (isSuspiciousTld) {
            return UrlAnalysisResult(url, domain, isSuspicious = true, severity = 0.70f, "Uncommon high-risk top level domain")
        }

        return UrlAnalysisResult(url, domain, isSuspicious = false, severity = 0.05f, "Standard domain hostname")
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = URI(if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url)
            uri.host ?: url
        } catch (e: Exception) {
            url.substringAfter("://").substringBefore("/").substringBefore(":")
        }
    }
}
