@file:OptIn(ExperimentalWasmJsInterop::class)

package com.church.presenter.churchpresentermobile.library

/**
 * localStorage-backed store for the WasmJS target, via JS interop.
 *
 * Standalone mode is off on web, so nothing writes here today — see the JS
 * implementation for the reasoning.
 */
class WasmJsFileStore : FileStore {

    override fun read(name: String): String? = storageGet(KEY_PREFIX + name)

    override fun write(name: String, text: String) = storageSet(KEY_PREFIX + name, text)

    override fun delete(name: String) = storageRemove(KEY_PREFIX + name)

    override fun list(): List<String> = storageKeys(KEY_PREFIX)
        .split('\n')
        .filter { it.isNotBlank() }

    override fun sizeBytes(name: String): Long = read(name)?.length?.toLong() ?: 0L

    private companion object {
        const val KEY_PREFIX = "cpm.lib."
    }
}

@JsFun("(key) => localStorage.getItem(key)")
private external fun storageGet(key: String): String?

@JsFun("(key, value) => localStorage.setItem(key, value)")
private external fun storageSet(key: String, value: String)

@JsFun("(key) => localStorage.removeItem(key)")
private external fun storageRemove(key: String)

// Returned newline-joined: Wasm JS interop cannot hand back a Kotlin List directly.
@JsFun("""(prefix) => Object.keys(localStorage).filter(k => k.startsWith(prefix)).map(k => k.slice(prefix.length)).join('\n')""")
private external fun storageKeys(prefix: String): String

actual fun createFileStore(): FileStore = WasmJsFileStore()
