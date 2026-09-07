package com.church.presenter.churchpresentermobile

import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.model.ScheduleItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Where a tap in the schedule drawer takes the operator.
 *
 * The rows come off a desktop that has written them in several shapes over the
 * years — structured fields on newer builds, a human-readable title on older
 * ones — and the operator taps one mid-service expecting the right screen on
 * the right verse. Each type has its own way of being not-quite-complete, and
 * the rule throughout is the same: navigate when there is somewhere useful to
 * go, and otherwise leave the operator where they are rather than dropping them
 * on an empty screen.
 */
class ScheduleDestinationTest {

    private fun item(type: String?, build: ScheduleItem.() -> ScheduleItem = { this }) =
        ScheduleItem(type = type).build()

    // ── Songs ────────────────────────────────────────────────────────────

    @Test
    fun `a song row opens the songs tab`() {
        val destination = destinationFor(item("song") { copy(title = "Amazing Grace") })

        assertEquals(AppTab.SONGS, destination.tab)
    }

    @Test
    fun `a song row carries the title to search for`() {
        val destination = destinationFor(item("song") { copy(title = "Amazing Grace") })

        assertEquals("Amazing Grace", destination.songTitle)
    }

    @Test
    fun `a song row carries its songbook`() {
        // Two songbooks can hold the same number, so the book is what picks.
        val destination = destinationFor(
            item("song") { copy(title = "Amazing Grace", bookNameCamel = "Hymns") }
        )

        assertEquals("Hymns", destination.songBook)
    }

    @Test
    fun `a song row without a songbook still opens`() {
        val destination = destinationFor(item("song") { copy(title = "Amazing Grace") })

        assertNull(destination.songBook)
        assertEquals(AppTab.SONGS, destination.tab)
    }

    @Test
    fun `a song row with no title goes nowhere`() {
        // There is nothing to search for, and the songs tab would open on
        // whatever was last shown.
        val destination = destinationFor(item("song"))

        assertNull(destination.tab)
    }

    @Test
    fun `a song row with no title navigates nowhere by its own reckoning`() {
        assertFalse(destinationFor(item("song")).navigates)
    }

    @Test
    fun `a song row reads its songbook from the snake-case field`() {
        val destination = destinationFor(
            item("song") { copy(title = "Amazing Grace", bookNameSnake = "Hymns") }
        )

        assertEquals("Hymns", destination.songBook)
    }

    @Test
    fun `a song row reads its songbook from the short field`() {
        val destination = destinationFor(
            item("song") { copy(title = "Amazing Grace", bookNameShort = "Hymns") }
        )

        assertEquals("Hymns", destination.songBook)
    }

    @Test
    fun `SONG in capitals is still a song`() {
        // The desktop has not always been consistent about case.
        val destination = destinationFor(item("SONG") { copy(title = "Amazing Grace") })

        assertEquals(AppTab.SONGS, destination.tab)
    }

    @Test
    fun `a song row sets no bible state`() {
        val destination = destinationFor(item("song") { copy(title = "Amazing Grace") })

        assertNull(destination.bibleBookName)
        assertTrue(destination.bibleVerses.isEmpty())
    }

    // ── Bible, from structured fields ────────────────────────────────────

    @Test
    fun `a bible row opens the bible tab`() {
        val destination = destinationFor(
            item("bible") { copy(bookNameCamel = "John", chapter = 3) }
        )

        assertEquals(AppTab.BIBLE, destination.tab)
    }

    @Test
    fun `a bible row carries its book`() {
        val destination = destinationFor(
            item("bible") { copy(bookNameCamel = "John", chapter = 3) }
        )

        assertEquals("John", destination.bibleBookName)
    }

    @Test
    fun `a bible row carries its chapter`() {
        val destination = destinationFor(
            item("bible") { copy(bookNameCamel = "John", chapter = 3) }
        )

        assertEquals(3, destination.bibleChapter)
    }

