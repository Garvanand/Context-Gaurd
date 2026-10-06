package com.contextguard.app.core.privacy

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.contextguard.app.core.perception.FaceFinding
import com.contextguard.app.core.perception.PiiDetector
import com.contextguard.app.core.perception.PiiFinding
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * On-Device Redaction Engine.
 *
 * Implements privacy-preserving visual masking (Blackout & Blur) and text token sanitization
 * entirely in volatile memory before any network transmission or local backend call.
 */
class RedactionEngine {

    private val blackoutPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val blackoutBorderPaint = Paint().apply {
        color = 0xFF38BDF8.toInt() // Cyan Accent border
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    /**
     * Executes the redaction pipeline on an artifact and its perceptual findings.
     */
    fun redact(
        bitmap: Bitmap?,
        ocrText: String,
        piiFindings: List<PiiFinding>,
        faceFindings: List<FaceFinding>,
        style: RedactionStyle = RedactionStyle.BLACKOUT
    ): RedactionResult {
        val sensitiveRegions = mutableListOf<SensitiveRegion>()

        // 1. Map PII spatial regions
        piiFindings.forEachIndexed { index, pii ->
            val box = pii.boundingBox ?: Rect(0, 0, 0, 0)
            sensitiveRegions.add(
                SensitiveRegion(
                    id = "pii_${index}_${pii.type.name}",
                    type = pii.type.name,
                    bounds = box,
                    confidence = pii.confidence,
                    source = pii.source
                )
            )
        }

        // 2. Map Face spatial regions
        faceFindings.forEachIndexed { index, face ->
            val box = face.boundingBox
            if (box != null && box.width() > 0 && box.height() > 0) {
                sensitiveRegions.add(
                    SensitiveRegion(
                        id = "face_${index}_${face.trackingId ?: index}",
                        type = "FACE",
                        bounds = Rect(box),
                        confidence = 0.98f,
                        source = "ML_KIT_FACE"
                    )
                )
            }
        }

        // 3. Process Visual Bitmap Redaction
        val redactedBitmap: Bitmap? = if (bitmap != null && !bitmap.isRecycled) {
            val copy = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(copy)

            for (region in sensitiveRegions) {
                val clamped = clampBounds(region.bounds, copy.width, copy.height)
                if (clamped.width() <= 0 || clamped.height() <= 0) continue

                when (style) {
                    RedactionStyle.BLACKOUT -> {
                        canvas.drawRect(clamped, blackoutPaint)
                        canvas.drawRect(clamped, blackoutBorderPaint)
                    }
                    RedactionStyle.BLUR -> {
                        applyPixelateBlur(copy, clamped)
                    }
                }
            }
            copy
        } else {
            null
        }

        // 4. Process Text Redaction
        val maskedText = PiiDetector.maskPiiInText(ocrText, piiFindings)

        // 5. Compute cryptographic digests (SHA-256)
        val originalDigest = if (bitmap != null) {
            computeBitmapSha256(bitmap)
        } else {
            CryptoUtils.sha256(ocrText)
        }

        val redactedDigest = if (redactedBitmap != null) {
            computeBitmapSha256(redactedBitmap)
        } else {
            CryptoUtils.sha256(maskedText)
        }

        return RedactionResult(
            originalBitmap = bitmap,
            redactedBitmap = redactedBitmap,
            originalText = ocrText,
            redactedText = maskedText,
            regionsRedacted = sensitiveRegions,
            style = style,
            sha256Original = originalDigest,
            sha256Redacted = redactedDigest
        )
    }

    /**
     * Applies authentic block pixelation blur over the bounded sensitive region.
     */
    private fun applyPixelateBlur(bitmap: Bitmap, bounds: Rect, blockSize: Int = 16) {
        val width = bounds.width()
        val height = bounds.height()
        if (width <= 0 || height <= 0) return

        val effectiveBlock = max(4, min(blockSize, min(width, height) / 3))

        for (y in bounds.top until bounds.bottom step effectiveBlock) {
            for (x in bounds.left until bounds.right step effectiveBlock) {
                val blockW = min(effectiveBlock, bounds.right - x)
                val blockH = min(effectiveBlock, bounds.bottom - y)

                // Sample center pixel of the block
                val sampleX = min(x + blockW / 2, bitmap.width - 1)
                val sampleY = min(y + blockH / 2, bitmap.height - 1)
                val pixelColor = bitmap.getPixel(sampleX, sampleY)

                // Paint the entire block with the sampled color
                for (by in 0 until blockH) {
                    for (bx in 0 until blockW) {
                        val px = x + bx
                        val py = y + by
                        if (px < bitmap.width && py < bitmap.height) {
                            bitmap.setPixel(px, py, pixelColor)
                        }
                    }
                }
            }
        }
    }

    /**
     * Clamps a rectangle to fit strictly inside the bitmap dimensions.
     */
    private fun clampBounds(rect: Rect, maxWidth: Int, maxHeight: Int): Rect {
        val left = max(0, min(rect.left, maxWidth))
        val top = max(0, min(rect.top, maxHeight))
        val right = max(left, min(rect.right, maxWidth))
        val bottom = max(top, min(rect.bottom, maxHeight))
        return Rect(left, top, right, bottom)
    }

    /**
     * Computes the SHA-256 hash of a bitmap's compressed representation.
     */
    private fun computeBitmapSha256(bitmap: Bitmap): String {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            CryptoUtils.sha256(stream.toByteArray())
        } catch (e: Exception) {
            CryptoUtils.sha256("bitmap_${bitmap.width}x${bitmap.height}_${System.currentTimeMillis()}")
        }
    }
}
