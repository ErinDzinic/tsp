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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.maca.tsp.R
import com.maca.tsp.data.enums.ControlMode
import com.maca.tsp.designsystem.BackButton
import com.maca.tsp.designsystem.SecondaryButton
import com.maca.tsp.designsystem.TspCircularIconButton
import com.maca.tsp.features.editimage.composables.RemoveBackgroundSwitch
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun SecondControls(
    modifier: Modifier = Modifier,
    viewState: ImageContract.ImageViewState,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TspTheme.colors.scrim)
                    .padding(TspTheme.spacing.spacing1)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = TspTheme.spacing.spacing0_5)
                ) {
                    BackButton(
                        modifier = Modifier.weight(1f).padding(end = TspTheme.spacing.spacing1),
                        onClick = {
                            onEvent(ImageContract.ImageEvent.ChangeControlMode(ControlMode.BASIC))
                        }
                    )

                    TspCircularIconButton(
                        icon = painterResource(id = if (viewState.isMinimized) R.drawable.ic_maximise else R.drawable.ic_minimize),
                        backgroundColor = if (viewState.isMinimized) TspTheme.colors.colorPurple else TspTheme.colors.colorGrayishBlack,
                        iconColor = if (viewState.isMinimized) TspTheme.colors.darkYellow else TspTheme.colors.background,
                        onClick = { onEvent(ImageContract.ImageEvent.IsMinimized) }
                    )

                    SecondaryButton(
                        modifier = Modifier.weight(1f).padding(start = TspTheme.spacing.spacing1),
                        text = stringResource(R.string.next),
                        onClick = { onEvent(ImageContract.ImageEvent.ChangeControlMode(ControlMode.POST_PROCESS)) }
                    )
                }
            }

            if (!viewState.isMinimized) {
                RemoveBackgroundSwitch(viewState.isRemoveBackground) { isChecked ->
                    onEvent(ImageContract.ImageEvent.ToggleRemoveBackground(isChecked))
                }

                SingleFilterControl(
                    iconRes = R.drawable.ic_edit,
                    sliderValue = viewState.sketchDetails,
                    filterValue = viewState.sketchDetails,
                    valueRange = 1f..50f,
                    onIconClick = { /* Maybe reset details? */ },
                    onSliderChange = { newValue ->
                        onEvent(ImageContract.ImageEvent.UpdateSketchDetails(newValue))
                    }
                )

                SingleFilterControl(
                    iconRes = R.drawable.ic_gamma,
                    sliderValue = viewState.sketchGamma,
                    filterValue = viewState.sketchGamma,
                    valueRange = 0.1f..3f,
                    onIconClick = { /* Maybe reset gamma? */ },
                    onSliderChange = { newValue ->
                        onEvent(ImageContract.ImageEvent.UpdateSketchGamma(newValue))
                    }
                )
            }
        }
    }
}