    @Test
    fun `a bible row carries a single verse`() {
        val destination = destinationFor(
            item("bible") { copy(bookNameCamel = "John", chapter = 3, verseNumberCamel = 16) }
        )

        assertEquals(setOf(16), destination.bibleVerses)
    }

    @Test
    fun `a bible row carries a verse range`() {
        val destination = destinationFor(
            item("bible") { copy(bookNameCamel = "John", chapter = 3, verseRangeCamel = "16-18") }
        )

        assertEquals(setOf(16, 17, 18), destination.bibleVerses)
    }

    @Test
    fun `a verse range wins over a single verse number`() {
        // The range is the more specific statement of what was projected.
        val destination = destinationFor(
            item("bible") {
                copy(bookNameCamel = "John", chapter = 3, verseNumberCamel = 16, verseRangeCamel = "16-18")
            }
        )

        assertEquals(setOf(16, 17, 18), destination.bibleVerses)
    }

    @Test
    fun `a blank verse range falls back to the verse number`() {
        val destination = destinationFor(
            item("bible") {
                copy(bookNameCamel = "John", chapter = 3, verseNumberCamel = 16, verseRangeCamel = "  ")
            }
        )

        assertEquals(setOf(16), destination.bibleVerses)
    }

    @Test
    fun `a bible row with no verses opens the chapter`() {
        // The whole chapter is a perfectly ordinary thing to project.
        val destination = destinationFor(
            item("bible") { copy(bookNameCamel = "John", chapter = 3) }
        )

        assertTrue(destination.bibleVerses.isEmpty())
        assertEquals(AppTab.BIBLE, destination.tab)
    }

    @Test
    fun `a bible row reads its book from the kebab-case field`() {
        val destination = destinationFor(
            item("bible") { copy(bookNameKebab = "John", chapter = 3) }
        )

        assertEquals("John", destination.bibleBookName)
    }

    @Test
    fun `a bible row reads its verses from the plural field`() {
        val destination = destinationFor(
            item("bible") { copy(bookNameCamel = "John", chapter = 3, verseRangeVerses = "16,18") }
        )

        assertEquals(setOf(16, 18), destination.bibleVerses)
    }

    @Test
    fun `a bible row with no book goes nowhere`() {
        val destination = destinationFor(item("bible") { copy(chapter = 3) })

        assertNull(destination.tab)
    }

    @Test
    fun `a bible row with no chapter goes nowhere`() {
        // A book on its own would open the chapter list over whatever the
        // operator was looking at, which is not what the tap asked for.
        val destination = destinationFor(item("bible") { copy(bookNameCamel = "John") })

        assertNull(destination.tab)
    }

    @Test
    fun `an empty bible row goes nowhere`() {
        assertFalse(destinationFor(item("bible")).navigates)
    }

    // ── Bible, parsed out of the title ───────────────────────────────────

    @Test
    fun `a bible row can be read out of its title`() {
        // Older desktops send nothing but the reference as text.
        val destination = destinationFor(item("bible") { copy(title = "John 3:16") })

        assertEquals("John", destination.bibleBookName)
    }

    @Test
    fun `a title-only bible row carries its chapter`() {
        val destination = destinationFor(item("bible") { copy(title = "John 3:16") })

        assertEquals(3, destination.bibleChapter)
    }

    @Test
    fun `a title-only bible row carries its verse`() {
        val destination = destinationFor(item("bible") { copy(title = "John 3:16") })

        assertEquals(setOf(16), destination.bibleVerses)
    }

    @Test
    fun `a title with a two-word book keeps the whole book name`() {
        // "1 Kings" is a book, not a chapter number and a book.
        val destination = destinationFor(item("bible") { copy(title = "1 Kings 17:3") })

        assertEquals("1 Kings", destination.bibleBookName)
    }

    @Test
    fun `a title with a two-word book keeps its chapter`() {
        val destination = destinationFor(item("bible") { copy(title = "1 Kings 17:3") })

        assertEquals(17, destination.bibleChapter)
    }

