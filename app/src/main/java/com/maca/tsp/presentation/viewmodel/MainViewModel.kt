package com.maca.tsp.presentation.viewmodel

import com.maca.tsp.common.appstate.GlobalState
import com.maca.tsp.common.util.DispatcherProvider
import com.maca.tsp.presentation.state.BaseViewModel
import com.maca.tsp.presentation.state.MainContract
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val globalState: GlobalState,
    private val dispatcher: DispatcherProvider,
) : BaseViewModel<MainContract.MainEvent, MainContract.MainViewState, MainContract.MainEffect>(
    dispatcher
) {

    var keepSplashScreen = true

    override fun setInitialState(): MainContract.MainViewState = MainContract.MainViewState()

    override fun handleEvents(event: MainContract.MainEvent) {
        when (event) {
            MainContract.MainEvent.IdleAppState -> globalState.idle()
            MainContract.MainEvent.OnRetry -> {}
        }
    }
}