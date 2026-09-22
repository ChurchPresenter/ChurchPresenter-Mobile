package com.church.presenter.churchpresentermobile.ui.calendar

import com.church.presenter.churchpresentermobile.calendar.ParsedReference
import com.church.presenter.churchpresentermobile.calendar.PickerBook
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PresetSummary
import com.church.presenter.churchpresentermobile.model.RowKind
import com.church.presenter.churchpresentermobile.model.SectionPalette
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDurations
import com.church.presenter.churchpresentermobile.viewmodel.ChapterPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the "Add to service" picker has selected, and the row each tab would add from it.
 *
 * All of it is decided before anything is drawn, so it is tested without drawing: what a second
 * tap on a verse means, what a tab with nothing chosen offers (nothing), and what ends up in the
 * plan when it does.
 */
class PickerStateTest {

    private val john = PickerBook(number = 43, name = "John", chapters = 21)
    private val psalms = PickerBook(number = 19, name = "Psalms", chapters = 150)

    private val song = Song(number = "42", title = "Amazing Grace", bookName = "Hymnal")
    private val preset = PresetSummary(id = "p1", name = "Welcome loop", kind = RowKind.MEDIA)

    private fun state() = PickerState()
    private val newId: () -> String = { "row-1" }

    // ── Picking a passage ────────────────────────────────────────────────

    @Test
    fun `picking a book clears whatever was chosen inside the last one`() {
        val picker = state()
        picker.pickBook(john)
        picker.pickChapter(3)
        picker.pickVerse(16)

        picker.pickBook(psalms)

        assertEquals(psalms, picker.book)
        assertNull(picker.chapter)
        assertNull(picker.verseFrom)
        assertNull(picker.verseTo)
    }

    @Test
    fun `tapping the open book again closes it`() {
        val picker = state()
        picker.pickBook(john)

        picker.pickBook(john)

        assertNull(picker.book)
    }

    @Test
    fun `tapping the open chapter again closes it`() {
        val picker = state()
        picker.pickBook(john)
        picker.pickChapter(3)

        picker.pickChapter(3)

        assertNull(picker.chapter)
    }

    @Test
    fun `picking a chapter clears the verses of the last one`() {
        val picker = state()
        picker.pickBook(john)
        picker.pickChapter(3)
        picker.pickVerse(16)

        picker.pickChapter(4)

        assertEquals(4, picker.chapter)
        assertNull(picker.verseFrom)
    }

    @Test
    fun `a second verse after the first makes a range`() {
        val picker = state()

        picker.pickVerse(16)
        picker.pickVerse(18)

        assertEquals(16, picker.verseFrom)
        assertEquals(18, picker.verseTo)
    }

    @Test
    fun `a verse before the first starts over`() {
        val picker = state()
        picker.pickVerse(16)

        picker.pickVerse(12)

        assertEquals(12, picker.verseFrom)
        assertNull(picker.verseTo)
    }

    @Test
    fun `a third verse starts a new selection rather than widening the range`() {
        val picker = state()
        picker.pickVerse(16)
        picker.pickVerse(18)

        picker.pickVerse(20)

        assertEquals(20, picker.verseFrom)
        assertNull(picker.verseTo)
    }

    @Test
    fun `the same verse twice is not a range of one`() {
        val picker = state()
        picker.pickVerse(16)

        picker.pickVerse(16)

        assertEquals(16, picker.verseFrom)
        assertNull(picker.verseTo, "a range needs a second, later verse")
    }

    @Test
    fun `clearing leaves the book and chapter but drops everything chosen inside them`() {
        val picker = state()
        picker.pickBook(john)
        picker.pickChapter(3)
        picker.pickVerse(16)
        picker.song = song
        picker.preset = preset
        picker.ministryWhat = "Notices"
        picker.ministryWho = "Anna"
        picker.ministryDuration = "5:00"

        picker.clearSelection()

        assertEquals(john, picker.book, "the book stays open for the next add")
        assertEquals(3, picker.chapter)
        assertNull(picker.song)
        assertNull(picker.preset)
        assertNull(picker.verseFrom)
        assertEquals("", picker.ministryWhat)
        assertEquals("", picker.ministryWho)
        assertEquals("", picker.ministryDuration)
    }

