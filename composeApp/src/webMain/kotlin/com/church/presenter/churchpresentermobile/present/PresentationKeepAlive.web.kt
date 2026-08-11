package com.church.presenter.churchpresentermobile.present

// There is no server to keep alive in a browser.
actual object PresentationKeepAlive {
    actual fun start(detail: String?) = Unit
    actual fun stop() = Unit
}
