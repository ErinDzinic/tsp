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
import kotlin.math.min

@Composable
fun PrintableImageCanvas(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    printType: PrintType,
    onScaleChanged: (Float) -> Unit // Callback to provide the scale
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
                    val newScale = (scale * zoom).coerceIn(0.5f, 3f)
                    scale = newScale
                    onScaleChanged(scale) // Update the scale in the parent composable

                    // Limit offset to stay within bounds
                    val newOffsetX = offset.x + pan.x
                    val newOffsetY = offset.y + pan.y

                    // Calculate how far we can move based on scale
                    val boundX = maxOffsetX.floatValue * (scale - 1f)
                    val boundY = maxOffsetY.floatValue * (scale - 1f)

                    offset = Offset(
                        x = newOffsetX.coerceIn(-boundX, boundX),
                        y = newOffsetY.coerceIn(-boundY, boundY)
                    )
                }
            }
    ) {
        val canvasSize = size

        // Update max offsets for boundary checking
        maxOffsetX.floatValue = canvasSize.width / 2
        maxOffsetY.floatValue = canvasSize.height / 2

        // Draw the image
        with(drawContext.canvas.nativeCanvas) {
            withSave {
                // Calculate appropriate scaling for the image
                val bitmapWidth = bitmap.width.toFloat()
                val bitmapHeight = bitmap.height.toFloat()
                val scaleFactor = min(
                    canvasSize.width / bitmapWidth,
                    canvasSize.height / bitmapHeight
                ) * 0.8f // Leave some margin

                // Center the image
                val centerX = canvasSize.width / 2f
                val centerY = canvasSize.height / 2f

                translate(offset.x + centerX, offset.y + centerY)
                scale(scale * scaleFactor, scale * scaleFactor, 0f, 0f)

                // Draw from center
                drawImage(
                    bitmap.asImageBitmap(),
                    Offset(-bitmapWidth / 2f, -bitmapHeight / 2f)
                )
            }
        }

        // Draw grid with dashed lines
        val dashPathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
            floatArrayOf(10f, 10f), 0f
        )

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