package com.maca.tsp.features.editimage.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maca.tsp.R
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun BlackAndWhiteSwitch(
    isBlackAndWhite: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .background(TspTheme.colors.scrim)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TspTheme.spacing.spacing2_5,
                    vertical = TspTheme.spacing.spacing0_5
                )
        ) {
            Text(
                stringResource(R.string.black_and_white_grayscale),
                color = TspTheme.colors.background,
                modifier = Modifier.padding(end = TspTheme.spacing.spacing1),
                style = TspTheme.typography.bodyLarge
            )
            Switch(
                checked = isBlackAndWhite,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun RemoveBackgroundSwitch(
    isRemoveBackground: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .background(TspTheme.colors.scrim)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TspTheme.spacing.spacing2_5,
                    vertical = TspTheme.spacing.spacing0_5
                )
        ) {
            Text(
                stringResource(R.string.remove_background),
                color = TspTheme.colors.background,
                modifier = Modifier.padding(end = TspTheme.spacing.spacing1),
                style = TspTheme.typography.bodyLarge
            )
            Switch(
                checked = isRemoveBackground,
                onCheckedChange = onCheckedChange
            )
        }
    }
}