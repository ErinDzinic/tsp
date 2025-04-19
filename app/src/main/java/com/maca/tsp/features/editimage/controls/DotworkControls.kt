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
import com.maca.tsp.features.editimage.composables.BlackAndWhiteSwitch
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.presentation.state.ImageContract.ImageEvent
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun DotworkControls(
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
            modifier = Modifier.fillMaxWidth().padding(bottom = TspTheme.spacing.spacing1), // Add padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TspTheme.spacing.spacing1) // Add spacing
        ) {
            // --- Top Navigation Row ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TspTheme.colors.scrim) // Ensure background consistency
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TspTheme.spacing.spacing1),
                    modifier = Modifier.padding(TspTheme.spacing.spacing1)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TspTheme.spacing.spacing2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TspTheme.spacing.spacing0_5)
                    ) {
                        BackButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onEvent(ImageEvent.ToggleDotwork(false))
                                onEvent(ImageEvent.ChangeControlMode(ControlMode.POST_PROCESS))
                            }
                        )

                        TspCircularIconButton(
                            icon = painterResource(id = if (viewState.isMinimized) R.drawable.ic_maximise else R.drawable.ic_minimize),
                            backgroundColor = if (viewState.isMinimized) TspTheme.colors.colorPurple else TspTheme.colors.colorGrayishBlack,
                            iconColor = if (viewState.isMinimized) TspTheme.colors.darkYellow else TspTheme.colors.background,
                            onClick = { onEvent(ImageEvent.IsMinimized) }
                        )

                        SecondaryButton(
                            modifier = Modifier.weight(1f),
                            icon = R.drawable.ic_print,
                            text = stringResource(R.string.print), onClick = {
                                onEvent(ImageContract.ImageEvent.PrintButtonClicked)
                            })
                    }
                }
            }

            // --- Controls visible when not minimized ---
            if (!viewState.isMinimized) {

                // --- Dot Density Slider ---
                SingleFilterControl(
                    iconRes = R.drawable.ic_exit, // Replace with actual density icon
                    sliderValue = viewState.dotDensity,
                    filterValue = viewState.dotDensity, // Display value
                    valueRange = 0.1f..1.0f, // Adjust range as needed
                    onIconClick = { /* Reset density? */
                        onEvent(ImageEvent.UpdateDotDensity(0.3f)) // Example reset
                    },
                    onSliderChange = { newValue ->
                        onEvent(ImageEvent.UpdateDotDensity(newValue))
                    }
                )

                // --- Dot Size Slider ---
                SingleFilterControl(
                    iconRes = R.drawable.ic_close, // Replace with actual size icon
                    sliderValue = viewState.dotSize,
                    filterValue = viewState.dotSize, // Display value
                    valueRange = 2f..50f, // Adjust range as needed (iOS max was 50)
                    onIconClick = { /* Reset size? */
                        onEvent(ImageEvent.UpdateDotSize(2f)) // Example reset
                    },
                    onSliderChange = { newValue ->
                        onEvent(ImageEvent.UpdateDotSize(newValue))
                    }
                )

                BlackAndWhiteSwitch(
                    isBlackAndWhite = viewState.isDotworkEnabled
                ) {
                    onEvent(ImageEvent.ToggleDotwork(!viewState.isDotworkEnabled))
                }
            }
        }
    }
}