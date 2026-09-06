package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SlideDeckBuilderTest {

    private val amazingGrace = Song(number = "42", title = "Amazing Grace", bookName = "Hymns")

    // ── Songs ────────────────────────────────────────────────────────────

    @Test
    fun `a song with a verses array becomes one slide per verse`() {
        val detail = SongDetail(
            number = "42",
            title = "Amazing Grace",
            verses = listOf(
                SongVerse(number = 1, lines = listOf("Amazing grace!", "how sweet the sound")),
                SongVerse(number = 2, lines = listOf("'Twas grace that taught")),
            ),
        )

        val deck = SlideDeckBuilder.fromSong(amazingGrace, detail)

        assertEquals(SlideKind.SONG, deck.kind)
        assertEquals("42 Amazing Grace", deck.title)
        assertEquals(2, deck.slides.size)
        assertEquals("Amazing grace!\nhow sweet the sound", deck.slides[0].body)
        assertEquals("Amazing Grace · Verse 1", deck.slides[0].reference)
        assertEquals("Amazing Grace · Verse 2", deck.slides[1].reference)
    }

    @Test
    fun `a sections array is used when there is no verses array`() {
        val detail = SongDetail(
            title = "Amazing Grace",
            sections = listOf(SongVerse(label = "Chorus", text = "Praise the Lord")),
        )

        val deck = SlideDeckBuilder.fromSong(amazingGrace, detail)

        assertEquals(1, deck.slides.size)
        assertEquals("Praise the Lord", deck.slides[0].body)
        assertEquals("Amazing Grace · Chorus", deck.slides[0].reference)
    }

    @Test
    fun `a named section label is kept verbatim rather than turned into a verse number`() {
        val detail = SongDetail(
            title = "Amazing Grace",
            verses = listOf(
                SongVerse(number = 1, text = "one"),
                SongVerse(label = "Chorus", text = "chorus"),
                SongVerse(label = "Bridge", text = "bridge"),
            ),
        )

        val references = SlideDeckBuilder.fromSong(amazingGrace, detail).slides.map { it.reference }

        assertEquals(
            listOf("Amazing Grace · Verse 1", "Amazing Grace · Chorus", "Amazing Grace · Bridge"),
            references,
        )
    }

    @Test
    fun `a missing section label falls back to the slide position`() {
        val detail = SongDetail(
            title = "Amazing Grace",
            verses = listOf(SongVerse(text = "first"), SongVerse(text = "second")),
        )

        val references = SlideDeckBuilder.fromSong(amazingGrace, detail).slides.map { it.reference }

        assertEquals(listOf("Amazing Grace · Verse 1", "Amazing Grace · Verse 2"), references)
    }

    @Test
    fun `plain text lyrics are split into stanzas on blank lines`() {
        val detail = SongDetail(
            title = "Amazing Grace",
            text = "Amazing grace\nhow sweet\n\n'Twas grace\nthat taught\n\n\nThrough many dangers",
        )

        val deck = SlideDeckBuilder.fromSong(amazingGrace, detail)

        assertEquals(3, deck.slides.size)
        assertEquals("Amazing grace\nhow sweet", deck.slides[0].body)
        assertEquals("Through many dangers", deck.slides[2].body)
        assertEquals("Amazing Grace · Verse 3", deck.slides[2].reference)
    }

    @Test
    fun `blank verses are dropped rather than projected as empty slides`() {
        val detail = SongDetail(
            title = "Amazing Grace",
            verses = listOf(
                SongVerse(number = 1, text = "real"),
                SongVerse(number = 2, text = "   "),
                SongVerse(number = 3, text = "also real"),
            ),
        )

        val deck = SlideDeckBuilder.fromSong(amazingGrace, detail)

        assertEquals(2, deck.slides.size)
        assertEquals(listOf("real", "also real"), deck.slides.map { it.body })
    }

    @Test
    fun `a song with no lyrics at all yields an empty deck`() {
        val deck = SlideDeckBuilder.fromSong(amazingGrace, SongDetail(title = "Amazing Grace"))

        assertTrue(deck.isEmpty, "an empty deck lets the UI say 'no lyrics' instead of projecting blank")
        assertEquals("42 Amazing Grace", deck.title)
    }

    @Test
    fun `index and total are stamped on every song slide`() {
        val detail = SongDetail(
            title = "Amazing Grace",
            verses = List(4) { SongVerse(number = it + 1, text = "verse $it") },
        )

        val slides = SlideDeckBuilder.fromSong(amazingGrace, detail).slides

        assertEquals(listOf(0, 1, 2, 3), slides.map { it.index })
        assertTrue(slides.all { it.total == 4 })
    }

    @Test
    fun `song slides carry a book and number source id`() {
        val detail = SongDetail(number = "42", title = "Amazing Grace", songbook = "Hymns",
            verses = listOf(SongVerse(number = 1, text = "one")))

        assertEquals("Hymns:42", SlideDeckBuilder.fromSong(amazingGrace, detail).slides[0].sourceId)
    }

    @Test
    fun `song detail fields win over the catalogue entry`() {
        val stale = Song(number = "1", title = "Old Title", bookName = "Hymns")
        val detail = SongDetail(number = "42", title = "Amazing Grace",
            verses = listOf(SongVerse(number = 1, text = "one")))

        val deck = SlideDeckBuilder.fromSong(stale, detail)

        assertEquals("42 Amazing Grace", deck.title)
        assertEquals("Amazing Grace · Verse 1", deck.slides[0].reference)
    }

    @Test
    fun `song slides use the default serif theme`() {
        val detail = SongDetail(title = "Amazing Grace", verses = listOf(SongVerse(number = 1, text = "one")))
        assertEquals(SlideFont.SERIF, SlideDeckBuilder.fromSong(amazingGrace, detail).slides[0].theme.font)
    }

    // ── Local songs ──────────────────────────────────────────────────────

    private fun localSong(sections: List<LocalSongSection>) = LocalSong(
        id = "local-1",
        number = "42",
        title = "Amazing Grace",
        sections = sections,
    )

    @Test
    fun `a local song numbers its verses while naming its other sections`() {
        val deck = SlideDeckBuilder.fromLocalSong(
            localSong(
                listOf(
                    LocalSongSection(SectionType.VERSE, "one"),
                    LocalSongSection(SectionType.CHORUS, "chorus"),
                    LocalSongSection(SectionType.VERSE, "two"),
                    LocalSongSection(SectionType.BRIDGE, "bridge"),
                )
            )
        )

        assertEquals(
            listOf(
                "Amazing Grace · Verse 1",
                "Amazing Grace · Chorus",
                "Amazing Grace · Verse 2",
                "Amazing Grace · Bridge",
            ),
            deck.slides.map { it.reference },
        )
    }

    @Test
    fun `a chorus between verses does not consume a verse number`() {
        val deck = SlideDeckBuilder.fromLocalSong(
            localSong(
                listOf(
                    LocalSongSection(SectionType.VERSE, "one"),
                    LocalSongSection(SectionType.CHORUS, "chorus"),
                    LocalSongSection(SectionType.VERSE, "two"),
                )
            )
        )
        assertEquals("Amazing Grace · Verse 2", deck.slides[2].reference)
    }

    /** A user who typed "Verse 1a" meant it. */
    @Test
    fun `a custom section label wins over the generated one`() {
        val deck = SlideDeckBuilder.fromLocalSong(
            localSong(listOf(LocalSongSection(SectionType.VERSE, "words", label = "Verse 1a")))
        )
        assertEquals("Amazing Grace · Verse 1a", deck.slides.single().reference)
    }

    @Test
    fun `blank local sections are dropped`() {
        val deck = SlideDeckBuilder.fromLocalSong(
            localSong(
                listOf(
                    LocalSongSection(SectionType.VERSE, "real"),
                    LocalSongSection(SectionType.VERSE, "  "),
                )
            )
        )
        assertEquals(1, deck.slides.size)
        assertEquals(1, deck.slides.single().total)
    }

    @Test
    fun `a local song deck is titled with its number`() {
        assertEquals(
            "42 Amazing Grace",
            SlideDeckBuilder.fromLocalSong(
                localSong(listOf(LocalSongSection(SectionType.VERSE, "words")))
            ).title,
        )
    }

    @Test
    fun `copyright becomes the slide footer`() {
        val song = localSong(listOf(LocalSongSection(SectionType.VERSE, "words")))
            .copy(copyright = "CCLI 12345")
        assertEquals("CCLI 12345", SlideDeckBuilder.fromLocalSong(song).slides.single().footer)
    }

    @Test
    fun `a local song with no usable sections yields an empty deck`() {
        assertTrue(SlideDeckBuilder.fromLocalSong(localSong(emptyList())).isEmpty)
    }

    // ── Bible ────────────────────────────────────────────────────────────

    private val john = BibleBook(name = "John", chapterTotal = 21)

    private fun verses(vararg pairs: Pair<Int, String>) =
        pairs.map { (n, t) -> BibleVerse(verse = n, text = t) }

    @Test
    fun `a chapter becomes one slide per verse`() {
        val deck = SlideDeckBuilder.fromBibleChapter(
            john, 3, verses(14 to "And as Moses", 15 to "That whosoever", 16 to "For God so loved"),
        )

        assertEquals(SlideKind.BIBLE, deck.kind)
        assertEquals("John 3", deck.title)
        assertEquals(3, deck.slides.size)
        assertEquals("For God so loved", deck.slides[2].body)
        assertEquals("John 3:16", deck.slides[2].reference)
        assertEquals("John:3:16", deck.slides[2].sourceId)
    }

    @Test
    fun `a verse selection narrows the deck to the chosen passage`() {
        val deck = SlideDeckBuilder.fromBibleChapter(
            john, 3, verses(14 to "a", 15 to "b", 16 to "c", 17 to "d"), selectedVerses = setOf(16, 17),
        )

        assertEquals(2, deck.slides.size)
        assertEquals(listOf("John 3:16", "John 3:17"), deck.slides.map { it.reference })
        assertEquals(listOf(0, 1), deck.slides.map { it.index })
        assertTrue(deck.slides.all { it.total == 2 })
    }

    @Test
    fun `a selection that matches nothing falls back to the whole chapter`() {
        val deck = SlideDeckBuilder.fromBibleChapter(
            john, 3, verses(14 to "a", 15 to "b"), selectedVerses = setOf(99),
        )

        assertEquals(2, deck.slides.size, "an empty deck mid-service is worse than too many verses")
    }

    @Test
    fun `empty verses are dropped`() {
        val deck = SlideDeckBuilder.fromBibleChapter(john, 3, verses(1 to "real", 2 to "  ", 3 to "also real"))

        assertEquals(2, deck.slides.size)
        assertEquals(listOf("John 3:1", "John 3:3"), deck.slides.map { it.reference })
    }

    @Test
    fun `an empty chapter yields an empty deck that still names the passage`() {
        val deck = SlideDeckBuilder.fromBibleChapter(john, 3, emptyList())

        assertTrue(deck.isEmpty)
        assertEquals("John 3", deck.title)
    }

    @Test
    fun `a book falls back to its display name resolution`() {
        val book = BibleBook(bookName = "1 Corinthians")
        val deck = SlideDeckBuilder.fromBibleChapter(book, 13, verses(1 to "Though I speak"))

        assertEquals("1 Corinthians 13", deck.title)
        assertEquals("1 Corinthians 13:1", deck.slides[0].reference)
    }

    // ── Announcements ────────────────────────────────────────────────────

    @Test
    fun `an announcement becomes one sans-serif slide`() {
        val deck = SlideDeckBuilder.fromAnnouncement(
            text = "Prayer meeting\nWednesday · 7:00 pm",
            title = "This week",
            id = "ann-1",
        )

        assertEquals(SlideKind.ANNOUNCEMENT, deck.kind)
        assertEquals(1, deck.slides.size)
        assertEquals(SlideFont.SANS, deck.slides[0].theme.font)
        assertEquals("This week", deck.slides[0].reference)
        assertEquals("ann-1", deck.slides[0].sourceId)
    }

    @Test
    fun `an untitled announcement takes its title from the first line`() {
        val deck = SlideDeckBuilder.fromAnnouncement("Welcome!\nService begins at 10:30")
        assertEquals("Welcome!", deck.title)
        assertNull(deck.slides[0].reference)
    }

    @Test
    fun `a blank announcement yields no slides`() {
        assertTrue(SlideDeckBuilder.fromAnnouncement("   ").isEmpty)
    }

    @Test
    fun `a list of announcements becomes a steppable deck`() {
        val deck = SlideDeckBuilder.fromAnnouncements(
            listOf("Welcome", "  ", "Prayer meeting", "Offering"), title = "Notices",
        )

        assertEquals("Notices", deck.title)
        assertEquals(3, deck.slides.size)
        assertEquals(listOf("Welcome", "Prayer meeting", "Offering"), deck.slides.map { it.body })
        assertTrue(deck.slides.all { it.theme.font == SlideFont.SANS })
        assertTrue(deck.slides.all { it.total == 3 })
    }

    // ── Links ────────────────────────────────────────────────────────────

    @Test
    fun `only http links can be projected`() {
        assertTrue(SlideDeckBuilder.isProjectableLink("http://example.org"))
        assertTrue(SlideDeckBuilder.isProjectableLink("HTTPS://Example.org"))
        assertFalse(SlideDeckBuilder.isProjectableLink("javascript:alert(1)"))
        assertFalse(SlideDeckBuilder.isProjectableLink("file:///etc/passwd"))
        assertFalse(SlideDeckBuilder.isProjectableLink("  "))
        // "example.org" used to be refused here. It is the most ordinary thing an operator
        // types, and refusing it silently is what made the Web screen look broken — it now
        // gets its scheme filled in. The refusals above are unchanged.
        assertTrue(SlideDeckBuilder.isProjectableLink("example.org"))
    }

    @Test
    fun `a rejected link never becomes a slide`() {
        assertTrue(SlideDeckBuilder.fromWebPage("javascript:alert(1)").slides.isEmpty())
        assertTrue(SlideDeckBuilder.fromVideo("file:///movie.mp4").slides.isEmpty())
    }

    @Test
    fun `a web slide carries the page and nothing else`() {
        val deck = SlideDeckBuilder.fromWebPage("https://example.org/notices")

        val slide = deck.slides.single()
        assertEquals(SlideKind.WEB, slide.kind)
        assertEquals("https://example.org/notices", slide.mediaUrl)
        assertEquals("", slide.body)
    }

    @Test
    fun `video extensions are recognised past a query string`() {
        assertTrue(SlideDeckBuilder.looksLikeVideo("https://x.org/a.mp4"))
        assertTrue(SlideDeckBuilder.looksLikeVideo("https://x.org/a.MP4?t=1#s"))
        assertTrue(SlideDeckBuilder.looksLikeVideo("https://x.org/a.webm"))
        assertFalse(SlideDeckBuilder.looksLikeVideo("https://x.org/notices"))
        assertFalse(SlideDeckBuilder.looksLikeVideo("https://x.org/a.png"))
    }

    @Test
    fun `photos become one full-bleed slide each`() {
        val deck = SlideDeckBuilder.fromPhotos(listOf("http://phone/photo/a", "http://phone/photo/b"))

        assertEquals(2, deck.slides.size)
        assertEquals(SlideKind.IMAGE, deck.slides.first().kind)
        assertEquals(SlideBackdrop.IMAGE, deck.slides.first().backdrop)
        assertEquals("http://phone/photo/b", deck.slides.last().backdropUrl)
        assertEquals(listOf(0, 1), deck.slides.map { it.index })
    }
    // ── Links the operator types ─────────────────────────────────────────

    @Test
    fun anAddressWithNoSchemeIsTheOrdinaryCase() {
        // Requiring "https://" made the Go Live button silently do nothing for exactly what
        // people type, which read as the feature being broken.
        assertEquals("https://example.com", SlideDeckBuilder.normaliseLink("example.com"))
        assertEquals("https://youtube.com/watch?v=x", SlideDeckBuilder.normaliseLink("youtube.com/watch?v=x"))
        assertTrue(SlideDeckBuilder.isProjectableLink("example.com"))
    }

    @Test
    fun anAddressThatAlreadyHasASchemeIsLeftAlone() {
        assertEquals("http://192.168.1.5:8000", SlideDeckBuilder.normaliseLink("http://192.168.1.5:8000"))
        assertEquals("https://example.com", SlideDeckBuilder.normaliseLink("  https://example.com  "))
    }

    @Test
    fun aSchemeThatIsNotHttpIsStillRefused() {
        // The address reaches an embedded browser and an iframe on the audience screen, where
        // these are code execution and disk access rather than a page.
        assertNull(SlideDeckBuilder.normaliseLink("javascript:alert(1)"))
        assertNull(SlideDeckBuilder.normaliseLink("file:///etc/passwd"))
        assertNull(SlideDeckBuilder.normaliseLink("data:text/html,<script>"))
        assertNull(SlideDeckBuilder.normaliseLink("JavaScript:alert(1)"))
        assertFalse(SlideDeckBuilder.isProjectableLink("javascript:alert(1)"))
    }

    @Test
    fun fillingInASchemeCannotCreateOneThatWasRefused() {
        // The guard is on the scheme, not on the text, so no amount of prefixing turns a
        // refused link into an allowed one.
        assertNull(SlideDeckBuilder.normaliseLink("javascript:https://example.com"))
    }

    @Test
    fun nothingTypedIsNotALink() {
        assertNull(SlideDeckBuilder.normaliseLink(""))
        assertNull(SlideDeckBuilder.normaliseLink("   "))
        assertFalse(SlideDeckBuilder.isProjectableLink(""))
    }

    @Test
    fun aProtocolRelativeAddressGetsAScheme() {
        assertEquals("https://example.com", SlideDeckBuilder.normaliseLink("//example.com"))
    }

    // ── Titles, numbers and books: which source wins ─────────────────────
    //
    // A song arrives twice — once from the catalogue row and once from the detail
    // fetch — and the two can disagree. The detail is the fuller record, so it
    // wins, but only where it actually says something.

    @Test
    fun `the detail's title wins over the catalogue row`() {
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "42", title = "Amazing Grace"),
            SongDetail(number = "42", title = "Amazing Grace (arr.)", text = "words"),
        )

        assertTrue(deck.title.contains("arr."), deck.title)
    }

    @Test
    fun `a blank detail title falls back to the catalogue row`() {
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "42", title = "Amazing Grace"),
            SongDetail(number = "42", title = "   ", text = "words"),
        )

        assertTrue(deck.title.contains("Amazing Grace"), deck.title)
    }

    @Test
    fun `a blank detail number falls back to the catalogue row`() {
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "42", title = "Grace"),
            SongDetail(number = "", title = "Grace", text = "words"),
        )

        assertTrue(deck.title.startsWith("42"), deck.title)
    }

    @Test
    fun `a song with no number is titled by name alone`() {
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "", title = "Grace"),
            SongDetail(title = "Grace", text = "words"),
        )

        assertEquals("Grace", deck.title)
    }

    @Test
    fun `the book and number identify the slide's source`() {
        // sourceId is how a later edit finds its way back to the right song.
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "42", title = "Grace", bookName = "Hymns"),
            SongDetail(number = "42", title = "Grace", text = "words"),
        )

        assertEquals("Hymns:42", deck.slides.first().sourceId)
    }

    @Test
    fun `a song with neither book nor number has no source id`() {
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "", title = "Grace"),
            SongDetail(title = "Grace", text = "words"),
        )

        assertNull(deck.slides.first().sourceId)
    }

    // ── Section labels ───────────────────────────────────────────────────

    @Test
    fun `a numeric label is expanded into a verse heading`() {
        // Desktops send "1"; the screen should read "Verse 1".
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "42", title = "Grace"),
            SongDetail(
                title = "Grace",
                verses = listOf(SongVerse(label = "2", lines = listOf("words"))),
            ),
        )

        assertTrue(deck.slides.first().reference!!.endsWith("Verse 2"), deck.slides.first().reference!!)
    }

    @Test
    fun `a worded label is kept as it was sent`() {
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "42", title = "Grace"),
            SongDetail(
                title = "Grace",
                verses = listOf(SongVerse(label = "Chorus", lines = listOf("words"))),
            ),
        )

        assertTrue(deck.slides.first().reference!!.endsWith("Chorus"), deck.slides.first().reference!!)
    }

    @Test
    fun `a missing label is numbered by position`() {
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "42", title = "Grace"),
            SongDetail(
                title = "Grace",
                verses = listOf(
                    SongVerse(lines = listOf("one")),
                    SongVerse(lines = listOf("two")),
                ),
            ),
        )

        assertTrue(deck.slides[1].reference!!.endsWith("Verse 2"), deck.slides[1].reference!!)
    }

    @Test
    fun `a whitespace-only label is numbered by position too`() {
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "42", title = "Grace"),
            SongDetail(title = "Grace", verses = listOf(SongVerse(label = "   ", lines = listOf("one")))),
        )

        assertTrue(deck.slides.first().reference!!.endsWith("Verse 1"), deck.slides.first().reference!!)
    }

    @Test
    fun `a song with no title shows only the section heading`() {
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "", title = ""),
            SongDetail(title = "", verses = listOf(SongVerse(label = "Chorus", lines = listOf("words")))),
        )

        assertEquals("Chorus", deck.slides.first().reference)
    }

    // ── Falling back to plain lyrics ─────────────────────────────────────

    @Test
    fun `a song with no verses is split into stanzas on blank lines`() {
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "42", title = "Grace"),
            SongDetail(title = "Grace", text = "one\n\ntwo\n\nthree"),
        )

        assertEquals(3, deck.slides.size)
        assertEquals(listOf("one", "two", "three"), deck.slides.map { it.body })
    }

    @Test
    fun `runs of blank lines do not produce empty slides`() {
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "42", title = "Grace"),
            SongDetail(title = "Grace", text = "one\n\n\n\n   \n\ntwo"),
        )

        assertEquals(2, deck.slides.size)
        assertTrue(deck.slides.none { it.body.isBlank() })
    }

    @Test
    fun `verses that are all blank fall through to the plain text`() {
        // A desktop can answer with a verse array that decodes but says nothing.
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "42", title = "Grace"),
            SongDetail(
                title = "Grace",
                verses = listOf(SongVerse(label = "Verse 1", lines = emptyList())),
                text = "the real words",
            ),
        )

        assertEquals(listOf("the real words"), deck.slides.map { it.body })
    }

    @Test
    fun `a song with nothing at all makes an empty deck`() {
        val deck = SlideDeckBuilder.fromSong(
            Song(id = 1, number = "42", title = "Grace"),
            SongDetail(title = "Grace"),
        )

        assertTrue(deck.slides.isEmpty())
    }

    // ── Announcements ────────────────────────────────────────────────────

    @Test
    fun `an announcement with no title is headed by its first line`() {
        val deck = SlideDeckBuilder.fromAnnouncement(text = "Welcome to the service\nStarts at 10")

        assertEquals("Welcome to the service", deck.title)
    }

    @Test
    fun `a very long first line is trimmed for the heading`() {
        // The heading is a label, not the content; the body still has all of it.
        val long = "w".repeat(120)
        val deck = SlideDeckBuilder.fromAnnouncement(text = long)

        assertEquals(40, deck.title.length)
        assertEquals(long, deck.slides.first().body)
    }

    @Test
    fun `a blank announcement title falls back to the first line`() {
        val deck = SlideDeckBuilder.fromAnnouncement(text = "Welcome", title = "   ")

        assertEquals("Welcome", deck.title)
        assertNull(deck.slides.first().reference, "a blank title is no heading at all")
    }

    @Test
    fun `an announcement with a title shows it above the body`() {
        val deck = SlideDeckBuilder.fromAnnouncement(text = "Starts at 10", title = "Welcome")

        assertEquals("Welcome", deck.title)
        assertEquals("Welcome", deck.slides.first().reference)
    }

    @Test
    fun `an empty announcement makes an empty deck`() {
        // Nothing to project; a blank slide would just black out the screen.
        val deck = SlideDeckBuilder.fromAnnouncement(text = "   ")

        assertTrue(deck.slides.isEmpty())
    }

    @Test
    fun `an empty announcement still keeps its title`() {
        val deck = SlideDeckBuilder.fromAnnouncement(text = "", title = "Welcome")

        assertEquals("Welcome", deck.title)
    }

    @Test
    fun `an announcement carries its id so an edit can find it again`() {
        val deck = SlideDeckBuilder.fromAnnouncement(text = "Welcome", id = "a1")

        assertEquals("a1", deck.slides.first().sourceId)
    }

    @Test
    fun `an announcement is drawn in the sans face`() {
        // Notices are prose, not verse; the serif face is for lyrics.
        val deck = SlideDeckBuilder.fromAnnouncement(text = "Welcome")

        assertEquals(SlideFont.SANS, deck.slides.first().theme.font)
    }
}
