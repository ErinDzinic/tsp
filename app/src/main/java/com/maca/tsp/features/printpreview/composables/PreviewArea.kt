package com.maca.tsp.features.printpreview.composables

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.maca.tsp.data.enums.PrintType
import com.maca.tsp.features.printpreview.PrintableImageCanvas

/**
 * Displays the printable image within a clipped area representing the paper/sleeve.
 * The size of the preview area adjusts based on the selected PrintType.
 *
 * @param bitmap The bitmap image to display.
 * @param printType The selected print type, determining the preview area's aspect ratio/size.
 * @param onScaleChanged Callback invoked when the image scale changes within PrintableImageCanvas.
 * @param modifier Modifier for this composable.
 */
@Composable
fun PreviewArea(
    bitmap: Bitmap,
    printType: PrintType,
    onScaleChanged: (Float) -> Unit,
    onOffsetChanged: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // This clip will prevent content from being visible outside the box
        Box(
            modifier = Modifier
                .let {
                    when (printType) {
                        PrintType.SLEEVE -> Modifier
                            .fillMaxHeight(0.9f) // Maintain original ratios
                            .fillMaxWidth(0.7f)
                        else -> Modifier
                            .fillMaxWidth(1f) // Maintain original ratios
                            .aspectRatio(210f / 297f) // A4 Aspect Ratio
                    }
                }
                .clip(RectangleShape) // Clip to prevent content from spilling out
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // Assuming PrintableImageCanvas is defined elsewhere and handles zooming/panning
            PrintableImageCanvas(
                bitmap = bitmap,
                printType = printType,
                onScaleChanged = onScaleChanged,
                onOffsetChanged = onOffsetChanged
            )
        }
    }
}