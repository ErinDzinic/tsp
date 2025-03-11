package com.maca.tsp.common.appstate

import androidx.compose.runtime.mutableStateOf
import com.maca.tsp.common.util.AppException
import androidx.compose.runtime.State
import javax.inject.Inject


class GlobalStateImpl @Inject constructor(
) : GlobalState {
    override val loadingState = mutableStateOf(false)
    override val errorState = mutableStateOf<AppException?>(null)
    override val successState = mutableStateOf<String?>(null)
    override val appLoaded = mutableStateOf(false)
    override var onPositiveAction: (() -> Unit?)? = null

    override fun idle() {
        loadingState.value = false
        errorState.value = null
        successState.value = null
        onPositiveAction = null
    }

    override fun loading(show: Boolean) {
        errorState.value = null
        successState.value = null
        loadingState.value = show
        onPositiveAction = null
    }

    override fun error(error: AppException, hideLoading: Boolean, onAction: (() -> Unit)?) {
        if (hideLoading) loadingState.value = false
        successState.value = null
        errorState.value = error
        onPositiveAction = onAction
    }

    override fun error(msgs: List<AppException>, hideLoading: Boolean) {
        if (hideLoading) loadingState.value = false
        successState.value = null
        errorState.value = msgs.last()
        onPositiveAction = null
    }


    override fun success(msg: String, hideLoading: Boolean) {
        if (hideLoading) loadingState.value = false
        errorState.value = null
        successState.value = msg
        onPositiveAction = null
    }

    override fun appLoaded() {
        appLoaded.value = true
        onPositiveAction = null
    }

}

interface GlobalState {
    val loadingState: State<Boolean>
    val errorState: State<AppException?>
    val successState: State<String?>
    val appLoaded: State<Boolean>
    var onPositiveAction: (() -> Unit?)?

    fun idle()
    fun loading(show: Boolean)
    fun error(error: AppException, hideLoading: Boolean = true, onAction: (() -> Unit)? = null)
    fun error(msgs: List<AppException>, hideLoading: Boolean = true)
    fun success(msg: String, hideLoading: Boolean = true)
    fun appLoaded()
}