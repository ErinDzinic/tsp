package com.maca.tsp.common.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap

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

    // Method to adjust brightness
    fun adjustBrightness(original: Bitmap, brightness: Float): Bitmap {
        val result = createBitmap(original.width, original.height)
        val canvas = Canvas(result)
        val paint = Paint()

        // Normalize brightness value
        val adjustedBrightness = brightness / 255f

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

    // Method to adjust contrast
    fun adjustContrast(original: Bitmap, contrast: Float): Bitmap {
        val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        val scale = contrast
        val translate = (-.5f * scale + .5f) * 255f

        colorMatrix.set(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(original, 0f, 0f, paint)

        return result
    }

    fun applyBrightness(original: Bitmap, brightness: Float): Bitmap {
        val result = createBitmap(original.width, original.height)
        val canvas = Canvas(result)
        val paint = Paint()

        // Convert from -100...100 range to a reasonable brightness adjustment
        // Will give -127...127 range which is good for pixel adjustments
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
}