package com.contextguard.app.core.perception

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance, memory-bounded image preprocessor.
 * Prevents Out-Of-Memory (OOM) errors and ANRs by bounding dimensions to 1536px
 * and avoiding unnecessary memory allocations.
 */
object ImagePreprocessor {

    const val MAX_ANALYSIS_DIMENSION = 1536

    /**
     * Resizes a bitmap in-memory so its largest dimension does not exceed MAX_ANALYSIS_DIMENSION.
     * If the bitmap is already within bounds, returns the original instance without copying.
     */
    fun downscaleIfNeeded(bitmap: Bitmap, maxDimension: Int = MAX_ANALYSIS_DIMENSION): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxDim = max(width, height)

        if (maxDim <= maxDimension) {
            return bitmap
        }

        val scale = maxDimension.toFloat() / maxDim.toFloat()
        val targetWidth = max(1, (width * scale).toInt())
        val targetHeight = max(1, (height * scale).toInt())

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    /**
     * Memory-safe decoder from InputStream using inSampleSize to prevent giant allocations.
     */
    fun decodeSampledBitmap(
        inputStream: InputStream,
        reqWidth: Int = MAX_ANALYSIS_DIMENSION,
        reqHeight: Int = MAX_ANALYSIS_DIMENSION
    ): Bitmap? {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) return null

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        return downscaleIfNeeded(decoded, MAX_ANALYSIS_DIMENSION)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
