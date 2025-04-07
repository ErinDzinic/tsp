package com.maca.tsp.features.printpreview.composables


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.maca.tsp.designsystem.SecondaryButton
import com.maca.tsp.ui.theme.TspTheme

/**
 * Displays the primary action buttons for the print preview screen.
 *
 * @param onPrintClick Lambda function to execute when the Print button is clicked.
 * @param onSaveClick Lambda function to execute when the Save button is clicked.
 * @param onExitClick Lambda function to execute when the Exit button is clicked.
 * @param modifier Modifier for this composable.
 */
@Composable
fun ActionButtons(
    onPrintClick: () -> Unit,
    onSaveClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(max = TspTheme.spacing.spacing30) // Use theme spacing
            .padding(horizontal = TspTheme.spacing.spacing10), // Use theme spacing
        verticalArrangement = Arrangement.spacedBy(TspTheme.spacing.spacing1_5) // Use theme spacing
    ) {
        SecondaryButton(
            text = "Print", // Pass text directly
            onClick = onPrintClick // Use passed lambda
        )

        SecondaryButton(
            text = "Save", // Pass text directly
            onClick = onSaveClick // Use passed lambda
        )

        SecondaryButton(
            text = "Exit",
            onClick = onExitClick // Use passed lambda
        )
    }
}