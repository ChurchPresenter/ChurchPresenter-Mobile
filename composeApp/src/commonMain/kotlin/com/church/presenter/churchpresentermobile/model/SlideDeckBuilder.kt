package com.church.presenter.churchpresentermobile.model

/**
 * Turns the app's existing content models into projectable [SlideDeck]s.
 *
 * This is the whole of "standalone is a new output target, not a new content
 * layer": nothing here invents a model, it only re-shapes [Song]/[SongDetail],
 * [BibleBook]/[BibleVerse] and announcement text into slides. Every function is
 * pure, so the awkward parts — the API's dozen field-name variants, missing
 * lyrics, empty chapters — are cheap to pin down in tests.
 */
object SlideDeckBuilder {

    /** Separator between the title and the section label on a slide's reference line. */
    private const val REFERENCE_SEPARATOR = " · "

    /**
     * Builds a deck from a song and its fetched lyrics.
     *
     * Prefers the structured verse list ([SongDetail.allVerses]). When the
     * server returned only a blob of text, falls back to splitting
     * [SongDetail.plainText] on blank lines — the near-universal stanza
     * separator. A song with neither yields an empty deck rather than a deck of
     * one empty slide, so the controller can say "no lyrics" instead of
     * projecting a blank screen that looks like a bug.
     */
    fun fromSong(song: Song, detail: SongDetail): SlideDeck {
        val title = detail.title?.takeIf { it.isNotBlank() } ?: song.title
        val number = detail.number?.takeIf { it.isNotBlank() } ?: song.number
        val book = detail.bookName ?: song.bookName

        val bodies: List<Pair<String, String?>> = detail.allVerses
            .map { verse -> verse.displayText to verse.displayLabel }
            .filter { (text, _) -> text.isNotBlank() }
            .ifEmpty {
                splitStanzas(detail.plainText).mapIndexed { i, text -> text to (i + 1).toString() }
            }

        val slides = bodies.mapIndexed { index, (text, label) ->
            Slide(
                kind = SlideKind.SONG,
                body = text.trim(),
                reference = buildReference(title, sectionLabel(label, index)),
                sourceId = listOfNotNull(book, number).joinToString(":").ifBlank { null },
                index = index,
                total = bodies.size,
            )
        }

        return SlideDeck(
            kind = SlideKind.SONG,
            title = listOf(number, title).filter { it.isNotBlank() }.joinToString(" "),
            slides = slides,
        )
    }

    /**
     * Builds a deck from a song the user owns on this device.
     *
     * The local model already has clean structured sections, so unlike
     * [fromSong] there is nothing to guess at — the only work is labelling.
     * A named override on a section wins over the auto-generated label, since a
     * user who typed "Verse 1a" meant it.
     */
    fun fromLocalSong(song: LocalSong): SlideDeck {
        // Labelling is shared with LocalSongAdapter so the heading on the screen
        // and the heading in the detail sheet cannot drift apart.
        val usable = LocalSongAdapter.usableSections(song.sections)
        val labels = LocalSongAdapter.sectionLabels(usable)

        val slides = usable.mapIndexed { index, section ->
            val label = labels[index]
            Slide(
                kind = SlideKind.SONG,
                body = section.text.trim(),
                reference = buildReference(song.title, label),
                footer = song.copyright?.takeIf { it.isNotBlank() },
                sourceId = song.id,
                index = index,
                total = usable.size,
            )
        }

        return SlideDeck(kind = SlideKind.SONG, title = song.displayTitle, slides = slides)
    }

    /**
     * Builds a deck of one slide per verse.
     *
     * When [selectedVerses] is non-empty only those verse numbers are included,
     * which is how the operator projects a passage rather than a whole chapter.
     * An unknown or empty selection falls back to the full chapter — better to
     * offer too much than to hand the operator an empty deck mid-service.
     */
    fun fromBibleChapter(
        book: BibleBook,
        chapter: Int,
        verses: List<BibleVerse>,
        selectedVerses: Set<Int> = emptySet(),
    ): SlideDeck {
        val bookName = book.displayName
        val included = verses
            .filter { it.displayText.isNotBlank() }
            .let { all ->
                if (selectedVerses.isEmpty()) all
                else all.filter { it.number in selectedVerses }.ifEmpty { all }
            }

        val slides = included.mapIndexed { index, verse ->
            Slide(
                kind = SlideKind.BIBLE,
                body = verse.displayText.trim(),
                reference = "$bookName $chapter:${verse.number}",
                sourceId = "$bookName:$chapter:${verse.number}",
                index = index,
                total = included.size,
            )
        }

        return SlideDeck(
            kind = SlideKind.BIBLE,
            title = "$bookName $chapter",
            slides = slides,
        )
    }

    /**
     * Builds a single-slide deck from announcement text.
     *
     * Announcements are set in a sans face — they are notices, not liturgy, and
     * the mockup switches the output to `.sans` for exactly this case.
     */
    fun fromAnnouncement(text: String, title: String? = null, id: String? = null): SlideDeck {
        val body = text.trim()
        if (body.isEmpty()) return SlideDeck(kind = SlideKind.ANNOUNCEMENT, title = title.orEmpty())

        return SlideDeck(
            kind = SlideKind.ANNOUNCEMENT,
            title = title?.takeIf { it.isNotBlank() } ?: body.lineSequence().first().take(40),
            slides = listOf(
                Slide(
                    kind = SlideKind.ANNOUNCEMENT,
                    body = body,
                    reference = title?.takeIf { it.isNotBlank() },
                    theme = SlideTheme(font = SlideFont.SANS),
                    sourceId = id,
                    index = 0,
                    total = 1,
                )
            ),
        )
    }

