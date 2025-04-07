package com.maca.tsp.designsystem.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.maca.tsp.R
import com.maca.tsp.data.enums.PrintType
import com.maca.tsp.designsystem.SecondaryButton
import com.maca.tsp.designsystem.TspCircularIconButton
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun PrintOptionsDialog(
    onDismissRequest: () -> Unit,
    onPrintTypeSelected: (PrintType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                "Please choose print version",
                style = TspTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = TspTheme.spacing.spacing4)
            )
        },
        titleContentColor = TspTheme.colors.background,
        containerColor = TspTheme.colors.colorDarkGray,
        shape = RoundedCornerShape(TspTheme.spacing.spacing3),
        icon = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.align(Alignment.End)) {
                    TspCircularIconButton(
                        icon = painterResource(id = R.drawable.ic_close),
                        buttonSize = TspTheme.spacing.spacing6,
                        backgroundColor = TspTheme.colors.colorDarkGray,
                        iconColor = TspTheme.colors.background,
                        onClick = {},
                        modifier = Modifier
                            .padding(horizontal = TspTheme.spacing.spacing0_5)
                    )
                }

                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(
                        painterResource(R.drawable.ic_print),
                        contentDescription = null,
                        modifier = Modifier.size(TspTheme.spacing.spacing8),
                        tint = TspTheme.colors.background
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    TspTheme.spacing.spacing2
                )
            ) {
                SecondaryButton(
                    stringResource(R.string.print_single),
                    showIcon = false
                ) {
                    onPrintTypeSelected(PrintType.SINGLE)
                }

                SecondaryButton(stringResource(R.string.print_sleeve), showIcon = false) {
                    onPrintTypeSelected(PrintType.SLEEVE)
                }

                SecondaryButton(stringResource(R.string.print_big_piece), showIcon = false) {
                    onPrintTypeSelected(PrintType.BACK)
                }
            }
        }
    )
}