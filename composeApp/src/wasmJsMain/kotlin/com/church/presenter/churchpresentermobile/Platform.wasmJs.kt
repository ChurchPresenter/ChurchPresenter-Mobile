package com.church.presenter.churchpresentermobile

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val os: String = "web"
}

actual fun getPlatform(): Platform = WasmPlatform()

@JsFun("() => crypto.randomUUID()")
private external fun jsRandomUUID(): String

actual fun generateUUID(): String = jsRandomUUID()

/** Blank for the same reason as the js target — see Platform.js.kt. */
actual fun deviceName(): String = ""

@JsFun("(url) => { window.open(url, '_blank'); }")
private external fun jsOpenUrl(url: String)

actual fun openUrl(url: String) = jsOpenUrl(url)