    /**
     * True for a link the output is allowed to open.
     *
     * Only http and https: the URL reaches an embedded browser view and the
     * display page's iframe, where a `javascript:` or `file:` link would be
     * running code or reading disk on the audience screen. Operator-typed, but
     * typed in a hurry and sometimes pasted from somewhere else.
     */
    fun isProjectableLink(url: String): Boolean = normaliseLink(url) != null

    /** A scheme at the front of a URL: "https:", "javascript:", "file:". */
    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.\\-]*:")

    /**
     * The link as it should actually be opened, or null when it is not one to open.
     *
     * Operators type "youtube.com", because that is what a browser accepts. Requiring the
     * scheme meant the Go Live button silently did nothing for the most ordinary input there
     * is, with no hint as to why — so a missing scheme is now filled in rather than refused.
     *
     * What is still refused is anything carrying a scheme that is not http(s). The address
     * reaches an embedded browser and an iframe, where "javascript:" is code execution and
     * "file:" is the device's disk, on the screen the congregation is looking at. Adding
     * "https://" to something schemeless cannot turn it into either.
     */
    fun normaliseLink(url: String): String? {
        val trimmed = url.trim().removePrefix("//")
        if (trimmed.isBlank()) return null
        val scheme = SCHEME.find(trimmed)?.value?.lowercase()
        return when {
            scheme == null -> "https://$trimmed"
            scheme == "http:" || scheme == "https:" -> trimmed
            else -> null
        }
    }

    /** Extensions the output can play as video rather than show as a page. */
    private val VIDEO_EXTENSIONS = setOf("mp4", "m4v", "mov", "webm", "ogv")

    /** True when [url] points at something to play rather than a page to show. */
    fun looksLikeVideo(url: String): Boolean {
        val path = url.trim().substringBefore('?').substringBefore('#')
        return path.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS
    }

    /**
     * A single-slide deck showing a web page live on the output.
     *
     * Returns an empty deck for anything [isProjectableLink] rejects, so a
     * mistyped address cannot become a slide.
     */
    fun fromWebPage(url: String, title: String = ""): SlideDeck {
        val link = url.trim()
        if (!isProjectableLink(link)) return SlideDeck(kind = SlideKind.WEB, title = title)
        return SlideDeck(
            kind = SlideKind.WEB,
            title = title.ifBlank { link },
            slides = listOf(Slide(kind = SlideKind.WEB, mediaUrl = link, sourceId = link, index = 0, total = 1)),
        )
    }

    /** A single-slide deck playing a video on the output. Same link rules as [fromWebPage]. */
    fun fromVideo(url: String, title: String = ""): SlideDeck {
        val link = url.trim()
        if (!isProjectableLink(link)) return SlideDeck(kind = SlideKind.VIDEO, title = title)
        return SlideDeck(
            kind = SlideKind.VIDEO,
            title = title.ifBlank { link },
            slides = listOf(Slide(kind = SlideKind.VIDEO, mediaUrl = link, sourceId = link, index = 0, total = 1)),
        )
    }

    /**
     * Builds a deck of photographs, one per slide.
     *
     * The image is the slide: no body text, and the backdrop carries it, so the
     * same renderer that puts a photo behind lyrics shows it full-bleed here.
     * The operator steps through them exactly as through a song's sections.
     *
     * @param urls Where each photo can be fetched — for standalone, the phone's
     *   own server. Callers drop photos with no URL rather than passing null.
     */
    fun fromPhotos(urls: List<String>, title: String = ""): SlideDeck {
        val usable = urls.filter { it.isNotBlank() }
        return SlideDeck(
            kind = SlideKind.IMAGE,
            title = title,
            slides = usable.mapIndexed { index, url ->
                Slide(
                    kind = SlideKind.IMAGE,
                    backdrop = SlideBackdrop.IMAGE,
                    backdropUrl = url,
                    sourceId = url,
                    index = index,
                    total = usable.size,
                )
            },
        )
    }

    /**
     * Builds a deck where each entry is its own slide — used for a list of
     * announcements the operator steps through.
     */
    fun fromAnnouncements(items: List<String>, title: String = ""): SlideDeck {
        val bodies = items.map { it.trim() }.filter { it.isNotEmpty() }
        return SlideDeck(
            kind = SlideKind.ANNOUNCEMENT,
            title = title,
            slides = bodies.mapIndexed { index, body ->
                Slide(
                    kind = SlideKind.ANNOUNCEMENT,
                    body = body,
                    theme = SlideTheme(font = SlideFont.SANS),
                    index = index,
                    total = bodies.size,
                )
            },
        )
    }

    /**
     * Renders a section label for display.
     *
     * The API returns either a bare number ("1", "2") or a name ("Chorus",
     * "Bridge"). A bare number on its own reads as nothing on a projected
     * screen, so it becomes "Verse 1"; a name is kept as the server wrote it.
     */
    private fun sectionLabel(label: String?, index: Int): String {
        val trimmed = label?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> "Verse ${index + 1}"
            trimmed.all { it.isDigit() } -> "Verse $trimmed"
            else -> trimmed
        }
    }

    private fun buildReference(title: String, section: String): String? {
        val cleanTitle = title.trim()
        return when {
            cleanTitle.isEmpty() -> section.ifBlank { null }
            else -> cleanTitle + REFERENCE_SEPARATOR + section
        }
    }

    /** Splits a lyrics blob into stanzas on blank lines, dropping empty runs. */
    private fun splitStanzas(text: String?): List<String> =
        text?.split(Regex("\\n\\s*\\n"))
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
}
