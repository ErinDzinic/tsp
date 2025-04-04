package com.maca.tsp.features.editimage

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.graphics.withSave
import kotlin.math.min

@Composable
fun EditableImageCanvas(
    displayBitmap: Bitmap,
) {
    // Image transformations
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        displayBitmap.let { bitmap ->
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rotationChange ->
                            scale = (scale * zoom).coerceIn(0.5f, 3f) // Restrict zooming
                            offset = Offset(offset.x + pan.x, offset.y + pan.y) // Dragging
                            rotation += rotationChange // Update rotation
                        }
                    }
            ) {
                val canvasSize = size

                // Maintain A4 aspect ratio (210mm x 297mm)
                val a4Ratio = 210f / 297f
                val viewportWidth = canvasSize.width
                val viewportHeight = viewportWidth / a4Ratio

                // Ensure viewport height does not exceed the canvas height
                val adjustedHeight = viewportHeight.coerceAtMost(canvasSize.height)

                val left = (canvasSize.width - viewportWidth) / 2f
                val top = (canvasSize.height - adjustedHeight) / 2f

                val bitmapWidth = bitmap.width.toFloat()
                val bitmapHeight = bitmap.height.toFloat()
                val scaleFactor = min(viewportWidth / bitmapWidth, adjustedHeight / bitmapHeight)

                val centerX = viewportWidth / 2f
                val centerY = adjustedHeight / 2f

                with(drawContext.canvas.nativeCanvas) {
                    withSave {

                        // Translate to center viewport
                        translate(left + offset.x, top + offset.y)

                        // Rotation should be applied at the center of the viewport
                        rotate(rotation, centerX, centerY)

                        // Scale keeping image centered within A4
                        scale(scale * scaleFactor, scale * scaleFactor, centerX, centerY)

                        // Center image inside the A4 viewport
                        drawImage(
                            image = bitmap.asImageBitmap(),
                            topLeft = Offset(
                                (viewportWidth - bitmapWidth * scaleFactor) / 2f,
                                (adjustedHeight - bitmapHeight * scaleFactor) / 2f
                            )
                        )

                    }
                }
            }
        }
    }
}