package com.maca.tsp.features.editimage

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.maca.tsp.common.util.ImageUtils.saveBitmapToUri
import com.maca.tsp.common.util.rememberImagePicker
import com.maca.tsp.data.enums.ControlMode
import com.maca.tsp.designsystem.dialogs.PrintOptionsDialog
import com.maca.tsp.features.editimage.composables.ImageCanvasContainer
import com.maca.tsp.features.editimage.controls.BasicControls
import com.maca.tsp.features.editimage.controls.DotworkControls
import com.maca.tsp.features.editimage.controls.SecondControls
import com.maca.tsp.features.editimage.controls.ThirdControls
import com.maca.tsp.features.editimage.cropimage.CropImageScreen
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.presentation.state.ImageContract.ImageEvent
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun EditImageScreen(
    viewState: ImageContract.ImageViewState,
    onEvent: (ImageEvent) -> Unit
) {
    val context = LocalContext.current
    val pickImage = rememberImagePicker(
        onImageSelected = { uri -> onEvent(ImageEvent.ImageSelected(uri, context)) },
        onError = { it.printStackTrace() }
    )

    val heightFraction by animateFloatAsState(
        targetValue = if (viewState.isMinimized) 0.9f else 0.65f,
        animationSpec = tween(durationMillis = 300),
        label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TspTheme.colors.background)
    ) {
        ImageCanvasContainer(viewState, heightFraction)

        when (viewState.controlMode) {
            ControlMode.BASIC -> {
                BasicControls(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    viewState = viewState,
                    pickImage = { pickImage.invoke() },
                    context = context,
                    onEvent = onEvent
                )
            }

            ControlMode.ADVANCED -> {
                SecondControls(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    viewState = viewState, onEvent = onEvent
                )
            }

            ControlMode.POST_PROCESS -> {
                ThirdControls(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    showExposure = false,
                    viewState = viewState, onEvent = onEvent
                )
            }

            ControlMode.DOTWORK -> {
                DotworkControls(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    viewState = viewState,
                    onEvent = onEvent
                )
            }
        }
    }

    if (viewState.isCropping && viewState.rawImage != null) {
        CropImageScreen(
            imageUri = if (viewState.isBlackAndWhite) saveBitmapToUri(
                context,
                viewState.transformedImage!!
            )!! else viewState.rawImage,
            onCropSuccess = { croppedUri ->
                onEvent(ImageEvent.CropResult(croppedUri, context))
            },
            onCancel = { onEvent(ImageEvent.CancelCrop) }
        )
    }

    if (viewState.showPrintDialog) {
        PrintOptionsDialog(
            onDismissRequest = { onEvent(ImageEvent.PrintDialogDismissed) },
            onPrintTypeSelected = { printType ->
                onEvent(ImageEvent.PrintTypeSelected(printType))
            }
        )
    }
}