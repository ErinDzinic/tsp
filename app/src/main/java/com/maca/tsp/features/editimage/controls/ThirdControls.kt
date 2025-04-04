package com.maca.tsp.features.editimage.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.maca.tsp.R
import com.maca.tsp.data.enums.ControlMode
import com.maca.tsp.data.enums.ImageFilterType
import com.maca.tsp.designsystem.BackButton
import com.maca.tsp.designsystem.SecondaryButton
import com.maca.tsp.designsystem.TspCircularIconButton
import com.maca.tsp.features.editimage.composables.ImageFilterControls
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun ThirdControls(
    modifier: Modifier = Modifier,
    viewState: ImageContract.ImageViewState,
    showExposure: Boolean,
    onEvent: (ImageContract.ImageEvent) -> Unit
) {
    val context = LocalContext.current

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TspTheme.colors.scrim)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TspTheme.spacing.spacing1),
                    modifier = Modifier.padding(TspTheme.spacing.spacing1)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TspTheme.spacing.spacing2),
                        modifier = Modifier.padding(horizontal = TspTheme.spacing.spacing0_5)
                    ) {
                        BackButton(
                            modifier = Modifier.weight(0.3f),
                            onClick = {
                                onEvent(ImageContract.ImageEvent.ChangeControlMode(ControlMode.ADVANCED))
                            }
                        )

                        TspCircularIconButton(
                            modifier = Modifier.weight(0.15f),
                            icon = painterResource(id = if (viewState.isMinimized) R.drawable.ic_maximise else R.drawable.ic_minimize),
                            backgroundColor = if (viewState.isMinimized) TspTheme.colors.colorPurple else TspTheme.colors.colorGrayishBlack,
                            iconColor = if (viewState.isMinimized) TspTheme.colors.darkYellow else TspTheme.colors.background,
                            onClick = { onEvent(ImageContract.ImageEvent.IsMinimized) }
                        )

                        SecondaryButton(
                            modifier = Modifier.weight(0.3f),
                            icon = R.drawable.ic_print,
                            text = stringResource(R.string.print), onClick = {
                                onEvent(ImageContract.ImageEvent.PrintButtonClicked)
                            })
                    }
                }
            }

            if (!viewState.isMinimized) {
                ImageFilterControls(
                    selectedFilter = viewState.selectedFilter,
                    showExposure = showExposure,
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