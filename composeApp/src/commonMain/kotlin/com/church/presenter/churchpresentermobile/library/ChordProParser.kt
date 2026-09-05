package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType

/**
 * Reads ChordPro (and plain-text) song files into the local model.
 *
 * ChordPro is the lingua franca of worship-song files — OnSong, SongBook Pro
 * and most desktop tools export it — so supporting it means an operator can
 * bring a library they already have rather than retyping it.
 *
 * Chords are stripped, not kept: this app projects words to a congregation, and
 * `[G]Amazing [D]grace` on a screen is noise. A file with no directives at all
 * is still read as plain text split on blank lines, since that is what a lot of
 * "ChordPro" files really are.
 *
 * Pure `String -> LocalSong`, so the many shapes real files come in are cheap
 * to pin down in tests.
 */
object ChordProParser {

    /** `[Am7]`, `[G/B]` — removed from the projected text. */
    private val chordPattern = Regex("""\[[^\]\n]*\]""")

    /**
     * `{directive: value}` or `{directive}`.
     *
     * Anchored, and matched a line at a time — a brace in the middle of a lyric
     * line is not a directive.
     */
    private val directivePattern = Regex("""^\s*\{\s*([a-zA-Z_]+)\s*:?\s*([^}]*)\}\s*$""")

    /** Unanchored variant, for sniffing a whole file in [looksLikeChordPro]. */
    private val anyDirectivePattern = Regex("""\{\s*[a-zA-Z_]+\s*[:}]""")

    /** Directives that name a section rather than carry metadata. */
    private val sectionStarts = mapOf(
        "start_of_chorus" to SectionType.CHORUS,
        "soc" to SectionType.CHORUS,
        "start_of_bridge" to SectionType.BRIDGE,
        "sob" to SectionType.BRIDGE,
        "start_of_verse" to SectionType.VERSE,
        "sov" to SectionType.VERSE,
        "start_of_tab" to SectionType.VERSE,
    )

    private val sectionEnds = setOf(
        "end_of_chorus", "eoc",
        "end_of_bridge", "eob",
        "end_of_verse", "eov",
        "end_of_tab", "eot",
    )

    /**
     * Parses [text] into a song.
     *
     * @param id Identifier for the new song, supplied by the caller so this
     *   stays free of UUID generation and remains deterministic in tests.
     * @param fallbackTitle Used when the file carries no `{title}` — typically
     *   the file name, which is usually right.
     */
    fun parse(text: String, id: String, fallbackTitle: String = ""): LocalSong {
        var title = ""
        var subtitle = ""
        var artist = ""
        var number = ""
        var book = ""
        var copyright = ""

        val sections = mutableListOf<LocalSongSection>()
        val buffer = StringBuilder()
        var currentType = SectionType.VERSE
        // Non-null while inside an explicit {soc}…{eoc} block, which suppresses
        // the blank-line splitting so a chorus with a gap stays one section.
        var explicitType: SectionType? = null
        var pendingLabel: String? = null

        fun flush() {
            val body = buffer.toString().trim()
            buffer.clear()
            if (body.isEmpty()) return
            sections += LocalSongSection(
                type = explicitType ?: currentType,
                text = body,
                label = pendingLabel,
            )
            pendingLabel = null
        }

        text.lineSequence().forEach { rawLine ->
            val directive = directivePattern.find(rawLine)
            if (directive != null) {
                val key = directive.groupValues[1].lowercase()
                val value = directive.groupValues[2].trim()

                when {
                    key in sectionStarts -> {
                        flush()
                        explicitType = sectionStarts.getValue(key)
                        if (value.isNotEmpty()) pendingLabel = value
                    }

                    key in sectionEnds -> {
                        flush()
                        explicitType = null
                        currentType = SectionType.VERSE
                    }

                    key == "title" || key == "t" -> title = value
                    key == "subtitle" || key == "st" -> subtitle = value
                    key == "artist" || key == "composer" -> artist = value
                    key == "album" || key == "book" || key == "songbook" -> book = value
                    key == "number" || key == "no" -> number = value
                    key == "copyright" || key == "ccli" -> copyright = value

                    // A free-standing comment usually names the part that follows.
                    key == "comment" || key == "c" || key == "ci" -> {
                        flush()
                        pendingLabel = value.takeIf { it.isNotEmpty() }
                        currentType = typeFromLabel(value)
                    }

                    // Anything else (key, tempo, tab settings) is not projected.
                    else -> Unit
                }
                return@forEach
            }

            val line = chordPattern.replace(rawLine, "").trimEnd()

            // A blank line separates stanzas — unless an explicit block owns them.
            if (line.isBlank() && explicitType == null) {
                flush()
            } else if (line.isNotBlank() || buffer.isNotEmpty()) {
                if (buffer.isNotEmpty()) buffer.append('\n')
                buffer.append(line)
            }
        }
        flush()

        return LocalSong(
            id = id,
            number = number,
            title = listOf(title, fallbackTitle).firstOrNull { it.isNotBlank() }.orEmpty(),
            author = listOf(artist, subtitle).firstOrNull { it.isNotBlank() },
            bookName = book.takeIf { it.isNotBlank() },
            copyright = copyright.takeIf { it.isNotBlank() },
            sections = sections.filter { it.text.isNotBlank() },
        )
    }

    /** True when [text] looks like ChordPro — used to pick a parser for a dropped file. */
    fun looksLikeChordPro(text: String): Boolean =
        anyDirectivePattern.containsMatchIn(text) || chordPattern.containsMatchIn(text)

    /** Infers a section type from a comment such as "Chorus 2" or "Bridge". */
    private fun typeFromLabel(label: String): SectionType {
        val normalized = label.trim().lowercase()
        return when {
            normalized.startsWith("chorus") || normalized.startsWith("refrain") -> SectionType.CHORUS
            normalized.startsWith("bridge") -> SectionType.BRIDGE
            normalized.startsWith("tag") -> SectionType.TAG
            normalized.startsWith("ending") || normalized.startsWith("outro") -> SectionType.ENDING
            else -> SectionType.VERSE
        }
    }
}
