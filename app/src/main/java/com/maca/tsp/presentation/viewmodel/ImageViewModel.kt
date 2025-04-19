package com.maca.tsp.presentation.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import com.maca.tsp.R
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis


@HiltViewModel
class ImageViewModel @Inject constructor(
    private val dispatcher: DispatcherProvider,
    @ApplicationContext private val appContext: Context // Inject Application Context
) : BaseViewModel<ImageEvent, ImageViewState, ImageEffect>(
    dispatcher
) {
    private var originalBitmap: Bitmap? = null
    private var filterJob: Job? = null

    // --- Preloaded Textures ---
    private var grainTexture1: Bitmap? = null
    private var grainTexture2: Bitmap? = null
    private var blackTexture: Bitmap? = null

    init {
        // Preload textures on initialization
        viewModelScope.launch(dispatcher.io) {
            loadTexture(R.drawable.grain_texture_1)?.let { grainTexture1 = it }
            loadTexture(R.drawable.grain_texture_2)?.let { grainTexture2 = it }
            loadTexture(R.drawable.grain_texture_3)?.let { blackTexture = it }
            if (grainTexture1 == null || grainTexture2 == null || blackTexture == null) {
                Timber.w("Failed to load one or more dotwork textures!")
                // Optionally set an effect to notify UI?
            } else {
                Timber.d("Dotwork textures loaded successfully.")
            }
        }
    }

    /** Helper function to load textures safely */
    private fun loadTexture(@DrawableRes resId: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inScaled = false }
            BitmapFactory.decodeResource(appContext.resources, resId, options)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load texture resource: $resId")
            null
        }
    }

    // --- Initial State ---
    override fun setInitialState(): ImageViewState = ImageViewState(
        brightness = 0f, contrast = 1f, exposure = 1f, gamma = 1f, sharpness = 0f,
        gaussianBlur = 1f, sketchDetails = 10f, sketchGamma = 1.0f,
        isDotworkEnabled = false, dotDensity = 0.3f, dotSize = 4f,
        basicFilteredBitmap = null, advancedFilteredBitmap = null, isProcessingFilters = false
    )

    // --- Event Handling ---
    override fun handleEvents(event: ImageEvent) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            withContext(dispatcher.main) { setState { copy(isProcessingFilters = true) } }
            try {
                withContext(dispatcher.default) {
                    when (event) {
                        is ImageEvent.ImageSelected -> handleImageSelected(event)
                        is ImageEvent.IsMinimized -> {
                            withContext(dispatcher.main) { setState { copy(isMinimized = !isMinimized) } }
                        }
                        is ImageEvent.StartCrop -> startCropping()
                        is ImageEvent.CropResult -> handleCropResult(event.uri, event.context)
                        is ImageEvent.ToggleBlackAndWhite -> handleToggleBlackAndWhite(event.isEnabled)
                        is ImageEvent.ToggleRemoveBackground -> handleToggleRemoveBackground(event.isEnabled)
                        is ImageEvent.FlipImage -> handleFlipImage(event.horizontal)
                        is ImageEvent.CancelCrop -> {
                            withContext(dispatcher.main) { setState { copy(isCropping = false) } }
                        }
                        is ImageEvent.SelectFilter -> handleSelectFilter(event.filterType)
                        is ImageEvent.UpdateFilterValue -> handleUpdateFilterValue(event)
                        is ImageEvent.ChangeControlMode -> handleChangeControlMode(event.mode)
                        ImageEvent.PrintButtonClicked -> {
                            withContext(dispatcher.main) { setState { copy(showPrintDialog = true) } }
                        }
                        ImageEvent.PrintDialogDismissed -> {
                            withContext(dispatcher.main) { setState { copy(showPrintDialog = false) } }
                        }
                        is ImageEvent.PrintTypeSelected -> {
                            withContext(dispatcher.main) {
                                setState { copy(showPrintDialog = false, selectedPrintType = event.printType) }
                            }
                            setEvent(ImageEvent.SaveCanvasStateRequested)
                        }
                        ImageEvent.SaveCanvasStateRequested -> {
                            withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ToPrintPreview } }
                        }
                        is ImageEvent.SaveImageClicked -> {
                            viewState.value.displayBitmap?.let { bitmap ->
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
                        // --- Stipple Event Handlers ---
                        is ImageEvent.UpdateDotDensity -> {
                            // Only update value, enable happens via ToggleDotwork
                            withContext(dispatcher.main) { setState { copy(dotDensity = event.value) } }
                            if (viewState.value.isDotworkEnabled) recalculateFiltersForCurrentMode()
                        }
                        is ImageEvent.UpdateDotSize -> {
                            // Only update value, enable happens via ToggleDotwork
                            withContext(dispatcher.main) { setState { copy(dotSize = event.value) } }
                            if (viewState.value.isDotworkEnabled) recalculateFiltersForCurrentMode()
                        }
                        is ImageEvent.ToggleDotwork -> { // Handler for explicit toggle
                            withContext(dispatcher.main) { setState { copy(isDotworkEnabled = event.isEnabled) } }
                            recalculateFiltersForCurrentMode()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling event: $event")
                withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ShowToast("Error: ${e.message}") } }
            } finally {
                withContext(dispatcher.main) { setState { copy(isProcessingFilters = false) } }
            }
        }
    }

    // --- Event Handler Implementations ---
    // ... (handleImageSelected, startCropping, handleCropResult, handleToggleBlackAndWhite, handleToggleRemoveBackground, handleFlipImage, handleSelectFilter, handleUpdateFilterValue, handleChangeControlMode remain the same) ...
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
                setInitialState().copy( // Reset state completely
                    rawImage = event.uri,
                    displayBitmap = bitmap, // Display original initially
                    transformedImage = null // Clear transformed/cache
                )
            }
            setEffect { ImageEffect.Navigation.ToImageDetails }
        }
        recalculateFiltersForCurrentMode() // Calculate initial filters
        Timber.d("handleImageSelected END")
    }

    private fun startCropping() {
        // Update state on Main thread
        setState { copy(isCropping = true) }
    }

    private suspend fun handleCropResult(uri: Uri, context: Context) {
        Timber.d("handleCropResult START")
        val croppedBitmap = try {
            withContext(dispatcher.io) { uriToBitmap(context, uri) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load cropped bitmap from URI")
            withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ShowToast("Failed to process crop") } }
            // Ensure state is updated even on error
            withContext(dispatcher.main) { setState { copy(isCropping = false) } }
            return
        }
        originalBitmap = croppedBitmap
        withContext(dispatcher.main) {
            setState {
                setInitialState().copy( // Reset state
                    rawImage = uri,
                    displayBitmap = croppedBitmap, // Display cropped initially
                    transformedImage = null,
                    isCropping = false
                )
            }
        }
        recalculateFiltersForCurrentMode() // Recalculate filters for cropped image
        Timber.d("handleCropResult END")
    }

    private suspend fun handleToggleBlackAndWhite(isEnabled: Boolean) {
        Timber.d("handleToggleBlackAndWhite: $isEnabled")
        // Update state on Main thread
        withContext(dispatcher.main) { setState { copy(isBlackAndWhite = isEnabled) } }
        recalculateFiltersForCurrentMode() // Recalculate on default dispatcher
    }

    private suspend fun handleToggleRemoveBackground(isEnabled: Boolean) {
        Timber.d("handleToggleRemoveBackground: $isEnabled")
        if (originalBitmap == null && isEnabled) {
            withContext(dispatcher.main) {
                setEffect { ImageEffect.Navigation.ShowToast("Please select an image first.") }
            }
            return
        }
        // Update state on Main thread
        withContext(dispatcher.main) { setState { copy(isRemoveBackground = isEnabled) } }
        recalculateFiltersForCurrentMode() // Recalculate on default dispatcher
    }

    private suspend fun handleFlipImage(horizontal: Boolean) {
        Timber.d("handleFlipImage START")
        val baseBitmap = originalBitmap ?: return
        val flippedBitmap = try {
            withContext(dispatcher.default) { // Use default for flip CPU work
                ImageFilterUtils.flipBitmap(baseBitmap, horizontal)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to flip bitmap")
            withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ShowToast("Failed to flip image") } }
            return
        }
        originalBitmap = flippedBitmap // Update the base original bitmap

        // Update state on Main thread - Reset caches
        withContext(dispatcher.main) {
            setState {
                copy(
                    basicFilteredBitmap = null,
                    advancedFilteredBitmap = null,
                    transformedImage = null
                    // Optionally update rawImage URI if needed
                    // rawImage = withContext(dispatcher.io) { saveBitmapToUri(context, flippedBitmap) }
                )
            }
        }
        recalculateFiltersForCurrentMode() // Recalculate all filters on default dispatcher
        Timber.d("handleFlipImage END")
    }

    private fun handleSelectFilter(filterType: ImageFilterType) {
        Timber.d("handleSelectFilter: $filterType")
        // Update state on Main thread
        setState { copy(selectedFilter = filterType) }
        // No recalculation needed here, happens on value change
    }

    private suspend fun handleUpdateFilterValue(event: ImageEvent.UpdateFilterValue) {
        Timber.d("handleUpdateFilterValue: ${event.filterType} = ${event.value}")
        // Update state on Main thread
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
        recalculateFiltersForCurrentMode() // Recalculate on default dispatcher
    }


    // --- Corrected handleChangeControlMode (No Reset) ---
    private suspend fun handleChangeControlMode(mode: ControlMode) {
        Timber.d("handleChangeControlMode: $mode")
        val shouldEnableSketch = mode == ControlMode.ADVANCED
        // Update isDotworkEnabled based on whether the NEW mode is DOTWORK
        // Keep existing isDotworkEnabled state unless explicitly changed by Toggle event
        // val shouldEnableDotwork = mode == ControlMode.DOTWORK // Don't automatically enable/disable here

        withContext(dispatcher.main) {
            // Update state: set new mode and sketch UI status
            setState {
                copy(
                    controlMode = mode,
                    isSketchEnabled = shouldEnableSketch // Controls UI for sketch sliders
                    // isDotworkEnabled = shouldEnableDotwork // Let ToggleDotwork handle this
                )
            }
        }
        // Always recalculate filters after mode change
        recalculateFiltersForCurrentMode()
    }
    // --- End Corrected handleChangeControlMode ---


    // --- Central Recalculation Logic (Staged Caching) ---
    private suspend fun recalculateFiltersForCurrentMode() {
        val baseBitmap = originalBitmap ?: run {
            Timber.w("recalculateFiltersForCurrentMode: originalBitmap is null")
            return
        }
        // Capture state at the beginning of the calculation
        val currentState = viewState.value
        Timber.d("Recalculating filters for mode: ${currentState.controlMode}")

        // Indicate processing started (moved to handleEvents start)
        // withContext(dispatcher.main) { setState { copy(isProcessingFilters = true) } }

        var finalResult: Bitmap? = null
        val time = measureTimeMillis { // Measure execution time
            finalResult = try {
                // Run calculations on background thread (dispatcher.default)
                withContext(dispatcher.default) {

                    // --- Stage 1: BASIC ---
                    Timber.d("Applying BASIC filters...")
                    // TODO: Optimization - Use cached basicFilteredBitmap if available and basic params haven't changed
                    val stage1Result = applyBasicFilters(baseBitmap, currentState)
                    // Update cache (can stay on background thread)
                    // Be careful updating state directly from background thread
                    // Consider sending an update event or using thread-safe state mechanism if needed
                    // For now, direct update (might cause issues if read/write overlap)
                    setState { copy(basicFilteredBitmap = stage1Result) }
                    Timber.d("BASIC filters applied.")

                    // If current mode is BASIC, apply final adjustments and finish
                    if (currentState.controlMode == ControlMode.BASIC) {
                        Timber.d("Mode is BASIC, applying final adjustments.")
                        return@withContext applyFinalAdjustments(stage1Result, currentState)
                    }

                    // --- Stage 2: ADVANCED (Sketch) ---
                    Timber.d("Applying ADVANCED filters (Sketch)...")
                    val stage2Input = stage1Result // Use result from previous stage
                    // TODO: Optimization - Use cached advancedFilteredBitmap if available and sketch params haven't changed (and stage1Result is same as cached basic)
                    val stage2Result = applyAdvancedFilters(stage2Input, currentState) // Applies sketch based on params
                    setState { copy(advancedFilteredBitmap = stage2Result) }
                    Timber.d("ADVANCED filters applied.")

                    // If current mode is ADVANCED, apply final adjustments and finish
                    if (currentState.controlMode == ControlMode.ADVANCED) {
                        Timber.d("Mode is ADVANCED, applying final adjustments.")
                        return@withContext applyFinalAdjustments(stage2Result, currentState)
                    }

                    // --- Stage 3: ANOTHER_MODE / DOTWORK (Blur + Dotwork) ---
                    Timber.d("Applying Stage 3 filters (Blur/Dotwork)...")
                    val stage3Input = stage2Result // Use result from previous stage (which now includes sketch)
                    // Pass textures to the stage 3 function
                    val stage3Result = applyPostProcessingFilters(stage3Input, currentState)
                    Timber.d("Stage 3 filters applied.")

                    if (currentState.controlMode == ControlMode.POST_PROCESS || currentState.controlMode == ControlMode.DOTWORK) {
                        Timber.d("Mode is ${currentState.controlMode}, applying final adjustments.")
                        return@withContext applyFinalAdjustments(stage3Result, currentState)
                    }

                    Timber.w("Reached end of recalculateFilters without matching mode, returning stage 1 result.")
                    applyFinalAdjustments(stage1Result, currentState) // Apply final adjustments even in fallback
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during filter recalculation for mode ${currentState.controlMode}")
                withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ShowToast("Error applying filters") } }
                null
            }
        }
        Timber.d("Filter recalculation took $time ms")

        if (finalResult != null) {
            withContext(dispatcher.main) {
                Timber.d("Updating displayBitmap.")
                setState {
                    copy(
                        transformedImage = finalResult, // Store final cumulative result
                        displayBitmap = finalResult    // Display final cumulative result
                    )
                }
            }
        }
        // Indicate processing finished (moved to finally block in handleEvents)
        // withContext(dispatcher.main) { setState { copy(isProcessingFilters = false) } }
    }

    // --- Stage-Specific Filter Functions (with Error Handling and Logging) ---

    /** Applies filters relevant to the BASIC stage */
    private suspend fun applyBasicFilters(inputBitmap: Bitmap, state: ImageViewState): Bitmap {
        // ... (Implementation remains the same) ...
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

    /** Applies filters relevant to the ADVANCED (Sketch) stage */
    private suspend fun applyAdvancedFilters(inputBitmap: Bitmap, state: ImageViewState): Bitmap {
        // ... (Implementation remains the same - applies sketch based on params) ...
        return try {
            withContext(dispatcher.default) {
                var result = inputBitmap
                Timber.v("Applying Sketch: details=${state.sketchDetails}, gamma=${state.sketchGamma}")
                result = ImageFilterUtils.applySketchFilter(
                    result,
                    state.sketchDetails,
                    state.sketchGamma
                )
                result
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in applyAdvancedFilters (Sketch)")
            inputBitmap // Return input on error
        }
    }

    /** Applies filters relevant to the Post-Processing stage (Blur, Dotwork) */
    private suspend fun applyPostProcessingFilters(inputBitmap: Bitmap, state: ImageViewState): Bitmap {
        return try {
            withContext(dispatcher.default) {
                var result = inputBitmap // Start from previous stage result (includes sketch)

                // Apply Gaussian Blur if value is set (controlled by ANOTHER_MODE UI)
                if (state.gaussianBlur > 1f) { // Check default
                    Timber.v("Applying Gaussian Blur: ${state.gaussianBlur}")
                    result = ImageFilterUtils.applyGaussianBlur(result, state.gaussianBlur)
                } else {
                    Timber.v("Skipping Gaussian Blur (value <= 1)")
                }

                // Apply Dotwork Filter (controlled by DOTWORK UI state)
                if (state.isDotworkEnabled) { // Check the flag
                    Timber.v("Applying Dotwork: density=${state.dotDensity}, size=${state.dotSize}")
                    // Pass preloaded textures
                    result = ImageFilterUtils.applyDotworkEffect(
                        inputBitmap = result, // Apply to blurred (if applied) or sketched result
                        dotDensity = state.dotDensity,
                        dotSize = state.dotSize,
                        grainTexture1 = grainTexture1,
                        grainTexture2 = grainTexture2,
                        blackTexture = blackTexture
                    )
                } else {
                    Timber.v("Skipping Dotwork (isDotworkEnabled=false)")
                }

                result
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in applyPostProcessingFilters")
            inputBitmap // Return input on error
        }
    }

    /** Applies final adjustments like B&W and Background Removal */
    private suspend fun applyFinalAdjustments(inputBitmap: Bitmap, state: ImageViewState): Bitmap {
        // ... (Implementation remains the same) ...
        return try {
            withContext(dispatcher.default) {
                var result = inputBitmap
                if (state.isRemoveBackground) {
                    Timber.v("Applying Background Removal")
                    result = ImageFilterUtils.removeBackground(result)
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
        viewModelScope.launch(dispatcher.io) {
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
                        contentResolver.delete(uri, null, null)
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

// --- Add new State properties ---
// (Add to ImageContract.kt)
// data class ImageViewState(...) : ViewState {
//    ...
//    val basicFilteredBitmap: Bitmap? = null, // Cache after basic filters
//    val advancedFilteredBitmap: Bitmap? = null, // Cache after advanced/sketch filters
//    val isProcessingFilters: Boolean = false, // Flag for loading state
//    ...
// }
