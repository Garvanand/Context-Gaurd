package com.contextguard.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.contextguard.app.MainActivity
import com.contextguard.app.core.logging.AppLogger
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.SafetyResult

/**
 * Dispatches high-priority contextual advisory notifications for detected threats.
 *
 * ARCHITECTURAL INVARIANTS:
 * - Posts to dedicated NotificationChannel 'contextguard_threat_alerts'.
 * - Explains the strongest available evidence in the notification body.
 * - Provides immediate PendingIntent to launch ContextGuard for deeper analysis.
 * - Does NOT intercept or prevent external apps from opening links automatically;
 *   advises the user with clear evidence grounding.
 */
object NotificationAlertDispatcher {

    const val CHANNEL_ID = "contextguard_threat_alerts"
    private const val CHANNEL_NAME = "ContextGuard Threat Alerts"
    private const val CHANNEL_DESC = "Real-time threat triage alerts for suspicious incoming notifications"
    private const val BASE_NOTIFICATION_ID = 20000

    private var notificationIdCounter = BASE_NOTIFICATION_ID

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null && notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Posts an advisory notification explaining the strongest available risk evidence.
     */
    fun postThreatAlert(
        context: Context,
        features: NotificationFeatures,
        safetyResult: SafetyResult
    ) {
        // ACT is silent
        if (safetyResult.intervention == InterventionType.ACT) return

        try {
            ensureChannel(context)

            val notificationId = synchronized(this) {
                notificationIdCounter++
                if (notificationIdCounter > BASE_NOTIFICATION_ID + 500) {
                    notificationIdCounter = BASE_NOTIFICATION_ID
                }
                notificationIdCounter
            }

            // Strongest available evidence explanation
            val evidenceSummary = safetyResult.rationale.ifBlank {
                safetyResult.evidence.firstOrNull() ?: "Potential mobile security hazard detected."
            }

            val title = when (safetyResult.intervention) {
                InterventionType.STOP -> "ContextGuard Alert: Severe Threat Detected"
                InterventionType.WARN -> "ContextGuard Caution: Suspicious Message"
                InterventionType.ASK -> "ContextGuard Advisory: Unverified Notification"
                InterventionType.ACT -> "ContextGuard Notification"
            }

            // PendingIntent to launch MainActivity for deep risk review
            val reviewIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.contextguard.app.ACTION_REVIEW_RISK"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_REVIEW_RISK", true)
                putExtra("EXTRA_TARGET_PACKAGE", features.sourcePackage)
                putExtra("EXTRA_RATIONALE", evidenceSummary)
                putExtra("EXTRA_RISK_SCORE", safetyResult.riskScore)
                putExtra("EXTRA_INTERVENTION", safetyResult.intervention.name)
                putExtra("EXTRA_CANDIDATE_ACTION", "NOTIFICATION")
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingReview = PendingIntent.getActivity(
                context,
                notificationId,
                reviewIntent,
                pendingIntentFlags
            )

            // Build NotificationCompat
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(evidenceSummary)
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "$evidenceSummary\n\nTarget App: ${features.sourcePackage}\nIntervention: ${safetyResult.intervention}"
                ))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingReview)
                .addAction(
                    android.R.drawable.ic_menu_view,
                    "Review in ContextGuard",
                    pendingReview
                )

            // Color based on intervention
            when (safetyResult.intervention) {
                InterventionType.STOP -> builder.setColor(0xFFFF1744.toInt()) // Red
                InterventionType.WARN -> builder.setColor(0xFFFF9100.toInt()) // Orange
                InterventionType.ASK -> builder.setColor(0xFFFFD600.toInt()) // Yellow
                else -> builder.setColor(0xFF00E5FF.toInt()) // Cyan
            }

            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(notificationId, builder.build())
                AppLogger.i("Threat alert notification posted for ${features.sourcePackage} [${safetyResult.intervention}]")
            } catch (se: SecurityException) {
                AppLogger.w("POST_NOTIFICATIONS permission not granted: ${se.message}")
            }

            // Record in privacy-safe audit ledger
            NotificationGuardStateManager.recordAudit(
                NotificationAuditRecord(
                    sourcePackage = features.sourcePackage,
                    intervention = safetyResult.intervention.name,
                    riskScore = safetyResult.riskScore,
                    evidenceSummary = evidenceSummary
                )
            )

        } catch (e: Exception) {
            AppLogger.e("Failed to post threat alert notification: ${e.message}")
        }
    }
}
