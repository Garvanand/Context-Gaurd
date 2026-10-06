package com.contextguard.app.core.sharesheet

import android.graphics.Bitmap
import android.net.Uri

/**
 * Strongly typed representation of payloads received from Android Sharesheet / ACTION_SEND.
 */
sealed interface SharePayload {

    /**
     * An image payload resolved from EXTRA_STREAM or ClipData.
     */
    data class ImagePayload(
        val bitmap: Bitmap?,
        val uri: Uri?,
        val title: String,
        val mimeType: String,
        val sourceApp: String,
        val isDownsampled: Boolean = false
    ) : SharePayload

    /**
     * Plain text or URL payload resolved from EXTRA_TEXT or ClipData.
     */
    data class TextPayload(
        val text: String,
        val detectedUrl: String?,
        val title: String,
        val sourceApp: String
    ) : SharePayload

    /**
     * PDF document payload safely rendered into in-memory page bitmaps.
     */
    data class PdfPayload(
        val pageBitmaps: List<Bitmap>,
        val uri: Uri?,
        val title: String,
        val sourceApp: String,
        val totalPages: Int
    ) : SharePayload

    /**
     * When multiple attachments were received via ACTION_SEND_MULTIPLE.
     * Contains the primary attachment plus metadata about remaining items.
     */
    data class MultipleAttachmentsPayload(
        val primaryPayload: SharePayload,
        val totalCount: Int,
        val warningMessage: String
    ) : SharePayload

    /**
     * Represents a graceful error or unsupported payload (never crashes).
     */
    data class ErrorPayload(
        val reason: String,
        val recoverable: Boolean = true,
        val originalMime: String? = null,
        val sourceApp: String = "External App",
        val exceptionMessage: String? = null
    ) : SharePayload

    /**
     * Intent was empty, cancelled, or not an ACTION_SEND.
     */
    object EmptyOrCancelled : SharePayload
}
