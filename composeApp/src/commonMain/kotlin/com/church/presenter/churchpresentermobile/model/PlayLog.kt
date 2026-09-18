package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/** Bumped when [PlayLog] gains a field an older build would misread. */
const val PLAY_LOG_SCHEMA_VERSION: Int = 1

/**
 * Who to credit for a projected song — what a CCLI report needs and nothing
 * the slide renderer wants.
 *
 * Rides on every slide of a song deck so the recorder can read it off whatever
 * is on the screen, without a lookup back into a library that may have been
 * edited since. Absent on anything that is not a song.
 *
 * @property id The library id, so the same song is one row however its title
 *   is later edited. Empty for a song that has no stable id.
 * @property ccliNumber Empty when unknown; the library has no field for it and
 *   it is only ever pulled out of the copyright line.
 */
@Serializable
data class SongCredit(
    val id: String = "",
    val number: String = "",
    val title: String = "",
    val author: String = "",
    val songbook: String = "",
    val ccliNumber: String = "",
) {
    /** What makes this the same song as another play: the id, else book + number + title. */
    val key: String
        get() = id.ifBlank { "$songbook::$number::${title.lowercase()}" }

    companion object {
        /**
         * "CCLI Song # 22025", "CCLI #7011351", "ccli: 4348399" — the song number, when a
         * copyright line carries one. A church's *licence* number sits on the same lines
         * ("CCLI License #123456") and is not the song, so anything naming a licence is skipped.
         */
        private val CCLI_NUMBER = Regex(
            "ccli\\s*(?:song)?\\s*(?:#|no\\.?|number|:)?\\s*#?\\s*(\\d{4,})",
            RegexOption.IGNORE_CASE,
        )
        private val CCLI_LICENCE = Regex("ccli\\s*licen[cs]e", RegexOption.IGNORE_CASE)

        /** The CCLI song number found in [text], or empty. */
        fun ccliNumberIn(text: String?): String {
            if (text.isNullOrBlank() || CCLI_LICENCE.containsMatchIn(text)) return ""
            return CCLI_NUMBER.find(text)?.groupValues?.get(1).orEmpty()
        }
    }
}

/** The verse a Bible slide is, for the report. Absent on anything that is not scripture. */
@Serializable
data class VerseCredit(
    val bibleName: String = "",
    val bookName: String = "",
    val chapter: Int = 0,
    val verse: Int = 0,
) {
    val key: String get() = "$bibleName::$bookName::$chapter::$verse"

    /** "John 3:16" — how the verse is named in the report and its exports. */
    val reference: String get() = "$bookName $chapter:$verse"
}

/** One time a song went on the screen. */
@Serializable
data class SongPlay(
    val credit: SongCredit = SongCredit(),
    val at: Long = 0L,
)

/** One time a verse went on the screen. */
@Serializable
data class VersePlay(
    val credit: VerseCredit = VerseCredit(),
    val at: Long = 0L,
)

/**
 * Everything this device has ever projected, dated.
 *
 * Kept as events rather than counters so the report can be asked about any
 * date range after the fact: a licence period is whatever CCLI says it is,
 * and it is never the period the counter happened to start on.
 */
@Serializable
data class PlayLog(
    val version: Int = PLAY_LOG_SCHEMA_VERSION,
    val songs: List<SongPlay> = emptyList(),
    val verses: List<VersePlay> = emptyList(),
) {
    val isEmpty: Boolean get() = songs.isEmpty() && verses.isEmpty()

    /** When the first play was recorded, or null for an empty log. */
    val earliest: Long?
        get() = (songs.map { it.at } + verses.map { it.at }).minOrNull()

    companion object {
        val EMPTY: PlayLog = PlayLog()
    }
}
