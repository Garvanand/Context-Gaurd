package com.contextguard.app.core.sharesheet

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.contextguard.app.core.logging.AppLogger
import com.contextguard.app.core.perception.ImagePreprocessor
import com.contextguard.app.core.perception.PdfPerceptionRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.regex.Pattern

/**
 * Robust, memory-bounded Android Sharesheet (ACTION_SEND / ACTION_SEND_MULTIPLE) payload resolver.
 * Enforces the zero raw disk persistence invariant and guarantees the app never crashes on corrupted,
 * oversized, or inaccessible shared inputs.
 */
object SharesheetPayloadResolver {

    private val URL_PATTERN: Pattern = Pattern.compile(
        "https?://[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})+(?::[0-9]+)?(?:/[^\\s]*)?",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Resolves incoming Intent into a strongly-typed SharePayload.
     * Guarantees non-throwing execution.
     */
    suspend fun resolve(context: Context, intent: Intent?): SharePayload = withContext(Dispatchers.IO) {
        if (intent == null) {
            return@withContext SharePayload.EmptyOrCancelled
        }

        val action = intent.action ?: return@withContext SharePayload.EmptyOrCancelled
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) {
            return@withContext SharePayload.EmptyOrCancelled
        }

        val sourceApp = resolveSourceApp(context, intent)
        AppLogger.i("Resolving Sharesheet intent: action=$action, mime=${intent.type}, source=$sourceApp")

        try {
            when (action) {
                Intent.ACTION_SEND_MULTIPLE -> resolveMultipleSend(context, intent, sourceApp)
                Intent.ACTION_SEND -> resolveSingleSend(context, intent, sourceApp)
                else -> SharePayload.EmptyOrCancelled
            }
        } catch (e: SecurityException) {
            AppLogger.e("URI permission revoked or inaccessible during share: ${e.message}", e)
            SharePayload.ErrorPayload(
                reason = "Access to shared content was denied by source application (Permission revoked).",
                recoverable = true,
                sourceApp = sourceApp
            )
        } catch (e: OutOfMemoryError) {
            AppLogger.e("OOM caught during sharesheet resolution: ${e.message}", e)
            SharePayload.ErrorPayload(
                reason = "Shared artifact exceeds available volatile memory. Processing halted safely.",
                recoverable = true,
                sourceApp = sourceApp
            )
        } catch (e: Exception) {
            AppLogger.e("Unexpected exception resolving share intent: ${e.message}", e)
            SharePayload.ErrorPayload(
                reason = "Unable to process shared item: ${e.localizedMessage ?: "Unknown error"}",
                recoverable = true,
                sourceApp = sourceApp
            )
        }
    }

    private suspend fun resolveSingleSend(
        context: Context,
        intent: Intent,
        sourceApp: String
    ): SharePayload {
        // Priority 1: Check for stream URI (Image / PDF / File)
        val streamUri: Uri? = extractStreamUri(intent)

        if (streamUri != null) {
            return resolveUriPayload(context, streamUri, intent.type, sourceApp)
        }

        // Priority 2: Check for plain text or shared URL
        val sharedText = extractSharedText(intent)
        if (!sharedText.isNullOrBlank()) {
            return resolveTextPayload(intent, sharedText, sourceApp)
        }

        AppLogger.w("ACTION_SEND contained neither valid stream URI nor text content")
        return SharePayload.ErrorPayload(
            reason = "No content stream or text received in share intent.",
            recoverable = true,
            sourceApp = sourceApp
        )
    }

    private suspend fun resolveMultipleSend(
        context: Context,
        intent: Intent,
        sourceApp: String
    ): SharePayload {
        val uris = extractMultipleUris(intent)
        if (uris.isEmpty()) {
            return SharePayload.ErrorPayload(
                reason = "ACTION_SEND_MULTIPLE received but attachment list was empty.",
                recoverable = true,
                sourceApp = sourceApp
            )
        }

        val primaryUri = uris.first()
        AppLogger.i("Multiple attachments received (${uris.size}). Processing primary: $primaryUri")

        val primaryPayload = resolveUriPayload(context, primaryUri, intent.type, sourceApp)

        return if (primaryPayload is SharePayload.ErrorPayload) {
            primaryPayload
        } else {
            SharePayload.MultipleAttachmentsPayload(
                primaryPayload = primaryPayload,
                totalCount = uris.size,
                warningMessage = "Received ${uris.size} items. ContextGuard evaluated primary attachment: '${getDisplayName(context, primaryUri)}'."
            )
        }
    }

