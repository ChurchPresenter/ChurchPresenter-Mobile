package com.church.presenter.churchpresentermobile.library

/**
 * Chord markup inside song lyrics, and how to take it back out.
 *
 * Songs copied from the computer arrive with their chords still in them: the
 * companion API builds a song's sections straight from its lyric lines, markup
 * and all, so `[G]Amazing grace` is what reaches this device. Chords belong to
 * whoever is playing, never to the audience, so the projected words have to be
 * stripped before they reach a screen.
 *
 * The rule is ported from the desktop's `ChordTransposer` rather than reinvented,
 * so both apps agree on what a chord is. That matters more than it looks:
 * stripping every bracketed token would also eat `[Repeat]` and `[Verse 1]`,
 * silently deleting words the author wrote.
 */
object Chords {

    /**
     * What counts as a chord rather than a section name. Deliberately strict:
     * `[Verse 1]` and `[Bridge]` must not parse as chords, or a header would
     * vanish out of the lyric line.
     */
    private val CHORD = Regex(
        "^[A-G][#b]?" +                                  // root
            "(maj|min|dim|aug|sus|add|m|M)?" +           // quality
            "[0-9]*" +                                   // extension
            "(sus[24]|add[0-9]+|dim|aug)?" +             // trailing modifier
            "(/[A-G][#b]?)?$"                            // slash bass
    )

    /** Everything in brackets, chord or not. */
    private val BRACKETED = Regex("""\[[^\]\n]*\]""")

    /** True when [token] — the text between the brackets — names a chord. */
    fun isChord(token: String): Boolean = CHORD.matches(token.trim())

    /** True when [text] carries at least one real chord. */
    fun hasChords(text: String): Boolean =
        BRACKETED.findAll(text).any { isChord(it.value.trim('[', ']')) }

    /**
     * [text] with its chord markers removed and nothing else touched.
     *
     * Bracketed text that is not a chord survives verbatim — the author meant
     * those words. Whitespace left stranded where a chord used to sit is
     * collapsed, and each line is trimmed, so `[G] Amazing` does not project
     * with a leading gap. Lyrics carry no meaningful indentation, and the slide
     * lays the line out itself.
     */
    fun stripChords(text: String): String {
        if (!hasChords(text)) return text
        return text.lineSequence()
            .map { line ->
                BRACKETED.replace(line) { match ->
                    if (isChord(match.value.trim('[', ']'))) "" else match.value
                }.replace(Regex(" {2,}"), " ").trim()
            }
            .joinToString("\n")
    }
}
