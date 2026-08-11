package com.church.presenter.churchpresentermobile.present

import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.util.Logger
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val TAG = "WebAssets"

/** Root of the bundled display page inside composeResources. */
private const val ASSET_ROOT = "files/present"

/** One file the presentation server can serve, held in memory. */
class WebAsset(val bytes: ByteArray, val contentType: String)

/**
 * The bundled display page, loaded once and served from memory.
 *
 * Everything the projected page needs — markup, CSS, script, fonts — ships
 * inside the app. That is the whole design: a church hall's Wi-Fi often has no
 * internet, and a display that fetches its stylesheet from a CDN is not
 * actually offline-capable. (The reference app VerseCAST loads its theme from
 * GitHub Pages and is broken by exactly this.) A CI check greps these files for
 * absolute URLs so the guarantee cannot rot.
 */
class WebAssets(private val files: Map<String, WebAsset>) {

    /** Returns the asset for a request path such as `/` or `/assets/app.js`. */
    fun forPath(path: String): WebAsset? = files[normalize(path)]

    val isEmpty: Boolean get() = files.isEmpty()

    companion object {
        /** Files bundled under [ASSET_ROOT], in load order. */
        val FILE_NAMES: List<String> = listOf(
            "index.html",
            "app.js",
            "style.css",
        )

        /**
         * Maps a request path onto a bundled file name.
         *
         * `/` serves the page; anything else is looked up by its last segment,
         * so `/assets/app.js` and `/app.js` both resolve. Query strings and
         * fragments are stripped — a browser cache-buster must not 404.
         */
        fun normalize(path: String): String {
            val clean = path.substringBefore('?').substringBefore('#')
            if (clean.isEmpty() || clean == "/" || clean == "/index.html") return "index.html"
            return clean.trimStart('/').substringAfterLast('/')
        }

        /** Content type for a bundled file, by extension. */
        fun contentTypeFor(fileName: String): String = when (fileName.substringAfterLast('.', "")) {
            "html" -> "text/html; charset=utf-8"
            "js" -> "application/javascript; charset=utf-8"
            "css" -> "text/css; charset=utf-8"
            "woff2" -> "font/woff2"
            "woff" -> "font/woff"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "svg" -> "image/svg+xml"
            else -> "application/octet-stream"
        }

        /**
         * Reads every bundled file into memory.
         *
         * A file that fails to load is skipped rather than fatal: a missing
         * font should degrade the page's typography, not stop the service.
         */
        @OptIn(ExperimentalResourceApi::class)
        suspend fun load(): WebAssets {
            val loaded = buildMap {
                FILE_NAMES.forEach { name ->
                    runCatching { Res.readBytes("$ASSET_ROOT/$name") }
                        .onSuccess { put(name, WebAsset(it, contentTypeFor(name))) }
                        .onFailure { Logger.e(TAG, "missing bundled asset '$name': ${it.message}") }
                }
            }
            Logger.d(TAG, "loaded ${loaded.size}/${FILE_NAMES.size} display assets")
            return WebAssets(loaded)
        }
    }
}
