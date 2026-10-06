package com.contextguard.app.core.network

enum class InferenceMode {
    OFFLINE,
    REDACTED_LOCAL_BACKEND,
    RAW_EVALUATION_ONLY
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
        get() = inferenceMode == InferenceMode.OFFLINE
}
