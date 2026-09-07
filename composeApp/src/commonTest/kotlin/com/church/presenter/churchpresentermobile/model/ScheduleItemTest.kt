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

    // ── How a picture row is named ───────────────────────────────────────
    //
    // Pictures are the one type where the folder matters as much as the file: two
    // services can both have "slide1.jpg", so the row reads "Folder / file".

    private fun picture(
        displayText: String? = null,
        folderName: String? = null,
        title: String? = null,
        id: String? = "i1",
    ) = ScheduleItem(
        id = id,
        type = "image",
        displayTextCamel = displayText,
        title = title,
        folderNameCamel = folderName,
    )

    @Test
    fun `a picture with both a folder and a file name shows both`() {
        assertEquals("Nature / sunrise.jpg", picture(displayText = "sunrise.jpg", folderName = "Nature").displayTitle)
    }

    @Test
    fun `a picture with only a file name shows just the file`() {
        assertEquals("sunrise.jpg", picture(displayText = "sunrise.jpg").displayTitle)
    }

    @Test
    fun `a picture with only a folder shows just the folder`() {
        assertEquals("Nature", picture(folderName = "Nature").displayTitle)
    }

    @Test
    fun `a picture with neither falls back to its title`() {
        assertEquals("Slide 3", picture(title = "Slide 3").displayTitle)
    }

    @Test
    fun `a picture with nothing at all falls back to its id`() {
        assertEquals("i1", picture().displayTitle)
    }

    @Test
    fun `a picture with no id at all is still named`() {
        // A blank row would be untappable and unexplainable.
        assertEquals("Untitled", picture(id = null).displayTitle)
    }

    @Test
    fun `blank picture fields are treated as absent`() {
        assertEquals("Nature", picture(displayText = "   ", folderName = "Nature").displayTitle)
    }

    @Test
    fun `the picture spelling is accepted alongside image`() {
        val item = ScheduleItem(id = "i1", type = "PICTURE", displayTextCamel = "a.jpg", folderNameCamel = "Nature")

        assertEquals("Nature / a.jpg", item.displayTitle)
    }

    // ── How a bible row is named ─────────────────────────────────────────

    @Test
    fun `a bible row with a single verse reads as chapter and verse`() {
        val item = ScheduleItem(id = "b1", type = "bible", bookNameCamel = "John", chapter = 3, verseNumberCamel = 16)

        assertEquals("John 3:16", item.displayTitle)
    }

    @Test
    fun `a bible row with a range reads as a range`() {
        val item = ScheduleItem(
            id = "b1",
            type = "bible",
            bookNameCamel = "John",
            chapter = 3,
            verseNumberCamel = 16,
            verseRangeCamel = "16-18",
        )

        assertEquals("John 3:16-18", item.displayTitle)
    }

    @Test
    fun `a blank range falls back to the single verse`() {
        val item = ScheduleItem(
            id = "b1",
            type = "bible",
            bookNameCamel = "John",
            chapter = 3,
            verseNumberCamel = 16,
            verseRangeCamel = "  ",
        )

        assertEquals("John 3:16", item.displayTitle)
    }

    @Test
    fun `a bible row missing any part of its reference falls back to the id`() {
        // Partial references would read as "John null:16".
        assertEquals("b1", ScheduleItem(id = "b1", type = "bible", bookNameCamel = "John", chapter = 3).displayTitle)
        assertEquals("b1", ScheduleItem(id = "b1", type = "bible", chapter = 3, verseNumberCamel = 16).displayTitle)
        assertEquals("b1", ScheduleItem(id = "b1",
            type = "bible",
            bookNameCamel = "John",
            verseNumberCamel = 16)
            .displayTitle)
    }

    // ── Precedence for everything else ───────────────────────────────────

    @Test
    fun `an explicit display text wins over a title`() {
        val item = ScheduleItem(id = "s1", type = "song", displayTextCamel = "42 - Amazing Grace", title = "Grace")

        assertEquals("42 - Amazing Grace", item.displayTitle)
    }

    @Test
    fun `a blank display text falls through to the title`() {
        val item = ScheduleItem(id = "s1", type = "song", displayTextCamel = "   ", title = "Grace")

        assertEquals("Grace", item.displayTitle)
    }

    @Test
    fun `an item with nothing to say falls back to its id`() {
        assertEquals("s1", ScheduleItem(id = "s1", type = "song").displayTitle)
    }

    @Test
    fun `an item with no id and nothing to say is still named`() {
        assertEquals("Untitled", ScheduleItem(type = "song").displayTitle)
    }

    // ── Active flag and icons ────────────────────────────────────────────

    @Test
    fun `either spelling of the active flag is honoured`() {
        assertTrue(ScheduleItem(id = "a", isActive = true).active)
        assertTrue(ScheduleItem(id = "a", isActiveCamel = true).active)
    }

    @Test
    fun `an item with no active flag is not active`() {
        assertFalse(ScheduleItem(id = "a").active)
    }

    @Test
    fun `each item type has its own icon`() {
        val icons = listOf("song", "bible", "presentation", "image", "video", "announcement")
            .map { ScheduleItem(id = "x", type = it).typeIcon }

        assertEquals(icons.size, icons.toSet().size, "two types share an icon: $icons")
    }

    @Test
    fun `picture and image share the picture icon`() {
        assertEquals(
            ScheduleItem(id = "x", type = "image").typeIcon,
            ScheduleItem(id = "x", type = "picture").typeIcon,
        )
    }

    @Test
    fun `an unknown or missing type gets the generic icon`() {
        assertEquals(
            ScheduleItem(id = "x", type = "hologram").typeIcon,
            ScheduleItem(id = "x").typeIcon,
        )
    }

    @Test
    fun `the type is read case-insensitively`() {
        assertEquals(
            ScheduleItem(id = "x", type = "song").typeIcon,
            ScheduleItem(id = "x", type = "SONG").typeIcon,
        )
    }
}
