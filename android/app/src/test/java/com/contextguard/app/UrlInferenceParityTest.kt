package com.contextguard.app

import com.contextguard.app.core.engine.UrlFeatureExtractor
import com.contextguard.app.core.engine.UrlTreeInferenceEngine
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * Deterministic Parity Acceptance Test.
 *
 * Verifies that on-device Kotlin tree inference achieves exact parity with Python XGBoost
 * across the entire shared Golden-Test Corpus (features, margins, calibrated probabilities, and decisions).
 */
class UrlInferenceParityTest {

    private lateinit var engine: UrlTreeInferenceEngine
    private lateinit var goldenCorpus: JSONArray

    @Before
    fun setUp() {
        val modelFile = File("src/main/assets/models/url_model_portable.json")
        assertTrue("Model asset file must exist at ${modelFile.absolutePath}", modelFile.exists())
        val modelJson = modelFile.readText()
        engine = UrlTreeInferenceEngine.fromJsonString(modelJson)

        val corpusFile = File("src/main/assets/models/url_golden_corpus.json")
        assertTrue("Golden corpus asset must exist at ${corpusFile.absolutePath}", corpusFile.exists())
        val corpusJson = corpusFile.readText()
        goldenCorpus = JSONArray(corpusJson)
    }

    @Test
    fun testGoldenCorpusNotEmpty() {
        assertTrue("Golden corpus must have at least 20 test URLs", goldenCorpus.length() >= 20)
    }

    @Test
    fun testFeatureVectorParity() {
        var totalFeaturesChecked = 0

        for (i in 0 until goldenCorpus.length()) {
            val sample = goldenCorpus.getJSONObject(i)
            val url = sample.getString("url")
            val pyFeatures = sample.getJSONArray("feature_vector")

            assertEquals("Feature count must match 37", 37, pyFeatures.length())

            val ktFeatures = UrlFeatureExtractor.extractVector(url)
            assertEquals("Kotlin feature vector size must be 37", 37, ktFeatures.size)

            for (f in 0 until 37) {
                val pyVal = pyFeatures.getDouble(f).toFloat()
                val ktVal = ktFeatures[f]
                val featName = UrlFeatureExtractor.FEATURE_NAMES[f]

                val diff = abs(pyVal - ktVal)
                assertTrue(
                    "Feature parity mismatch for URL '$url', feature [$f: $featName]: Python=$pyVal, Kotlin=$ktVal, diff=$diff",
                    diff <= 0.005f
                )
                totalFeaturesChecked++
            }
        }
        println("Successfully verified parity for $totalFeaturesChecked feature values across ${goldenCorpus.length()} URLs.")
    }

    @Test
    fun testModelPredictionAndProbabilityParity() {
        var matches = 0
        val t0 = System.nanoTime()

        for (i in 0 until goldenCorpus.length()) {
            val sample = goldenCorpus.getJSONObject(i)
            val url = sample.getString("url")
            val pyMargin = sample.getDouble("raw_margin").toFloat()
            val pyProb = sample.getDouble("calibrated_probability").toFloat()
            val pyIsPhish = sample.getBoolean("is_phishing")

            val ktPred = engine.predictUrl(url)

            // 1. Margin parity
            val marginDiff = abs(pyMargin - ktPred.rawMargin)
            assertTrue(
                "Margin mismatch for '$url': Py=$pyMargin, Kt=${ktPred.rawMargin}, diff=$marginDiff",
                marginDiff <= 0.05f
            )

            // 2. Calibrated probability parity
            val probDiff = abs(pyProb - ktPred.probability)
            assertTrue(
                "Probability mismatch for '$url': Py=$pyProb, Kt=${ktPred.probability}, diff=$probDiff",
                probDiff <= 0.02f
            )

            // 3. Classification decision must match exactly
            assertEquals(
                "Classification decision mismatch for '$url'",
                pyIsPhish,
                ktPred.isPhishing
            )

            matches++
        }

        val totalTimeMs = (System.nanoTime() - t0) / 1_000_000.0
        val avgLatencyMs = totalTimeMs / goldenCorpus.length()
        println("Evaluated ${goldenCorpus.length()} URLs with 100% decision parity in ${totalTimeMs}ms (avg ${avgLatencyMs}ms per URL).")
        assertEquals(goldenCorpus.length(), matches)
    }

    @Test
    fun testLiveUrlEvaluationOnDevice() {
        // Safe standard site
        val google = engine.predictUrl("https://www.google.com")
        assertTrue("Google must not be flagged as phishing", !google.isPhishing)
        assertTrue("Google probability must be low", google.probability < 0.10f)

        // Phishing IP site
        val ipPhish = engine.predictUrl("http://185.220.101.5/drop/payload.exe")
        assertTrue("IP payload URL must be flagged as phishing", ipPhish.isPhishing)
        assertTrue("IP payload probability must be elevated", ipPhish.probability >= 0.70f)

        // Phishing deceptive domain
        val bankPhish = engine.predictUrl("http://secure-banking-alert.top/verify-account?client=10923")
        assertTrue("Deceptive banking URL must be flagged as phishing", bankPhish.isPhishing)
        assertTrue("Deceptive banking probability must be high", bankPhish.probability >= 0.80f)
    }
}
