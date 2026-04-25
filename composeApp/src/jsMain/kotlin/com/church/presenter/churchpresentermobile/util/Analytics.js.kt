package com.church.presenter.churchpresentermobile.util

actual object Analytics {
    actual fun init() {}
    actual fun logEvent(name: String, params: Map<String, String>) {
        println("[Analytics] $name $params")
    }
}

