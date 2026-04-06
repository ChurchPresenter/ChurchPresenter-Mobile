package com.church.presenter.churchpresentermobile

class JsPlatform : Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun generateUUID(): String = js("crypto.randomUUID()") as String

