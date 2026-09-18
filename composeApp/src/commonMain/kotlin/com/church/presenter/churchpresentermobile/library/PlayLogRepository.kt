package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.PlayLog
import com.church.presenter.churchpresentermobile.model.SongCredit
import com.church.presenter.churchpresentermobile.model.SongPlay
import com.church.presenter.churchpresentermobile.model.VerseCredit
import com.church.presenter.churchpresentermobile.model.VersePlay
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

private const val TAG = "PlayLogRepository"

internal const val PLAY_LOG_FILE = "play_log.json"

/**
 * The dated record of what this device has projected — the source of the CCLI
 * report.
 *
 * One JSON document, held in memory and written through on every play, the
 * same shape as [LibraryRepository]. It is its own file rather than a field of
 * the library because the library is rewritten whole on every song edit, and a
 * play log grows for years: keeping the two apart means neither write pays for
 * the other.
 *
 * A corrupt file is treated as empty. There is no backup copy the way the
 * library keeps one: a play is appended, never edited, so a torn write can only
 * lose the plays of one service, and the operator has the atomic write in
 * [FileStore] standing between them and that.
 *
 * @param now Supplies the timestamp of each play. Injected so tests are deterministic.
 */
class PlayLogRepository(
    private val storage: FileStore = createFileStore(),
    private val now: () -> Long = { 0L },
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val _log = MutableStateFlow(PlayLog.EMPTY)

    /** Every play so far. The report screen collects this rather than re-reading the file. */
    val log: StateFlow<PlayLog> = _log.asStateFlow()

    /** Reads the log from disk. An unreadable file is an empty log. Safe to call more than once. */
    fun load(): PlayLog {
        val text = storage.read(PLAY_LOG_FILE)
        val loaded = if (text.isNullOrBlank()) {
            PlayLog.EMPTY
        } else {
            runCatching { json.decodeFromString<PlayLog>(text) }
                .onFailure { Logger.e(TAG, "could not parse play log: ${it.message}") }
                .getOrDefault(PlayLog.EMPTY)
        }
        _log.value = loaded
        Logger.d(TAG, "loaded ${loaded.songs.size} song plays, ${loaded.verses.size} verse plays")
        return loaded
    }

    /** Appends one play of [credit], stamped with the current time. */
    fun recordSong(credit: SongCredit) {
        mutate { it.copy(songs = it.songs + SongPlay(credit = credit, at = now())) }
        Logger.d(TAG, "recordSong — ${credit.title}")
    }

    /** Appends one play of [credit], stamped with the current time. */
    fun recordVerse(credit: VerseCredit) {
        mutate { it.copy(verses = it.verses + VersePlay(credit = credit, at = now())) }
        Logger.d(TAG, "recordVerse — ${credit.reference}")
    }

    /** Forgets every play. There is no undo; the screen asks before calling this. */
    fun clear() {
        mutate { PlayLog.EMPTY }
        Logger.d(TAG, "clear — play log emptied")
    }

    private fun mutate(block: (PlayLog) -> PlayLog) {
        val updated = block(_log.value)
        _log.value = updated
        runCatching { storage.write(PLAY_LOG_FILE, json.encodeToString(PlayLog.serializer(), updated)) }
            .onFailure { Logger.e(TAG, "could not write play log: ${it.message}") }
    }
}
