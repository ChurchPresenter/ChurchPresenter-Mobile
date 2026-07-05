package com.church.presenter.churchpresentermobile

import kotlinx.browser.window

class JsPlatform : Platform {
    override val name: String = "Web with Kotlin/JS"
    override val os: String = "web"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun generateUUID(): String = js("crypto.randomUUID()") as String

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}
