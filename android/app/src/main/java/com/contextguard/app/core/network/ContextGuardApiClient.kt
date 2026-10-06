package com.contextguard.app.core.network

import com.contextguard.app.core.logging.AppLogger
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.SafetyResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Payload sent to ContextGuard backend.
 * INVARIANT: Must NEVER contain unredacted raw artifacts or unmasked secrets.
 */
data class AnalysisRequestPayload(
    val artifactBase64: String?,
    val artifactType: String,
    val ocrTextRedacted: String?,
    val detectedFaces: Int,
    val detectedPiiTypes: List<String>,
    val redactionMetadata: Map<String, Any>,
    val selectedAction: String,
    val destination: String,
    val recipient: String,
    val sourceApp: String,
    val networkState: String
)

/**
 * Android client communicating with local ContextGuard FastAPI backend.
 * Enforces Model Failure Safety Rule: If backend is offline or errors, never emits ACT.
 */
open class ContextGuardApiClient {

    /**
     * Sends an action-conditioned risk evaluation request to the backend.
     */
    open fun analyze(
        payload: AnalysisRequestPayload,
        baseUrl: String,
        timeoutMs: Int = 10000
    ): Pair<SafetyResult, Int> {
        val endpointUrl = "$baseUrl/api/v1/analyze"
        var connection: HttpURLConnection? = null
        val startTime = System.currentTimeMillis()

        try {
            val url = URL(endpointUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.doOutput = true

            // Build JSON Body
            val jsonBody = JSONObject().apply {
                put("artifact", payload.artifactBase64 ?: JSONObject.NULL)
                put("artifact_type", payload.artifactType)
                put("ocr_text", payload.ocrTextRedacted ?: JSONObject.NULL)
                put("detected_faces", payload.detectedFaces)
                put("detected_pii", JSONArray(payload.detectedPiiTypes))

                val metaObj = JSONObject()
                payload.redactionMetadata.forEach { (k, v) -> metaObj.put(k, v) }
                put("redaction_metadata", metaObj)

                put("selected_action", payload.selectedAction)
                put("destination", payload.destination)
                put("recipient", payload.recipient)
                put("source_app", payload.sourceApp)
                put("network_state", payload.networkState)
            }

            // Write payload
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val statusCode = connection.responseCode
            val latency = System.currentTimeMillis() - startTime

            if (statusCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val responseJson = JSONObject(responseText)

                val interventionStr = responseJson.optString("intervention", "ASK")
                val intervention = try {
                    InterventionType.valueOf(interventionStr)
                } catch (e: Exception) {
                    InterventionType.ASK
                }

                val riskScore = responseJson.optDouble("risk_score", 0.5).toFloat()
                val severity = responseJson.optDouble("severity", 0.5).toFloat()
                val irreversibility = responseJson.optDouble("reversibility", 0.5).toFloat()
                val confidence = responseJson.optDouble("confidence", 0.5).toFloat()
                val reason = responseJson.optString("reason", "Action evaluated by ContextGuard policy engine.")

                val evidenceList = mutableListOf<String>()
                val evidenceArray = responseJson.optJSONArray("evidence")
                if (evidenceArray != null) {
                    for (i in 0 until evidenceArray.length()) {
                        val item = evidenceArray.optJSONObject(i)
                        if (item != null) {
                            evidenceList.add("${item.optString("type")}: ${item.optString("description")}")
                        }
                    }
                }

                val result = SafetyResult(
                    intervention = intervention,
                    riskScore = riskScore,
                    severity = severity,
                    irreversibility = irreversibility,
                    confidence = confidence,
                    evidence = if (evidenceList.isNotEmpty()) evidenceList else listOf("Evaluated via ContextGuard Backend"),
                    rationale = reason,
                    artifactTitle = payload.sourceApp,
                    intendedAction = payload.selectedAction,
                    destination = payload.destination,
                    latencyMs = latency
                )
                return Pair(result, statusCode)
            } else {
                // Non-200 response -> Model Failure Safety Rule applies
                AppLogger.w("Backend returned HTTP $statusCode")
                val fallback = createSafeFallback(
                    reason = "Backend error (HTTP $statusCode). Safety fallback engaged.",
                    payload = payload,
                    latencyMs = latency
                )
                return Pair(fallback, statusCode)
            }
        } catch (e: Exception) {
            // Connection exception -> Model Failure Safety Rule applies
            val latency = System.currentTimeMillis() - startTime
            AppLogger.e("Backend connection failed: ${e.message}")
            val fallback = createSafeFallback(
                reason = "Backend unreachable (${e.javaClass.simpleName}). Safe offline fallback engaged.",
                payload = payload,
                latencyMs = latency
            )
            return Pair(fallback, 0)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Model Failure Safety Rule: Never emit ACT on failure. Emits ASK with safe parameters.
     */
    fun createSafeFallback(
        reason: String,
        payload: AnalysisRequestPayload,
        latencyMs: Long
    ): SafetyResult {
        return SafetyResult(
            intervention = InterventionType.ASK,
            riskScore = 0.50f,
            severity = 0.50f,
            irreversibility = 0.50f,
            confidence = 0.10f,
            evidence = listOf(
                "System uncertainty: Model reasoning degraded",
                "Backend communication inactive or returned error",
                "Manual user confirmation mandated by Model Failure Safety Rule"
            ),
            rationale = reason,
            artifactTitle = payload.sourceApp,
            intendedAction = payload.selectedAction,
            destination = payload.destination,
            latencyMs = latencyMs
        )
    }
}
