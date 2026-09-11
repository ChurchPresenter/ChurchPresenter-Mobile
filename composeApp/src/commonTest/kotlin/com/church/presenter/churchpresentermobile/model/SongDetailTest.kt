package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * How a song's detail is read out of whatever shape the desktop sent it in.
 *
 * [SongDetail] declares the same three things several times over — the songbook
 * name under five keys, the verses under eight, the plain words under six —
 * because different ChurchPresenter versions name them differently and the app
 * has to read them all. Every one of those fields is optional, so the class is
 * almost entirely *fallback*, and fallback is exactly the kind of code that is
 * never noticed when it picks wrong.
 *
 * What picking wrong costs: the songbook name is what goes back to the desktop
 * in the project request (see CALLBACK_API.md), so reading the wrong key sends
 * the congregation a song out of the wrong book.
 */
class SongDetailTest {

    // ── The songbook name ────────────────────────────────────────────────

    @Test
    fun `the kebab-case key wins when several are present`() {
        val detail = SongDetail(
            bookNameKebab = "Kebab",
            bookNameCamel = "Camel",
            bookNameSnake = "Snake",
            songbook = "Songbook",
            songBookKebab = "SongBook",
        )

        assertEquals("Kebab", detail.bookName)
    }

    @Test
    fun `each key is read when the ones before it are absent`() {
        // The order is the contract: a newer desktop sends the first, an older
        // one the last, and a test that only checked the first would pass while
        // every older server silently lost its songbook.
        assertEquals("Camel", SongDetail(bookNameCamel = "Camel").bookName)
        assertEquals("Snake", SongDetail(bookNameSnake = "Snake").bookName)
        assertEquals("Songbook", SongDetail(songbook = "Songbook").bookName)
        assertEquals("SongBook", SongDetail(songBookKebab = "SongBook").bookName)
    }

    @Test
    fun `a blank name is skipped rather than shown`() {
        // A desktop that serialises an empty string rather than omitting the key
        // must not win over the one that actually has the name.
        val detail = SongDetail(bookNameKebab = "   ", bookNameCamel = "Hymnal")

        assertEquals("Hymnal", detail.bookName)
    }

    @Test
    fun `no name at all is null, not an empty string`() {
        assertNull(SongDetail(title = "Amazing Grace").bookName)
    }

    // ── The verses ───────────────────────────────────────────────────────

    @Test
    fun `the verses key wins when several arrays are present`() {
        val detail = SongDetail(
            verses = listOf(verse("first")),
            sections = listOf(verse("second")),
            lyrics = listOf(verse("third")),
        )

        assertEquals("first", detail.allVerses.single().text)
    }

    @Test
    fun `each verse array is read when the ones before it are absent`() {
        assertEquals("s", SongDetail(sections = listOf(verse("s"))).allVerses.single().text)
        assertEquals("l", SongDetail(lyrics = listOf(verse("l"))).allVerses.single().text)
        assertEquals("sl", SongDetail(slides = listOf(verse("sl"))).allVerses.single().text)
        assertEquals("st", SongDetail(stanzas = listOf(verse("st"))).allVerses.single().text)
        assertEquals("kv", SongDetail(songVersesKebab = listOf(verse("kv"))).allVerses.single().text)
        assertEquals("cv", SongDetail(songVersesCamel = listOf(verse("cv"))).allVerses.single().text)
        assertEquals("vl", SongDetail(verseListKebab = listOf(verse("vl"))).allVerses.single().text)
    }

    @Test
    fun `an empty array is skipped for one with verses in it`() {
        // The failure this guards: a desktop sending `"verses": []` alongside a
        // populated `sections` showed the operator a song with no words.
        val detail = SongDetail(verses = emptyList(), sections = listOf(verse("real")))

        assertEquals("real", detail.allVerses.single().text)
    }

