package com.contextguard.app.core.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Live lifecycle and monitoring state manager for ContextGuard Screen Protection.
 *
 * INVARIANTS:
 * - Does not claim monitoring is active unless the service is genuinely connected and receiving events.
 * - Provides immediate pause/resume kill-switch control.
 * - Manages an in-memory ephemeral audit ledger with an instant erase action.
 */
object ScreenGuardStateManager {

    private const val MAX_AUDIT_ENTRIES = 100

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _isMonitoringPaused = MutableStateFlow(false)
    val isMonitoringPaused: StateFlow<Boolean> = _isMonitoringPaused.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    fun onServiceConnected() {
        _isServiceConnected.value = true
    }

    fun onServiceDisconnected() {
        _isServiceConnected.value = false
    }

    fun pauseMonitoring() {
        _isMonitoringPaused.value = true
        com.contextguard.app.core.overlay.InterventionOverlayManager.dismissActiveOverlay()
    }

    fun resumeMonitoring() {
        _isMonitoringPaused.value = false
    }

    fun toggleMonitoring() {
        _isMonitoringPaused.update { paused ->
            val next = !paused
            if (next) {
                com.contextguard.app.core.overlay.InterventionOverlayManager.dismissActiveOverlay()
            }
            next
        }
    }

    /**
     * True only if the accessibility service is genuinely bound AND the user has not paused monitoring.
     */
    fun canProcessEvents(): Boolean {
        return _isServiceConnected.value && !_isMonitoringPaused.value
    }

    /**
     * Records a privacy-safe audit entry in volatile memory (capped at 100 items).
     */
    fun recordAuditEntry(entry: AuditLogEntry) {
        _auditLogs.update { current ->
            (listOf(entry) + current).take(MAX_AUDIT_ENTRIES)
        }
    }

    /**
     * Erases all ephemeral event metadata in memory.
     */
    fun clearAuditLog() {
        _auditLogs.value = emptyList()
    }

    /**
     * Checks whether the service is enabled in Android System Settings.
     */
    fun isAccessibilityEnabledInSettings(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val targetPackage = context.packageName
        return enabledServices.any {
            it.resolveInfo?.serviceInfo?.packageName == targetPackage
        }
    }

    fun resetForTesting() {
        _isServiceConnected.value = false
        _isMonitoringPaused.value = false
        _auditLogs.value = emptyList()
        com.contextguard.app.core.overlay.InterventionOverlayManager.resetForTesting()
    }
}
