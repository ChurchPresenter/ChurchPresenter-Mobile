package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import kotlin.test.Test
import kotlin.test.assertEquals
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
 * Projecting a web page — a hymn video, a giving page, a slideshow built
 * elsewhere.
 *
 * Typed on a phone keyboard, "https://" is the first thing left off, and the
 * desktop cannot open a bare host.
 */
@OptIn(ExperimentalTestApi::class)
class WebProjectionTest {
    @Test
    fun theUrlBarStartsEmpty() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)

        assertEquals("", vm.url.value)
    }

    @Test
    fun typingAnAddressIsKept() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)

        type(UiTags.WEB_URL, "example.org/giving")

        assertEquals("example.org/giving", vm.url.value)
    }

    @Test
    fun theAddressIsShownBackToTheOperator() = runComposeUiTest {
        val vm = viewModel()
        showWeb(vm)

        type(UiTags.WEB_URL, "example.org/giving")

        assertTrue(isShowing("example.org/giving"))
    }

    // ── Going live ───────────────────────────────────────────────────────

    @Test
    fun goingLiveSendsThePageToTheDesktop() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = viewModel(sender)
        showWeb(vm)

        type(UiTags.WEB_URL, "example.org")
        click(UiTags.WEB_GO_LIVE)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.PROJECT, sender.lastType)
    }

    @Test
    fun aTypedAddressIsSentWithItsScheme() = runComposeUiTest {
        // Typed on a phone keyboard, "https://" is the first thing left off, and
        // the desktop cannot open a bare host.
        val sender = FakeWsSender()
        val vm = viewModel(sender)
        showWeb(vm)

        type(UiTags.WEB_URL, "example.org/giving")
        click(UiTags.WEB_GO_LIVE)

        awaitThat { sender.calls.isNotEmpty() }
        assertTrue("https://example.org/giving" in sender.lastPayload, sender.lastPayload)
    }

    @Test
    fun anAddressThatAlreadyHasASchemeIsLeftAlone() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = viewModel(sender)
        showWeb(vm)

        type(UiTags.WEB_URL, "http://192.168.1.5:8080/slides")
        click(UiTags.WEB_GO_LIVE)

        awaitThat { sender.calls.isNotEmpty() }
        assertTrue("http://192.168.1.5:8080/slides" in sender.lastPayload, sender.lastPayload)
    }
}
