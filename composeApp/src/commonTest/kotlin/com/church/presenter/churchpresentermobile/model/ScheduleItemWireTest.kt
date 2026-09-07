package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Every spelling the desktop is known to send, decoded.
 *
 * [ScheduleItem] carries the same fact under up to four names because the
 * desktop's schedule serialiser has not been consistent across versions — a
 * Bible row's book has arrived as `bookName`, `book_name`, `book-name` and
 * `book`. The accessors then pick the first one present.
 *
 * A wrong `@SerialName` here does not fail: the field is simply absent, the
 * accessor falls through to the next spelling, and the schedule row renders as
 * "Untitled" or as a bare id against a server that is sending the data
 * perfectly well. Nothing else in the app would show it, so it is pinned here
 * one alias at a time.
 */
class ScheduleItemWireTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun decode(body: String) = json.decodeFromString(ScheduleItem.serializer(), body)

    // ── The book name, four ways ─────────────────────────────────────────

    @Test
    fun `every spelling of the book name decodes`() {
        assertEquals("John", decode("""{"bookName":"John"}""").bookNameCamel)
        assertEquals("John", decode("""{"book_name":"John"}""").bookNameSnake)
        assertEquals("John", decode("""{"book-name":"John"}""").bookNameKebab)
        assertEquals("John", decode("""{"book":"John"}""").bookNameShort)
    }

    @Test
    fun `whichever spelling arrives, the book name is found`() {
        for (key in listOf("bookName", "book_name", "book-name", "book")) {
            assertEquals("John", decode("""{"$key":"John"}""").bookName, key)
        }
    }

    @Test
    fun `the camel spelling of the book name wins over the others`() {
        // Only matters when a server sends more than one, which a mid-upgrade
        // desktop has done; the order must be stable rather than incidental.
        val item = decode("""{"bookName":"John","book_name":"Mark","book-name":"Luke","book":"Acts"}""")

        assertEquals("John", item.bookName)
    }

    // ── Verse number and range ───────────────────────────────────────────

    @Test
    fun `every spelling of the verse number decodes`() {
        assertEquals(16, decode("""{"verseNumber":16}""").verseNumberCamel)
        assertEquals(16, decode("""{"verse_number":16}""").verseNumberSnake)
        assertEquals(16, decode("""{"verse":16}""").verseNumberShort)
    }

    @Test
    fun `whichever spelling arrives, the verse number is found`() {
        for (key in listOf("verseNumber", "verse_number", "verse")) {
            assertEquals(16, decode("""{"$key":16}""").verseNumber, key)
        }
    }

    @Test
    fun `every spelling of the verse range decodes`() {
        assertEquals("16-18", decode("""{"verseRange":"16-18"}""").verseRangeCamel)
        assertEquals("16-18", decode("""{"verse_range":"16-18"}""").verseRangeSnake)
        assertEquals("16-18", decode("""{"verse-range":"16-18"}""").verseRangeKebab)
        assertEquals("16-18", decode("""{"verses":"16-18"}""").verseRangeVerses)
    }

    @Test
    fun `whichever spelling arrives, the verse range is found`() {
        for (key in listOf("verseRange", "verse_range", "verse-range", "verses")) {
            assertEquals("16-18", decode("""{"$key":"16-18"}""").verseRange, key)
        }
    }

    // ── Display text ─────────────────────────────────────────────────────

    @Test
    fun `every spelling of the display text decodes`() {
        assertEquals("42 - Amazing Grace", decode("""{"displayText":"42 - Amazing Grace"}""").displayTextCamel)
        assertEquals("42 - Amazing Grace", decode("""{"display_text":"42 - Amazing Grace"}""").displayTextSnake)
        assertEquals("42 - Amazing Grace", decode("""{"display-text":"42 - Amazing Grace"}""").displayTextKebab)
    }

    @Test
    fun `whichever spelling arrives, the display text is found`() {
        for (key in listOf("displayText", "display_text", "display-text")) {
            assertEquals("Hymn", decode("""{"$key":"Hymn"}""").displayText, key)
        }
    }

    // ── Pictures ─────────────────────────────────────────────────────────

    @Test
    fun `every spelling of the folder id decodes`() {
        assertEquals("f1", decode("""{"folderId":"f1"}""").folderIdCamel)
        assertEquals("f1", decode("""{"folder_id":"f1"}""").folderIdSnake)
        assertEquals("f1", decode("""{"folder-id":"f1"}""").folderIdKebab)
    }

    @Test
    fun `every spelling of the folder name decodes`() {
        assertEquals("Advent", decode("""{"folderName":"Advent"}""").folderNameCamel)
        assertEquals("Advent", decode("""{"folder_name":"Advent"}""").folderNameSnake)
        assertEquals("Advent", decode("""{"folder-name":"Advent"}""").folderNameKebab)
    }

    @Test
    fun `every spelling of the image index decodes`() {
        assertEquals(3, decode("""{"imageIndex":3}""").imageIndexCamel)
        assertEquals(3, decode("""{"image_index":3}""").imageIndexSnake)
        assertEquals(3, decode("""{"image-index":3}""").imageIndexKebab)
        assertEquals(3, decode("""{"imageNumber":3}""").imageNumber)
    }

    @Test
    fun `whichever spelling arrives, the picture fields are found`() {
        for (key in listOf("folderId", "folder_id", "folder-id")) {
            assertEquals("f1", decode("""{"$key":"f1"}""").folderId, key)
        }
        for (key in listOf("folderName", "folder_name", "folder-name")) {
            assertEquals("Advent", decode("""{"$key":"Advent"}""").folderName, key)
        }
        for (key in listOf("imageIndex", "image_index", "image-index", "imageNumber")) {
            assertEquals(3, decode("""{"$key":3}""").imageIndex, key)
        }
    }

    // ── The active flag ──────────────────────────────────────────────────

    @Test
    fun `both spellings of the active flag decode`() {
        assertEquals(true, decode("""{"is_active":true}""").isActive)
        assertEquals(true, decode("""{"isActive":true}""").isActiveCamel)
    }

    @Test
    fun `an item that says nothing about being active leaves both unset`() {
        // The row is drawn as inactive rather than guessing; null and false have
        // to stay distinguishable at this level for that decision to be made once.
        val item = decode("""{"id":"1"}""")

        assertNull(item.isActive)
        assertNull(item.isActiveCamel)
    }

    // ── The rest of the payload ──────────────────────────────────────────

    @Test
    fun `the plain fields decode`() {
        val item = decode(
            """{"id":"i1","type":"song","title":"Amazing Grace","details":"Hymns",
               "index":4,"notes":"key of G","chapter":3}"""
        )

        assertEquals("i1", item.id)
        assertEquals("song", item.type)
        assertEquals("Amazing Grace", item.title)
        assertEquals("Hymns", item.details)
        assertEquals(4, item.index)
        assertEquals("key of G", item.notes)
        assertEquals(3, item.chapter)
    }

    @Test
    fun `a media row carries its url and its kind`() {
        // mediaType is what tells the desktop to stream rather than open a file.
        val item = decode("""{"type":"media","mediaUrl":"https://example.org/clip.mp4","mediaType":"url"}""")

        assertEquals("https://example.org/clip.mp4", item.mediaUrl)
        assertEquals("url", item.mediaType)
    }

    @Test
    fun `a website row carries its url`() {
        assertEquals("https://example.org", decode("""{"type":"web","url":"https://example.org"}""").url)
    }

    @Test
    fun `an announcement carries its text and its colours`() {
        val item = decode(
            """{"type":"announcement","text":"Welcome","textColor":"#FFFFFF",
               "backgroundColor":"#000000","fontSize":48}"""
        )

        assertEquals("Welcome", item.text)
        assertEquals("#FFFFFF", item.textColor)
        assertEquals("#000000", item.backgroundColor)
        assertEquals(48, item.fontSize)
    }

    @Test
    fun `an announcement carries how it animates in`() {
        val item = decode("""{"animationType":"fade","animationDuration":400}""")

        assertEquals("fade", item.animationType)
        assertEquals(400, item.animationDuration)
    }

    @Test
    fun `a countdown carries everything needed to rebuild its composer`() {
        // Editing a timer re-opens the composer from these fields alone, so a
        // missing one silently resets part of what the operator had set.
        val item = decode(
            """{"isTimer":true,"timerMode":"countdown","timerHours":1,"timerMinutes":30,
               "timerSeconds":15,"timerExpiredText":"Starting now"}"""
        )

        assertEquals(true, item.isTimer)
        assertEquals("countdown", item.timerMode)
        assertEquals(1, item.timerHours)
        assertEquals(30, item.timerMinutes)
        assertEquals(15, item.timerSeconds)
        assertEquals("Starting now", item.timerExpiredText)
    }

    @Test
    fun `a count-to-a-time carries its target and a live clock its format`() {
        val item = decode("""{"targetHour":10,"targetMinute":45,"liveClockFormat":"HH:mm"}""")

        assertEquals(10, item.targetHour)
        assertEquals(45, item.targetMinute)
        assertEquals("HH:mm", item.liveClockFormat)
    }

    // ── Robustness ───────────────────────────────────────────────────────

    @Test
    fun `an empty object decodes to an item with nothing set`() {
        // The schedule endpoint has returned bare objects; the row must still
        // render rather than failing the whole list.
        val item = decode("{}")

        assertNull(item.id)
        assertEquals("Untitled", item.displayTitle)
    }

    @Test
    fun `a field the app does not know about is ignored`() {
        // A newer desktop adding a field must not break an older phone.
        val item = decode("""{"id":"1","somethingNew":{"nested":true}}""")

        assertEquals("1", item.id)
    }

    @Test
    fun `a full bible row decodes into a readable reference`() {
        val item = decode(
            """{"id":"b1","type":"bible","book_name":"John","chapter":3,"verse":16,"verses":"16-18"}"""
        )

        assertTrue("John" in item.displayTitle, item.displayTitle)
        assertTrue("3" in item.displayTitle, item.displayTitle)
    }
}
