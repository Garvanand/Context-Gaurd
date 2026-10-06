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
        detail = "35-feature extractor pipeline loaded"
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