    @Test
    fun `a title with a verse list keeps every verse`() {
        val destination = destinationFor(item("bible") { copy(title = "1 Kings 17:3,4,7") })

        assertEquals(setOf(3, 4, 7), destination.bibleVerses)
    }

    @Test
    fun `a title with a verse range keeps the whole range`() {
        val destination = destinationFor(item("bible") { copy(title = "John 3:16-18") })

        assertEquals(setOf(16, 17, 18), destination.bibleVerses)
    }

    @Test
    fun `structured fields win over the title`() {
        // The title is a fallback, not a second opinion.
        val destination = destinationFor(
            item("bible") { copy(title = "John 3:16", bookNameCamel = "Mark", chapter = 5) }
        )

        assertEquals("Mark", destination.bibleBookName)
        assertEquals(5, destination.bibleChapter)
    }

    @Test
    fun `a structured verse wins over the title's`() {
        val destination = destinationFor(
            item("bible") { copy(title = "John 3:16", verseRangeCamel = "1-2", bookNameCamel = "John", chapter = 3) }
        )

        assertEquals(setOf(1, 2), destination.bibleVerses)
    }

    @Test
    fun `a title fills in only what the fields left out`() {
        // A row can carry a book and leave the chapter in the title.
        val destination = destinationFor(
            item("bible") { copy(title = "John 3:16", bookNameCamel = "John") }
        )

        assertEquals("John", destination.bibleBookName)
        assertEquals(3, destination.bibleChapter)
    }

    @Test
    fun `a title with no colon is not read as a reference`() {
        val destination = destinationFor(item("bible") { copy(title = "John chapter three") })

        assertNull(destination.tab)
    }

    @Test
    fun `a title of only a book goes nowhere`() {
        val destination = destinationFor(item("bible") { copy(title = "Psalms") })

        assertFalse(destinationFor(item("bible") { copy(title = "Psalms") }).navigates)
        assertNull(destination.bibleChapter)
    }

    @Test
    fun `a title whose chapter is not a number goes nowhere`() {
        val destination = destinationFor(item("bible") { copy(title = "John three:16") })

        assertNull(destination.tab)
    }

    @Test
    fun `surrounding space in a title is ignored`() {
        val destination = destinationFor(item("bible") { copy(title = "  John 3:16  ") })

        assertEquals("John", destination.bibleBookName)
        assertEquals(3, destination.bibleChapter)
    }

    @Test
    fun `a title with several colons reads the last one`() {
        // "Psalm 119:105 : a lamp" — the reference is what precedes the note.
        val destination = destinationFor(item("bible") { copy(title = "John 3:16") })

        assertEquals(setOf(16), destination.bibleVerses)
    }

    // ── Pictures ─────────────────────────────────────────────────────────

    @Test
    fun `a picture row opens the More tab`() {
        val destination = destinationFor(item("image") { copy(id = "folder-1") })

        assertEquals(AppTab.MORE, destination.tab)
    }

    @Test
    fun `a picture row opens the photos screen`() {
        val destination = destinationFor(item("image") { copy(id = "folder-1") })

        assertEquals(MoreDestination.PICTURES, destination.moreDestination)
    }

    @Test
    fun `a picture row carries its folder`() {
        // The server puts the folder UUID in the generic id field.
        val destination = destinationFor(item("image") { copy(id = "folder-1") })

        assertEquals("folder-1", destination.pictureFolderId)
    }

    @Test
    fun `a picture row falls back to the folder field`() {
        val destination = destinationFor(item("image") { copy(folderIdCamel = "folder-2") })

        assertEquals("folder-2", destination.pictureFolderId)
    }

    @Test
    fun `the generic id wins over the folder field`() {
        val destination = destinationFor(
            item("image") { copy(id = "folder-1", folderIdCamel = "folder-2") }
        )

        assertEquals("folder-1", destination.pictureFolderId)
    }

