package com.maca.tsp.features.editimage.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.maca.tsp.data.enums.ImageFilterType
import com.maca.tsp.data.enums.filterOptions
import com.maca.tsp.designsystem.FilterButton
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun ImageFilterControls(
    selectedFilter: ImageFilterType?,
    onFilterSelected: (ImageFilterType) -> Unit,
    filterValue: Float,
    onValueChange: (Float) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TspTheme.colors.scrim)
            .padding(horizontal = TspTheme.spacing.spacing1_5)
            .padding(top = TspTheme.spacing.default, bottom = TspTheme.spacing.spacing0_5),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Slider(
                value = filterValue,
                onValueChange = onValueChange, // This can now directly pass the value
                enabled = selectedFilter != null,
                valueRange = -100f..100f,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = TspTheme.spacing.spacing1),
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
                modifier = Modifier.padding(start = TspTheme.spacing.spacing1,bottom = TspTheme.spacing.spacing1)
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TspTheme.spacing.spacing1_75),
            contentPadding = PaddingValues(horizontal = TspTheme.spacing.spacing0_5)
        ) {
            items(filterOptions.size) { index ->
                FilterButton(
                    buttonSize = TspTheme.spacing.spacing8_5,
                    filterType = filterOptions[index],
                    isSelected = filterOptions[index] == selectedFilter,
                    onClick = { onFilterSelected(filterOptions[index]) }
                )
            }
        }
    }
}
