package com.contextguard.app.core.privacy

import java.util.Collections
import java.util.UUID

/**
 * Privacy-Preserving Network Audit Logger.
 *
 * CRITICAL INVARIANT:
 * Records metadata ONLY (timestamp, request ID, endpoint, payload size, latency, response code).
 * NEVER stores raw artifacts, OCR text, model prompts, or sensitive tokens.
 */
object NetworkAuditLogger {

    private val auditLogList = Collections.synchronizedList(mutableListOf<NetworkAuditEntry>())

    /**
     * Records a network audit event. Enforces privacy invariants.
     */
    fun record(
        endpointCategory: String,
        payloadType: String,
        payloadSizeBytes: Long,
        isRedacted: Boolean,
        responseStatus: Int,
        latencyMs: Long,
        artifactHashSha256: String,
        requestId: String = UUID.randomUUID().toString()
    ): NetworkAuditEntry {
        // Enforce that only cryptographic hash is stored (32 bytes = 64 hex chars or placeholder)
        val sanitizedHash = if (artifactHashSha256.length == 64 && artifactHashSha256.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            artifactHashSha256
        } else {
            CryptoUtils.sha256(artifactHashSha256)
        }

        val entry = NetworkAuditEntry(
            requestId = requestId,
            timestamp = System.currentTimeMillis(),
            endpointCategory = endpointCategory.take(64),
            payloadType = payloadType.take(64),
            payloadSizeBytes = payloadSizeBytes,
            isRedacted = isRedacted,
            responseStatus = responseStatus,
            latencyMs = latencyMs,
            artifactHashSha256 = sanitizedHash
        )

        auditLogList.add(0, entry) // prepend newest
        // Keep bounded size to avoid unbounded memory growth
        if (auditLogList.size > 200) {
            auditLogList.removeAt(auditLogList.size - 1)
        }
        return entry
    }

    /**
     * Retrieves an immutable snapshot of all logged entries.
     */
    fun getEntries(): List<NetworkAuditEntry> {
        return synchronized(auditLogList) {
            auditLogList.toList()
        }
    }

    /**
     * Clears all recorded entries.
     */
    fun clear() {
        auditLogList.clear()
    }

    /**
     * Computes cumulative telemetry stats without accessing any payload data.
     */
    fun getTelemetrySummary(): NetworkTelemetrySummary {
        val entries = getEntries()
        val totalBytes = entries.sumOf { it.payloadSizeBytes }
        val redactedCount = entries.count { it.isRedacted }
        val offlineCount = entries.count { it.endpointCategory == "OFFLINE_SIMULATION" || it.responseStatus == 0 }

        return NetworkTelemetrySummary(
            totalRequests = entries.size,
            totalBytesTransmitted = totalBytes,
            redactedRequestsCount = redactedCount,
            offlineInterventionsCount = offlineCount,
            lastTransmissionTimestamp = entries.firstOrNull()?.timestamp
        )
    }
}

data class NetworkTelemetrySummary(
    val totalRequests: Int,
    val totalBytesTransmitted: Long,
    val redactedRequestsCount: Int,
    val offlineInterventionsCount: Int,
    val lastTransmissionTimestamp: Long?
)
