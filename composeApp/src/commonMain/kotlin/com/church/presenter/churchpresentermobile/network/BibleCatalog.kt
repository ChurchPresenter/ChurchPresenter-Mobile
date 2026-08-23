package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.model.InstalledBibleBook
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private const val TAG = "BibleCatalog"

/**
 * The read side of the Bible feature, so [BibleCatalog] can be tested — and satisfied by a
 * downloaded module — without an HTTP client. [BibleService] already implements both methods.
 */
interface BibleReader {
    suspend fun getBooks(): Result<List<BibleBook>>
    suspend fun getChapter(bookNumber: Int, chapter: Int): Result<List<BibleVerse>>
}

/**
 * Where Bible text comes from, decided per call by the current [AppMode] and by what is
 * installed. The content twin of [SongCatalog], with one deliberate difference.
 *
 * Songs never read the on-device library in remote mode, so a desktop's list is not overwritten
 * by a library the operator isn't looking at. A Bible is not like that: the desktop's translation
 * and a downloaded copy of the same translation say the same thing, so when the desktop cannot be
 * reached mid-service a downloaded module is strictly better than an empty tab. Remote therefore
 * asks the desktop first and falls back; standalone reads the device and never asks at all.
 *
 * @param bibles Translations on this device. Null means remote-only — the right default for
 *   previews and for tests that predate this.
 */
class BibleCatalog(
    private val mode: StateFlow<AppMode>,
    private val remote: BibleReader,
    private val bibles: LocalBibleRepository? = null,
) {
    /** True when Bible text is being served from this device rather than a desktop. */
    val isLocal: Boolean
        get() = mode.value == AppMode.STANDALONE && bibles != null

    /** [isLocal] as a stream, for UI that must follow a mode switch made in Settings. */
    val isLocalSource: Flow<Boolean> =
        mode.map { it == AppMode.STANDALONE && bibles != null }

    /**
     * Standalone with nothing installed — the Bible tab's empty state, as a stream so that
     * finishing a download opens the tab without the app being restarted.
     */
    val hasNoBible: Flow<Boolean> =
        if (bibles == null) mode.map { it == AppMode.STANDALONE }
        else combine(mode, bibles.index) { current, index ->
            current == AppMode.STANDALONE && index.isEmpty
        }

    /**
     * The books to browse.
     *
     * An empty library is a success with no rows, never a failure: there is nothing wrong, and
     * an error banner on a tab the operator has just opened reads as a fault they must fix.
     */
    suspend fun books(): Result<List<BibleBook>> {
        if (isLocal) return Result.success(localBooks())
        val fromDesktop = remote.getBooks()
        if (fromDesktop.isSuccess) return fromDesktop
        val fallback = localBooks()
        if (fallback.isEmpty()) return fromDesktop
        Logger.d(TAG, "books — desktop unreachable, reading the downloaded translation instead")
        return Result.success(fallback)
    }

    /** One chapter's verses, from the same source [books] came from. */
    suspend fun chapter(bookNumber: Int, chapter: Int): Result<List<BibleVerse>> {
        if (isLocal) return Result.success(localChapter(bookNumber, chapter))
        val fromDesktop = remote.getChapter(bookNumber, chapter)
        if (fromDesktop.isSuccess) return fromDesktop
        val fallback = localChapter(bookNumber, chapter)
        if (fallback.isEmpty()) return fromDesktop
        Logger.d(TAG, "chapter — desktop unreachable, reading the downloaded translation instead")
        return Result.success(fallback)
    }

    /** Books straight out of the index, so drawing the list never parses a module. */
    private fun localBooks(): List<BibleBook> =
        bibles?.index?.value?.active?.books.orEmpty().map(::toBibleBook)

    private fun localChapter(bookNumber: Int, chapter: Int): List<BibleVerse> =
        bibles?.openActive()?.chapter(bookNumber, chapter).orEmpty()

    private fun toBibleBook(book: InstalledBibleBook): BibleBook = BibleBook(
        name = book.name,
        // BibleViewModel.selectBook passes this straight to getChapter, so it has to be the
        // module's own book number rather than a position in the list.
        bookId = book.bookId,
        chapterTotal = book.chapterCount,
    )
}
