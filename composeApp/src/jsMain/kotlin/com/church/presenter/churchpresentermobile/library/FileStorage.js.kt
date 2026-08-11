package com.church.presenter.churchpresentermobile.library

import kotlinx.browser.localStorage

/**
 * localStorage-backed store, keyed by a prefix so it cannot collide with the
 * app's settings keys.
 *
 * Standalone mode is off on web, so nothing writes here today. It exists so the
 * shared library code compiles for js, and leaves the door open for a read-only
 * library view in the browser later.
 */
class JsFileStore : FileStore {

    override fun read(name: String): String? = localStorage.getItem(KEY_PREFIX + name)

    override fun write(name: String, text: String) {
        localStorage.setItem(KEY_PREFIX + name, text)
    }

    override fun delete(name: String) {
        localStorage.removeItem(KEY_PREFIX + name)
    }

    override fun list(): List<String> = (0 until localStorage.length)
        .mapNotNull { localStorage.key(it) }
        .filter { it.startsWith(KEY_PREFIX) }
        .map { it.removePrefix(KEY_PREFIX) }

    override fun sizeBytes(name: String): Long = read(name)?.length?.toLong() ?: 0L

    private companion object {
        const val KEY_PREFIX = "cpm.lib."
    }
}

actual fun createFileStore(): FileStore = JsFileStore()
