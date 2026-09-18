package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Every slide the builder makes carries what the CCLI report needs to know
 * about it — and nothing else does.
 */
class SlideCreditTest {

    private val localSong = LocalSong(
        id = "uuid-1",
        number = "42",
        title = "Amazing Grace",
        author = "John Newton",
        bookName = "Hymnal",
        copyright = "Public domain · CCLI Song # 22025",
        sections = listOf(LocalSongSection(text = "Amazing grace"), LocalSongSection(text = "'Twas grace")),
    )

    @Test
    fun `a library song's slides all carry the same credit`() {
        val deck = SlideDeckBuilder.fromLocalSong(localSong)

        val credits = deck.slides.map { it.songCredit }.distinct()

        assertEquals(1, credits.size, "every section credits the same song")
        val credit = credits.single()!!
        assertEquals("uuid-1", credit.id)
        assertEquals("42", credit.number)
        assertEquals("Amazing Grace", credit.title)
        assertEquals("John Newton", credit.author)
        assertEquals("Hymnal", credit.songbook)
    }

    @Test
    fun `the CCLI number comes out of the copyright line`() {
        assertEquals("22025", SlideDeckBuilder.fromLocalSong(localSong).slides.first().songCredit?.ccliNumber)
        val unlicensed = SlideDeckBuilder.fromLocalSong(localSong.copy(copyright = null))
        assertEquals("", unlicensed.slides.first().songCredit?.ccliNumber)
    }

    @Test
    fun `a desktop song is credited from its catalogue row and detail`() {
        val song = Song(number = "7", title = "Come Thou Fount", bookName = "Hymnal", author = "Robinson")
        val detail = SongDetail(title = "Come Thou Fount", sections = listOf(SongVerse(text = "Come thou fount")))

        val credit = SlideDeckBuilder.fromSong(song, detail).slides.single().songCredit

        assertEquals(
            SongCredit(number = "7", title = "Come Thou Fount", author = "Robinson", songbook = "Hymnal"),
            credit,
        )
    }

    @Test
    fun `each verse slide credits its own verse in the named translation`() {
        val book = BibleBook(name = "John", bookId = 43)
        val verses = listOf(BibleVerse(verse = 16, text = "For God"), BibleVerse(verse = 17, text = "For God sent"))

        val deck = SlideDeckBuilder.fromBibleChapter(book, 3, verses, bibleName = "KJV")

        assertEquals(
            listOf(
                VerseCredit(bibleName = "KJV", bookName = "John", chapter = 3, verse = 16),
                VerseCredit(bibleName = "KJV", bookName = "John", chapter = 3, verse = 17),
            ),
            deck.slides.map { it.verseCredit },
        )
    }

    @Test
    fun `a verse from a desktop has no translation name`() {
        val verses = listOf(BibleVerse(verse = 16, text = "x"))
        val deck = SlideDeckBuilder.fromBibleChapter(BibleBook(name = "John"), 3, verses)

        assertEquals("", deck.slides.single().verseCredit?.bibleName)
    }

    @Test
    fun `nothing else is credited`() {
        val notice = SlideDeckBuilder.fromAnnouncement("Coffee after").slides.single()
        val photo = SlideDeckBuilder.fromPhotos(listOf("http://p/1.jpg")).slides.single()
        val page = SlideDeckBuilder.fromWebPage("https://example.org").slides.single()

        listOf(notice, photo, page).forEach {
            assertNull(it.songCredit)
            assertNull(it.verseCredit)
        }
    }
}
