package com.contextguard.app.core.event

import java.util.concurrent.ConcurrentHashMap

/**
 * Filters incoming accessibility events:
 * - Checks allowlist
 * - Enforces 300ms debounce window
 * - Strictly ignores password fields
 */
object AccessibilityEventFilter {

    private const val DEBOUNCE_WINDOW_MS = 300L
    private val lastEventTimestamp = ConcurrentHashMap<String, Long>()

    /**
     * Determines whether an event from this package should be processed.
     */
    fun shouldProcessPackage(packageName: String): Boolean {
        if (!ProtectionStateManager.isProtectionActive()) return false
        return ProtectionStateManager.isPackageAllowlisted(packageName)
    }

    /**
     * Evaluates whether enough time has elapsed since the last event from this package.
     */
    fun shouldDebounce(packageName: String, currentTimestamp: Long = System.currentTimeMillis()): Boolean {
        val lastTime = lastEventTimestamp[packageName] ?: 0L
        if (currentTimestamp - lastTime < DEBOUNCE_WINDOW_MS) {
            return true
        }
        lastEventTimestamp[packageName] = currentTimestamp
        return false
    }

    /**
     * Strict Privacy Barrier: Checks if a node represents a password field.
     * Nodes with isPassword == true MUST NEVER have their text extracted.
     */
    fun isPasswordNode(isPassword: Boolean, inputType: Int = 0): Boolean {
        if (isPassword) return true
        // 0x81 = TYPE_TEXT_VARIATION_PASSWORD, 0x91 = TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, 0x12 = TYPE_NUMBER_VARIATION_PASSWORD
        val passwordVariations = listOf(0x81, 0x91, 0x12, 0x80)
        return passwordVariations.any { (inputType and it) == it }
    }

    /**
     * Extracts browser URL text only from known browser address bar identifiers.
     */
    fun extractBrowserUrl(viewId: String?, rawText: String?): String? {
        if (rawText.isNullOrBlank()) return null
        val trimmed = rawText.trim()
        val isBrowserBar = viewId?.contains("url_bar", ignoreCase = true) == true ||
                viewId?.contains("toolbar", ignoreCase = true) == true ||
                viewId?.contains("location_bar", ignoreCase = true) == true

        if (isBrowserBar || trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.contains(".")) {
            return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
        }
        return null
    }

    /**
     * Maps button text to canonical pre-action intent.
     */
    fun mapButtonToAction(buttonText: String?): String {
        if (buttonText.isNullOrBlank()) return "OPEN"
        val lower = buttonText.trim().lowercase()
        return when {
            lower.contains("login") || lower.contains("sign in") || lower.contains("signin") || lower.contains("verify") -> "LOGIN"
            lower.contains("upload") || lower.contains("attach") -> "UPLOAD"
            lower.contains("send") || lower.contains("submit") || lower.contains("share") -> "SEND"
            lower.contains("post") || lower.contains("tweet") || lower.contains("broadcast") -> "POST"
            lower.contains("pay") || lower.contains("transfer") || lower.contains("approve") -> "APPROVE"
            lower.contains("sign") || lower.contains("consent") -> "SIGN"
            lower.contains("save") || lower.contains("download") -> "SAVE"
            else -> "OPEN"
        }
    }

    fun clearTimestamps() {
        lastEventTimestamp.clear()
    }
}
