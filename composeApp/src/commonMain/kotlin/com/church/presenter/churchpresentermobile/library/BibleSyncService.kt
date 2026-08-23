package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.SyncProgress
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

private const val TAG = "BibleSyncService"

/** How a finished Bible copy turned out. */
sealed interface BibleSyncOutcome {

    /** @param installed Titles now on the device. @param failed Modules that would not come. */
    data class Success(val installed: List<String>, val failed: List<String>) : BibleSyncOutcome

    /** The desktop could not be reached, or refused. Nothing was changed. */
    data class Failed(val message: String) : BibleSyncOutcome

    /** The operator stopped it. Whatever finished is kept. */
    data class Cancelled(val installed: List<String>) : BibleSyncOutcome
}

/**
 * Copies chosen translations from a desktop onto this device.
 *
 * Deliberately serial where [LibrarySyncService] is concurrent: a song detail is a few hundred
 * bytes and four at once is free, while two 4.6 MB modules in flight together is a memory spike
 * on a phone for no wall-clock gain on a LAN.
 *
 * Each module is written the moment it parses, so an interrupted copy leaves a smaller but
 * entirely valid library rather than nothing.
 *
 * @param listTranslations The desktop's manifest, in its own order.
 * @param downloadTranslation One module by its position in that manifest.
 */
class BibleSyncService(
    private val repository: LocalBibleRepository,
    private val listTranslations: suspend () -> Result<List<String>>,
    private val downloadTranslation: suspend (Int) -> Result<String>,
    private val sourceHost: () -> String = { "" },
) {
    private val _progress = MutableStateFlow(SyncProgress.IDLE)
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()

    private var cancelRequested = false

    /**
     * Asks to stop after the module in flight.
     *
     * A multi-megabyte GET already on the wire cannot be stopped without cancelling the coroutine,
     * which would also kill the line that reports what happened. So this is checked between
     * modules, and the UI says so rather than implying an instant stop.
     */
    fun requestCancel() {
        cancelRequested = true
    }

    /** What the desktop is offering, for the picker. */
    suspend fun catalogue(): Result<List<String>> = listTranslations()

    /**
     * Copies each of [wanted] — file names from the manifest, not positions in it.
     *
     * The desktop indexes into a list it rebuilds from its own settings, so a position captured
     * when the picker was drawn may point at a different module by the time the operator taps
     * Download. The manifest is therefore re-read here and each name resolved against it; a name
     * that has since vanished is reported rather than downloaded by a stale index.
     */
    suspend fun sync(wanted: List<String>): BibleSyncOutcome = withContext(Dispatchers.Default) {
        cancelRequested = false
        _progress.value = SyncProgress(isRunning = true)

        val manifest = listTranslations().getOrElse { error ->
            _progress.value = SyncProgress.IDLE
            Logger.e(TAG, "sync — could not read the manifest: ${error.message}")
            return@withContext BibleSyncOutcome.Failed(error.message ?: "Could not reach your computer")
        }

        val installed = mutableListOf<String>()
        val failed = mutableListOf<String>()
        _progress.value = SyncProgress(done = 0, total = wanted.size, isRunning = true)

        for ((position, fileName) in wanted.withIndex()) {
            if (cancelRequested) {
                _progress.value = SyncProgress.IDLE
                Logger.d(TAG, "sync — stopped after $installed")
                return@withContext BibleSyncOutcome.Cancelled(installed)
            }
            _progress.value = SyncProgress(
                done = position,
                total = wanted.size,
                currentTitle = displayNameFor(fileName),
                isRunning = true,
            )

            val index = manifest.indexOf(fileName)
            if (index < 0) {
                Logger.e(TAG, "sync — '$fileName' is no longer offered; not guessing at an index")
                failed += displayNameFor(fileName)
                continue
            }
            val text = downloadTranslation(index).getOrElse { error ->
                Logger.e(TAG, "sync — '$fileName' failed: ${error.message}")
                failed += displayNameFor(fileName)
                null
            } ?: continue

            val entry = repository.install(fileName, text, sourceHost())
            if (entry == null) failed += displayNameFor(fileName) else installed += entry.title
        }

        _progress.value = SyncProgress.IDLE
        Logger.d(TAG, "sync — installed ${installed.size}, failed ${failed.size}")
        BibleSyncOutcome.Success(installed = installed, failed = failed)
    }

    private companion object {
        /** "en_KJV.spb" reads as "en KJV" until the module itself supplies a title. */
        fun displayNameFor(fileName: String): String =
            fileName.substringBeforeLast(".").replace('_', ' ')
    }
}
