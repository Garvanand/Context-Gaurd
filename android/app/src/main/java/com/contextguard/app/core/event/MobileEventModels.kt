package com.contextguard.app.core.event

/**
 * Screen context taxonomy identified from active window hierarchy.
 */
enum class ScreenContextType {
    BROWSER,
    CHAT,
    LOGIN,
    PAYMENT,
    SOCIAL,
    UNKNOWN
}

/**
 * Ephemeral screen context event extracted from active window.
 * INVARIANT: Never contains raw password field contents.
 */
data class UiScreenEvent(
    val packageName: String,
    val screenType: ScreenContextType = ScreenContextType.UNKNOWN,
    val activeUrl: String? = null,
    val inputDraft: String? = null,
    val focusedAction: String? = null,
    val isPasswordContext: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Ephemeral notification event extracted via NotificationListenerService.
 */
data class NotificationEvent(
    val packageName: String,
    val title: String,
    val text: String,
    val extractedUrls: List<String> = emptyList(),
    val isOngoing: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Global configuration and state for background event protection.
 */
data class ProtectionConfig(
    val isProtectionEnabled: Boolean = true,
    val allowlistedPackages: Set<String> = DEFAULT_ALLOWLIST,
    val overlayEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val minConfidenceThreshold: Float = 0.70f,
    val lambda: Float = 0.75f
) {
    companion object {
        val DEFAULT_ALLOWLIST: Set<String> = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser",
            "com.whatsapp",
            "org.telegram.messenger",
            "com.google.android.gm",
            "com.twitter.android",
            "com.instagram.android",
            "com.reddit.frontpage",
            "com.google.android.apps.messaging"
        )
    }
}
