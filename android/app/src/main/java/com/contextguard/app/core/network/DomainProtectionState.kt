package com.contextguard.app.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Status lifecycle for Domain Network Protection extension.
 */
enum class ProtectionStatus {
    DISABLED,          // Default off state
    PREPARING,         // Awaiting user consent or OS VPN authorization
    ACTIVE,            // Actively inspecting domains locally
    STOPPED,           // Stopped via single-tap user control
    REVOKED,           // Revoked by OS (conflicting third-party VPN started)
    FALLBACK_INACTIVE  // Safe non-dropping fallback active
}

data class DomainInspectionEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val hostname: String,
    val riskScore: Float,
    val isThreat: Boolean,
    val action: String // "ALLOWED", "WARNED", "SAFE_ALLOWLIST"
)

data class ProtectionTelemetry(
    val totalInspections: Long = 0,
    val threatsDetected: Long = 0,
    val activeSinceTimestamp: Long? = null,
    val isWifi: Boolean = true,
    val isCellular: Boolean = false,
    val activeInterfaceName: String = "wlan0"
)

/**
 * Singleton State & Lifecycle Manager for Network Domain Protection.
 *
 * Enforces:
 * 1. Single-tap immediate stop control.
 * 2. Transient in-memory telemetry with zero disk persistence of visited domains.
 * 3. Graceful revocation handling when conflicting VPNs start.
 * 4. Seamless Wi-Fi to cellular handover awareness.
 */
object DomainProtectionManager {

    private val _status = MutableStateFlow(ProtectionStatus.DISABLED)
    val status: StateFlow<ProtectionStatus> = _status.asStateFlow()

    private val _telemetry = MutableStateFlow(ProtectionTelemetry())
    val telemetry: StateFlow<ProtectionTelemetry> = _telemetry.asStateFlow()

    // Bounded in-memory transient log (max 20 entries, strictly transient, zero DB persistence)
    private val eventQueue = ConcurrentLinkedDeque<DomainInspectionEvent>()
    private val _recentEvents = MutableStateFlow<List<DomainInspectionEvent>>(emptyList())
    val recentEvents: StateFlow<List<DomainInspectionEvent>> = _recentEvents.asStateFlow()

    val evaluator = DomainRiskEvaluator()

    /**
     * Updates protection status.
     */
    fun setStatus(newStatus: ProtectionStatus) {
        _status.value = newStatus
        if (newStatus == ProtectionStatus.ACTIVE) {
            _telemetry.value = _telemetry.value.copy(
                activeSinceTimestamp = System.currentTimeMillis()
            )
        } else if (newStatus in listOf(ProtectionStatus.DISABLED, ProtectionStatus.STOPPED, ProtectionStatus.REVOKED)) {
            _telemetry.value = _telemetry.value.copy(
                activeSinceTimestamp = null
            )
        }
    }

    /**
     * Single-tap immediate stop control.
     * Tears down active monitoring and resets state cleanly.
     */
    fun stopProtection() {
        setStatus(ProtectionStatus.STOPPED)
        eventQueue.clear()
        _recentEvents.value = emptyList()
    }

    /**
     * Handles revocation when another VPN starts.
     */
    fun onVpnRevoked() {
        setStatus(ProtectionStatus.REVOKED)
    }

    /**
     * Updates network interface capabilities (Wi-Fi <-> Cellular handover).
     */
    fun onNetworkTransition(isWifi: Boolean, isCellular: Boolean, interfaceName: String = "") {
        _telemetry.value = _telemetry.value.copy(
            isWifi = isWifi,
            isCellular = isCellular,
            activeInterfaceName = if (interfaceName.isNotEmpty()) interfaceName else _telemetry.value.activeInterfaceName
        )
    }

    /**
     * Evaluates a destination domain and updates in-memory diagnostic telemetry.
     */
    fun inspectDomain(hostname: String): DomainRiskEvaluator.DomainEvaluation {
        val eval = evaluator.evaluate(hostname)

        val action = when {
            eval.isAllowlisted -> "SAFE_ALLOWLIST"
            eval.isThreat -> "WARNED"
            else -> "ALLOWED"
        }

        val event = DomainInspectionEvent(
            hostname = eval.hostname,
            riskScore = eval.riskScore,
            isThreat = eval.isThreat,
            action = action
        )

        // Maintain bounded queue in memory
        eventQueue.addFirst(event)
        while (eventQueue.size > 20) {
            eventQueue.removeLast()
        }
        _recentEvents.value = eventQueue.toList()

        // Update counts
        _telemetry.value = _telemetry.value.copy(
            totalInspections = _telemetry.value.totalInspections + 1,
            threatsDetected = if (eval.isThreat) _telemetry.value.threatsDetected + 1 else _telemetry.value.threatsDetected
        )

        return eval
    }

    /**
     * Adds domain to safe allowlist.
     */
    fun addSafeDomain(domain: String) {
        evaluator.addSafeDomain(domain)
    }

    /**
     * Removes domain from safe allowlist.
     */
    fun removeSafeDomain(domain: String) {
        evaluator.removeSafeDomain(domain)
    }

    /**
     * Resets telemetry for testing.
     */
    fun resetForTesting() {
        _status.value = ProtectionStatus.DISABLED
        _telemetry.value = ProtectionTelemetry()
        eventQueue.clear()
        _recentEvents.value = emptyList()
    }
}
