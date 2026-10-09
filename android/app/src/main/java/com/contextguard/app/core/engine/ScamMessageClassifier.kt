package com.contextguard.app.core.engine

import android.content.Context
import org.json.JSONObject
import java.io.InputStream
import java.util.regex.Pattern

/**
 * On-device, zero-network scam & phishing message classifier.
 *
 * INVARIANTS:
 * - Executes in < 0.5ms per message on mobile CPU.
 * - Offline, pure Kotlin, zero heavy native C++/TFLite dependencies.
 * - Platt scaling calibrated sigmoid output.
 */
class ScamMessageClassifier private constructor(
    val modelType: String,
    val schemaVersion: String,
    val vocabularySize: Int,
    val intercept: Float,
    val calibrationWeight: Float,
    val calibrationBias: Float,
    private val tokenMap: Map<String, FeatureEntry>
) {

    data class FeatureEntry(
        val idx: Int,
        val token: String,
        val idf: Float,
        val weight: Float
    )

    data class MessagePrediction(
        val rawMargin: Float,
        val probability: Float,
        val isPhishing: Boolean,
        val confidence: Float,
        val riskLevel: String,
        val matchedTokens: List<String>
    )

    private val wordPattern = Pattern.compile("\\b\\w+\\b")

    fun predict(text: String): MessagePrediction {
        if (text.isBlank()) {
            return MessagePrediction(
                rawMargin = intercept,
                probability = 0.01f,
                isPhishing = false,
                confidence = 0.98f,
                riskLevel = "LOW",
                matchedTokens = emptyList()
            )
        }

        val cleaned = text.lowercase()
        val matcher = wordPattern.matcher(cleaned)
        val words = mutableListOf<String>()
        while (matcher.find()) {
            words.add(matcher.group())
        }

        // Count unigrams and bigrams
        val tokenCounts = HashMap<String, Int>()
        for (i in words.indices) {
            val w = words[i]
            tokenCounts[w] = (tokenCounts[w] ?: 0) + 1

            if (i + 1 < words.size) {
                val bigram = "$w ${words[i + 1]}"
                tokenCounts[bigram] = (tokenCounts[bigram] ?: 0) + 1
            }
        }

        // Match tokens with vocabulary and compute TF-IDF
        var sumSquares = 0.0
        val activeFeatures = mutableListOf<Pair<FeatureEntry, Double>>()
        val matchedTokenNames = mutableListOf<String>()

        for ((tok, count) in tokenCounts) {
            val entry = tokenMap[tok] ?: continue
            // Sublinear TF: 1.0 + ln(count)
            val tf = 1.0 + Math.log(count.toDouble())
            val tfidf = tf * entry.idf.toDouble()
            sumSquares += tfidf * tfidf
            activeFeatures.add(Pair(entry, tfidf))
            matchedTokenNames.add(tok)
        }

        var rawMargin = intercept.toDouble()
        if (sumSquares > 0.0) {
            val l2Norm = Math.sqrt(sumSquares)
            for ((entry, tfidf) in activeFeatures) {
                val normalizedVal = tfidf / l2Norm
                rawMargin += normalizedVal * entry.weight.toDouble()
            }
        }

        // Apply Platt scaling calibrated sigmoid:
        // p = 1.0 / (1.0 + exp(-(weight * raw_margin + bias)))
        val exponent = -(calibrationWeight.toDouble() * rawMargin + calibrationBias.toDouble())
        val calibratedProb = (1.0 / (1.0 + Math.exp(exponent))).toFloat()

        val isPhish = calibratedProb >= 0.50f
        val confidence = Math.abs(calibratedProb - 0.50f) * 2.0f

        val riskLevel = when {
            calibratedProb >= 0.85f -> "CRITICAL"
            calibratedProb >= 0.65f -> "HIGH"
            calibratedProb >= 0.35f -> "MODERATE"
            else -> "LOW"
        }

        return MessagePrediction(
            rawMargin = rawMargin.toFloat(),
            probability = calibratedProb,
            isPhishing = isPhish,
            confidence = confidence,
            riskLevel = riskLevel,
            matchedTokens = matchedTokenNames
        )
    }

    companion object {
        private const val DEFAULT_ASSET_PATH = "models/message_model_portable.json"
        private var instance: ScamMessageClassifier? = null

        fun getInstance(context: Context): ScamMessageClassifier {
            return instance ?: synchronized(this) {
                instance ?: loadFromAssets(context, DEFAULT_ASSET_PATH).also { instance = it }
            }
        }

        fun getInstanceOrNull(): ScamMessageClassifier? = instance

        fun loadFromAssets(context: Context, assetPath: String = DEFAULT_ASSET_PATH): ScamMessageClassifier {
            val jsonString = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            return fromJsonString(jsonString)
        }

        fun loadFromStream(stream: InputStream): ScamMessageClassifier {
            val jsonString = stream.bufferedReader().use { it.readText() }
            return fromJsonString(jsonString)
        }

        fun fromJsonString(jsonString: String): ScamMessageClassifier {
            val root = JSONObject(jsonString)
            val modelType = root.optString("model_type", "tfidf_calibrated_logistic_regression")
            val schemaVersion = root.optString("schema_version", "1.0.0")
            val vocabSize = root.optInt("vocabulary_size", 0)
            val intercept = root.optDouble("intercept", 0.0).toFloat()

            val calObj = root.optJSONObject("calibration")
            val calWeight = calObj?.optDouble("weight", 1.0)?.toFloat() ?: 1.0f
            val calBias = calObj?.optDouble("bias", 0.0)?.toFloat() ?: 0.0f

            val featuresArray = root.getJSONArray("features")
            val tokenMap = HashMap<String, FeatureEntry>(featuresArray.length())

            for (i in 0 until featuresArray.length()) {
                val fObj = featuresArray.getJSONObject(i)
                val idx = fObj.getInt("idx")
                val token = fObj.getString("token")
                val idf = fObj.getDouble("idf").toFloat()
                val weight = fObj.getDouble("weight").toFloat()

                tokenMap[token] = FeatureEntry(idx, token, idf, weight)
            }

            return ScamMessageClassifier(
                modelType = modelType,
                schemaVersion = schemaVersion,
                vocabularySize = vocabSize,
                intercept = intercept,
                calibrationWeight = calWeight,
                calibrationBias = calBias,
                tokenMap = tokenMap
            )
        }
    }
}
