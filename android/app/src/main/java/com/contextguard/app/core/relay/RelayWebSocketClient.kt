package com.contextguard.app.core.relay

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class RelaySocketState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

/**
 * Authenticated OkHttp WebSocket client for mobile-to-relay communication.
 */
class RelayWebSocketClient(
    private val storage: RelayStorage,
    private val onCommandReceived: (RelayCommandEnvelope) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private val _connectionState = MutableStateFlow(RelaySocketState.DISCONNECTED)
    val connectionState: StateFlow<RelaySocketState> = _connectionState.asStateFlow()

    private var currentBaseUrl: String = ""
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectAttempts = 0

    fun connect(baseUrl: String) {
        currentBaseUrl = baseUrl
        val token = storage.deviceToken
        if (token.isNullOrBlank()) {
            Log.d(TAG, "Cannot connect WebSocket: Device not yet paired")
            _connectionState.value = RelaySocketState.DISCONNECTED
            return
        }

        reconnectJob?.cancel()
        _connectionState.value = RelaySocketState.CONNECTING

        val wsUrl = baseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/') + "/api/v1/relay/ws/device"

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.i(TAG, "Relay WebSocket opened, sending AUTH handshake")
                // Send authentication handshake
                val authMsg = JSONObject().apply {
                    put("token", token)
                    put("device_id", storage.deviceId)
                }
                ws.send(authMsg.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "AUTH_SUCCESS" -> {
                            Log.i(TAG, "Relay WebSocket AUTH_SUCCESS")
                            _connectionState.value = RelaySocketState.CONNECTED
                            reconnectAttempts = 0
                        }
                        "COMMAND_DELIVER" -> {
                            val payload = json.optJSONObject("payload") ?: JSONObject()
                            val commandId = payload.optString("command_id")
                            val typeStr = payload.optString("command_type")
                            val cmdPayload = payload.optJSONObject("payload") ?: JSONObject()
                            val expiresAt = if (payload.has("expires_at")) payload.optString("expires_at") else null

                            val cmdType = try {
                                RelayCommandType.valueOf(typeStr)
                            } catch (e: Exception) {
                                RelayCommandType.REQUEST_STATUS
                            }

                            val env = RelayCommandEnvelope(
                                commandId = commandId,
                                commandType = cmdType,
                                payload = cmdPayload,
                                expiresAt = expiresAt
                            )
                            onCommandReceived(env)
                        }
                        "PONG" -> {
                            // Keepalive acknowledged
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message from relay: $text", e)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                _connectionState.value = RelaySocketState.DISCONNECTED
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                _connectionState.value = RelaySocketState.DISCONNECTED
                scheduleReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure: ${t.message}")
                _connectionState.value = RelaySocketState.DISCONNECTED
                scheduleReconnect()
            }
        })
    }

    fun sendTelemetry(event: RelayEventPayload): Boolean {
        val ws = webSocket ?: return false
        if (_connectionState.value != RelaySocketState.CONNECTED) return false
        val envelope = JSONObject().apply {
            put("type", "TELEMETRY_EVENT")
            put("payload", event.toJson())
        }
        return ws.send(envelope.toString())
    }

    fun sendHeartbeat(hb: DeviceHeartbeatPayload): Boolean {
        val ws = webSocket ?: return false
        if (_connectionState.value != RelaySocketState.CONNECTED) return false
        val envelope = JSONObject().apply {
            put("type", "HEARTBEAT")
            put("payload", hb.toJson())
        }
        return ws.send(envelope.toString())
    }

    fun sendAck(ack: RelayCommandAck): Boolean {
        val ws = webSocket ?: return false
        if (_connectionState.value != RelaySocketState.CONNECTED) return false
        val envelope = JSONObject().apply {
            put("type", "COMMAND_ACK")
            put("payload", ack.toJson())
        }
        return ws.send(envelope.toString())
    }

    private fun scheduleReconnect() {
        if (!storage.isPaired) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            _connectionState.value = RelaySocketState.RECONNECTING
            reconnectAttempts++
            val delayMs = (1000L * (1 shl minOf(reconnectAttempts, 5))).coerceAtMost(30000L)
            Log.d(TAG, "Scheduling WebSocket reconnect in ${delayMs}ms")
            delay(delayMs)
            if (currentBaseUrl.isNotBlank()) {
                connect(currentBaseUrl)
            }
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _connectionState.value = RelaySocketState.DISCONNECTED
    }

    companion object {
        private const val TAG = "RelayWebSocket"
    }
}
