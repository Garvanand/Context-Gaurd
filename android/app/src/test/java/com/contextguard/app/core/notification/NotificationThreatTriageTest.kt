package com.contextguard.app.core.notification

import com.contextguard.app.core.accessibility.AllowlistManager
import com.contextguard.app.ui.viewmodel.InterventionType
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Verification test suite for Notification-Based Threat Triage.
 *
 * PROVES:
 * 1. Notification arrival & phishing bank impersonation (STOP alert).
 * 2. OTP / credential theft detection (STOP alert).
 * 3. Urgent language alone does NOT trigger false fraud alerts (ACT / benign control).
 * 4. Ambiguous evidence uses cautious wording (ASK/WARN rather than premature STOP).
 * 5. Incomplete or truncated notifications handled conservatively with reduced confidence.
 * 6. Duplicate notification states skipped via fingerprinting (battery optimization).
 * 7. Notification removal cleanly evicts state.
 * 8. Unsupported / non-allowlisted packages ignored.
 * 9. Paused protection & disabled state handling.
 * 10. Privacy invariant: Zero raw notification payload stored in audit records.
 * 11. Suspicious URL candidates detected (IP hosts, high-risk TLDs).
 */
class NotificationThreatTriageTest {

    @Before
    fun setUp() {
        AllowlistManager.resetToDefaults()
        NotificationGuardStateManager.resetForTesting()
        NotificationGuardStateManager.onListenerConnected()

        // Enable test packages in AllowlistManager
        AllowlistManager.setAppInstalledAndEnabledForTesting("com.whatsapp", installed = true, enabled = true)
        AllowlistManager.setAppInstalledAndEnabledForTesting("com.google.android.apps.messaging", installed = true, enabled = true)
        AllowlistManager.setAppInstalledAndEnabledForTesting("org.telegram.messenger", installed = true, enabled = true)
        AllowlistManager.setAppInstalledAndEnabledForTesting("com.google.android.gm", installed = true, enabled = true)
    }

    @After
    fun tearDown() {
        AllowlistManager.resetToDefaults()
        NotificationGuardStateManager.resetForTesting()
    }

    // =========================================================================
    // 1. PHISHING BANK IMPERSONATION WITH DOMAIN MISMATCH
    // =========================================================================

    @Test
    fun test_notification_arrival_phishing_bank_impersonation() {
        val features = NotificationThreatAnalyzer.extractFeatures(
            packageName = "com.google.android.apps.messaging",
            category = "msg",
            title = "HDFC Bank Alert",
            text = "Dear customer, your netbanking access is suspended. Login immediately to verify your account: https://hdfc-kyc-update.xyz/login.php"
        )

        assertEquals("HDFC Bank", features.impersonationEntity)
        assertEquals(1, features.extractedUrls.size)
        assertTrue("Domain mismatch must be identified", features.domainMismatches.isNotEmpty())
        assertTrue("Mismatched domain text noted in features",
            features.domainMismatches.first().contains("hdfc-kyc-update.xyz"))

        val result = NotificationThreatAnalyzer.analyze(features)
        assertEquals("Bank impersonation with fraudulent domain must trigger STOP",
            InterventionType.STOP, result.intervention)
        assertTrue("Risk score must be elevated (>= 0.65)", result.riskScore >= 0.65f)
        assertTrue("Evidence must clearly explain the domain mismatch",
            result.evidence.any { it.contains("does not match the claimed organization", ignoreCase = true) })
    }

    // =========================================================================
    // 2. OTP / CREDENTIAL THEFT DETECTION
    // =========================================================================

    @Test
    fun test_notification_arrival_otp_theft() {
        val features = NotificationThreatAnalyzer.extractFeatures(
            packageName = "com.whatsapp",
            category = "msg",
            title = "Account Security",
            text = "Your one-time password OTP is 849201. Please share OTP with our support agent to cancel unauthorized transaction."
        )

        assertTrue("Must detect request for credentials/OTP", features.requestsCredentialsOrOtp)

        val result = NotificationThreatAnalyzer.analyze(features)
        assertEquals("Attempted OTP theft must trigger STOP",
            InterventionType.STOP, result.intervention)
        assertTrue("Evidence must cite confidential OTP solicitation",
            result.evidence.any { it.contains("OTP", ignoreCase = true) })
    }

    // =========================================================================
    // 3. CRITICAL RULE: URGENT LANGUAGE ALONE IS NOT FRAUD (BENIGN CONTROL)
    // =========================================================================

