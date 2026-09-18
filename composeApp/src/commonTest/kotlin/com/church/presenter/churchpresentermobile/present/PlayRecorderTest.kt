package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.library.PlayLogRepository
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.model.SongCredit
import com.church.presenter.churchpresentermobile.model.VerseCredit
import com.church.presenter.churchpresentermobile.testutil.FakeOutputSink
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * When a play is counted.
 *
 * The rule the report stands on: a song or verse counts once it is actually in
 * front of the congregation, and a song counts once however many of its
 * sections are shown. Everything here is one of the ways that can go wrong —
 * a rehearsal with nothing plugged in, a blank toggled on and off, a display
 * that drops and comes back — and the count each must leave behind.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayRecorderTest {

    private val amazingGrace = SongCredit(id = "s1", number = "42", title = "Amazing Grace", songbook = "Hymnal")
    private val howGreat = SongCredit(id = "s2", number = "7", title = "How Great Thou Art", songbook = "Hymnal")
    private val john316 = VerseCredit(bibleName = "KJV", bookName = "John", chapter = 3, verse = 16)
    private val john317 = john316.copy(verse = 17)

    private class Fixture {
        val storage = InMemoryFileStorage()
        var clock = 1_000L
        val log = PlayLogRepository(storage) { clock }
        val slides = MutableStateFlow(Slide.BLANK)
        val outputs = MutableStateFlow(listOf(SinkStatus("tv", "TV", SinkState.ATTACHED, clientCount = 1)))
        val recorder = PlayRecorder(slides, outputs, log)

        val songTitles: List<String> get() = log.log.value.songs.map { it.credit.title }
        val verseRefs: List<String> get() = log.log.value.verses.map { it.credit.reference }

        fun noScreen() { outputs.value = listOf(SinkStatus("tv", "TV", SinkState.DETACHED)) }
        fun screen() { outputs.value = listOf(SinkStatus("tv", "TV", SinkState.ATTACHED, clientCount = 1)) }
    }

    private fun song(credit: SongCredit, section: Int = 0, hidden: Boolean = false) = Slide(
        kind = SlideKind.SONG,
        body = "words of section $section",
        songCredit = credit,
        index = section,
        isBlank = hidden,
    )

    private fun verse(credit: VerseCredit) = Slide(kind = SlideKind.BIBLE, body = "verse", verseCredit = credit)

    // ── The basic count ──────────────────────────────────────────────────

    @Test
    fun `a song on a screen is one play`() {
        val f = Fixture()

        f.recorder.observe(song(amazingGrace), onScreen = true)

        assertEquals(listOf("Amazing Grace"), f.songTitles)
    }

    @Test
    fun `stepping through a song's sections is still one play`() {
        // The desktop counts a song when it goes live, not on every section
        // change, and a report that said "Amazing Grace × 4" for its four verses
        // would over-report the licence fourfold.
        val f = Fixture()

        (0 until 4).forEach { f.recorder.observe(song(amazingGrace, section = it), onScreen = true) }

        assertEquals(listOf("Amazing Grace"), f.songTitles)
    }

    @Test
    fun `every verse shown is its own play`() {
        val f = Fixture()

        f.recorder.observe(verse(john316), onScreen = true)
        f.recorder.observe(verse(john317), onScreen = true)

        assertEquals(listOf("John 3:16", "John 3:17"), f.verseRefs)
    }

    @Test
    fun `a song sung twice with something else between counts twice`() {
        val f = Fixture()

        f.recorder.observe(song(amazingGrace), onScreen = true)
        f.recorder.observe(song(howGreat), onScreen = true)
        f.recorder.observe(song(amazingGrace), onScreen = true)

        assertEquals(listOf("Amazing Grace", "How Great Thou Art", "Amazing Grace"), f.songTitles)
    }

    @Test
    fun `a cleared screen separates two plays of the same song`() {
        // What the engine emits for a clear: nothing loaded, and not blanked —
        // the screen is showing its backdrop, on purpose.
        val f = Fixture()
        val cleared = Slide(kind = SlideKind.BLANK)

        f.recorder.observe(song(amazingGrace), onScreen = true)
        f.recorder.observe(cleared, onScreen = true)
        f.recorder.observe(song(amazingGrace), onScreen = true)

        assertEquals(listOf("Amazing Grace", "Amazing Grace"), f.songTitles)
    }

    @Test
    fun `a photo between two plays of the same song separates them too`() {
        val f = Fixture()
        val photo = Slide(kind = SlideKind.IMAGE, backdrop = SlideBackdrop.IMAGE, backdropUrl = "http://p/1.jpg")

        f.recorder.observe(song(amazingGrace), onScreen = true)
        f.recorder.observe(photo, onScreen = true)
        f.recorder.observe(song(amazingGrace), onScreen = true)

        assertEquals(listOf("Amazing Grace", "Amazing Grace"), f.songTitles)
    }

    // ── Only when projected ──────────────────────────────────────────────

    @Test
    fun `nothing is counted with no screen attached`() {
        // Loading and stepping through a song with nothing plugged in is a
        // rehearsal, and rehearsals are not reportable use.
        val f = Fixture()

        f.recorder.observe(song(amazingGrace), onScreen = false)
        f.recorder.observe(verse(john316), onScreen = false)

        assertTrue(f.log.log.value.isEmpty)
    }

    @Test
    fun `a blanked song is not counted`() {
        val f = Fixture()

        f.recorder.observe(song(amazingGrace, hidden = true), onScreen = true)

        assertTrue(f.log.log.value.isEmpty)
    }

    @Test
    fun `a song held off-air is not counted`() {
        val f = Fixture()

        f.recorder.observe(song(amazingGrace).copy(isLive = false), onScreen = true)

        assertTrue(f.log.log.value.isEmpty)
    }

    @Test
    fun `a song already up counts the moment a screen attaches`() {
        // The operator went live before the TV was plugged in. The song reaches
        // the congregation when the screen does, and that is when it counts.
        val f = Fixture()

        f.recorder.observe(song(amazingGrace), onScreen = false)
        f.recorder.observe(song(amazingGrace), onScreen = true)

        assertEquals(listOf("Amazing Grace"), f.songTitles)
    }

    @Test
    fun `a song moved on from before any screen attached is never counted`() {
        val f = Fixture()

        f.recorder.observe(song(amazingGrace), onScreen = false)
        f.recorder.observe(song(howGreat), onScreen = false)
        f.recorder.observe(song(howGreat), onScreen = true)

        assertEquals(listOf("How Great Thou Art"), f.songTitles)
    }

    // ── What must not count again ────────────────────────────────────────

    @Test
    fun `blanking and restoring the same song is one play`() {
        val f = Fixture()

        f.recorder.observe(song(amazingGrace), onScreen = true)
        f.recorder.observe(song(amazingGrace, hidden = true), onScreen = true)
        f.recorder.observe(song(amazingGrace), onScreen = true)

        assertEquals(listOf("Amazing Grace"), f.songTitles)
    }

    @Test
    fun `taking the output off-air and back is one play`() {
        val f = Fixture()

        f.recorder.observe(song(amazingGrace), onScreen = true)
        f.recorder.observe(song(amazingGrace).copy(isLive = false), onScreen = true)
        f.recorder.observe(song(amazingGrace), onScreen = true)

        assertEquals(listOf("Amazing Grace"), f.songTitles)
    }

    @Test
    fun `a display dropping and returning is one play`() {
        val f = Fixture()

        f.recorder.observe(song(amazingGrace), onScreen = true)
        f.recorder.observe(song(amazingGrace), onScreen = false)
        f.recorder.observe(song(amazingGrace), onScreen = true)

        assertEquals(listOf("Amazing Grace"), f.songTitles)
    }

    @Test
    fun `restyling the slide is not another play`() {
        val f = Fixture()

        f.recorder.observe(song(amazingGrace), onScreen = true)
        f.recorder.observe(song(amazingGrace).copy(textSize = SlideTextSize.LARGE), onScreen = true)
        f.recorder.observe(song(amazingGrace).copy(theme = SlideTheme(showClock = false)), onScreen = true)

        assertEquals(listOf("Amazing Grace"), f.songTitles)
    }

    @Test
    fun `going back to a verse counts it again`() {
        // Navigating 16 → 17 → 16 showed verse 16 twice, and the desktop counts it so.
        val f = Fixture()

        f.recorder.observe(verse(john316), onScreen = true)
        f.recorder.observe(verse(john317), onScreen = true)
        f.recorder.observe(verse(john316), onScreen = true)

        assertEquals(listOf("John 3:16", "John 3:17", "John 3:16"), f.verseRefs)
    }

    // ── Which screens count ──────────────────────────────────────────────

    @Test
    fun `an attached web page nobody has opened is not a screen`() {
        // The web sink is attached the moment its server is up. Attached is not
        // watched: no browser has connected, so nothing is in front of anyone.
        val idle = SinkStatus("web", "Web page", SinkState.ATTACHED, clientCount = 0)

        assertTrue(!idle.isOnScreen)
    }

    @Test
    fun `an attached display is a screen and a detached one is not`() {
        assertTrue(SinkStatus("tv", "TV", SinkState.ATTACHED, clientCount = 1).isOnScreen)
        assertTrue(!SinkStatus("tv", "TV", SinkState.DETACHED, clientCount = 0).isOnScreen)
        assertTrue(!SinkStatus("tv", "TV", SinkState.ERROR, clientCount = 0).isOnScreen)
    }

    // ── Wired to the engine ──────────────────────────────────────────────

    @Test
    fun `run follows the engine and the registry`() = runTest {
        // End to end through the real engine: what the operator does, and
        // what the report says afterwards.
        val storage = InMemoryFileStorage()
        val log = PlayLogRepository(storage) { 5_000L }
        val registry = SinkRegistry()
        val sink = FakeOutputSink()
        registry.register(sink)
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), registry) { }
        val recorder = PlayRecorder(engine.currentSlide, registry.statuses, log)
        val job = launch { recorder.run() }
        val deck = SlideDeck(
            kind = SlideKind.SONG,
            title = "42 Amazing Grace",
            slides = List(3) { song(amazingGrace, section = it) },
        )

        engine.setDeck(deck)
        advanceUntilIdle()
        engine.next()
        advanceUntilIdle()
        assertTrue(log.log.value.isEmpty, "nothing attached yet, so nothing projected")

        sink.attach()
        registry.refreshStatuses()
        advanceUntilIdle()
        assertEquals(listOf("Amazing Grace"), log.log.value.songs.map { it.credit.title })

        engine.next()
        advanceUntilIdle()
        engine.toggleBlank()
        advanceUntilIdle()
        engine.toggleBlank()
        advanceUntilIdle()
        assertEquals(1, log.log.value.songs.size, "sections and blanks are not plays")

        engine.setDeck(SlideDeck(kind = SlideKind.BIBLE, slides = listOf(verse(john316), verse(john317))))
        advanceUntilIdle()
        engine.next()
        advanceUntilIdle()
        assertEquals(listOf("John 3:16", "John 3:17"), log.log.value.verses.map { it.credit.reference })

        engine.clear()
        advanceUntilIdle()
        engine.setDeck(deck)
        advanceUntilIdle()
        assertEquals(2, log.log.value.songs.size, "the same song after a clear is a second play")

        job.cancel()
    }
}
