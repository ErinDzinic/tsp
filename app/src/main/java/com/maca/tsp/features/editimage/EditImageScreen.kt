package com.maca.tsp.features.editimage

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.maca.tsp.common.util.ImageUtils.saveBitmapToUri
import com.maca.tsp.common.util.rememberImagePicker
import com.maca.tsp.data.enums.ImageFilterType
import com.maca.tsp.features.editimage.composables.BlackAndWhiteSwitch
import com.maca.tsp.features.editimage.composables.ImageCanvasContainer
import com.maca.tsp.features.editimage.composables.ImageEditingControls
import com.maca.tsp.features.editimage.composables.ImageFilterControls
import com.maca.tsp.features.editimage.cropimage.CropImageScreen
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun EditImageScreen(
    viewState: ImageContract.ImageViewState,
    onEvent: (ImageContract.ImageEvent) -> Unit
) {
    val context = LocalContext.current
    val pickImage = rememberImagePicker(
        onImageSelected = { uri -> onEvent(ImageContract.ImageEvent.ImageSelected(uri, context)) },
        onError = { it.printStackTrace() }
    )

    val heightFraction by animateFloatAsState(
        targetValue = if (viewState.isMinimized) 0.9f else 0.65f,
        animationSpec = tween(durationMillis = 300),
        label = ""
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        ImageCanvasContainer(viewState, heightFraction)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
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

    if (viewState.isCropping && viewState.rawImage != null) {
        CropImageScreen(
            imageUri = if (viewState.isBlackAndWhite) saveBitmapToUri(
                context,
                viewState.transformedImage!!
            )!! else viewState.rawImage,
            onCropSuccess = { croppedUri ->
                onEvent(ImageContract.ImageEvent.CropResult(croppedUri, context))
            },
            onCancel = { onEvent(ImageContract.ImageEvent.CancelCrop) }
        )
    }
}