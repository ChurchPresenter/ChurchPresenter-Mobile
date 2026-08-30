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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

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

    /** Serialises the read-modify-write of [SyncProgress] across concurrent detail fetches. */
    private val progressLock = Mutex()

    /**
     * Set by [requestCancel] and checked between units of work.
     *
     * Cancellation is cooperative rather than a scope cancel because the caller
     * needs the outcome back: cancelling the coroutine that is awaiting [sync]
     * means the line which publishes [SyncOutcome.Cancelled] never runs, and the
     * sheet just stops with no explanation.
     */
    private var cancelRequested = false

    /** Asks a sync in flight to stop. Whatever was already written is kept. */
    fun requestCancel() {
        cancelRequested = true
    }

    /**
     * Fetches the catalogue and merges it in.
     *
     * All of the work runs on [Dispatchers.Default]. That is not an optimisation:
     * [LibraryRepository] writes the whole library document synchronously, so on
     * the caller's main dispatcher every batch blocked the UI thread — the app
     * appeared to freeze for the duration of a sync.
     */
    /**
     * @param books Which songbooks to copy, by [Song.bookName]. Null takes the
     *   whole catalogue — a church that uses one of five books on the computer
     *   should not have to carry the other four on a phone. Filtered here rather
     *   than at the source because the catalogue already names each song's book,
     *   so choosing costs no extra request.
     */
    suspend fun sync(books: Set<String>? = null): SyncOutcome = withContext(Dispatchers.Default) {
        cancelRequested = false
        _progress.value = SyncProgress(isRunning = true)

        try {
            val fetched = fetchCatalogue().getOrElse { error ->
                Logger.e(TAG, "catalogue fetch failed: ${error.message}")
                return@withContext SyncOutcome.Failed(error.message ?: "Could not reach the computer")
            }
            val catalogue = if (books == null) fetched else fetched.filter { it.bookName in books }
            if (books != null) {
                Logger.d(TAG, "sync — ${catalogue.size} of ${fetched.size} songs in ${books.size} chosen book(s)")
            }

            if (catalogue.isEmpty()) {
                return@withContext SyncOutcome.Success(songCount = 0, failedCount = 0, keptLocal = 0)
            }

            _progress.value = SyncProgress(done = 0, total = catalogue.size, isRunning = true)

            var failed = 0
            var completed = 0
            var keptLocal = 0
            var written = 0

            val gate = Semaphore(DETAIL_CONCURRENCY)
            for (batch in catalogue.chunked(BATCH_SIZE)) {
                if (cancelRequested) break

                val fetched = coroutineScope {
                    batch.map { song ->
                        async {
                            gate.withPermit {
                                if (cancelRequested) return@withPermit null
                                val result = fetchDetail(song)
                                progressLock.withLock {
                                    completed++
                                    _progress.value = SyncProgress(
                                        done = completed,
                                        total = catalogue.size,
                                        currentTitle = song.title,
                                        isRunning = true,
                                    )
                                }
                                // One unreadable song must not abort the rest of
                                // the catalogue.
                                result.getOrNull()?.let { toLocalSong(song, it) }
                            }
                        }
                    }.awaitAll()
                }

                // Counted here rather than inside the coroutines: four of them run
                // at once, and `failed++` from all four is a lost-update race on
                // every platform whose dispatcher is not single-threaded.
                failed += fetched.count { it == null }

                // Merge only this batch. Merging the running total instead made the
                // work quadratic in catalogue size — 500 songs meant re-serialising
                // 50 + 100 + … + 500 songs — which is what made a large sync crawl.
                val batchSongs = fetched.filterNotNull()
                written += batchSongs.size
                keptLocal = applyMerge(batchSongs)
            }

            if (cancelRequested) {
                Logger.d(TAG, "sync cancelled after $completed of ${catalogue.size}")
                return@withContext SyncOutcome.Cancelled(songCount = written)
            }

            Logger.d(TAG, "sync complete — $written songs, $failed failed, $keptLocal kept local")
            SyncOutcome.Success(
                songCount = written,
                failedCount = failed,
                keptLocal = keptLocal,
            )
        } finally {
            // Every exit — success, failure, cancel, or the scope dying under us —
            // must leave the sheet idle, or it stays stuck showing a running sync.
            _progress.value = SyncProgress.IDLE
        }
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
