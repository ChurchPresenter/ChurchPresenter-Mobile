package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests the library→Songs-tab mapping, including the labels the UI shows. */
class LocalSongAdapterTest {

    private fun song(vararg sections: LocalSongSection) = LocalSong(
        id = "uuid-1",
        number = "42",
        title = "Amazing Grace",
        author = "John Newton",
        bookName = "Hymns",
        sections = sections.toList(),
    )

    @Test
    fun `a library song becomes a catalogue row carrying its local id`() {
        val row = LocalSongAdapter.toSong(song(LocalSongSection(SectionType.VERSE, "Amazing grace")))

        assertEquals("42", row.number)
        assertEquals("Amazing Grace", row.title)
        assertEquals("John Newton", row.author)
        assertEquals("Hymns", row.bookName)
        assertEquals("uuid-1", row.localId)
        // The desktop's numeric id means nothing for a library song.
        assertEquals(-1, row.id)
    }

    @Test
    fun `verses are numbered by their own running count`() {
        // A chorus between verses one and two must not make the next one "Verse 3".
        val labels = LocalSongAdapter.sectionLabels(
            listOf(
                LocalSongSection(SectionType.VERSE, "one"),
                LocalSongSection(SectionType.CHORUS, "sung twice"),
                LocalSongSection(SectionType.VERSE, "two"),
                LocalSongSection(SectionType.BRIDGE, "bridge"),
                LocalSongSection(SectionType.TAG, "tag"),
                LocalSongSection(SectionType.ENDING, "ending"),
            )
        )

        assertEquals(listOf("Verse 1", "Chorus", "Verse 2", "Bridge", "Tag", "Ending"), labels)
    }

    @Test
    fun `a label the user typed wins over the generated one`() {
        val labels = LocalSongAdapter.sectionLabels(
            listOf(
                LocalSongSection(SectionType.VERSE, "one", label = "Verse 1a"),
                LocalSongSection(SectionType.VERSE, "two"),
            )
        )

        assertEquals(listOf("Verse 1a", "Verse 2"), labels)
    }

    @Test
    fun `blank sections are not slides`() {
        val detail = LocalSongAdapter.toDetail(
            song(
                LocalSongSection(SectionType.VERSE, "real words"),
                LocalSongSection(SectionType.VERSE, "   "),
                LocalSongSection(SectionType.VERSE, ""),
            )
        )

        assertEquals(1, detail.allVerses.size)
        assertEquals("real words", detail.allVerses.single().text)
    }

    @Test
    fun `the detail carries the book name the Songs tab reads`() {
        val detail = LocalSongAdapter.toDetail(song(LocalSongSection(SectionType.VERSE, "words")))

        assertEquals("Hymns", detail.bookName)
        assertTrue(detail.hasLyrics)
    }

    @Test
    fun `a song with no sections yields no lyrics rather than failing`() {
        val detail = LocalSongAdapter.toDetail(song())

        assertTrue(detail.allVerses.isEmpty())
        assertNull(detail.plainText)
    }

    // ── Section headings ─────────────────────────────────────────────────
    //
    // Shared with SlideDeckBuilder so the heading on the projected slide and the
    // one in the detail sheet cannot drift apart.

    private fun part(type: SectionType, text: String = "words", label: String? = null) =
        LocalSongSection(type = type, text = text, label = label)

    private fun labels(vararg sections: LocalSongSection): List<String> =
        LocalSongAdapter.sectionLabels(LocalSongAdapter.usableSections(sections.toList()))


    @Test
    fun `each section type has its own default heading`() {
        val result = labels(
            part(SectionType.VERSE),
            part(SectionType.CHORUS),
            part(SectionType.BRIDGE),
            part(SectionType.TAG),
            part(SectionType.ENDING),
        )

        assertEquals(listOf("Verse 1", "Chorus", "Bridge", "Tag", "Ending"), result)
    }

    @Test
    fun `a name the user typed wins over the generated one`() {
        // Someone who wrote "Verse 1a" meant it.
        val result = labels(part(SectionType.VERSE, label = "Verse 1a"))

        assertEquals(listOf("Verse 1a"), result)
    }

    @Test
    fun `a blank label falls back to the generated heading`() {
        val result = labels(part(SectionType.VERSE, label = "   "))

        assertEquals(listOf("Verse 1"), result)
    }

    @Test
    fun `a custom label does not disturb the verse numbering`() {
        val result = labels(
            part(SectionType.VERSE),
            part(SectionType.VERSE, label = "Verse 2a"),
            part(SectionType.VERSE),
        )

        assertEquals(listOf("Verse 1", "Verse 2a", "Verse 3"), result)
    }


    @Test
    fun `a song with no usable sections has no headings`() {
        assertTrue(labels(part(SectionType.VERSE, text = "  ")).isEmpty())
    }

    @Test
    fun `a song of only choruses numbers nothing`() {
        val result = labels(part(SectionType.CHORUS), part(SectionType.CHORUS))

        assertEquals(listOf("Chorus", "Chorus"), result)
    }
}
