package com.maca.tsp.features.printpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.maca.tsp.features.printpreview.composables.ActionButtons
import com.maca.tsp.features.printpreview.composables.DimensionDisplay
import com.maca.tsp.features.printpreview.composables.PreviewArea
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.ui.theme.TspTheme

/**
 * Displays a preview of how the bitmap will be arranged for printing based on PrintType.
 * Assumes an A4 paper aspect ratio for the preview area.
 */
/**
 * Displays a preview of how the bitmap will be arranged for printing,
 * along with dimensions and action buttons.
 * Assumes an A4 paper aspect ratio for the preview area by default.
 */
@Composable
fun PrintPreviewCanvas(
    modifier: Modifier = Modifier,
    viewState: ImageContract.ImageViewState,
    onEvent: (ImageContract.ImageEvent) -> Unit, // Keep onEvent for actions
    onExitClick: () -> Unit
) {
    // State for scale is needed here because DimensionDisplay depends on it.
    var currentScale by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TspTheme.colors.colorPurple), // Use theme color
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main image preview area
        Box(
            modifier = Modifier
                .weight(1f) // Takes available space
                .fillMaxWidth(),
            contentAlignment = Alignment.Center // Center the PreviewArea box
        ) {
            viewState.displayBitmap?.let { bitmap ->
                PreviewArea(
                    bitmap = bitmap,
                    printType = viewState.selectedPrintType,
                    onScaleChanged = { scale -> currentScale = scale },
                    // Pass modifier if needed, e.g., for padding within the weighted Box
                    // modifier = Modifier.padding(TspTheme.spacing.spacing2)
                )
            }
            // Consider adding a placeholder here if bitmap is null
            // ?: Placeholder("No image loaded")
        }

        // Dimensions and buttons section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black) // Keep explicit background if desired, or use Theme
                .padding(bottom = TspTheme.spacing.spacing3), // Use theme spacing
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display dimensions only if bitmap exists
            viewState.displayBitmap?.let { bitmap ->
                DimensionDisplay(
                    bitmap = bitmap,
                    currentScale = currentScale
                    // No modifier needed here as alignment is handled by parent Column
                )
                // Divider below dimensions
                Divider(
                    color = TspTheme.colors.colorGrayishBlack, // Use theme color
                    thickness = TspTheme.spacing.extra_xxs // Use theme spacing
                )
            }

            Spacer(modifier = Modifier.height(TspTheme.spacing.spacing2)) // Use theme spacing

            // Action Buttons
            ActionButtons(
                onPrintClick = { /* TODO: onEvent(ImageContract.ImageEvent.PrintClicked) */ },
                onSaveClick = { /* TODO: onEvent(ImageContract.ImageEvent.SaveClicked) */ },
                onExitClick = { onExitClick.invoke() }
            )
        }
    }
}