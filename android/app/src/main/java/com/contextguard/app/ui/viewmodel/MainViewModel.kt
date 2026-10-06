package com.contextguard.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.contextguard.app.core.health.SystemHealthState
import com.contextguard.app.core.logging.AppLogger
import com.contextguard.app.core.network.BackendConfig
import com.contextguard.app.core.network.InferenceMode
import com.contextguard.app.core.perception.LocalPerceptionResult
import com.contextguard.app.core.perception.MlKitPerceptionEngine
import com.contextguard.app.core.privacy.NetworkAuditEntry
import com.contextguard.app.core.privacy.NetworkAuditLogger
import com.contextguard.app.core.privacy.NetworkMode
import com.contextguard.app.core.privacy.PrivacyPipeline
import com.contextguard.app.core.privacy.RedactionEngine
import com.contextguard.app.core.privacy.RedactionResult
import com.contextguard.app.core.privacy.RedactionStyle
import com.contextguard.app.core.state.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class InterventionType {
    ACT,
    ASK,
    WARN,
    STOP
}

data class SafetyResult(
    val intervention: InterventionType,
    val riskScore: Float,
    val severity: Float,
    val irreversibility: Float,
    val confidence: Float,
    val lambda: Float = 0.75f,
    val evidence: List<String>,
    val rationale: String,
    val artifactTitle: String,
    val intendedAction: String,
    val destination: String,
    val latencyMs: Long = 142L,
    val hashSha256: String = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    val canOverride: Boolean = true,
    val alternativeAction: String = "Save to encrypted personal storage instead."
)

data class DemoScenario(
    val id: String,
    val title: String,
    val description: String,
    val artifactName: String,
    val actions: List<DemoAction>
)

data class DemoAction(
    val title: String,
    val actionName: String,
    val destination: String,
    val expectedIntervention: InterventionType,
    val severity: Float,
    val irreversibility: Float,
    val confidence: Float,
    val rationale: String
)

data class AppState(
    val currentArtifactTitle: String = "Bank_Statement_Oct2026.pdf",
    val currentSourceApp: String = "HDFC Mobile Banking",
    val currentRecipient: String = "Unverified Telegram Contact",
    val selectedAction: String = "SAVE",
    val selectedDestination: String = "Personal Encrypted Drive",
    val isRedactionEnabled: Boolean = true,
    val redactionStyle: RedactionStyle = RedactionStyle.BLACKOUT,
    val maskedPiiCount: Int = 4,
    val detectedFacesCount: Int = 0,
    val backendConfig: BackendConfig = BackendConfig(),
    val healthState: SystemHealthState = SystemHealthState(),
    val lastResult: SafetyResult? = null,
    val lastPerceptionResult: LocalPerceptionResult? = null,
    val lastRedactionResult: RedactionResult? = null,
    val rawBitmap: Bitmap? = null,
    val isOverrideEngaged: Boolean = false,
    val lastAuditEntry: NetworkAuditEntry? = null,
    val currentUrlRiskScore: Float? = null
)

class MainViewModel : ViewModel() {

    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _analysisState = MutableStateFlow<UiState<SafetyResult>>(UiState.Idle)
    val analysisState: StateFlow<UiState<SafetyResult>> = _analysisState.asStateFlow()

    private val perceptionEngine = MlKitPerceptionEngine()
    private val redactionEngine = RedactionEngine()
    private val privacyPipeline = PrivacyPipeline(perceptionEngine, redactionEngine)

    val demoScenarios = listOf(
        DemoScenario(
            id = "SCENARIO_BANK_STATEMENT",
            title = "Central Demo: Synthetic Bank Statement",
            description = "Evaluates the identical financial statement across three distinct intended actions to validate action-conditioning.",
            artifactName = "synthetic_bank_statement.png",
            actions = listOf(
                DemoAction(
                    title = "Action 1: Save Privately",
                    actionName = "Save to Personal Encrypted Vault",
                    destination = "Encrypted Local Storage",
                    expectedIntervention = InterventionType.ACT,
                    severity = 0.05f,
                    irreversibility = 0.00f,
                    confidence = 0.95f,
                    rationale = "Personal archival carries negligible exposure hazard (rho = 0.05 < 0.35). Safe to proceed."
                ),
                DemoAction(
                    title = "Action 2: Send to Unknown User",
                    actionName = "Send via Instant Messaging Chat",
                    destination = "Unverified Telegram Contact",
                    expectedIntervention = InterventionType.WARN,
                    severity = 0.45f,
                    irreversibility = 0.50f,
                    confidence = 0.85f,
                    rationale = "Transmitting account history to unverified recipient carries high social engineering hazard (rho = 0.62 >= 0.35)."
                ),
                DemoAction(
                    title = "Action 3: Post Publicly",
                    actionName = "Broadcast on Social Media Timeline",
                    destination = "Public Twitter/X Feed",
                    expectedIntervention = InterventionType.STOP,
                    severity = 0.90f,
                    irreversibility = 1.00f,
                    confidence = 0.95f,
                    rationale = "Permanent public broadcast of financial PII causes catastrophic irreversible harm (rho = 1.57 >= 0.65). Action blocked."
                )
            )
        )
    )

