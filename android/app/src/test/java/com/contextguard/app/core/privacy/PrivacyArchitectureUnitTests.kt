package com.contextguard.app.core.privacy

import com.contextguard.app.core.network.AnalysisRequestPayload
import com.contextguard.app.core.network.BackendConfig
import com.contextguard.app.core.network.ContextGuardApiClient
import com.contextguard.app.core.perception.MlKitPerceptionEngine
import com.contextguard.app.ui.viewmodel.AppState
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.MainViewModel
import com.contextguard.app.ui.viewmodel.SafetyResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verification test suite for ContextGuard Privacy Architecture.
 *
 * Verifies the 6 core privacy contracts:
 * 1. Offline mode performs zero backend calls.
 * 2. Redaction occurs before network request.
 * 3. Raw artifact is not included in backend request.
 * 4. Network log does not contain OCR content.
 * 5. STOP can still be overridden.
 * 6. Model failure does not become ACT.
 */
class PrivacyArchitectureUnitTests {

    private lateinit var testApiClient: SpyingApiClient
    private lateinit var privacyPipeline: PrivacyPipeline

    class SpyingApiClient : ContextGuardApiClient() {
        var callCount = 0
        var lastPayload: AnalysisRequestPayload? = null

        override fun analyze(
            payload: AnalysisRequestPayload,
            baseUrl: String,
            timeoutMs: Int
        ): Pair<SafetyResult, Int> {
            callCount++
            lastPayload = payload
            val mockResult = SafetyResult(
                intervention = InterventionType.WARN,
                riskScore = 0.55f,
                severity = 0.45f,
                irreversibility = 0.50f,
                confidence = 0.90f,
                evidence = listOf("Mocked Backend Verification"),
                rationale = "Backend verified redacted payload",
                artifactTitle = payload.sourceApp,
                intendedAction = payload.selectedAction,
                destination = payload.destination,
                latencyMs = 45L
            )
            return Pair(mockResult, 200)
        }
    }

    @Before
    fun setUp() {
        NetworkAuditLogger.clear()
        testApiClient = SpyingApiClient()
        privacyPipeline = PrivacyPipeline(
            perceptionEngine = MlKitPerceptionEngine(),
            redactionEngine = RedactionEngine(),
            apiClient = testApiClient
        )
    }

    // -------------------------------------------------------------------------
    // TEST 1: Offline mode performs zero backend calls
    // -------------------------------------------------------------------------
    @Test
    fun testOfflineModePerformsZeroBackendCalls() = runBlocking {
        val sampleText = "Transfer INR 50,000 to Account 987654321098. OTP is 849201."

        val result = privacyPipeline.processText(
            rawText = sampleText,
            selectedAction = "Save to Private Vault",
            destination = "Personal Encrypted Drive",
            networkMode = NetworkMode.OFFLINE
        )

        // Strict verification: Zero HTTP calls executed
        assertEquals("Offline mode must execute 0 backend network calls", 0, testApiClient.callCount)
        assertEquals("Network mode must remain OFFLINE", NetworkMode.OFFLINE, result.networkMode)
        assertEquals("Transmitted payload bytes must be 0 in offline mode", 0L, result.auditEntry.payloadSizeBytes)
        assertEquals("Offline endpoint must be local evaluation", "OFFLINE_LOCAL_EVALUATION", result.auditEntry.endpointCategory)
    }

    // -------------------------------------------------------------------------
    // TEST 2: Redaction occurs before network request
    // -------------------------------------------------------------------------
    @Test
    fun testRedactionOccursBeforeNetworkRequest() = runBlocking {
        val sensitiveText = "My confidential card number is 4532 0150 1234 5671 and phone is +91 9876543210."

        val result = privacyPipeline.processText(
            rawText = sensitiveText,
            selectedAction = "Send via Instant Messaging Chat",
            destination = "Unverified Telegram Contact",
            networkMode = NetworkMode.LOCAL_BACKEND
        )

        // Redaction result must be produced and populated before backend network request
        assertNotNull(result.redactionResult)
        assertTrue(
            "Redaction regions must be detected and masked before dispatch",
            result.redactionResult.regionsRedacted.isNotEmpty()
        )
        assertEquals("Backend call should have occurred", 1, testApiClient.callCount)

        // Verify the payload sent to backend was already sanitized
        val capturedPayload = testApiClient.lastPayload
        assertNotNull(capturedPayload)
        assertEquals(result.redactionResult.redactedText, capturedPayload!!.ocrTextRedacted)
    }

    // -------------------------------------------------------------------------
    // TEST 3: Raw artifact is not included in backend request
    // -------------------------------------------------------------------------
    @Test
    fun testRawArtifactIsNotIncludedInBackendRequest() = runBlocking {
        val rawSensitiveCard = "4532 0150 1234 5671"
        val rawSensitiveEmail = "alice.secret@enterprise.com"
        val rawSensitivePhone = "9876543210"
        val rawInput = "Payment Card: $rawSensitiveCard, Email: $rawSensitiveEmail, Phone: +91 $rawSensitivePhone"

        privacyPipeline.processText(
            rawText = rawInput,
            selectedAction = "Broadcast on Social Media",
            destination = "Public Twitter/X Feed",
            networkMode = NetworkMode.LOCAL_BACKEND
        )

        val capturedPayload = testApiClient.lastPayload
        assertNotNull("Payload must have been captured", capturedPayload)

        val transmittedText = capturedPayload!!.ocrTextRedacted ?: ""

        // INVARIANT: Raw plaintexts must NOT be present in transmitted payload
        assertFalse("Raw card number must NOT appear in backend request", transmittedText.contains(rawSensitiveCard))
        assertFalse("Raw email must NOT appear in backend request", transmittedText.contains(rawSensitiveEmail))
        assertFalse("Raw phone digits must NOT appear in backend request", transmittedText.contains(rawSensitivePhone))

        // Redacted tokens must be present
        assertTrue("Masked card token must be present", transmittedText.contains("****-****-****-"))
        assertTrue("Masked email must obscure username while preserving domain", transmittedText.contains("@enterprise.com") && transmittedText.contains("***"))
    }

