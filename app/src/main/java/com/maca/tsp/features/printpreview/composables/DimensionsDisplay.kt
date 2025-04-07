package com.maca.tsp.features.printpreview.composables

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.maca.tsp.ui.theme.TspTheme

/**
 * Displays the calculated dimensions of the image based on its original size and current scale factor.
 * Shows dimensions in both centimeters and inches.
 *
 * @param bitmap The bitmap whose dimensions are being displayed.
 * @param currentScale The current scale factor applied to the image (e.g., from zooming).
 * @param modifier Modifier for this composable.
 */
@Composable
fun DimensionDisplay(
    bitmap: Bitmap,
    currentScale: Float,
    modifier: Modifier = Modifier
) {
    val dpi = 300f // Assuming a standard DPI

    val originalWidthInches = bitmap.width / dpi
    val originalHeightInches = bitmap.height / dpi

    val scaledWidthInches = originalWidthInches * currentScale
    val scaledHeightInches = originalHeightInches * currentScale

    val scaledWidthCm = scaledWidthInches * 2.54f
    val scaledHeightCm = scaledHeightInches * 2.54f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TspTheme.colors.colorDarkGray), // Use theme color
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(TspTheme.spacing.spacing3),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TspTheme.spacing.spacing1)
        ) {
            Text(
                text = String.format("%.2f x %.2f cm", scaledWidthCm, scaledHeightCm),
                color = TspTheme.colors.darkYellow, // Use theme color
                textAlign = TextAlign.Center,
                style = TspTheme.typography.titleLarge // Use theme typography
            )

            Text(
                text = String.format("%.2f x %.2f in", scaledWidthInches, scaledHeightInches),
                color = TspTheme.colors.colorMediumGray, // Use theme color
                textAlign = TextAlign.Center,
                style = TspTheme.typography.titleMedium // Use theme typography
            )
        }
    }
}