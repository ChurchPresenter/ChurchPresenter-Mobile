package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.BibleBook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Bible tab's book list.
 *
 * A stateless list, so what it has to get right is which of three states it is
 * in: a search that found nothing, a Bible that was never downloaded, and a
 * fetch still in flight. Confusing the first two sends an operator to download
 * a translation they already have — or to clear a filter that is not set.
 */
/**
 * The Bible tab's book list.
 *
 * Three states it has to keep apart: a search that found nothing, a Bible that
 * was never downloaded, and a fetch still in flight. Confusing the first two
 * sends an operator to download a translation they already have.
 */
@OptIn(ExperimentalTestApi::class)
class BibleBooksListTest {
    @Test
    fun everyBookIsListed() = runComposeUiTest {
        showBooks()

        assertTrue(exists(UiTags.bibleBook("Genesis")))
        assertTrue(exists(UiTags.bibleBook("Exodus")))
    }

    @Test
    fun aBookIsListedByName() = runComposeUiTest {
        showBooks()

        assertTrue(isShowing("Genesis"))
    }

    @Test
    fun booksWithAndWithoutAChapterCountAreBothListed() = runComposeUiTest {
        // Some modules do not declare a count; that book still has to be
        // openable rather than dropped from the list.
        //
        // The count itself is drawn through a formatted string resource, which
        // renders empty in this runtime, so it cannot be asserted here — see
        // the wasmJs note in AGENT.md.
        showBooks(books = listOf(book("Genesis", chapters = 50), book("Obadiah", chapters = 0)))

        assertTrue(exists(UiTags.bibleBook("Genesis")))
        assertTrue(exists(UiTags.bibleBook("Obadiah")))
    }

    @Test
    fun tappingABookReportsIt() = runComposeUiTest {
        var picked: BibleBook? = null
        showBooks(onBookSelect = { picked = it })

        click(UiTags.bibleBook("Exodus"))

        assertEquals("Exodus", picked?.displayName)
    }

    @Test
    fun tappingReportsTheBookThatWasTapped() = runComposeUiTest {
        var picked: BibleBook? = null
        showBooks(onBookSelect = { picked = it })

        click(UiTags.bibleBook("Genesis"))

        assertEquals("Genesis", picked?.displayName)
    }

    // ── Searching ────────────────────────────────────────────────────────

    @Test
    fun typingInTheSearchFieldIsReported() = runComposeUiTest {
        // The screen holds no query — filtering is the caller's job.
        var typed: String? = null
        showBooks(onSearchQueryChange = { typed = it })

        type(UiTags.BIBLE_SEARCH, "gen")

        assertEquals("gen", typed)
    }

    @Test
    fun theSearchFieldShowsTheQueryItWasGiven() = runComposeUiTest {
        showBooks(searchQuery = "gen")

        assertTrue(isShowing("gen"))
    }
}
