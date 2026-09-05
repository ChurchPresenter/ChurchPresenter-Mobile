package com.church.presenter.churchpresentermobile

import kotlinx.browser.window

class JsPlatform : Platform {
    override val name: String = "Web with Kotlin/JS"
    override val os: String = "web"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun generateUUID(): String = js("crypto.randomUUID()") as String

/**
 * Blank: a browser has no device name to offer, and the user agent is neither
 * stable nor meaningful to the operator approving the connection. The custom
 * name in Settings is how a browser remote gets named.
 */
actual fun deviceName(): String = ""

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}
