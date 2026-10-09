package com.contextguard.app

import com.contextguard.app.core.engine.ScamMessageClassifier
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * On-Device Scam Message Classifier Acceptance & Parity Test.
 *
 * Verifies genuine offline ML text classification on Android:
 * - Parity against Python TF-IDF + Calibrated Logistic Regression.
 * - Sub-millisecond execution latency.
 * - Robust handling of benign sensitive messages (OTP/banking statements).
 */
class ScamMessageInferenceTest {

    private lateinit var classifier: ScamMessageClassifier
    private lateinit var goldenCorpus: JSONArray

    @Before
    fun setUp() {
        val modelFile = File("src/main/assets/models/message_model_portable.json")
        assertTrue("Message model asset must exist at ${modelFile.absolutePath}", modelFile.exists())
        classifier = ScamMessageClassifier.fromJsonString(modelFile.readText())

        val corpusFile = File("src/main/assets/models/message_golden_corpus.json")
        assertTrue("Message golden corpus asset must exist at ${corpusFile.absolutePath}", corpusFile.exists())
        goldenCorpus = JSONArray(corpusFile.readText())
    }

    @Test
    fun testGoldenCorpusParity() {
        var matches = 0
        val t0 = System.nanoTime()

        for (i in 0 until goldenCorpus.length()) {
            val sample = goldenCorpus.getJSONObject(i)
            val text = sample.getString("text")
            val pyProb = sample.getDouble("calibrated_probability").toFloat()
            val pyIsPhish = sample.getBoolean("predicted_is_phishing")

            val ktPred = classifier.predict(text)

            // Probability parity within tolerance
            val diff = abs(pyProb - ktPred.probability)
            assertTrue(
                "Probability mismatch for sample '$text': Python=$pyProb, Kotlin=${ktPred.probability}, diff=$diff",
                diff <= 0.05f
            )

            // Decision parity
            assertEquals(
                "Decision mismatch for sample '$text'",
                pyIsPhish,
                ktPred.isPhishing
            )
            matches++
        }

        val totalTimeMs = (System.nanoTime() - t0) / 1_000_000.0
        val avgLatencyMs = totalTimeMs / goldenCorpus.length()
        println("Evaluated ${goldenCorpus.length()} messages with 100% parity in ${totalTimeMs}ms (avg ${avgLatencyMs}ms per message).")
        assertEquals(goldenCorpus.length(), matches)
    }

    @Test
    fun testBenignMessagesWithSensitiveKeywords() {
        // Legitimate bank OTP
        val otpMsg = "Your one-time password (OTP) for transaction at Amazon is 582910. Do not share with anyone."
        val otpPred = classifier.predict(otpMsg)
        assertTrue("Legitimate OTP notice must not be classified as phishing", !otpPred.isPhishing)
        assertTrue("Legitimate OTP probability should be low", otpPred.probability < 0.35f)

        // Legitimate bank account statement notification
        val bankMsg = "Your HDFC Bank account statement for October is now ready for download."
        val bankPred = classifier.predict(bankMsg)
        assertTrue("Legitimate account statement notice must not be classified as phishing", !bankPred.isPhishing)
        assertTrue("Legitimate bank statement probability should be low", bankPred.probability < 0.35f)
    }

    @Test
    fun testPhishingLuresDetection() {
        val phishKyc = "URGENT: Your bank account is locked due to KYC expiry. Update immediately at http://bit.ly/sbi-kyc-auth"
        val kycPred = classifier.predict(phishKyc)
        assertTrue("KYC lure message must be flagged as phishing", kycPred.isPhishing)
        assertTrue("KYC lure probability must be high", kycPred.probability >= 0.70f)
        assertTrue("Matched tokens should contain kyc or urgent tokens", kycPred.matchedTokens.isNotEmpty())

        val phishPrize = "Congratulations! You won a $1,000 Walmart gift card. Claim your prize here: http://claim-rewards.top/win"
        val prizePred = classifier.predict(phishPrize)
        assertTrue("Prize lure message must be flagged as phishing", prizePred.isPhishing)
        assertTrue("Prize lure probability must be high", prizePred.probability >= 0.70f)
    }

    @Test
    fun testInferenceLatencyUnderOneMillisecond() {
        val testMsg = "Dear customer, your electricity bill of Rs 1,420 is overdue. Pay immediately to avoid disconnection."
        // Warmup
        repeat(10) { classifier.predict(testMsg) }

        val iterations = 100
        val t0 = System.nanoTime()
        repeat(iterations) {
            classifier.predict(testMsg)
        }
        val elapsedMs = (System.nanoTime() - t0) / 1_000_000.0
        val latencyPerSample = elapsedMs / iterations

        println("Benchmarked $iterations inferences: ${latencyPerSample}ms per sample.")
        assertTrue("On-device message inference must execute in < 1.0ms on CPU", latencyPerSample < 1.0)
    }
}
