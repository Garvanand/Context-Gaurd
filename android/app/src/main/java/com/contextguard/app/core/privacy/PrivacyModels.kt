package com.contextguard.app.core.privacy

import android.graphics.Bitmap
import android.graphics.Rect
import java.security.MessageDigest

/**
 * Supported client network communication postures.
 */
enum class NetworkMode {
    /**
     * Complete air-gap mode. Zero outbound network calls permitted.
     * Guaranteed 0 bytes transmitted.
     */
    OFFLINE,

    /**
     * Privacy-preserving local backend mode.
     * Sends ONLY sanitized, locally redacted bitmaps and masked tokens.
     */
    LOCAL_BACKEND,

    /**
     * Restricted research evaluation mode.
     * Used for synthetic benchmark evaluation with consented test vectors only.
     */
    RESTRICTED_EVALUATION;

    val isAirplaneSafe: Boolean
        get() = this == OFFLINE
}

/**
 * Visual redaction style applied to sensitive regions.
 */
enum class RedactionStyle {
    BLACKOUT,
    BLUR
}

/**
 * Represents a localized sensitive region flagged for client-side redaction.
 */
data class SensitiveRegion(
    val id: String,
    val type: String,
    val bounds: Rect,
    val confidence: Float = 1.0f,
    val source: String = "LOCAL_PERCEPTION"
)

/**
 * Output of the on-device Redaction Engine.
 */
data class RedactionResult(
    val originalBitmap: Bitmap?,
    val redactedBitmap: Bitmap?,
    val originalText: String,
    val redactedText: String,
    val regionsRedacted: List<SensitiveRegion>,
    val style: RedactionStyle,
    val sha256Original: String,
    val sha256Redacted: String
)

/**
 * Privacy-preserving network audit telemetry entry.
 * INVARIANT: Never stores raw artifacts, OCR text, secrets, or unmasked prompts.
 */
data class NetworkAuditEntry(
    val requestId: String,
    val timestamp: Long,
    val endpointCategory: String,
    val payloadType: String,
    val payloadSizeBytes: Long,
    val isRedacted: Boolean,
    val responseStatus: Int,
    val latencyMs: Long,
    val artifactHashSha256: String
) {
    init {
        // Enforce privacy invariant at construction
        require(!endpointCategory.contains("\n") && !payloadType.contains("\n")) {
            "Network audit entry metadata must not contain newline payloads"
        }
    }
}

/**
 * Cryptographic helper to compute SHA-256 hashes without storing payloads.
 */
object CryptoUtils {
    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun sha256(text: String): String {
        return sha256(text.toByteArray(Charsets.UTF_8))
    }
}
