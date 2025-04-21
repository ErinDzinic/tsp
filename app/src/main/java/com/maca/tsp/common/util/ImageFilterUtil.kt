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
        colorMatrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, adjustedBrightness,
                0f, 1f, 0f, 0f, adjustedBrightness,
                0f, 0f, 1f, 0f, adjustedBrightness,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(original, 0f, 0f, paint)

        return result
    }

    fun applyExposure(bitmap: Bitmap, exposure: Float): Bitmap {
        val srcMat = Mat()
        val resultMat = Mat()
        val resultBitmap: Bitmap? = null
        try {
            Utils.bitmapToMat(bitmap, srcMat)
            val exposureMultiplier = 1.0 + exposure * 0.25
            srcMat.convertTo(resultMat, -1, exposureMultiplier, 0.0)
            // Create bitmap *before* releasing mats
            val tempResultBitmap = createBitmap(bitmap.width, bitmap.height)
            Utils.matToBitmap(resultMat, tempResultBitmap)
            return tempResultBitmap // Return the created bitmap
        } finally {
            srcMat.release()
            resultMat.release()
            // resultBitmap is returned or was never assigned if error before creation
        }
    }

    // Uses OpenCV - remains mostly the same logic, ensure releases
    fun applyContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        val srcMat = Mat()
        val resultMat = Mat()
        val resultBitmap: Bitmap? = null
        try {
            Utils.bitmapToMat(bitmap, srcMat)
            val safeContrast = contrast.coerceAtLeast(0.1f)
            srcMat.convertTo(
                resultMat,
                -1,
                safeContrast.toDouble(),
                ((1 - safeContrast) * 128).toDouble()
            )
            // Create bitmap *before* releasing mats
            val tempResultBitmap = createBitmap(bitmap.width, bitmap.height)
            Utils.matToBitmap(resultMat, tempResultBitmap)
            return tempResultBitmap // Return the created bitmap
        } finally {
            srcMat.release()
            resultMat.release()
        }
    }

    // Internal helper, unchanged
    private fun applyGammaToMat(srcMat: Mat, gamma: Float): Mat {
        val resultMat = Mat() // Create new Mat for result
        val lut = Mat(1, 256, CvType.CV_8U) // Create LUT Mat
        try {
            val safeGamma = if (gamma < 0.1f) 0.1f else gamma
            val gammaCorrection = 1.0f / safeGamma
            val lutData = ByteArray(256)
            for (i in 0..255) {
                lutData[i] =
                    (255.0 * (i / 255.0).pow(gammaCorrection.toDouble())).coerceIn(0.0, 255.0)
                        .toInt().toByte()
            }
            lut.put(0, 0, lutData)
            Core.LUT(srcMat, lut, resultMat) // Apply LUT
        } finally {
            lut.release() // Release LUT Mat
        }
        return resultMat // Return the new result Mat (caller must release)
    }

    // *** OPTIMIZED applyGamma ***
    fun applyGamma(original: Bitmap, gamma: Float): Bitmap {
        val rgbaMat = Mat()
        var gammaAppliedMat: Mat? = null // To hold result from helper
        val resultBitmap: Bitmap? = null
        try {
            Utils.bitmapToMat(original, rgbaMat) // Get RGBA Mat directly

            // Apply gamma using helper (operates on channels independently)
            // The helper returns a *new* Mat that needs release
            gammaAppliedMat = applyGammaToMat(rgbaMat, gamma)

            // Create result bitmap from the gamma-applied Mat
            val tempResultBitmap = createBitmap(original.width, original.height)
            Utils.matToBitmap(gammaAppliedMat, tempResultBitmap)
            return tempResultBitmap

        } finally {
            rgbaMat.release() // Release the source Mat
            gammaAppliedMat?.release() // Release the Mat returned by the helper
        }
    }

    // Uses OpenCV - ensure releases, preserveBackground adds complexity
    fun applySharpness(
        original: Bitmap,
        sharpness: Float,
        preserveBackground: Boolean = true
    ): Bitmap {
        val srcMat = Mat()
        val rgbMat = Mat() // Use intermediate RGB for processing consistency
        val resultMat = Mat()
        val resultRgbaMat = Mat() // For final conversion back
        var blurredMat: Mat? = null // Optional temp mat
        var grayMat: Mat? = null // Optional temp mat
        var mask: Mat? = null // Optional temp mat
        val resultBitmap: Bitmap? = null

        try {
            Utils.bitmapToMat(original, srcMat) // RGBA
            Imgproc.cvtColor(
                srcMat,
                rgbMat,
                Imgproc.COLOR_RGBA2RGB
            ) // Convert to RGB for processing

            if (sharpness <= 0f) {
                rgbMat.copyTo(resultMat) // Copy RGB if no sharpening
            } else {
                blurredMat = Mat()
                Imgproc.GaussianBlur(rgbMat, blurredMat, Size(3.0, 3.0), 0.0)
                Core.addWeighted(
                    rgbMat,
                    1.0 + sharpness * 1.5,
                    blurredMat,
                    (-sharpness * 1.5),
                    0.0,
                    resultMat
                )

                if (preserveBackground) {
                    grayMat = Mat()
                    mask = Mat()
                    Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY) // Use rgbMat
                    Imgproc.threshold(grayMat, mask, 240.0, 255.0, Imgproc.THRESH_BINARY)
                    rgbMat.copyTo(resultMat, mask) // Copy original RGB pixels back
                }
            }

            // Convert result back to RGBA
            Imgproc.cvtColor(resultMat, resultRgbaMat, Imgproc.COLOR_RGB2RGBA)

            // Create final bitmap
            val tempResultBitmap = createBitmap(original.width, original.height)
            Utils.matToBitmap(resultRgbaMat, tempResultBitmap)
            return tempResultBitmap

        } finally {
            // Release all Mats
            srcMat.release()
            rgbMat.release()
            resultMat.release()
            resultRgbaMat.release()
            blurredMat?.release()
            grayMat?.release()
            mask?.release()
        }
    }

    // Uses OpenCV - complex, ensure releases
    fun applySketchFilter(bitmap: Bitmap, details: Float, gamma: Float): Bitmap {
        val srcMat = Mat()
        val bgrMat = Mat() // Intermediate BGR
        val grayMat = Mat()
        val invertedMat = Mat()
        val blurredMat = Mat()
        val invertedBlurredMat = Mat()
        val sketchMat = Mat()
        var gammaAppliedSketchMat: Mat? = null // Result from helper
        val resultRgbaMat = Mat() // Final RGBA
        val resultBitmap: Bitmap? = null

        try {
            Utils.bitmapToMat(bitmap, srcMat) // RGBA
            Imgproc.cvtColor(srcMat, bgrMat, Imgproc.COLOR_RGBA2BGR) // To BGR

            Imgproc.cvtColor(bgrMat, grayMat, Imgproc.COLOR_BGR2GRAY) // To Gray

            Core.bitwise_not(grayMat, invertedMat) // Invert

            val kernelValue = (details * 1.5f).roundToInt().coerceAtLeast(1)
            val kernelSize = max(1, (kernelValue / 2) * 2 + 1)
            Imgproc.GaussianBlur(
                invertedMat,
                blurredMat,
                Size(kernelSize.toDouble(), kernelSize.toDouble()),
                0.0
            ) // Blur

            Core.bitwise_not(blurredMat, invertedBlurredMat) // Invert blurred

            Core.divide(grayMat, invertedBlurredMat, sketchMat, 255.0) // Dodge

            // Apply Gamma Correction (helper returns new Mat)
            gammaAppliedSketchMat = applyGammaToMat(sketchMat, gamma)

            // Convert final sketch back to RGBA
            Imgproc.cvtColor(gammaAppliedSketchMat, resultRgbaMat, Imgproc.COLOR_GRAY2RGBA)

            // Convert Mat to Bitmap
            val tempResultBitmap = createBitmap(bitmap.width, bitmap.height)
            Utils.matToBitmap(resultRgbaMat, tempResultBitmap)
            return tempResultBitmap

        } finally {
            // Release all Mats
            srcMat.release()
            bgrMat.release()
            grayMat.release()
            invertedMat.release()
            blurredMat.release()
            invertedBlurredMat.release()
            sketchMat.release()
            gammaAppliedSketchMat?.release()
            resultRgbaMat.release()
        }
    }

    // *** OPTIMIZED applyGaussianBlur ***
    fun applyGaussianBlur(original: Bitmap, radius: Float): Bitmap {
        val rgbaMat = Mat() // Input RGBA Mat
        val resultMat = Mat() // Output RGBA Mat
        val resultBitmap: Bitmap? = null
        try {
            Utils.bitmapToMat(original, rgbaMat) // RGBA

            // Ensure kernel size is positive & odd
            val safeRadius = (radius.toInt().coerceAtLeast(1) / 2) * 2 + 1
            val kernelSize = Size(safeRadius.toDouble(), safeRadius.toDouble())

            // Apply GaussianBlur directly to RGBA Mat
            Imgproc.GaussianBlur(rgbaMat, resultMat, kernelSize, 0.0)

            // Create result bitmap from the blurred RGBA Mat
            val tempResultBitmap = createBitmap(original.width, original.height)
            Utils.matToBitmap(resultMat, tempResultBitmap)
            return tempResultBitmap

        } finally {
            rgbaMat.release()
            resultMat.release()
        }
    }


    // ML Kit - remains the same
    suspend fun removeBackground(source: Bitmap): Bitmap {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .enableRawSizeMask()
            .build()
        val segmenter: Segmenter = Segmentation.getClient(options)
        val inputImage = InputImage.fromBitmap(source, 0)

        return try {
            Timber.v("Starting background segmentation...")
            val resultMask = segmenter.process(inputImage).await()
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
            try {
                segmenter.close()
            } catch (e: Exception) {
                Timber.e(e, "Error closing segmenter")
            }
            Timber.v("Segmenter closed.")
        }
    }


    private fun resizeMat(inputMat: Mat, targetSize: Size): Mat? {
        if (inputMat.empty() || targetSize.width <= 0 || targetSize.height <= 0) {
            Timber.w("resizeMat: Invalid input Mat or target size.")
            return null
        }
        val outputMat = Mat()
        try {
            Imgproc.resize(inputMat, outputMat, targetSize, 0.0, 0.0, Imgproc.INTER_AREA)
        } catch (e: Exception) {
            Timber.e(e, "Error resizing Mat")
            outputMat.release() // Release if created but error occurred
            return null
        }
        return outputMat // Caller must release this
    }

    /**
     * Creates a new Mat of a specific size filled with a solid color.
     * IMPORTANT: The returned Mat must be released by the caller.
     */
    private fun createSolidColorMat(targetSize: Size, colorScalar: Scalar, matType: Int): Mat? {
        if (targetSize.width <= 0 || targetSize.height <= 0) {
            Timber.w("createSolidColorMat: Invalid target size.")
            return null
        }
        val mat = Mat(targetSize, matType, colorScalar)
        // Check if mat creation was successful (though constructor usually throws if error)
        if (mat.empty()) {
            Timber.e("Failed to create solid color Mat.")
            mat.release() // Attempt release just in case
            return null
        }
        return mat // Caller must release this
    }

    /**
     * Stipple effect implementation (from previous attempts, ensure it works).
     * Takes a GRAYSCALE Mat as input.
     * Returns a BGR Mat (black dots on white background).
     * IMPORTANT: The returned Mat must be released by the caller. Input Mat is not released.
     */
    private fun applyStippleEffectMat(grayMat: Mat, dotDensity: Float, dotSize: Float): Mat? {
        Timber.d("Applying Stipple (Mat): density=$dotDensity, size=$dotSize")
        if (dotSize < 1f || grayMat.empty() || grayMat.type() != CvType.CV_8UC1) {
            Timber.w("Invalid input for applyStippleEffectMat. DotSize: $dotSize, Empty: ${grayMat.empty()}, Type: ${grayMat.type()}")
            return null // Return null on invalid input
        }
        val dotSizeInt = dotSize.roundToInt().coerceAtLeast(1) // Ensure dot size is at least 1
        val radius = (dotSizeInt / 2.0).roundToInt().coerceAtLeast(1) // Ensure radius is at least 1
        val outputMat = Mat() // Create output Mat
        val intermediates = mutableListOf<Mat>() // List to hold intermediates for release
        intermediates.add(outputMat)

        try {
            // Create output Mat as BGR, initialized to white
            outputMat.create(grayMat.rows(), grayMat.cols(), CvType.CV_8UC3) // Use create
            outputMat.setTo(Scalar(255.0, 255.0, 255.0)) // Initialize to white

            val dotColor = Scalar(0.0, 0.0, 0.0)
            val rows = grayMat.rows()
            val cols = grayMat.cols()
            val grayData = ByteArray(rows * cols) // Buffer for pixel data

            // Get pixel data safely
            if (!grayMat.isContinuous) {
                Timber.w("GrayMat not continuous for stipple, cloning.")
                val continuousGray = grayMat.clone() // Clone to ensure continuity
                intermediates.add(continuousGray) // Add clone to intermediates list
                continuousGray.get(0, 0, grayData)
            } else {
                grayMat.get(0, 0, grayData)
            }

            // Stipple loop
            for (y in 0 until rows step dotSizeInt) {
                for (x in 0 until cols step dotSizeInt) {
                    val index = y * cols + x
                    if (index < grayData.size) {
                        val intensityByte = grayData[index]
                        val unsignedIntValue = intensityByte.toInt() and 0xFF
                        val intensity = unsignedIntValue / 255.0
                        // Using density value directly (can be > 1)
                        val probability = ((1.0 - intensity) * dotDensity).coerceIn(0.0, 1.0)

                        if (Random.nextFloat() < probability) {
                            val centerX = x + dotSizeInt / 2.0
                            val centerY = y + dotSizeInt / 2.0
                            val center = Point(centerX, centerY)
                            Imgproc.circle(outputMat, center, radius, dotColor, -1) // Draw filled circle
                        }
                    }
                }
            }
            Timber.d("Stipple effect (Mat) applied.")
            // Detach outputMat from intermediates for returning, caller manages release
            intermediates.remove(outputMat)
            return outputMat // Return the BGR Mat with dots

        } catch (e: Exception) {
            Timber.e(e, "Error during OpenCV stipple processing")
            return null // Return null on error
        } finally {
            intermediates.forEach { it.release() } // Release all intermediates
        }
    }


    // --- Blend Mode Implementations using OpenCV ---

    /**
     * Screen Blend Mode. Assumes input Mats are compatible (e.g., CV_8UC3 or CV_8UC4).
     * Returns a NEW Mat that must be released by the caller. Inputs are NOT released.
     */
    private fun blendScreen(base: Mat?, blend: Mat?): Mat? {
        if (base == null || blend == null || base.size() != blend.size() || base.type() != blend.type()) {
            Timber.e("blendScreen: Invalid input mats.")
            return null
        }
        val result = Mat()
        val baseF = Mat(); val blendF = Mat(); val resultF = Mat()
        val one = Mat(); val term1 = Mat(); val term2 = Mat(); val product = Mat()
        val intermediates = mutableListOf(baseF, blendF, resultF, one, term1, term2, product, result)

        try {
            // Create 'one' Mat with matching type and size
            val type32F = CvType.makeType(CvType.CV_32F, base.channels()) // Use makeType

            one.create(base.size(), type32F)
            one.setTo(Scalar.all(1.0))

            base.convertTo(baseF, type32F, 1.0 / 255.0)
            blend.convertTo(blendF, type32F, 1.0 / 255.0)

            Core.subtract(one, baseF, term1)       // term1 = 1 - base
            Core.subtract(one, blendF, term2)       // term2 = 1 - blend
            Core.multiply(term1, term2, product)    // product = (1 - base) * (1 - blend)
            Core.subtract(one, product, resultF)    // resultF = 1 - product
            resultF.convertTo(result, base.type(), 255.0)

            intermediates.remove(result) // Detach result for returning
            return result
        } catch (e: Exception) {
            Timber.e(e, "Error in blendScreen")
            return null
        } finally {
            intermediates.forEach { it.release() }
        }
    }

    /**
     * Normal Blend Mode (Alpha Blending). Assumes input Mats are compatible.
     * Returns a NEW Mat that must be released by the caller. Inputs are NOT released.
     */
    private fun blendNormal(base: Mat?, blend: Mat?, opacity: Double = 1.0): Mat? {
        if (base == null || blend == null || base.size() != blend.size() || base.type() != blend.type()) {
            Timber.e("blendNormal: Invalid input mats")
            return null
        }
        val result = Mat()
        try {
            val clampedOpacity = opacity.coerceIn(0.0, 1.0)
            Core.addWeighted(base, 1.0 - clampedOpacity, blend, clampedOpacity, 0.0, result)
            return result // Caller releases
        } catch (e: Exception) {
            Timber.e(e, "Error in blendNormal")
            result.release()
            return null
        }
    }

    /**
     * Overlay Blend Mode. Requires CV_8UC3 or CV_8UC4 input Mats.
     * Returns a NEW Mat that must be released by the caller. Inputs are NOT released.
     */
    private fun blendOverlay(base: Mat?, blend: Mat?): Mat? {
        if (base == null || blend == null || base.size() != blend.size() || base.type() != blend.type()) {
            Timber.e("blendOverlay: Invalid input mats")
            return null
        }
        if (base.channels() < 3 || blend.channels() < 3) {
            Timber.e("blendOverlay requires at least 3 channel input Mats.")
            return null
        }

        val result = Mat()
        // Declare Mats needed
        val baseF = Mat(); val blendF = Mat(); val resultF = Mat()
        val mask = Mat(); val invertedMask = Mat();
        // Removed 'half' Mat --> val half = Mat();
        val one = Mat(); val two = Mat()
        val term1 = Mat(); val tempMul1 = Mat()
        val term2 = Mat(); val oneMinusBase = Mat(); val oneMinusBlend = Mat();
        val tempMul2 = Mat(); val twoTimesProduct = Mat();
        val baseFChannels = mutableListOf<Mat>()

        // Add ALL local Mats (except result and split channels) to intermediates
        val intermediates = mutableListOf(
            result, baseF, blendF, resultF, mask, invertedMask, /*half,*/ one, two, // Removed half
            term1, tempMul1, term2, oneMinusBase, oneMinusBlend,
            tempMul2, twoTimesProduct
        )

        try {
            val type32F = CvType.makeType(CvType.CV_32F, base.channels())

            // *** REMOVED 'half' MAT CREATION ***
            // half.create(base.size(), type32F); half.setTo(Scalar.all(0.5))
            val halfScalar = Scalar(0.5) // *** USE SCALAR INSTEAD ***

            one.create(base.size(), type32F); one.setTo(Scalar.all(1.0))
            two.create(base.size(), type32F); two.setTo(Scalar.all(2.0))

            base.convertTo(baseF, type32F, 1.0 / 255.0)
            blend.convertTo(blendF, type32F, 1.0 / 255.0)

            // term1 = 2 * base * blend
            Core.multiply(baseF, blendF, tempMul1)
            Core.multiply(two, tempMul1, term1)

            // term2 = 1 - 2 * (1 - base) * (1 - blend)
            Core.subtract(one, baseF, oneMinusBase)
            Core.subtract(one, blendF, oneMinusBlend)
            Core.multiply(oneMinusBase, oneMinusBlend, tempMul2)
            Core.multiply(two, tempMul2, twoTimesProduct)
            Core.subtract(one, twoTimesProduct, term2)

            // Create mask where base < 0.5
            Core.split(baseF, baseFChannels)
            intermediates.addAll(baseFChannels) // Add split channels for release

            // *** CORRECTED COMPARE ***
            // Compare single channel against the scalar 0.5
            Core.compare(baseFChannels[0], halfScalar, mask, Core.CMP_LT)
            // *** END CORRECTION ***

            // Invert the mask
            Core.bitwise_not(mask, invertedMask)

            // Blend using the mask and the invertedMask
            resultF.create(base.size(), type32F)
            term1.copyTo(resultF, mask)
            term2.copyTo(resultF, invertedMask)

            // Convert back to original type
            resultF.convertTo(result, base.type(), 255.0)

            return result // Return the final result

        } catch (e: Exception) {
            Timber.e(e, "Error in blendOverlay")
            result.release()
            return null
        } finally {
            // Release all intermediates EXCLUDING the 'result' if it's being returned
            intermediates.forEach { mat ->
                if (mat !== result) { // Avoid double-releasing the return value
                    try { mat.release() } catch (e: Exception) { /* Log */ }
                }
            }
            // Release split channels explicitly
            baseFChannels.forEach { chan -> try { chan.release() } catch (e: Exception) { /* ignore */ } }
        }
    }

    /**
     * Hard Mix Blend Mode Simulation (Option B). Assumes CV_8UC3 or CV_8UC4 Mats.
     * Returns a NEW Mat that must be released by the caller. Inputs are NOT released.
     */
    private fun blendHardMix(base: Mat?, blend: Mat?): Mat? {
        if (base == null || blend == null || base.size() != blend.size() || base.type() != blend.type()) {
            Timber.e("blendHardMix: Invalid input mats.")
            return null
        }
        val sumMat = Mat()
        val resultMat = Mat()
        val intermediates = mutableListOf(sumMat, resultMat)
        try {
            // Add base and blend matrices element-wise
            Core.add(base, blend, sumMat)

            // Threshold the sum: pixels >= 255 become 255, others become 0
            Imgproc.threshold(sumMat, resultMat, 254.9, 255.0, Imgproc.THRESH_BINARY)

            intermediates.remove(resultMat) // Detach result
            return resultMat
        } catch (e: Exception) {
            Timber.e(e, "Error in blendHardMix")
            return null
        } finally {
            intermediates.forEach { it.release() }
        }
    }


    // --- Main Dotwork Effect Function ---

    /**
     * Applies the full multi-step dotwork effect based on iOS logic.
     * Handles texture loading/resizing and memory management.
     */
    fun applyDotworkEffect(
        inputBitmap: Bitmap,
        dotDensity: Float,
        dotSize: Float,
        grainTextureBitmap: Bitmap?, // Preloaded grain texture IS needed
        // blackTextureBitmap: Bitmap?, // Can be removed if always generating
        // grayTextureBitmap: Bitmap?   // Can be removed if always generating
    ): Bitmap? { // Removed black/gray bitmap params
        Timber.d("Applying FULL Dotwork Effect...")
        val matsToRelease = mutableListOf<Mat>()
        var finalBitmap: Bitmap? = null
        // Temp Mats for results of each step
        var grayMat: Mat? = null
        var stippleMat: Mat? = null
        var grainMat: Mat? = null
        var blackMat: Mat? = null
        var grayMatTexture: Mat? = null
        var blend1Mat: Mat? = null
        var blend2Mat: Mat? = null
        var blur1Mat: Mat? = null
        var hardMixMat: Mat? = null
        var blur2Mat: Mat? = null
        var overlay1Mat: Mat? = null
        var finalMat: Mat? = null


        try {
            // --- Input & Target Size ---
            val targetWidth = inputBitmap.width
            val targetHeight = inputBitmap.height
            if (targetWidth <= 0 || targetHeight <= 0) return null // Validate input bitmap size
            val targetSize = Size(targetWidth.toDouble(), targetHeight.toDouble())

            val inputMat = Mat()
            matsToRelease.add(inputMat)
            Utils.bitmapToMat(inputBitmap, inputMat)
            val targetType = inputMat.type() // e.g., CV_8UC4

            // --- Prepare Textures ---
            Timber.v("Preparing textures...")
            // 1. Grain Texture (Still needs to be loaded and resized)
            grainMat = grainTextureBitmap?.let { bitmapToMatAndResize(it, targetSize, targetType, "grain") }
            grainMat?.also { matsToRelease.add(it) } ?: run {
                Timber.e("Dotwork effect cannot proceed: Grain texture is missing or failed to process.")
                return null // Grain is essential
            }

            // 2. Generate Black Texture Mat
            blackMat = createSolidColorMat(targetSize, Scalar(0.0, 0.0, 0.0, 255.0), targetType)
            blackMat?.also { matsToRelease.add(it) } ?: run {
                Timber.e("Failed to generate black texture Mat.")
                return null
            }

            // 3. Generate Gray Texture Mat
            grayMatTexture = createSolidColorMat(targetSize, Scalar(128.0, 128.0, 128.0, 255.0), targetType)
            grayMatTexture?.also { matsToRelease.add(it) } ?: run {
                Timber.e("Failed to generate gray texture Mat.")
                return null
            }
            Timber.v("Textures prepared/generated.")


            // --- Pipeline Steps (4-13, same as before) ---
            // 1. Grayscale for Stipple
            grayMat = Mat()
            matsToRelease.add(grayMat)
            Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_BGRA2GRAY) // Assuming input is BGRA

            // 3. Stipple
            Timber.v("Applying stipple...")
            val stippleMatBGR = applyStippleEffectMat(grayMat, dotDensity, dotSize) // Returns BGR
            stippleMatBGR?.also { matsToRelease.add(it) } ?: return null

            stippleMat = Mat()
            matsToRelease.add(stippleMat)
            if (targetType == CvType.CV_8UC4) Imgproc.cvtColor(stippleMatBGR, stippleMat, Imgproc.COLOR_BGR2BGRA)
            else stippleMatBGR.copyTo(stippleMat)
            Timber.v("Stipple applied.")

            // 4. Blend Grain 1 (Screen 0.15)
            Timber.v("Blending Grain 1 (Screen)...")
            blend1Mat = blendScreen(stippleMat, grainMat)
            blend1Mat?.also { matsToRelease.add(it) } ?: return null
            Timber.v("Blend Grain 1 done.")

            // 5. Blend Grain 2 (Normal 0.55)
            Timber.v("Blending Grain 2 (Normal)...")
            blend2Mat = blendNormal(blend1Mat, grainMat, 0.55)
            blend2Mat?.also { matsToRelease.add(it) } ?: return null
            Timber.v("Blend Grain 2 done.")

            // 6. Blur 1 (Gaussian 0.5)
            Timber.v("Applying Blur 1...")
            blur1Mat = Mat()
            matsToRelease.add(blur1Mat)
            Imgproc.GaussianBlur(blend2Mat, blur1Mat, Size(3.0, 3.0), 0.0)
            Timber.v("Blur 1 done.")

            // 7. Blend Gray (Hard Mix)
            Timber.v("Blending Gray (Hard Mix)...")
            hardMixMat = blendHardMix(blur1Mat, grayMatTexture)
            hardMixMat?.also { matsToRelease.add(it) } ?: return null
            Timber.v("Blend Gray done.")

            // 8. Blur 2 (Gaussian 0.5)
            Timber.v("Applying Blur 2...")
            blur2Mat = Mat()
            matsToRelease.add(blur2Mat)
            Imgproc.GaussianBlur(hardMixMat, blur2Mat, Size(3.0, 3.0), 0.0)
            Timber.v("Blur 2 done.")

            // 9. Blend Black 1 (Overlay)
            Timber.v("Blending Black 1 (Overlay)...")
            overlay1Mat = blendOverlay(blur2Mat, blackMat)
            overlay1Mat?.also { matsToRelease.add(it) } ?: return null
            Timber.v("Blend Black 1 done.")

            // 10. Blend Black 2 (Overlay)
            Timber.v("Blending Black 2 (Overlay)...")
            finalMat = blendOverlay(overlay1Mat, blackMat)
            finalMat ?: return null // Check if finalMat is null
            Timber.v("Blend Black 2 done.")

            // --- Final Conversion ---
            Timber.v("Converting final Mat to Bitmap...")
            // Ensure Mat type matches Bitmap config (e.g., BGRA <-> ARGB_8888)
            val finalMatCorrectType = Mat() // Use a new Mat for potential conversion
            matsToRelease.add(finalMatCorrectType)
            if(finalMat.type() == CvType.CV_8UC3) { // Example: If final was BGR
                Imgproc.cvtColor(finalMat, finalMatCorrectType, Imgproc.COLOR_BGR2BGRA)
            } else if (finalMat.type() == CvType.CV_8UC4) { // If it was already BGRA (or RGBA)
                finalMat.copyTo(finalMatCorrectType)
            } else {
                Timber.e("Unsupported final Mat type for Bitmap conversion: ${CvType.typeToString(finalMat.type())}")
                return null // Cannot convert
            }
            finalMat.release() // Release the original finalMat

            // Now convert finalMatCorrectType which should be BGRA
            val tempFinalBitmap = createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(finalMatCorrectType, tempFinalBitmap)
            finalBitmap = tempFinalBitmap
            Timber.d("Full Dotwork effect applied successfully.")


        } catch (e: Exception) {
            Timber.e(e, "Error during full dotwork effect pipeline")
            finalBitmap = null
        } finally {
            // Release Mats
            Timber.d("Releasing ${matsToRelease.size} dotwork Mats.")
            matsToRelease.forEach {
                try { it.release() } catch (e: Exception) { Timber.e(e, "Error releasing mat") }
            }
            Timber.d("Dotwork Mats released.")
        }

        return finalBitmap
    }
    /** Helper to convert Bitmap, resize, and return Mat, adding to release list on success */
    private fun bitmapToMatAndResize(bitmap: Bitmap, targetSize: Size, targetType: Int, name: String): Mat? {
        val tempMat = Mat()
        val resizedMat: Mat?
        try {
            Utils.bitmapToMat(bitmap, tempMat) // Convert
            // Convert color if necessary (e.g., RGBA to BGR if targetType is CV_8UC3)
            val typeConvertedMat = Mat()
            if (tempMat.type() != targetType) {
                val code = when {
                    tempMat.type() == CvType.CV_8UC4 && targetType == CvType.CV_8UC3 -> Imgproc.COLOR_BGRA2BGR
                    tempMat.type() == CvType.CV_8UC3 && targetType == CvType.CV_8UC4 -> Imgproc.COLOR_BGR2BGRA
                    // Add other conversions if needed
                    else -> -1 // No conversion needed or supported
                }
                if (code != -1) Imgproc.cvtColor(tempMat, typeConvertedMat, code) else tempMat.copyTo(typeConvertedMat)
            } else {
                tempMat.copyTo(typeConvertedMat)
            }
            tempMat.release() // Release original temp mat

            resizedMat = resizeMat(typeConvertedMat, targetSize) // Resize
            typeConvertedMat.release() // Release type-converted mat
            if (resizedMat == null) {
                Timber.e("Failed to resize $name texture Mat.")
                return null
            }
            Timber.v("$name texture resized successfully.")
            return resizedMat // Caller adds to release list
        } catch (e: Exception) {
            Timber.e(e, "Error processing $name texture bitmap")
            tempMat.release() // Ensure release on error
            return null
        }
    }


}
