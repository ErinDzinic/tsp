package com.maca.tsp.features.printpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maca.tsp.data.enums.PrintType
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.ui.theme.TspTheme

/**
 * Displays a preview of how the bitmap will be arranged for printing based on PrintType.
 * Assumes an A4 paper aspect ratio for the preview area.
 */
@Composable
fun PrintPreviewCanvas(
    modifier: Modifier = Modifier,
    viewState: ImageContract.ImageViewState,
    onEvent: (ImageContract.ImageEvent) -> Unit
) {
    var currentScale by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TspTheme.colors.colorPurple),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main image preview area with clipping
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            viewState.displayBitmap?.let { bitmap ->
                // This clip will prevent content from being visible outside the box
                Box(
                    modifier = Modifier
                        .let {
                            when (viewState.selectedPrintType) {
                                PrintType.SLEEVE -> Modifier
                                    .fillMaxHeight(0.9f)
                                    .fillMaxWidth(0.25f)
                                    .clip(RectangleShape) // Clip to prevent content from spilling out
                                else -> Modifier
                                    .fillMaxWidth(0.8f)
                                    .aspectRatio(210f / 297f)
                                    .clip(RectangleShape) // Clip to prevent content from spilling out
                            }
                        }
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    PrintableImageCanvas(
                        bitmap = bitmap,
                        printType = viewState.selectedPrintType,
                        onScaleChanged = { scale -> currentScale = scale } // Callback for scale
                    )
                }
            }
        }

        // Dimensions and buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dynamic dimensions text
            viewState.displayBitmap?.let { bitmap ->
                // Calculate physical dimensions based on DPI (remains the same)
                val dpi = 300f
                val widthInches = bitmap.width / dpi
                val heightInches = bitmap.height / dpi
                val widthCm = widthInches * 2.54f
                val heightCm = heightInches * 2.54f

                // Calculate scaled pixel dimensions
                val scaledWidthPixels = (bitmap.width * currentScale).toInt()
                val scaledHeightPixels = (bitmap.height * currentScale).toInt()

                Text(
                    text = String.format(
                        "%.2fx%.2f cm\n%.2fx%.2f in\n(%d x %d px - Scaled)",
                        widthCm, heightCm, widthInches, heightInches, scaledWidthPixels, scaledHeightPixels
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons (remains the same)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = TspTheme.colors.colorPurple),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Print", color = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.Done, contentDescription = null, tint = Color.White)
                    }
                }

                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = TspTheme.colors.colorPurple),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Save", color = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = Color.White)
                    }
                }

                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Exit", color = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}