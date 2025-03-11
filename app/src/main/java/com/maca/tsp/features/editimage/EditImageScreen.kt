package com.maca.tsp.features.editimage

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.maca.tsp.R
import com.maca.tsp.common.util.rememberImagePicker
import com.maca.tsp.designsystem.SecondaryButton
import com.maca.tsp.designsystem.TspCircularIconButton
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun EditImageScreen(
    viewState: ImageContract.ImageViewState,
    onEvent: (ImageContract.ImageEvent) -> Unit
) {
    val pickImage = rememberImagePicker(
        onImageSelected = { uri -> onEvent(ImageContract.ImageEvent.ImageSelected(uri)) },
        onError = { it.printStackTrace() }
    )

    val heightFraction by animateFloatAsState(
        targetValue = if (viewState.isMinimized) 0.9f else 0.65f,
        animationSpec = tween(durationMillis = 300),
        label = "heightAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(heightFraction)
            ) {
                if (viewState.rawImage != null)
                    EditableImageCanvas(imageUri = viewState.rawImage)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(Color.Black)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(TspTheme.spacing.spacing1).padding(horizontal = TspTheme.spacing.spacing0_5)
                ) {
                    TspCircularIconButton(
                        modifier = Modifier.padding(end = TspTheme.spacing.spacing1),
                        icon = painterResource(id = R.drawable.path_9),
                        onClick = { pickImage.invoke() }
                    )
                    TspCircularIconButton(
                        modifier = Modifier.padding(end = TspTheme.spacing.spacing1),
                        icon = painterResource(id = R.drawable.ic_crop),
                        onClick = { }
                    )
                    TspCircularIconButton(
                        modifier = Modifier.padding(end = TspTheme.spacing.spacing1),
                        icon = painterResource(id = R.drawable.ic_crop),
                        onClick = { }
                    )
                    TspCircularIconButton(
                        icon = painterResource(id = if(viewState.isMinimized) R.drawable.ic_maximise else R.drawable.ic_minimize),
                        backgroundColor = if(viewState.isMinimized) TspTheme.colors.colorPurple else TspTheme.colors.colorGrayishBlack,
                        iconColor = if(viewState.isMinimized) TspTheme.colors.darkYellow else TspTheme.colors.background,
                        onClick = {
                            onEvent(ImageContract.ImageEvent.IsMinimized)
                        }
                    )
                    SecondaryButton("Next", onClick = { })
                }
            }
        }
    }
}

