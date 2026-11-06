package com.church.presenter.churchpresentermobile.tv

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.church.presenter.churchpresentermobile.network.apiRunCatching
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.websocket.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

private const val TAG = "DesktopFrames"

/** The header before each picture: six big-endian 32-bit numbers. */
internal const val FRAME_HEADER_BYTES = 24

private const val BYTE_MASK = 0xFF

/** Outputs a TV looks for. A desktop with more Browser Sources than this is unheard of. */
internal const val MAX_OUTPUTS = 8

/** Where one changed rectangle goes, and how big the whole output is. */
internal data class FrameHeader(
    val x: Int,
    val y: Int,
    val rectWidth: Int,
    val rectHeight: Int,
    val fullWidth: Int,
    val fullHeight: Int,
)

/** The big-endian 32-bit number starting at [offset]. */
private fun bigEndianInt(bytes: ByteArray, offset: Int): Int =
    (offset until offset + Int.SIZE_BYTES).fold(0) { value, index ->
        (value shl Byte.SIZE_BITS) or (bytes[index].toInt() and BYTE_MASK)
    }

/**
 * Reads the header of one message from `WS /api/browser-source/{n}/ws` (desktop
 * `COMPANION_API.md`, "Browser Source"), or null if it is too short or names a
 * size no picture could have.
 */
internal fun parseFrameHeader(bytes: ByteArray): FrameHeader? {
    if (bytes.size <= FRAME_HEADER_BYTES) return null
    val numbers = (0 until FRAME_HEADER_BYTES step Int.SIZE_BYTES).map { bigEndianInt(bytes, it) }.iterator()
    val header = FrameHeader(
        x = numbers.next(),
        y = numbers.next(),
        rectWidth = numbers.next(),
        rectHeight = numbers.next(),
        fullWidth = numbers.next(),
        fullHeight = numbers.next(),
    )
    return header.takeIf {
        it.fullWidth > 0 && it.fullHeight > 0 && it.rectWidth > 0 && it.rectHeight > 0 &&
            it.x >= 0 && it.y >= 0 &&
            it.x + it.rectWidth <= it.fullWidth && it.y + it.rectHeight <= it.fullHeight
    }
}

/**
 * Paints each changed rectangle onto one picture of the whole output.
 *
 * The desktop sends only what changed, so the picture is the sum of everything
 * received. A new output size starts a new picture: the desktop sends the whole
 * frame when a client connects, so nothing is lost.
 *
 * @param decode Turns a JPEG or PNG into a bitmap. A seam over Compose's
 *   decoder, which needs the platform's codecs.
 * @param paint Draws a rectangle onto the picture. A seam over Compose's Canvas,
 *   so which rectangle lands where can be asserted without a real bitmap.
 * @param create Makes a blank picture of the output's size.
 */
internal class FrameCompositor(
    private val decode: (ByteArray) -> ImageBitmap = { it.decodeToImageBitmap() },
    private val paint: (target: ImageBitmap, rect: ImageBitmap, header: FrameHeader) -> Unit = ::paintRect,
    private val create: (width: Int, height: Int) -> ImageBitmap = { w, h -> ImageBitmap(w, h) },
) {
    private var picture: ImageBitmap? = null
    private var size: Pair<Int, Int>? = null

    /** Applies one message, or returns null if it was not a usable frame. */
    fun apply(message: ByteArray): ImageBitmap? {
        val header = parseFrameHeader(message) ?: return null
        val rect = runCatching { decode(message.copyOfRange(FRAME_HEADER_BYTES, message.size)) }.getOrNull()
            ?: return null
        val wanted = header.fullWidth to header.fullHeight
        val target = picture.takeIf { size == wanted } ?: create(header.fullWidth, header.fullHeight).also {
            picture = it
            size = wanted
        }
        paint(target, rect, header)
        return target
    }
}

private fun paintRect(target: ImageBitmap, rect: ImageBitmap, header: FrameHeader) {
    Canvas(target).drawImageRect(
        image = rect,
        dstOffset = IntOffset(header.x, header.y),
        dstSize = IntSize(header.rectWidth, header.rectHeight),
        paint = Paint(),
    )
}

/**
 * A desktop output's pictures, for as long as this is collected, reconnecting
 * for ever. The last picture stays up while the desktop is away, for the same
 * reason a phone's last slide does.
 *
 * @param connect Opens the socket and hands each binary message to the block.
 *   A seam over the Ktor client.
 * @param compositor Builds the picture; a fresh one per connection, since the
 *   desktop sends a whole frame to each new client.
 */
internal fun desktopLiveStates(
    source: TvSource,
    connect: suspend (url: String, onBytes: suspend (ByteArray) -> Unit) -> Unit,
    compositor: () -> FrameCompositor = { FrameCompositor() },
): Flow<LiveState> = channelFlow {
    var last: ImageBitmap? = null
    var version = 0
    var waitMs = RECONNECT_MIN_MS
    send(LiveState.Connecting)
    while (true) {
        val pictures = compositor()
        apiRunCatching {
            connect(source.liveUrl) { bytes ->
                waitMs = RECONNECT_MIN_MS
                // Decoding a 1080p JPEG is too much for the main thread on a TV's chip.
                val image = withContext(Dispatchers.Default) { pictures.apply(bytes) }
                if (image != null) {
                    last = image
                    send(LiveState.Picture(image, ++version))
                }
            }
        }.onFailure { Logger.d(TAG, "connection to ${source.host} failed: ${it.message}") }
        send(LiveState.Lost(lastSlide = null, lastPicture = last))
        delay(waitMs)
        waitMs = nextReconnectDelay(waitMs)
    }
}

/** [desktopLiveStates] over a real WebSocket client. */
fun desktopLiveStates(source: TvSource, client: HttpClient): Flow<LiveState> =
    desktopLiveStates(source, connect = { url, onBytes ->
        client.webSocket(urlString = url) {
            for (frame in incoming) {
                if (frame is Frame.Binary) onBytes(frame.data)
            }
        }
    })

/** One output a desktop offers. */
data class DesktopOutput(val number: Int, val needsKey: Boolean)

/**
 * The Browser Source outputs a desktop has, found by asking for each page: `200`
 * is an output, `401` is one behind a key, `404` is none. A desktop has no
 * route that lists them, and asking costs nothing.
 *
 * @param status Fetches a page's status code, or null if the desktop could not
 *   be reached. A seam over the HTTP client.
 */
internal suspend fun findOutputs(source: TvSource, status: suspend (url: String) -> Int?): List<DesktopOutput> =
    (1..MAX_OUTPUTS).mapNotNull { number ->
        when (status(source.outputPageUrl(number))) {
            HttpStatusCode.OK.value -> DesktopOutput(number, needsKey = false)
            HttpStatusCode.Unauthorized.value -> DesktopOutput(number, needsKey = true)
            else -> null
        }
    }

/** [findOutputs] over a real HTTP client. */
suspend fun findOutputs(source: TvSource, client: HttpClient): List<DesktopOutput> =
    findOutputs(source) { url -> apiRunCatching { client.get(url).status.value }.getOrNull() }
