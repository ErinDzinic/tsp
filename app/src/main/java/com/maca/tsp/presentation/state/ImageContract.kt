package com.maca.tsp.presentation.state

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

class ImageContract {
    data class ImageViewState(
        val rawImage: Uri? = null,
        val filteredImage: Bitmap? = null,
        val filteredImageUri: Uri? = null,
        val isMinimized: Boolean = false,
        val croppedImage: Uri? = null,
        val isCropping: Boolean = false,
        val isBlackAndWhite: Boolean = false
    ) : ViewState

    sealed class ImageEvent : ViewEvent {
        data class ImageSelected(val uri: Uri) : ImageEvent()
        data object IsMinimized : ImageEvent()
        data object StartCrop : ImageEvent()
        data class CropResult(val uri: Uri) : ImageEvent()
        data class ToggleBlackAndWhite(val isEnabled: Boolean, val context: Context) : ImageEvent()
    }

    sealed class ImageEffect : ViewSideEffect {
        sealed class Navigation : ImageEffect() {
            data object ToImageDetails : Navigation()
        }
    }
}