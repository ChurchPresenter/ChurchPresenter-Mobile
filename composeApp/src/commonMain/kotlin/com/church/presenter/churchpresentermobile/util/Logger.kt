package com.church.presenter.churchpresentermobile.util

/** Lightweight multiplatform logger. */
expect object Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
