package com.contextguard.app.core.relay

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Data contracts and serialization for the ContextGuard Mobile <-> Web Relay.
 */

enum class RelayDeviceStatus {
    ONLINE, OFFLINE, DEGRADED, UNPAIRED
}

enum class RelayPairingStatus {
    PENDING, CLAIMED, APPROVED, DENIED, EXPIRED, REVOKED
}

enum class RelayCommandType {
    REQUEST_STATUS,
    REQUEST_DIAGNOSTICS,
    RUN_SYNTHETIC_DEMO,
    REQUEST_CONFIG_REFRESH
}

enum class RelayCommandStatus {
    QUEUED, DELIVERED, ACKNOWLEDGED, SUCCEEDED, FAILED, EXPIRED, CANCELLED
}

enum class RelayEventType {
    ACCESSIBILITY_TRIGGER,
    NOTIFICATION_TRIAGE,
    URL_RISK_EVALUATION,
    PRE_ACTION_INTERVENTION,
    SERVICE_STATUS_CHANGE,
    MODEL_HEALTH,
    SYNTHETIC_DEMO_RESULT
}

data class PairingStartResponse(
    val sessionId: String,
    val pairingCode: String,
    val expiresAt: String,
    val expiresInSeconds: Int,
    val qrPayload: String
)

data class PairingStatusResponse(
    val sessionId: String,
    val status: RelayPairingStatus,
    val deviceId: String?,
    val deviceName: String?,
    val token: String?
)

data class DeviceHeartbeatPayload(
    val deviceId: String,
    val monitoringActive: Boolean,
    val notificationActive: Boolean,
    val selectedAppsCount: Int,
    val localModelHealthy: Boolean,
    val networkMode: String,
    val telemetryEnabled: Boolean,
    val batteryLevel: Float? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("device_id", deviceId)
        put("monitoring_active", monitoringActive)
        put("notification_active", notificationActive)
        put("selected_apps_count", selectedAppsCount)
        put("local_model_healthy", localModelHealthy)
        put("network_mode", networkMode)
        put("telemetry_enabled", telemetryEnabled)
        if (batteryLevel != null) put("battery_level", batteryLevel.toDouble())
    }
}

/**
 * Privacy-Safe Outbox Event.
 * INVARIANT: Never contains raw bitmap blobs, raw passwords, or unmasked credentials.
 */
data class RelayEventPayload(
    val eventId: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val eventType: RelayEventType,
    val timestamp: String,
    val sourceApp: String?,
    val riskCategory: String,
    val intervention: String,
    val riskScore: Float,
    val severity: Float,
    val reversibility: Float,
    val confidence: Float,
    val evidenceSummary: List<String>,
    val modelVersion: String = "1.0.0",
    val latencyMs: Int,
    val redactionCount: Int,
    val correlationId: String?,
    val networkMode: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("event_id", eventId)
        put("device_id", deviceId)
        put("event_type", eventType.name)
        put("timestamp", timestamp)
        put("source_app", sourceApp ?: JSONObject.NULL)
        put("risk_category", riskCategory)
        put("intervention", intervention)
        put("risk_score", riskScore.toDouble())
        put("severity", severity.toDouble())
        put("reversibility", reversibility.toDouble())
        put("confidence", confidence.toDouble())
        put("evidence_summary", JSONArray(evidenceSummary))
        put("model_version", modelVersion)
        put("latency_ms", latencyMs)
        put("redaction_count", redactionCount)
        put("correlation_id", correlationId ?: JSONObject.NULL)
        put("network_mode", networkMode)
    }
}

data class RelayCommandEnvelope(
    val commandId: String,
    val commandType: RelayCommandType,
    val payload: JSONObject,
    val expiresAt: String?
)

data class RelayCommandAck(
    val commandId: String,
    val deviceId: String,
    val status: RelayCommandStatus,
    val result: Map<String, Any> = emptyMap(),
    val errorMessage: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("command_id", commandId)
        put("device_id", deviceId)
        put("status", status.name)
        val resObj = JSONObject()
        result.forEach { (k, v) -> resObj.put(k, v) }
        put("result", resObj)
        if (errorMessage != null) put("error_message", errorMessage)
    }
}
