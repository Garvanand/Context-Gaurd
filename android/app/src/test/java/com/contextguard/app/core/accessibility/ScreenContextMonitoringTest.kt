package com.contextguard.app.core.accessibility

import com.contextguard.app.core.event.AccessibilityEventFilter
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Verification test suite for Screen-Context Monitoring Layer.
 * Tests allowlisting, event filtering, debounce/cooldown, password exclusion,
 * missing nodes, permission/lifecycle state, and kill-switch erasure.
 */
class ScreenContextMonitoringTest {

    @Before
    fun setUp() {
        AllowlistManager.resetToDefaults()
        ScreenGuardStateManager.resetForTesting()
        ScreenRiskTriggerEngine.clearState()
        AccessibilityEventFilter.clearTimestamps()
    }

    @After
    fun tearDown() {
        AllowlistManager.resetToDefaults()
        ScreenGuardStateManager.resetForTesting()
        ScreenRiskTriggerEngine.clearState()
    }

    // =========================================================================
    // 1. PACKAGE ALLOWLISTING & AVAILABILITY
    // =========================================================================

    @Test
    fun test_package_allowlisting_enabled_and_disabled() {
        // By default, Chrome and WhatsApp are enabled in default list
        assertTrue(AllowlistManager.isPackageAllowed("com.android.chrome"))
        assertTrue(AllowlistManager.isPackageAllowed("com.whatsapp"))

        // Samsung Internet is in list but disabled by default
        assertFalse(AllowlistManager.isPackageAllowed("com.sec.android.app.sbrowser"))

        // User enables Samsung Internet
        AllowlistManager.toggleApp("com.sec.android.app.sbrowser", true)
        assertTrue(AllowlistManager.isPackageAllowed("com.sec.android.app.sbrowser"))

        // Non-allowlisted package is strictly disallowed
        assertFalse(AllowlistManager.isPackageAllowed("com.malicious.spyware"))
        assertFalse(AllowlistManager.isPackageAllowed("com.unknown.game"))
    }

    @Test
    fun test_uninstalled_package_marked_unavailable_without_fabricating_connection() {
        val app = AllowlistManager.targetApps.value.find { it.packageName == "com.android.chrome" }
        assertNotNull(app)
        // In unit test environment without real package manager, isInstalled defaults to false
        assertFalse("Uninstalled app must be marked isInstalled = false", app!!.isInstalled)
    }

    // =========================================================================
    // 2. EVENT FILTERING, DEBOUNCE & COOLDOWN
    // =========================================================================

    @Test
    fun test_event_debounce_throttles_rapid_events() {
        val pkg = "com.android.chrome"
        val t0 = 1000L

        // First event passes debounce
        assertFalse(AccessibilityEventFilter.shouldDebounce(pkg, t0))

        // Immediate subsequent event (100ms later) MUST be debounced (< 300ms)
        assertTrue(AccessibilityEventFilter.shouldDebounce(pkg, t0 + 100L))
        assertTrue(AccessibilityEventFilter.shouldDebounce(pkg, t0 + 250L))

        // Event after 300ms window passes
        assertFalse(AccessibilityEventFilter.shouldDebounce(pkg, t0 + 350L))
    }

    @Test
    fun test_screen_risk_trigger_engine_cooldown_prevents_warning_fatigue() {
        ScreenGuardStateManager.onServiceConnected()

        val metadata = ScreenContextMetadata(
            packageName = "com.android.chrome",
            screenIdentifier = "MainActivity",
            activeUrl = "http://192.168.1.1/login",
            buttonLabels = listOf("Submit"),
            candidateAction = ScreenActionType.LOGIN
        )

        // First evaluation runs and returns STOP
        val result1 = ScreenRiskTriggerEngine.evaluate(metadata)
        assertNotNull("First evaluation must execute", result1)

        // Immediate subsequent evaluation with identical content returns null (cooldown active)
        val result2 = ScreenRiskTriggerEngine.evaluate(metadata)
        assertNull("Identical content within cooldown must be skipped", result2)
    }

    // =========================================================================
    // 3. FINGERPRINTING & DUPLICATE EVENT SUPPRESSION
    // =========================================================================

    @Test
    fun test_content_fingerprint_deterministic_and_unique() {
        val metaA = ScreenContextMetadata(
            packageName = "com.android.chrome",
            activeUrl = "https://reuters.com",
            candidateAction = ScreenActionType.OPEN
        )
        val metaB = ScreenContextMetadata(
            packageName = "com.android.chrome",
            activeUrl = "https://reuters.com",
            candidateAction = ScreenActionType.OPEN
        )
        val metaC = ScreenContextMetadata(
            packageName = "com.android.chrome",
            activeUrl = "http://phishing.xyz",
            candidateAction = ScreenActionType.LOGIN
        )

        val fpA = ScreenRiskTriggerEngine.computeFingerprint(metaA)
        val fpB = ScreenRiskTriggerEngine.computeFingerprint(metaB)
        val fpC = ScreenRiskTriggerEngine.computeFingerprint(metaC)

        assertEquals("Identical metadata must yield identical fingerprint", fpA, fpB)
        assertNotEquals("Different URL/action must yield different fingerprint", fpA, fpC)
    }

