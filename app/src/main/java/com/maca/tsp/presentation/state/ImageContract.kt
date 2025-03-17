package com.maca.tsp.presentation.state

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.maca.tsp.data.enums.ImageFilterType

class ImageContract {
    data class ImageViewState(
        val rawImage: Uri? = null,
        val transformedImage: Bitmap? = null,
        val displayBitmap: Bitmap? = null,
        val isMinimized: Boolean = false,
        val isCropping: Boolean = false,
        val isBlackAndWhite: Boolean = false,
        // Store values separately for each filter
        val selectedFilter: ImageFilterType? = null,
        val brightness: Float = 0f,
        val exposure: Float = 0f,
        val contrast: Float = 0f,
        val sharpness: Float = 0f,
        val gamma: Float = 0f
    ) : ViewState

    sealed class ImageEvent : ViewEvent {
        data class ImageSelected(val uri: Uri, val context: Context) : ImageEvent()
        data object IsMinimized : ImageEvent()
        // Crop Image
        data object StartCrop : ImageEvent()
        data class CropResult(val uri: Uri, val context: Context) : ImageEvent()
        data object CancelCrop : ImageEvent()
        // Toggle Black & White & Flip Image
        data class ToggleBlackAndWhite(val isEnabled: Boolean, val context: Context) : ImageEvent()
        data class FlipImage(val horizontal: Boolean, val context: Context) : ImageEvent()
        // Adjust filter values
        data class SelectFilter(val filterType: ImageFilterType) : ImageEvent()
        data class UpdateFilterValue(val filterType: ImageFilterType, val value: Float, val context: Context) : ImageEvent()

    }

    sealed class ImageEffect : ViewSideEffect {
        sealed class Navigation : ImageEffect() {
            data object ToImageDetails : Navigation()
        }
    }
}