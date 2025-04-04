package com.maca.tsp.features.editimage.controls

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.maca.tsp.designsystem.TspCircularIconButton
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun SingleFilterControl(
    @DrawableRes iconRes: Int,
    sliderValue: Float,
    filterValue: Float,
    valueRange: ClosedFloatingPointRange<Float>?,
    onIconClick: () -> Unit,
    onSliderChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TspTheme.spacing.spacing1)
    ) {
        TspCircularIconButton(
            buttonSize = TspTheme.spacing.spacing3_75,
            modifier = Modifier.padding(horizontal = TspTheme.spacing.spacing1_5),
            icon = painterResource(id = iconRes),
            onClick = onIconClick
        )

        Slider(
            value = sliderValue,
            onValueChange = onSliderChange,
            valueRange = valueRange ?: 0f..1f,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = TspTheme.spacing.spacing1, horizontal = TspTheme.spacing.spacing3),
            colors = SliderDefaults.colors(
                disabledThumbColor = TspTheme.colors.colorGrayishBlack,
                thumbColor = TspTheme.colors.background,
                activeTrackColor = TspTheme.colors.darkYellow,
                inactiveTrackColor = TspTheme.colors.colorGrayishBlack
            )
        )

        Text(
            text = "%.0f".format(filterValue),
            color = TspTheme.colors.darkYellow,
            style = TspTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(start = TspTheme.spacing.spacing1, bottom = TspTheme.spacing.spacing1, top = TspTheme.spacing.spacing1)
        )
    }
}
