package com.contextguard.app.core.privacy

import android.graphics.Bitmap
import com.contextguard.app.core.network.AnalysisRequestPayload
import com.contextguard.app.core.network.BackendConfig
import com.contextguard.app.core.network.ContextGuardApiClient
import com.contextguard.app.core.perception.LocalPerceptionResult
import com.contextguard.app.core.perception.MlKitPerceptionEngine
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.SafetyResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Output of the PrivacyPipeline.
 */
data class PrivacyPipelineResult(
    val safetyResult: SafetyResult,
    val redactionResult: RedactionResult,
    val perceptionResult: LocalPerceptionResult,
    val auditEntry: NetworkAuditEntry,
    val networkMode: NetworkMode
)

/**
 * End-to-End ContextGuard Privacy Pipeline:
 *
 * Raw artifact
 *    ↓
 * On-device perception
 *    ↓
 * Sensitive-region detection
 *    ↓
 * Redaction
 *    ↓
 * Privacy decision
 *    ↓
 * Redacted artifact only
 *    ↓
 * Local backend
 *    ↓
 * Structured result
 */
class PrivacyPipeline(
    private val perceptionEngine: MlKitPerceptionEngine = MlKitPerceptionEngine(),
    private val redactionEngine: RedactionEngine = RedactionEngine(),
    private val apiClient: ContextGuardApiClient = ContextGuardApiClient()
) {

    /**
     * Executes the privacy pipeline for an image/screenshot artifact.
     */
    suspend fun processImage(
        rawBitmap: Bitmap,
        selectedAction: String,
        destination: String,
        sourceApp: String = "Gallery",
        recipient: String = "Unknown",
        networkMode: NetworkMode = NetworkMode.LOCAL_BACKEND,
        redactionStyle: RedactionStyle = RedactionStyle.BLACKOUT,
        backendConfig: BackendConfig = BackendConfig()
    ): PrivacyPipelineResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // 1. On-device perception (ML Kit OCR & Face Detection)
        val perceptionResult = perceptionEngine.analyzeImage(rawBitmap)

        // 2 & 3. Sensitive-region detection & Redaction (in volatile RAM)
        val redactionResult = redactionEngine.redact(
            bitmap = rawBitmap,
            ocrText = perceptionResult.ocrText,
            piiFindings = perceptionResult.piiFindings,
            faceFindings = perceptionResult.faces,
            style = redactionStyle
        )

        // 4. Privacy Decision & Routing
        executePrivacyRouting(
            redactionResult = redactionResult,
            perceptionResult = perceptionResult,
            selectedAction = selectedAction,
            destination = destination,
            sourceApp = sourceApp,
            recipient = recipient,
            networkMode = networkMode,
            backendConfig = backendConfig,
            startTime = startTime,
            artifactType = "IMAGE"
        )
    }

    /**
     * Executes the privacy pipeline for a text artifact.
     */
    suspend fun processText(
        rawText: String,
        selectedAction: String,
        destination: String,
        sourceApp: String = "Clipboard",
        recipient: String = "Unknown",
        networkMode: NetworkMode = NetworkMode.LOCAL_BACKEND,
        backendConfig: BackendConfig = BackendConfig()
    ): PrivacyPipelineResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // 1. On-device perception
        val perceptionResult = perceptionEngine.analyzeText(rawText)

        // 2 & 3. Sensitive-region detection & Redaction
        val redactionResult = redactionEngine.redact(
            bitmap = null,
            ocrText = rawText,
            piiFindings = perceptionResult.piiFindings,
            faceFindings = emptyList()
        )

        // 4. Privacy Decision & Routing
        executePrivacyRouting(
            redactionResult = redactionResult,
            perceptionResult = perceptionResult,
            selectedAction = selectedAction,
            destination = destination,
            sourceApp = sourceApp,
            recipient = recipient,
            networkMode = networkMode,
            backendConfig = backendConfig,
            startTime = startTime,
            artifactType = "TEXT"
        )
    }

    /**
     * Executes the privacy pipeline for a PDF document.
     */
    suspend fun processPdf(
        pdfFile: File,
        selectedAction: String,
        destination: String,
        sourceApp: String = "FilesApp",
        recipient: String = "Unknown",
        networkMode: NetworkMode = NetworkMode.LOCAL_BACKEND,
        backendConfig: BackendConfig = BackendConfig()
    ): PrivacyPipelineResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // 1. On-device perception (bounded first 3 pages)
        val perceptionResult = perceptionEngine.analyzePdf(pdfFile)

        // 2 & 3. Sensitive-region detection & Redaction
        val redactionResult = redactionEngine.redact(
            bitmap = null,
            ocrText = perceptionResult.ocrText,
            piiFindings = perceptionResult.piiFindings,
            faceFindings = emptyList()
        )

        // 4. Privacy Decision & Routing
        executePrivacyRouting(
            redactionResult = redactionResult,
            perceptionResult = perceptionResult,
            selectedAction = selectedAction,
            destination = destination,
            sourceApp = sourceApp,
            recipient = recipient,
            networkMode = networkMode,
            backendConfig = backendConfig,
            startTime = startTime,
            artifactType = "DOCUMENT"
        )
    }

    /**
     * Enforces the privacy decision, dispatches to network only if permitted,
     * guarantees raw artifact is NEVER sent, and records metadata audit log.
     */
    private fun executePrivacyRouting(
        redactionResult: RedactionResult,
        perceptionResult: LocalPerceptionResult,
        selectedAction: String,
        destination: String,
        sourceApp: String,
        recipient: String,
        networkMode: NetworkMode,
        backendConfig: BackendConfig,
        startTime: Long,
        artifactType: String
    ): PrivacyPipelineResult {
        return when (networkMode) {
            NetworkMode.OFFLINE -> {
                // Airplane-safe: Application layer completely disables network sockets.
                val latency = System.currentTimeMillis() - startTime
                val localSafetyResult = evaluateOfflinePolicy(
                    redactionResult = redactionResult,
                    perceptionResult = perceptionResult,
                    selectedAction = selectedAction,
                    destination = destination,
                    sourceApp = sourceApp,
                    latencyMs = latency
                )

                val auditEntry = NetworkAuditLogger.record(
                    endpointCategory = "OFFLINE_LOCAL_EVALUATION",
                    payloadType = "ZERO_NETWORK_BYTES",
                    payloadSizeBytes = 0L,
                    isRedacted = true,
                    responseStatus = 0,
                    latencyMs = latency,
                    artifactHashSha256 = redactionResult.sha256Original
                )

                PrivacyPipelineResult(
                    safetyResult = localSafetyResult,
                    redactionResult = redactionResult,
                    perceptionResult = perceptionResult,
                    auditEntry = auditEntry,
                    networkMode = networkMode
                )
            }

            NetworkMode.LOCAL_BACKEND,
            NetworkMode.RESTRICTED_EVALUATION -> {
                // Send REDACTED artifact only to local backend.
                // Raw artifact is strictly EXCLUDED.
                val redactedBase64 = redactionResult.redactedBitmap?.let { bmp ->
                    try {
                        val stream = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        java.util.Base64.getEncoder().encodeToString(stream.toByteArray())
                    } catch (e: Exception) {
                        null
                    }
                }

                val payload = AnalysisRequestPayload(
                    artifactBase64 = redactedBase64,
                    artifactType = artifactType,
                    ocrTextRedacted = redactionResult.redactedText,
                    detectedFaces = perceptionResult.faces.size,
                    detectedPiiTypes = perceptionResult.piiFindings.map { it.type.name }.distinct(),
                    redactionMetadata = mapOf(
                        "regions_count" to redactionResult.regionsRedacted.size,
                        "style" to redactionResult.style.name,
                        "sha256_redacted" to redactionResult.sha256Redacted
                    ),
                    selectedAction = selectedAction,
                    destination = destination,
                    recipient = recipient,
                    sourceApp = sourceApp,
                    networkState = networkMode.name
                )

                val estimatedPayloadSize = (redactedBase64?.length?.toLong() ?: 0L) +
                        (redactionResult.redactedText.length.toLong()) + 512L

                val (safetyResult, responseCode) = apiClient.analyze(
                    payload = payload,
                    baseUrl = backendConfig.baseUrl,
                    timeoutMs = backendConfig.timeoutSeconds * 1000
                )

                val latency = System.currentTimeMillis() - startTime

                val auditEntry = NetworkAuditLogger.record(
                    endpointCategory = "LOCAL_BACKEND_REDACTED",
                    payloadType = if (redactedBase64 != null) "REDACTED_IMAGE_AND_TEXT" else "REDACTED_TEXT_ONLY",
                    payloadSizeBytes = estimatedPayloadSize,
                    isRedacted = true,
                    responseStatus = responseCode,
                    latencyMs = latency,
                    artifactHashSha256 = redactionResult.sha256Original
                )

                PrivacyPipelineResult(
                    safetyResult = safetyResult,
                    redactionResult = redactionResult,
                    perceptionResult = perceptionResult,
                    auditEntry = auditEntry,
                    networkMode = networkMode
                )
            }
        }
    }

    /**
     * Deterministic on-device policy engine for OFFLINE mode.
     * Evaluates rho = s * (1 + lambda * r) directly on device.
     */
    fun evaluateOfflinePolicy(
        redactionResult: RedactionResult,
        perceptionResult: LocalPerceptionResult,
        selectedAction: String,
        destination: String,
        sourceApp: String,
        latencyMs: Long
    ): SafetyResult {
        val destLower = destination.lowerCase()
        val actionLower = selectedAction.lowerCase()

        val isPublic = destLower.contains("public") || destLower.contains("twitter") ||
                destLower.contains("social") || destLower.contains("feed")
        val isUnverified = destLower.contains("unknown") || destLower.contains("unverified") ||
                destLower.contains("telegram") || destLower.contains("stranger")
        val hasFinancialPii = perceptionResult.piiFindings.any {
            it.type.name.contains("CARD") || it.type.name.contains("ACCOUNT") || it.type.name.contains("OTP")
        }
        val hasFaces = perceptionResult.faces.isNotEmpty()

        val s = when {
            hasFinancialPii && isPublic -> 0.90f
            hasFinancialPii && isUnverified -> 0.45f
            hasFinancialPii -> 0.20f
            hasFaces && isPublic -> 0.60f
            isPublic -> 0.40f
            else -> 0.05f
        }

        val r = when {
            isPublic -> 1.00f
            isUnverified -> 0.50f
            actionLower.contains("save") || destLower.contains("vault") || destLower.contains("personal") -> 0.00f
            else -> 0.25f
        }

        val c = if (perceptionResult.ocrText.isEmpty()) 0.65f else 0.95f
        val lambdaVal = 0.75f
        val rho = s * (1.0f + lambdaVal * r)

        val intervention = when {
            rho >= 0.65f -> InterventionType.STOP
            rho >= 0.35f && c < 0.70f -> InterventionType.ASK
            rho >= 0.35f -> InterventionType.WARN
            else -> InterventionType.ACT
        }

        val rationale = when (intervention) {
            InterventionType.STOP -> "Irreversible disclosure hazard detected on public channel (Offline Policy Engine)."
            InterventionType.WARN -> "Elevated risk with unverified destination. Proceed with caution."
            InterventionType.ASK -> "High uncertainty in recipient identity. Manual verification required."
            InterventionType.ACT -> "Safe action bounded within encrypted/private perimeter."
        }

        return SafetyResult(
            intervention = intervention,
            riskScore = rho,
            severity = s,
            irreversibility = r,
            confidence = c,
            evidence = listOf(
                "Offline Edge Policy: Calculated entirely on-device (Zero Network Traffic)",
                "Redacted regions: ${redactionResult.regionsRedacted.size} sensitive entities masked",
                "Action evaluated: $selectedAction -> $destination",
                "Calculated rho = $s * (1 + 0.75 * $r) = ${String.format("%.3f", rho)}"
            ),
            rationale = rationale,
            artifactTitle = sourceApp,
            intendedAction = selectedAction,
            destination = destination,
            latencyMs = latencyMs,
            hashSha256 = redactionResult.sha256Original
        )
    }

    private fun String.lowerCase(): String = this.lowercase()
}
