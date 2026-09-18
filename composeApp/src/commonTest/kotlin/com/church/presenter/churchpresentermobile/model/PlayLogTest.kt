package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The play log's models: what identifies a song, what a CCLI number looks like, what survives disk. */
class PlayLogTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── Identity ─────────────────────────────────────────────────────────

    @Test
    fun `a library song is identified by its id`() {
        // The id survives a retitle; the title does not.
        val before = SongCredit(id = "abc", number = "42", title = "Amazing Grace", songbook = "Hymnal")
        val after = before.copy(title = "Amazing Grace (My Chains Are Gone)")

        assertEquals(before.key, after.key)
    }

    @Test
    fun `a song with no id is identified by book, number and title, ignoring case`() {
        val a = SongCredit(number = "42", title = "Amazing Grace", songbook = "Hymnal")
        val b = SongCredit(number = "42", title = "amazing grace", songbook = "Hymnal")
        val other = SongCredit(number = "42", title = "Amazing Grace", songbook = "Gospel Songs")

        assertEquals(a.key, b.key)
        assertTrue(a.key != other.key, "a different songbook is a different song")
    }

    @Test
    fun `a verse is identified by translation, book, chapter and verse`() {
        val kjv = VerseCredit(bibleName = "KJV", bookName = "John", chapter = 3, verse = 16)

        assertEquals("KJV::John::3::16", kjv.key)
        assertEquals("John 3:16", kjv.reference)
        assertTrue(kjv.key != kjv.copy(bibleName = "NIV").key, "the same verse in another translation is its own row")
    }

    // ── CCLI numbers ─────────────────────────────────────────────────────

    @Test
    fun `the CCLI song number is read out of a copyright line`() {
        assertEquals("22025", SongCredit.ccliNumberIn("CCLI Song # 22025"))
        assertEquals("7011351", SongCredit.ccliNumberIn("© 2012 Sixsteps Music · CCLI #7011351"))
        assertEquals("4348399", SongCredit.ccliNumberIn("ccli: 4348399"))
        assertEquals("12345", SongCredit.ccliNumberIn("CCLI Song No. 12345"))
    }

    @Test
    fun `a licence number is not a song number`() {
        // Every church's footer carries its own licence; treating it as the song
        // would put the same number on every row of the report.
        assertEquals("", SongCredit.ccliNumberIn("CCLI License #123456"))
        assertEquals("", SongCredit.ccliNumberIn("Used by permission. CCLI Licence 987654"))
    }

    @Test
    fun `no number is an empty string, never a crash`() {
        assertEquals("", SongCredit.ccliNumberIn(null))
        assertEquals("", SongCredit.ccliNumberIn(""))
        assertEquals("", SongCredit.ccliNumberIn("Public Domain"))
        assertEquals("", SongCredit.ccliNumberIn("CCLI 12"), "too short to be a song number")
    }

    // ── The log ──────────────────────────────────────────────────────────

    @Test
    fun `an empty log has no earliest play`() {
        assertTrue(PlayLog.EMPTY.isEmpty)
        assertNull(PlayLog.EMPTY.earliest)
    }

    @Test
    fun `the earliest play is the earliest of songs and verses together`() {
        val log = PlayLog(
            songs = listOf(SongPlay(ReportFixtures.amazingGrace, at = 5_000L)),
            verses = listOf(VersePlay(ReportFixtures.john316, at = 2_000L)),
        )

        assertEquals(2_000L, log.earliest)
    }

    @Test
    fun `the log survives a round trip through JSON`() {
        val encoded = json.encodeToString(PlayLog.serializer(), ReportFixtures.log)

        assertEquals(ReportFixtures.log, json.decodeFromString(PlayLog.serializer(), encoded))
    }

    @Test
    fun `a log from a newer build with fields this one does not know still reads`() {
        val text = """{"version":2,"songs":[{"credit":{"title":"X","newField":1},"at":5}],"verses":[],"extra":true}"""

        val log = json.decodeFromString(PlayLog.serializer(), text)

        assertEquals("X", log.songs.single().credit.title)
        assertEquals(5L, log.songs.single().at)
    }
}