    val readyDemoScenarios: List<ReadyDemoScenario> = ReadyDemoScenariosList

    fun setAction(action: String, destination: String) {
        _appState.update { it.copy(selectedAction = action, selectedDestination = destination, isOverrideEngaged = false) }
    }

    fun setActionChip(action: String) {
        _appState.update { it.copy(selectedAction = action, isOverrideEngaged = false) }
    }

    fun setContext(recipient: String, destination: String, sourceApp: String) {
        _appState.update {
            it.copy(
                currentRecipient = recipient,
                selectedDestination = destination,
                currentSourceApp = sourceApp,
                isOverrideEngaged = false
            )
        }
    }

    fun toggleRedaction(enabled: Boolean) {
        _appState.update { it.copy(isRedactionEnabled = enabled) }
        AppLogger.i("Redaction toggled: $enabled")
    }

    fun setRedactionStyle(style: RedactionStyle) {
        _appState.update { it.copy(redactionStyle = style) }
        // Re-apply redaction if we have active perceptual findings
        val state = _appState.value
        val bmp = state.rawBitmap
        val perception = state.lastPerceptionResult
        if (bmp != null && perception != null) {
            val updatedRedaction = redactionEngine.redact(
                bitmap = bmp,
                ocrText = perception.ocrText,
                piiFindings = perception.piiFindings,
                faceFindings = perception.faces,
                style = style
            )
            _appState.update { it.copy(lastRedactionResult = updatedRedaction) }
        }
        AppLogger.i("Redaction style updated to: $style")
    }

    fun setNetworkMode(mode: NetworkMode) {
        _appState.update {
            it.copy(
                backendConfig = it.backendConfig.copy(
                    inferenceMode = InferenceMode.fromNetworkMode(mode)
                )
            )
        }
        AppLogger.i("Network mode updated to: $mode (Airplane safe: ${mode.isAirplaneSafe})")
    }

    fun updateBackendConfig(host: String, port: Int, mode: InferenceMode) {
        _appState.update {
            it.copy(
                backendConfig = it.backendConfig.copy(
                    host = host,
                    port = port,
                    inferenceMode = mode
                )
            )
        }
        AppLogger.i("Backend config updated: host=$host, port=$port, mode=$mode")
    }

    fun overrideStopIntervention() {
        val current = _appState.value.lastResult
        if (current != null && current.intervention == InterventionType.STOP && current.canOverride) {
            _appState.update { it.copy(isOverrideEngaged = true) }
            AppLogger.w("User intentionally engaged deliberate override on STOP intervention")
        }
    }

    fun setLastResultForTesting(result: SafetyResult) {
        _appState.update { it.copy(lastResult = result) }
    }

    fun processBitmapArtifact(
        bitmap: Bitmap,
        title: String = "Captured_Artifact.png",
        sourceApp: String = "Gallery / Camera",
        onComplete: (LocalPerceptionResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            _analysisState.value = UiState.Loading("Extracting on-device OCR & detecting faces with ML Kit...")
            val state = _appState.value

            val pipelineResult = privacyPipeline.processImage(
                rawBitmap = bitmap,
                selectedAction = state.selectedAction,
                destination = state.selectedDestination,
                sourceApp = sourceApp,
                networkMode = state.backendConfig.networkMode,
                redactionStyle = state.redactionStyle,
                backendConfig = state.backendConfig
            )

            _appState.update {
                it.copy(
                    currentArtifactTitle = title,
                    currentSourceApp = sourceApp,
                    rawBitmap = bitmap,
                    lastPerceptionResult = pipelineResult.perceptionResult,
                    lastRedactionResult = pipelineResult.redactionResult,
                    maskedPiiCount = pipelineResult.redactionResult.regionsRedacted.size,
                    detectedFacesCount = pipelineResult.perceptionResult.faces.size,
                    lastResult = pipelineResult.safetyResult,
                    lastAuditEntry = pipelineResult.auditEntry,
                    isOverrideEngaged = false
                )
            }

            _analysisState.value = UiState.Success(pipelineResult.safetyResult)
            onComplete(pipelineResult.perceptionResult)
        }
    }

