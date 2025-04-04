package com.maca.tsp.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.maca.tsp.common.util.DispatcherProvider
import com.maca.tsp.common.util.ImageFilterUtils
import com.maca.tsp.common.util.ImageUtils.saveBitmapToUri
import com.maca.tsp.common.util.ImageUtils.uriToBitmap
import com.maca.tsp.data.enums.ControlMode
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
    // Keep an original bitmap to use as a base for all transformations
    private var originalBitmap: Bitmap? = null

    override fun setInitialState(): ImageViewState = ImageViewState()

    override fun handleEvents(event: ImageEvent) {
        when (event) {
            is ImageEvent.ImageSelected -> handleImageSelected(event)
            is ImageEvent.IsMinimized -> setState { copy(isMinimized = !isMinimized) }
            is ImageEvent.StartCrop -> startCropping()
            is ImageEvent.CropResult -> handleCropResult(event.uri, event.context)
            is ImageEvent.ToggleBlackAndWhite -> handleToggleBlackAndWhite(event.isEnabled)
            is ImageEvent.FlipImage -> handleFlipImage(event.horizontal, event.context)
            is ImageEvent.CancelCrop -> setState { copy(isCropping = false) }
            is ImageEvent.SelectFilter -> setState { copy(selectedFilter = event.filterType) }
            is ImageEvent.UpdateFilterValue -> updateFilterValue(event)
            is ImageEvent.ChangeControlMode -> {
                if (event.mode != ControlMode.BASIC) {
                    enableSketch()
                    setState {
                        copy(
                            brightness = 0f,
                            contrast = 1f,
                            exposure = 0f,
                            gamma = 1f,
                            sharpness = 0f,

                        )
                    }
                } else {
                    disableSketch()
                }
                setState { copy(controlMode = event.mode) }
            }

            ImageEvent.DisableSketch -> disableSketch()
            ImageEvent.EnableSketch -> enableSketch()
            ImageEvent.PrintButtonClicked -> {
                setState { copy(showPrintDialog = true) }
            }
            ImageEvent.PrintDialogDismissed -> {
                setState { copy(showPrintDialog = false) }
            }
            is ImageEvent.PrintTypeSelected -> {
                // First, save the current state, then prepare for print
                setState { copy(showPrintDialog = false, selectedPrintType = event.printType) }
                // Trigger saving BEFORE proceeding to print preparation
                setEvent(ImageEvent.SaveCanvasStateRequested)
                // The actual print preparation will be triggered after successful save
                // (See SaveCanvasStateRequested handler)
            }
            ImageEvent.SaveCanvasStateRequested -> {
                setEffect { ImageEffect.Navigation.ToPrintPreview }
            }
        }
    }

    private fun handleImageSelected(event: ImageEvent.ImageSelected) {
        val bitmap = uriToBitmap(event.context, event.uri)
        originalBitmap = bitmap // Store the original for future transformations
        setState {
            copy(
                rawImage = event.uri,
                displayBitmap = bitmap,
                transformedImage = bitmap,
                selectedFilter = null,
                brightness = 0f,
                exposure = 1f,
                contrast = 1f,
                sharpness = 0f,
                gamma = 1f,
                isBlackAndWhite = false
            )
        }
        setEffect { ImageEffect.Navigation.ToImageDetails }
    }

    private fun startCropping() {
        setState { copy(isCropping = true) }
    }

    //TODO Pitaj RIKIJA!: Kada se kropa/flipa slika da li ona postaje originnalna? Te filteri se vracaju na nulu?

    private fun handleCropResult(uri: Uri, context: Context) {
        val croppedBitmap = uriToBitmap(context, uri)
        originalBitmap = croppedBitmap

        setState {
            copy(
                rawImage = uri,
                transformedImage = croppedBitmap,
                displayBitmap = croppedBitmap,
                isCropping = false,
                // Reset filter values
                brightness = 0f,
                contrast = 1f,
                exposure = 1f,
                gamma = 1f,
                sharpness = 0f,
                isBlackAndWhite = false
            )
        }
    }

    private fun handleToggleBlackAndWhite(isEnabled: Boolean) {
        applyAllFilters(isEnabled)
    }

    private fun handleFlipImage(horizontal: Boolean, context: Context) {
        val currentImage = viewState.value.transformedImage ?: return

        val flippedBitmap = ImageFilterUtils.flipBitmap(currentImage, horizontal)
        originalBitmap = flippedBitmap

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
                sharpness = 0f,
                isBlackAndWhite = false
            )
        }
    }

    private fun updateFilterValue(event: ImageEvent.UpdateFilterValue) {
        val newState = when (event.filterType) {
            ImageFilterType.BRIGHTNESS -> viewState.value.copy(brightness = event.value)
            ImageFilterType.CONTRAST -> viewState.value.copy(contrast = event.value)
            ImageFilterType.EXPOSURE -> viewState.value.copy(exposure = event.value)
            ImageFilterType.GAMMA -> viewState.value.copy(gamma = event.value)
            ImageFilterType.SHARPNESS -> viewState.value.copy(sharpness = event.value)
            ImageFilterType.GAUSSIAN_BLUR -> viewState.value.copy(gaussianBlur = event.value)
        }

        setState { newState }
        applyAllFilters(viewState.value.isBlackAndWhite)
    }

    private fun applyAllFilters(blackAndWhite: Boolean) {
        val baseBitmap = originalBitmap ?: return
        var result = baseBitmap.copy(baseBitmap.config!!, true)

        // Brightness
        if (viewState.value.brightness != 0f) {
            result = ImageFilterUtils.applyBrightness(result, viewState.value.brightness)
        }

        // Exposure
        if (viewState.value.exposure != 1f) {
            result = ImageFilterUtils.applyExposure(result, viewState.value.exposure)
        }

        // Contrast
        if (viewState.value.contrast != 1f) {
            result = ImageFilterUtils.applyContrast(result, viewState.value.contrast)
        }

        // Gamma
        if (viewState.value.gamma != 1f) {
            result = ImageFilterUtils.applyGamma(result, viewState.value.gamma)
        }

        // Sharpness
        if (viewState.value.sharpness != 0f) {
            result = ImageFilterUtils.applySharpness(result, viewState.value.sharpness)
        }

        // Black & White
        if (blackAndWhite) {
            result = ImageFilterUtils.applyBlackAndWhiteFilter(result)
        }

        // Sketch
        if (viewState.value.isSketchEnabled) {
            result = ImageFilterUtils.applySketchFilter(result)
        }

        // Gaussian Blur/Details
        if (viewState.value.gaussianBlur > 1f) {
            result = ImageFilterUtils.applyGaussianBlur(result, viewState.value.gaussianBlur)
        }

        setState {
            copy(
                transformedImage = result,
                displayBitmap = result,
                isBlackAndWhite = blackAndWhite
            )
        }
    }

    private fun enableSketch() {
        setState { copy(isSketchEnabled = true, gamma = 1f) }
        applyAllFilters(viewState.value.isBlackAndWhite)
    }

    private fun disableSketch() {
        setState { copy(isSketchEnabled = false) }
        applyAllFilters(viewState.value.isBlackAndWhite)
    }
}