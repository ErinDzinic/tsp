package com.maca.tsp.common.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.tasks.await
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

object ImageFilterUtils {
    fun applyBlackAndWhiteFilter(original: Bitmap): Bitmap {
        val result = createBitmap(original.width, original.height)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(original, 0f, 0f, paint)

        return result
    }

    fun flipBitmap(bitmap: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            postScale(
                if (horizontal) -1f else 1f,
                if (horizontal) 1f else -1f,
                bitmap.width / 2f,
                bitmap.height / 2f
            )
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun applyBrightness(original: Bitmap, brightness: Float): Bitmap {
        val result = createBitmap(original.width, original.height)
        val canvas = Canvas(result)
        val paint = Paint()

        val adjustedBrightness = brightness * 1.27f

        val colorMatrix = ColorMatrix()
        colorMatrix.set(floatArrayOf(
            1f, 0f, 0f, 0f, adjustedBrightness,
            0f, 1f, 0f, 0f, adjustedBrightness,
            0f, 0f, 1f, 0f, adjustedBrightness,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(original, 0f, 0f, paint)

        return result
    }

    fun applyExposure(bitmap: Bitmap, exposure: Float): Bitmap {
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)
        val resultMat = Mat()

        // Adjust multiplier (e.g., exposure range -1 to 1 maps to 0.5 to 1.5 multiplier)
        val exposureMultiplier = 1.0 + exposure * 0.25 // play with multiplier strength

        srcMat.convertTo(resultMat, -1, exposureMultiplier, 0.0)

        val resultBitmap = createBitmap(bitmap.width, bitmap.height)
        Utils.matToBitmap(resultMat, resultBitmap)

        srcMat.release()
        resultMat.release()

        return resultBitmap
    }

    fun applyContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)

        val resultMat = Mat()

        // Prevent 0 value (black image)
        val safeContrast = contrast.coerceAtLeast(0.1f)

        srcMat.convertTo(resultMat, -1, safeContrast.toDouble(),
            ((1 - safeContrast) * 128).toDouble()
        )

        val resultBitmap = createBitmap(bitmap.width, bitmap.height)
        Utils.matToBitmap(resultMat, resultBitmap)

        srcMat.release()
        resultMat.release()

        return resultBitmap
    }

    private fun applyGammaToMat(srcMat: Mat, gamma: Float): Mat {
        val resultMat = srcMat.clone() // Work on a copy

        // Safe gamma value
        val safeGamma = if (gamma < 0.1f) 0.1f else gamma
        val gammaCorrection = 1.0f / safeGamma

        // Build gamma lookup table (LUT)
        val lut = Mat(1, 256, CvType.CV_8U)
        val lutData = ByteArray(256)
        for (i in 0..255) {
            lutData[i] = (255.0 * (i / 255.0).pow(gammaCorrection.toDouble())).coerceIn(0.0, 255.0).toInt().toByte()
        }
        lut.put(0, 0, lutData)

        // Apply LUT
        Core.LUT(srcMat, lut, resultMat)

        lut.release() // Release LUT Mat
        return resultMat
    }

    fun applyGamma(original: Bitmap, gamma: Float): Bitmap {
        val srcMat = Mat()
        Utils.bitmapToMat(original, srcMat)
        // Assuming input might be RGBA, convert to RGB for gamma, then back
        val rgbMat = Mat()
        Imgproc.cvtColor(srcMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
        srcMat.release() // Release original RGBA mat

        val gammaAppliedMat = applyGammaToMat(rgbMat, gamma)
        rgbMat.release() // Release intermediate RGB mat

        // Convert back to RGBA before creating bitmap
        val resultRgbaMat = Mat()
        Imgproc.cvtColor(gammaAppliedMat, resultRgbaMat, Imgproc.COLOR_RGB2RGBA)
        gammaAppliedMat.release() // Release gamma applied mat

        val resultBitmap = createBitmap(original.width, original.height)
        Utils.matToBitmap(resultRgbaMat, resultBitmap)
        resultRgbaMat.release() // Release final RGBA mat

        return resultBitmap
    }

    fun applySharpness(original: Bitmap, sharpness: Float, preserveBackground: Boolean = true): Bitmap {
        // Convert to OpenCV Mat
        val srcMat = Mat()
        Utils.bitmapToMat(original, srcMat)

        // Convert to proper format
        Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB)

        // Create result matrix
        val resultMat = Mat()

        // No sharpening needed
        if (sharpness <= 0f) {
            srcMat.copyTo(resultMat)
        } else {
            // Create a blurred version for unsharp mask
            val blurredMat = Mat()
            Imgproc.GaussianBlur(srcMat, blurredMat, Size(3.0, 3.0), 0.0)
            Core.addWeighted(srcMat, 1.0 + sharpness * 1.5, blurredMat, (-sharpness * 1.5), 0.0, resultMat)
            if (preserveBackground) {
                val grayMat = Mat()
                Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGB2GRAY)
                val mask = Mat()
                Imgproc.threshold(grayMat, mask, 240.0, 255.0, Imgproc.THRESH_BINARY)
                srcMat.copyTo(resultMat, mask)
                grayMat.release()
                mask.release()
            }

            blurredMat.release()
        }
        Imgproc.cvtColor(resultMat, resultMat, Imgproc.COLOR_RGB2RGBA)
        val resultBitmap = createBitmap(original.width, original.height)
        Utils.matToBitmap(resultMat, resultBitmap)

