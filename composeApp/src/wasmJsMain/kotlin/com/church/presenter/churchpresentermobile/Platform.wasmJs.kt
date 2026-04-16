package com.church.presenter.churchpresentermobile

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

@JsFun("() => crypto.randomUUID()")
private external fun jsRandomUUID(): String

actual fun generateUUID(): String = jsRandomUUID()

@JsFun("(url) => { window.open(url, '_blank'); }")
private external fun jsOpenUrl(url: String)

actual fun openUrl(url: String) = jsOpenUrl(url)
