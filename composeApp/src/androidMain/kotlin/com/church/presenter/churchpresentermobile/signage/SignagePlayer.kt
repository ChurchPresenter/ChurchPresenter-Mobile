package com.church.presenter.churchpresentermobile.signage

import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.churchpresenter.signage.core.CecState
import org.churchpresenter.signage.core.Features
import org.churchpresenter.signage.core.MediaKind
import org.churchpresenter.signage.core.PROTOCOL_VERSION
import org.churchpresenter.signage.core.Platform
import org.churchpresenter.signage.core.PlayerMessage
import org.churchpresenter.signage.core.Playlist
import org.churchpresenter.signage.core.PlaylistItem
import org.churchpresenter.signage.core.ServerMessage
import org.churchpresenter.signage.core.Show
import org.churchpresenter.signage.core.decide
import java.io.File
import java.time.Instant
import kotlin.random.Random

private const val TAG = "SignagePlayer"

/** How often the TV re-decides what to show, as the Pi player does. */
internal const val TICK_MS = 1_000L

/** How long a picture stays up when its item gives no time. */
internal const val DEFAULT_IMAGE_MS = 10_000L

private const val PAIRING_CODE_DIGITS = 6
private const val PAIRING_CODE_RANGE = 1_000_000
private const val MILLIS_PER_SECOND = 1_000L

/** What a TV can do. No web pages or self-update: see `SignageMode`. */
internal val TV_FEATURES = listOf(Features.WIPE, Features.STREAM, Features.PREVIEW)

/** Previews waiting to be sent. Only the latest matters, so a slow socket drops the rest. */
private const val OUTGOING_CAPACITY = 2

/** What the screen should be drawing. */
internal sealed interface SignageScreen {
    /** Not paired, and nothing has answered yet. */
    data object Searching : SignageScreen

    /** Waiting for someone to approve [code] in the desktop's Signage tab. */
    data class Pairing(val code: String) : SignageScreen

    /** Paired, with nothing playable: the church's idle card. */
    data class Idle(val title: String, val logo: File?) : SignageScreen

    /** Meant to be off. */
    data object Blank : SignageScreen

    /** A picture, a video file or a stream. */
    data class Playing(val item: PlaylistItem, val file: File?) : SignageScreen
}

/** The items of [playlist] this TV can show now: files it holds, and streams. */
internal fun playableItems(playlist: Playlist, has: (String) -> Boolean): List<PlaylistItem> =
    playlist.items.filter { item ->
        when (item.kind) {
            MediaKind.IMAGE, MediaKind.VIDEO -> has(item.sha256)
            MediaKind.STREAM -> !item.url.isNullOrBlank()
            MediaKind.WEB -> false
        }
    }

/**
 * Where a playlist has got to. A picture moves on when its time is up; a video
 * with no time set moves on when it ends ([itemEnded]). A single item just stays.
 */
internal class PlaylistCursor {
    private var playlistId: String? = null
    private var index = 0
    private var startedAtMs = 0L

    /** The item to show at [nowMs], moving on if the one before it is done. */
    fun current(playlistId: String, items: List<PlaylistItem>, nowMs: Long): PlaylistItem? {
        if (items.isEmpty()) return null
        if (playlistId != this.playlistId) {
            this.playlistId = playlistId
            index = 0
            startedAtMs = nowMs
        }
        index %= items.size
        val duration = durationOf(items[index])
        if (duration != null && nowMs - startedAtMs >= duration) {
            index = (index + 1) % items.size
            startedAtMs = nowMs
        }
        return items[index]
    }

    /** The video on screen played to its end. */
    fun itemEnded(items: List<PlaylistItem>, nowMs: Long) {
        if (items.isEmpty()) return
        index = (index + 1) % items.size
        startedAtMs = nowMs
    }

    private fun durationOf(item: PlaylistItem): Long? = item.durationMs
        ?: DEFAULT_IMAGE_MS.takeIf { item.kind == MediaKind.IMAGE }
}

/** What a TV says about itself in its hello. */
internal data class PlayerInfo(val version: String, val width: Int, val height: Int)

/** The hello a TV opens every connection with. */
internal fun helloFor(
    identity: PlayerIdentity,
    info: PlayerInfo,
    manifestVersion: String?,
    pairingCode: String,
): PlayerMessage.Hello = PlayerMessage.Hello(
    playerId = identity.playerId,
    agentVersion = info.version,
    protocolMin = PROTOCOL_VERSION,
    protocolMax = PROTOCOL_VERSION,
    manifestVersion = manifestVersion,
    screenWidth = info.width,
    screenHeight = info.height,
    platform = Platform.ANDROID_TV,
    pairingCode = pairingCode.takeUnless { identity.isPaired },
    features = TV_FEATURES,
)

/** What ended a connection, and so how soon to try again. */
internal enum class SessionNext { STAY, RECONNECT, WAIT_LONG }

/**
 * The TV as a signage player: what it shows, and what it does with each message.
 * It plays what it holds whether ChurchPresenter can be reached or not; the
 * reaching is [SignageConnection]'s.
 *
 * The same behaviour as the Pi player's `PlayerSession` and `PlayerEngine`,
 * without what a TV has no use for.
 *
 * @param sync Fetches the manifest from the given address.
 * @param now The clock. A seam so a test can drive the schedule.
 * @param previews Takes a picture no wider than the width given, of the screen given.
 */