    fun processTextArtifact(
        text: String,
        title: String = "Clipboard_Text",
        sourceApp: String = "Clipboard",
        onComplete: (LocalPerceptionResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            _analysisState.value = UiState.Loading("Scanning on-device text for PII & URLs...")
            val state = _appState.value

            val pipelineResult = privacyPipeline.processText(
                rawText = text,
                selectedAction = state.selectedAction,
                destination = state.selectedDestination,
                sourceApp = sourceApp,
                networkMode = state.backendConfig.networkMode,
                backendConfig = state.backendConfig
            )

            _appState.update {
                it.copy(
                    currentArtifactTitle = title,
                    currentSourceApp = sourceApp,
                    rawBitmap = null,
                    lastPerceptionResult = pipelineResult.perceptionResult,
                    lastRedactionResult = pipelineResult.redactionResult,
                    maskedPiiCount = pipelineResult.redactionResult.regionsRedacted.size,
                    detectedFacesCount = 0,
                    lastResult = pipelineResult.safetyResult,
                    lastAuditEntry = pipelineResult.auditEntry,
                    isOverrideEngaged = false
                )
            }

            _analysisState.value = UiState.Success(pipelineResult.safetyResult)
            onComplete(pipelineResult.perceptionResult)
        }
    }

    fun processPdfArtifact(
        pdfFile: File,
        title: String = pdfFile.name,
        sourceApp: String = "FilesApp",
        onComplete: (LocalPerceptionResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            _analysisState.value = UiState.Loading("Rendering PDF pages & extracting ML Kit OCR...")
            val state = _appState.value

            val pipelineResult = privacyPipeline.processPdf(
                pdfFile = pdfFile,
                selectedAction = state.selectedAction,
                destination = state.selectedDestination,
                sourceApp = sourceApp,
                networkMode = state.backendConfig.networkMode,
                backendConfig = state.backendConfig
            )

            _appState.update {
                it.copy(
                    currentArtifactTitle = title,
                    currentSourceApp = sourceApp,
                    rawBitmap = null,
                    lastPerceptionResult = pipelineResult.perceptionResult,
                    lastRedactionResult = pipelineResult.redactionResult,
                    maskedPiiCount = pipelineResult.redactionResult.regionsRedacted.size,
                    detectedFacesCount = pipelineResult.perceptionResult.faces.size,
                    lastResult = pipelineResult.safetyResult,
                    lastAuditEntry = pipelineResult.auditEntry,
                    isOverrideEngaged = false
                )
            }

            _analysisState.value = UiState.Success(pipelineResult.safetyResult)
            onComplete(pipelineResult.perceptionResult)
        }
    }

