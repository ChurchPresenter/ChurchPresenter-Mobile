package com.church.presenter.churchpresentermobile.bibleimport.catalog

import com.church.presenter.churchpresentermobile.library.InstallDetails
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "BibleDownloads"

/** Where one catalogue row's install stands. */
sealed interface InstallState {
    val bible: CatalogBible

    /** Waiting behind another install. */
    data class Queued(override val bible: CatalogBible) : InstallState

    data class Running(override val bible: CatalogBible, val progress: InstallProgress) : InstallState

    data class Done(
        override val bible: CatalogBible,
        val installed: InstalledBible,
        val books: Int,
        val verses: Int,
    ) : InstallState

    data class Failed(
        override val bible: CatalogBible,
        val failure: InstallFailure,
        val detail: String,
        /** Where it got to, so the failed step is the one marked on screen. */
        val progress: InstallProgress,
    ) : InstallState
}

/**
 * The installs in flight, and the ones just finished.
 *
 * Owned by the app shell rather than by a screen, because design 3a offers "Run in background":
 * closing the install sheet — or leaving the Bible tab entirely — must not cancel a download
 * halfway through. Installs run one at a time, in the order they were asked for; the rest wait
 * as [InstallState.Queued].
 *
 * @param scope Where installs run. Defaults to one owned here, which lives as long as the app.
 * @param convertDispatcher Where the CPU-bound conversion runs; tests pass their own.
 */
class BibleDownloads(
    private val repository: LocalBibleRepository,
    fetcher: WebFetcher = KtorWebFetcher(),
    private val clock: () -> Long,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val convertDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val installer = BibleInstaller(fetcher, clock)
    private val oneAtATime = Mutex()
    /** Touched only from the caller's thread — [install] and [cancel] — never from an install. */
    private val jobs = mutableMapOf<String, Job>()

    private val _states = MutableStateFlow<Map<String, InstallState>>(emptyMap())

    /** Every install this session knows about, by catalogue key. */
    val states: StateFlow<Map<String, InstallState>> = _states.asStateFlow()

    /** Starts installing [bible], or queues it behind the one in progress. Asking twice is a no-op. */
    fun install(bible: CatalogBible) {
        val current = _states.value[bible.key]
        if (current is InstallState.Queued || current is InstallState.Running) return
        set(InstallState.Queued(bible))
        jobs[bible.key] = scope.launch {
            oneAtATime.withLock { run(bible) }
        }
    }

    /** Stops an install; it leaves nothing behind, since nothing is stored until the last step. */
    fun cancel(key: String) {
        jobs.remove(key)?.cancel()
        _states.update { it - key }
    }

    /** Forgets a finished or failed install, once the operator has seen how it ended. */
    fun dismiss(key: String) {
        val state = _states.value[key]
        if (state is InstallState.Done || state is InstallState.Failed) _states.update { it - key }
    }

    fun retry(key: String) {
        val state = _states.value[key] as? InstallState.Failed ?: return
        install(state.bible)
    }

    private suspend fun run(bible: CatalogBible) {
        var progress = InstallProgress()
        try {
            val (body, downloaded) = installer.download(bible) {
                progress = it
                set(InstallState.Running(bible, it))
            }
            progress = downloaded
            val converted = withContext(convertDispatcher) {
                installer.convert(bible, body) { convert ->
                    progress = progress.copy(convert = convert)
                    set(InstallState.Running(bible, progress))
                }
            }
            progress = progress.copy(step = InstallStep.INSTALL)
            set(InstallState.Running(bible, progress))
            val installed = withContext(convertDispatcher) {
                repository.install(
                    fileName = bible.fileName,
                    text = converted.spbText,
                    details = InstallDetails(
                        languageName = languageOf(bible, converted.bible.language),
                        origin = bible.source.label,
                        license = converted.license,
                        catalogKey = bible.key,
                    ),
                )
            } ?: installFailed(InstallFailure.STORAGE, "the module could not be stored")
            set(InstallState.Done(bible, installed, converted.bible.books.size, converted.bible.verseCount))
            Logger.d(TAG, "installed ${bible.key} as ${installed.fileName}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: InstallException) {
            Logger.e(TAG, "install ${bible.key} failed: ${e.failure} ${e.message}")
            set(InstallState.Failed(bible, e.failure, e.message.orEmpty(), progress))
        }
    }

    /**
     * What to call the installed translation's language: the catalogue's name, unless the file
     * turned out to be in another language — the Zefania archive files a Ukrainian Bible under
     * its Russian folder, and the module is converted as the Ukrainian it is.
     */
    private fun languageOf(bible: CatalogBible, converted: String?): String =
        if (converted != null && !converted.equals(bible.language, ignoreCase = true)) {
            BibleLanguageNames.nameOf(converted)
        } else {
            bible.languageLabel
        }

    /**
     * Records [state] — unless it is progress for an install that is no longer listed. A progress
     * callback can land just after [cancel] removed the row, and would otherwise bring a cancelled
     * install back as a "Running" one that never ends.
     */
    private fun set(state: InstallState) {
        _states.update { current ->
            val cancelled = state !is InstallState.Queued && state.bible.key !in current
            if (cancelled) current else current + (state.bible.key to state)
        }
    }
}
