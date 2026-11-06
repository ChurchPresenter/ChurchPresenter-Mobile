package com.church.presenter.churchpresentermobile.ui.tv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.tv.LiveState
import com.church.presenter.churchpresentermobile.tv.SourceKind
import com.church.presenter.churchpresentermobile.tv.TvSource
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The info card held OK brings up, and what the live screen does with a desktop's
 * picture. The card is for someone in front of the TV wondering why it is not
 * showing what they expect, so each state has to say which one it is.
 */
@OptIn(ExperimentalTestApi::class)
class TvInfoOverlayTest {

    private val phone = TvSource("Pixel", "192.168.1.20", 8766)
    private val desktop = TvSource("Main hall", "192.168.1.10", 8765, SourceKind.DESKTOP, output = 2)
    private val picture = ImageBitmap(2, 2)

    @Test
    fun itSaysWhichPhoneAndWhere() = runComposeUiTest {
        showScreen { TvInfoOverlay(phone, LiveState.Connecting) }

        assertTrue(exists(TvTags.INFO))
        assertTrue(isShowing("Pixel"))
        assertTrue(isShowing("Phone"))
        assertTrue(isShowing("192.168.1.20:8766"))
    }

    @Test
    fun aDesktopSaysWhichOutput() = runComposeUiTest {
        showScreen { TvInfoOverlay(desktop, LiveState.Picture(picture, version = 1)) }

        assertTrue(isShowing("Computer"))
        assertTrue(isShowing("Output 2"))
        assertTrue(isShowing("Showing"))
    }

    @Test
    fun everyStateIsNamed() = runComposeUiTest {
        var state by mutableStateOf<LiveState>(LiveState.Connecting)
        showScreen { TvInfoOverlay(phone, state) }

        listOf(
            LiveState.Connecting to "Connecting to Pixel",
            LiveState.Showing(Slide(body = "Verse one")) to "Showing",
            LiveState.Lost(Slide(body = "Verse one")) to "Reconnecting to Pixel",
            LiveState.Ended to "The presentation has ended",
            LiveState.TooNew to "Update ChurchPresenter on this TV",
        ).forEach { (next, words) ->
            state = next
            waitForIdle()
            assertTrue(isShowing(words), "$next")
        }
    }

    @Test
    fun onlyTheRemotesOkKeysCountAsOk() {
        assertTrue(Key.DirectionCenter in OK_KEYS)
        assertTrue(Key.Enter in OK_KEYS)
        assertFalse(Key.Back in OK_KEYS)
    }

    // ── A desktop's picture on the live screen ───────────────────────────

    @Test
    fun aPictureKeepsItsImageAndVersion() {
        val state = LiveState.Picture(picture, version = 3)

        assertSame(picture, state.image)
        assertEquals(3, state.version)
    }

    @Test
    fun aDesktopsPictureIsDrawnWithNoMessageOverIt() = runComposeUiTest {
        showScreen { TvLiveContent(LiveState.Picture(picture, version = 1), "Main hall") }

        assertTrue(exists(TvTags.LIVE))
        assertFalse(isShowing("Connecting to"))
    }

    @Test
    fun aLostDesktopKeepsItsLastPictureUp() = runComposeUiTest {
        showScreen { TvLiveContent(LiveState.Lost(lastSlide = null, lastPicture = picture), "Main hall") }

        assertTrue(exists(TvTags.LIVE))
        assertFalse(isShowing("Connecting to"), "the room keeps the picture, not a message")
    }

    @Test
    fun aDropWithNothingToKeepUpSaysItIsConnecting() = runComposeUiTest {
        showScreen { TvLiveContent(LiveState.Lost(lastSlide = null), "Main hall") }

        assertTrue(isShowing("Connecting to Main hall"))
    }
}
