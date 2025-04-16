package com.maca.tsp.features.editimage.controls

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.maca.tsp.data.enums.ImageFilterType
import com.maca.tsp.features.editimage.composables.BlackAndWhiteSwitch
import com.maca.tsp.features.editimage.composables.ImageEditingControls
import com.maca.tsp.features.editimage.composables.ImageFilterControls
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun BasicControls(
    modifier: Modifier = Modifier,
    viewState: ImageContract.ImageViewState,
    pickImage: () -> Unit,
    context: Context,
    onEvent: (ImageContract.ImageEvent) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TspTheme.colors.scrim)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TspTheme.spacing.spacing0_5)
        ) {
            ImageEditingControls(viewState, pickImage, onEvent)

            if (!viewState.isMinimized) {
                BlackAndWhiteSwitch(viewState.isBlackAndWhite) { isChecked ->
                    onEvent(ImageContract.ImageEvent.ToggleBlackAndWhite(isChecked, context))
                }

                ImageFilterControls(
                    selectedFilter = viewState.selectedFilter,
                    onFilterSelected = { filterType ->
                        onEvent(ImageContract.ImageEvent.SelectFilter(filterType))
                    },
                    filterValue = when (viewState.selectedFilter) {
                        ImageFilterType.BRIGHTNESS -> viewState.brightness
                        ImageFilterType.EXPOSURE -> viewState.exposure
                        ImageFilterType.CONTRAST -> viewState.contrast
                        ImageFilterType.SHARPNESS -> viewState.sharpness
                        ImageFilterType.GAMMA -> viewState.gamma
                        else -> 0f
                    },
                    onValueChange = { value ->
                        onEvent(
                            ImageContract.ImageEvent.UpdateFilterValue(
                                viewState.selectedFilter ?: return@ImageFilterControls,
                                value,
                                context
                            )
                        )
                    }
                )
            }
        }
    }
}