    // ── The rows each tab builds ─────────────────────────────────────────

    @Test
    fun `a song row carries its number, book and the desktop's id for it`() {
        val row = state().songRow(song, newId, seconds = 270).row

        val built = assertIs<PlanRow.Song>(row)
        assertEquals("42 - Amazing Grace", built.title)
        assertEquals("Hymnal", built.songbook)
        assertEquals("42", built.number)
        assertEquals(song.desktopSongId, built.songId)
    }

    @Test
    fun `a song row carries the length the desktop measured`() {
        assertEquals(270, state().songRow(song, newId, seconds = 270).seconds)
        assertNull(state().songRow(song, newId, seconds = null).seconds, "never measured is no length")
    }

    @Test
    fun `a preset row keeps the desktop's id and kind, and nothing of its contents`() {
        val picked = state().presetRow(preset, newId)

        val row = assertIs<PlanRow.Preset>(picked.row)
        assertEquals("Welcome loop", row.title)
        assertEquals("p1", row.presetId)
        assertEquals(RowKind.MEDIA, row.kind)
        assertNull(picked.seconds)
    }

    @Test
    fun `a bible row is titled as the reference reads and keeps the book number`() {
        val reference = ParsedReference(john, 3, 16, 17)

        val row = assertIs<PlanRow.Bible>(state().bibleRow(reference, newId).row)

        assertEquals("John 3:16-17", row.title)
        assertEquals(43, row.bookId)
    }

    // ── What the Add button would add right now ──────────────────────────

    @Test
    fun `nothing is pending on a songs tab with nothing chosen`() {
        assertNull(state().pendingRow(PickerTab.SONGS, query = "", reference = null, newId = newId))
    }

    @Test
    fun `a chosen song is what the songs tab would add`() {
        val picker = state()
        picker.song = song

        val pending = picker.pendingRow(PickerTab.SONGS, query = "", reference = null, newId = newId)

        assertIs<PlanRow.Song>(pending?.row)
    }

    @Test
    fun `a typed reference is what the songs tab adds when no song is chosen`() {
        // The search field doubles as a reference field, so `John 3:16` typed there is a passage.
        val reference = ParsedReference(john, 3, 16, null)

        val pending = state().pendingRow(PickerTab.SONGS, query = "John 3:16", reference = reference, newId = newId)

        assertIs<PlanRow.Bible>(pending?.row)
    }

    @Test
    fun `a chosen song wins over a reference that also parses`() {
        val picker = state()
        picker.song = song
        val reference = ParsedReference(john, 3, 16, null)

        val pending = picker.pendingRow(PickerTab.SONGS, query = "John 3", reference = reference, newId = newId)

        assertIs<PlanRow.Song>(pending?.row)
    }

    @Test
    fun `the songs tab fills in the measured length when it knows one`() {
        val picker = state()
        picker.song = song
        val durations = SongDurations.of(listOf(song to 315))

        val pending = picker.pendingRow(PickerTab.SONGS, "", null, newId, durations)

        assertEquals(315, pending?.seconds)
    }

    @Test
    fun `the bible tab needs a book and a chapter before it can add anything`() {
        val picker = state()
        assertNull(picker.pendingRow(PickerTab.BIBLE, "", null, newId))

        picker.pickBook(john)
        assertNull(picker.pendingRow(PickerTab.BIBLE, "", null, newId), "a book alone is not a passage")

        picker.pickChapter(3)
        assertIs<PlanRow.Bible>(picker.pendingRow(PickerTab.BIBLE, "", null, newId)?.row)
    }

    @Test
    fun `a whole chapter is added when no verse is chosen`() {
        val picker = state()
        picker.pickBook(john)
        picker.pickChapter(3)

        val row = assertIs<PlanRow.Bible>(picker.pendingRow(PickerTab.BIBLE, "", null, newId)?.row)

        assertEquals("John 3", row.title)
    }

