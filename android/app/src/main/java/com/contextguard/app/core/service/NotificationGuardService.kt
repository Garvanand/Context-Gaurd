package com.contextguard.app.core.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.contextguard.app.core.accessibility.AllowlistManager
import com.contextguard.app.core.logging.AppLogger
import com.contextguard.app.core.notification.NotificationAlertDispatcher
import com.contextguard.app.core.notification.NotificationGuardStateManager
import com.contextguard.app.core.notification.NotificationThreatAnalyzer
import com.contextguard.app.ui.viewmodel.InterventionType

/**
 * Real Android Notification Listener Service for ContextGuard.
 *
 * ARCHITECTURAL INVARIANTS:
 * - Ingestion Path B: Subscribes strictly to system notifications from allowlisted packages.
 * - Does NOT request access to SMS storage or Contacts to operate.
 * - Extracts transient features in-memory; strictly NEVER buffers raw notification text.
 * - Deduplicates identical notification states to conserve battery.
 * - Posts evidence-grounded alerts on elevated risk (WARN/STOP/ASK).
 * - ACT is completely silent.
 */
class NotificationGuardService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val urlEngine = com.contextguard.app.core.engine.UrlTreeInferenceEngine.getInstance(applicationContext)
            val msgClassifier = com.contextguard.app.core.engine.ScamMessageClassifier.getInstance(applicationContext)
            NotificationThreatAnalyzer.setModels(urlEngine, msgClassifier)
        } catch (e: Exception) {
            AppLogger.e("Could not attach models in NotificationGuardService: ${e.message}")
        }
        NotificationGuardStateManager.onListenerConnected()
        NotificationAlertDispatcher.ensureChannel(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // 1. Verify global notification triage is active
        if (!NotificationGuardStateManager.canProcessNotifications()) {
            return
        }

        val targetPackage = sbn.packageName ?: return

        // 2. Prevent recursive feedback loop from ContextGuard's own security alerts
        if (targetPackage == applicationContext.packageName) {
            return
        }

        // 3. Package filtering: Only inspect user-allowlisted communication/messaging packages
        if (!AllowlistManager.isPackageAllowed(targetPackage)) {
            return
        }

        // 4. Ignore ongoing notifications (e.g. music playback, sticky foreground service alerts)
        if (sbn.isOngoing) {
            return
        }

        try {
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            // Extract text fragments from standard Notification extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

            if (title.isNullOrBlank() && text.isNullOrBlank()) {
                return
            }

            val messageLength = (title?.length ?: 0) + (text?.length ?: 0)

            // 5. Anti-Fatigue & Deduplication: Skip if identical notification state was already processed
            val fingerprint = NotificationGuardStateManager.computeNotificationFingerprint(
                packageName = targetPackage,
                id = sbn.id,
                postTime = sbn.postTime,
                messageLength = messageLength
            )

            if (NotificationGuardStateManager.isDuplicate(fingerprint)) {
                return
            }
            NotificationGuardStateManager.recordFingerprint(sbn.key, fingerprint)

            val category = notification.category

            // 6. Extract transient threat features (NEVER stores raw text)
            val features = NotificationThreatAnalyzer.extractFeatures(
                packageName = targetPackage,
                category = category,
                title = title,
                text = text
            )

            // 7. Execute Fast On-Device Threat Triage (< 10ms)
            val safetyResult = NotificationThreatAnalyzer.analyze(features)

            AppLogger.i("Notification triage: [${safetyResult.intervention}] for $targetPackage (Risk: ${safetyResult.riskScore})")

            // 8. Post alert if risk is elevated (STOP, WARN, ASK)
            if (safetyResult.intervention != InterventionType.ACT) {
                NotificationAlertDispatcher.postThreatAlert(
                    context = applicationContext,
                    features = features,
                    safetyResult = safetyResult
                )
            }
        } catch (e: Exception) {
            AppLogger.e("Error triaging notification from $targetPackage: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn != null) {
            NotificationGuardStateManager.onNotificationRemoved(sbn.key)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationGuardStateManager.onListenerDisconnected()
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationGuardStateManager.onListenerDisconnected()
    }
}
