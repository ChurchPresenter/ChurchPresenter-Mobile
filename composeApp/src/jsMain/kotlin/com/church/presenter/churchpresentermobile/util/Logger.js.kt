package com.church.presenter.churchpresentermobile.util

actual object Logger {
    actual fun d(tag: String, message: String) {
        println("D/$tag: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("E/$tag: $message${throwable?.let { " | ${it.message}" } ?: ""}")
        throwable?.printStackTrace()
    }
}

