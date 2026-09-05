package com.church.presenter.churchpresentermobile.model

/**
 * Presents a [LocalSong] from the on-device library in the shapes the Songs tab
 * already speaks — [Song] for the list, [SongDetail] for the lyric sheet.
 *
 * Adapting here rather than teaching the Songs screens about the library is what
 * keeps that whole UI — the number chip, search, the book filter, the verse
 * cards and their "Verse 2 / Chorus" headings — working unchanged on local
 * content. The alternative was a parallel set of standalone screens.
 */
object LocalSongAdapter {

    /** The library song as a catalogue row. */
    fun toSong(song: LocalSong): Song = Song(
        // The desktop's numeric id has no meaning here; localId is the real key.
        id = -1,
        number = song.number,
        title = song.title,
        author = song.author,
        bookName = song.bookName,
        localId = song.id,
    )

    /**
     * The library song as a lyric payload.
     *
     * Sections become verses with an explicit label, so the detail sheet shows
     * the same headings the Library tab does instead of re-deriving them.
     */
    fun toDetail(song: LocalSong): SongDetail {
        val usable = usableSections(song.sections)
        val labels = sectionLabels(usable)
        return SongDetail(
            number = song.number,
            title = song.title,
            author = song.author,
            bookNameCamel = song.bookName,
            sections = usable.mapIndexed { index, section ->
                SongVerse(label = labels[index], text = section.text.trim())
            },
        )
    }

    /** Sections with any actual words in them — a blank section is not a slide. */
    fun usableSections(sections: List<LocalSongSection>): List<LocalSongSection> =
        sections.filter { it.text.isNotBlank() }

    /**
     * The heading for each of [usable], in order.
     *
     * A name the user typed always wins over a generated one — someone who wrote
     * "Verse 1a" meant it. Verses are numbered by their own running count, so a
     * chorus between verses one and two does not make the next verse "Verse 3".
     *
     * Shared with [SlideDeckBuilder.fromLocalSong] so the label on the projected
     * slide and the label in the detail sheet cannot drift apart.
     */
    fun sectionLabels(usable: List<LocalSongSection>): List<String> {
        var verseNumber = 0
        return usable.map { section ->
            if (section.type == SectionType.VERSE) verseNumber++
            section.label?.takeIf { it.isNotBlank() }
                ?: when (section.type) {
                    SectionType.VERSE -> "Verse $verseNumber"
                    SectionType.CHORUS -> "Chorus"
                    SectionType.BRIDGE -> "Bridge"
                    SectionType.TAG -> "Tag"
                    SectionType.ENDING -> "Ending"
                }
        }
    }
}
