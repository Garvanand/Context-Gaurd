package com.contextguard.app.core.event

import com.contextguard.app.core.engine.OnDeviceDecisionEngine
import com.contextguard.app.ui.viewmodel.InterventionType
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Executable Acceptance Test Suite for ContextGuard Event-Driven Architecture.
 * Verifies Ingestion Paths A, B, Fast On-Device Engine, and Kill-Switch invariants.
 */
class EventDrivenProtectionAcceptanceTest {

    @Before
    fun setUp() {
        ProtectionStateManager.resetToDefaults()
        AccessibilityEventFilter.clearTimestamps()
    }

    @After
    fun tearDown() {
        ProtectionStateManager.resetToDefaults()
    }

    // =========================================================================
    // PATH A: ACCESSIBILITY SCREEN CONTEXT ACCEPTANCE TESTS
    // =========================================================================

    @Test
    fun test_path_a_allowlist_filtering() {
        // Allowlisted packages must be accepted
        assertTrue(AccessibilityEventFilter.shouldProcessPackage("com.android.chrome"))
        assertTrue(AccessibilityEventFilter.shouldProcessPackage("com.whatsapp"))
        assertTrue(AccessibilityEventFilter.shouldProcessPackage("org.mozilla.firefox"))
        assertTrue(AccessibilityEventFilter.shouldProcessPackage("com.google.android.gm"))

        // Non-allowlisted apps must be rejected immediately
        assertFalse(AccessibilityEventFilter.shouldProcessPackage("com.random.untrusted.app"))
        assertFalse(AccessibilityEventFilter.shouldProcessPackage("com.game.flappy"))
    }

    @Test
    fun test_path_a_password_exclusion_invariant() {
        // Nodes marked isPassword == true MUST be flagged as password nodes
        assertTrue(AccessibilityEventFilter.isPasswordNode(isPassword = true, inputType = 0))

        // Nodes with password inputType variations MUST be flagged
        assertTrue(AccessibilityEventFilter.isPasswordNode(isPassword = false, inputType = 0x81))
        assertTrue(AccessibilityEventFilter.isPasswordNode(isPassword = false, inputType = 0x91))

        // Standard text node must not be flagged
        assertFalse(AccessibilityEventFilter.isPasswordNode(isPassword = false, inputType = 0x01))
    }

    @Test
    fun test_path_a_browser_url_extraction() {
        // Chrome address bar extraction
        val chromeUrl = AccessibilityEventFilter.extractBrowserUrl(
            viewId = "com.android.chrome:id/url_bar",
            rawText = "example.com/login"
        )
        assertEquals("https://example.com/login", chromeUrl)

        // Raw text with full scheme
        val directUrl = AccessibilityEventFilter.extractBrowserUrl(
            viewId = "org.mozilla.firefox:id/url_bar_title",
            rawText = "https://legitimate-bank.com/portal"
        )
        assertEquals("https://legitimate-bank.com/portal", directUrl)

        // Blank or non-URL text returns null
        assertNull(AccessibilityEventFilter.extractBrowserUrl("id/title", "Welcome to My Page"))
        assertNull(AccessibilityEventFilter.extractBrowserUrl(null, null))
    }

    @Test
    fun test_path_a_button_to_action_mapping() {
        assertEquals("SEND", AccessibilityEventFilter.mapButtonToAction("Send Message"))
        assertEquals("SEND", AccessibilityEventFilter.mapButtonToAction("Submit Form"))
        assertEquals("POST", AccessibilityEventFilter.mapButtonToAction("Tweet"))
        assertEquals("POST", AccessibilityEventFilter.mapButtonToAction("Post to Feed"))
        assertEquals("APPROVE", AccessibilityEventFilter.mapButtonToAction("Pay Now"))
        assertEquals("APPROVE", AccessibilityEventFilter.mapButtonToAction("Transfer Funds"))
        assertEquals("LOGIN", AccessibilityEventFilter.mapButtonToAction("Sign In"))
        assertEquals("SIGN", AccessibilityEventFilter.mapButtonToAction("Sign Document"))
        assertEquals("OPEN", AccessibilityEventFilter.mapButtonToAction("Details"))
    }

    // =========================================================================
    // PATH B: NOTIFICATION PROTECTION ACCEPTANCE TESTS
    // =========================================================================

    @Test
    fun test_path_b_notification_filtering() {
        // Standard notification from allowlisted messaging app accepted
        assertTrue(NotificationFilter.shouldProcessNotification("com.whatsapp", isOngoing = false))
        assertTrue(NotificationFilter.shouldProcessNotification("com.google.android.apps.messaging", isOngoing = false))

        // Ongoing notification (media player, progress) rejected
        assertFalse(NotificationFilter.shouldProcessNotification("com.whatsapp", isOngoing = true))

        // Non-allowlisted app notification rejected
        assertFalse(NotificationFilter.shouldProcessNotification("com.casual.game", isOngoing = false))
    }

