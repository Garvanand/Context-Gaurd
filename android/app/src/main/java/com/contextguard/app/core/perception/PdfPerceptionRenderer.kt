package com.contextguard.app.core.perception

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Android-native PdfRenderer implementation for pre-action document perception.
 * Safely renders the first 3 pages without crashing or causing OOM on large PDFs.
 */
object PdfPerceptionRenderer {

    const val MAX_PDF_PAGES = 3
    const val PDF_RENDER_MAX_DIMENSION = 1536

    /**
     * Renders up to 3 pages from a PDF File into bounded bitmaps for OCR processing.
     * Guarantees all file descriptors and native pages are safely closed.
     */
    suspend fun renderPdfPages(
        pdfFile: File,
        maxPages: Int = MAX_PDF_PAGES
    ): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("PDF file does not exist or is empty: ${pdfFile.path}"))
        }

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val renderedBitmaps = mutableListOf<Bitmap>()

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            val totalPages = renderer.pageCount
            val pagesToProcess = min(totalPages, maxPages)

            for (i in 0 until pagesToProcess) {
                val page = renderer.openPage(i)
                try {
                    val pageWidth = page.width
                    val pageHeight = page.height

                    // Calculate bounded scale to preserve readability without exceeding 1536px
                    val maxDim = max(pageWidth, pageHeight)
                    val scale = if (maxDim > PDF_RENDER_MAX_DIMENSION) {
                        PDF_RENDER_MAX_DIMENSION.toFloat() / maxDim.toFloat()
                    } else {
                        // Standard density scaling for OCR clarity
                        min(2.0f, PDF_RENDER_MAX_DIMENSION.toFloat() / maxDim.toFloat())
                    }

                    val bmpWidth = max(1, (pageWidth * scale).toInt())
                    val bmpHeight = max(1, (pageHeight * scale).toInt())

                    val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                    // Pre-fill with white background (PDF pages can have transparent backgrounds)
                    bitmap.eraseColor(Color.WHITE)

                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    renderedBitmaps.add(bitmap)
                } finally {
                    page.close()
                }
            }

            Result.success(renderedBitmaps)
        } catch (e: Exception) {
            // Clean up any bitmaps created before failure
            renderedBitmaps.forEach { if (!it.isRecycled) it.recycle() }
            Result.failure(e)
        } finally {
            try {
                renderer?.close()
            } catch (_: Exception) {}
            try {
                pfd?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Renders up to 3 pages from an existing ParcelFileDescriptor (e.g. from ContentResolver).
     * Does not require filesystem persistence.
     */
    suspend fun renderPdfFromPfd(
        pfd: ParcelFileDescriptor,
        maxPages: Int = MAX_PDF_PAGES
    ): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        var renderer: PdfRenderer? = null
        val renderedBitmaps = mutableListOf<Bitmap>()

        try {
            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            val pagesToProcess = min(totalPages, maxPages)

            for (i in 0 until pagesToProcess) {
                val page = renderer.openPage(i)
                try {
                    val pageWidth = page.width
                    val pageHeight = page.height

                    val maxDim = max(pageWidth, pageHeight)
                    val scale = if (maxDim > PDF_RENDER_MAX_DIMENSION) {
                        PDF_RENDER_MAX_DIMENSION.toFloat() / maxDim.toFloat()
                    } else {
                        min(2.0f, PDF_RENDER_MAX_DIMENSION.toFloat() / maxDim.toFloat())
                    }

                    val bmpWidth = max(1, (pageWidth * scale).toInt())
                    val bmpHeight = max(1, (pageHeight * scale).toInt())

                    val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)

                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    renderedBitmaps.add(bitmap)
                } finally {
                    page.close()
                }
            }

            Result.success(renderedBitmaps)
        } catch (e: Exception) {
            renderedBitmaps.forEach { if (!it.isRecycled) it.recycle() }
            Result.failure(e)
        } finally {
            try {
                renderer?.close()
            } catch (_: Exception) {}
            try {
                pfd.close()
            } catch (_: Exception) {}
        }
    }
}
