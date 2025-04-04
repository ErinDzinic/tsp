package com.maca.tsp.designsystem.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.maca.tsp.data.enums.PrintType

@Composable
fun PrintOptionsDialog(
    onDismissRequest: () -> Unit,
    onPrintTypeSelected: (PrintType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Print Type") },
        text = { Text("Choose the layout for printing:") },
        confirmButton = {}, // We use dismiss buttons for actions
        dismissButton = {
            // Column for options + Cancel
            Column {
                TextButton(onClick = { onPrintTypeSelected(PrintType.SINGLE) }) {
                    Text("Single Print (A4)")
                }
                TextButton(onClick = { onPrintTypeSelected(PrintType.SLEEVE) }) {
                    Text("Sleeve Print")
                }
                TextButton(onClick = { onPrintTypeSelected(PrintType.BACK) }) {
                    Text("Back Print (3x3)")
                }
                Divider()
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        }
    )
}