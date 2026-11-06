package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.tv.FoundService
import com.church.presenter.churchpresentermobile.ui.ModeBadge
import com.church.presenter.churchpresentermobile.ui.UiTags
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.ui.tagged
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Picking a TV from the phone. The failure worth catching is a tap reaching the
 * wrong TV, or a TV that looks pickable with nothing to send it.
 */
@OptIn(ExperimentalTestApi::class)
class TvPickerTest {

    private val livingRoom = FoundService("Living Room", "192.168.1.30", 8771)
    private val lobby = FoundService("Lobby", "192.168.1.31", 8771)

    private fun ComposeUiTest.showPicker(
        tvs: List<FoundService> = listOf(livingRoom, lobby),
        canSend: Boolean = true,
        showingOn: String? = null,
        failedOn: String? = null,
        onSend: (FoundService) -> Unit = {},
        onStop: (FoundService) -> Unit = {},
    ) = showScreen { TvPickerContent(tvs, canSend, showingOn, failedOn, onSend, onStop) }

    @Test
    fun withNoTvsItSaysHowToGetOne() = runComposeUiTest {
        showPicker(tvs = emptyList())

        assertTrue(exists(StandaloneTags.TVS_EMPTY))
    }

    @Test
    fun tappingATvSendsToThatOne() = runComposeUiTest {
        var sent: FoundService? = null
        showPicker(onSend = { sent = it })

        click(StandaloneTags.tv("Lobby"))

        assertEquals(lobby, sent)
    }

    @Test
    fun withNothingToSendTheTvsCannotBePicked() = runComposeUiTest {
        showPicker(canSend = false)

        tagged(StandaloneTags.tv("Lobby")).assertIsNotEnabled()
        assertTrue(exists(StandaloneTags.TVS_NEED_SOURCE))
    }

    @Test
    fun theTvItIsShowingOnOffersStop() = runComposeUiTest {
        var stopped: FoundService? = null
        showPicker(showingOn = "Lobby", onStop = { stopped = it })

        click(StandaloneTags.tvStop("Lobby"))

        assertEquals(lobby, stopped)
        assertFalse(exists(StandaloneTags.tvStop("Living Room")))
    }

    @Test
    fun aTvThatCouldNotBeReachedSaysSo() = runComposeUiTest {
        showPicker(failedOn = "Lobby")

        assertTrue(exists(StandaloneTags.tvFailed("Lobby")))
        assertFalse(exists(StandaloneTags.tvFailed("Living Room")))
        tagged(StandaloneTags.tv("Lobby")).assertIsEnabled()
    }

    @Test
    fun theHeaderNamesTheMode() = runComposeUiTest {
        showScreen { ModeBadge(AppMode.STANDALONE) }

        assertTrue(exists(UiTags.HEADER_MODE))
    }
}
