package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.SlideFont
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.ui.standalone.normaliseHex
import com.church.presenter.churchpresentermobile.ui.standalone.parseHexColorOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The look of the audience screen: what the operator can change, and what survives a restart.
 *
 * The gradient used to be three constants compiled into two different renderers, so a church
 * could not change it at all.
 */
class StandaloneLookTest {

    private fun engine() = StandaloneEngine(
        mode = MutableStateFlow(AppMode.STANDALONE),
        registry = SinkRegistry(),
        publish = {},
    )

    private fun vm(settings: AppSettings = AppSettings(InMemorySettingsStorage())) =
        StandaloneViewModel(engine(), SinkRegistry(), settings) to settings

    @Test
    fun theGradientStartsOnTheLookTheAppAlwaysHad() = runVmTest {
        val (viewModel, _) = vm()

        assertEquals("#2A1D5E", viewModel.theme.value.gradientTop)
        assertEquals("#05060D", viewModel.theme.value.gradientBottom)
    }

    @Test
    fun aChosenGradientReachesTheProjectedSlide() = runVmTest {
        // The theme rides inside every slide, which is how one edit reaches the phone's output,
        // an attached screen and a browser at once.
        val standaloneEngine = engine()
        val viewModel = StandaloneViewModel(standaloneEngine, SinkRegistry(), null)

        viewModel.updateTheme { it.copy(gradientTop = "#123456", gradientBottom = "#000000") }

        assertEquals("#123456", standaloneEngine.currentSlide.value.theme.gradientTop)
        assertEquals("#000000", standaloneEngine.currentSlide.value.theme.gradientBottom)
    }

    @Test
    fun changingOnePartOfTheLookLeavesTheRest() = runVmTest {
        val (viewModel, _) = vm()

        viewModel.updateTheme { it.copy(gradientTop = "#123456") }
        viewModel.updateTheme { it.copy(font = SlideFont.SANS) }

        assertEquals("#123456", viewModel.theme.value.gradientTop)
        assertEquals(SlideFont.SANS, viewModel.theme.value.font)
    }

    @Test
    fun aChosenLookSurvivesTheAppBeingClosed() = runVmTest {
        val storage = InMemorySettingsStorage()
        val first = AppSettings(storage)
        StandaloneViewModel(engine(), SinkRegistry(), first)
            .updateTheme { it.copy(gradientTop = "#123456", brandLine = "Grace Chapel") }

        // A fresh launch, reading the same storage.
        val reopened = StandaloneViewModel(engine(), SinkRegistry(), AppSettings(storage))

        assertEquals("#123456", reopened.theme.value.gradientTop)
        assertEquals("Grace Chapel", reopened.theme.value.brandLine)
    }

    @Test
    fun aStoredLookThatCannotBeReadFallsBackRatherThanCrashing() = runVmTest {
        val storage = InMemorySettingsStorage()
        AppSettings(storage).slideThemeJson = "{ not json"

        val viewModel = StandaloneViewModel(engine(), SinkRegistry(), AppSettings(storage))

        assertEquals(SlideTheme().gradientTop, viewModel.theme.value.gradientTop)
    }

    @Test
    fun resettingReturnsEveryPartOfTheLook() = runVmTest {
        val (viewModel, _) = vm()
        viewModel.updateTheme { it.copy(gradientTop = "#123456", textColor = "#FF0000") }

        viewModel.updateTheme { SlideTheme() }

        assertEquals(SlideTheme(), viewModel.theme.value)
    }

    @Test
    fun aBlankChurchNameMeansNoCornerLine() = runVmTest {
        val (viewModel, _) = vm()

        viewModel.updateTheme { it.copy(brandLine = null) }

        assertNull(viewModel.theme.value.brandLine)
    }

    @Test
    fun sixDigitsAndThreeDigitsBothParse() {
        assertNotNull(parseHexColorOrNull("#2A1D5E"))
        assertNotNull(parseHexColorOrNull("2A1D5E"))
        assertNotNull(parseHexColorOrNull("#abc"))
    }

    @Test
    fun aHalfTypedColourIsNotAColourYet() {
        // The field keeps showing it; nothing is published until it parses.
        assertNull(parseHexColorOrNull("#2A1D"))
        assertNull(parseHexColorOrNull("#"))
        assertNull(parseHexColorOrNull("#GGGGGG"))
        assertNull(parseHexColorOrNull(""))
    }

    @Test
    fun whatIsStoredIsAlwaysTheSameShape() {
        // Every renderer reads these, so "abc", "#ABC" and "aabbcc" must not reach them as
        // three different things.
        assertEquals("#AABBCC", normaliseHex("abc"))
        assertEquals("#AABBCC", normaliseHex("#AABBCC"))
        assertEquals("#2A1D5E", normaliseHex(" 2a1d5e "))
    }
}
