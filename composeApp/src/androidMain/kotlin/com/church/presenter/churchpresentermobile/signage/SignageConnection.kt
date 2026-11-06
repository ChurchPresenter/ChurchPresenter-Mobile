package com.church.presenter.churchpresentermobile.signage

import com.church.presenter.churchpresentermobile.network.apiRunCatching
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.bearerAuth
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.churchpresenter.signage.core.PlayerMessage
import org.churchpresenter.signage.core.ServerMessage
import org.churchpresenter.signage.core.SignageJson

private const val TAG = "SignageConnection"

/** Shortest and longest pause between attempts to reach ChurchPresenter. */
private const val MIN_BACKOFF_MS = 2_000L
private const val MAX_BACKOFF_MS = 15_000L

/** The pause after a ChurchPresenter that speaks another protocol, which will not change soon. */
private const val UNSUPPORTED_WAIT_MS = 60_000L
private const val STATUS_EVERY_MS = 60_000L

/**
 * Keeps one WebSocket open to ChurchPresenter whenever it is reachable, and gives
 * up gracefully when it is not — the player plays on regardless. The Pi player's
 * `ServerConnection`, for a TV.
 *
 * @param addresses What is advertising on the network now, as `host:port`.
 */
internal class SignageConnection(
    private val client: HttpClient,
    private val store: SignageStore,
    private val player: SignagePlayer,
    private val info: PlayerInfo,
    private val addresses: () -> List<String>,
) {
    /** Keeps trying, for as long as this runs. */
    suspend fun run() {
        var backoff = MIN_BACKOFF_MS
        while (true) {
            val next = connectToAny()
            backoff = when (next) {
                SessionNext.RECONNECT -> MIN_BACKOFF_MS
                SessionNext.WAIT_LONG -> UNSUPPORTED_WAIT_MS
                SessionNext.STAY, null -> (backoff * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
            if (next != SessionNext.RECONNECT) delay(backoff)
        }
    }

    /**
     * The address that last answered, then whatever advertises. The last address
     * goes first so a paired TV does not wait on mDNS after a restart.
     */
    private suspend fun connectToAny(): SessionNext? {
        val candidates = (listOfNotNull(store.identity().lastAddress) + addresses()).distinct()
        for (address in candidates) {
            val result = apiRunCatching { connect(address) }
            result.onFailure { Logger.d(TAG, "ChurchPresenter not reachable at $address: ${it.message}") }
            result.getOrNull()?.let { return it }
        }
        return null
    }

    private suspend fun connect(address: String): SessionNext {
        var next = SessionNext.STAY
        val identity = store.identity()
        client.webSocket(urlString = "ws://$address/api/signage/ws", request = { identity.token?.let(::bearerAuth) }) {
            store.saveIdentity(identity.copy(lastAddress = address))
            val hello = helloFor(identity, info, store.manifest()?.version, player.pairingCode)
            send(Frame.Text(encode(hello)))
            if (!identity.isPaired) player.awaitingApproval()
            val reporter = launch {
                while (true) {
                    send(Frame.Text(encode(player.status())))
                    delay(STATUS_EVERY_MS)
                }
            }
            val sender = launch {
                for (message in player.outgoing) send(Frame.Text(encode(message)))
            }
            if (identity.isPaired) player.syncFrom(address)
            for (frame in incoming) {
                decoded(frame)?.let { next = player.handle(it, address) }
                if (next != SessionNext.STAY) break
            }
            reporter.cancel()
            sender.cancel()
        }
        return next
    }

    private fun encode(message: PlayerMessage): String =
        SignageJson.encodeToString(PlayerMessage.serializer(), message)

    /** The message in [frame], or null for one this player cannot read: both sides ignore those. */
    private fun decoded(frame: Frame): ServerMessage? = (frame as? Frame.Text)?.let {
        runCatching { SignageJson.decodeFromString(ServerMessage.serializer(), it.readText()) }.getOrNull()
    }
}
