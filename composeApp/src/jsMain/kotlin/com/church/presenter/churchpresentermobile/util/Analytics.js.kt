package com.church.presenter.churchpresentermobile.util

/**
 * The browser build has no Firebase, so there is nothing to start or switch
 * off — events are printed to the console instead, which is where a developer
 * running the web target will look for them.
 */
actual object Analytics {
    actual fun init() = Unit
    actual fun setEnabled(enabled: Boolean) = Unit
    actual fun logEvent(name: String, params: Map<String, String>) {
        println("[Analytics] $name $params")
    }
    actual fun logScreenView(screenName: String) {
        println("[Analytics] screenView: $screenName")
    }
}
