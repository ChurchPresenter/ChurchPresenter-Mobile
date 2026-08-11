package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.generateUUID
import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.model.SyncOutcome
import com.church.presenter.churchpresentermobile.model.SyncProgress
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

private const val TAG = "LibrarySyncService"

/** How many song-detail requests are in flight at once. */
private const val DETAIL_CONCURRENCY = 4

/** Songs written per batch, so an interrupted sync leaves valid partial data. */
private const val BATCH_SIZE = 50

/**
 * Pulls a desktop's song catalogue onto the phone for offline use.
 *
 * The catalogue is one request but the lyrics are one request per song, which
 * is the expensive part — those run four at a time and are written to the
 * library in batches, so a sync interrupted by a dropped Wi-Fi connection
 * leaves a smaller but entirely valid library rather than nothing.
 *
 * Merging is delegated to [LibraryMerge], which is where the rules protecting
 * the user's own edits live.
 *
 * @param fetchCatalogue Returns the desktop's song list. Injected so the whole
 *   service is testable without a server.
 * @param fetchDetail Returns one song's lyrics.
 */
class LibrarySyncService(
    private val repository: LibraryRepository,
    private val fetchCatalogue: suspend () -> Result<List<Song>>,
    private val fetchDetail: suspend (Song) -> Result<SongDetail>,
    private val now: () -> Long = { 0L },
) {
    private val _progress = MutableStateFlow(SyncProgress.IDLE)
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()

    /**
     * Fetches the catalogue and merges it in.
     *
     * Cancelling the calling scope stops the sync; whatever batches had already
     * been written are kept.
     */
    suspend fun sync(): SyncOutcome {
        _progress.value = SyncProgress(isRunning = true)

        val catalogue = fetchCatalogue().getOrElse { error ->
            _progress.value = SyncProgress.IDLE
            Logger.e(TAG, "catalogue fetch failed: ${error.message}")
            return SyncOutcome.Failed(error.message ?: "Could not reach the computer")
        }

        if (catalogue.isEmpty()) {
            _progress.value = SyncProgress.IDLE
            return SyncOutcome.Success(songCount = 0, failedCount = 0, keptLocal = 0)
        }

        _progress.value = SyncProgress(done = 0, total = catalogue.size, isRunning = true)

        var failed = 0
        var completed = 0
        var keptLocal = 0
        val pending = mutableListOf<LocalSong>()

        try {
            val gate = Semaphore(DETAIL_CONCURRENCY)
            catalogue.chunked(BATCH_SIZE).forEach { batch ->
                val fetched = coroutineScope {
                    batch.map { song ->
                        async {
                            gate.withPermit {
                                val result = fetchDetail(song)
                                _progress.value = SyncProgress(
                                    done = ++completed,
                                    total = catalogue.size,
                                    currentTitle = song.title,
                                    isRunning = true,
                                )
                                result.getOrNull()?.let { toLocalSong(song, it) }
                                    ?: run {
                                        // One unreadable song must not abort the
                                        // rest of the catalogue.
                                        failed++
                                        Logger.e(TAG, "detail failed for ${song.number} ${song.title}")
                                        null
                                    }
                            }
                        }
                    }.awaitAll().filterNotNull()
                }

                pending += fetched
                // Write through per batch so an interruption leaves valid data.
                keptLocal = applyMerge(pending)
            }
        } catch (e: CancellationException) {
            _progress.value = SyncProgress.IDLE
            Logger.d(TAG, "sync cancelled after $completed of ${catalogue.size}")
            return SyncOutcome.Cancelled(songCount = pending.size)
        }

        _progress.value = SyncProgress.IDLE
        Logger.d(TAG, "sync complete — ${pending.size} songs, $failed failed, $keptLocal kept local")
        return SyncOutcome.Success(
            songCount = pending.size,
            failedCount = failed,
            keptLocal = keptLocal,
        )
    }

    /** Merges everything fetched so far, returning how many local edits were preserved. */
    private fun applyMerge(fetched: List<LocalSong>): Int {
        val referenced = repository.setlists
            .flatMap { it.entries }
            .map { it.reference }
            .toSet()

        val result = LibraryMerge.mergeSongs(
            existing = repository.library.value.songs,
            incoming = fetched,
            referencedIds = referenced,
        )
        repository.replaceAll(repository.library.value.copy(songs = result.songs))
        return result.kept
    }

    /**
     * Converts a fetched song into the local model.
     *
     * Reuses the same section-shaping the presenter already does: prefer the
     * structured verse list, fall back to splitting a plain-text blob on blank
     * lines. Section type is inferred from the server's label, since the desktop
     * has no typed notion of a chorus.
     */
    private fun toLocalSong(song: Song, detail: SongDetail): LocalSong {
        val verses = detail.allVerses
            .map { it.displayText.trim() to it.displayLabel }
            .filter { (text, _) -> text.isNotBlank() }

        val sections = if (verses.isNotEmpty()) {
            verses.map { (text, label) ->
                LocalSongSection(type = sectionTypeFor(label), text = text)
            }
        } else {
            detail.plainText
                ?.split(Regex("\\n\\s*\\n"))
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.map { LocalSongSection(type = SectionType.VERSE, text = it) }
                .orEmpty()
        }

        return LocalSong(
            id = generateUUID(),
            number = detail.number?.takeIf { it.isNotBlank() } ?: song.number,
            title = detail.title?.takeIf { it.isNotBlank() } ?: song.title,
            author = detail.author ?: song.author,
            bookName = detail.bookName ?: song.bookName,
            sections = sections,
            origin = ContentOrigin.DESKTOP,
            updatedAt = now(),
        )
    }

    /** Maps a server-supplied label onto a typed section. Bare numbers are verses. */
    internal fun sectionTypeFor(label: String?): SectionType {
        val normalized = label?.trim()?.lowercase().orEmpty()
        return when {
            normalized.startsWith("chorus") || normalized.startsWith("refrain") -> SectionType.CHORUS
            normalized.startsWith("bridge") -> SectionType.BRIDGE
            normalized.startsWith("tag") -> SectionType.TAG
            normalized.startsWith("ending") || normalized.startsWith("outro") -> SectionType.ENDING
            else -> SectionType.VERSE
        }
    }
}
