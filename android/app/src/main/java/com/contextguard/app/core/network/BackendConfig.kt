package com.contextguard.app.core.network

import com.contextguard.app.core.privacy.NetworkMode

/**
 * Three explicit experimental evaluation modes for ContextGuard:
 * - ON_DEVICE: Zero outbound transmission; purely local perception and rule gating.
 * - REDACTED_LOCAL_BACKEND: Standard proposed ContextGuard mode with on-device canvas & PII redaction.
 * - RAW_CLOUD_EVALUATION: Research benchmark evaluation mode ONLY. Never permitted in production.
 */
enum class InferenceMode(val displayName: String, val isBenchmarkOnly: Boolean = false) {
    ON_DEVICE("ON-DEVICE"),
    REDACTED_LOCAL_BACKEND("REDACTED"),
    RAW_CLOUD_EVALUATION("RAW EVALUATION", isBenchmarkOnly = true);

    fun toNetworkMode(): NetworkMode = when (this) {
        ON_DEVICE -> NetworkMode.OFFLINE
        REDACTED_LOCAL_BACKEND -> NetworkMode.LOCAL_BACKEND
        RAW_CLOUD_EVALUATION -> NetworkMode.RESTRICTED_EVALUATION
    }

    companion object {
        // Backwards compatibility aliases
        val OFFLINE: InferenceMode get() = ON_DEVICE
        val RAW_EVALUATION_ONLY: InferenceMode get() = RAW_CLOUD_EVALUATION

        fun fromNetworkMode(mode: NetworkMode): InferenceMode = when (mode) {
            NetworkMode.OFFLINE -> ON_DEVICE
            NetworkMode.LOCAL_BACKEND -> REDACTED_LOCAL_BACKEND
            NetworkMode.RESTRICTED_EVALUATION -> RAW_CLOUD_EVALUATION
        }
    }
}

data class BackendConfig(
    val host: String = "10.0.2.2", // Standard Android emulator loopback to host
    val port: Int = 8000,
    val useHttps: Boolean = false,
    val inferenceMode: InferenceMode = InferenceMode.REDACTED_LOCAL_BACKEND,
    val timeoutSeconds: Int = 15
) {
    val baseUrl: String
        get() {
            val scheme = if (useHttps) "https" else "http"
            return "$scheme://$host:$port"
        }

    val isOffline: Boolean
        get() = inferenceMode == InferenceMode.ON_DEVICE

    val networkMode: NetworkMode
        get() = inferenceMode.toNetworkMode()
}
