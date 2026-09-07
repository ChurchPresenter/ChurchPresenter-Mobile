package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Projecting a web page — a hymn video, a giving page, a slideshow someone
 * built elsewhere.
 *
 * Driven through the real ViewModel and a stand-in sender, so the assertions
 * are about what reaches the desktop rather than about which composable ran.
 * The bookmark list is the part that bites: a saved page is the only record of
 * an address someone typed once, and the row for the page currently on screen
 * has to be the one marked live.
 */
/**
 * Saved pages.
 *
 * A bookmark is the only record of an address someone typed once, and the row
 * for the page currently on screen has to be the one marked live.
 */
@OptIn(ExperimentalTestApi::class)
class WebBookmarksTest {
    @Test
    fun thereAreNoBookmarksToStartWith() = runComposeUiTest {
        showWeb(viewModel())

        assertTrue(exists(UiTags.WEB_NO_BOOKMARKS))
    }

    @Test
    fun savingTheCurrentAddressAddsABookmark() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)

        val id = bookmark(vm, "example.org/giving")

        assertTrue(exists(UiTags.bookmark(id)))
        assertFalse(exists(UiTags.WEB_NO_BOOKMARKS))
    }

    @Test
    fun aBookmarkShowsTheAddressItSaved() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)

        bookmark(vm, "example.org/giving")

        assertTrue(isShowing("example.org"))
    }

    @Test
    fun anEmptyAddressIsNotBookmarked() = runComposeUiTest {
        // A bookmark with no address could never be opened again.
        val vm = viewModel()
        showWeb(vm)

        click(UiTags.WEB_ADD_BOOKMARK)

        assertTrue(vm.bookmarks.value.isEmpty())
        assertTrue(exists(UiTags.WEB_NO_BOOKMARKS))
    }

    @Test
    fun severalBookmarksAreAllListed() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)

        val first = bookmark(vm, "example.org")
        val second = bookmark(vm, "example.com")

        assertTrue(exists(UiTags.bookmark(first)))
        assertTrue(exists(UiTags.bookmark(second)))
    }

    @Test
    fun tappingABookmarkLoadsItIntoTheAddressBar() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)
        val first = bookmark(vm, "example.org/giving")
        type(UiTags.WEB_URL, "something-else.org")

        click(UiTags.bookmark(first))

        assertTrue(vm.url.value.contains("example.org/giving"))
    }

    @Test
    fun tappingLoadsTheBookmarkThatWasTapped() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)
        bookmark(vm, "example.org")
        val second = bookmark(vm, "example.com/notices")

        click(UiTags.bookmark(second))

        assertTrue(vm.url.value.contains("example.com/notices"))
    }

    @Test
    fun aBookmarkCanBeDeleted() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)
        val first = bookmark(vm, "example.org")

        click(UiTags.bookmarkDelete(first))

        assertFalse(exists(UiTags.bookmark(first)))
        assertTrue(exists(UiTags.WEB_NO_BOOKMARKS))
    }

    @Test
    fun deletingOneBookmarkLeavesTheOthers() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)
        val first = bookmark(vm, "example.org")
        val second = bookmark(vm, "example.com")

        click(UiTags.bookmarkDelete(first))

        assertFalse(exists(UiTags.bookmark(first)))
        assertTrue(exists(UiTags.bookmark(second)))
    }

    // ── Which page is on the screen ──────────────────────────────────────
}
