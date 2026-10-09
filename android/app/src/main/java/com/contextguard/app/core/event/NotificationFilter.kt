package com.contextguard.app.core.event

import java.util.regex.Pattern

/**
 * Filters and extracts evidence from system notifications.
 */
object NotificationFilter {

    private val URL_PATTERN = Pattern.compile(
        "(https?://(?:[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})+|(?:\\d{1,3}\\.){3}\\d{1,3})(?::\\d+)?(?:/[^\\s]*)?)",
        Pattern.CASE_INSENSITIVE
    )

    private val SCAM_KEYWORDS = listOf(
        "urgent", "suspended", "account blocked", "kyc update", "verify identity",
        "electricity bill overdue", "debit alert", "lottery", "unclaimed", "reward points",
        "click here to avoid cancellation", "immediate action required"
    )

    private val OTP_KEYWORDS = listOf(
        "otp", "one-time password", "verification code", "secret pin", "do not share"
    )

    /**
     * Determines whether this notification should be ingested.
     */
    fun shouldProcessNotification(packageName: String, isOngoing: Boolean): Boolean {
        if (!ProtectionStateManager.isProtectionActive()) return false
        if (isOngoing) return false // Ignore media playback, foreground services, ongoing downloads
        return ProtectionStateManager.isPackageAllowlisted(packageName)
    }

    /**
     * Extracts URLs from notification text.
     */
    fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = URL_PATTERN.matcher(text)
        while (matcher.find()) {
            matcher.group(1)?.let { urls.add(it) }
        }
        return urls
    }

    /**
     * Checks if notification contains urgency or scam lures.
     */
    fun hasScamLure(text: String): Boolean {
        val lower = text.lowercase()
        return SCAM_KEYWORDS.any { lower.contains(it) }
    }

    /**
     * Checks if notification contains high-consequence OTP or credential tokens.
     */
    fun hasOtpContext(text: String): Boolean {
        val lower = text.lowercase()
        return OTP_KEYWORDS.any { lower.contains(it) }
    }
}
