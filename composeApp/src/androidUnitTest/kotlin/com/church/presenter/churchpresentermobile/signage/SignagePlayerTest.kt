package com.church.presenter.churchpresentermobile.signage

import kotlinx.coroutines.test.runTest
import org.churchpresenter.signage.core.Features
import org.churchpresenter.signage.core.Manifest
import org.churchpresenter.signage.core.MediaKind
import org.churchpresenter.signage.core.Platform
import org.churchpresenter.signage.core.PlayerMessage
import org.churchpresenter.signage.core.Playlist
import org.churchpresenter.signage.core.PlaylistItem
import org.churchpresenter.signage.core.ScreenSettings
import org.churchpresenter.signage.core.ServerMessage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The TV as a signage player. It has to do what the Pi player does in every way
 * the desktop can see: a wrong hello is refused, a half-downloaded playlist must
 * never replace the one playing, and a stranger's manifest must never be taken.
 */
class SignagePlayerTest {

    private val dir: File = Files.createTempDirectory("signage").toFile()
    private val store = SignageStore(dir)

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun sha(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun image(content: String, durationMs: Long? = 5_000) =
        PlaylistItem(sha256 = sha(content), kind = MediaKind.IMAGE, durationMs = durationMs)

    private fun manifest(vararg items: PlaylistItem, owner: String = "church") = Manifest(
        ownerId = owner,
        screen = ScreenSettings(name = "Lobby"),
        playlists = listOf(Playlist("p1", "Welcome", items.toList())),
        defaultPlaylistId = "p1",
        timeZone = "UTC",
    )

    private fun paired() = store.saveIdentity(store.identity().copy(token = "t", ownerId = "church"))

    /** A source serving [files], by content. */
    private fun source(manifest: Manifest, version: String = "v1", vararg files: String) = object : ContentSource {
        override suspend fun manifest() = version to manifest

        override suspend fun <T> file(sha256: String, consume: (InputStream) -> T): T {
            val content = files.firstOrNull { sha(it) == sha256 } ?: throw IOException("no such file")
            return consume(ByteArrayInputStream(content.toByteArray()))
        }
    }

    private val info = PlayerInfo("1.0", 1920, 1080)

    private fun player(now: Instant = Instant.EPOCH) =
        SignagePlayer(store, sync = { SyncResult.UpToDate }, now = { now })

    // ── Hello ────────────────────────────────────────────────────────────

    @Test
    fun `an unpaired TV gives its code in its hello`() {
        val hello = helloFor(PlayerIdentity("id"), info, null, "123456")

        assertEquals(Platform.ANDROID_TV, hello.platform)
        assertEquals("123456", hello.pairingCode)
    }

    @Test
    fun `a paired TV gives no code`() {
        val identity = PlayerIdentity("id", token = "t", ownerId = "church")

        assertNull(helloFor(identity, info, "v1", "123456").pairingCode)
    }

    @Test
    fun `a TV only lists what it can do`() {
        // The desktop sends only what is listed: a `web` item or an update offer
        // would reach a player that cannot act on it.
        assertFalse(Features.WEB in TV_FEATURES)
        assertFalse(Features.UPDATE in TV_FEATURES)
        assertTrue(Features.WIPE in TV_FEATURES)
        assertTrue(Features.PREVIEW in TV_FEATURES)
    }

    @Test
    fun `a preview is no wider than asked and keeps the screen's shape`() {
        assertEquals(480 to 270, previewSize(1920, 1080, 480))
    }

    @Test
    fun `a preview is never enlarged`() {
        assertEquals(320 to 180, previewSize(320, 180, 480))
    }

    @Test
    fun `a preview request is answered with what is on screen`() = runTest {
        val picture = PlayerMessage.Preview("jpeg", 480, 270, 0)
        var asked: Pair<Int, SignageScreen>? = null
        val player = SignagePlayer(
            store,
            sync = { SyncResult.UpToDate },
            previews = { width, screen ->
                asked = width to screen
                picture
            },
        )

        player.handle(ServerMessage.PreviewRequest(480), "a:1")

        assertEquals(480 to SignageScreen.Searching, asked)
        assertEquals(picture, player.outgoing.receive())
    }

    // ── Sync ─────────────────────────────────────────────────────────────

    @Test
    fun `a manifest and its files are stored together`() = runTest {
        paired()

        val result = ManifestSync(store, source(manifest(image("a")), "v1", "a")).sync()

        assertEquals(SyncResult.Updated("v1", 1), result)
        assertTrue(store.files.has(sha("a")))
        assertEquals("v1", store.manifest()?.version)
    }

    @Test
    fun `a manifest from another owner is refused`() = runTest {
        // A second ChurchPresenter on the network must not take the screen over.
        paired()

        val result = ManifestSync(store, source(manifest(image("a"), owner = "stranger"), "v1", "a")).sync()

        assertIs<SyncResult.Failed>(result)
        assertNull(store.manifest())
    }

    @Test
    fun `a failed download leaves the old manifest playing`() = runTest {
        paired()
        ManifestSync(store, source(manifest(image("a")), "v1", "a")).sync()

        val result = ManifestSync(store, source(manifest(image("b")), "v2")).sync()

        assertIs<SyncResult.Failed>(result)
        assertEquals("v1", store.manifest()?.version)
    }

    @Test
    fun `a file that does not match its hash is not kept`() {
        assertFalse(store.files.put(sha("a"), ByteArrayInputStream("not a".toByteArray())))
        assertFalse(store.files.has(sha("a")))
    }

    @Test
    fun `an up-to-date manifest fetches nothing`() = runTest {
        paired()
        ManifestSync(store, source(manifest(image("a")), "v1", "a")).sync()

        assertEquals(SyncResult.UpToDate, ManifestSync(store, source(manifest(image("a")), "v1", "a")).sync())
    }

    @Test
    fun `files the new manifest does not name are removed`() = runTest {
        paired()
        ManifestSync(store, source(manifest(image("a")), "v1", "a")).sync()

        ManifestSync(store, source(manifest(image("b")), "v2", "b")).sync()

        assertFalse(store.files.has(sha("a")))
        assertTrue(store.files.has(sha("b")))
    }

    // ── Playing ──────────────────────────────────────────────────────────

    @Test
    fun `a picture moves on when its time is up`() {
        val items = listOf(image("a", durationMs = 1_000), image("b", durationMs = 1_000))
        val cursor = PlaylistCursor()

        assertEquals(items[0], cursor.current("p", items, 0))
        assertEquals(items[0], cursor.current("p", items, 999))
        assertEquals(items[1], cursor.current("p", items, 1_000))
        assertEquals(items[0], cursor.current("p", items, 2_000))
    }

    @Test
    fun `a video with no time plays to its end`() {
        val video = PlaylistItem(sha256 = sha("v"), kind = MediaKind.VIDEO)
        val items = listOf(video, image("a"))
        val cursor = PlaylistCursor()

        assertEquals(video, cursor.current("p", items, 0))
        assertEquals(video, cursor.current("p", items, 3_600_000))

        cursor.itemEnded(items, 3_600_000)

        assertEquals(items[1], cursor.current("p", items, 3_600_000))
    }

    @Test
    fun `a new playlist starts from its first item`() {
        val items = listOf(image("a", 1_000), image("b", 1_000))
        val cursor = PlaylistCursor()
        cursor.current("p", items, 0)
        cursor.current("p", items, 1_000)

        assertEquals(items[0], cursor.current("q", items, 1_500))
    }

    @Test
    fun `only what this TV holds counts as playable`() {
        val stream = PlaylistItem(kind = MediaKind.STREAM, url = "https://example.org/live")
        val web = PlaylistItem(kind = MediaKind.WEB, url = "https://example.org")
        val playlist = Playlist("p", "P", listOf(image("held"), image("missing"), stream, web))

        val playable = playableItems(playlist) { it == sha("held") }

        assertEquals(listOf(image("held"), stream), playable)
    }

    @Test
    fun `with its files here the TV plays with no connection at all`() = runTest {
        // The whole point: the computer is off.
        paired()
        ManifestSync(store, source(manifest(image("a")), "v1", "a")).sync()
        val player = player()

        player.tick()

        assertEquals(store.files.file(sha("a")), assertIs<SignageScreen.Playing>(player.screen.value).file)
    }

    @Test
    fun `an unpaired TV is still looking`() {
        val player = player()

        player.tick()

        assertEquals(SignageScreen.Searching, player.screen.value)
    }

    // ── Messages ─────────────────────────────────────────────────────────

    @Test
    fun `pairing stores the owner and reconnects with the token`() = runTest {
        val next = player().handle(ServerMessage.Paired("token", "church", "First Church"), "a:1")

        assertEquals(SessionNext.RECONNECT, next)
        assertEquals("church", store.identity().ownerId)
        assertTrue(store.identity().isPaired)
    }

    @Test
    fun `waiting for approval shows the pairing code`() = runTest {
        val player = player()

        player.handle(ServerMessage.PairingPending("ignored"), "a:1")

        assertIs<SignageScreen.Pairing>(player.screen.value)
    }

    @Test
    fun `being released forgets the owner and everything stored`() = runTest {
        paired()
        ManifestSync(store, source(manifest(image("a")), "v1", "a")).sync()

        player().handle(ServerMessage.Released, "a:1")

        assertFalse(store.identity().isPaired)
        assertNull(store.manifest())
        assertFalse(store.files.has(sha("a")))
    }

    @Test
    fun `a desktop that speaks another protocol is left alone for a while`() = runTest {
        assertEquals(SessionNext.WAIT_LONG, player().handle(ServerMessage.Unsupported(9), "a:1"))
    }
}
