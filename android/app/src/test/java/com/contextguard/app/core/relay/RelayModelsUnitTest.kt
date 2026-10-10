package com.contextguard.app.core.relay

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RelayModelsUnitTest {

    @Test
    fun testRelayEventPayloadSerialization() {
        val event = RelayEventPayload(
            eventId = "evt-unit-1",
            deviceId = "dev_test_123",
            eventType = RelayEventType.PRE_ACTION_INTERVENTION,
            timestamp = "2026-10-10T12:00:00Z",
            sourceApp = "com.whatsapp",
            riskCategory = "CREDENTIAL_HARVESTING",
            intervention = "STOP",
            riskScore = 0.88f,
            severity = 0.90f,
            reversibility = 1.0f,
            confidence = 0.95f,
            evidenceSummary = listOf("Masked account [ACCOUNT_REDACTED]"),
            modelVersion = "1.0.0",
            latencyMs = 120,
            redactionCount = 1,
            correlationId = "sha256_mock",
            networkMode = "LOCAL_BACKEND"
        )

        val json = event.toJson()
        assertEquals("evt-unit-1", json.getString("event_id"))
        assertEquals("dev_test_123", json.getString("device_id"))
        assertEquals("PRE_ACTION_INTERVENTION", json.getString("event_type"))
        assertEquals("STOP", json.getString("intervention"))
        assertEquals(0.88, json.getDouble("risk_score"), 0.001)

        // Privacy Guarantee: Assert no raw image or password fields exist
        assertFalse(json.has("screenshot"))
        assertFalse(json.has("raw_image"))
        assertFalse(json.has("password"))
        assertFalse(json.has("otp"))
    }

    @Test
    fun testHeartbeatPayloadSerialization() {
        val hb = DeviceHeartbeatPayload(
            deviceId = "dev_test_456",
            monitoringActive = true,
            notificationActive = true,
            selectedAppsCount = 8,
            localModelHealthy = true,
            networkMode = "LOCAL_BACKEND",
            telemetryEnabled = true,
            batteryLevel = 0.92f
        )

        val json = hb.toJson()
        assertEquals("dev_test_456", json.getString("device_id"))
        assertTrue(json.getBoolean("monitoring_active"))
        assertEquals(8, json.getInt("selected_apps_count"))
        assertEquals(0.92, json.getDouble("battery_level"), 0.01)
    }

    @Test
    fun testCommandAckSerialization() {
        val ack = RelayCommandAck(
            commandId = "cmd_test_789",
            deviceId = "dev_test_456",
            status = RelayCommandStatus.SUCCEEDED,
            result = mapOf("status" to "ACTIVE", "code" to 200)
        )

        val json = ack.toJson()
        assertEquals("cmd_test_789", json.getString("command_id"))
        assertEquals("SUCCEEDED", json.getString("status"))
        assertTrue(json.getJSONObject("result").has("status"))
    }
}
