package com.maca.tsp.presentation.state

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.maca.tsp.data.enums.ControlMode
import com.maca.tsp.data.enums.ImageFilterType
import com.maca.tsp.data.enums.PrintType

class ImageContract {
    data class ImageViewState(
        val rawImage: Uri? = null,
        val isImageLoading: Boolean = false,
        val transformedImage: Bitmap? = null,
        val displayBitmap: Bitmap? = null,
        val isMinimized: Boolean = false,
        val isCropping: Boolean = false,
        val isBlackAndWhite: Boolean = false,
        val isRemoveBackground: Boolean = false,
        val isSketchEnabled: Boolean = false,
        val controlMode: ControlMode = ControlMode.BASIC,
        // Store values separately for each filter
        val selectedFilter: ImageFilterType? = ImageFilterType.GAMMA,
        val brightness: Float = 0f,
        val exposure: Float = 1f,
        val contrast: Float = 1f,
        val sharpness: Float = 0f,
        val gamma: Float = 1f,
        val gaussianBlur: Float = 1f,
        val currentBitmap: Bitmap? = null,
        val showPrintDialog: Boolean = false,
        val selectedPrintType: PrintType = PrintType.SINGLE,
        val sketchDetails: Float = 10f,
        val sketchGamma: Float = 1.0f,
        val basicFilteredBitmap: Bitmap? = null,
        val advancedFilteredBitmap: Bitmap? = null,
        val isProcessingFilters: Boolean = false,
        val isDotworkEnabled: Boolean = false,
        val dotDensity: Float = 0.2f,
        val dotSize: Float = 2f
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
        data class ToggleRemoveBackground(val isEnabled: Boolean) : ImageEvent()
        data class FlipImage(val horizontal: Boolean) : ImageEvent()
        // Adjust filter values
        data class SelectFilter(val filterType: ImageFilterType) : ImageEvent()
        data class UpdateFilterValue(val filterType: ImageFilterType, val value: Float, val context: Context) : ImageEvent()
        data class ChangeControlMode(val mode: ControlMode) : ImageEvent()
        data object PrintButtonClicked : ImageEvent()
        data class PrintTypeSelected(val printType: PrintType) : ImageEvent()
        data object PrintDialogDismissed : ImageEvent()
        data object SaveCanvasStateRequested : ImageEvent()
        data class SaveImageClicked(val context: Context) : ImageEvent()
        data class UpdateSketchDetails(val value: Float) : ImageEvent()
        data class UpdateSketchGamma(val value: Float) : ImageEvent()
        data class UpdateDotDensity(val value: Float) : ImageEvent()
        data class UpdateDotSize(val value: Float) : ImageEvent()
        data class ToggleDotwork(val isEnabled: Boolean) : ImageEvent()
    }

    sealed class ImageEffect : ViewSideEffect {
        sealed class Navigation : ImageEffect() {
            data object ToImageDetails : Navigation()
            data object ToPrintPreview : Navigation()
            data class ShowToast(val message: String) : ImageEffect()
            data class SaveImageToGallery(val bitmap: Bitmap, val context: Context) : ImageEffect()
        }
    }
}