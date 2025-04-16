package com.maca.tsp.features.printpreview.composables


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maca.tsp.R
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
            .heightIn(max = TspTheme.spacing.spacing30)
            .padding(horizontal = TspTheme.spacing.spacing10)
            .padding(top = TspTheme.spacing.spacing3),
        verticalArrangement = Arrangement.spacedBy(TspTheme.spacing.spacing1_5)
    ) {
        SecondaryButton(
            text = stringResource(R.string.print),
            onClick = onPrintClick,
            icon = R.drawable.ic_print,
            iconSize = TspTheme.spacing.spacing2_75
        )
        SecondaryButton(
            text = stringResource(R.string.save),
            onClick = onSaveClick,
            icon = R.drawable.ic_save,
            iconSize = TspTheme.spacing.spacing2_75
        )
        SecondaryButton(
            text = stringResource(R.string.exit),
            onClick = onExitClick,
            icon = R.drawable.ic_exit,
            iconSize = TspTheme.spacing.spacing2_75
        )
    }
}