    @Test
    fun `a chosen verse carries its first words as a preview`() {
        val picker = state()
        picker.pickBook(john)
        picker.pickChapter(3)
        picker.pickVerse(16)
        picker.preview = ChapterPreview(verseCount = 36, firstWords = mapOf(16 to "For God so loved the world"))

        val row = assertIs<PlanRow.Bible>(picker.pendingRow(PickerTab.BIBLE, "", null, newId)?.row)

        assertEquals("John 3:16", row.title)
        assertTrue(row.preview.startsWith("For God so loved"))
    }

    @Test
    fun `the section tab adds whatever is typed, in the chosen color`() {
        val picker = state()
        picker.sectionColor = SectionPalette.AMBER

        val row = assertIs<PlanRow.Section>(picker.pendingRow(PickerTab.SECTION, " Worship ", null, newId)?.row)

        assertEquals("Worship", row.title, "trimmed")
        assertEquals(SectionPalette.AMBER, row.color)
    }

    @Test
    fun `an empty section name adds nothing`() {
        assertNull(state().pendingRow(PickerTab.SECTION, "   ", null, newId))
    }

    @Test
    fun `a ministry item takes who it is and how long it runs`() {
        val picker = state()
        picker.ministryWhat = " Welcome & notices "
        picker.ministryWho = " Anna "
        picker.ministryDuration = "5:00"

        val pending = picker.pendingRow(PickerTab.MINISTRY, "", null, newId)

        val row = assertIs<PlanRow.Ministry>(pending?.row)
        assertEquals("Welcome & notices", row.title)
        assertEquals("Anna", row.detail)
        assertEquals(300, pending?.seconds)
    }

    @Test
    fun `a ministry item with nothing to do adds nothing`() {
        val picker = state()
        picker.ministryWho = "Anna"

        assertNull(picker.pendingRow(PickerTab.MINISTRY, "", null, newId), "a name is not an item")
    }

    @Test
    fun `the presets tab needs one chosen`() {
        val picker = state()
        assertNull(picker.pendingRow(PickerTab.PRESETS, "", null, newId))

        picker.preset = preset
        assertIs<PlanRow.Preset>(picker.pendingRow(PickerTab.PRESETS, "", null, newId)?.row)
    }

    // ── The song list the picker shows ───────────────────────────────────

    private val library = listOf(
        Song(number = "42", title = "Amazing Grace", bookName = "Hymnal", author = "John Newton"),
        Song(number = "108", title = "How Great Thou Art", bookName = "Hymnal"),
        Song(number = "7", title = "Благодать", bookName = "Гимны", secondaryTitle = "Amazing Grace"),
    )

    @Test
    fun `an empty query lists everything`() {
        assertEquals(library, filterSongs(library, "   "))
    }

    @Test
    fun `a title matches anywhere inside it, ignoring case`() {
        assertEquals(listOf(library[1]), filterSongs(library, "great thou"))
    }

    @Test
    fun `a number matches from the start, so 4 does not find 108`() {
        assertEquals(listOf(library[0]), filterSongs(library, "4"))
    }

    @Test
    fun `the author is searched too`() {
        assertEquals(listOf(library[0]), filterSongs(library, "newton"))
    }

    @Test
    fun `a second title is searched, so a bilingual library finds either name`() {
        val found = filterSongs(library, "amazing")

        assertEquals(listOf(library[0], library[2]), found)
    }

    @Test
    fun `nothing matching is an empty list, not everything`() {
        assertTrue(filterSongs(library, "zzz").isEmpty())
    }

    @Test
    fun `a song reads as number then title`() {
        assertEquals("42 - Amazing Grace", songLabel(library[0]))
    }

    @Test
    fun `a song with both names shows both`() {
        assertEquals("7 - Благодать · Amazing Grace", songLabel(library[2]))
    }

    @Test
    fun `a song with no number is its title alone`() {
        assertEquals("Amazing Grace", songLabel(Song(number = "", title = "Amazing Grace")))
        assertEquals("Amazing Grace", songLabel(Song(number = "0", title = "Amazing Grace")))
    }
}