    // -------------------------------------------------------------------------
    // TEST 4: Network log does not contain OCR content
    // -------------------------------------------------------------------------
    @Test
    fun testNetworkLogDoesNotContainOcrContent() = runBlocking {
        val secretOcrText = "SECRET BANK TRANSACTION #998822: Balance INR 1,500,000 for John Doe"

        privacyPipeline.processText(
            rawText = secretOcrText,
            selectedAction = "Save to Private Vault",
            destination = "Personal Vault",
            networkMode = NetworkMode.LOCAL_BACKEND
        )

        val logEntries = NetworkAuditLogger.getEntries()
        assertTrue("Audit log must record entry", logEntries.isNotEmpty())

        val entry = logEntries.first()

        // INVARIANT: Metadata only. Zero raw OCR text or secrets.
        assertFalse("Network log must NOT contain secret OCR string", entry.endpointCategory.contains("SECRET"))
        assertFalse("Network log must NOT contain secret OCR string", entry.payloadType.contains("SECRET"))
        assertFalse("Network log must NOT contain customer name or balance", entry.endpointCategory.contains("John Doe"))
        assertFalse("Network log must NOT contain customer name or balance", entry.payloadType.contains("1,500,000"))

        // Artifact hash must be a valid 64-character SHA-256 hex string
        assertEquals("Audit log hash must be 64-character SHA-256", 64, entry.artifactHashSha256.length)
        assertTrue("Audit log hash must be hexadecimal", entry.artifactHashSha256.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' })
    }

    // -------------------------------------------------------------------------
    // TEST 5: STOP can still be overridden
    // -------------------------------------------------------------------------
    @Test
    fun testStopCanStillBeOverridden() {
        val viewModel = MainViewModel()

        // 1. Initial state
        assertFalse("Initial override state must be false", viewModel.appState.value.isOverrideEngaged)

        // 2. Set STOP result where canOverride = true
        val stopResult = SafetyResult(
            intervention = InterventionType.STOP,
            riskScore = 1.57f,
            severity = 0.90f,
            irreversibility = 1.00f,
            confidence = 0.95f,
            evidence = listOf("Critical exposure hazard on public channel"),
            rationale = "Permanent public broadcast of financial PII causes catastrophic harm.",
            artifactTitle = "synthetic_bank_statement.png",
            intendedAction = "Broadcast on Social Media",
            destination = "Public Twitter/X Feed",
            canOverride = true
        )
        viewModel.setLastResultForTesting(stopResult)

        // Verify STOP intervention is active and canOverride is true
        val result = viewModel.appState.value.lastResult
        assertNotNull(result)
        assertEquals(InterventionType.STOP, result!!.intervention)
        assertTrue("STOP decision must allow user emergency override", result.canOverride)
        assertFalse("Override must NOT be active before explicit user engagement", viewModel.appState.value.isOverrideEngaged)

        // 3. User explicitly engages deliberate override
        viewModel.overrideStopIntervention()

        // Verify override is engaged
        assertTrue("Override must be active after deliberate user engagement", viewModel.appState.value.isOverrideEngaged)
    }

    // -------------------------------------------------------------------------
    // TEST 6: Model failure does not become ACT
    // -------------------------------------------------------------------------
    @Test
    fun testModelFailureDoesNotBecomeAct() {
        val client = ContextGuardApiClient()

        val mockPayload = AnalysisRequestPayload(
            artifactBase64 = null,
            artifactType = "TEXT",
            ocrTextRedacted = "Sample test text",
            detectedFaces = 0,
            detectedPiiTypes = emptyList(),
            redactionMetadata = emptyMap(),
            selectedAction = "Send",
            destination = "Remote Server",
            recipient = "Contact",
            sourceApp = "TestingApp",
            networkState = "OFFLINE"
        )

        // 1. Test client fallback creator
        val clientFallback = client.createSafeFallback(
            reason = "Connection timeout",
            payload = mockPayload,
            latencyMs = 15000L
        )

        assertEquals("Client failure fallback must NEVER emit ACT; must be ASK", InterventionType.ASK, clientFallback.intervention)
        assertTrue("Confidence must be degraded on failure", clientFallback.confidence <= 0.20f)

        // 2. Test analyze against completely invalid / unreachable server
        val (unreachableResult, statusCode) = client.analyze(
            payload = mockPayload,
            baseUrl = "http://127.0.0.1:59999", // Unreachable port
            timeoutMs = 500
        )

        assertEquals("Unreachable backend must return status 0", 0, statusCode)
        assertEquals("Model Failure Safety Rule: Unreachable server MUST degrade to ASK", InterventionType.ASK, unreachableResult.intervention)
    }
}
