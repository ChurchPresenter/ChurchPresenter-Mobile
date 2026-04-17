package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val number: String,
    val title: String,
    val tune: String? = null,
    val author: String? = null,
    // bookName is not in the JSON — populated in code after parsing
    val bookName: String? = null
)

// ── Project / schedule-add request models ─────────────────────────────────────

private const val SONG_ITEM_TYPE =
    "org.churchpresenter.app.churchpresenter.models.ScheduleItem.SongItem"

/**
 * The `item` object sent in the body of POST /api/project and POST /api/schedule/add.
 */
@Serializable
data class SongItemPayload(
    val type: String = SONG_ITEM_TYPE,
    val id: String,
    val songNumber: Int,
    val title: String,
    val songbook: String? = null,
    val displayText: String
)

/** Wrapper request body for both project and schedule-add endpoints. */
@Serializable
data class ProjectSongRequest(val item: SongItemPayload)

/** Request body for POST /api/songs/{number}/select — navigates to a specific section index.
 *  Both fields are required: `number` must match the song number in the URL path,
 *  and `section` is the 0-based index into the song's section list. */
@Serializable
data class SelectSectionRequest(val number: String, val section: Int)

/** Payload for the WebSocket `select_song` command — navigates the schedule to a song. */
@Serializable
data class SelectSongPayload(
    val id: String,
    val songNumber: Int,
    val title: String,
    val songbook: String? = null,
)

@Serializable
data class SongBook(
    @SerialName("book-name") val bookName: String,
    @SerialName("song-total") val songTotal: Int,
    val songs: List<Song>
)

@Serializable
data class SongsResponse(
    @SerialName("song-book") val songBook: List<SongBook>,
    val songBooks: Int? = null,
    val total: Int? = null
)

/**
 * A single verse (stanza) within a [SongDetail].
 * Maps every field name variant we've seen in ChurchPresenter-style APIs.
 */
@Serializable
data class SongVerse(
    // Verse number / index
    val number: Int? = null,
    val verse: Int? = null,
    val index: Int? = null,
    @SerialName("verse-number") val verseNumberKebab: Int? = null,
    @SerialName("verseNumber") val verseNumberCamel: Int? = null,
    @SerialName("slide-index") val slideIndex: Int? = null,
    // Label / type (e.g. "Chorus", "Bridge")
    val label: String? = null,
    val type: String? = null,
    val name: String? = null,
    // Lines as a list
    val lines: List<String>? = null,
    @SerialName("verse-lines") val verseLinesKebab: List<String>? = null,
    @SerialName("verseLines") val verseLinesCamel: List<String>? = null,
    // Full text as a single string
    val text: String? = null,
    val content: String? = null,
    val lyrics: String? = null,
    val words: String? = null,
    val body: String? = null,
    @SerialName("verse-text") val verseTextKebab: String? = null,
    @SerialName("verseText") val verseTextCamel: String? = null,
    @SerialName("slide-text") val slideText: String? = null,
    @SerialName("lyric-text") val lyricText: String? = null,
) {
    /** The verse number/label to display (e.g. "1", "Chorus"). */
    val displayLabel: String?
        get() = label?.takeIf { it.isNotBlank() }
            ?: type?.takeIf { it.isNotBlank() }
            ?: name?.takeIf { it.isNotBlank() }
            ?: (number ?: verse ?: verseNumberKebab ?: verseNumberCamel ?: index)?.toString()

    /** All lines joined — tries every possible field, lines-list first then text fallbacks. */
    val displayText: String
        get() {
            val linesList = lines ?: verseLinesKebab ?: verseLinesCamel
            if (!linesList.isNullOrEmpty()) return linesList.joinToString("\n")
            return listOfNotNull(text, content, lyrics, words, body, verseTextKebab, verseTextCamel, slideText, lyricText)
                .firstOrNull { it.isNotBlank() } ?: ""
        }
}

/**
 * Full song details returned by GET /api/songs/{number}[?songbook=Name].
 */
@Serializable
data class SongDetail(
    val number: String? = null,
    val title: String? = null,
    val tune: String? = null,
    val author: String? = null,
    @SerialName("book-name")    val bookNameKebab: String? = null,
    @SerialName("bookName")     val bookNameCamel: String? = null,
    @SerialName("book_name")    val bookNameSnake: String? = null,
    @SerialName("songbook")     val songbook: String? = null,
    @SerialName("song-book")    val songBookKebab: String? = null,
    // Verse arrays — every common container name
    val verses: List<SongVerse>? = null,
    val lyrics: List<SongVerse>? = null,
    val slides: List<SongVerse>? = null,
    val stanzas: List<SongVerse>? = null,
    @SerialName("song-verses")  val songVersesKebab: List<SongVerse>? = null,
    @SerialName("songVerses")   val songVersesCamel: List<SongVerse>? = null,
    @SerialName("verse-list")   val verseListKebab: List<SongVerse>? = null,
    // Plain-text fallbacks
    val text: String? = null,
    val content: String? = null,
    val words: String? = null,
    val body: String? = null,
    @SerialName("lyrics-text")  val lyricsTextKebab: String? = null,
    @SerialName("lyricsText")   val lyricsTextCamel: String? = null,
) {
    val bookName: String?
        get() = listOfNotNull(bookNameKebab, bookNameCamel, bookNameSnake, songbook, songBookKebab)
            .firstOrNull { it.isNotBlank() }

    val allVerses: List<SongVerse>
        get() = listOfNotNull(verses, lyrics, slides, stanzas, songVersesKebab, songVersesCamel, verseListKebab)
            .firstOrNull { it.isNotEmpty() } ?: emptyList()

    val hasLyrics: Boolean
        get() = allVerses.isNotEmpty() || plainText != null

    val plainText: String?
        get() = listOfNotNull(text, content, words, body, lyricsTextKebab, lyricsTextCamel)
            .firstOrNull { it.isNotBlank() }
}


