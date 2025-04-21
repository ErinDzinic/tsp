package com.maca.tsp.features.printpreview

// Import zIndex if you re-add it, but likely not needed
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.maca.tsp.data.enums.PrintType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

    Box(modifier = modifier.fillMaxSize()) { // Parent Box for layering

        // --- Layer 1: Interactive Image Canvas ---
        // Drawn first, so it's visually underneath the grid layer
        Canvas(
            modifier = Modifier
                .fillMaxSize() // Takes up the whole Box area
                .pointerInput(bitmap) { // Handles gestures
                    detectTransformGestures { centroid, pan, zoom, rotationChange ->
                        // --- Gesture Detection Logic (Corrected bounds) ---
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        val bitmapWidth = bitmap.width.toFloat()
                        val bitmapHeight = bitmap.height.toFloat()

                        val fitScale = min(
                            canvasWidth / bitmapWidth,
                            canvasHeight / bitmapHeight
                        )
                        val newScale = (scale * zoom).coerceIn(0.5f, 5f)
                        scale = newScale
                        onScaleChanged(scale)

                        val totalEffectiveScale = fitScale * newScale
                        val displayWidth = bitmapWidth * totalEffectiveScale
                        val displayHeight = bitmapHeight * totalEffectiveScale
                        val maxXBound = max(0f, (displayWidth - canvasWidth) / 2f)
                        val maxYBound = max(0f, (displayHeight - canvasHeight) / 2f)

                        val currentOffset = offset
                        offset = Offset(
                            x = (currentOffset.x + pan.x).coerceIn(-maxXBound, maxXBound),
                            y = (currentOffset.y + pan.y).coerceIn(-maxYBound, maxYBound)
                        )
                        onOffsetChanged(offset)
                        // --- End Gesture Detection ---
                    }
                }
                .graphicsLayer { // Apply transformations ONLY to this image canvas
                    translationX = offset.x
                    translationY = offset.y
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            // --- Drawing Logic (Draws ONLY the image) ---
            val canvasSize = size
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()

            // Calculate initial fitting scale based on this Canvas's size
            val fitScale = min(
                canvasSize.width / bitmapWidth,
                canvasSize.height / bitmapHeight
            )
            val bitmapDisplayWidth = (bitmapWidth * fitScale)
            val bitmapDisplayHeight = (bitmapHeight * fitScale)

            // Calculate top-left to center the initially fitted bitmap
            val dstTopLeft = Offset(
                x = (canvasSize.width - bitmapDisplayWidth) / 2f,
                y = (canvasSize.height - bitmapDisplayHeight) / 2f
            )

            // Draw the image using the correct overload
            drawImage(
                image = bitmap.asImageBitmap(),
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bitmap.width, bitmap.height),
                dstOffset = IntOffset(dstTopLeft.x.roundToInt(), dstTopLeft.y.roundToInt()),
                dstSize = IntSize(bitmapDisplayWidth.roundToInt(), bitmapDisplayHeight.roundToInt())
            )
            // --- End Drawing Logic ---
        }
        // --- End Layer 1 (Image) ---


        // --- Layer 2: Static Grid Canvas ---
        // Drawn second, so it's visually on top of the image canvas
        Canvas(modifier = Modifier.fillMaxSize()) { // Fills the same Box area
            // NO pointerInput or graphicsLayer here - this stays static

            val canvasSize = size
            val dashPathEffect = PathEffect.dashPathEffect(
                floatArrayOf(10f, 10f), 0f
            )

            // --- Grid Drawing Logic ---
            when (printType) {
                PrintType.SLEEVE -> {
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
                    val colWidth = canvasSize.width / 3
                    val rowHeight = canvasSize.height / 3
                    for (i in 1 until 3) { // Vertical
                        drawLine(
                            color = Color.Black.copy(alpha = 0.5f),
                            start = Offset(i * colWidth, 0f),
                            end = Offset(i * colWidth, canvasSize.height),
                            strokeWidth = 2f,
                            pathEffect = dashPathEffect
                        )
                    }
                    for (i in 1 until 3) { // Horizontal
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
            // --- End Grid Drawing Logic ---
        }
        // --- End Layer 2 (Grid) ---

    } // End Box
}