    @Test
    fun `no verses at all is an empty list, not null`() {
        // The UI iterates this directly, so it must never be null.
        assertEquals(emptyList(), SongDetail(title = "Amazing Grace").allVerses)
    }

    // ── The plain-text fallback ──────────────────────────────────────────

    @Test
    fun `the text key wins when several are present`() {
        val detail = SongDetail(text = "text", content = "content", words = "words")

        assertEquals("text", detail.plainText)
    }

    @Test
    fun `each text key is read when the ones before it are absent`() {
        assertEquals("c", SongDetail(content = "c").plainText)
        assertEquals("w", SongDetail(words = "w").plainText)
        assertEquals("b", SongDetail(body = "b").plainText)
        assertEquals("lk", SongDetail(lyricsTextKebab = "lk").plainText)
        assertEquals("lc", SongDetail(lyricsTextCamel = "lc").plainText)
    }

    @Test
    fun `blank text is skipped`() {
        assertEquals("real", SongDetail(text = "  ", content = "real").plainText)
    }

    @Test
    fun `no text at all is null`() {
        assertNull(SongDetail(title = "Amazing Grace").plainText)
    }

    // ── Whether there is anything to show ────────────────────────────────

    @Test
    fun `a song with verses has lyrics`() {
        assertTrue(SongDetail(verses = listOf(verse("something"))).hasLyrics)
    }

    @Test
    fun `a song with only plain text still has lyrics`() {
        // This is the whole reason plainText exists — an older desktop sends the
        // words as one blob, and the screen has to offer them rather than claim
        // the song is empty.
        assertTrue(SongDetail(text = "one long blob of words").hasLyrics)
    }

    @Test
    fun `a song with neither has none`() {
        assertFalse(SongDetail(title = "Amazing Grace", tune = "NEW BRITAIN").hasLyrics)
    }

    @Test
    fun `an empty verse array with no text has no lyrics`() {
        assertFalse(SongDetail(verses = emptyList(), text = "   ").hasLyrics)
    }

    // ── The rest of the payload ──────────────────────────────────────────

    @Test
    fun `the plain fields are carried through untouched`() {
        // No fallback on these, but they are what the header and the project
        // request are built from, so they are worth pinning.
        val detail = SongDetail(number = "42", title = "Amazing Grace", tune = "NEW BRITAIN", author = "John Newton")

        assertEquals("42", detail.number)
        assertEquals("Amazing Grace", detail.title)
        assertEquals("NEW BRITAIN", detail.tune)
        assertEquals("John Newton", detail.author)
    }

    // ── The wire ─────────────────────────────────────────────────────────
    //
    // The fallbacks above only matter because the desktop really does send these
    // under different keys, so the keys themselves are worth pinning: a
    // @SerialName typo leaves the field null, the fallback quietly moves to the
    // next one, and the screen shows something plausible and wrong.

    @Test
    fun `each songbook key lands in its own field`() {
        // Asserted on the field rather than on `bookName`: read through the
        // fallback, a @SerialName moved to the WRONG property still passes,
        // because the next candidate picks the value up and the answer is the
        // same. On the field it cannot.
        assertEquals("Kebab", decode("""{"book-name":"Kebab"}""").bookNameKebab)
        assertEquals("Camel", decode("""{"bookName":"Camel"}""").bookNameCamel)
        assertEquals("Snake", decode("""{"book_name":"Snake"}""").bookNameSnake)
        assertEquals("Songbook", decode("""{"songbook":"Songbook"}""").songbook)
        assertEquals("SongBook", decode("""{"song-book":"SongBook"}""").songBookKebab)
    }

    @Test
    fun `each songbook key is also readable through the fallback`() {
        assertEquals("Kebab", decode("""{"book-name":"Kebab"}""").bookName)
        assertEquals("Camel", decode("""{"bookName":"Camel"}""").bookName)
        assertEquals("Snake", decode("""{"book_name":"Snake"}""").bookName)
        assertEquals("Songbook", decode("""{"songbook":"Songbook"}""").bookName)
        assertEquals("SongBook", decode("""{"song-book":"SongBook"}""").bookName)
    }

