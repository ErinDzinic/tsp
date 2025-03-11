package com.maca.tsp.common.util

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException


const val TIME_OUT = -100
const val NO_CONNECTION = -101
const val UNEXPECTED = -102
const val UNAUTHORIZED = 30001

sealed class AppException {
    abstract val errorCode: Int?
    abstract val errorMessage: String?
    abstract val errorBody: String?
    abstract val throwable: Throwable?

    class NoConnectionException(
        override val errorCode: Int = 0,
        override val errorMessage: String? = null,
        override val throwable: Throwable? = null,
        override val errorBody: String? = null,
    ) : AppException()

    class TimeOutException(
        override val errorCode: Int = 0,
        override val errorMessage: String? = null,
        override val throwable: Throwable? = null,
        override val errorBody: String? = null,
    ) : AppException()

    class UnexpectedException(
        override val errorCode: Int = 0,
        override val errorMessage: String? = null,
        override val throwable: Throwable? = null,
        override val errorBody: String? = null,
    ) : AppException()

    class UnAuthorizedException(
        override val errorCode: Int? = null,
        override val errorMessage: String? = null,
        override val throwable: Throwable? = null,
        override val errorBody: String? = null,
    ) : AppException()

}

fun mapErrorToAppError(
    errorCode: Int?,
    errorMessage: String? = null,
    errorBody: String? = null,
    throwable: Throwable
): AppException {

    return when (val code = errorCode ?: UNEXPECTED) {
        TIME_OUT -> AppException.TimeOutException(
            errorCode = code,
            errorMessage = errorMessage,
            errorBody = errorBody,
            throwable = throwable
        )

        NO_CONNECTION -> AppException.TimeOutException(
            errorCode = code,
            errorMessage = errorMessage,
            errorBody = errorBody,
            throwable = throwable
        )

        UNAUTHORIZED -> AppException.UnAuthorizedException(
            errorCode = code,
            errorMessage = errorMessage,
            errorBody = errorBody,
            throwable = throwable
        )

        else -> {
            AppException.UnexpectedException(
                errorCode = code,
                errorMessage = errorMessage ?: "The application has encountered an unknown error",
                errorBody = errorBody,
                throwable = throwable
            )
        }
    }
}


fun Throwable.mapToAppException() = when (this) {
    is UnknownHostException -> {
        AppException.NoConnectionException(
            errorMessage = "${this.message}",
            throwable = this
        )
    }

    is SocketTimeoutException,
    is TimeoutException -> {
        AppException.TimeOutException(
            errorMessage = "${this.message}",
            throwable = this
        )
    }

    else -> {
        AppException.UnexpectedException(
            errorCode = UNEXPECTED,
            errorMessage = "${this.message}",
            throwable = this
        )
    }
}

fun appExceptionHandler(
    error: AppException?,
    onNoConnectionException: (() -> Unit?)? = null,
    onTimeOutException: (() -> Unit?)? = null,
    onUnAuthorizedException: (() -> Unit?)? = null,
    onUnexpectedException: (() -> Unit?)? = null,
) {
    when (error) {
        is AppException.NoConnectionException -> onNoConnectionException?.invoke()
        is AppException.TimeOutException -> onTimeOutException?.invoke()
        is AppException.UnAuthorizedException -> onUnAuthorizedException?.invoke()
        is AppException.UnexpectedException -> onUnexpectedException?.invoke()
        else -> {

        }
    }
}


fun AppException.unauthorizedHandler(
    onUnAuthorizedException: (() -> Unit?)? = null,
    onError: (() -> Unit?)? = null,
) {
    when (this) {
        is AppException.UnAuthorizedException -> onUnAuthorizedException?.invoke()
        else -> {
            onError?.invoke()
        }
    }
}