    @Test
    fun test_path_b_scam_and_otp_detection() {
        val scamMsg = "URGENT: Your electricity connection will be suspended. Pay immediately at http://192.168.1.1/pay"
        assertTrue(NotificationFilter.hasScamLure(scamMsg))
        val urls = NotificationFilter.extractUrls(scamMsg)
        assertEquals(1, urls.size)
        assertEquals("http://192.168.1.1/pay", urls[0])

        val otpMsg = "Your Axis Bank OTP is 849201. Do not share with anyone."
        assertTrue(NotificationFilter.hasOtpContext(otpMsg))

        val benignMsg = "Hey, are you free for lunch tomorrow?"
        assertFalse(NotificationFilter.hasScamLure(benignMsg))
        assertFalse(NotificationFilter.hasOtpContext(benignMsg))
    }

    // =========================================================================
    // ON-DEVICE REAL-TIME DECISION ENGINE ACCEPTANCE TESTS
    // =========================================================================

    @Test
    fun test_on_device_engine_phishing_url_blocks_instantly() {
        val event = UiScreenEvent(
            packageName = "com.android.chrome",
            screenType = ScreenContextType.BROWSER,
            activeUrl = "http://192.168.1.50/secure-login",
            focusedAction = "OPEN"
        )

        val result = OnDeviceDecisionEngine.evaluateScreenEvent(event)

        assertEquals(InterventionType.STOP, result.intervention)
        assertTrue("Risk score should exceed STOP threshold", result.riskScore >= 0.65f)
        assertTrue("Latency should be real-time (< 50ms)", result.latencyMs < 50L)
        assertTrue(result.evidence.any { it.contains("raw IP address") })
    }

    @Test
    fun test_on_device_engine_public_pii_post_blocks_action() {
        // User drafting sensitive Indian PAN card and phone number into Twitter/X post
        val event = UiScreenEvent(
            packageName = "com.twitter.android",
            screenType = ScreenContextType.SOCIAL,
            inputDraft = "Here is my document details: ABCDE1234F, contact 9876543210",
            focusedAction = "POST"
        )

        val result = OnDeviceDecisionEngine.evaluateScreenEvent(event)

        assertEquals(InterventionType.STOP, result.intervention)
        assertEquals(1.00f, result.irreversibility, 0.01f) // Public broadcast
        assertTrue("Severity must be high for public PII", result.severity >= 0.80f)
        assertTrue(result.evidence.any { it.contains("public broadcast") })
    }

    @Test
    fun test_on_device_engine_routine_action_allows() {
        val event = UiScreenEvent(
            packageName = "com.android.chrome",
            screenType = ScreenContextType.BROWSER,
            activeUrl = "https://www.reuters.com/world",
            focusedAction = "OPEN"
        )

        val result = OnDeviceDecisionEngine.evaluateScreenEvent(event)

        assertEquals(InterventionType.ACT, result.intervention)
        assertTrue("Risk score should be low", result.riskScore < 0.35f)
    }

    @Test
    fun test_on_device_engine_notification_scam_escalates_to_stop() {
        val notif = NotificationEvent(
            packageName = "com.google.android.apps.messaging",
            title = "Urgent Bank Notice",
            text = "Your account is locked. Restore immediately at http://10.0.0.1/verify",
            extractedUrls = listOf("http://10.0.0.1/verify")
        )

        val result = OnDeviceDecisionEngine.evaluateNotificationEvent(notif)

        assertEquals(InterventionType.STOP, result.intervention)
        assertTrue("Should detect phishing domain in notification", result.evidence.any { it.contains("phishing") })
    }

    // =========================================================================
    // KILL-SWITCH & LIFECYCLE ACCEPTANCE TESTS
    // =========================================================================

    @Test
    fun test_kill_switch_immediate_pause() {
        assertTrue(ProtectionStateManager.isProtectionActive())
        assertTrue(AccessibilityEventFilter.shouldProcessPackage("com.android.chrome"))

        // User engages kill-switch
        ProtectionStateManager.pauseProtection()

        assertFalse(ProtectionStateManager.isProtectionActive())
        // When protection is paused, ALL packages MUST be rejected immediately
        assertFalse(AccessibilityEventFilter.shouldProcessPackage("com.android.chrome"))
        assertFalse(NotificationFilter.shouldProcessNotification("com.whatsapp", isOngoing = false))

        // User resumes protection
        ProtectionStateManager.resumeProtection()
        assertTrue(ProtectionStateManager.isProtectionActive())
        assertTrue(AccessibilityEventFilter.shouldProcessPackage("com.android.chrome"))
    }
}
