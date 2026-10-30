package com.church.presenter.churchpresentermobile.tv

import com.church.presenter.churchpresentermobile.model.SLIDE_PROTOCOL_VERSION
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.model.SlideMessageType
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.network.apiRunCatching
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json

private const val TAG = "PhoneLiveStream"

/** Shortest and longest wait between reconnect attempts — the same as the browser page's. */
internal const val RECONNECT_MIN_MS = 500L
internal const val RECONNECT_MAX_MS = 5_000L

/**
 * A phone presenting in standalone, as a TV reaches it: the display server the
 * phone already runs for browsers, whose `/live` socket pushes every slide.
 */
data class PhoneSource(val name: String, val host: String, val port: Int) {
    val liveUrl: String get() = "ws://$host:$port/live"
}

/**
 * An address typed on the TV — `10.1.10.162`, or `10.1.10.162:8767` when the
 * phone had to take another port — or null for anything that is not one.
 * A remote's keyboard is slow; a mistyped address should say so before the TV
 * spends five seconds failing to reach it.
 */
internal fun parseAddress(
    text: String,
    defaultPort: Int = ApiConstants.STANDALONE_HTTP_PORT_DEFAULT,
): PhoneSource? {
    val trimmed = text.trim().removePrefix("http://").removePrefix("ws://").trimEnd('/')
    val host = trimmed.substringBefore(':')
    val portText = trimmed.substringAfter(':', missingDelimiterValue = "")
    val port = if (portText.isEmpty()) defaultPort else portText.toIntOrNull()
    val valid = host.isNotEmpty() && host.none { it.isWhitespace() } && port != null && port in 1..65_535
    return if (valid && port != null) PhoneSource(name = host, host = host, port = port) else null
}

/** What the TV is showing, as the connection to the phone stands. */
sealed interface LiveState {
    /** Not reached the phone yet. */
    data object Connecting : LiveState

    /** The slide the room should see. */
    data class Showing(val slide: Slide) : LiveState

    /**
     * The phone went away — locked, out of Wi-Fi, closed. The last slide stays
     * on screen while the TV keeps trying: blacking out the room mid-song is
     * worse than a slide that is a few seconds stale.
     */
    data class Lost(val lastSlide: Slide?) : LiveState

    /** The phone ended the presentation. */
    data object Ended : LiveState

    /** The phone speaks a newer protocol than this TV understands. */
    data object TooNew : LiveState
}

/** What one message from the phone means for the screen. */
internal sealed interface EnvelopeOutcome {
    data class Show(val slide: Slide) : EnvelopeOutcome
    data object Ended : EnvelopeOutcome
    data object TooNew : EnvelopeOutcome

    /** A keep-alive, an out-of-order repeat, or something unreadable. */
    data object Ignore : EnvelopeOutcome
}

private val envelopeJson = Json { ignoreUnknownKeys = true }

/**
 * Reads one message from the phone, given the newest revision already shown.
 *
 * The same rules as the browser page (`app.js`), so a TV and a browser on the
 * same phone never disagree: an older revision than the last is a delayed
 * repeat and is dropped; a newer protocol is said plainly rather than half
 * drawn; a clear with no slide is the blank screen.
 *
 * @return what to show, and the revision to remember next.
 */
internal fun readEnvelope(text: String, lastRev: Long): Pair<EnvelopeOutcome, Long> {
    val envelope = runCatching { envelopeJson.decodeFromString<SlideEnvelope>(text) }.getOrNull()
        ?: return EnvelopeOutcome.Ignore to lastRev
    if (envelope.v > SLIDE_PROTOCOL_VERSION) return EnvelopeOutcome.TooNew to lastRev
    if (envelope.rev < lastRev) return EnvelopeOutcome.Ignore to lastRev
    val rev = envelope.rev
    return when {
        envelope.type == SlideMessageType.PING -> EnvelopeOutcome.Ignore
        envelope.type == SlideMessageType.BYE -> EnvelopeOutcome.Ended
        envelope.slide != null -> EnvelopeOutcome.Show(envelope.slide)
        envelope.type == SlideMessageType.CLEAR -> EnvelopeOutcome.Show(Slide.BLANK)
        else -> EnvelopeOutcome.Ignore
    } to rev
}

/** The wait before the next reconnect attempt: doubling, from the minimum, up to the maximum. */
internal fun nextReconnectDelay(current: Long): Long =
    (current * 2).coerceIn(RECONNECT_MIN_MS, RECONNECT_MAX_MS)

/**
 * The phone's live slides, for as long as this is collected, reconnecting for
 * ever. A lobby TV is left on all week; the phone comes and goes.
 *
 * @param connect Opens the socket and hands each text message to the block.
 *   A seam over the Ktor client, so the reconnect behaviour can be tested
 *   without a phone.
 */
internal fun phoneLiveStates(
    source: PhoneSource,
    connect: suspend (url: String, onText: suspend (String) -> Unit) -> Unit,
): Flow<LiveState> = channelFlow {
    var lastSlide: Slide? = null
    var waitMs = RECONNECT_MIN_MS
    send(LiveState.Connecting)
    while (true) {
        var lastRev = -1L
        // Whatever ends the connection — a refused socket, a dropped Wi-Fi, a
        // phone that closed — means the same thing here: show the last slide
        // and try again. apiRunCatching still lets cancellation through.
        apiRunCatching {
            connect(source.liveUrl) { text ->
                waitMs = RECONNECT_MIN_MS
                val (outcome, rev) = readEnvelope(text, lastRev)
                lastRev = rev
                when (outcome) {
                    is EnvelopeOutcome.Show -> {
                        lastSlide = outcome.slide
                        send(LiveState.Showing(outcome.slide))
                    }
                    EnvelopeOutcome.Ended -> send(LiveState.Ended)
                    EnvelopeOutcome.TooNew -> send(LiveState.TooNew)
                    EnvelopeOutcome.Ignore -> Unit
                }
            }
        }.onFailure { Logger.d(TAG, "connection to ${source.liveUrl} failed: ${it.message}") }
        send(LiveState.Lost(lastSlide))
        delay(waitMs)
        waitMs = nextReconnectDelay(waitMs)
    }
}

/** [phoneLiveStates] over a real WebSocket client. */
fun phoneLiveStates(source: PhoneSource, client: HttpClient): Flow<LiveState> =
    phoneLiveStates(source) { url, onText ->
        client.webSocket(urlString = url) {
            for (frame in incoming) {
                if (frame is Frame.Text) onText(frame.readText())
            }
        }
    }
