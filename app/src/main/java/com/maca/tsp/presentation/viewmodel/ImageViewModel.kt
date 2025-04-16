package com.maca.tsp.presentation.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import com.maca.tsp.common.util.DispatcherProvider
import com.maca.tsp.common.util.ImageFilterUtils
import com.maca.tsp.common.util.ImageUtils.uriToBitmap
import com.maca.tsp.data.enums.ControlMode
import com.maca.tsp.data.enums.ImageFilterType
import com.maca.tsp.presentation.state.BaseViewModel
import com.maca.tsp.presentation.state.ImageContract.ImageEffect
import com.maca.tsp.presentation.state.ImageContract.ImageEvent
import com.maca.tsp.presentation.state.ImageContract.ImageViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis


@HiltViewModel
class ImageViewModel @Inject constructor(
    private val dispatcher: DispatcherProvider,
) : BaseViewModel<ImageEvent, ImageViewState, ImageEffect>(
    dispatcher
) {
    private var originalBitmap: Bitmap? = null
    private var filterJob: Job? = null

    // --- Initial State ---
    override fun setInitialState(): ImageViewState = ImageViewState(
        sketchDetails = 10f,
        sketchGamma = 1.0f,
         // Add processing flag
    )

    // --- Event Handling ---
    override fun handleEvents(event: ImageEvent) {
        // Don't cancel if already processing? Or allow cancellation? Let's allow cancellation.
        filterJob?.cancel() // Cancel previous job if a new event comes in quickly
        filterJob = viewModelScope.launch { // Use viewModelScope directly
            // Set processing flag immediately on the main thread for UI feedback
            withContext(dispatcher.main) { setState { copy(isProcessingFilters = true) } }
            try {
                // Handle events on the default dispatcher (can call suspend functions)
                withContext(dispatcher.default) {
                    when (event) {
                        is ImageEvent.ImageSelected -> handleImageSelected(event)
                        is ImageEvent.IsMinimized -> { // No filtering needed
                            withContext(dispatcher.main) { setState { copy(isMinimized = !isMinimized) } }
                        }
                        is ImageEvent.StartCrop -> startCropping() // No filtering needed here
                        is ImageEvent.CropResult -> handleCropResult(event.uri, event.context)
                        is ImageEvent.ToggleBlackAndWhite -> handleToggleBlackAndWhite(event.isEnabled)
                        is ImageEvent.ToggleRemoveBackground -> handleToggleRemoveBackground(event.isEnabled)
                        is ImageEvent.FlipImage -> handleFlipImage(event.horizontal, event.context)
                        is ImageEvent.CancelCrop -> { // No filtering needed
                            withContext(dispatcher.main) { setState { copy(isCropping = false) } }
                        }
                        is ImageEvent.SelectFilter -> handleSelectFilter(event.filterType)
                        is ImageEvent.UpdateFilterValue -> handleUpdateFilterValue(event)
                        is ImageEvent.ChangeControlMode -> handleChangeControlMode(event.mode)
                        ImageEvent.PrintButtonClicked -> { // No filtering needed here
                            withContext(dispatcher.main) { setState { copy(showPrintDialog = true) } }
                        }
                        ImageEvent.PrintDialogDismissed -> { // No filtering needed
                            withContext(dispatcher.main) { setState { copy(showPrintDialog = false) } }
                        }
                        is ImageEvent.PrintTypeSelected -> { // No filtering needed here
                            withContext(dispatcher.main) {
                                setState { copy(showPrintDialog = false, selectedPrintType = event.printType) }
                            }
                            setEvent(ImageEvent.SaveCanvasStateRequested) // Trigger next event
                        }
                        ImageEvent.SaveCanvasStateRequested -> { // No filtering needed
                            // Ensure effect is set on main thread if it interacts with UI directly
                            withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ToPrintPreview } }
                        }
                        is ImageEvent.SaveImageClicked -> { // No filtering needed here
                            viewState.value.displayBitmap?.let { bitmap ->
                                // Ensure effect is set on main thread
                                withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.SaveImageToGallery(bitmap, event.context) } }
                            } ?: withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ShowToast("No image to save.") } }
                        }
                        is ImageEvent.UpdateSketchDetails -> {
                            withContext(dispatcher.main) { setState { copy(sketchDetails = event.value) } }
                            recalculateFiltersForCurrentMode()
                        }
                        is ImageEvent.UpdateSketchGamma -> {
                            withContext(dispatcher.main) { setState { copy(sketchGamma = event.value) } }
                            recalculateFiltersForCurrentMode()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling event: $event")
                // Optionally show error effect
                // withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ShowToast("Error processing: ${e.message}") } }
            } finally {
                // Ensure processing flag is turned off on the main thread
                withContext(dispatcher.main) { setState { copy(isProcessingFilters = false) } }
            }
        }
    }

    // --- Event Handler Implementations ---

    private suspend fun handleImageSelected(event: ImageEvent.ImageSelected) {
        Timber.d("handleImageSelected START")
        val bitmap = try {
            withContext(dispatcher.io) { uriToBitmap(event.context, event.uri) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load bitmap from URI")
            withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ShowToast("Failed to load image") } }
            return
        }
        originalBitmap = bitmap
        withContext(dispatcher.main) {
            setState {
                setInitialState().copy(
                    rawImage = event.uri,
                    displayBitmap = bitmap,
                    transformedImage = null
                )
            }
            setEffect { ImageEffect.Navigation.ToImageDetails }
        }
        recalculateFiltersForCurrentMode()
        Timber.d("handleImageSelected END")
    }

    private fun startCropping() {
        // This only sets state, doesn't need background thread
        setState { copy(isCropping = true) }
    }

    private suspend fun handleCropResult(uri: Uri, context: Context) {
        Timber.d("handleCropResult START")
        val croppedBitmap = try {
            withContext(dispatcher.io) { uriToBitmap(context, uri) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load cropped bitmap from URI")
            withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ShowToast("Failed to process crop") } }
            setState { copy(isCropping = false) } // Ensure cropping state is reset
            return
        }
        originalBitmap = croppedBitmap
        withContext(dispatcher.main) {
            setState {
                setInitialState().copy(
                    rawImage = uri,
                    displayBitmap = croppedBitmap,
                    transformedImage = null,
                    isCropping = false
                )
            }
        }
        recalculateFiltersForCurrentMode()
        Timber.d("handleCropResult END")
    }

    private suspend fun handleChangeControlMode(mode: ControlMode) {
        Timber.d("handleChangeControlMode: $mode")
        val shouldEnableSketch = mode == ControlMode.ADVANCED
        withContext(dispatcher.main) {
            setState {
                copy(
                    controlMode = mode,
                    isSketchEnabled = shouldEnableSketch
                )
            }
        }
        recalculateFiltersForCurrentMode()
    }

    private suspend fun handleToggleBlackAndWhite(isEnabled: Boolean) {
        Timber.d("handleToggleBlackAndWhite: $isEnabled")
        withContext(dispatcher.main) { setState { copy(isBlackAndWhite = isEnabled) } }
        recalculateFiltersForCurrentMode()
    }

    private suspend fun handleToggleRemoveBackground(isEnabled: Boolean) {
        Timber.d("handleToggleRemoveBackground: $isEnabled")
        if (originalBitmap == null && isEnabled) {
            withContext(dispatcher.main) {
                setEffect { ImageEffect.Navigation.ShowToast("Please select an image first.") }
            }
            return
        }
        withContext(dispatcher.main) { setState { copy(isRemoveBackground = isEnabled) } }
        recalculateFiltersForCurrentMode()
    }

    private suspend fun handleFlipImage(horizontal: Boolean, context: Context) {
        Timber.d("handleFlipImage START")
        val baseBitmap = originalBitmap ?: return
        val flippedBitmap = try {
            withContext(dispatcher.default) {
                ImageFilterUtils.flipBitmap(baseBitmap, horizontal)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to flip bitmap")
            withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ShowToast("Failed to flip image") } }
            return
        }
        originalBitmap = flippedBitmap

        withContext(dispatcher.main) {
            setState {
                copy(
                    // Reset caches after flip, as base image changed
                    basicFilteredBitmap = null,
                    advancedFilteredBitmap = null,
                    transformedImage = null
                    // rawImage = withContext(dispatcher.io) { saveBitmapToUri(context, flippedBitmap) } // Optional URI update
                )
            }
        }
        recalculateFiltersForCurrentMode()
        Timber.d("handleFlipImage END")
    }

    private fun handleSelectFilter(filterType: ImageFilterType) {
        Timber.d("handleSelectFilter: $filterType")
        // Update state on Main thread
        setState { copy(selectedFilter = filterType) }
        // No recalculation needed here
    }

    private suspend fun handleUpdateFilterValue(event: ImageEvent.UpdateFilterValue) {
        Timber.d("handleUpdateFilterValue: ${event.filterType} = ${event.value}")
        withContext(dispatcher.main) {
            val newState = when (event.filterType) {
                ImageFilterType.BRIGHTNESS -> viewState.value.copy(brightness = event.value)
                ImageFilterType.CONTRAST -> viewState.value.copy(contrast = event.value)
                ImageFilterType.EXPOSURE -> viewState.value.copy(exposure = event.value)
                ImageFilterType.GAMMA -> viewState.value.copy(gamma = event.value)
                ImageFilterType.SHARPNESS -> viewState.value.copy(sharpness = event.value)
                ImageFilterType.GAUSSIAN_BLUR -> viewState.value.copy(gaussianBlur = event.value)
            }
            setState { newState }
        }
        recalculateFiltersForCurrentMode()
    }

    // --- Central Recalculation Logic ---
    private suspend fun recalculateFiltersForCurrentMode() {
        val baseBitmap = originalBitmap ?: run {
            Timber.w("recalculateFiltersForCurrentMode: originalBitmap is null")
            return
        }
        val currentState = viewState.value // Capture state for this run
        Timber.d("Recalculating filters for mode: ${currentState.controlMode}")

        // Indicate processing started
        withContext(dispatcher.main) { setState { copy(isProcessingFilters = true) } }

        var finalResult: Bitmap? = null
        val time = measureTimeMillis { // Measure execution time
            finalResult = try {
                withContext(dispatcher.default) { // Run on background thread

                    // --- Stage 1: BASIC ---
                    Timber.d("Applying BASIC filters...")
                    val stage1Result = applyBasicFilters(baseBitmap, currentState)
                    // Update cache (can stay on background thread)
                    setState { copy(basicFilteredBitmap = stage1Result) }
                    Timber.d("BASIC filters applied.")

                    if (currentState.controlMode == ControlMode.BASIC) {
                        Timber.d("Mode is BASIC, applying final adjustments.")
                        return@withContext applyFinalAdjustments(stage1Result, currentState)
                    }

                    // --- Stage 2: ADVANCED (Sketch) ---
                    Timber.d("Applying ADVANCED filters (Sketch)...")
                    val stage2Input = stage1Result // Use result from previous stage
                    val stage2Result = applyAdvancedFilters(stage2Input, currentState)
                    setState { copy(advancedFilteredBitmap = stage2Result) }
                    Timber.d("ADVANCED filters applied.")

                    if (currentState.controlMode == ControlMode.ADVANCED) {
                        Timber.d("Mode is ADVANCED, applying final adjustments.")
                        return@withContext applyFinalAdjustments(stage2Result, currentState)
                    }

                    // --- Stage 3: ANOTHER_MODE (Blur) ---
                    Timber.d("Applying ANOTHER_MODE filters (Blur)...")
                    val stage3Input = stage2Result // Use result from previous stage
                    val stage3Result = applyAnotherModeFilters(stage3Input, currentState)
                    // No separate cache needed for final stage before adjustments
                    Timber.d("ANOTHER_MODE filters applied.")

                    if (currentState.controlMode == ControlMode.ANOTHER_MODE) {
                        Timber.d("Mode is ANOTHER_MODE, applying final adjustments.")
                        return@withContext applyFinalAdjustments(stage3Result, currentState)
                    }

                    // Fallback if mode is somehow invalid
                    Timber.w("Reached end of recalculateFilters without matching mode, returning base.")
                    baseBitmap
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during filter recalculation for mode ${currentState.controlMode}")
                withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ShowToast("Error applying filters") } }
                null // Return null on error
            }
        }
        Timber.d("Filter recalculation took $time ms")

        // Update the display bitmap on the Main thread if successful
        if (finalResult != null) {
            withContext(dispatcher.main) {
                Timber.d("Updating displayBitmap.")
                setState {
                    copy(
                        transformedImage = finalResult, // Store final result
                        displayBitmap = finalResult    // Display final result
                    )
                }
            }
        }
        // Indicate processing finished
        withContext(dispatcher.main) { setState { copy(isProcessingFilters = false) } }
    }

    // --- Stage-Specific Filter Functions (with Error Handling) ---

    private suspend fun applyBasicFilters(inputBitmap: Bitmap, state: ImageViewState): Bitmap {
        return try {
            withContext(dispatcher.default) {
                var result = inputBitmap.copy(inputBitmap.config ?: Bitmap.Config.ARGB_8888, true)
                Timber.v("Applying Brightness: ${state.brightness}")
                if (state.brightness != 0f) result = ImageFilterUtils.applyBrightness(result, state.brightness)

                Timber.v("Applying Exposure: ${state.exposure}")
                if (state.exposure != 1f) result = ImageFilterUtils.applyExposure(result, state.exposure)

                Timber.v("Applying Contrast: ${state.contrast}")
                if (state.contrast != 1f) result = ImageFilterUtils.applyContrast(result, state.contrast)

                Timber.v("Applying Gamma: ${state.gamma}")
                if (state.gamma != 1f) result = ImageFilterUtils.applyGamma(result, state.gamma)

                Timber.v("Applying Sharpness: ${state.sharpness}")
                if (state.sharpness != 0f) result = ImageFilterUtils.applySharpness(result, state.sharpness)

                result
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in applyBasicFilters")
            inputBitmap // Return input on error
        }
    }

    private suspend fun applyAdvancedFilters(inputBitmap: Bitmap, state: ImageViewState): Bitmap {
        return try {
            withContext(dispatcher.default) {
                var result = inputBitmap // Start from previous stage result
                if (state.isSketchEnabled) {
                    Timber.v("Applying Sketch: details=${state.sketchDetails}, gamma=${state.sketchGamma}")
                    result = ImageFilterUtils.applySketchFilter(
                        result,
                        state.sketchDetails,
                        state.sketchGamma
                    )
                } else {
                    Timber.v("Skipping Sketch (isSketchEnabled=false)")
                }
                result
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in applyAdvancedFilters (Sketch)")
            inputBitmap // Return input on error
        }
    }

    private suspend fun applyAnotherModeFilters(inputBitmap: Bitmap, state: ImageViewState): Bitmap {
        return try {
            withContext(dispatcher.default) {
                var result = inputBitmap // Start from previous stage result
                if (state.gaussianBlur > 1f) { // Check default
                    Timber.v("Applying Gaussian Blur: ${state.gaussianBlur}")
                    result = ImageFilterUtils.applyGaussianBlur(result, state.gaussianBlur)
                } else {
                    Timber.v("Skipping Gaussian Blur (value <= 1)")
                }
                // Add other filters specific to ANOTHER_MODE here if any
                result
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in applyAnotherModeFilters")
            inputBitmap // Return input on error
        }
    }

    /** Applies final adjustments like B&W and Background Removal */
    private suspend fun applyFinalAdjustments(inputBitmap: Bitmap, state: ImageViewState): Bitmap {
        return try {
            withContext(dispatcher.default) {
                var result = inputBitmap

                if (state.isRemoveBackground) {
                    Timber.v("Applying Background Removal")
                    result = ImageFilterUtils.removeBackground(result) // Suspend function
                } else {
                    Timber.v("Skipping Background Removal")
                }

                if (state.isBlackAndWhite) {
                    Timber.v("Applying Black and White")
                    result = ImageFilterUtils.applyBlackAndWhiteFilter(result)
                } else {
                    Timber.v("Skipping Black and White")
                }
                result
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in applyFinalAdjustments")
            inputBitmap // Return input on error
        }
    }


    // --- Save to Gallery (remains the same) ---
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveBitmapToGallery(bitmap: Bitmap, context: Context) {
        // ... (implementation remains the same) ...
        val contentResolver = context.contentResolver
        val imageName = "FilteredImage_${System.currentTimeMillis()}.png"
        val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }

        viewModelScope.launch(dispatcher.io) { // Use IO dispatcher for file operations
            try {
                contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                            setEffect { ImageEffect.Navigation.ShowToast("Failed to save image.") }
                        } else {
                            setEffect { ImageEffect.Navigation.ShowToast("Image saved to gallery.") }
                        }
                    } ?: run {
                        setEffect { ImageEffect.Navigation.ShowToast("Failed to get output stream.") }
                        contentResolver.delete(uri, null, null) // Clean up entry if stream fails
                    }
                } ?: run {
                    setEffect { ImageEffect.Navigation.ShowToast("Failed to create gallery entry.") }
                }
            } catch (e: Exception) {
                setEffect { ImageEffect.Navigation.ShowToast("Error saving image: ${e.message}") }
            }
        }
    }
}