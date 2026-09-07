package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The shapes the desktop and the app agree on, decoded from the JSON the
 * desktop actually sends.
 *
 * Every one of these has fields with defaults, which is what lets an older
 * phone talk to a newer desktop and the other way round — but it also means a
 * field the server stopped sending goes quiet rather than failing: the default
 * takes over and the screen shows something plausible and wrong. Each type is
 * decoded twice here, once from a full payload and once from the minimum the
 * server is allowed to send, so both halves of that contract are pinned.
 */
class ApiPayloadWireTest {

    private val json = Json { ignoreUnknownKeys = true }

    private inline fun <reified T> decode(body: String): T = json.decodeFromString(body)

    // ── Q&A ──────────────────────────────────────────────────────────────

    @Test
    fun `a Q and A status decodes in full`() {
        val status: QAStatusResponse = decode(
            """{"sessionActive":true,"cooldownSeconds":60,"displayedQuestionId":"q7","votingEnabled":true}"""
        )

        assertTrue(status.sessionActive)
        assertEquals(60, status.cooldownSeconds)
        assertEquals("q7", status.displayedQuestionId)
        assertTrue(status.votingEnabled)
    }

    @Test
    fun `a Q and A status needs only the session flag`() {
        // The rest predates the voting feature; a desktop that has not been
        // updated sends the flag alone.
        val status: QAStatusResponse = decode("""{"sessionActive":false}""")

        assertFalse(status.sessionActive)
        assertEquals(30, status.cooldownSeconds)
        assertEquals("", status.displayedQuestionId)
        assertFalse(status.votingEnabled)
    }

    @Test
    fun `voting defaults to off rather than on`() {
        // The safe side: a desktop that says nothing about voting must not have
        // the phone offering vote buttons the server will reject.
        assertFalse(decode<QAStatusResponse>("""{"sessionActive":true}""").votingEnabled)
    }

    @Test
    fun `a question decodes with its votes`() {
        val question = decode<QuestionDto>(
            """{"id":"q1","text":"Why?","submitterName":"Ada","timestamp":17,
               "status":"APPROVED","upvotes":4,"downvotes":1}"""
        ).toQuestion()

        assertEquals("q1", question.id)
        assertEquals("Ada", question.submitterName)
        assertEquals(QuestionStatus.APPROVED, question.status)
        assertEquals(4, question.upvotes)
        assertEquals(1, question.downvotes)
    }

    @Test
    fun `a question with no author is anonymous rather than broken`() {
        val question = decode<QuestionDto>(
            """{"id":"q1","text":"Why?","timestamp":17,"status":"PENDING"}"""
        ).toQuestion()

        assertEquals("", question.submitterName)
        assertEquals(0, question.upvotes)
        assertEquals(0, question.downvotes)
    }

    @Test
    fun `every status the desktop can send maps across`() {
        for (status in QuestionStatus.entries) {
            val body = """{"id":"q","text":"t","timestamp":1,"status":"""" + status.name + """"}"""
            val question = decode<QuestionDto>(body).toQuestion()

            assertEquals(status, question.status, status.name)
        }
    }

    @Test
    fun `a status this build does not know is treated as pending`() {
        // A newer desktop adding a state must not make the question vanish from
        // the admin list — pending is where an operator will see and triage it.
        val question = decode<QuestionDto>(
            """{"id":"q","text":"t","timestamp":1,"status":"SOMETHING_NEW"}"""
        ).toQuestion()

        assertEquals(QuestionStatus.PENDING, question.status)
    }

    @Test
    fun `an empty status is treated as pending rather than crashing the tab`() {
        val question = decode<QuestionDto>(
            """{"id":"q","text":"t","timestamp":1,"status":""}"""
        ).toQuestion()

        assertEquals(QuestionStatus.PENDING, question.status)
    }

    // ── Pictures ─────────────────────────────────────────────────────────