    private suspend fun resolveUriPayload(
        context: Context,
        uri: Uri,
        intentMime: String?,
        sourceApp: String
    ): SharePayload {
        val cr = context.contentResolver
        val detectedMime = resolveMimeType(cr, uri, intentMime)
        val displayName = getDisplayName(context, uri) ?: "Shared_Artifact"

        AppLogger.i("Processing URI: $uri, mime: $detectedMime, name: $displayName")

        return when {
            detectedMime.startsWith("image/") -> {
                resolveImageUri(context, uri, displayName, detectedMime, sourceApp)
            }
            detectedMime == "application/pdf" || displayName.endsWith(".pdf", ignoreCase = true) -> {
                resolvePdfUri(context, uri, displayName, sourceApp)
            }
            detectedMime == "text/plain" || displayName.endsWith(".txt", ignoreCase = true) -> {
                resolveTextStreamUri(context, uri, displayName, sourceApp)
            }
            else -> {
                // Check if it's an image disguised as generic octet-stream
                val sniffedImage = trySniffImage(context, uri, displayName, sourceApp)
                sniffedImage ?: SharePayload.ErrorPayload(
                    reason = "Unsupported MIME type: '$detectedMime'. ContextGuard evaluates images, PDFs, URLs, and plain text.",
                    recoverable = true,
                    originalMime = detectedMime,
                    sourceApp = sourceApp
                )
            }
        }
    }

