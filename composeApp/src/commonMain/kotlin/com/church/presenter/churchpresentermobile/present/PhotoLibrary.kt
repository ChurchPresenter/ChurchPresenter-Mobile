package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "PhotoLibrary"

/** Path prefix the phone's own server answers photo requests on. */
const val PHOTO_ROUTE = "photo"

/** A photo the operator picked this session, ready to project. */
data class StoredPhoto(val id: String, val fileName: String)

/** A photo as the phone's server should answer it. */
class ServedPhoto(val bytes: ByteArray, val contentType: String)

/**
 * Where the presentation server gets photo bytes.
 *
 * A seam rather than a direct dependency so [LocalWebServer] keeps knowing
 * nothing about picking, projecting or modes — it is handed a lookup and serves
 * what comes back.
 */
fun interface PhotoSource {
    fun photo(id: String): ServedPhoto?

    companion object {
        /** Serves nothing — for remote mode and for the web target. */
        val NONE: PhotoSource = PhotoSource { null }
    }
}

/**
 * Photos the operator picked from this device, held for the service.
 *
 * Standalone's Photos tab used to be the desktop's picture folders, which a
 * phone with no desktop could never list. This is the local answer: pick from
 * the device, project straight from here.
 *
 * Both renderers reach a photo the same way — by URL. The phone is already
 * running a server for the display page in standalone, so the bytes are served
 * from there and the browser sink and the in-process display load the identical
 * address. That is why [serveFrom] exists: the server decides its own port at
 * bind time, and until it has, there is no URL to hand out.
 *
 * Deliberately in memory and deliberately not persisted. A service's photos are
 * that service's; keeping picked copies of someone's camera roll on disk is a
 * privacy cost with no matching benefit, and the picker is two taps away.
 */
class PhotoLibrary(
    private val newId: () -> String,
    /**
     * Shrinks a picked photo before it is kept. Injected so the library stays
     * testable without an image pipeline.
     */
    private val downscale: (ByteArray) -> ByteArray = { downscaleImage(it) },
) {

    private val bytesById = mutableMapOf<String, ByteArray>()

    private val _photos = MutableStateFlow<List<StoredPhoto>>(emptyList())

    /** The photos picked this session, in the order they were picked. */
    val photos: StateFlow<List<StoredPhoto>> = _photos.asStateFlow()

    private val _baseUrl = MutableStateFlow<String?>(null)

    /**
     * Where photos are served from, or null while the server is down — which is
     * also "photos cannot be projected yet", and the UI says so.
     */
    val baseUrl: StateFlow<String?> = _baseUrl.asStateFlow()

    /**
     * Tells the library where its photos are reachable, e.g.
     * `http://192.168.1.50:8080`. Null when the server is not running, which is
     * what makes [urlFor] refuse rather than hand out a dead address.
     */
    fun serveFrom(baseUrl: String?) {
        _baseUrl.value = baseUrl?.trimEnd('/')
        Logger.d(TAG, "serveFrom — $baseUrl")
    }

    /** Stores [bytes] and returns the photo, ready to be listed and projected. */
    fun add(fileName: String, bytes: ByteArray): StoredPhoto {
        // Shrunk on the way in, not on the way out: these bytes are held in
        // memory for the life of the session and sent to every screen watching,
        // so a camera-sized original would be paid for twice over. A phone photo
        // is several times larger than any output this app drives can show.
        val stored = downscale(bytes)
        val photo = StoredPhoto(id = newId(), fileName = fileName)
        bytesById[photo.id] = stored
        _photos.value = _photos.value + photo
        Logger.d(TAG, "add — ${photo.fileName} (${bytes.size} bytes in, ${stored.size} kept)")
        return photo
    }

    /** The bytes to serve for [id], or null when nothing was picked under it. */
    fun bytes(id: String): ByteArray? = bytesById[id]

    /**
     * Where [id] can be fetched from, or null when the server is not running or
     * the photo is unknown — a slide with a dead URL renders as an empty screen
     * mid-service, so it is better not to build one.
     */
    fun urlFor(id: String): String? {
        if (id !in bytesById) return null
        return _baseUrl.value?.let { "$it/$PHOTO_ROUTE/$id" }
    }

    /** Forgets one photo. */
    fun remove(id: String) {
        bytesById.remove(id)
        _photos.value = _photos.value.filterNot { it.id == id }
    }

    /** Forgets the lot — the end of a service, or a change of mind. */
    fun clear() {
        bytesById.clear()
        _photos.value = emptyList()
    }

    /** This library as something the server can serve from. */
    val source: PhotoSource = PhotoSource { id ->
        bytes(id)?.let { bytes ->
            ServedPhoto(bytes, contentTypeFor(_photos.value.firstOrNull { it.id == id }?.fileName ?: ""))
        }
    }

    /** Content type for [fileName], for the server to answer with. */
    fun contentTypeFor(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        else -> "image/jpeg"
    }
}
