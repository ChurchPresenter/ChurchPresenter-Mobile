package com.church.presenter.churchpresentermobile.ui.tv

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.tv.DesktopOutput
import com.church.presenter.churchpresentermobile.tv.SourceKind
import com.church.presenter.churchpresentermobile.tv.TvSource
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.ui.tagged
import kotlinx.coroutines.awaitCancellation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Choosing one of a desktop's outputs with a remote. The pick has to carry the
 * right number and the key: a wrong number shows the wrong screen in the room,
 * and a missing key shows nothing at all.
 */
@OptIn(ExperimentalTestApi::class)
class TvOutputPickerTest {

    private val desktop = TvSource("Main hall", "192.168.1.10", 8765, SourceKind.DESKTOP)

    private fun ComposeUiTest.showPicker(
        onPick: (TvSource) -> Unit = {},
        find: suspend () -> List<DesktopOutput>,
    ) = showScreen { OutputPicker(desktop, onPick, find = { _, _ -> find() }) }

    @Test
    fun whileItAsksItShowsWhichComputerAndNoVerdict() = runComposeUiTest {
        showPicker { awaitCancellation() }

        assertTrue(isShowing("Main hall"))
        assertFalse(exists(TvTags.NO_OUTPUTS))
    }

    @Test
    fun aSingleOpenOutputGoesStraightOn() = runComposeUiTest {
        var picked: TvSource? = null

        showPicker(onPick = { picked = it }) { listOf(DesktopOutput(2, needsKey = false)) }

        awaitThat { picked != null }
        assertEquals(2, picked?.output)
    }

    @Test
    fun aSingleOutputBehindAKeyWaitsForTheKey() = runComposeUiTest {
        var picked: TvSource? = null

        showPicker(onPick = { picked = it }) { listOf(DesktopOutput(1, needsKey = true)) }

        awaitThat { exists(TvTags.OUTPUT_KEY) }
        assertTrue(isShowing("Needs the API key"))
        assertNull(picked)
    }

    @Test
    fun theKeyTypedIsCarriedWithThePick() = runComposeUiTest {
        var picked: TvSource? = null
        showPicker(onPick = { picked = it }) { listOf(DesktopOutput(1, needsKey = true)) }
        awaitThat { exists(TvTags.OUTPUT_KEY) }

        tagged(TvTags.OUTPUT_KEY).performTextReplacement("secret")
        click(TvTags.output(1))

        awaitThat { picked != null }
        assertEquals("secret", picked?.apiKey)
        assertEquals(1, picked?.output)
    }

    @Test
    fun severalOutputsAreOfferedAndThePickCarriesTheOneChosen() = runComposeUiTest {
        var picked: TvSource? = null
        showPicker(onPick = { picked = it }) {
            listOf(DesktopOutput(1, needsKey = false), DesktopOutput(3, needsKey = false))
        }
        awaitThat { exists(TvTags.output(3)) }

        assertTrue(isShowing("Output 1"))
        assertFalse(exists(TvTags.OUTPUT_KEY), "nothing here needs a key")
        click(TvTags.output(3))

        awaitThat { picked != null }
        assertEquals(3, picked?.output)
    }

    @Test
    fun aComputerWithNoOutputSaysWhatToDoAndCanBeAskedAgain() = runComposeUiTest {
        var asked = 0
        showPicker {
            asked++
            if (asked == 1) emptyList() else listOf(DesktopOutput(1, false), DesktopOutput(2, false))
        }
        awaitThat { exists(TvTags.NO_OUTPUTS) }

        click(TvTags.OUTPUTS_RETRY)

        awaitThat { exists(TvTags.output(2)) }
        assertEquals(2, asked)
        assertFalse(exists(TvTags.NO_OUTPUTS))
    }
}