        srcMat.release()
        resultMat.release()

        return resultBitmap
    }

    fun applySketchFilter(bitmap: Bitmap, details: Float, gamma: Float): Bitmap {
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)
        Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2BGR) // OpenCV often prefers BGR

        // Convert to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGR2GRAY)
        srcMat.release() // Release BGR mat

        // Invert the grayscale image
        val invertedMat = Mat()
        Core.bitwise_not(grayMat, invertedMat)

        // Apply Gaussian blur based on 'details'
        // Map details (assuming 1-25 range from filter type) to kernel size
        val kernelValue = (details * 1.5f).roundToInt().coerceAtLeast(1) // Adjust multiplier as needed
        val kernelSize = max(1, (kernelValue / 2) * 2 + 1) // Ensure kernel size is positive and odd
        val blurredMat = Mat()
        Imgproc.GaussianBlur(invertedMat, blurredMat, Size(kernelSize.toDouble(), kernelSize.toDouble()), 0.0)
        invertedMat.release() // Release inverted mat

        // Invert the blurred image
        val invertedBlurredMat = Mat()
        Core.bitwise_not(blurredMat, invertedBlurredMat)
        blurredMat.release() // Release blurred mat

        // Create pencil sketch effect using Color Dodge blend (Core.divide)
        val sketchMat = Mat()
        Core.divide(grayMat, invertedBlurredMat, sketchMat, 255.0) // Scale factor 255 for dodge
        grayMat.release() // Release gray mat
        invertedBlurredMat.release() // Release inverted blurred mat

        // Apply Gamma Correction directly to the sketch Mat
        val gammaAppliedSketchMat = applyGammaToMat(sketchMat, gamma)
        sketchMat.release() // Release initial sketch mat

        // Convert final sketch back to RGBA for Bitmap creation
        val resultRgbaMat = Mat()
        Imgproc.cvtColor(gammaAppliedSketchMat, resultRgbaMat, Imgproc.COLOR_GRAY2RGBA) // Convert final grayscale sketch to RGBA
        gammaAppliedSketchMat.release() // Release gamma applied mat

        // Convert Mat to Bitmap
        val resultBitmap = createBitmap(bitmap.width, bitmap.height)
        Utils.matToBitmap(resultRgbaMat, resultBitmap)
        resultRgbaMat.release() // Release final RGBA mat

        return resultBitmap
    }

    fun applyGaussianBlur(original: Bitmap, radius: Float): Bitmap {
        val srcMat = Mat()
        Utils.bitmapToMat(original, srcMat)

        // Convert to RGB format
        Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB)

        val resultMat = Mat()

        // Ensure kernel size is positive & odd
        val safeRadius = (radius.toInt().coerceAtLeast(1) / 2) * 2 + 1
        val kernelSize = Size(safeRadius.toDouble(), safeRadius.toDouble())

        Imgproc.GaussianBlur(srcMat, resultMat, kernelSize, 0.0)

        // Convert back to RGBA
        Imgproc.cvtColor(resultMat, resultMat, Imgproc.COLOR_RGB2RGBA)

        val resultBitmap = createBitmap(original.width, original.height)
        Utils.matToBitmap(resultMat, resultBitmap)

        srcMat.release()
        resultMat.release()

        return resultBitmap
    }


    suspend fun removeBackground(source: Bitmap): Bitmap {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .enableRawSizeMask()
            .build()
        // Ensure Segmentation and SelfieSegmenterOptions are imported from ML Kit
        val segmenter: Segmenter = Segmentation.getClient(options)
        val inputImage = InputImage.fromBitmap(source, 0)

        return try {
            Timber.v("Starting background segmentation...")
            // --- FIX: Use await() ---
            val resultMask = segmenter.process(inputImage).await()
            // --- End Fix ---
            Timber.v("Segmentation complete.")

            val maskBuffer = resultMask.buffer
            val maskWidth = resultMask.width
            val maskHeight = resultMask.height
            Timber.v("Mask dimensions: ${maskWidth}x$maskHeight")

            if (maskWidth != source.width || maskHeight != source.height) {
                Timber.w("Mask size ($maskWidth x $maskHeight) differs from bitmap size (${source.width} x ${source.height}). Skipping background removal.")
                return source // Return original if sizes mismatch
            }

            val resultBitmap = createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            resultBitmap.eraseColor(Color.TRANSPARENT)
            val pixels = IntArray(source.width * source.height)
            source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
            val resultPixels = IntArray(source.width * source.height)

            for (y in 0 until maskHeight) {
                for (x in 0 until maskWidth) {
                    val confidence = maskBuffer.float
                    val pixelIndex = y * source.width + x
                    if (pixelIndex < pixels.size) { // Ensure index is within bounds
                        if (confidence > 0.5f) { // Keep foreground pixel
                            resultPixels[pixelIndex] = pixels[pixelIndex]
                        }
                        // else: resultPixels[pixelIndex] remains 0 (transparent)
                    }
                }
            }
            maskBuffer.rewind() // Good practice

            resultBitmap.setPixels(resultPixels, 0, source.width, 0, 0, source.width, source.height)
            Timber.v("Background removal applied.")
            resultBitmap

        } catch (e: Exception) {
            Timber.e(e, "Error removing background")
            source // Return original on error
        } finally {
            // Ensure segmenter is always closed
            try { segmenter.close() } catch (e: Exception) { Timber.e(e, "Error closing segmenter") }
            Timber.v("Segmenter closed.")
        }
    }

    private fun applyStippleEffectMat(grayMat: Mat, dotDensity: Float, dotSize: Float): Mat {
        // ... (Implementation remains the same as previous version) ...
        Timber.d("Applying Stipple (Mat): density=$dotDensity, size=$dotSize")
        if (dotSize < 1f || grayMat.empty() || grayMat.type() != CvType.CV_8UC1) {
            Timber.w("Invalid input for applyStippleEffectMat. DotSize: $dotSize, Empty: ${grayMat.empty()}, Type: ${grayMat.type()}")
            return Mat(grayMat.size(), CvType.CV_8UC3, Scalar(255.0, 255.0, 255.0))
        }
        val dotSizeInt = dotSize.roundToInt()
        val outputMat = Mat(grayMat.rows(), grayMat.cols(), CvType.CV_8UC3, Scalar(255.0, 255.0, 255.0))
        val dotColor = Scalar(0.0, 0.0, 0.0)
        val rows = grayMat.rows()
        val cols = grayMat.cols()
        val grayData = ByteArray(rows * cols)
        try {
            if (grayMat.isContinuous) {
                grayMat.get(0, 0, grayData)
            } else {
                Timber.w("GrayMat not continuous, stipple might be slow.")
                grayMat.get(0, 0, grayData)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting grayMat data")
            outputMat.release()
            val errorMat = Mat(grayMat.size(), CvType.CV_8UC3, Scalar(255.0, 255.0, 255.0))
            return errorMat
        }
        try {
            for (y in 0 until rows step dotSizeInt) {
                for (x in 0 until cols step dotSizeInt) {
                    val index = y * cols + x
                    if (index < grayData.size) {
                        val intensityByte = grayData[index]
                        val unsignedIntValue = intensityByte.toInt() and 0xFF
                        val intensity = unsignedIntValue / 255.0
                        val effectiveDensity = dotDensity.coerceIn(0f, 2f)
                        val probability = ((1.0 - intensity) * effectiveDensity).coerceIn(0.0, 1.0)
                        if (Random.nextFloat() < probability) {
                            val p1 = Point(x.toDouble(), y.toDouble())
                            val x2 = (x + dotSizeInt).coerceAtMost(cols).toDouble()
                            val y2 = (y + dotSizeInt).coerceAtMost(rows).toDouble()
                            val p2 = Point(x2, y2)
                            Imgproc.rectangle(outputMat, p1, p2, dotColor, -1)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during OpenCV stipple processing loop")
        }
        Timber.d("Stipple effect (Mat) applied.")
        return outputMat // Return the BGR Mat with dots
    }
    // --- End Stipple Effect Function ---


    // --- Dotwork Effect Orchestrator (SIMPLIFIED FOR TESTING) ---
    /**
     * Applies ONLY the stipple effect for debugging purposes.
     * Ignores textures and blending.
     */
    fun applyDotworkEffect(
        inputBitmap: Bitmap,
        dotDensity: Float,
        dotSize: Float,
        // Textures are ignored in this simplified version
        grainTexture1: Bitmap?,
        grainTexture2: Bitmap?,
        blackTexture: Bitmap?
    ): Bitmap {
        Timber.d("Applying SIMPLIFIED Dotwork Effect (Stipple Only)")

        val matsToRelease = mutableListOf<Mat>()
        try {
            // 1. Prepare grayscale input Mat
            val grayInputMat = Mat()
            val tempInputMat = Mat()
            Utils.bitmapToMat(inputBitmap, tempInputMat)
            matsToRelease.add(tempInputMat)
            Imgproc.cvtColor(tempInputMat, grayInputMat, Imgproc.COLOR_RGBA2GRAY)
            matsToRelease.add(grayInputMat)

            // 2. Apply stipple effect (returns BGR Mat)
            val stippledMat = applyStippleEffectMat(grayInputMat, dotDensity, dotSize)
            matsToRelease.add(stippledMat)

            // 3. Convert stipple Mat directly to output Bitmap (RGBA)
            Timber.v("Converting stipple Mat to Bitmap...")
            val finalBitmap = createBitmap(stippledMat.cols(), stippledMat.rows(), Bitmap.Config.ARGB_8888)
            val rgbaMat = Mat() // Temporary Mat for conversion
            matsToRelease.add(rgbaMat)
            Imgproc.cvtColor(stippledMat, rgbaMat, Imgproc.COLOR_BGR2RGBA) // Convert BGR to RGBA
            Utils.matToBitmap(rgbaMat, finalBitmap)
            Timber.d("Simplified Dotwork (Stipple only) applied successfully.")
            return finalBitmap

            // --- Original Blending/Blurring Steps (Commented Out) ---
            /*
            var currentResultMat: Mat? = stippledMat // Start subsequent blending with the stipple Mat

            // 2. Resize textures and convert to Mat (ensure BGR)
            val targetSize = Size(currentResultMat.width().toDouble(), currentResultMat.height().toDouble())
            val grain1Mat = Mat(); matsToRelease.add(grain1Mat)
            Utils.bitmapToMat(grainTexture1!!, grain1Mat) // Assuming textures are not null here
            Imgproc.resize(grain1Mat, grain1Mat, targetSize)
            Imgproc.cvtColor(grain1Mat, grain1Mat, Imgproc.COLOR_RGBA2BGR)

            val grain2Mat = Mat(); matsToRelease.add(grain2Mat)
            Utils.bitmapToMat(grainTexture2!!, grain2Mat)
            Imgproc.resize(grain2Mat, grain2Mat, targetSize)
            Imgproc.cvtColor(grain2Mat, grain2Mat, Imgproc.COLOR_RGBA2BGR)

            val blackMat = Mat(); matsToRelease.add(blackMat)
            Utils.bitmapToMat(blackTexture!!, blackMat)
            Imgproc.resize(blackMat, blackMat, targetSize)
            Imgproc.cvtColor(blackMat, blackMat, Imgproc.COLOR_RGBA2BGR)

            // --- Blending Stages ---
            Timber.v("Dotwork: Blending Grain 1 (Screen)")
            var blendResult1 = blendScreen(currentResultMat, grain1Mat)
            blendResult1?.let { if(it !== currentResultMat) matsToRelease.add(it); currentResultMat = it } ?: Timber.e("Blend 1 (Screen) failed")

            Timber.v("Dotwork: Blending Grain 2 (Normal)")
            var blendResult2 = blendNormal(currentResultMat, grain2Mat, 0.5) // Use 0.5 weight
            blendResult2?.let { if(it !== currentResultMat) matsToRelease.add(it); currentResultMat = it } ?: Timber.e("Blend 2 (Normal) failed")

            Timber.v("Dotwork: Applying Blur 1")
            currentResultMat?.let { Imgproc.GaussianBlur(it, it, Size(3.0, 3.0), 0.0) }

            Timber.v("Dotwork: Skipping Hard Mix Blend (TODO)")

            Timber.v("Dotwork: Applying Blur 2")
             currentResultMat?.let { Imgproc.GaussianBlur(it, it, Size(3.0, 3.0), 0.0) }

            Timber.v("Dotwork: Blending Black Texture 1 (Overlay)")
            var blendResult4 = blendOverlay(currentResultMat, blackMat) // Uses fixed blendOverlay
            blendResult4?.let { if(it !== currentResultMat) matsToRelease.add(it); currentResultMat = it } ?: Timber.e("Blend 4 (Overlay) failed")

            Timber.v("Dotwork: Blending Black Texture 2 (Overlay)")
            var blendResult5 = blendOverlay(currentResultMat, blackMat) // Uses fixed blendOverlay
             blendResult5?.let { if(it !== currentResultMat) matsToRelease.add(it); currentResultMat = it } ?: Timber.e("Blend 5 (Overlay) failed")

            currentResultMat ?: run { Timber.e("currentResultMat became null"); return inputBitmap }
            val finalRgbaMat = Mat(); matsToRelease.add(finalRgbaMat)
            Imgproc.cvtColor(currentResultMat, finalRgbaMat, Imgproc.COLOR_BGR2RGBA)
            val finalBitmap = createBitmap(inputBitmap.width, inputBitmap.height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(finalRgbaMat, finalBitmap)
            Timber.d("Full Dotwork effect applied successfully.")
            return finalBitmap
            */
            // --- End Commented Out Original Logic ---

        } catch (e: Exception) {
            Timber.e(e, "Error applying simplified dotwork effect")
            return inputBitmap // Return original on error
        } finally {
            // Release all intermediate Mats collected
            Timber.v("Releasing ${matsToRelease.size} simplified dotwork Mats")
            matsToRelease.forEach { it.release() }
        }
    }
    // --- End Simplified Dotwork ---


    // --- Blend Mode Helpers (Private - Fixed Scalars, Revised Overlay) ---
    private fun blendScreen(base: Mat?, blend: Mat?): Mat? {
        // ... (Implementation remains the same as previous version - using 3-channel scalars) ...
        if (base == null || blend == null || base.size() != blend.size() || base.type() != blend.type()) {
            Timber.e("blendScreen: Invalid input mats. Base: ${base?.size()} ${base?.type()}, Blend: ${blend?.size()} ${blend?.type()}")
            return null
        }
        val result8U = Mat(base.rows(), base.cols(), base.type())
        val baseF = Mat(); val blendF = Mat(); val resultF = Mat();
        val one = Mat(base.rows(), base.cols(), CvType.CV_32FC3, Scalar(1.0, 1.0, 1.0))
        val term1 = Mat(); val term2 = Mat(); val product = Mat()
        val intermediates = mutableListOf(baseF, blendF, resultF, one, term1, term2, product)
        try {
            base.convertTo(baseF, CvType.CV_32FC3, 1.0 / 255.0)
            blend.convertTo(blendF, CvType.CV_32FC3, 1.0 / 255.0)
            Core.subtract(one, baseF, term1)
            Core.subtract(one, blendF, term2)
            Core.multiply(term1, term2, product)
            Core.subtract(one, product, resultF)
            resultF.convertTo(result8U, base.type(), 255.0)
        } catch(e: Exception) {
            Timber.e(e, "Error in blendScreen operation")
            result8U.release()
            intermediates.forEach { it.release() }
            return null
        } finally {
            intermediates.forEach { it.release() }
        }
        return result8U
    }

    private fun blendNormal(base: Mat?, blend: Mat?, opacity: Double = 1.0): Mat? {
        // ... (Implementation remains the same) ...
        if (base == null || blend == null || base.size() != blend.size() || base.type() != blend.type()) {
            Timber.e("blendNormal: Invalid input mats")
            return null
        }
        val result = Mat()
        try {
            Core.addWeighted(base, 1.0 - opacity, blend, opacity, 0.0, result)
        } catch (e: Exception) {
            Timber.e(e, "Error in blendNormal operation")
            result.release()
            return null
        }
        return result
    }

    // Overlay Blend (Revised to use copyTo with single-channel mask)
    private fun blendOverlay(base: Mat?, blend: Mat?): Mat? {
        if (base == null || blend == null || base.size() != blend.size() || base.type() != blend.type()) {
            Timber.e("blendOverlay: Invalid input mats")
            return null
        }
        // Ensure inputs are BGR (CV_8UC3)
        if (base.type() != CvType.CV_8UC3 || blend.type() != CvType.CV_8UC3) {
            Timber.e("blendOverlay requires CV_8UC3 input Mats. Base: ${base.type()}, Blend: ${blend.type()}")
            return null
        }

        val result8U = Mat() // Final result
        val baseF = Mat(); val blendF = Mat(); val resultF = Mat();
        val one = Mat(base.rows(), base.cols(), CvType.CV_32FC3, Scalar(1.0, 1.0, 1.0))
        val two = Mat(base.rows(), base.cols(), CvType.CV_32FC3, Scalar(2.0, 2.0, 2.0))
        val half = Mat(base.rows(), base.cols(), CvType.CV_32FC3, Scalar(0.5, 0.5, 0.5))
        val mask = Mat(); // CV_8UC1 mask for base < 0.5
        val invertedMask = Mat(); // CV_8UC1 mask for base >= 0.5
        val term1 = Mat(); val tempMul1 = Mat();
        val term2 = Mat(); val oneMinusBase = Mat(); val oneMinusBlend = Mat();
        val tempMul2 = Mat(); val twoTimesProduct = Mat();

        val intermediates = mutableListOf(baseF, blendF, resultF, one, two, half, mask, invertedMask,
            term1, tempMul1, term2, oneMinusBase, oneMinusBlend,
            tempMul2, twoTimesProduct)

        try {
            // Convert base and blend to float [0, 1]
            base.convertTo(baseF, CvType.CV_32FC3, 1.0 / 255.0)
            blend.convertTo(blendF, CvType.CV_32FC3, 1.0 / 255.0)

            // --- Calculate term1 = 2 * Base * Blend ---
            Core.multiply(baseF, blendF, tempMul1)
            Core.multiply(two, tempMul1, term1) // term1 is CV_32FC3

            // --- Calculate term2 = 1 - 2 * (1 - Base) * (1 - Blend) ---
            Core.subtract(one, baseF, oneMinusBase)
            Core.subtract(one, blendF, oneMinusBlend)
            Core.multiply(oneMinusBase, oneMinusBlend, tempMul2)
            Core.multiply(two, tempMul2, twoTimesProduct)
            Core.subtract(one, twoTimesProduct, term2) // term2 is CV_32FC3

            // --- Create single-channel CV_8U mask ---
            Core.compare(baseF, half, mask, Core.CMP_LT) // mask is CV_8UC1 (0 or 255 where base < 0.5)

            // Create inverted mask
            Core.bitwise_not(mask, invertedMask) // invertedMask is CV_8UC1 (0 or 255 where base >= 0.5)

            // --- Combine using copyTo with single-channel mask ---
            resultF.create(base.rows(), base.cols(), CvType.CV_32FC3) // Create destination for float result
            term1.copyTo(resultF, mask) // Copy term1 where mask is non-zero (base < 0.5)
            term2.copyTo(resultF, invertedMask) // Copy term2 where inverted mask is non-zero (base >= 0.5)

            // Convert final float result back to original type (e.g., CV_8UC3)
            resultF.convertTo(result8U, base.type(), 255.0)

        } catch (e: Exception) {
            Timber.e(e, "Error in revised blendOverlay (copyTo) operation")
            result8U.release()
            intermediates.forEach { it.release() }
            return null // Return null on error
        } finally {
            intermediates.forEach { it.release() } // Release all intermediates
        }
        return result8U
    }


}
