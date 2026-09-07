package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the kebab-case ↔ camelCase mappings between the desktop's JSON and our
 * models.
 *
 * These `@SerialName` pairings are the one thing a Kotlin compiler cannot check:
 * rename a field and the code still builds, the decode still succeeds, and the
 * value silently arrives as its default. Each case below decodes the shape the
 * desktop actually sends.
 */
class WireContractTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ── Songs ────────────────────────────────────────────────────────────

    @Test
    fun aSongBookDecodesItsKebabCaseKeys() {
        val book = json.decodeFromString<SongBook>(
            """{"book-name":"Hymns","song-total":2,"songs":[{"id":1,"number":"1","title":"Amazing Grace"}]}""",
        )

        assertEquals("Hymns", book.bookName)
        assertEquals(2, book.songTotal)
        assertEquals(1, book.songs.size)
    }

    @Test
    fun theCatalogueResponseDecodesItsWrapperKey() {
        val response = json.decodeFromString<SongsResponse>(
            """{"song-book":[{"book-name":"Hymns","song-total":0,"songs":[]}],"songBooks":1,"total":0}""",
        )

        assertEquals(1, response.songBook.size)
        assertEquals("Hymns", response.songBook.first().bookName)
        assertEquals(1, response.songBooks)
        assertEquals(0, response.total)
    }

    @Test
    fun theCountsAreOptionalBecauseOlderDesktopsOmitThem() {
        val response = json.decodeFromString<SongsResponse>("""{"song-book":[]}""")

        assertNull(response.songBooks)
        assertNull(response.total)
        assertTrue(response.songBook.isEmpty())
    }

    @Test
    fun theSelectSongPayloadNamesTheSongbookTheDesktopExpects() {
        val payload = SelectSongPayload(id = "42", songNumber = 42, title = "Amazing Grace", songbook = "Hymns")

        val encoded = json.encodeToString(SelectSongPayload.serializer(), payload)

        assertTrue(encoded.contains("\"songNumber\":42"), encoded)
        assertTrue(encoded.contains("\"songbook\":\"Hymns\""), encoded)
    }

    // ── Pictures ─────────────────────────────────────────────────────────

    @Test
    fun anUploadedPhotoDecodesWhereTheServerPutIt() {
        val response = json.decodeFromString<UploadPhotoResponse>(
            """{"ok":true,"folder-id":"device_uploads","image-index":7,"file-name":"a.jpg"}""",
        )

        assertTrue(response.ok)
        assertEquals("device_uploads", response.folderId)
        assertEquals(7, response.imageIndex)
        assertEquals("a.jpg", response.fileName)
    }

    @Test
    fun anUploadReplyWithoutAFileNameStillDecodes() {
        // The name is the server's to choose and older builds omit it; the folder
        // and index are what the app navigates by.
        val response = json.decodeFromString<UploadPhotoResponse>(
            """{"folder-id":"device_uploads","image-index":0}""",
        )

        assertNull(response.fileName)
        assertTrue(response.ok, "ok defaults to true — a reply that parsed is a success")
    }

    // ── Q&A ──────────────────────────────────────────────────────────────

    @Test
    fun theSessionStatusDecodesEveryFlagTheAdminScreenReads() {
        val status = json.decodeFromString<QAStatusResponse>(
            """{"sessionActive":true,"cooldownSeconds":15,"displayedQuestionId":"q1","votingEnabled":true}""",
        )

        assertTrue(status.sessionActive)
        assertEquals(15, status.cooldownSeconds)
        assertEquals("q1", status.displayedQuestionId)
        assertTrue(status.votingEnabled)
    }

    @Test
    fun aMinimalStatusFallsBackToSafeDefaults() {
        // Voting off and no question displayed is the safe reading of silence:
        // the screen shows nothing rather than something unvetted.
        val status = json.decodeFromString<QAStatusResponse>("""{"sessionActive":false}""")

        assertEquals(30, status.cooldownSeconds)
        assertEquals("", status.displayedQuestionId)
        assertEquals(false, status.votingEnabled)
    }

    @Test
    fun anAskedQuestionCarriesBothTextAndName() {
        val encoded = json.encodeToString(QATextRequest.serializer(), QATextRequest("Why?", "Ada"))

        assertTrue(encoded.contains("Why?"), encoded)
        assertTrue(encoded.contains("Ada"), encoded)
    }

    // ── Dictionary ───────────────────────────────────────────────────────

    @Test
    fun anAppearsInVerseDecodesItsReferenceAndText() {
        val verse = json.decodeFromString<DictionaryVerse>(
            """{"bookName":"Genesis","chapter":1,"verse":1,"reference":"Genesis 1:1","text":"In the beginning"}""",
        )

        assertEquals("Genesis", verse.bookName)
        assertEquals(1, verse.chapter)
        assertEquals(1, verse.verse)
        assertEquals("Genesis 1:1", verse.reference)
        assertEquals("In the beginning", verse.text)
    }

    @Test
    fun aVerseWithNoTextStillDecodes() {
        // The list can be rendered from the reference alone.
        val verse = json.decodeFromString<DictionaryVerse>(
            """{"bookName":"Genesis","chapter":1,"verse":1,"reference":"Genesis 1:1"}""",
        )

        assertEquals("", verse.text)
    }

    @Test
    fun theVersesResponseCarriesTheUncappedTotal() {
        // `verses` is capped by the request's limit; `total` is how many there
        // really are, which is what the sheet's "showing N of M" reads.
        val response = json.decodeFromString<DictionaryVersesResponse>(
            """{"number":"H1254","total":42,"verses":[
                 {"bookName":"Genesis","chapter":1,"verse":1,"reference":"Genesis 1:1"}]}""",
        )

        assertEquals("H1254", response.number)
        assertEquals(42, response.total)
        assertEquals(1, response.verses.size)
    }

    @Test
    fun anEntryThatAppearsNowhereDecodesAsAnEmptyList() {
        val response = json.decodeFromString<DictionaryVersesResponse>("""{"number":"H9999"}""")

        assertEquals(0, response.total)
        assertTrue(response.verses.isEmpty())
    }
}