    private fun resolveImageUri(
        context: Context,
        uri: Uri,
        title: String,
        mimeType: String,
        sourceApp: String
    ): SharePayload {
        val cr = context.contentResolver
        var inputStream: InputStream? = null
        try {
            inputStream = cr.openInputStream(uri)
                ?: return SharePayload.ErrorPayload("Cannot open stream for image: $uri", sourceApp = sourceApp)

            val bitmap = ImagePreprocessor.decodeSampledBitmap(
                inputStream = inputStream,
                reqWidth = ImagePreprocessor.MAX_ANALYSIS_DIMENSION,
                reqHeight = ImagePreprocessor.MAX_ANALYSIS_DIMENSION
            ) ?: return SharePayload.ErrorPayload("Failed to decode image pixels (Corrupted or empty image).", sourceApp = sourceApp)

            return SharePayload.ImagePayload(
                bitmap = bitmap,
                uri = uri,
                title = title,
                mimeType = mimeType,
                sourceApp = sourceApp,
                isDownsampled = bitmap.width == ImagePreprocessor.MAX_ANALYSIS_DIMENSION || bitmap.height == ImagePreprocessor.MAX_ANALYSIS_DIMENSION
            )
        } catch (e: Exception) {
            AppLogger.e("Failed to decode image URI: $uri", e)
            return SharePayload.ErrorPayload(
                reason = "Error loading image: ${e.localizedMessage ?: "Inaccessible file"}",
                exceptionMessage = e.message,
                sourceApp = sourceApp
            )
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    private suspend fun resolvePdfUri(
        context: Context,
        uri: Uri,
        title: String,
        sourceApp: String
    ): SharePayload {
        val cr = context.contentResolver
        try {
            val pfd = cr.openFileDescriptor(uri, "r")
                ?: return SharePayload.ErrorPayload("Cannot open file descriptor for PDF document.", sourceApp = sourceApp)

            val renderResult = PdfPerceptionRenderer.renderPdfFromPfd(pfd)
            return if (renderResult.isSuccess) {
                val pages = renderResult.getOrThrow()
                if (pages.isEmpty()) {
                    SharePayload.ErrorPayload("PDF document contains 0 renderable pages.", sourceApp = sourceApp)
                } else {
                    SharePayload.PdfPayload(
                        pageBitmaps = pages,
                        uri = uri,
                        title = title,
                        sourceApp = sourceApp,
                        totalPages = pages.size
                    )
                }
            } else {
                val err = renderResult.exceptionOrNull()
                SharePayload.ErrorPayload(
                    reason = "PDF document is password-protected or corrupted: ${err?.localizedMessage ?: "Parse error"}",
                    exceptionMessage = err?.message,
                    sourceApp = sourceApp
                )
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to open PDF descriptor: $uri", e)
            return SharePayload.ErrorPayload(
                reason = "Cannot access PDF document: ${e.localizedMessage}",
                sourceApp = sourceApp
            )
        }
    }

    private fun resolveTextStreamUri(
        context: Context,
        uri: Uri,
        title: String,
        sourceApp: String
    ): SharePayload {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: ""

            resolveTextPayload(null, content, sourceApp, title)
        } catch (e: Exception) {
            SharePayload.ErrorPayload("Error reading text file: ${e.localizedMessage}", sourceApp = sourceApp)
        }
    }

    private fun trySniffImage(
        context: Context,
        uri: Uri,
        title: String,
        sourceApp: String
    ): SharePayload? {
        return try {
            val isImage = context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = ByteArray(8)
                val read = stream.read(bytes)
                if (read >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) true // JPEG
                else if (read >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()) true // PNG
                else false
            } ?: false

            if (isImage) {
                resolveImageUri(context, uri, title, "image/jpeg", sourceApp)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveTextPayload(
        intent: Intent?,
        text: String,
        sourceApp: String,
        customTitle: String? = null
    ): SharePayload {
        val detectedUrl = extractUrl(text)
        val title = customTitle
            ?: intent?.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: intent?.getStringExtra(Intent.EXTRA_TITLE)
            ?: if (detectedUrl != null) "Shared_URL.link" else "Shared_Text.txt"

        val resolvedSource = if (detectedUrl != null && sourceApp == "External App") {
            "Browser / Web Client"
        } else {
            sourceApp
        }

        return SharePayload.TextPayload(
            text = text,
            detectedUrl = detectedUrl,
            title = title,
            sourceApp = resolvedSource
        )
    }

    private fun extractStreamUri(intent: Intent): Uri? {
        // 1. EXTRA_STREAM
        @Suppress("DEPRECATION")
        val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (stream != null) return stream

        // 2. ClipData
        val clipData = intent.clipData
        if (clipData != null && clipData.itemCount > 0) {
            val clipUri = clipData.getItemAt(0).uri
            if (clipUri != null) return clipUri
        }

        // 3. Intent Data URI
        return intent.data
    }

    private fun extractMultipleUris(intent: Intent): List<Uri> {
        val uris = mutableListOf<Uri>()

        @Suppress("DEPRECATION")
        val streamList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        if (streamList != null) {
            uris.addAll(streamList.filterNotNull())
        }

        val clipData = intent.clipData
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                val itemUri = clipData.getItemAt(i).uri
                if (itemUri != null && !uris.contains(itemUri)) {
                    uris.add(itemUri)
                }
            }
        }

        return uris
    }

    private fun extractSharedText(intent: Intent): String? {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!text.isNullOrBlank()) return text

        val clipData = intent.clipData
        if (clipData != null && clipData.itemCount > 0) {
            val clipText = clipData.getItemAt(0).text?.toString()
            if (!clipText.isNullOrBlank()) return clipText
        }

        return null
    }

    internal fun extractUrl(text: String): String? {
        val matcher = URL_PATTERN.matcher(text)
        return if (matcher.find()) {
            matcher.group()
        } else {
            null
        }
    }

    private fun resolveMimeType(cr: ContentResolver, uri: Uri, intentMime: String?): String {
        return try {
            cr.getType(uri) ?: intentMime ?: "application/octet-stream"
        } catch (_: Exception) {
            intentMime ?: "application/octet-stream"
        }
    }

    private fun getDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            return uri.lastPathSegment
        }
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            return try {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) cursor.getString(idx) else null
                    } else null
                }
            } catch (_: Exception) {
                uri.lastPathSegment
            }
        }
        return uri.lastPathSegment
    }

    private fun resolveSourceApp(@Suppress("UNUSED_PARAMETER") context: Context, intent: Intent): String {
        val referrer = intent.getStringExtra(Intent.EXTRA_REFERRER_NAME)
        if (!referrer.isNullOrBlank()) return referrer

        @Suppress("DEPRECATION")
        val referrerUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_REFERRER)
        if (referrerUri != null) {
            val auth = referrerUri.authority ?: referrerUri.host
            if (!auth.isNullOrBlank()) return auth
        }

        val type = intent.type ?: ""
        return when {
            type.startsWith("image/") -> "Gallery / Photos"
            type == "application/pdf" -> "Files / Documents"
            type == "text/plain" -> "Browser / Messenger"
            else -> "External App"
        }
    }
}
