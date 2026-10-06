package com.contextguard.app.core.perception

import android.graphics.Rect

/**
 * Supported artifact input types for ContextGuard perception layer.
 */
enum class ArtifactType {
    IMAGE,
    SCREENSHOT,
    TEXT,
    URL,
    PDF
}

/**
 * PII classification categories detected on-device.
 */
enum class PiiType {
    PHONE_NUMBER,
    EMAIL_ADDRESS,
    OTP_CODE,
    PAYMENT_CARD,
    AADHAAR_NUMBER,
    BANK_ACCOUNT,
    URL_STRING,
    DATE_STRING,
    STREET_ADDRESS,
    PERSONAL_NAME
}

/**
 * Grounded on-device PII finding with spatial coordinates and confidence.
 */
data class PiiFinding(
    val type: PiiType,
    val patternId: String,
    val rawValue: String,
    val maskedValue: String,
    val confidence: Float,
    val boundingBox: Rect? = null,
    val source: String = "REGEX_HEURISTIC",
    val pageIndex: Int = 0
)

/**
 * OCR text block with bounding box and line breakdowns.
 */
data class TextBlockResult(
    val text: String,
    val boundingBox: Rect?,
    val lines: List<TextLineResult> = emptyList(),
    val pageIndex: Int = 0
)

/**
 * OCR text line with spatial bounding box.
 */
data class TextLineResult(
    val text: String,
    val boundingBox: Rect?,
    val elements: List<TextElementResult> = emptyList()
)

/**
 * Single OCR word/token element.
 */
data class TextElementResult(
    val text: String,
    val boundingBox: Rect?
)

/**
 * Human facial contour identified on-device.
 */
data class FaceFinding(
    val count: Int,
    val boundingBox: Rect?,
    val trackingId: Int? = null,
    val smilingProbability: Float? = null,
    val confidence: Float = 0.90f
)

/**
 * Spatial redaction region for in-memory pixel masking.
 */
data class RedactionRegion(
    val id: String,
    val boundingBox: Rect,
    val type: String,
    val label: String,
    val pageIndex: Int = 0
)

/**
 * Strongly-typed perception output contract.
 * Aggregates OCR text, structured blocks, PII findings, detected faces,
 * extracted URLs, suggested redaction regions, and execution telemetry.
 */
data class LocalPerceptionResult(
    val ocrText: String,
    val blocks: List<TextBlockResult> = emptyList(),
    val piiFindings: List<PiiFinding> = emptyList(),
    val faces: List<FaceFinding> = emptyList(),
    val urlCandidates: List<String> = emptyList(),
    val redactionRegions: List<RedactionRegion> = emptyList(),
    val processingTimeMs: Long,
    val errors: List<String> = emptyList(),
    val pageCount: Int = 1
) {
    val hasSensitiveFindings: Boolean
        get() = piiFindings.isNotEmpty() || faces.isNotEmpty() || urlCandidates.isNotEmpty()

    val faceCount: Int
        get() = faces.size
}