    @Test
    fun `an uploaded photo decodes its kebab-case fields`() {
        // The desktop sends folder-id and image-index with hyphens; camelCase
        // here would silently lose the address of the photo just uploaded.
        val uploaded: UploadPhotoResponse = decode(
            """{"ok":true,"folder-id":"f1","image-index":3,"file-name":"advent.jpg"}"""
        )

        assertEquals("f1", uploaded.folderId)
        assertEquals(3, uploaded.imageIndex)
        assertEquals("advent.jpg", uploaded.fileName)
    }

    @Test
    fun `an upload with no file name still carries its address`() {
        val uploaded: UploadPhotoResponse = decode("""{"folder-id":"f1","image-index":0}""")

        assertEquals("f1", uploaded.folderId)
        assertNull(uploaded.fileName)
        assertTrue(uploaded.ok, "an upload that came back at all is an ok one")
    }

    // ── Bible ────────────────────────────────────────────────────────────

    @Test
    fun `a bible selection carries a single verse`() {
        val request: BibleSelectRequest = decode(
            """{"bookName":"John","chapter":3,"verseNumber":16,"verseText":"For God so loved"}"""
        )

        assertEquals("John", request.bookName)
        assertEquals(3, request.chapter)
        assertEquals(16, request.verseNumber)
        assertNull(request.verseRange, "a single verse has no range")
    }

    @Test
    fun `a bible selection carries a range when there is one`() {
        val request: BibleSelectRequest = decode(
            """{"bookName":"John","chapter":3,"verseNumber":16,"verseRange":"16-18"}"""
        )

        assertEquals("16-18", request.verseRange)
        assertEquals("", request.verseText)
    }

    // ── Dictionary ───────────────────────────────────────────────────────

    @Test
    fun `a dictionary verse decodes with its reference`() {
        val verse: DictionaryVerse = decode(
            """{"bookName":"Genesis","chapter":1,"verse":1,"reference":"Genesis 1:1",
               "text":"In the beginning"}"""
        )

        assertEquals("Genesis", verse.bookName)
        assertEquals(1, verse.chapter)
        assertEquals(1, verse.verse)
        assertEquals("Genesis 1:1", verse.reference)
        assertEquals("In the beginning", verse.text)
    }

    @Test
    fun `a dictionary verse with no text still names where it is`() {
        // The list shows the reference; the text arrives with the full lookup.
        val verse: DictionaryVerse = decode(
            """{"bookName":"Genesis","chapter":1,"verse":1,"reference":"Genesis 1:1"}"""
        )

        assertEquals("", verse.text)
        assertEquals("Genesis 1:1", verse.reference)
    }

    @Test
    fun `a verses response caps its list below the real total`() {
        // The desktop sends a page; the total is what the screen reports as
        // "appears in N verses", and reading it off the list would understate it.
        val response: DictionaryVersesResponse = decode(
            """{"number":"H1","total":412,"verses":[
               {"bookName":"Genesis","chapter":1,"verse":1,"reference":"Genesis 1:1"}]}"""
        )

        assertEquals(412, response.total)
        assertEquals(1, response.verses.size)
    }

    @Test
    fun `a number that appears nowhere decodes to an empty list`() {
        val response: DictionaryVersesResponse = decode("""{"number":"H9999"}""")

        assertEquals(0, response.total)
        assertTrue(response.verses.isEmpty())
    }

    // ── Unknown fields ───────────────────────────────────────────────────

    @Test
    fun `a newer desktop's extra fields do not break an older phone`() {
        // The whole reason for ignoreUnknownKeys: every one of these is decoded
        // from a server that ships on its own schedule.
        assertTrue(decode<QAStatusResponse>("""{"sessionActive":true,"future":1}""").sessionActive)
        assertEquals("f1", decode<UploadPhotoResponse>("""{"folder-id":"f1","image-index":0,"x":[]}""").folderId)
        assertEquals(
            "John",
            decode<BibleSelectRequest>("""{"bookName":"John","chapter":1,"verseNumber":1,"x":{}}""").bookName,
        )
    }
}
