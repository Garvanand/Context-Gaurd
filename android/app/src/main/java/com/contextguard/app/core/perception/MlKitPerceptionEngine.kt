package com.contextguard.app.core.perception

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Production ML Kit Perception Engine.
 * Combines Google ML Kit Text Recognition v2, Face Detection, and on-device PII extraction.
 * Guarantees zero fake OCR: executes real ML Kit models on background coroutine dispatchers.
 */
class MlKitPerceptionEngine {

    // Lazy initialization of real ML Kit clients
    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }

    /**
     * Analyzes an in-memory bitmap (image or screenshot) using ML Kit OCR and Face Detection.
     * Automatically downscales oversized bitmaps to bounded resolution off the UI thread.
     */
    suspend fun analyzeImage(bitmap: Bitmap): LocalPerceptionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()

        val boundedBitmap = try {
            ImagePreprocessor.downscaleIfNeeded(bitmap)
        } catch (e: Exception) {
            errors.add("Image downscaling failed: ${e.message}")
            bitmap
        }

        val inputImage = InputImage.fromBitmap(boundedBitmap, 0)

        // 1. ML Kit Text Recognition
        val blocks = mutableListOf<TextBlockResult>()
        var fullOcrText = ""
        try {
            val mlText = textRecognizer.process(inputImage).await()
            fullOcrText = mlText.text
            blocks.addAll(parseMlKitTextBlocks(mlText, pageIndex = 0))
        } catch (e: Exception) {
            errors.add("ML Kit Text Recognition failed: ${e.message}")
        }

        // 2. ML Kit Face Detection
        val faces = mutableListOf<FaceFinding>()
        try {
            val mlFaces = faceDetector.process(inputImage).await()
            for (face in mlFaces) {
                faces.add(
                    FaceFinding(
                        count = 1,
                        boundingBox = face.boundingBox,
                        trackingId = face.trackingId,
                        smilingProbability = face.smilingProbability,
                        confidence = 0.90f
                    )
                )
            }
        } catch (e: Exception) {
            errors.add("ML Kit Face Detection failed: ${e.message}")
        }

        // 3. Local PII Detection
        val piiFindings = PiiDetector.detectPii(fullOcrText, blocks, pageIndex = 0)

        // 4. URL Candidate Extraction
        val urlCandidates = piiFindings
            .filter { it.type == PiiType.URL_STRING }
            .map { it.rawValue }
            .distinct()

        // 5. Redaction Regions Assembly
        val redactionRegions = buildRedactionRegions(faces, piiFindings, pageIndex = 0)

        val duration = System.currentTimeMillis() - startTime
        LocalPerceptionResult(
            ocrText = fullOcrText,
            blocks = blocks,
            piiFindings = piiFindings,
            faces = faces,
            urlCandidates = urlCandidates,
            redactionRegions = redactionRegions,
            processingTimeMs = duration,
            errors = errors,
            pageCount = 1
        )
    }

    /**
     * Analyzes a multi-page PDF document using native PdfRenderer and ML Kit OCR.
     * Renders up to 3 pages to bound memory footprint and execution latency.
     */
    suspend fun analyzePdf(pdfFile: File): LocalPerceptionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()

        val renderResult = PdfPerceptionRenderer.renderPdfPages(pdfFile)
        if (renderResult.isFailure) {
            val ex = renderResult.exceptionOrNull()
            return@withContext LocalPerceptionResult(
                ocrText = "",
                blocks = emptyList(),
                piiFindings = emptyList(),
                faces = emptyList(),
                urlCandidates = emptyList(),
                redactionRegions = emptyList(),
                processingTimeMs = System.currentTimeMillis() - startTime,
                errors = listOf("PDF rendering error: ${ex?.message}"),
                pageCount = 0
            )
        }

        val pageBitmaps = renderResult.getOrThrow()
        val allBlocks = mutableListOf<TextBlockResult>()
        val allPii = mutableListOf<PiiFinding>()
        val allFaces = mutableListOf<FaceFinding>()
        val allRedactions = mutableListOf<RedactionRegion>()
        val pageTextBuilder = StringBuilder()

        try {
            for (pageIdx in pageBitmaps.indices) {
                val pageBmp = pageBitmaps[pageIdx]
                val inputImage = InputImage.fromBitmap(pageBmp, 0)

                pageTextBuilder.append("--- PAGE ${pageIdx + 1} ---\n")

                try {
                    val mlText = textRecognizer.process(inputImage).await()
                    pageTextBuilder.append(mlText.text).append("\n\n")

                    val pageBlocks = parseMlKitTextBlocks(mlText, pageIndex = pageIdx)
                    allBlocks.addAll(pageBlocks)

                    val pagePii = PiiDetector.detectPii(mlText.text, pageBlocks, pageIndex = pageIdx)
                    allPii.addAll(pagePii)
                } catch (e: Exception) {
                    errors.add("Page ${pageIdx + 1} OCR failed: ${e.message}")
                }

                try {
                    val mlFaces = faceDetector.process(inputImage).await()
                    for (face in mlFaces) {
                        allFaces.add(
                            FaceFinding(
                                count = 1,
                                boundingBox = face.boundingBox,
                                trackingId = face.trackingId,
                                smilingProbability = face.smilingProbability,
                                confidence = 0.90f
                            )
                        )
                    }
                } catch (e: Exception) {
                    errors.add("Page ${pageIdx + 1} Face Detection failed: ${e.message}")
                }

                allRedactions.addAll(buildRedactionRegions(allFaces, allPii, pageIndex = pageIdx))
            }
        } finally {
            // Free bitmaps immediately
            pageBitmaps.forEach { if (!it.isRecycled) it.recycle() }
        }

        val fullText = pageTextBuilder.toString().trim()
        val urlCandidates = allPii
            .filter { it.type == PiiType.URL_STRING }
            .map { it.rawValue }
            .distinct()

        val duration = System.currentTimeMillis() - startTime
        LocalPerceptionResult(
            ocrText = fullText,
            blocks = allBlocks,
            piiFindings = allPii,
            faces = allFaces,
            urlCandidates = urlCandidates,
            redactionRegions = allRedactions,
            processingTimeMs = duration,
            errors = errors,
            pageCount = pageBitmaps.size
        )
    }

    /**
     * Direct string text perception (e.g. from clipboard or shared raw text).
     */
    suspend fun analyzeText(rawText: String): LocalPerceptionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val cleanText = rawText.trim()

        val syntheticBlock = TextBlockResult(
            text = cleanText,
            boundingBox = null,
            lines = listOf(TextLineResult(cleanText, null))
        )
        val blocks = listOf(syntheticBlock)

        val piiFindings = PiiDetector.detectPii(cleanText, blocks, pageIndex = 0)
        val urlCandidates = piiFindings
            .filter { it.type == PiiType.URL_STRING }
            .map { it.rawValue }
            .distinct()

        val duration = System.currentTimeMillis() - startTime
        LocalPerceptionResult(
            ocrText = cleanText,
            blocks = blocks,
            piiFindings = piiFindings,
            faces = emptyList(),
            urlCandidates = urlCandidates,
            redactionRegions = emptyList(),
            processingTimeMs = duration,
            errors = emptyList(),
            pageCount = 1
        )
    }

    /**
     * URL direct analysis (validates URL structure, finds embedded parameters and tokens).
     */
    suspend fun analyzeUrl(url: String): LocalPerceptionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val cleanUrl = url.trim()

        val blocks = listOf(
            TextBlockResult(
                text = cleanUrl,
                boundingBox = null,
                lines = listOf(TextLineResult(cleanUrl, null))
            )
        )
        val piiFindings = PiiDetector.detectPii(cleanUrl, blocks, pageIndex = 0)
        val urlCandidates = listOf(cleanUrl)

        val duration = System.currentTimeMillis() - startTime
        LocalPerceptionResult(
            ocrText = cleanUrl,
            blocks = blocks,
            piiFindings = piiFindings,
            faces = emptyList(),
            urlCandidates = urlCandidates,
            redactionRegions = emptyList(),
            processingTimeMs = duration,
            errors = emptyList(),
            pageCount = 1
        )
    }

    /**
     * Parses ML Kit Text object into strongly-typed TextBlockResult models.
     */
    fun parseMlKitTextBlocks(mlText: Text, pageIndex: Int = 0): List<TextBlockResult> {
        val blockResults = mutableListOf<TextBlockResult>()
        for (mlBlock in mlText.textBlocks) {
            val lineResults = mutableListOf<TextLineResult>()
            for (mlLine in mlBlock.lines) {
                val elementResults = mutableListOf<TextElementResult>()
                for (mlElement in mlLine.elements) {
                    elementResults.add(
                        TextElementResult(
                            text = mlElement.text,
                            boundingBox = mlElement.boundingBox
                        )
                    )
                }
                lineResults.add(
                    TextLineResult(
                        text = mlLine.text,
                        boundingBox = mlLine.boundingBox,
                        elements = elementResults
                    )
                )
            }
            blockResults.add(
                TextBlockResult(
                    text = mlBlock.text,
                    boundingBox = mlBlock.boundingBox,
                    lines = lineResults,
                    pageIndex = pageIndex
                )
            )
        }
        return blockResults
    }

    /**
     * Synthesizes bounding boxes from detected faces and PII for on-device pixel redaction.
     */
    private fun buildRedactionRegions(
        faces: List<FaceFinding>,
        piiFindings: List<PiiFinding>,
        pageIndex: Int = 0
    ): List<RedactionRegion> {
        val regions = mutableListOf<RedactionRegion>()

        // 1. Face redaction boxes
        faces.forEachIndexed { i, face ->
            face.boundingBox?.let { box ->
                regions.add(
                    RedactionRegion(
                        id = "face_${pageIndex}_$i",
                        boundingBox = box,
                        type = "FACE",
                        label = "Detected Face",
                        pageIndex = pageIndex
                    )
                )
            }
        }

        // 2. PII bounding boxes
        piiFindings.forEachIndexed { i, pii ->
            pii.boundingBox?.let { box ->
                regions.add(
                    RedactionRegion(
                        id = "pii_${pageIndex}_$i",
                        boundingBox = box,
                        type = pii.type.name,
                        label = pii.maskedValue,
                        pageIndex = pageIndex
                    )
                )
            }
        }

        return regions
    }
}
