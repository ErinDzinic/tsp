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
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

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


    fun removeBackground(source: Bitmap): Bitmap {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE) // Or SINGLE_IMAGE_MODE
            .enableRawSizeMask()
            .build()
        val segmenter: Segmenter = Segmentation.getClient(options)

        val inputImage = InputImage.fromBitmap(source, 0)

        return try {
            val resultMask = segmenter.process(inputImage).result // Use await for suspend function

            val maskBuffer = resultMask.buffer
            val maskWidth = resultMask.width
            val maskHeight = resultMask.height

            // Create a new bitmap for the result, initially transparent
            val resultBitmap = createBitmap(source.width, source.height)
            resultBitmap.eraseColor(Color.TRANSPARENT) // Start with transparent background

            val pixels = IntArray(source.width * source.height)
            source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

            val resultPixels = IntArray(source.width * source.height)

            // Iterate through the mask and original bitmap
            for (y in 0 until maskHeight) {
                for (x in 0 until maskWidth) {
                    // The mask buffer contains confidence values from 0.0 to 1.0.
                    val confidence = maskBuffer.float // Read next float confidence
                    val originalPixelIndex = y * source.width + x // Map mask coords to bitmap coords

                    // If confidence is above a threshold, keep the original pixel
                    if (confidence > 0.5f && originalPixelIndex < pixels.size) { // Adjust threshold as needed
                        resultPixels[originalPixelIndex] = pixels[originalPixelIndex]
                    } else if (originalPixelIndex < resultPixels.size) {
                        // Otherwise, keep it transparent (already set by eraseColor)
                        resultPixels[originalPixelIndex] = Color.TRANSPARENT
                    }
                }
            }
            maskBuffer.rewind() // Rewind buffer if reusing maskBuffer

            resultBitmap.setPixels(resultPixels, 0, source.width, 0, 0, source.width, source.height)
            resultBitmap

        } catch (e: Exception) {
            // Handle ML Kit exceptions
            e.printStackTrace()
            source // Return original on error
        } finally {
            segmenter.close() // Close the segmenter
        }
    }}
