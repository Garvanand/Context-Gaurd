package com.contextguard.app.core.accessibility

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Represents a target application eligible for pre-action screen context protection.
 */
data class TargetAppInfo(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val isInstalled: Boolean,
    val isEnabled: Boolean
)

enum class AppCategory {
    BROWSER,
    MESSAGING,
    EMAIL,
    SOCIAL,
    PAYMENT
}

/**
 * Manages the user-configurable application allowlist.
 *
 * INVARIANTS:
 * - Only inspects enabled packages.
 * - Queries PackageManager respecting Android 11+ platform visibility rules.
 * - If an app is not installed, it is marked unavailable rather than fabricating a connection.
 */
object AllowlistManager {

    private val DEFAULT_TARGET_APPS = listOf(
        // Browsers
        TargetAppInfo("com.android.chrome", "Google Chrome", AppCategory.BROWSER, isInstalled = false, isEnabled = true),
        TargetAppInfo("org.mozilla.firefox", "Firefox", AppCategory.BROWSER, isInstalled = false, isEnabled = true),
        TargetAppInfo("com.sec.android.app.sbrowser", "Samsung Internet", AppCategory.BROWSER, isInstalled = false, isEnabled = false),
        TargetAppInfo("com.microsoft.emmx", "Microsoft Edge", AppCategory.BROWSER, isInstalled = false, isEnabled = false),

        // Messaging
        TargetAppInfo("com.whatsapp", "WhatsApp", AppCategory.MESSAGING, isInstalled = false, isEnabled = true),
        TargetAppInfo("org.telegram.messenger", "Telegram", AppCategory.MESSAGING, isInstalled = false, isEnabled = true),
        TargetAppInfo("com.google.android.apps.messaging", "Google Messages", AppCategory.MESSAGING, isInstalled = false, isEnabled = true),
        TargetAppInfo("org.thoughtcrime.securesms", "Signal", AppCategory.MESSAGING, isInstalled = false, isEnabled = false),

        // Email
        TargetAppInfo("com.google.android.gm", "Gmail", AppCategory.EMAIL, isInstalled = false, isEnabled = true),
        TargetAppInfo("com.microsoft.office.outlook", "Microsoft Outlook", AppCategory.EMAIL, isInstalled = false, isEnabled = false),

        // Social
        TargetAppInfo("com.twitter.android", "X (Twitter)", AppCategory.SOCIAL, isInstalled = false, isEnabled = true),
        TargetAppInfo("com.reddit.frontpage", "Reddit", AppCategory.SOCIAL, isInstalled = false, isEnabled = false),

        // Supported Payment Screens
        TargetAppInfo("com.google.android.apps.nbu.paisa.user", "Google Pay (Pre-Screen)", AppCategory.PAYMENT, isInstalled = false, isEnabled = false),
        TargetAppInfo("com.phonepe.app", "PhonePe (Pre-Screen)", AppCategory.PAYMENT, isInstalled = false, isEnabled = false)
    )

    private val _targetApps = MutableStateFlow(DEFAULT_TARGET_APPS)
    val targetApps: StateFlow<List<TargetAppInfo>> = _targetApps.asStateFlow()

    /**
     * Refreshes installed app statuses via PackageManager.
     */
    fun refreshInstalledApps(context: Context) {
        val pm = context.packageManager
        _targetApps.update { currentList ->
            currentList.map { app ->
                val installed = try {
                    pm.getPackageInfo(app.packageName, 0) != null
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                } catch (e: Exception) {
                    false
                }
                app.copy(isInstalled = installed)
            }
        }
    }

    /**
     * Toggles the user-enabled monitoring switch for a specific package.
     */
    fun toggleApp(packageName: String, enabled: Boolean) {
        _targetApps.update { currentList ->
            currentList.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isEnabled = enabled)
                } else {
                    app
                }
            }
        }
    }

    /**
     * Determines whether events from this package should be processed.
     */
    fun isPackageAllowed(packageName: String): Boolean {
        val app = _targetApps.value.find { it.packageName == packageName }
        // Must be in allowlist and user-enabled
        return app?.isEnabled == true
    }

    /**
     * For unit test setup.
     */
    fun setAppInstalledAndEnabledForTesting(packageName: String, installed: Boolean, enabled: Boolean) {
        _targetApps.update { list ->
            list.map { if (it.packageName == packageName) it.copy(isInstalled = installed, isEnabled = enabled) else it }
        }
    }

    fun resetToDefaults() {
        _targetApps.value = DEFAULT_TARGET_APPS
    }
}
