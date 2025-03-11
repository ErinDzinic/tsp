package com.maca.tsp.presentation.state

import android.net.Uri

class ImageContract {
    data class ImageViewState(
        val rawImage: Uri? = null,
        val isMinimized: Boolean = false,
    ) : ViewState

    sealed class ImageEvent : ViewEvent {
        data class ImageSelected(val uri: Uri) : ImageEvent()
        data object IsMinimized : ImageEvent()
    }

    sealed class ImageEffect : ViewSideEffect {
        sealed class Navigation : ImageEffect() {
            data object ToImageDetails : Navigation()
        }
    }
}