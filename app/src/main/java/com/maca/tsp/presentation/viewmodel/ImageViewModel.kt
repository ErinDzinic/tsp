package com.maca.tsp.presentation.viewmodel

import com.maca.tsp.common.util.DispatcherProvider
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
}