    @Test
    fun `each verse key lands in its own field`() {
        val body = """[{"type":"verse","text":"words"}]"""
        fun one(key: String, read: (SongDetail) -> List<SongVerse>?) =
            assertEquals("words", read(decode("""{"$key":$body}"""))?.singleOrNull()?.text, "key $key")

        one("verses") { it.verses }
        one("sections") { it.sections }
        one("lyrics") { it.lyrics }
        one("slides") { it.slides }
        one("stanzas") { it.stanzas }
        one("song-verses") { it.songVersesKebab }
        one("songVerses") { it.songVersesCamel }
        one("verse-list") { it.verseListKebab }
    }

    @Test
    fun `every verse key is also readable through the fallback`() {
        val body = """[{"type":"verse","text":"words"}]"""
        listOf("verses", "sections", "lyrics", "slides", "stanzas", "song-verses", "songVerses", "verse-list")
            .forEach { key ->
                val detail = decode("""{"$key":$body}""")
                assertEquals("words", detail.allVerses.singleOrNull()?.text, "key $key did not decode")
            }
    }

    @Test
    fun `each plain-words key lands in its own field`() {
        assertEquals("blob", decode("""{"text":"blob"}""").text)
        assertEquals("blob", decode("""{"content":"blob"}""").content)
        assertEquals("blob", decode("""{"words":"blob"}""").words)
        assertEquals("blob", decode("""{"body":"blob"}""").body)
        assertEquals("blob", decode("""{"lyrics-text":"blob"}""").lyricsTextKebab)
        assertEquals("blob", decode("""{"lyricsText":"blob"}""").lyricsTextCamel)
    }

    @Test
    fun `every plain-words key is also readable through the fallback`() {
        listOf("text", "content", "words", "body", "lyrics-text", "lyricsText").forEach { key ->
            assertEquals("blob", decode("""{"$key":"blob"}""").plainText, "key $key did not decode")
        }
    }

    @Test
    fun `a detail survives a round trip through the wire`() {
        // Encoding is what the app does when it hands a song back to the
        // desktop, so every declared key has to write as well as read.
        val original = SongDetail(
            number = "42",
            title = "Amazing Grace",
            tune = "NEW BRITAIN",
            author = "John Newton",
            bookNameKebab = "Hymnal",
            verses = listOf(verse("Amazing grace! how sweet the sound")),
            text = "Amazing grace! how sweet the sound",
        )

        val decoded: SongDetail = json.decodeFromString(json.encodeToString(SongDetail.serializer(), original))

        assertEquals(original, decoded)
        assertEquals("Hymnal", decoded.bookName)
        assertEquals("Amazing grace! how sweet the sound", decoded.allVerses.single().text)
    }

    @Test
    fun `a detail the desktop sends almost empty still decodes`() {
        // The minimum an older server is allowed to send. Every field has a
        // default, which is what lets an old phone talk to a new desktop — and
        // is also why a field going quiet has to be visible as null, not as a
        // plausible stand-in.
        val detail: SongDetail = decode("""{"title":"Amazing Grace"}""")

        assertEquals("Amazing Grace", detail.title)
        assertNull(detail.bookName)
        assertNull(detail.plainText)
        assertEquals(emptyList(), detail.allVerses)
        assertFalse(detail.hasLyrics)
    }

    @Test
    fun `an unknown key from a newer desktop is ignored rather than fatal`() {
        val detail: SongDetail = decode("""{"title":"Amazing Grace","somethingNew":{"a":1}}""")

        assertEquals("Amazing Grace", detail.title)
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun decode(body: String): SongDetail = json.decodeFromString(body)

    private fun verse(text: String) = SongVerse(type = "verse", text = text)
}