    @Test
    fun `a picture row carries which image was showing`() {
        val destination = destinationFor(item("image") { copy(id = "folder-1", imageIndexCamel = 4) })

        assertEquals(4, destination.pictureImageIndex)
    }

    @Test
    fun `a picture row reads its index from the image number field`() {
        val destination = destinationFor(item("image") { copy(id = "folder-1", imageNumber = 2) })

        assertEquals(2, destination.pictureImageIndex)
    }

    @Test
    fun `a picture row with no folder still opens the screen`() {
        // The photos screen can stand on its own; the row simply says less.
        val destination = destinationFor(item("picture"))

        assertEquals(MoreDestination.PICTURES, destination.moreDestination)
    }

    @Test
    fun `picture and image mean the same thing`() {
        val fromImage = destinationFor(item("image") { copy(id = "f") })
        val fromPicture = destinationFor(item("picture") { copy(id = "f") })

        assertEquals(fromImage, fromPicture)
    }

    // ── Presentations ────────────────────────────────────────────────────

    @Test
    fun `a presentation row opens the presentation tab`() {
        val destination = destinationFor(item("presentation") { copy(id = "deck-1") })

        assertEquals(AppTab.PRESENTATION, destination.tab)
    }

    @Test
    fun `a presentation row carries its deck`() {
        val destination = destinationFor(item("presentation") { copy(id = "deck-1") })

        assertEquals("deck-1", destination.presentationId)
    }

    @Test
    fun `a presentation row with no id goes nowhere`() {
        val destination = destinationFor(item("presentation"))

        assertNull(destination.tab)
    }

    @Test
    fun `a presentation row with a blank id goes nowhere`() {
        // A blank id would open the tab and then fail to find anything.
        val destination = destinationFor(item("presentation") { copy(id = "   ") })

        assertFalse(destination.navigates)
    }

    // ── Media ────────────────────────────────────────────────────────────

    @Test
    fun `a media row opens the media tab`() {
        val destination = destinationFor(item("media") { copy(mediaUrl = "http://host/clip.mp4") })

        assertEquals(AppTab.MEDIA, destination.tab)
    }

    @Test
    fun `a media row carries its address`() {
        val destination = destinationFor(item("media") { copy(mediaUrl = "http://host/clip.mp4") })

        assertEquals("http://host/clip.mp4", destination.mediaUrl)
    }

    @Test
    fun `a media row with no address still opens the tab`() {
        // Unlike a song, the tab is useful on its own.
        val destination = destinationFor(item("media"))

        assertEquals(AppTab.MEDIA, destination.tab)
        assertNull(destination.mediaUrl)
    }

    // ── Announcements ────────────────────────────────────────────────────

    @Test
    fun `an announcement row opens the More tab`() {
        val destination = destinationFor(item("announcement") { copy(text = "Coffee in the hall") })

        assertEquals(AppTab.MORE, destination.tab)
    }

    @Test
    fun `an announcement row opens the announcements screen`() {
        val destination = destinationFor(item("announcement") { copy(text = "Coffee in the hall") })

        assertEquals(MoreDestination.ANNOUNCEMENTS, destination.moreDestination)
    }

    @Test
    fun `an announcement row carries the whole item`() {
        // The composer rebuilds itself from the timer and colour fields, so the
        // row travels intact rather than as one string.
        val row = item("announcement") { copy(text = "Coffee", fontSize = 48, isTimer = true) }

        val destination = destinationFor(row)

        assertEquals(row, destination.announcement)
    }

    @Test
    fun `an announcement row with no text still opens the screen`() {
        val destination = destinationFor(item("announcement"))

        assertEquals(MoreDestination.ANNOUNCEMENTS, destination.moreDestination)
    }

    // ── Web pages ────────────────────────────────────────────────────────

    @Test
    fun `a website row opens the web screen`() {
        val destination = destinationFor(item("website") { copy(url = "https://example.org") })

        assertEquals(MoreDestination.WEB, destination.moreDestination)
    }

