package com.maca.tsp.presentation.state

class MainContract {

    data class MainViewState(
        val isUserFirstTime: Boolean = true,
    ) : ViewState

    sealed class MainEvent : ViewEvent {
        data object OnRetry : MainEvent()
        data object IdleAppState : MainEvent()
    }

    sealed class MainEffect : ViewSideEffect {
        sealed class Navigation : MainEffect()
    }
}