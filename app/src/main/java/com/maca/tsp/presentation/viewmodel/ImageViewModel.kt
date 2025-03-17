package com.maca.tsp.presentation.viewmodel

import android.content.Context
import android.net.Uri
import com.maca.tsp.common.util.DispatcherProvider
import com.maca.tsp.common.util.ImageFilterUtils
import com.maca.tsp.common.util.ImageUtils.saveBitmapToUri
import com.maca.tsp.common.util.ImageUtils.uriToBitmap
import com.maca.tsp.data.enums.ImageFilterType
import com.maca.tsp.presentation.state.BaseViewModel
import com.maca.tsp.presentation.state.ImageContract.ImageEffect
import com.maca.tsp.presentation.state.ImageContract.ImageEvent
import com.maca.tsp.presentation.state.ImageContract.ImageViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class ImageViewModel @Inject constructor(
    private val dispatcher: DispatcherProvider,
) : BaseViewModel<ImageEvent, ImageViewState, ImageEffect>(
    dispatcher
) {

    override fun setInitialState(): ImageViewState = ImageViewState()

    override fun handleEvents(event: ImageEvent) {
        when (event) {
            is ImageEvent.ImageSelected -> handleImageSelected(event)
            is ImageEvent.IsMinimized -> setState { copy(isMinimized = !isMinimized) }
            is ImageEvent.StartCrop -> startCropping()
            is ImageEvent.CropResult -> handleCropResult(event.uri,event.context)
            is ImageEvent.ToggleBlackAndWhite -> handleToggleBlackAndWhite(event.isEnabled, event.context)
            is ImageEvent.FlipImage -> handleFlipImage(event.horizontal, event.context)
            is ImageEvent.CancelCrop -> setState { copy(isCropping = false) }
            is ImageEvent.SelectFilter -> setState { copy(selectedFilter = event.filterType) }
            is ImageEvent.UpdateFilterValue -> updateFilterValue(event)
        }
    }

    private fun handleImageSelected(event: ImageEvent.ImageSelected) {
        val bitmap = uriToBitmap(event.context, event.uri)
        setState {
            copy(
                rawImage = event.uri,
                displayBitmap = bitmap,
                transformedImage = bitmap,
                selectedFilter = null,
                brightness = 0f,
                exposure = 0f,
                contrast = 0f,
                sharpness = 0f,
                gamma = 0f
            )
        }
        setEffect { ImageEffect.Navigation.ToImageDetails }
    }

    private fun startCropping() {
        setState { copy(isCropping = true) }
    }

    private fun handleCropResult(uri: Uri, context: Context) {
        val croppedBitmap = uriToBitmap(context, uri)

        setState {
            copy(
                rawImage = uri,
                transformedImage = croppedBitmap,
                displayBitmap = croppedBitmap,
                isCropping = false,
                // Reset filter values
                brightness = 0f,
                contrast = 1f,
                exposure = 0f,
                gamma = 1f,
                sharpness = 0f
            )
        }
    }

    private fun handleToggleBlackAndWhite(isEnabled: Boolean, context: Context) {
        val currentBitmap = viewState.value.transformedImage ?: uriToBitmap(context, viewState.value.rawImage!!)

        val filteredBitmap = if (isEnabled && currentBitmap != null) {
            ImageFilterUtils.applyBlackAndWhiteFilter(currentBitmap)
        } else {
            currentBitmap
        }

        setState {
            copy(
                isBlackAndWhite = isEnabled,
                transformedImage = filteredBitmap,
                displayBitmap = if (isEnabled) filteredBitmap else uriToBitmap(context, viewState.value.rawImage!!),
                brightness = viewState.value.brightness
            )
        }
    }

    private fun handleFlipImage(horizontal: Boolean, context: Context) {
        val currentImage = viewState.value.transformedImage ?: uriToBitmap(context, viewState.value.rawImage ?: return)

        val flippedBitmap = ImageFilterUtils.flipBitmap(currentImage!!, horizontal)

        // Convert flipped image to Uri
        val flippedUri = saveBitmapToUri(context, flippedBitmap)

        setState {
            copy(
                rawImage = flippedUri,
                transformedImage = flippedBitmap,
                displayBitmap = flippedBitmap,
                brightness = 0f,
                contrast = 1f,
                exposure = 0f,
                gamma = 1f,
                sharpness = 0f
            )
        }
    }

    private fun updateFilterValue(event: ImageEvent.UpdateFilterValue) {
        // 1. Update the filter value in state
        setState {
            when(event.filterType) {
                ImageFilterType.BRIGHTNESS -> copy(brightness = event.value)
                ImageFilterType.EXPOSURE -> copy(exposure = event.value)
                ImageFilterType.CONTRAST -> copy(contrast = event.value)
                ImageFilterType.GAMMA -> copy(gamma = event.value)
                ImageFilterType.SHARPNESS -> copy(sharpness = event.value)
            }
        }

        // 2. Then reapply all filters
        applyAllFilters(event.context)
    }

    private fun applyAllFilters(context: Context) {
        // Get the latest state
        val currentState = viewState.value

        // Start with the base transformed image (after crop/flip)
        val baseImage = currentState.transformedImage ?:
        uriToBitmap(context, currentState.rawImage ?: return)

        // Create a copy to work with
        var resultBitmap = baseImage?.copy(baseImage.config!!, true)

        // Apply filters in sequence
        if (currentState.brightness != 0f) {
            resultBitmap = ImageFilterUtils.applyBrightness(resultBitmap!!, currentState.brightness)
        }

        // Apply other filters...
        // (Add similar logic for contrast, exposure, etc.)

        // Update only the display bitmap with the filtered result
        setState { copy(displayBitmap = resultBitmap) }
    }
}
