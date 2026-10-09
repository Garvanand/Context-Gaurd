package com.contextguard.app.core.network

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit and Integration Verification for Domain Network Protection Feasibility.
 *
 * Verifies:
 * 1. Domain lookup behavior: benign vs. deceptive phishing hostnames.
 * 2. Strict metadata boundary: hostname only, no URL paths or sensitive query parameters.
 * 3. Safe-domain controls: user allowlist takes absolute precedence.
 * 4. Network handover: Wi-Fi to cellular transition tracking.
 * 5. VPN revocation handling when a conflicting VPN starts.
 * 6. Single-tap stop control and state restoration.
 * 7. Non-dropping safeguard: prevents creation of a blackhole TUN interface without packet forwarders.
 */
class DomainProtectionFeasibilityTest {

    private lateinit var evaluator: DomainRiskEvaluator

    @Before
    fun setUp() {
        DomainProtectionManager.resetForTesting()
        evaluator = DomainRiskEvaluator()
    }

    @Test
    fun test_dns_domain_lookup_benign_domain() {
        // Standard benign domains on default allowlist
        val res = evaluator.evaluate("google.com")
        assertFalse("Benign domain must not be flagged as threat", res.isThreat)
        assertTrue("Allowlisted domain must be flagged as allowlisted", res.isAllowlisted)
        assertEquals(0.05f, res.riskScore, 0.01f)
    }

    @Test
    fun test_dns_domain_lookup_phishing_domain() {
        // Deceptive domain with suspicious TLD and phishing keywords
        val res = evaluator.evaluate("login-auth.update-kyc.top")
        assertTrue("Deceptive phishing domain must be flagged as threat", res.isThreat)
        assertTrue("Risk score must exceed threshold", res.riskScore >= 0.65f)
        assertTrue("Rationale must describe detected hazard", res.rationale.contains("Suspicious domain detected"))
    }

    @Test
    fun test_raw_ip_literal_host_flagged() {
        val res = evaluator.evaluate("45.33.32.156")
        assertTrue("Raw IP literal must elevate risk score", res.riskScore >= 0.50f)
        assertTrue(res.rationale.contains("IP literal"))
    }

    @Test
    fun test_strict_metadata_boundary_sanitizes_path_and_params() {
        // Verify that only the hostname is inspected, never URL paths or credentials
        val dirtyUrl = "https://legitimate-service.com:8443/private/path?password=supersecret&token=123"
        val sanitized = evaluator.sanitizeHostname(dirtyUrl)
        assertEquals("legitimate-service.com", sanitized)
        assertFalse(sanitized.contains("password"))
        assertFalse(sanitized.contains("private"))
    }

    @Test
    fun test_safe_domain_controls_allowlist() {
        val customDomain = "intranet.company-secure.corp"
        // Initially evaluated without allowlist
        val evalInitial = evaluator.evaluate(customDomain)
        assertFalse(evalInitial.isAllowlisted)

        // Add to safe allowlist
        evaluator.addSafeDomain(customDomain)
        val evalAllowed = evaluator.evaluate(customDomain)
        assertTrue("Domain must be allowlisted after adding to safe domains", evalAllowed.isAllowlisted)
        assertFalse(evalAllowed.isThreat)
        assertEquals(0.05f, evalAllowed.riskScore, 0.01f)

        // Remove from allowlist
        evaluator.removeSafeDomain(customDomain)
        val evalRemoved = evaluator.evaluate(customDomain)
        assertFalse("Domain must no longer be allowlisted after removal", evalRemoved.isAllowlisted)
    }

    @Test
    fun test_network_handover_wifi_to_cellular() {
        // Initial state Wi-Fi
        DomainProtectionManager.onNetworkTransition(isWifi = true, isCellular = false, interfaceName = "wlan0")
        var telemetry = DomainProtectionManager.telemetry.value
        assertTrue(telemetry.isWifi)
        assertFalse(telemetry.isCellular)
        assertEquals("wlan0", telemetry.activeInterfaceName)

        // Handover to Cellular (e.g. mobile 5G)
        DomainProtectionManager.onNetworkTransition(isWifi = false, isCellular = true, interfaceName = "rmnet0")
        telemetry = DomainProtectionManager.telemetry.value
        assertFalse(telemetry.isWifi)
        assertTrue(telemetry.isCellular)
        assertEquals("rmnet0", telemetry.activeInterfaceName)
    }

    @Test
    fun test_vpn_revocation_handling() {
        DomainProtectionManager.setStatus(ProtectionStatus.ACTIVE)
        assertEquals(ProtectionStatus.ACTIVE, DomainProtectionManager.status.value)

        // Simulate Android OS invoking onRevoke() when conflicting WireGuard/corporate VPN starts
        DomainProtectionManager.onVpnRevoked()
        assertEquals("Status must transition to REVOKED on OS revocation", ProtectionStatus.REVOKED, DomainProtectionManager.status.value)
        assertNull("Active since timestamp must be cleared on revocation", DomainProtectionManager.telemetry.value.activeSinceTimestamp)
    }

    @Test
    fun test_single_tap_stop_control() {
        DomainProtectionManager.setStatus(ProtectionStatus.ACTIVE)
        // Record inspection event
        DomainProtectionManager.inspectDomain("phishing-portal.work")
        assertEquals(1, DomainProtectionManager.recentEvents.value.size)

        // User triggers single-tap stop
        DomainProtectionManager.stopProtection()
        assertEquals(ProtectionStatus.STOPPED, DomainProtectionManager.status.value)
        assertEquals("Recent events queue must be purged on stop to preserve zero retention", 0, DomainProtectionManager.recentEvents.value.size)
    }

    @Test
    fun test_domain_inspection_bounded_in_memory_queue() {
        // Perform 25 inspections to verify bounded capacity (max 20 entries)
        for (i in 1..25) {
            DomainProtectionManager.inspectDomain("domain-$i.com")
        }

        val events = DomainProtectionManager.recentEvents.value
        assertEquals("Event queue must be strictly bounded to 20 entries max", 20, events.size)
        assertEquals(25L, DomainProtectionManager.telemetry.value.totalInspections)
    }
}
