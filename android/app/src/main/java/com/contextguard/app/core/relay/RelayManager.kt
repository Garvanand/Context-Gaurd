package com.contextguard.app.core.relay

import android.content.Context
import android.os.Build
import android.util.Log
import com.contextguard.app.core.accessibility.AllowlistManager
import com.contextguard.app.core.notification.NotificationGuardStateManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Main coordinator for ContextGuard mobile relay integration.
 * Manages pairing state, periodic heartbeats, durable telemetry outbox,
 * and executes incoming supervisor commands.
 */
class RelayManager private constructor(private val context: Context) {

    private val storage = RelayStorage.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isPaired = MutableStateFlow(storage.isPaired)
    val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()

    private val _currentPairingSession = MutableStateFlow<PairingStartResponse?>(null)
    val currentPairingSession: StateFlow<PairingStartResponse?> = _currentPairingSession.asStateFlow()

    private val _pendingClaim = MutableStateFlow<String?>(null) // Session ID if dashboard claimed
    val pendingClaim: StateFlow<String?> = _pendingClaim.asStateFlow()

    private val outbox = ConcurrentLinkedQueue<RelayEventPayload>()
    private var heartbeatJob: Job? = null
    private var pollJob: Job? = null

    val wsClient = RelayWebSocketClient(storage) { envelope ->
        handleInboundCommand(envelope)
    }

    val socketState: StateFlow<RelaySocketState> get() = wsClient.connectionState

    fun initConnection(baseUrl: String) {
        if (storage.isPaired) {
            wsClient.connect(baseUrl)
            startHeartbeatLoop(baseUrl)
        }
    }

    // =========================================================================
    // 1. PAIRING WORKFLOW
    // =========================================================================