internal class SignagePlayer(
    private val store: SignageStore,
    private val sync: suspend (address: String) -> SyncResult,
    private val now: () -> Instant = Instant::now,
    random: Random = Random.Default,
    private val previews: (suspend (maxWidth: Int, screen: SignageScreen) -> PlayerMessage.Preview?)? = null,
) {
    val pairingCode: String = random.nextInt(PAIRING_CODE_RANGE).toString().padStart(PAIRING_CODE_DIGITS, '0')

    /** Messages for ChurchPresenter that are not sent on a timer. */
    val outgoing = Channel<PlayerMessage>(OUTGOING_CAPACITY, BufferOverflow.DROP_OLDEST)

    private val cursor = PlaylistCursor()
    private var lastSyncMs: Long? = null
    private var showingPlaylistId: String? = null
    private var showingItems: List<PlaylistItem> = emptyList()
    private var identifyUntilMs = 0L

    private val _screen = MutableStateFlow<SignageScreen>(SignageScreen.Searching)
    val screen: StateFlow<SignageScreen> = _screen.asStateFlow()

    private val _identify = MutableStateFlow<String?>(null)

    /** The screen's name while the desktop asks it to identify itself. */
    val identify: StateFlow<String?> = _identify.asStateFlow()

    /** Re-decides once a second, for as long as this runs. */
    suspend fun runScreen() {
        while (true) {
            tick()
            if (now().toEpochMilli() >= identifyUntilMs) _identify.value = null
            delay(TICK_MS)
        }
    }

    /** Works out what to show now. */
    fun tick() {
        val stored = store.manifest()
        if (stored == null) {
            if (store.identity().isPaired) _screen.value = SignageScreen.Idle("", null)
            return
        }
        val manifest = stored.manifest
        val decision = decide(manifest, store.override, now()) { playableItems(it, store.files::has).isNotEmpty() }
        _screen.value = when (val show = decision.show) {
            Show.Blank -> SignageScreen.Blank
            Show.Idle -> SignageScreen.Idle(
                manifest.idleCard.title,
                manifest.idleCard.logoSha256?.let(store.files::file),
            )
            is Show.PlaylistShow -> playing(manifest.playlist(show.playlistId))
        }
    }

    private fun playing(playlist: Playlist?): SignageScreen {
        val items = playlist?.let { playableItems(it, store.files::has) }.orEmpty()
        showingPlaylistId = playlist?.id
        showingItems = items
        val item = playlist?.let { cursor.current(it.id, items, now().toEpochMilli()) }
            ?: return SignageScreen.Idle("", null)
        return SignageScreen.Playing(item, store.files.file(item.sha256).takeIf { item.kind != MediaKind.STREAM })
    }

    /** The video on screen ended. */
    fun itemEnded() {
        cursor.itemEnded(showingItems, now().toEpochMilli())
        tick()
    }

    /** Waiting for approval: the code goes on screen. */
    fun awaitingApproval() {
        _screen.value = SignageScreen.Pairing(pairingCode)
    }

    /** Acts on one message from ChurchPresenter at [address]. */
    suspend fun handle(message: ServerMessage, address: String): SessionNext = when (message) {
        is ServerMessage.Unsupported -> SessionNext.WAIT_LONG
        is ServerMessage.Paired -> {
            store.saveIdentity(
                store.identity().copy(token = message.token, ownerId = message.ownerId, ownerName = message.ownerName)
            )
            SessionNext.RECONNECT
        }
        ServerMessage.Released -> {
            store.wipe()
            store.saveIdentity(store.identity().released())
            _screen.value = SignageScreen.Searching
            SessionNext.RECONNECT
        }
        else -> {
            apply(message, address)
            SessionNext.STAY
        }
    }

    private suspend fun apply(message: ServerMessage, address: String) {
        when (message) {
            is ServerMessage.PairingPending -> awaitingApproval()
            is ServerMessage.ManifestChanged -> syncFrom(address)
            is ServerMessage.PreviewRequest ->
                previews?.invoke(message.maxWidth, _screen.value)?.let(outgoing::trySend)
            is ServerMessage.SetOverride -> {
                store.override = message.override
                tick()
            }
            is ServerMessage.Identify -> {
                _identify.value = store.manifest()?.manifest?.screen?.name.orEmpty()
                identifyUntilMs = now().toEpochMilli() + message.seconds * MILLIS_PER_SECOND
            }
            ServerMessage.Wipe -> {
                store.wipe()
                tick()
                syncFrom(address)
            }
            // The rest are what a TV does not list in its features, or has no use for.
            else -> Unit
        }
    }

    /** Brings the manifest up to date from [address]. */
    suspend fun syncFrom(address: String) {
        val result = sync(address)
        Logger.d(TAG, "sync: $result")
        if (result !is SyncResult.Failed) lastSyncMs = now().toEpochMilli()
        tick()
    }

    /** What this TV reports about itself once a minute. */
    fun status() = PlayerMessage.Status(
        showingPlaylistId = showingPlaylistId,
        lastSyncMs = lastSyncMs,
        freeDiskBytes = store.freeBytes(),
        cec = CecState.UNAVAILABLE,
    )
}
