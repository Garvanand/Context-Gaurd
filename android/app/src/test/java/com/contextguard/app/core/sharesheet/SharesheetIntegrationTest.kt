package com.contextguard.app.core.sharesheet

import com.contextguard.app.core.state.UiState
import com.contextguard.app.ui.viewmodel.InterventionType
import com.contextguard.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharesheetIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUrlPatternExtractionFromSharesheetText() {
        val phishSharedText = "Urgent: Complete your KYC here http://kyc-update-sbi-portal-verify.support-desk91.net/auth"
        val detectedPhish = SharesheetPayloadResolver.extractUrl(phishSharedText)
        assertEquals("http://kyc-update-sbi-portal-verify.support-desk91.net/auth", detectedPhish)

        val cleanSharedText = "Check the docs at https://developer.android.com/guide/components/intents-filters"
        val detectedClean = SharesheetPayloadResolver.extractUrl(cleanSharedText)
        assertEquals("https://developer.android.com/guide/components/intents-filters", detectedClean)

        val plainNote = "Quarterly bank audit completed. No external links."
        val detectedNone = SharesheetPayloadResolver.extractUrl(plainNote)
        assertNull(detectedNone)
    }

    @Test
    fun testSharePayloadModelHierarchies() {
        // Image Payload
        val imgPayload = SharePayload.ImagePayload(
            bitmap = null,
            uri = null,
            title = "statement_scan.jpg",
            mimeType = "image/jpeg",
            sourceApp = "Gallery / Photos"
        )
        assertEquals("statement_scan.jpg", imgPayload.title)
        assertEquals("Gallery / Photos", imgPayload.sourceApp)

        // PDF Payload
        val pdfPayload = SharePayload.PdfPayload(
            pageBitmaps = emptyList(),
            uri = null,
            title = "tax_return_2025.pdf",
            sourceApp = "Files / Documents",
            totalPages = 3
        )
        assertEquals(3, pdfPayload.totalPages)
        assertEquals("tax_return_2025.pdf", pdfPayload.title)

        // Multiple Attachments Payload
        val multiPayload = SharePayload.MultipleAttachmentsPayload(
            primaryPayload = imgPayload,
            totalCount = 2,
            warningMessage = "Received 2 attachments. ContextGuard is analyzing primary attachment."
        )
        assertEquals(2, multiPayload.totalCount)
        assertEquals(imgPayload, multiPayload.primaryPayload)

        // Error Payload
        val errPayload = SharePayload.ErrorPayload(
            reason = "Inaccessible content URI: SecurityException",
            recoverable = true,
            sourceApp = "External App"
        )
        assertTrue(errPayload.recoverable)
        assertEquals("Inaccessible content URI: SecurityException", errPayload.reason)

        // Empty / Cancelled Payload
        val emptyPayload = SharePayload.EmptyOrCancelled
        assertNotNull(emptyPayload)
    }

    @Test
    fun testSharesheetTextPayloadIngestionInViewModel() = runTest {
        val viewModel = MainViewModel()

        val sharedUrlText = "Security alert from browser: http://kyc-update-sbi-portal-verify.support-desk91.net/auth"
        val detectedUrl = SharesheetPayloadResolver.extractUrl(sharedUrlText)
        assertNotNull(detectedUrl)

        val textPayload = SharePayload.TextPayload(
            text = sharedUrlText,
            detectedUrl = detectedUrl,
            title = "Browser_Share.link",
            sourceApp = "Browser / Web Client"
        )

        // Ingest into ViewModel
        viewModel.processSharesheetPayload(textPayload)
        testScheduler.advanceUntilIdle()

        val state = viewModel.appState.value
        assertEquals("Browser_Share.link", state.currentArtifactTitle)
        assertEquals(detectedUrl, state.selectedDestination)
        assertEquals("OPEN", state.selectedAction)
        assertEquals("Browser / Web Client", state.currentSourceApp)
        assertEquals("analyze", viewModel.pendingNavigation.value)

        // Consume navigation
        viewModel.consumeNavigation()
        assertNull(viewModel.pendingNavigation.value)
    }

    @Test
    fun testSharesheetMultipleAttachmentsIngestionInViewModel() = runTest {
        val viewModel = MainViewModel()

        val primaryItem = SharePayload.ImagePayload(
            bitmap = null,
            uri = null,
            title = "first_evidence.png",
            mimeType = "image/png",
            sourceApp = "Gallery / Photos"
        )

        val multiPayload = SharePayload.MultipleAttachmentsPayload(
            primaryPayload = primaryItem,
            totalCount = 3,
            warningMessage = "Received 3 attachments. ContextGuard analyzed first_evidence.png"
        )

        viewModel.processSharesheetPayload(multiPayload)
        testScheduler.advanceUntilIdle()

        val state = viewModel.appState.value
        assertEquals("first_evidence.png", state.currentArtifactTitle)
        assertEquals("Gallery / Photos", state.currentSourceApp)
        assertEquals("analyze", viewModel.pendingNavigation.value)
    }

    @Test
    fun testSharesheetErrorPayloadDoesNotCrashViewModel() = runTest {
        val viewModel = MainViewModel()

        val errPayload = SharePayload.ErrorPayload(
            reason = "Permission denied reading content URI",
            recoverable = true,
            sourceApp = "External App"
        )

        // Processing error payload should update error state gracefully without crash
        viewModel.processSharesheetPayload(errPayload)
        testScheduler.advanceUntilIdle()

        val analysisState = viewModel.analysisState.value
        assertTrue("Analysis state should transition to UiState.Error", analysisState is UiState.Error)
        val errorMsg = (analysisState as UiState.Error).message
        assertTrue(errorMsg.contains("Permission denied"))
    }

    @Test
    fun testSharesheetActionConditionedIdenticalArtifactTriadDemo() = runTest {
        val viewModel = MainViewModel()

        // 1. Ingest Synthetic Bank Statement Artifact from Gallery
        val artifactTitle = "synthetic_bank_statement.png"
        val sourceApp = "Gallery / Photos"
        val sharedText = "Account Number: 4532 0150 1234 5671\nIFSC: HDFC0000128\nAvailable Balance: INR 1,48,290.40"

        val payload = SharePayload.TextPayload(
            text = sharedText,
            detectedUrl = null,
            title = artifactTitle,
            sourceApp = sourceApp
        )

        // Process incoming share payload
        viewModel.processSharesheetPayload(payload)
        testScheduler.advanceUntilIdle()

        val stateAfterShare = viewModel.appState.value
        assertEquals(artifactTitle, stateAfterShare.currentArtifactTitle)
        assertEquals(sourceApp, stateAfterShare.currentSourceApp)
        assertEquals("analyze", viewModel.pendingNavigation.value)
        viewModel.consumeNavigation()

        // =========================================================================
        // ITERATION 1: Select SAVE -> ACT (Safe local storage)
        // =========================================================================
        viewModel.setActionChip("SAVE")
        viewModel.setContext(
            recipient = "Self / Personal Vault",
            destination = "Personal Encrypted Drive",
            sourceApp = sourceApp
        )
        var result1Intervention: InterventionType? = null
        var result1Title: String? = null
        var result1Action: String? = null
        var result1Risk: Float? = null

        viewModel.executeAnalysis { result ->
            result1Title = result.artifactTitle
            result1Action = result.intendedAction
            result1Intervention = result.intervention
            result1Risk = result.riskScore
        }
        testScheduler.advanceUntilIdle()

        assertEquals(artifactTitle, result1Title)
        assertEquals("SAVE", result1Action)
        assertEquals(InterventionType.ACT, result1Intervention)
        assertTrue("Expected low risk for private save", (result1Risk ?: 1.0f) < 0.35f)

        // =========================================================================
        // ITERATION 2: Same Artifact -> Select SEND + unknown recipient -> WARN or ASK
        // =========================================================================
        viewModel.setActionChip("SEND")
        viewModel.setContext(
            recipient = "Unverified Telegram Contact",
            destination = "Unverified Telegram Contact",
            sourceApp = sourceApp
        )
        var result2Intervention: InterventionType? = null
        var result2Title: String? = null
        var result2Action: String? = null
        var result2Risk: Float? = null

        viewModel.executeAnalysis { result ->
            // Invariant: The artifact must remain strictly the same
            result2Title = result.artifactTitle
            result2Action = result.intendedAction
            result2Intervention = result.intervention
            result2Risk = result.riskScore
        }
        testScheduler.advanceUntilIdle()

        assertEquals(artifactTitle, result2Title)
        assertEquals("SEND", result2Action)
        assertTrue(
            "Expected WARN or ASK for bank statement sent to unverified contact",
            result2Intervention == InterventionType.WARN || result2Intervention == InterventionType.ASK
        )
        assertTrue((result2Risk ?: 0f) >= 0.35f)

        // =========================================================================
        // ITERATION 3: Same Artifact -> Select POST to Public Feed -> STOP
        // =========================================================================
        viewModel.setActionChip("POST")
        viewModel.setContext(
            recipient = "Public Twitter/X Feed",
            destination = "Public Twitter/X Feed",
            sourceApp = sourceApp
        )
        var result3Intervention: InterventionType? = null
        var result3Title: String? = null
        var result3Action: String? = null
        var result3Risk: Float? = null

        viewModel.executeAnalysis { result ->
            // Invariant: The artifact must remain strictly the same
            result3Title = result.artifactTitle
            result3Action = result.intendedAction
            result3Intervention = result.intervention
            result3Risk = result.riskScore
        }
        testScheduler.advanceUntilIdle()

        assertEquals(artifactTitle, result3Title)
        assertEquals("POST", result3Action)
        assertEquals(InterventionType.STOP, result3Intervention)
        assertTrue("Expected high risk STOP for public post of bank statement", (result3Risk ?: 0f) >= 0.65f)
    }
}
