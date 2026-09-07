package com.church.presenter.churchpresentermobile.util

actual object Analytics {
    actual fun init() = Unit
    actual fun setEnabled(enabled: Boolean) = Unit
    actual fun logEvent(name: String, params: Map<String, String>) = Unit
    actual fun logScreenView(screenName: String) = Unit
}