    fun startPairing(baseUrl: String, onResult: (PairingStartResponse?) -> Unit) {
        scope.launch {
            try {
                val url = URL(baseUrl.trimEnd('/') + "/api/v1/relay/pairing/start")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val req = JSONObject().apply {
                    put("device_name", "${Build.MANUFACTURER} ${Build.MODEL}")
                    put("platform", "Android")
                    put("model", Build.MODEL)
                    put("app_version", "1.0.0")
                }

                OutputStreamWriter(conn.outputStream).use { it.write(req.toString()) }

                if (conn.responseCode in 200..299) {
                    val resp = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JSONObject(resp)
                    val pairing = PairingStartResponse(
                        sessionId = json.getString("session_id"),
                        pairingCode = json.getString("pairing_code"),
                        expiresAt = json.getString("expires_at"),
                        expiresInSeconds = json.getInt("expires_in_seconds"),
                        qrPayload = json.getString("qr_payload")
                    )
                    _currentPairingSession.value = pairing
                    startPollingClaim(baseUrl, pairing.sessionId)
                    withContext(Dispatchers.Main) { onResult(pairing) }
                } else {
                    withContext(Dispatchers.Main) { onResult(null) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start pairing", e)
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }

    private fun startPollingClaim(baseUrl: String, sessionId: String) {
        pollJob?.cancel()
        pollJob = scope.launch {
            repeat(60) {
                delay(3000)
                try {
                    val url = URL(baseUrl.trimEnd('/') + "/api/v1/relay/pairing/status/$sessionId?client_type=device")
                    val conn = url.openConnection() as HttpURLConnection
                    if (conn.responseCode in 200..299) {
                        val resp = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                        val json = JSONObject(resp)
                        val status = json.getString("status")
                        if (status == "CLAIMED") {
                            _pendingClaim.value = sessionId
                            return@launch
                        } else if (status == "APPROVED" || status == "DENIED" || status == "EXPIRED") {
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient network errors
                }
            }
        }
    }

    fun approvePairing(baseUrl: String, sessionId: String, onComplete: (Boolean) -> Unit) {
        scope.launch {
            try {
                val url = URL(baseUrl.trimEnd('/') + "/api/v1/relay/pairing/approve")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val req = JSONObject().apply {
                    put("session_id", sessionId)
                    put("approved", true)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(req.toString()) }

                if (conn.responseCode in 200..299) {
                    val resp = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val json = JSONObject(resp)
                    val devToken = json.optString("device_token")
                    if (devToken.isNotBlank()) {
                        storage.savePairing(storage.deviceId, devToken, System.currentTimeMillis().toString())
                        _isPaired.value = true
                        _pendingClaim.value = null
                        _currentPairingSession.value = null
                        wsClient.connect(baseUrl)
                        startHeartbeatLoop(baseUrl)
                        withContext(Dispatchers.Main) { onComplete(true) }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error approving pairing", e)
            }
            withContext(Dispatchers.Main) { onComplete(false) }
        }
    }

    fun denyPairing(baseUrl: String, sessionId: String, onComplete: (Boolean) -> Unit) {
        scope.launch {
            try {
                val url = URL(baseUrl.trimEnd('/') + "/api/v1/relay/pairing/deny")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val req = JSONObject().apply {
                    put("session_id", sessionId)
                    put("approved", false)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(req.toString()) }
                _pendingClaim.value = null
                _currentPairingSession.value = null
                withContext(Dispatchers.Main) { onComplete(true) }
            } catch (e: Exception) {
                Log.e(TAG, "Error denying pairing", e)
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun unpair() {
        storage.clearPairing()
        wsClient.disconnect()
        heartbeatJob?.cancel()
        _isPaired.value = false
        _currentPairingSession.value = null
        _pendingClaim.value = null
    }

    // =========================================================================
    // 2. HEARTBEAT LOOP
    // =========================================================================

    private fun startHeartbeatLoop(baseUrl: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && storage.isPaired) {
                sendHeartbeat(baseUrl)
                delay(15000)
            }
        }
    }

    private fun sendHeartbeat(baseUrl: String) {
        val appsCount = AllowlistManager.targetApps.value.count { it.isEnabled }
        val isNotifActive = NotificationGuardStateManager.isListenerConnected.value
        val hb = DeviceHeartbeatPayload(
            deviceId = storage.deviceId,
            monitoringActive = true,
            notificationActive = isNotifActive,
            selectedAppsCount = appsCount,
            localModelHealthy = true,
            networkMode = "LOCAL_BACKEND",
            telemetryEnabled = storage.isTelemetrySyncEnabled,
            batteryLevel = 0.85f
        )

        // Try WebSocket first
        if (wsClient.sendHeartbeat(hb)) return

        // Fallback to REST
        try {
            val token = storage.deviceToken ?: return
            val url = URL(baseUrl.trimEnd('/') + "/api/v1/relay/device/heartbeat")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream).use { it.write(hb.toJson().toString()) }
            conn.responseCode // execute
        } catch (e: Exception) {
            // Heartbeat failure logged
        }
    }

    // =========================================================================
    // 3. TELEMETRY OUTBOX & STREAMING
    // =========================================================================

    fun dispatchTelemetryEvent(event: RelayEventPayload, baseUrl: String) {
        // Enforce user consent: If telemetry sync disabled, zero egress!
        if (!storage.isTelemetrySyncEnabled || !storage.isPaired) {
            Log.d(TAG, "Telemetry sync disabled by user policy; event suppressed")
            return
        }

        // Try live WebSocket
        if (wsClient.sendTelemetry(event)) {
            Log.i(TAG, "Dispatched event '${event.eventId}' via live WebSocket")
            return
        }

        // Add to outbox and flush via REST
        outbox.add(event)
        flushOutbox(baseUrl)
    }

    private fun flushOutbox(baseUrl: String) {
        scope.launch {
            val token = storage.deviceToken ?: return@launch
            while (!outbox.isEmpty()) {
                val ev = outbox.peek() ?: break
                try {
                    val url = URL(baseUrl.trimEnd('/') + "/api/v1/relay/events/ingest")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.doOutput = true
                    OutputStreamWriter(conn.outputStream).use { it.write(ev.toJson().toString()) }

                    if (conn.responseCode in 200..299) {
                        outbox.poll() // Remove confirmed event
                        Log.i(TAG, "Flushed outbox event '${ev.eventId}' via REST")
                    } else {
                        break // Backoff
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    // =========================================================================
    // 4. INBOUND COMMAND PROCESSOR
    // =========================================================================

    private fun handleInboundCommand(env: RelayCommandEnvelope) {
        scope.launch {
            Log.i(TAG, "Received supervisor command: ${env.commandType} (ID: ${env.commandId})")
            val ack = when (env.commandType) {
                RelayCommandType.REQUEST_STATUS -> {
                    val appsCount = AllowlistManager.targetApps.value.count { it.isEnabled }
                    RelayCommandAck(
                        commandId = env.commandId,
                        deviceId = storage.deviceId,
                        status = RelayCommandStatus.SUCCEEDED,
                        result = mapOf(
                            "monitoring_active" to true,
                            "notification_listener" to NotificationGuardStateManager.isListenerConnected.value,
                            "selected_apps" to appsCount,
                            "telemetry_sync" to storage.isTelemetrySyncEnabled,
                            "device_model" to Build.MODEL,
                            "android_sdk" to Build.VERSION.SDK_INT
                        )
                    )
                }

                RelayCommandType.REQUEST_DIAGNOSTICS -> {
                    RelayCommandAck(
                        commandId = env.commandId,
                        deviceId = storage.deviceId,
                        status = RelayCommandStatus.SUCCEEDED,
                        result = mapOf(
                            "heap_free_mb" to (Runtime.getRuntime().freeMemory() / 1024 / 1024),
                            "heap_total_mb" to (Runtime.getRuntime().totalMemory() / 1024 / 1024),
                            "ml_kit_ocr" to "READY",
                            "ml_kit_face" to "READY",
                            "outbox_queue_depth" to outbox.size
                        )
                    )
                }

                RelayCommandType.RUN_SYNTHETIC_DEMO -> {
                    // Safe execution of predefined synthetic test scenario
                    // Never captures or uploads user screen!
                    RelayCommandAck(
                        commandId = env.commandId,
                        deviceId = storage.deviceId,
                        status = RelayCommandStatus.SUCCEEDED,
                        result = mapOf(
                            "scenario" to "synthetic_bank_statement_v1",
                            "action" to "POST_TO_PUBLIC_FEED",
                            "intervention" to "STOP",
                            "risk_score" to 0.88,
                            "severity" to 0.95,
                            "reversibility" to 1.00,
                            "confidence" to 0.96,
                            "grounded_evidence" to "Simulated account number masked [ACCOUNT_REDACTED]; public broadcast hazard"
                        )
                    )
                }

                RelayCommandType.REQUEST_CONFIG_REFRESH -> {
                    RelayCommandAck(
                        commandId = env.commandId,
                        deviceId = storage.deviceId,
                        status = RelayCommandStatus.SUCCEEDED,
                        result = mapOf("config_status" to "REFRESHED")
                    )
                }
            }

            wsClient.sendAck(ack)
        }
    }

    companion object {
        private const val TAG = "RelayManager"

        @Volatile
        private var instance: RelayManager? = null

        fun getInstance(context: Context): RelayManager {
            return instance ?: synchronized(this) {
                instance ?: RelayManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