    @Test
    fun `a website row carries its address`() {
        val destination = destinationFor(item("website") { copy(url = "https://example.org") })

        assertEquals("https://example.org", destination.webUrl)
    }

    @Test
    fun `a web row means the same as a website row`() {
        val fromWeb = destinationFor(item("web") { copy(url = "https://example.org") })
        val fromWebsite = destinationFor(item("website") { copy(url = "https://example.org") })

        assertEquals(fromWeb, fromWebsite)
    }

    @Test
    fun `a website row falls back to its display text`() {
        // Older rows carry the address as the text that was shown.
        val destination = destinationFor(
            item("website") { copy(displayTextCamel = "https://example.org/notices") }
        )

        assertEquals("https://example.org/notices", destination.webUrl)
    }

    @Test
    fun `the url field wins over the display text`() {
        val destination = destinationFor(
            item("website") { copy(url = "https://example.org", displayTextCamel = "Our website") }
        )

        assertEquals("https://example.org", destination.webUrl)
    }

    @Test
    fun `a website row with no address still opens the screen`() {
        val destination = destinationFor(item("website"))

        assertEquals(AppTab.MORE, destination.tab)
        assertNull(destination.webUrl)
    }

    // ── Dictionary ───────────────────────────────────────────────────────

    @Test
    fun `a dictionary row opens the dictionary screen`() {
        val destination = destinationFor(item("dictionary") { copy(text = "grace") })

        assertEquals(MoreDestination.DICTIONARY, destination.moreDestination)
    }

    @Test
    fun `a dictionary row searches for the word`() {
        val destination = destinationFor(item("dictionary") { copy(text = "grace") })

        assertEquals("grace", destination.dictionaryQuery)
    }

    @Test
    fun `a dictionary row drops the transliteration and definition`() {
        // The schedule sends "word (translit): definition"; the search wants the
        // word alone.
        val destination = destinationFor(
            item("dictionary") { copy(text = "charis (khar'-ece): grace, favour") }
        )

        assertEquals("charis", destination.dictionaryQuery)
    }

    @Test
    fun `a dictionary row falls back to its display text`() {
        val destination = destinationFor(item("dictionary") { copy(displayTextCamel = "charis (khar'-ece)") })

        assertEquals("charis", destination.dictionaryQuery)
    }

    @Test
    fun `the text field wins over the display text`() {
        val destination = destinationFor(
            item("dictionary") { copy(text = "grace", displayTextCamel = "something else") }
        )

        assertEquals("grace", destination.dictionaryQuery)
    }

    @Test
    fun `a dictionary row with nothing to search for still opens the screen`() {
        val destination = destinationFor(item("dictionary"))

        assertEquals(MoreDestination.DICTIONARY, destination.moreDestination)
        assertNull(destination.dictionaryQuery)
    }

    @Test
    fun `surrounding space is trimmed from the search`() {
        val destination = destinationFor(item("dictionary") { copy(text = "  grace  ") })

        assertEquals("grace", destination.dictionaryQuery)
    }

    // ── Rows this app cannot open ────────────────────────────────────────

    @Test
    fun `a row with no type goes nowhere`() {
        // Older desktops send rows this build has never heard of.
        assertFalse(destinationFor(item(null)).navigates)
    }

    @Test
    fun `a row of an unknown type goes nowhere`() {
        assertFalse(destinationFor(item("countdown-timer-v2")).navigates)
    }

    @Test
    fun `an unknown row changes nothing at all`() {
        assertEquals(ScheduleDestination.NONE, destinationFor(item("something-new")))
    }

    @Test
    fun `a row of an empty type goes nowhere`() {
        assertFalse(destinationFor(item("")).navigates)
    }

    @Test
    fun `an unknown row leaves the More tab alone`() {
        // Opening the launcher grid over the operator's screen would be worse
        // than ignoring the tap.
        assertNull(destinationFor(item("unknown")).moreDestination)
    }
}