    @Test
    fun test_dismissal_policy_suppresses_dismissed_fingerprints() {
        ScreenGuardStateManager.onServiceConnected()

        val metadata = ScreenContextMetadata(
            packageName = "com.android.chrome",
            activeUrl = "http://192.168.1.100/verify",
            candidateAction = ScreenActionType.LOGIN
        )

        // User explicitly dismisses this warning
        ScreenRiskTriggerEngine.recordDismissal(metadata)

        // Subsequent evaluation for the dismissed screen must return null
        val result = ScreenRiskTriggerEngine.evaluate(metadata)
        assertNull("Dismissed screen fingerprint must not re-trigger warning", result)
    }

    // =========================================================================
    // 4. PASSWORD & SENSITIVE CONTROL EXCLUSION
    // =========================================================================

    @Test
    fun test_password_field_exclusion_and_sensitive_control_skipping() {
        // Standard password flag
        assertTrue(AccessibilityEventFilter.isPasswordNode(isPassword = true, inputType = 0))

        // Password input type variations
        assertTrue(AccessibilityEventFilter.isPasswordNode(isPassword = false, inputType = 0x81))
        assertTrue(AccessibilityEventFilter.isPasswordNode(isPassword = false, inputType = 0x91))
        assertTrue(AccessibilityEventFilter.isPasswordNode(isPassword = false, inputType = 0x12))

        // Normal text field is not excluded
        assertFalse(AccessibilityEventFilter.isPasswordNode(isPassword = false, inputType = 0x01))
    }

    // =========================================================================
    // 5. MISSING ACCESSIBILITY NODES / FLAG_SECURE
    // =========================================================================

    @Test
    fun test_missing_accessibility_root_returns_inspection_unavailable() {
        // When active window root is null (e.g. banking FLAG_SECURE window)
        val metadata = ScreenContextExtractor.extract(rootNode = null, event = null)

        assertTrue("Root null must set inspectionUnavailable = true", metadata.inspectionUnavailable)
        assertTrue(metadata.inspectionErrors.any { it.contains("FLAG_SECURE") || it.contains("null") })

        // Trigger engine must skip null root gracefully without asserting false safety
        val result = ScreenRiskTriggerEngine.evaluate(metadata)
        assertNull("Trigger engine must skip unavailable screen without emitting fake ACT", result)
    }

    // =========================================================================
    // 6. PERMISSION REVOCATION & SERVICE DISCONNECTION
    // =========================================================================

    @Test
    fun test_service_disconnection_halts_processing() {
        // Initially disconnected
        assertFalse(ScreenGuardStateManager.isServiceConnected.value)
        assertFalse(ScreenGuardStateManager.canProcessEvents())

        // Service connects
        ScreenGuardStateManager.onServiceConnected()
        assertTrue(ScreenGuardStateManager.isServiceConnected.value)
        assertTrue(ScreenGuardStateManager.canProcessEvents())

        // System revokes permission / unbinds service
        ScreenGuardStateManager.onServiceDisconnected()
        assertFalse(ScreenGuardStateManager.isServiceConnected.value)
        assertFalse(ScreenGuardStateManager.canProcessEvents())
    }

    // =========================================================================
    // 7. MONITORING STOP / PAUSE KILL-SWITCH & AUDIT ERASURE
    // =========================================================================

    @Test
    fun test_immediate_pause_kill_switch() {
        ScreenGuardStateManager.onServiceConnected()
        assertTrue(ScreenGuardStateManager.canProcessEvents())

        // User taps Kill-Switch Pause button
        ScreenGuardStateManager.pauseMonitoring()
        assertTrue(ScreenGuardStateManager.isMonitoringPaused.value)
        assertFalse("canProcessEvents must be false when paused", ScreenGuardStateManager.canProcessEvents())

        // User resumes monitoring
        ScreenGuardStateManager.resumeMonitoring()
        assertFalse(ScreenGuardStateManager.isMonitoringPaused.value)
        assertTrue(ScreenGuardStateManager.canProcessEvents())
    }

    @Test
    fun test_ephemeral_audit_ledger_and_immediate_erasure() {
        val entry = AuditLogEntry(
            packageName = "com.android.chrome",
            screenIdentifier = "BrowserActivity",
            candidateAction = "OPEN",
            intervention = "ACT",
            riskScore = 0.05f,
            latencyMs = 4L,
            evidenceSummary = "URL passed fast on-device checks"
        )

        ScreenGuardStateManager.recordAuditEntry(entry)
        assertEquals(1, ScreenGuardStateManager.auditLogs.value.size)

        // User taps "Erase All Logs"
        ScreenGuardStateManager.clearAuditLog()
        assertEquals(0, ScreenGuardStateManager.auditLogs.value.size)
    }
}
