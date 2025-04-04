package com.maca.tsp.features.editimage.composables

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
import com.maca.tsp.designsystem.SecondaryButton
import com.maca.tsp.designsystem.TspCircularIconButton
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun ImageEditingControls(
    viewState: ImageContract.ImageViewState,
    pickImage: () -> Unit,
    onEvent: (ImageContract.ImageEvent) -> Unit
) {
    val context = LocalContext.current

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
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = TspTheme.spacing.spacing0_5)
            ) {
                TspCircularIconButton(
                    modifier = Modifier.padding(end = TspTheme.spacing.spacing1),
                    icon = painterResource(id = R.drawable.path_9),
                    onClick = { pickImage.invoke() }
                )
                TspCircularIconButton(
                    modifier = Modifier.padding(end = TspTheme.spacing.spacing1),
                    icon = painterResource(id = R.drawable.ic_crop),
                    onClick = { onEvent(ImageContract.ImageEvent.StartCrop) }
                )
                TspCircularIconButton(
                    modifier = Modifier.padding(end = TspTheme.spacing.spacing1),
                    icon = painterResource(id = R.drawable.ic_flip),
                    onClick = { onEvent(ImageContract.ImageEvent.FlipImage(horizontal = true, context = context)) }
                )
                TspCircularIconButton(
                    icon = painterResource(id = if (viewState.isMinimized) R.drawable.ic_maximise else R.drawable.ic_minimize),
                    backgroundColor = if (viewState.isMinimized) TspTheme.colors.colorPurple else TspTheme.colors.colorGrayishBlack,
                    iconColor = if (viewState.isMinimized) TspTheme.colors.darkYellow else TspTheme.colors.background,
                    onClick = { onEvent(ImageContract.ImageEvent.IsMinimized) }
                )
                SecondaryButton(stringResource(R.string.next), onClick = {
                    onEvent(ImageContract.ImageEvent.ChangeControlMode(
                        ControlMode.ADVANCED))
                })
            }
        }
    }
}
