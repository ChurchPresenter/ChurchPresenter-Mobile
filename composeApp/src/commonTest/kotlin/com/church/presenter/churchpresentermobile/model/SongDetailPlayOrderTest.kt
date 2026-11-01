package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The verse cards in the order the desktop plays them. The index a card sends is a position in the
 * desktop's played list, so a list that repeats the chorus there and not here sends the wrong slide.
 */
class SongDetailPlayOrderTest {

    private fun section(key: String, label: String, text: String) = SongVerse(key = key, label = label, text = text)

    private val sections = listOf(
        section("[Verse 1]", "Verse 1", "first verse"),
        section("{Chorus}", "Chorus", "the chorus"),
        section("[Verse 2]", "Verse 2", "second verse"),
    )

    private fun played(detail: SongDetail) = detail.slideVerses.map { it.displayText }

    @Test
    fun `with no play order the sections are listed once, as written`() {
        assertEquals(listOf("first verse", "the chorus", "second verse"), played(SongDetail(sections = sections)))
    }

    @Test
    fun `a play order lists a section as often as the desktop plays it`() {
        val detail = SongDetail(
            sections = sections,
            playOrder = listOf("[Verse 1]", "{Chorus}", "[Verse 2]", "{Chorus}"),
        )

        assertEquals(listOf("first verse", "the chorus", "second verse", "the chorus"), played(detail))
        assertTrue(detail.slideVerses.none { it.skipped }, "no order of its own leaves nothing out")
    }

    @Test
    fun `a section the order leaves out is listed after the rest, marked skipped`() {
        val detail = SongDetail(
            sections = sections,
            order = listOf("{Chorus}", "[Verse 1]"),
            playOrder = listOf("{Chorus}", "[Verse 1]", "[Verse 2]"),
        )

        assertEquals(listOf("the chorus", "first verse", "second verse"), played(detail))
        assertEquals(listOf(false, false, true), detail.slideVerses.map { it.skipped })
    }

    @Test
    fun `a split section brings all its slides each time it is played`() {
        val detail = SongDetail(
            sections = listOf(section("{Chorus}", "Chorus", "first half\n[---]\nsecond half"), sections[0]),
            playOrder = listOf("{Chorus}", "[Verse 1]", "{Chorus}"),
        )

        assertEquals(listOf("first half", "second half", "first verse", "first half", "second half"), played(detail))
        assertEquals(listOf(0, 1, 0, 0, 1), detail.slideVerses.map { it.part })
    }

    @Test
    fun `a play order naming a section that was not sent falls back to the written list`() {
        val detail = SongDetail(sections = sections, playOrder = listOf("[Verse 1]", "[Verse 9]"))

        assertEquals(listOf("first verse", "the chorus", "second verse"), played(detail))
    }

    @Test
    fun `a section's cue is found by its key, and a blank one is none`() {
        val detail = SongDetail(sections = sections, cues = mapOf("[Verse 1]" to "Piano only", "{Chorus}" to " "))

        assertEquals("Piano only", detail.cueFor(sections[0]))
        assertNull(detail.cueFor(sections[1]))
        assertNull(detail.cueFor(SongVerse(label = "No key")))
    }

    @Test
    fun `the arrangement reads from the desktop's json, and an older desktop's reads without it`() {
        val json = Json { ignoreUnknownKeys = true }
        val detail = json.decodeFromString<SongDetail>(
            """{"number":"12","title":"T","sections":[{"type":"verse","label":"Verse 1","key":"[Verse 1]","lines":["a"]}],
               "order":["[Verse 1]"],"notes":"Capo 2","cues":{"[Verse 1]":"Piano"},"playOrder":["[Verse 1]","[Verse 1]"]}""",
        )
        assertEquals("[Verse 1]", detail.allVerses.single().key)
        assertEquals("Capo 2", detail.notes)
        assertEquals(2, detail.slideVerses.size)

        val older = json.decodeFromString<SongDetail>("""{"number":"1","sections":[{"type":"verse","lines":["a"]}]}""")
        assertTrue(older.playOrder.isEmpty())
        assertFalse(older.slideVerses.single().skipped)
    }
}
