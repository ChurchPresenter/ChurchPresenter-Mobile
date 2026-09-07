package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Which saved page the congregation is looking at.
 *
 * A page still marked live after Clear would say the congregation can see it,
 * and the live row offers no delete — removing it would leave nothing on screen
 * to clear it from.
 */
@OptIn(ExperimentalTestApi::class)
class WebLiveMarkerTest {

    @Test
    fun noBookmarkIsMarkedLiveBeforeAnythingIsProjected() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)
        val first = bookmark(vm, "example.org")

        assertFalse(exists(UiTags.bookmarkLive(first)))
    }

    @Test
    fun theProjectedPagesBookmarkIsMarkedLive() = runComposeUiTest {
        // The one cue that says which of several saved pages the congregation
        // is looking at.
        val vm = viewModel()
        showWeb(vm)
        val first = bookmark(vm, "example.org")

        click(UiTags.WEB_GO_LIVE)

        awaitThat { vm.liveUrl.value != null }
        assertTrue(exists(UiTags.bookmarkLive(first)))
    }

    @Test
    fun onlyTheProjectedPageIsMarkedLive() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)
        val first = bookmark(vm, "example.org")
        val second = bookmark(vm, "example.com")

        click(UiTags.bookmark(second))
        click(UiTags.WEB_GO_LIVE)

        awaitThat { vm.liveUrl.value != null }
        assertTrue(exists(UiTags.bookmarkLive(second)))
        assertFalse(exists(UiTags.bookmarkLive(first)))
    }

    @Test
    fun theLiveMarkerGoesWhenTheScreenIsCleared() = runComposeUiTest {
        // A page still marked live after Clear would say the congregation can
        // see it.
        val vm = viewModel()
        showWeb(vm)
        val first = bookmark(vm, "example.org")
        click(UiTags.WEB_GO_LIVE)
        // Both actions go through the ViewModel's coroutine, so the marker
        // arrives and leaves a frame or more after the tap. Asserting straight
        // after the click passed most of the time and failed under load — and
        // without the first await the test could also pass while never having
        // been live at all.
        awaitThat { exists(UiTags.bookmarkLive(first)) }

        click(UiTags.WEB_CLEAR)

        awaitThat { !exists(UiTags.bookmarkLive(first)) }
    }

    @Test
    fun aBookmarkOffersNoDeleteWhileItIsLive() = runComposeUiTest {
        // Deleting the row the congregation is looking at would leave nothing
        // on screen to clear it from.
        val vm = viewModel()
        showWeb(vm)
        val first = bookmark(vm, "example.org")

        click(UiTags.WEB_GO_LIVE)

        awaitThat { vm.liveUrl.value != null }
        assertFalse(exists(UiTags.bookmarkDelete(first)))
    }
}