    @Test
    fun test_benign_control_message_routine_urgency_is_silent_act() {
        // Message contains urgent words but NO domain mismatch, NO credential solicitation, NO phishing URL
        val features = NotificationThreatAnalyzer.extractFeatures(
            packageName = "com.whatsapp",
            category = "msg",
            title = "Sprint Planning",
            text = "Urgent reminder: Team sprint planning meeting starts immediately in 10 minutes. Please join."
        )

        assertTrue("Urgency keywords detected", features.urgencyScore > 0f)
        assertFalse("No credentials requested", features.requestsCredentialsOrOtp)
        assertFalse("No payment requested", features.requestsImmediatePayment)
        assertTrue("No domain mismatches", features.domainMismatches.isEmpty())
        assertTrue("No suspicious URLs", features.suspiciousUrls.isEmpty())

        val result = NotificationThreatAnalyzer.analyze(features)
        assertEquals("Urgent language alone must NOT trigger fraud alerts; must evaluate to silent ACT",
            InterventionType.ACT, result.intervention)
        assertTrue("Risk score must remain below threshold (< 0.35)", result.riskScore < 0.35f)
    }

    // =========================================================================
    // 4. AMBIGUOUS EVIDENCE USES CAUTIOUS WORDING (ASK/WARN RATHER THAN STOP)
    // =========================================================================

    @Test
    fun test_ambiguous_urgency_with_unverified_url_emits_cautious_ask() {
        // Urgency with unverified link, but no confirmed bank impersonation or explicit theft
        val features = NotificationThreatAnalyzer.extractFeatures(
            packageName = "com.google.android.apps.messaging",
            category = "msg",
            title = "Important Notice",
            text = "Immediate action required for your pending shipment. Verify details at https://package-tracker-portal.org/details"
        )

        assertTrue("Urgency detected", features.urgencyScore > 0f)
        assertEquals(1, features.extractedUrls.size)
        // Standard .org domain without explicit brand claim is ambiguous
        assertTrue("No explicit bank domain mismatch", features.domainMismatches.isEmpty())

        val result = NotificationThreatAnalyzer.analyze(features)
        assertTrue("Ambiguous evidence must emit cautious ASK or WARN, NOT a definitive STOP",
            result.intervention == InterventionType.ASK || result.intervention == InterventionType.WARN)
        assertNotEquals("Must not emit STOP for ambiguous unverified link", InterventionType.STOP, result.intervention)
        assertTrue("Evidence should advise caution rather than declaring fraud is proven",
            result.evidence.any { it.contains("Caution", ignoreCase = true) || it.contains("unverified", ignoreCase = true) })
    }

    // =========================================================================
    // 5. INCOMPLETE OR TRUNCATED NOTIFICATION HANDLING
    // =========================================================================

    @Test
    fun test_incomplete_or_truncated_notification_handled_safely() {
        val features = NotificationThreatAnalyzer.extractFeatures(
            packageName = "com.google.android.gm",
            category = "email",
            title = "Security Alert",
            text = "Your account access has expired, click to renew..."
        )

        assertTrue("Must detect truncated ellipsis", features.isTruncated)

        val result = NotificationThreatAnalyzer.analyze(features)
        assertTrue("Confidence must be discounted for truncated text", result.confidence < 0.85f)
        assertTrue("Evidence notes truncation",
            result.evidence.any { it.contains("truncated", ignoreCase = true) })
    }

    // =========================================================================
    // 6. DEDUPLICATION & BATTERY OPTIMIZATION
    // =========================================================================

    @Test
    fun test_duplicate_notification_skipped_by_fingerprint() {
        val pkg = "com.whatsapp"
        val id = 101
        val postTime = 1728518400000L
        val length = 65

        val fingerprint = NotificationGuardStateManager.computeNotificationFingerprint(pkg, id, postTime, length)

        assertFalse("Initial notification state is not duplicate",
            NotificationGuardStateManager.isDuplicate(fingerprint))

        // Record fingerprint
        NotificationGuardStateManager.recordFingerprint("key_101", fingerprint)

        assertTrue("Identical notification state must be detected as duplicate to save battery",
            NotificationGuardStateManager.isDuplicate(fingerprint))

        // If postTime changes (new distinct notification), fingerprint changes
        val newFingerprint = NotificationGuardStateManager.computeNotificationFingerprint(pkg, id, postTime + 5000, length)
        assertFalse("New notification with different postTime must not be blocked",
            NotificationGuardStateManager.isDuplicate(newFingerprint))
    }

    // =========================================================================
    // 7. NOTIFICATION REMOVAL EVICTS STATE
    // =========================================================================

