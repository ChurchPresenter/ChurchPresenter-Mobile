package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Going live, queueing, and clearing a web page.
 */
@OptIn(ExperimentalTestApi::class)
class WebActionsTest {

    @Test
    fun anEmptyAddressIsNotSent() = runComposeUiTest {
        // Projecting nothing would blank the audience screen without saying so.
        val sender = FakeWsSender()
        val vm = viewModel(sender)
        showWeb(vm)

        click(UiTags.WEB_GO_LIVE)

        assertTrue(sender.calls.isEmpty())
    }

    @Test
    fun addingToTheScheduleSendsItDownTheScheduleRoute() = runComposeUiTest {
        // The same page, queued rather than projected.
        val sender = FakeWsSender()
        val vm = viewModel(sender)
        showWeb(vm)

        type(UiTags.WEB_URL, "example.org")
        click(UiTags.WEB_ADD_TO_SCHEDULE)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.ADD_TO_SCHEDULE, sender.lastType)
    }

    @Test
    fun clearingTellsTheDesktopToBlank() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = viewModel(sender)
        showWeb(vm)

        click(UiTags.WEB_CLEAR)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.CLEAR, sender.lastType)
    }

    @Test
    fun clearingForgetsWhatWasLive() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)
        type(UiTags.WEB_URL, "example.org")
        click(UiTags.WEB_GO_LIVE)

        click(UiTags.WEB_CLEAR)

        awaitThat { vm.liveUrl.value == null }
        assertEquals(null, vm.liveUrl.value)
    }

    // ── Bookmarks ────────────────────────────────────────────────────────
}