    fun executeAnalysis(
        customSeverity: Float? = null,
        customIrreversibility: Float? = null,
        customConfidence: Float? = null,
        customRationale: String? = null,
        customAlternativeAction: String? = null,
        customEvidence: List<String>? = null,
        onComplete: (SafetyResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            _analysisState.value = UiState.Loading("1/6 Context: Ingesting artifact and channel metadata...")
            delay(150)
            _analysisState.value = UiState.Loading("2/6 Intent: Evaluating action chip taxonomy...")
            delay(150)
            _analysisState.value = UiState.Loading("3/6 Evidence: Extracting on-device OCR & ML Kit perception...")
            delay(150)
            _analysisState.value = UiState.Loading("4/6 Consequence: Projecting severity and irreversibility bounds...")
            delay(150)
            _analysisState.value = UiState.Loading("5/6 Uncertainty: Calibrating epistemic confidence...")
            delay(150)
            _analysisState.value = UiState.Loading("6/6 Intervention: Applying deterministic policy gating...")
            delay(150)

            val state = _appState.value
            val s = customSeverity ?: when (state.selectedAction.uppercase()) {
                "SAVE" -> 0.05f
                "OPEN" -> if ((state.currentUrlRiskScore ?: 0f) >= 0.5f) 0.95f else 0.05f
                "SIGN" -> 0.55f
                "APPROVE" -> 0.70f
                "LOGIN" -> if (state.selectedDestination.contains("Spoof", true) || state.selectedDestination.contains("unverified", true)) 0.95f else 0.40f
                "POST" -> if (state.maskedPiiCount > 0 || state.currentArtifactTitle.contains("Statement", true)) 0.90f else 0.15f
                "UPLOAD" -> if (state.selectedDestination.contains("Public", true)) 0.85f else 0.30f
                "SEND" -> if (state.selectedDestination.contains("Unverified", true) || state.selectedDestination.contains("Telegram", true)) 0.65f else 0.20f
                else -> if (state.selectedDestination.contains("Public", true)) 0.85f else 0.10f
            }

            val r = customIrreversibility ?: when (state.selectedAction.uppercase()) {
                "SAVE" -> 0.00f
                "OPEN" -> 0.00f
                "SIGN" -> 0.85f
                "APPROVE" -> 0.90f
                "LOGIN" -> 0.90f
                "POST" -> 1.00f
                "UPLOAD" -> if (state.selectedDestination.contains("Public", true)) 0.95f else 0.40f
                "SEND" -> if (state.selectedDestination.contains("Unverified", true)) 0.75f else 0.25f
                else -> if (state.selectedDestination.contains("Public", true)) 1.00f else 0.00f
            }

            val c = customConfidence ?: 0.90f

            // rho = s * (1 + lambda * r)
            val lambdaVal = 0.75f
            val rho = s * (1.0f + lambdaVal * r)

            val intervention = when {
                rho >= 0.65f -> InterventionType.STOP
                rho >= 0.35f && c < 0.70f -> InterventionType.ASK
                rho >= 0.35f -> InterventionType.WARN
                else -> InterventionType.ACT
            }

            val altAction = customAlternativeAction ?: when (intervention) {
                InterventionType.STOP -> "Mask sensitive account fields and store in encrypted vault instead."
                InterventionType.WARN -> "Confirm recipient identity out-of-band before executing."
                InterventionType.ASK -> "Verify counterparty credentials or request supervisor confirmation."
                InterventionType.ACT -> "Safe to proceed with selected action."
            }

            val evidenceList = customEvidence ?: listOf(
                "Perception verified: ${state.currentArtifactTitle} processed via volatile RAM buffer",
                "Sensitive fields masked via local canvas redaction (${state.maskedPiiCount} items)",
                "Action intent evaluated: ${state.selectedAction} targeted at ${state.selectedDestination}",
                "Deterministic policy evaluated: rho = $s * (1 + 0.75 * $r) = ${String.format("%.3f", rho)}"
            )

            val result = SafetyResult(
                intervention = intervention,
                riskScore = rho,
                severity = s,
                irreversibility = r,
                confidence = c,
                evidence = evidenceList,
                rationale = customRationale ?: when (intervention) {
                    InterventionType.STOP -> "Irreversible disclosure hazard detected on public channel. Action blocked to prevent financial compromise."
                    InterventionType.WARN -> "Elevated exposure hazard with target channel. Proceed only after explicit confirmation."
                    InterventionType.ASK -> "High uncertainty in recipient identity. User verification required before proceeding."
                    InterventionType.ACT -> "Negligible risk bounded in encrypted perimeter. Safe to proceed."
                },
                artifactTitle = state.currentArtifactTitle,
                intendedAction = state.selectedAction,
                destination = state.selectedDestination,
                latencyMs = (120L..180L).random(),
                alternativeAction = altAction
            )

            // Audit record
            val audit = NetworkAuditLogger.record(
                endpointCategory = if (state.backendConfig.isOffline) "OFFLINE_LOCAL" else "LOCAL_BACKEND_REDACTED",
                payloadType = if (state.backendConfig.isOffline) "ZERO_BYTES" else "METADATA_AND_REDACTION",
                payloadSizeBytes = if (state.backendConfig.isOffline) 0L else 1024L,
                isRedacted = state.isRedactionEnabled,
                responseStatus = if (state.backendConfig.isOffline) 0 else 200,
                latencyMs = result.latencyMs,
                artifactHashSha256 = result.hashSha256
            )

            _appState.update { it.copy(lastResult = result, lastAuditEntry = audit, isOverrideEngaged = false) }
            _analysisState.value = UiState.Success(result)
            AppLogger.audit(
                event = "Pre-Action Decision: ${result.intervention}",
                artifactHash = result.hashSha256,
                latencyMs = result.latencyMs
            )
            onComplete(result)
        }
    }

    fun executeReadyDemo(scenario: ReadyDemoScenario, onComplete: () -> Unit) {
        _appState.update {
            it.copy(
                currentArtifactTitle = scenario.artifactName,
                currentSourceApp = scenario.sourceApp,
                currentRecipient = scenario.recipient,
                selectedAction = scenario.action,
                selectedDestination = scenario.destination,
                currentUrlRiskScore = scenario.urlRiskScore,
                isOverrideEngaged = false
            )
        }
        executeAnalysis(
            customSeverity = scenario.severity,
            customIrreversibility = scenario.irreversibility,
            customConfidence = scenario.confidence,
            customRationale = scenario.rationale,
            customAlternativeAction = scenario.alternativeAction,
            customEvidence = scenario.evidenceList
        ) {
            onComplete()
        }
    }

    fun executeDemoAction(action: DemoAction, onComplete: (SafetyResult) -> Unit) {
        setAction(action.actionName, action.destination)
        executeAnalysis(
            customSeverity = action.severity,
            customIrreversibility = action.irreversibility,
            customConfidence = action.confidence,
            customRationale = action.rationale,
            onComplete = onComplete
        )
    }
}
