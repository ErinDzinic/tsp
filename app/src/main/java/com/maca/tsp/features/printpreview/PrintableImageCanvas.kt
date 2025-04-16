package com.maca.tsp.features.printpreview

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.graphics.withSave
import com.maca.tsp.data.enums.PrintType
import kotlin.math.abs
import kotlin.math.min

@Composable
fun PrintableImageCanvas(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    printType: PrintType,
    onScaleChanged: (Float) -> Unit,
    onOffsetChanged: (Offset) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Use this to enforce bounds
    val maxOffsetX = remember { mutableFloatStateOf(0f) }
    val maxOffsetY = remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.5f, 3f) // Keep your scale limits
                    scale = newScale
                    onScaleChanged(scale) // Update the scale in the parent composable

                    val newOffsetX = offset.x + pan.x
                    val newOffsetY = offset.y + pan.y

                    // Calculate how far the image potentially overflows based on scale difference from 1.
                    // Note: This calculation provides *a* bounding logic, maybe not pixel-perfect edge detection,
                    // but it's the logic leading to the crash.
                    val boundX = maxOffsetX.floatValue * (scale - 1f)
                    val boundY = maxOffsetY.floatValue * (scale - 1f)

                    // --- FIX STARTS HERE ---
                    // Use the absolute value to define the magnitude of the allowed offset range.
                    // This ensures the range for coerceIn is always valid [-magnitude, +magnitude].
                    val absBoundX = abs(boundX)
                    val absBoundY = abs(boundY)

                    offset = Offset(
                        x = newOffsetX.coerceIn(-absBoundX, absBoundX),
                        y = newOffsetY.coerceIn(-absBoundY, absBoundY) // Use absBoundY here
                    )
                    onOffsetChanged(offset)
                }
            }
    ) {
        val canvasSize = size

        // Update max offsets based on the *current* canvas size.
        // This assumes the image at scale=1 fits within this central area.
        maxOffsetX.floatValue = canvasSize.width / 2f
        maxOffsetY.floatValue = canvasSize.height / 2f

        // Draw the image... (rest of your canvas code is likely fine)
        with(drawContext.canvas.nativeCanvas) {
            withSave {
                // Calculate appropriate scaling for the image
                val bitmapWidth = bitmap.width.toFloat()
                val bitmapHeight = bitmap.height.toFloat()

                // Calculate initial scale factor to fit the image (with margin)
                // Consider if the 0.8f factor is always desired or should adapt
                val scaleFactor = min(
                    canvasSize.width / bitmapWidth,
                    canvasSize.height / bitmapHeight
                ) * 0.8f // Example margin

                // Center point
                val centerX = canvasSize.width / 2f
                val centerY = canvasSize.height / 2f

                // Apply translation (pan) + centering
                translate(offset.x + centerX, offset.y + centerY)

                // Apply scaling (initial fit * user zoom) around the *translated* center (0,0 after translate)
                // The pivot point for scale here is implicitly (0,0) relative to the translated origin
                scale(scale * scaleFactor, scale * scaleFactor) // Simplified scale call


                // Draw from the image's top-left corner relative to the (now scaled and translated) origin
                // To center it, offset by negative half of its dimensions *before* scaling applied in this step
                drawImage(
                    bitmap.asImageBitmap(),
                    Offset(-bitmapWidth / 2f, -bitmapHeight / 2f) // Offset to draw centered
                )
            }
        }

        // Draw grid... (rest of your canvas code)
        val dashPathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
            floatArrayOf(10f, 10f), 0f
        )
        // ... (grid drawing code remains the same) ...
        when (printType) {
            PrintType.SLEEVE -> {
                // Draw two horizontal lines dividing the canvas into thirds
                val sectionHeight = canvasSize.height / 3
                for (i in 1 until 3) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.5f),
                        start = Offset(0f, i * sectionHeight),
                        end = Offset(canvasSize.width, i * sectionHeight),
                        strokeWidth = 2f,
                        pathEffect = dashPathEffect
                    )
                }
            }
            PrintType.BACK -> {
                // Draw a 3x3 grid
                val colWidth = canvasSize.width / 3
                val rowHeight = canvasSize.height / 3

                // Draw vertical lines
                for (i in 1 until 3) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.5f),
                        start = Offset(i * colWidth, 0f),
                        end = Offset(i * colWidth, canvasSize.height),
                        strokeWidth = 2f,
                        pathEffect = dashPathEffect
                    )
                }

                // Draw horizontal lines
                for (i in 1 until 3) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.5f),
                        start = Offset(0f, i * rowHeight),
                        end = Offset(canvasSize.width, i * rowHeight),
                        strokeWidth = 2f,
                        pathEffect = dashPathEffect
                    )
                }
            }
            else -> Unit // No grid for SINGLE
        }
    }
}