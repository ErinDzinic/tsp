package com.maca.tsp.presentation.viewmodel

import android.content.Context
import com.maca.tsp.common.util.DispatcherProvider
import com.maca.tsp.common.util.ImageFilterUtils
import com.maca.tsp.common.util.ImageUtils.saveBitmapToUri
import com.maca.tsp.common.util.ImageUtils.uriToBitmap
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
            is ImageEvent.CropResult -> handleCropResult(event)
            is ImageEvent.ToggleBlackAndWhite -> handleToggleBlackAndWhite(event.isEnabled, event.context)
        }
    }

    private fun handleImageSelected(event: ImageEvent.ImageSelected) {
        setState {
            copy(
                rawImage = event.uri,
            )
        }
        setEffect { ImageEffect.Navigation.ToImageDetails }
    }

    private fun startCropping() {
        setState { copy(isCropping = true) }
    }

    private fun handleCropResult(event: ImageEvent.CropResult) {
        setState {
            copy(
                rawImage = event.uri,
                isCropping = false
            )
        }
    }

    private fun handleToggleBlackAndWhite(isEnabled: Boolean, context: Context) {
        val currentUri = viewState.value.rawImage

        currentUri?.let { uri ->
            val bitmap = uriToBitmap(context, uri)
            val filteredBitmap = if (isEnabled && bitmap != null) {
                ImageFilterUtils.applyBlackAndWhiteFilter(bitmap)
            } else {
                bitmap
            }

            // Convert filtered bitmap to Uri (You need a function for this)
            val filteredUri = filteredBitmap?.let { saveBitmapToUri(context, it) }

            setState {
                copy(
                    isBlackAndWhite = isEnabled,
                    filteredImage = filteredBitmap,
                    filteredImageUri = filteredUri
                )
            }
        }
    }
}