    @Test
    fun test_notification_removal_cleans_fingerprint() {
        val pkg = "com.whatsapp"
        val id = 102
        val postTime = 1728518400000L
        val length = 50

        val fingerprint = NotificationGuardStateManager.computeNotificationFingerprint(pkg, id, postTime, length)
        val key = "key_102"

        NotificationGuardStateManager.recordFingerprint(key, fingerprint)
        assertTrue(NotificationGuardStateManager.isDuplicate(fingerprint))

        // System notification is dismissed/removed by user
        NotificationGuardStateManager.onNotificationRemoved(key)

        assertFalse("Evicted notification fingerprint is removed",
            NotificationGuardStateManager.isDuplicate(fingerprint))
    }

    // =========================================================================
    // 8. UNSUPPORTED / NON-ALLOWLISTED PACKAGES IGNORED
    // =========================================================================

    @Test
    fun test_unsupported_or_disallowed_package_ignored() {
        assertFalse("Non-allowlisted gaming package must be disallowed",
            AllowlistManager.isPackageAllowed("com.casual.untrusted.game"))
        assertFalse("Non-allowlisted ad network must be disallowed",
            AllowlistManager.isPackageAllowed("com.unknown.spam.app"))

        assertTrue("Allowlisted WhatsApp must be permitted",
            AllowlistManager.isPackageAllowed("com.whatsapp"))
    }

    // =========================================================================
    // 9. PAUSED PROTECTION & DISABLED STATE
    // =========================================================================

    @Test
    fun test_paused_protection_and_disabled_state() {
        assertTrue("Initially can process", NotificationGuardStateManager.canProcessNotifications())

        // User pauses notification triage
        NotificationGuardStateManager.pauseProtection()
        assertFalse("Paused protection must halt processing",
            NotificationGuardStateManager.canProcessNotifications())

        // User resumes
        NotificationGuardStateManager.resumeProtection()
        assertTrue("Resumed protection can process",
            NotificationGuardStateManager.canProcessNotifications())

        // Service unbind / disconnect
        NotificationGuardStateManager.onListenerDisconnected()
        assertFalse("Disconnected service cannot process",
            NotificationGuardStateManager.canProcessNotifications())
    }

    // =========================================================================
    // 10. PRIVACY INVARIANT: ZERO RAW PAYLOAD STORED
    // =========================================================================

    @Test
    fun test_zero_raw_payload_stored_in_audit_records() {
        val auditRecord = NotificationAuditRecord(
            sourcePackage = "com.whatsapp",
            intervention = "STOP",
            riskScore = 0.88f,
            evidenceSummary = "Potential impersonation: domain mismatch detected."
        )

        NotificationGuardStateManager.recordAudit(auditRecord)

        val records = NotificationGuardStateManager.recentAuditRecords.value
        assertEquals(1, records.size)

        // Verify reflection on NotificationAuditRecord has NO raw payload fields
        val fields = NotificationAuditRecord::class.java.declaredFields.map { it.name }
        assertFalse("Audit record must never contain raw text", fields.contains("rawText"))
        assertFalse("Audit record must never contain messageText", fields.contains("messageText"))
        assertFalse("Audit record must never contain body", fields.contains("body"))
        assertFalse("Audit record must never contain payload", fields.contains("payload"))
    }

    // =========================================================================
    // 11. SUSPICIOUS URL CANDIDATES DETECTED
    // =========================================================================

    @Test
    fun test_suspicious_url_candidates_detected() {
        // Raw IP host URL
        val ipUrl = "http://192.168.1.50/banking/login"
        val ipAnalysis = NotificationThreatAnalyzer.analyzeUrl(ipUrl)
        assertTrue("Raw IP address must be flagged suspicious", ipAnalysis.isSuspicious)
        assertEquals(0.95f, ipAnalysis.severity)

        // Phishing keyword + high-risk TLD
        val tldUrl = "https://secure-login-portal.xyz/verify"
        val tldAnalysis = NotificationThreatAnalyzer.analyzeUrl(tldUrl)
        assertTrue("Suspicious TLD with login keyword must be flagged", tldAnalysis.isSuspicious)
        assertEquals(0.90f, tldAnalysis.severity)

        // Legitimate standard URL
        val legitUrl = "https://en.wikipedia.org/wiki/Computer_security"
        val legitAnalysis = NotificationThreatAnalyzer.analyzeUrl(legitUrl)
        assertFalse("Legitimate domain must not be flagged suspicious", legitAnalysis.isSuspicious)
    }
}
