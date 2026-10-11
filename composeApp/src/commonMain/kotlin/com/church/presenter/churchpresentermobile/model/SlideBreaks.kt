package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.library.Chords

/**
 * Turns a song's sections into the slides the desktop actually shows.
 *
 * A long verse is split across slides with a manual break — a `[---]` line written inside it.
 * The desktop presenter honours that break, so one verse becomes two slides there, while the
 * song-detail API still sends the verse whole with the marker left in its lines. Selecting a
 * section by index (`select_song_section`) addresses the desktop's *slide* list, so a phone
 * that lists sections as they arrive is off by one for every break above the one tapped:
 * picking verse 2 lands on the second half of verse 1.
 *
 * [pieces] rebuilds the desktop's slide list from the sections, so an index means the same
 * slide on both ends — for what the phone sends, for the slide the desktop reports as live, and
 * for the deck standalone mode projects. It mirrors the desktop's rules (SongsViewModelSections
 * in the desktop repo) and has to keep doing so:
 *  - a break ends a slide only if the slide has something in it, so a leading or doubled
 *    marker adds no blank slide;
 *  - a slide that is nothing but chords — an intro, a turnaround — has no words to show, so it
 *    is folded into the slide after it (or, at the very end, the one before it), and a song
 *    that is chords throughout is left as written.
 */
object SlideBreaks {

    /** The desktop's grammar (ChordTransposer.SLIDE_BREAK_LINE): dashes in `[]` or `{}`, e.g. `[---]`. */
    private val BREAK_LINE = Regex("^[\\[{][\\s-]*-[\\s-]*[\\]}]$")

    fun isBreak(line: String): Boolean = BREAK_LINE.matches(line.trim())

    /**
     * One slide: the text it shows, the index of the section it came from in the list given
     * to [pieces], and which of that section's slides it is ([part] of [parts], 0-based).
     */
    data class Piece(val source: Int, val text: String, val part: Int, val parts: Int)

    /** The slides [sections] make, in order — see the class comment for the rules. */
    fun pieces(sections: List<String>): List<Piece> {
        val split = sections.flatMapIndexed { source, text -> chunks(text).map { source to it } }
        val folded = foldChordOnly(split) ?: split

        val counts = folded.groupingBy { it.first }.eachCount()
        val seen = mutableMapOf<Int, Int>()
        return folded.map { (source, text) ->
            val part = seen.getOrElse(source) { 0 }
            seen[source] = part + 1
            Piece(source = source, text = text, part = part, parts = counts.getValue(source))
        }
    }

    /** [text] cut at its break lines, keeping only the pieces with a non-blank line in them. */
    private fun chunks(text: String): List<String> {
        val out = mutableListOf<String>()
        val current = mutableListOf<String>()
        fun flush() {
            if (current.any { it.isNotBlank() }) out += current.joinToString("\n").trim('\n')
            current.clear()
        }
        for (line in text.lines()) {
            if (isBreak(line)) flush() else current += line
        }
        flush()
        return out
    }

    /** Null when every slide is chords only — the desktop leaves such a song as written. */
    private fun foldChordOnly(slides: List<Pair<Int, String>>): List<Pair<Int, String>>? {
        val out = mutableListOf<Pair<Int, String>>()
        val carried = mutableListOf<String>()
        for ((source, text) in slides) {
            if (Chords.stripChords(text).isBlank()) {
                carried += text
                continue
            }
            out += source to (carried + text).joinToString("\n")
            carried.clear()
        }
        if (carried.isNotEmpty()) {
            val (source, text) = out.removeLastOrNull() ?: return null
            out += source to (listOf(text) + carried).joinToString("\n")
        }
        return out
    }
}
