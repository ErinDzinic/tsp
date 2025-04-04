package com.maca.tsp.common.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.pow

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

    fun applyGamma(original: Bitmap, gamma: Float): Bitmap {
        val result = createBitmap(original.width, original.height)

        // Safe gamma value (avoid division by zero)
        val safeGamma = if (gamma < 0.1f) 0.1f else gamma

        // Create gamma array
        val gammaR = 1.0f / safeGamma
        val gammaG = 1.0f / safeGamma
        val gammaB = 1.0f / safeGamma

        // Build gamma lookup table (LUT)
        val redLUT = IntArray(256)
        val greenLUT = IntArray(256)
        val blueLUT = IntArray(256)

        for (i in 0..255) {
            redLUT[i] = (255.0 * (i / 255.0).pow(gammaR.toDouble())).toInt()
            greenLUT[i] = (255.0 * (i / 255.0).pow(gammaG.toDouble())).toInt()
            blueLUT[i] = (255.0 * (i / 255.0).pow(gammaB.toDouble())).toInt()
        }

        // Apply the matrix using OpenCV for better performance
        val srcMat = Mat()
        Utils.bitmapToMat(original, srcMat)
        Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB)

        // Split the image into channels
        val channels = ArrayList<Mat>()
        Core.split(srcMat, channels)

        // Apply gamma to each channel
        for (i in 0..2) {
            val lut = when (i) {
                0 -> blueLUT  // OpenCV uses BGR
                1 -> greenLUT
                else -> redLUT
            }

            val lookupTable = Mat(1, 256, CvType.CV_8U)
            val lutData = ByteArray(256)
            for (j in 0..255) {
                lutData[j] = lut[j].toByte()
            }
            lookupTable.put(0, 0, lutData)

            Core.LUT(channels[i], lookupTable, channels[i])
            lookupTable.release()
        }

        // Merge channels back
        Core.merge(channels, srcMat)
        Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGB2RGBA)

        // Convert back to bitmap
        Utils.matToBitmap(srcMat, result)

        // Release resources
        srcMat.release()
        for (channel in channels) {
            channel.release()
        }

        return result
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

            // Apply unsharp mask: result = original + sharpness * (original - blurred)
            Core.addWeighted(srcMat, 1.0 + sharpness * 1.5, blurredMat, (-sharpness * 1.5), 0.0, resultMat)

            // If preserving background, create a mask for very bright areas
            if (preserveBackground) {
                // Convert to grayscale to identify brightness
                val grayMat = Mat()
                Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGB2GRAY)

                // Create a mask for bright (background) pixels
                val mask = Mat()
                Imgproc.threshold(grayMat, mask, 240.0, 255.0, Imgproc.THRESH_BINARY)

                // Copy original pixels to result where mask is white
                srcMat.copyTo(resultMat, mask)

                // Release resources
                grayMat.release()
                mask.release()
            }

            blurredMat.release()
        }

        // Convert back to RGBA
        Imgproc.cvtColor(resultMat, resultMat, Imgproc.COLOR_RGB2RGBA)

        // Convert to Bitmap
        val resultBitmap = createBitmap(original.width, original.height)
        Utils.matToBitmap(resultMat, resultBitmap)

        // Clean up
        srcMat.release()
        resultMat.release()

        return resultBitmap
    }

    fun applySketchFilter(bitmap: Bitmap, intensity: Float = 0.7f, preserveBackground: Boolean = true): Bitmap {
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)

        // Convert to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Invert the grayscale image
        val invertedMat = Mat()
        Core.bitwise_not(grayMat, invertedMat)

        // Apply a smaller Gaussian blur radius
        val blurredMat = Mat()
        Imgproc.GaussianBlur(invertedMat, blurredMat, Size(11.0, 11.0), 0.0)

        // Invert the blurred image
        val invertedBlurredMat = Mat()
        Core.bitwise_not(blurredMat, invertedBlurredMat)

        // Create pencil sketch effect
        val sketchMat = Mat()
        Core.divide(grayMat, invertedBlurredMat, sketchMat, 220.0)

        // If preserveBackground is true, replace light pixels with white
        if (preserveBackground) {
            // Create a mask for light (background) pixels
            val mask = Mat()
            Imgproc.threshold(grayMat, mask, 220.0, 255.0, Imgproc.THRESH_BINARY)

            // Replace light pixels with white in the sketch
            sketchMat.setTo(org.opencv.core.Scalar(255.0), mask)

            mask.release()
        }

        // For a more subtle effect, blend with the original
        if (intensity < 1.0f) {
            val originalGray = Mat()
            Imgproc.cvtColor(srcMat, originalGray, Imgproc.COLOR_BGR2GRAY)

            val blendedMat = Mat()
            Core.addWeighted(sketchMat,
                intensity.toDouble(), originalGray, 1.0 - intensity, 0.0, blendedMat)

            sketchMat.release()
            val result = blendedMat.clone()
            blendedMat.release()
            originalGray.release()

            // Convert back to Bitmap
            val resultBitmap = createBitmap(bitmap.width, bitmap.height)
            Imgproc.cvtColor(result, result, Imgproc.COLOR_GRAY2BGR)
            Utils.matToBitmap(result, resultBitmap)

            // Release resources
            srcMat.release()
            grayMat.release()
            invertedMat.release()
            blurredMat.release()
            invertedBlurredMat.release()
            result.release()

            return resultBitmap
        } else {
            // Convert back to Bitmap
            val resultBitmap = createBitmap(bitmap.width, bitmap.height)
            Imgproc.cvtColor(sketchMat, sketchMat, Imgproc.COLOR_GRAY2BGR)
            Utils.matToBitmap(sketchMat, resultBitmap)

            // Release resources
            srcMat.release()
            grayMat.release()
            invertedMat.release()
            blurredMat.release()
            invertedBlurredMat.release()
            sketchMat.release()

            return resultBitmap
        }
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
}