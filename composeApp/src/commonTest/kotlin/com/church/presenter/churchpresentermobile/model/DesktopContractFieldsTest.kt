package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Every field of the bodies the phone and the desktop trade, read back by name.
 *
 * The desktop has answered under several spellings over the years and ignores what
 * it does not recognise, so a renamed field fails in silence: the verse just comes
 * out without its translation. These pin the names on both sides of that.
 */
class DesktopContractFieldsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun <T> read(serializer: KSerializer<T>, text: String): T = json.decodeFromString(serializer, text)

    private fun <T> roundTrip(serializer: KSerializer<T>, value: T): T =
        json.decodeFromString(serializer, json.encodeToString(serializer, value))

    // ── Bible ────────────────────────────────────────────────────────────

    @Test
    fun aBibleBookIsReadFromEveryKeyTheDesktopUses() {
        val book = read(
            BibleBook.serializer(),
            """{"id":"1","book_name":"Genesis","testament":"OT","chapter-total":50,"chapter_count":49}""",
        )

        assertEquals("1", book.id)
        assertEquals("Genesis", book.bookNameAlt)
        assertEquals("OT", book.testament)
        assertEquals(50, book.chapterTotal)
        assertEquals(49, book.chapterCount)
        assertEquals(50, book.totalChapters, "chapter-total wins over chapter_count")
    }

    @Test
    fun theBookListIsReadUnderEitherName() {
        val response = read(BibleBooksResponse.serializer(), """{"books":[{"id":"1"}],"bible":[{"id":"2"}]}""")

        assertEquals("1", response.books?.single()?.id)
        assertEquals("2", response.bible?.single()?.id)
    }

    @Test
    fun aVerseIsReadFromEveryKeyTheDesktopUses() {
        val verse = read(
            BibleVerse.serializer(),
            """{"verse-number":3,"verse_number":4,"content":"In the beginning"}""",
        )

        assertEquals(3, verse.verseNumber)
        assertEquals(4, verse.verseNumberAlt)
        assertEquals("In the beginning", verse.content)
        assertEquals(3, verse.number)
    }

    @Test
    fun aChapterKeepsItsBookNumberAndVerses() {
        val chapter = read(
            BibleChapterResponse.serializer(),
            """{"book":"John","chapter":3,"verses":[{"verse":16}],"content":[{"verse":17}]}""",
        )

        assertEquals("John", chapter.book)
        assertEquals(3, chapter.chapter)
        assertEquals(16, chapter.verses?.single()?.verse)
        assertEquals(17, chapter.content?.single()?.verse)
    }

    @Test
    fun aVerseSentToTheScheduleKeepsItsTranslation() {
        val request = ProjectBibleRequest(
            BibleItemPayload(
                id = "v1",
                bookName = "John",
                chapter = 3,
                verseNumber = 16,
                verseText = "For God so loved the world",
                verseRange = "16-17",
                bibleName = "King James",
                bibleAbbreviation = "KJV",
                useClientText = true,
                bookId = 43,
            ),
        )

        val item = roundTrip(ProjectBibleRequest.serializer(), request).item

        assertEquals("v1", item.id)
        assertEquals("John", item.bookName)
        assertEquals(3, item.chapter)
        assertEquals(16, item.verseNumber)
        assertEquals("For God so loved the world", item.verseText)
        assertEquals("16-17", item.verseRange)
        assertEquals("King James", item.bibleName)
        assertEquals("KJV", item.bibleAbbreviation)
        assertTrue(item.useClientText)
        assertEquals(43, item.bookId)
    }

    @Test
    fun aVerseSelectedDirectlyKeepsItsTranslation() {
        val request = BibleSelectRequest(
            bookName = "John",
            chapter = 3,
            verseNumber = 16,
            bibleName = "King James",
            bibleAbbreviation = "KJV",
            useClientText = true,
            bookId = 43,
        )

        val back = roundTrip(BibleSelectRequest.serializer(), request)

        assertEquals("King James", back.bibleName)
        assertEquals("KJV", back.bibleAbbreviation)
        assertTrue(back.useClientText)
        assertEquals(43, back.bookId)
    }

    @Test
    fun aVerseFromTheDesktopNamesNoTranslationOfItsOwn() {
        val back = roundTrip(BibleSelectRequest.serializer(), BibleSelectRequest("John", 3, 16))

        assertEquals("", back.bibleName)
        assertFalse(back.useClientText)
    }

    // ── Songs ────────────────────────────────────────────────────────────

    @Test
    fun aSongKeepsItsTune() {
        val song = read(Song.serializer(), """{"number":"1","title":"All People","tune":"Old 100th"}""")

        assertEquals("Old 100th", song.tune)
    }

    @Test
    fun aSongWithNoNumberIsKeyedByItsTitle() {
        val song = Song(number = "", title = "Amazing Grace", bookName = "Hymns")

        assertEquals("Hymns::Amazing Grace", song.desktopSongId)
    }

    @Test
    fun aSongOutsideAnyBookIsStillKeyed() {
        assertEquals("::42", Song(number = "42", title = "Amazing Grace").desktopSongId)
    }

    @Test
    fun theSongCatalogIsReadByBook() {
        val response = read(
            SongsResponse.serializer(),
            """{"song-book":[{"book-name":"Hymns","song-total":1,"songs":[{"number":"1","title":"A"}]}],"total":1}""",
        )

        val book = response.songBook.single()
        assertEquals("Hymns", book.bookName)
        assertEquals(1, book.songTotal)
        assertEquals("A", book.songs.single().title)
        assertEquals(1, response.total)
    }

    @Test
    fun aSongSentToTheDesktopKeepsItsItem() {
        val request = ProjectSongRequest(
            SongItemPayload(
                id = "1",
                songNumber = 42,
                title = "Amazing Grace",
                songbook = "Hymns",
                displayText = "42 - Amazing Grace",
            ),
        )

        val item = roundTrip(ProjectSongRequest.serializer(), request).item

        assertTrue(item.type.endsWith("ScheduleItem.SongItem"), "the desktop picks the item's class by this")
        assertEquals("1", item.id)
        assertEquals(42, item.songNumber)
        assertEquals("Amazing Grace", item.title)
        assertEquals("Hymns", item.songbook)
        assertEquals("42 - Amazing Grace", item.displayText)
    }

    @Test
    fun selectingASectionNamesTheSongAndTheSection() {
        val back = roundTrip(SelectSectionRequest.serializer(), SelectSectionRequest("42", 3))

        assertEquals("42", back.number)
        assertEquals(3, back.section)
    }

    @Test
    fun selectingASongOverTheSocketNamesItFully() {
        val back = roundTrip(
            SelectSongPayload.serializer(),
            SelectSongPayload(id = "1", songNumber = 42, title = "Amazing Grace", songbook = "Hymns"),
        )

        assertEquals("1", back.id)
        assertEquals(42, back.songNumber)
        assertEquals("Amazing Grace", back.title)
        assertEquals("Hymns", back.songbook)
    }

    @Test
    fun aSongDetailKeepsItsOrderAndCues() {
        val detail = read(SongDetail.serializer(), """{"order":["v1","c1"],"cues":{"v1":"softly"}}""")

        assertEquals(listOf("v1", "c1"), detail.order)
        assertEquals(mapOf("v1" to "softly"), detail.cues)
    }

    // ── Announcements ────────────────────────────────────────────────────

    @Test
    fun anAnnouncementKeepsEveryTimerField() {
        val request = AnnouncementRequest(
            AnnouncementItemPayload(
                id = "a1",
                announcementText = "Tea and coffee",
                fontSize = 60,
                animationType = "FADE",
                animationDuration = 300,
                timerHours = 1,
                timerMinutes = 2,
                timerSeconds = 3,
                targetHour = 4,
                targetMinute = 5,
                targetSecond = 6,
                timerExpiredText = "Starting now",
                displayText = "Tea",
            ),
        )

        val item = roundTrip(AnnouncementRequest.serializer(), request).item

        assertEquals("a1", item.id)
        assertEquals("Tea and coffee", item.announcementText)
        assertEquals(60, item.fontSize)
        assertEquals("FADE", item.animationType)
        assertEquals(300, item.animationDuration)
        assertEquals(listOf(1, 2, 3), listOf(item.timerHours, item.timerMinutes, item.timerSeconds))
        assertEquals(listOf(4, 5, 6), listOf(item.targetHour, item.targetMinute, item.targetSecond))
        assertEquals("Starting now", item.timerExpiredText)
        assertEquals("Tea", item.displayText)
    }

    @Test
    fun anAnimationIsOfferedByItsLabelAndSentByItsValue() {
        assertEquals("Slide up", AnnouncementAnimation.SLIDE_BOTTOM.label)
        assertEquals("SLIDE_FROM_BOTTOM", AnnouncementAnimation.SLIDE_BOTTOM.value)
    }

    // ── Presentations ────────────────────────────────────────────────────

    @Test
    fun aPresentationIsReadFromTheDesktopsKebabKeys() {
        val response = read(
            PresentationsResponse.serializer(),
            """{"presentations":[{"id":"p1","file-name":"a.pptx","file-type":"pptx","slide-total":3}],"total":1}""",
        )

        val presentation = response.presentations?.single()
        assertEquals("a.pptx", presentation?.fileName)
        assertEquals("pptx", presentation?.fileType)
        assertEquals(3, presentation?.slideTotal)
        assertEquals(1, response.total)
    }

    @Test
    fun aPresentationSentToTheScheduleSaysWhatItIs() {
        val request = PresentationScheduleAddRequest(
            PresentationSchedulePayload(id = "p1", title = "Easter", displayText = "Easter deck"),
        )

        val item = roundTrip(PresentationScheduleAddRequest.serializer(), request).item

        assertEquals("presentation", item.type)
        assertEquals("p1", item.id)
        assertEquals("Easter", item.title)
        assertEquals("Easter deck", item.displayText)
    }

    @Test
    fun selectingASlideNamesItsIndex() {
        assertEquals(2, roundTrip(SelectSlideRequest.serializer(), SelectSlideRequest(2)).index)

        val overSocket = roundTrip(SelectSlideWsPayload.serializer(), SelectSlideWsPayload("p1", 2))
        assertEquals("p1", overSocket.id)
        assertEquals(2, overSocket.index)
    }

    // ── Pictures ─────────────────────────────────────────────────────────

    @Test
    fun aPictureFolderIsReadFromTheDesktopsKebabKeys() {
        val folder = read(
            PicturesFolder.serializer(),
            """{"folder-name":"Easter","folder-path":"/pictures/easter","image-total":12}""",
        )

        assertEquals("Easter", folder.folderName)
        assertEquals("/pictures/easter", folder.folderPath)
        assertEquals(12, folder.imageTotal)
    }

    @Test
    fun selectingAPictureUsesTheDesktopsKebabKeys() {
        val request = PictureSelectRequest(folderId = "f1", fileName = "a.jpg", index = 2)

        val text = json.encodeToString(PictureSelectRequest.serializer(), request)
        val back = read(PictureSelectRequest.serializer(), text)

        assertTrue("\"folder-id\"" in text)
        assertTrue("\"file-name\"" in text)
        assertEquals("f1", back.folderId)
        assertEquals("a.jpg", back.fileName)
        assertEquals(2, back.index)
    }

    @Test
    fun aPictureSentToTheScheduleSaysWhatItIs() {
        val request = PictureScheduleAddRequest(
            PictureSchedulePayload(folderId = "f1", imageIndex = 2, displayText = "a.jpg"),
        )

        val text = json.encodeToString(PictureScheduleAddRequest.serializer(), request)
        val item = read(PictureScheduleAddRequest.serializer(), text).item

        assertTrue("\"image-index\"" in text)
        assertEquals("image", item.type)
        assertEquals("f1", item.folderId)
        assertEquals(2, item.imageIndex)
        assertEquals("a.jpg", item.displayText)
    }

    // ── Dictionary ───────────────────────────────────────────────────────

    @Test
    fun aStrongsEntryKeepsItsRoot() {
        val entry = read(StrongsEntry.serializer(), """{"number":"G26","word":"agape","root":"G25"}""")

        assertEquals("G25", entry.root)
    }

    @Test
    fun aWordsVersesAreReadWithTheirReferences() {
        val response = read(
            DictionaryVersesResponse.serializer(),
            """{"number":"G26","verses":[{"bookName":"John","chapter":3,"verse":16,"reference":"John 3:16"}]}""",
        )

        assertEquals("John 3:16", response.verses.single().reference)
        assertEquals(0, response.total, "an older desktop sends no total")
    }

    @Test
    fun aDictionaryEntrySentToTheDesktopSaysWhatItIs() {
        val request = ProjectDictionaryRequest(
            DictionaryItemPayload(
                id = "d1",
                strongsNumber = "G26",
                title = "agape",
                transliteration = "agapē",
                definition = "love",
                displayText = "G26 agape",
            ),
        )

        val item = roundTrip(ProjectDictionaryRequest.serializer(), request).item

        assertEquals("dictionary", item.type)
        assertEquals("d1", item.id)
        assertEquals("G26", item.strongsNumber)
        assertEquals("agape", item.title)
        assertEquals("agapē", item.transliteration)
        assertEquals("love", item.definition)
        assertEquals("G26 agape", item.displayText)
    }

    // ── Q&A and the schedule ─────────────────────────────────────────────

    @Test
    fun aQuestionKeepsItsWordsAndWhoAskedIt() {
        val back = roundTrip(QATextRequest.serializer(), QATextRequest("Where is the car park?", "Sam"))

        assertEquals("Where is the car park?", back.text)
        assertEquals("Sam", back.name)
    }

    @Test
    fun aQaStatusNeedsOnlyTheSessionFlag() {
        val status = read(QAStatusResponse.serializer(), """{"sessionActive":true}""")

        assertTrue(status.sessionActive)
        assertEquals(30, status.cooldownSeconds)
    }

    @Test
    fun aQuestionFromTheDesktopNeedsNoNameOrVotes() {
        val question = read(
            QuestionDto.serializer(),
            """{"id":"q1","text":"Why?","timestamp":5,"status":"APPROVED"}""",
        ).toQuestion()

        assertEquals("", question.submitterName)
        assertEquals(QuestionStatus.APPROVED, question.status)
    }

    @Test
    fun theScheduleIsReadUnderEitherName() {
        val response = read(ScheduleResponse.serializer(), """{"items":[],"schedule":[],"total":0}""")

        assertEquals(emptyList(), response.items)
        assertEquals(emptyList(), response.schedule)
        assertEquals(0, response.total)
    }

    // ── A set file ───────────────────────────────────────────────────────

    @Test
    fun aSetFileKeepsItsHeaderAndAssets() {
        val document = CpsetDocument(
            exportedAt = 5L,
            appVersion = "1.0",
            assets = listOf(CpsetAsset(id = "a1", mime = "image/png", base64 = "AAAA")),
        )

        val back = roundTrip(CpsetDocument.serializer(), document)

        assertEquals(5L, back.exportedAt)
        assertEquals("1.0", back.appVersion)
        val asset = back.assets.single()
        assertEquals("a1", asset.id)
        assertEquals("image/png", asset.mime)
        assertEquals("AAAA", asset.base64)
    }

    @Test
    fun aPreviewOfNothingNewIsEmpty() {
        val document = CpsetDocument(name = "Easter")

        val preview = ImportPreview(document)

        assertEquals(document, preview.document)
        assertTrue(preview.isEmpty)
    }
}
