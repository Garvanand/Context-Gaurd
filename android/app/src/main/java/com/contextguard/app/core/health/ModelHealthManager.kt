package com.contextguard.app.core.health

data class LayerHealth(
    val name: String,
    val type: String,
    val status: String,
    val isReady: Boolean,
    val detail: String
)

data class SystemHealthState(
    val isBackendConnected: Boolean = false,
    val overallStatus: String = "Connecting...",
    val mlKitStatus: LayerHealth = LayerHealth(
        name = "Google ML Kit",
        type = "On-Device Perception",
        status = "Active",
        isReady = true,
        detail = "Text Recognition + Face Detection initialized on device"
    ),
    val urlModelStatus: LayerHealth = LayerHealth(
        name = "XGBoost Phishing Model",
        type = "Trained Classifier (PhiUSIIL)",
        status = "Standby",
        isReady = true,
        detail = "37-feature extractor pipeline (Schema v1.1.0) loaded"
    ),
    val vlmStatus: LayerHealth = LayerHealth(
        name = "Qwen2.5-VL-3B Reasoner",
        type = "Multimodal Reasoner",
        status = "Fallback Ready",
        isReady = true,
        detail = "Local Ollama client with deterministic safety fallback"
    ),
    val policyEngineStatus: LayerHealth = LayerHealth(
        name = "Deterministic Policy Engine",
        type = "Mathematical Gating",
        status = "Ready",
        isReady = true,
        detail = "rho = s * (1 + lambda * r) | Fail-Safe Invariant Enforced"
    ),
    val lastCheckedMillis: Long = System.currentTimeMillis()
)

object ModelHealthManager {
    fun getLiveHealthState(isBackendConnected: Boolean = false): SystemHealthState {
        val urlEngineLoaded = com.contextguard.app.core.engine.UrlTreeInferenceEngine.getInstanceOrNull() != null

        return SystemHealthState(
            isBackendConnected = isBackendConnected,
            overallStatus = if (urlEngineLoaded) "Fully Operational (On-Device)" else "Core Perception Active",
            urlModelStatus = LayerHealth(
                name = "XGBoost Phishing Model",
                type = "On-Device Tree Interpreter",
                status = if (urlEngineLoaded) "Active (On-Device)" else "Standby",
                isReady = urlEngineLoaded,
                detail = if (urlEngineLoaded) "120 trees | 37 features (Schema v1.1.0) loaded in RAM" else "37-feature extractor pipeline loaded"
            ),
            lastCheckedMillis = System.currentTimeMillis()
        )
    }
}
