package com.contextguard.app.core.perception

import android.graphics.Rect
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PerceptionUnitTests {

    // =========================================================================
    // 1. PII Regex & Pattern Unit Tests
    // =========================================================================

    @Test
    fun testEmailDetection() {
        val sampleText = "Please send statement to john.doe@secure-bank.com or admin@contextguard.io"
        val findings = PiiDetector.detectPii(sampleText)

        val emailFindings = findings.filter { it.type == PiiType.EMAIL_ADDRESS }
        assertEquals(2, emailFindings.size)
        assertTrue(emailFindings.any { it.rawValue == "john.doe@secure-bank.com" })
        assertTrue(emailFindings.any { it.rawValue == "admin@contextguard.io" })
        assertEquals("j***e@secure-bank.com", emailFindings.first { it.rawValue == "john.doe@secure-bank.com" }.maskedValue)
    }

    @Test
    fun testPhoneNumberDetection() {
        val sampleText = "Contact support at +91 9876543210 or US line +1-202-555-0173."
        val findings = PiiDetector.detectPii(sampleText)

        val phoneFindings = findings.filter { it.type == PiiType.PHONE_NUMBER }
        assertTrue("Should detect at least one phone number", phoneFindings.isNotEmpty())
        assertTrue(phoneFindings.any { it.rawValue.contains("9876543210") })
    }

    @Test
    fun testOtpDetectionWithContext() {
        val prefixText = "Your One-Time Password is 849201. Valid for 10 minutes."
        val prefixFindings = PiiDetector.detectPii(prefixText).filter { it.type == PiiType.OTP_CODE }
        assertEquals(1, prefixFindings.size)
        assertEquals("849201", prefixFindings[0].rawValue)
        assertEquals("******", prefixFindings[0].maskedValue)

        val suffixText = "593021 is your verification code. Never share with bank staff."
        val suffixFindings = PiiDetector.detectPii(suffixText).filter { it.type == PiiType.OTP_CODE }
        assertEquals(1, suffixFindings.size)
        assertEquals("593021", suffixFindings[0].rawValue)
    }

    @Test
    fun testCardDetectionWithLuhnAlgorithm() {
        // Valid Luhn test card: 4532 0150 1234 5671 (sum == 50 % 10 == 0)
        val validCardDigits = "4532015012345671"
        assertTrue("Luhn check should succeed for test card", PiiDetector.validateLuhn(validCardDigits))

        val invalidLuhnCard = "4532015012345679"
        assertFalse("Luhn check should fail for invalid card", PiiDetector.validateLuhn(invalidLuhnCard))

        val text = "Card Payment: 4532 0150 1234 5671 Exp: 12/28"
        val findings = PiiDetector.detectPii(text)
        val cardFindings = findings.filter { it.type == PiiType.PAYMENT_CARD }
        assertEquals(1, cardFindings.size)
        assertEquals("PII_CARD_LUHN_VERIFIED", cardFindings[0].patternId)
        assertEquals("****-****-****-5671", cardFindings[0].maskedValue)
    }

    @Test
    fun testAadhaarDetection() {
        val sampleText = "Government ID - Aadhaar: 9876 5432 1098"
        val findings = PiiDetector.detectPii(sampleText).filter { it.type == PiiType.AADHAAR_NUMBER }

        assertEquals(1, findings.size)
        assertEquals("9876 5432 1098", findings[0].rawValue)
        assertEquals("XXXX-XXXX-1098", findings[0].maskedValue)
        assertEquals(0.88f, findings[0].confidence, 0.01f)
    }

    @Test
    fun testBankAccountDetection() {
        val sampleText = "National Reserve Bank - Account Number: 409289123456 Balance: $14,000"
        val findings = PiiDetector.detectPii(sampleText).filter { it.type == PiiType.BANK_ACCOUNT }

        assertEquals(1, findings.size)
        assertEquals("409289123456", findings[0].rawValue)
        assertEquals("****3456", findings[0].maskedValue)
    }

    @Test
    fun testUrlCandidateExtraction() {
        val sampleText = "Restore account access immediately at http://secure-online-login.xyz/verify?id=99"
        val findings = PiiDetector.detectPii(sampleText).filter { it.type == PiiType.URL_STRING }

        assertEquals(1, findings.size)
        assertEquals("http://secure-online-login.xyz/verify?id=99", findings[0].rawValue)
    }

    @Test
    fun testDateDetection() {
        val sampleText = "Statement Date: 15/10/2026 for billing cycle ending 2026-10-06"
        val findings = PiiDetector.detectPii(sampleText).filter { it.type == PiiType.DATE_STRING }

        assertTrue(findings.size >= 2)
        assertTrue(findings.any { it.rawValue == "15/10/2026" })
        assertTrue(findings.any { it.rawValue == "2026-10-06" })
    }

    // =========================================================================
    // 2. OCR Result Parser & Structural Tests
    // =========================================================================

    @Test
    fun testTextBlockResultStructure() {
        val element = TextElementResult(text = "NATIONAL", boundingBox = null)
        val line = TextLineResult(text = "NATIONAL RESERVE BANK", boundingBox = null, elements = listOf(element))
        val block = TextBlockResult(text = "NATIONAL RESERVE BANK", boundingBox = null, lines = listOf(line), pageIndex = 0)

        assertEquals("NATIONAL RESERVE BANK", block.text)
        assertEquals(1, block.lines.size)
        assertEquals(1, block.lines[0].elements.size)
        assertEquals("NATIONAL", block.lines[0].elements[0].text)
    }

    // =========================================================================
    // 3. Malformed and Edge Case Tests
    // =========================================================================

    @Test
    fun testEmptyAndMalformedTextHandling() {
        val emptyFindings = PiiDetector.detectPii("")
        assertTrue(emptyFindings.isEmpty())

        val noiseText = "@@#$%%^&*()_+=-~`{}[]|:;<>?,./"
        val noiseFindings = PiiDetector.detectPii(noiseText)
        assertTrue("Noise text should not crash parser", noiseFindings.isEmpty())
    }

    // =========================================================================
    // 4. PDF Perception & Bounds Tests
    // =========================================================================

    @Test
    fun testPdfPerceptionConstraints() {
        assertEquals(3, PdfPerceptionRenderer.MAX_PDF_PAGES)
        assertEquals(1536, PdfPerceptionRenderer.PDF_RENDER_MAX_DIMENSION)

        // Non-existent PDF file handling
        val nonExistentFile = File("non_existent_document_12345.pdf")
        runBlocking {
            val result = PdfPerceptionRenderer.renderPdfPages(nonExistentFile)
            assertTrue(result.isFailure)
        }
    }

    // =========================================================================
    // 5. Redaction Region Tests
    // =========================================================================

    @Test
    fun testRedactionRegionModel() {
        val dummyBox = Rect(10, 20, 100, 200)
        val region = RedactionRegion(
            id = "pii_0_1",
            boundingBox = dummyBox,
            type = "PAYMENT_CARD",
            label = "****-****-****-1234",
            pageIndex = 0
        )

        assertEquals("pii_0_1", region.id)
        assertEquals("PAYMENT_CARD", region.type)
        assertEquals("****-****-****-1234", region.label)
        assertEquals(0, region.pageIndex)
    }

    @Test
    fun testLocalPerceptionResultProperties() {
        val result = LocalPerceptionResult(
            ocrText = "Sample Bank Statement",
            blocks = emptyList(),
            piiFindings = listOf(
                PiiFinding(PiiType.BANK_ACCOUNT, "PAT_1", "1234567890", "****7890", 0.9f)
            ),
            faces = listOf(
                FaceFinding(count = 1, boundingBox = null)
            ),
            urlCandidates = emptyList(),
            redactionRegions = emptyList(),
            processingTimeMs = 45L,
            errors = emptyList(),
            pageCount = 1
        )

        assertTrue(result.hasSensitiveFindings)
        assertEquals(1, result.faceCount)
        assertEquals(45L, result.processingTimeMs)
    }
}
