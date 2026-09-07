package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasText
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.FakeDeckDesktop
import com.church.presenter.churchpresentermobile.ui.PresentationScreen
import com.church.presenter.churchpresentermobile.ui.deck
import com.church.presenter.churchpresentermobile.ui.offlineImageLoader
import io.ktor.http.HttpStatusCode
import kotlin.test.Test

/**
 * The presentation decks the desktop has open.
 *
 * Driven by `FakeDeckDesktop`, the same stand-in computer the behavioural deck
 * tests use, so the list is filled by a real request over a mock engine into a
 * real `PresentationsViewModel`.
 */
class PresentationScreenshotTest {

    private fun presentations(
        desktop: FakeDeckDesktop,
        canUploadFiles: Boolean = true,
    ): @Composable () -> Unit {
        val viewModel = desktop.viewModel()
        return {
            PresentationScreen(
                appSettings = AppSettings(InMemorySettingsStorage()),
                settingsSaveToken = 0,
                imageLoader = offlineImageLoader(),
                canUploadFiles = canUploadFiles,
                providedViewModel = viewModel,
            )
        }
    }

    /** The deck list has arrived when the first deck's name is on screen. */
    private fun hasDeckNamed(name: String): (androidx.compose.ui.test.ComposeUiTest) -> Boolean =
        { it.onAllNodes(hasText(name, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun deckList() = screenshot(
        "presentations__list",
        until = { hasDeckNamed("Notices")(this) },
        content = presentations(FakeDeckDesktop()),
    )

    @Test
    fun singleDeck() = screenshot(
        "presentations__single-deck",
        until = { hasDeckNamed("Sermon")(this) },
        content = presentations(
            FakeDeckDesktop(decks = listOf(deck(id = "sermon", fileName = "Sermon.pptx", slides = 12))),
        ),
    )

    @Test
    fun noDecks() = screenshot(
        "presentations__empty",
        content = presentations(FakeDeckDesktop(decks = emptyList())),
    )

    @Test
    fun desktopRefusedTheList() = screenshot(
        "presentations__error",
        content = presentations(
            FakeDeckDesktop(decks = emptyList(), listStatus = HttpStatusCode.InternalServerError),
        ),
    )

    @Test
    fun uploadsUnavailable() = screenshot(
        // An older desktop with no upload endpoint — the control is withheld
        // rather than offered and failing.
        "presentations__uploads-unavailable",
        until = { hasDeckNamed("Notices")(this) },
        content = presentations(FakeDeckDesktop(), canUploadFiles = false),
    )
}
