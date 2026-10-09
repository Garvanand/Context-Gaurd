package com.contextguard.app.core.notification

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.contextguard.app.core.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Lifecycle, deduplication, and ephemeral audit state manager for Notification Threat Triage.
 *
 * ARCHITECTURAL INVARIANTS:
 * - Real-time tracking of NotificationListenerService connection state.
 * - Anti-fatigue / battery saver: Deduplicates identical notification states.
 * - Hardware / Software kill-switch support (immediate pause/resume).
 * - Ephemeral audit ledger (capped at 50 items, strictly zero raw message text).
 */
object NotificationGuardStateManager {

    private const val MAX_FINGERPRINTS = 200
    private const val MAX_AUDIT_RECORDS = 50

    private val _isListenerConnected = MutableStateFlow(false)
    val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

    private val _isProtectionPaused = MutableStateFlow(false)
    val isProtectionPaused: StateFlow<Boolean> = _isProtectionPaused.asStateFlow()

    private val _recentAuditRecords = MutableStateFlow<List<NotificationAuditRecord>>(emptyList())
    val recentAuditRecords: StateFlow<List<NotificationAuditRecord>> = _recentAuditRecords.asStateFlow()

    // Bounded deduplication fingerprint ledger
    private val seenFingerprints = ConcurrentHashMap.newKeySet<String>()
    private val notificationKeyMap = ConcurrentHashMap<String, String>() // notificationKey -> fingerprint

    fun onListenerConnected() {
        _isListenerConnected.value = true
        AppLogger.i("NotificationGuardService connected to Android NotificationManager.")
    }

    fun onListenerDisconnected() {
        _isListenerConnected.value = false
        AppLogger.w("NotificationGuardService disconnected.")
    }

    fun pauseProtection() {
        _isProtectionPaused.value = true
        AppLogger.i("Notification Threat Triage paused by user.")
    }

    fun resumeProtection() {
        _isProtectionPaused.value = false
        AppLogger.i("Notification Threat Triage resumed by user.")
    }

    fun toggleProtection() {
        _isProtectionPaused.update { !it }
    }

    /**
     * Determines whether incoming notification events should be triaged.
     */
    fun canProcessNotifications(): Boolean {
        return _isListenerConnected.value && !_isProtectionPaused.value
    }

    /**
     * Computes a privacy-preserving fingerprint for deduplication.
     */
    fun computeNotificationFingerprint(
        packageName: String,
        id: Int,
        postTime: Long,
        messageLength: Int
    ): String {
        val raw = "$packageName|$id|$postTime|$messageLength"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }.take(16)
        } catch (e: Exception) {
            raw.hashCode().toString()
        }
    }

    /**
     * Checks if this notification state was already processed within the recent window.
     */
    fun isDuplicate(fingerprint: String): Boolean {
        return seenFingerprints.contains(fingerprint)
    }

    /**
     * Records a notification fingerprint to prevent repeated analysis of identical states.
     */
    fun recordFingerprint(notificationKey: String, fingerprint: String) {
        if (seenFingerprints.size >= MAX_FINGERPRINTS) {
            seenFingerprints.clear()
            notificationKeyMap.clear()
        }
        seenFingerprints.add(fingerprint)
        notificationKeyMap[notificationKey] = fingerprint
    }

    /**
     * Evicts tracking for a removed notification.
     */
    fun onNotificationRemoved(notificationKey: String) {
        val fingerprint = notificationKeyMap.remove(notificationKey)
        if (fingerprint != null) {
            seenFingerprints.remove(fingerprint)
        }
    }

    /**
     * Records a privacy-safe audit record in volatile memory.
     * INVARIANT: Never stores raw notification text.
     */
    fun recordAudit(record: NotificationAuditRecord) {
        _recentAuditRecords.update { current ->
            (listOf(record) + current).take(MAX_AUDIT_RECORDS)
        }
    }

    fun clearAuditLog() {
        _recentAuditRecords.value = emptyList()
    }

    /**
     * Checks whether Notification Access is granted to ContextGuard in Android System Settings.
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    fun resetForTesting() {
        _isListenerConnected.value = false
        _isProtectionPaused.value = false
        _recentAuditRecords.value = emptyList()
        seenFingerprints.clear()
        notificationKeyMap.clear()
    }
}
