package com.contextguard.app.core.event

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton state manager for ContextGuard mobile event protection.
 * Provides thread-safe kill-switch and configuration management.
 */
object ProtectionStateManager {

    private val _config = MutableStateFlow(ProtectionConfig())
    val config: StateFlow<ProtectionConfig> = _config.asStateFlow()

    /**
     * Immediate kill-switch: pauses all event ingestion and hides overlays.
     */
    fun pauseProtection() {
        _config.update { it.copy(isProtectionEnabled = false) }
    }

    /**
     * Resumes background event monitoring.
     */
    fun resumeProtection() {
        _config.update { it.copy(isProtectionEnabled = true) }
    }

    fun isProtectionActive(): Boolean = _config.value.isProtectionEnabled

    fun isPackageAllowlisted(packageName: String): Boolean {
        if (!isProtectionActive()) return false
        return _config.value.allowlistedPackages.contains(packageName)
    }

    fun updateAllowlist(packages: Set<String>) {
        _config.update { it.copy(allowlistedPackages = packages) }
    }

    fun toggleOverlay(enabled: Boolean) {
        _config.update { it.copy(overlayEnabled = enabled) }
    }

    fun toggleNotifications(enabled: Boolean) {
        _config.update { it.copy(notificationsEnabled = enabled) }
    }

    fun resetToDefaults() {
        _config.value = ProtectionConfig()
    }
}
