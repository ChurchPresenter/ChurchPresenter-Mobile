package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests the alias fallbacks and the [ScheduleItem.displayTitle] branching machine. */
class ScheduleItemTest {

    // ── Simple alias getters ─────────────────────────────────────────────────

    @Test
    fun bookNameFallsThroughAliases() {
        assertEquals("Genesis", ScheduleItem(bookNameCamel = "Genesis").bookName)
        assertEquals("Exodus", ScheduleItem(bookNameSnake = "Exodus").bookName)
        assertEquals("Mark", ScheduleItem(bookNameKebab = "Mark").bookName)
        assertEquals("Luke", ScheduleItem(bookNameShort = "Luke").bookName)
        assertNull(ScheduleItem().bookName)
    }

    @Test
    fun verseNumberAndRangeAndFolderAndImageIndexFallThrough() {
        assertEquals(16, ScheduleItem(verseNumberCamel = 16).verseNumber)
        assertEquals(3, ScheduleItem(verseNumberShort = 3).verseNumber)
        assertEquals("16-18", ScheduleItem(verseRangeCamel = "16-18").verseRange)
        assertEquals("1,3", ScheduleItem(verseRangeVerses = "1,3").verseRange)
        assertEquals("f1", ScheduleItem(folderIdSnake = "f1").folderId)
        assertEquals("Nature", ScheduleItem(folderNameKebab = "Nature").folderName)
        assertEquals(7, ScheduleItem(imageIndexCamel = 7).imageIndex)
        assertEquals(9, ScheduleItem(imageNumber = 9).imageIndex)
    }

    @Test
    fun displayTextFallsThrough() {
        assertEquals("A", ScheduleItem(displayTextCamel = "A").displayText)
        assertEquals("B", ScheduleItem(displayTextSnake = "B").displayText)
        assertEquals("C", ScheduleItem(displayTextKebab = "C").displayText)
        assertNull(ScheduleItem().displayText)
    }

    // ── active / typeIcon ────────────────────────────────────────────────────

    @Test
    fun activePrefersSnakeThenCamelThenFalse() {
        assertTrue(ScheduleItem(isActive = true).active)
        assertTrue(ScheduleItem(isActiveCamel = true).active)
        assertFalse(ScheduleItem(isActive = false, isActiveCamel = true).active)
        assertFalse(ScheduleItem().active)
    }

    @Test
    fun typeIconMapsKnownTypesAndDefaults() {
        assertEquals("🎵", ScheduleItem(type = "song").typeIcon)
        assertEquals("📖", ScheduleItem(type = "Bible").typeIcon)
        assertEquals("📽", ScheduleItem(type = "presentation").typeIcon)
        assertEquals("🖼", ScheduleItem(type = "picture").typeIcon)
        assertEquals("🎬", ScheduleItem(type = "video").typeIcon)
        assertEquals("📢", ScheduleItem(type = "announcement").typeIcon)
        assertEquals("📄", ScheduleItem(type = "mystery").typeIcon)
        assertEquals("📄", ScheduleItem().typeIcon)
    }

    // ── displayTitle: image/picture branch ───────────────────────────────────

    @Test
    fun displayTitleImageCombinesFolderAndText() {
        val item = ScheduleItem(type = "image", displayTextCamel = "Sunset", folderNameCamel = "Nature")
        assertEquals("Nature / Sunset", item.displayTitle)
    }

    @Test
    fun displayTitleImageFallsBackThroughDtFnTitleId() {
        assertEquals("Sunset", ScheduleItem(type = "image", displayTextCamel = "Sunset").displayTitle)
        assertEquals("Nature", ScheduleItem(type = "picture", folderNameCamel = "Nature").displayTitle)
        assertEquals("T", ScheduleItem(type = "image", title = "T").displayTitle)
        assertEquals("id-1", ScheduleItem(type = "image", id = "id-1").displayTitle)
        assertEquals("Untitled", ScheduleItem(type = "image").displayTitle)
    }

    // ── displayTitle: non-image branch ───────────────────────────────────────

    @Test
    fun displayTitlePrefersDisplayTextThenTitle() {
        assertEquals("DT", ScheduleItem(type = "song", displayTextCamel = "DT", title = "T").displayTitle)
        assertEquals("T", ScheduleItem(type = "song", title = "T").displayTitle)
    }

    @Test
    fun displayTitleBuildsBibleReference() {
        val single = ScheduleItem(type = "bible", bookNameCamel = "John", chapter = 3, verseNumberCamel = 16)
        assertEquals("John 3:16", single.displayTitle)

        val ranged = ScheduleItem(
            type = "bible",
            bookNameCamel = "John",
            chapter = 3,
            verseNumberCamel = 16,
            verseRangeCamel = "16-18",
        )
        assertEquals("John 3:16-18", ranged.displayTitle)
    }

    @Test
    fun displayTitleLastResortIsIdThenUntitled() {
        assertEquals("uuid-9", ScheduleItem(type = "song", id = "uuid-9").displayTitle)
        assertEquals("Untitled", ScheduleItem().displayTitle)
    }

    // ── ScheduleResponse.allItems ────────────────────────────────────────────

    @Test
    fun allItemsPrefersItemsThenScheduleThenEmpty() {
        val a = listOf(ScheduleItem(id = "a"))
        val b = listOf(ScheduleItem(id = "b"))
        assertEquals(a, ScheduleResponse(items = a, schedule = b).allItems)
        assertEquals(b, ScheduleResponse(schedule = b).allItems)
        assertTrue(ScheduleResponse().allItems.isEmpty())
    }
}
