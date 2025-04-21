package com.maca.tsp.presentation.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.lifecycle.viewModelScope
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.min


@OptIn(FlowPreview::class)
@HiltViewModel
class ImageViewModel @Inject constructor(
    private val dispatcher: DispatcherProvider,
    @ApplicationContext private val appContext: Context
) : BaseViewModel<ImageEvent, ImageViewState, ImageEffect>(
    dispatcher
) {
    private var originalBitmap: Bitmap? = null
    private val filterUpdateFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var grainTexture1: Bitmap? = null


    init {

        viewModelScope.launch(dispatcher.io) {
            Timber.d("Loading textures...")
            grainTexture1 = loadTexture(R.drawable.grain_texture_1, 512, 512)

            if (grainTexture1 == null) {
                Timber.w("Failed to load one or more grain textures!")
            } else {
                Timber.d("Grain texture(s) loaded successfully.")
            }
        }

        viewModelScope.launch(dispatcher.io) {
            filterUpdateFlow
                .debounce(200L)
                .collectLatest {
                    Timber.d("Debounced filter update triggered.")
                    val calculationJob = launch(dispatcher.default) {
                        withContext(dispatcher.main) { setState { copy(isProcessingFilters = true) } }
                        try {
                            recalculateFiltersForCurrentMode()
                        } catch (e: Exception) {
                            Timber.e(e, "Error during debounced filter recalculation")
                            withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ShowToast("Error applying filters: ${e.message}") } }
                        } finally {
                            withContext(dispatcher.main) { setState { copy(isProcessingFilters = false) } }
                        }
                    }
                }
        }
    }

    private fun loadTexture(@DrawableRes resId: Int, targetWidth: Int = 512, targetHeight: Int = 512): Bitmap? {
        return try {
            val drawable: Drawable? = AppCompatResources.getDrawable(appContext, resId)

            when (drawable) {
                is BitmapDrawable -> {
                    Timber.v("Loaded texture resource $resId as BitmapDrawable")
                    drawable.bitmap
                }
                is android.graphics.drawable.VectorDrawable, is VectorDrawableCompat -> {
                    Timber.v("Loaded texture resource $resId as VectorDrawable, rasterizing...")
                    val bitmap = createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }
                else -> {
                    Timber.w("Texture resource $resId is not a recognizable Bitmap or Vector type: ${drawable?.javaClass?.name}")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load or rasterize texture resource: $resId")
            null
        }
    }



    override fun setInitialState(): ImageViewState = ImageViewState(
        brightness = 0f, contrast = 1f, exposure = 1f, gamma = 1f, sharpness = 0f,
        gaussianBlur = 1f, sketchDetails = 10f, sketchGamma = 1.0f,
        isDotworkEnabled = false, dotDensity = 0.3f, dotSize = 4f,
        basicFilteredBitmap = null, advancedFilteredBitmap = null, isProcessingFilters = false,
        controlMode = ControlMode.BASIC
    )

    override fun handleEvents(event: ImageEvent) {
        viewModelScope.launch(dispatcher.default) {
            withContext(dispatcher.default) {
                when (event) {
                    is ImageEvent.ImageSelected -> handleImageSelected(event)
                    is ImageEvent.IsMinimized -> {
                        setState { copy(isMinimized = !isMinimized) }
                    }
                    is ImageEvent.StartCrop -> startCropping()
                    is ImageEvent.CropResult -> handleCropResult(event.uri, event.context)
                    is ImageEvent.ToggleBlackAndWhite -> handleToggleBlackAndWhite(event.isEnabled)
                    is ImageEvent.ToggleRemoveBackground -> handleToggleRemoveBackground(event.isEnabled)
                    is ImageEvent.FlipImage -> handleFlipImage(event.horizontal)
                    is ImageEvent.CancelCrop -> {
                        setState { copy(isCropping = false) }
                    }
                    is ImageEvent.SelectFilter -> handleSelectFilter(event.filterType)
                    is ImageEvent.UpdateFilterValue -> handleUpdateFilterValue(event)
                    is ImageEvent.ChangeControlMode -> handleChangeControlMode(event.mode)
                    ImageEvent.PrintButtonClicked -> {
                        setState { copy(showPrintDialog = true) }
                    }
                    ImageEvent.PrintDialogDismissed -> {
                        setState { copy(showPrintDialog = false) }
                    }
                    is ImageEvent.PrintTypeSelected -> {
                        setState { copy(showPrintDialog = false, selectedPrintType = event.printType) }
                        setEvent(ImageEvent.SaveCanvasStateRequested)
                    }
                    ImageEvent.SaveCanvasStateRequested -> {
                        setEffect { ImageEffect.Navigation.ToPrintPreview }
                    }
                    is ImageEvent.SaveImageClicked -> {
                        viewState.value.displayBitmap?.let { bitmap ->
                            setEffect { ImageEffect.Navigation.SaveImageToGallery(bitmap, event.context) }
                        } ?: setEffect { ImageEffect.Navigation.ShowToast("No image to save.") }
                    }
                    is ImageEvent.UpdateSketchDetails -> {
                        setState { copy(sketchDetails = event.value) }
                        triggerDebouncedUpdate()
                    }
                    is ImageEvent.UpdateSketchGamma -> {
                        setState { copy(sketchGamma = event.value) }
                        triggerDebouncedUpdate()
                    }
                    is ImageEvent.UpdateDotDensity -> {
                        setState { copy(dotDensity = event.value) }
                        if (viewState.value.isDotworkEnabled) triggerDebouncedUpdate()
                    }
                    is ImageEvent.UpdateDotSize -> {
                        setState { copy(dotSize = event.value) }
                        if (viewState.value.isDotworkEnabled) triggerDebouncedUpdate()
                    }
                    is ImageEvent.ToggleDotwork -> {
                        setState { copy(isDotworkEnabled = event.isEnabled) }
                        triggerDebouncedUpdate()
                    }
                }
            }
        }
    }

    private fun triggerDebouncedUpdate() {
        val result = filterUpdateFlow.tryEmit(Unit)
        Timber.v("Attempted to trigger debounced update. Success: $result")
    }

    private suspend fun handleImageSelected(event: ImageEvent.ImageSelected) {
        Timber.d("handleImageSelected START")
        setState { copy(isImageLoading = true) }
        val loadedBitmap = try {
            withContext(dispatcher.io) { uriToBitmap(event.context, event.uri) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load bitmap from URI")
            return
        }

        originalBitmap = loadedBitmap

        if (originalBitmap == null) {
            Timber.e("Original bitmap became null after potential rotation.")
            return
        }


        setState {
            setInitialState().copy(
                rawImage = event.uri,
                displayBitmap = originalBitmap,
                transformedImage = null,
                basicFilteredBitmap = null,
                advancedFilteredBitmap = null,
                isImageLoading = true
            )
        }
        setEffect { ImageEffect.Navigation.ToImageDetails }
        triggerDebouncedUpdate()
        Timber.d("handleImageSelected END")
    }

    private fun startCropping() {
        setState { copy(isCropping = true) }
    }

    private suspend fun handleCropResult(uri: Uri, context: Context) {
        Timber.d("handleCropResult START")
        val croppedBitmap = try {
            withContext(dispatcher.io) { uriToBitmap(context, uri) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load cropped bitmap from URI")
            setState { copy(isCropping = false) }
            return
        }
        originalBitmap = croppedBitmap
        withContext(dispatcher.main) {
            setState {
                setInitialState().copy(
                    rawImage = uri,
                    displayBitmap = croppedBitmap,
                    transformedImage = null,
                    basicFilteredBitmap = null,
                    advancedFilteredBitmap = null,
                    isCropping = false
                )
            }
        }
        triggerDebouncedUpdate()
        Timber.d("handleCropResult END")
    }

    private fun handleToggleBlackAndWhite(isEnabled: Boolean) {
        Timber.d("handleToggleBlackAndWhite: $isEnabled")
        setState { copy(isBlackAndWhite = isEnabled) }
        triggerDebouncedUpdate()
    }

    private suspend fun handleToggleRemoveBackground(isEnabled: Boolean) {
        Timber.d("handleToggleRemoveBackground: $isEnabled")
        if (originalBitmap == null && isEnabled) {
            withContext(dispatcher.main) {
                setEffect { ImageEffect.Navigation.ShowToast("Please select an image first.") }
            }
            return
        }
        setState { copy(isRemoveBackground = isEnabled) }
        triggerDebouncedUpdate()
    }

    private suspend fun handleFlipImage(horizontal: Boolean) {
        Timber.d("handleFlipImage START")
        val baseBitmap = originalBitmap ?: return
        val flippedBitmap = try {
            ImageFilterUtils.flipBitmap(baseBitmap, horizontal)
        } catch (e: Exception) {
            Timber.e(e, "Failed to flip bitmap")
            withContext(dispatcher.main) { setEffect { ImageEffect.Navigation.ShowToast("Failed to flip image") } }
            return
        }
        originalBitmap = flippedBitmap

        setState {
            copy(
                basicFilteredBitmap = null,
                advancedFilteredBitmap = null,
                transformedImage = null
            )
        }
        triggerDebouncedUpdate()
        Timber.d("handleFlipImage END")
    }

    private fun handleSelectFilter(filterType: ImageFilterType) {
        Timber.d("handleSelectFilter: $filterType")
        setState { copy(selectedFilter = filterType) }
    }

    private fun handleUpdateFilterValue(event: ImageEvent.UpdateFilterValue) {
        Timber.d("handleUpdateFilterValue: ${event.filterType} = ${event.value}")
        val newState = when (event.filterType) {
            ImageFilterType.BRIGHTNESS -> viewState.value.copy(brightness = event.value)
            ImageFilterType.CONTRAST -> viewState.value.copy(contrast = event.value)
            ImageFilterType.EXPOSURE -> viewState.value.copy(exposure = event.value)
            ImageFilterType.GAMMA -> viewState.value.copy(gamma = event.value)
            ImageFilterType.SHARPNESS -> viewState.value.copy(sharpness = event.value)
            ImageFilterType.GAUSSIAN_BLUR -> viewState.value.copy(gaussianBlur = event.value)
        }
        setState { newState }
        triggerDebouncedUpdate()
    }


    private fun handleChangeControlMode(mode: ControlMode) {
        Timber.d("handleChangeControlMode: $mode")
        val shouldEnableSketch = mode == ControlMode.ADVANCED

        setState {
            copy(
                controlMode = mode,
                isSketchEnabled = shouldEnableSketch
            )
        }
        triggerDebouncedUpdate()
    }

    private suspend fun recalculateFiltersForCurrentMode() {
        val baseBitmap = originalBitmap ?: run {
            return
        }
        val currentState = viewState.value

        var finalResult: Bitmap? = null

        finalResult = try {
            Timber.v("Applying BASIC filters...")
            var stage1Result = applyBasicFilters(baseBitmap, currentState)

            Timber.v("Applying ADVANCED filters (Sketch)...")
            var stage2Result = applyAdvancedFilters(stage1Result, currentState)

            Timber.v("Applying Stage 3 filters (Blur/Dotwork)...")
            var stage3Result = applyPostProcessingFilters(stage2Result, currentState)

            Timber.v("Applying final adjustments...")
            applyFinalAdjustments(stage3Result, currentState)

        } catch (e: Exception) {
            Timber.e(e, "Error during filter recalculation pipeline for mode ${currentState.controlMode}")
            null
        }

        val resultToUpdate = finalResult

        if (resultToUpdate != null) {
            val previewBitmap = createPreviewBitmap(resultToUpdate)

            withContext(dispatcher.main) {
                setState {
                    copy(
                        transformedImage = resultToUpdate,
                        displayBitmap = previewBitmap
                    )
                }
            }
        } else {
            Timber.w("Final filter result was null, not updating display.")
        }
    }

    private fun createPreviewBitmap(source: Bitmap, maxWidth: Int = 1080, maxHeight: Int = 1920): Bitmap {
        val scale = min(maxWidth.toFloat() / source.width, maxHeight.toFloat() / source.height)
        if (scale >= 1.0f) {
            Timber.v("Preview scaling skipped (scale >= 1.0)")
            return source
        }
        return try {
            source.scale((source.width * scale).toInt(), (source.height * scale).toInt())
        } catch (e: OutOfMemoryError) {
            Timber.e(e, "OOM Error creating preview bitmap, returning original.")
            source
        } catch (e: Exception) {
            Timber.e(e, "Error creating preview bitmap, returning original.")
            source
        }
    }

    private fun applyBasicFilters(inputBitmap: Bitmap, state: ImageViewState): Bitmap {
        return try {
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
        } catch (e: Exception) {
            Timber.e(e, "Error in applyBasicFilters")
            inputBitmap
        }
    }

    private fun applyAdvancedFilters(inputBitmap: Bitmap, state: ImageViewState): Bitmap {
        if (state.controlMode == ControlMode.BASIC) {
            Timber.v("Skipping Sketch filter (Mode: BASIC)")
            return inputBitmap
        }

        return try {
            Timber.v("Applying Sketch: details=${state.sketchDetails}, gamma=${state.sketchGamma}")
            ImageFilterUtils.applySketchFilter(
                inputBitmap,
                state.sketchDetails,
                state.sketchGamma
            )
        } catch (e: Exception) {
            Timber.e(e, "Error in applyAdvancedFilters (Sketch)")
            inputBitmap
        }
    }

    private fun applyPostProcessingFilters(inputBitmap: Bitmap, state: ImageViewState): Bitmap {
        if (state.controlMode != ControlMode.POST_PROCESS && state.controlMode != ControlMode.DOTWORK) {
            return inputBitmap
        }

        return try {
                var result = inputBitmap

                if (state.gaussianBlur > 1f) {
                    Timber.v("Applying Gaussian Blur: ${state.gaussianBlur}")
                    result = ImageFilterUtils.applyGaussianBlur(result, state.gaussianBlur)
                } else {
                    Timber.v("Skipping Gaussian Blur (value <= 1)")
                }

                if (state.isDotworkEnabled && state.controlMode == ControlMode.DOTWORK) {
                    result = ImageFilterUtils.applyDotworkEffect(
                        inputBitmap = result,
                        dotDensity = state.dotDensity,
                        dotSize = state.dotSize,
                        grainTextureBitmap = grainTexture1
                    )!!
                } else {
                    if(state.isDotworkEnabled){
                        Timber.v("Skipping Dotwork (Mode is not DOTWORK)")
                    } else {
                        Timber.v("Skipping Dotwork (isDotworkEnabled=false)")
                    }
                }
                result
        } catch (e: Exception) {
            Timber.e(e, "Error in applyPostProcessingFilters")
            inputBitmap
        }
    }

    private suspend fun applyFinalAdjustments(inputBitmap: Bitmap, state: ImageViewState): Bitmap {
        return try {
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
        } catch (e: Exception) {
            Timber.e(e, "Error in applyFinalAdjustments")
            inputBitmap
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveBitmapToGallery(bitmap: Bitmap, context: Context) {
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