package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.SlideFont
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.present.PhotoLibrary
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.ui.standalone.normaliseHex
import com.church.presenter.churchpresentermobile.ui.standalone.parseHexColorOrNull
import com.church.presenter.churchpresentermobile.ui.standalone.rgbToHex
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
    fun theSlidersProduceTheHexTheyLookLike() {
        assertEquals("#000000", rgbToHex(0, 0, 0))
        assertEquals("#FFFFFF", rgbToHex(255, 255, 255))
        assertEquals("#2A1D5E", rgbToHex(42, 29, 94))
        // Single-digit channels must not lose their leading zero — "#F0F0F" is not a colour.
        assertEquals("#0F0F0F", rgbToHex(15, 15, 15))
    }

    @Test
    fun aColourSurvivesTheTripThroughTheSliders() {
        // What the picker opens on must be what the field held, or every open nudges the colour.
        val original = "#2A1D5E"
        val parsed = assertNotNull(parseHexColorOrNull(original))

        val roundTripped = rgbToHex(
            (parsed.red * 255).roundToInt(),
            (parsed.green * 255).roundToInt(),
            (parsed.blue * 255).roundToInt(),
        )

        assertEquals(original, roundTripped)
    }

    @Test
    fun aChannelPushedOutOfRangeIsClamped() {
        assertEquals("#FF0000", rgbToHex(300, -20, 0))
    }

    @Test
    fun whatIsStoredIsAlwaysTheSameShape() {
        // Every renderer reads these, so "abc", "#ABC" and "aabbcc" must not reach them as
        // three different things.
        assertEquals("#AABBCC", normaliseHex("abc"))
        assertEquals("#AABBCC", normaliseHex("#AABBCC"))
        assertEquals("#2A1D5E", normaliseHex(" 2a1d5e "))
    }

    // ── Image backdrop ───────────────────────────────────────────────────────

    private fun photoLibrary(vararg names: String): PhotoLibrary {
        var n = 0
        val library = PhotoLibrary(newId = { "id-${n++}" })
        names.forEach { library.add(it, byteArrayOf(1, 2, 3)) }
        return library
    }

    @Test
    fun pickingAPhotoPutsItBehindTheWords() = runVmTest {
        // The Image option used to set the kind and nothing else: no call site
        // ever passed a URL, so the audience page fell back to the gradient and
        // the button looked dead.
        val library = photoLibrary("banner.jpg")
        library.serveFrom("http://192.168.1.50:8080")
        val engine = engine()
        val viewModel = StandaloneViewModel(engine, SinkRegistry(), null, library)

        viewModel.setImageBackdrop(library.photos.value.single())

        assertEquals(SlideBackdrop.IMAGE, engine.backdrop.value)
        assertEquals("http://192.168.1.50:8080/photo/id-0", engine.backdropUrl.value)
    }

    @Test
    fun aPhotoWithNoAddressYetIsNotProjected() = runVmTest {
        // Photos are served by the embedded web server, so until it is running
        // there is no address to send. Setting IMAGE with a null URL is exactly
        // the state that made this look broken, so it must not be entered.
        val library = photoLibrary("banner.jpg")
        val engine = engine()
        val viewModel = StandaloneViewModel(engine, SinkRegistry(), null, library)

        viewModel.setImageBackdrop(library.photos.value.single())

        assertEquals(SlideBackdrop.GRADIENT, engine.backdrop.value, "the backdrop must not change")
        assertNull(engine.backdropUrl.value)
    }

    @Test
    fun theChosenPhotoSurvivesASwitchToGradientAndBack() = runVmTest {
        val library = photoLibrary("banner.jpg")
        library.serveFrom("http://192.168.1.50:8080")
        val engine = engine()
        val viewModel = StandaloneViewModel(engine, SinkRegistry(), null, library)
        viewModel.setImageBackdrop(library.photos.value.single())

        viewModel.setBackdrop(SlideBackdrop.GRADIENT)
        viewModel.setBackdrop(SlideBackdrop.IMAGE)

        assertEquals("http://192.168.1.50:8080/photo/id-0", engine.backdropUrl.value,
            "switching away and back must not lose the photo")
    }

    @Test
    fun aPhotoBackdropIsOfferedOnlyOnceTheServerIsUp() = runVmTest {
        val library = photoLibrary("banner.jpg")
        val viewModel = StandaloneViewModel(engine(), SinkRegistry(), null, library)
        assertEquals(false, viewModel.canUsePhotoBackdrop.value)

        library.serveFrom("http://192.168.1.50:8080")
        assertEquals(true, viewModel.canUsePhotoBackdrop.first { it })
    }

    @Test
    fun withNoPhotoLibraryTheChoiceIsSimplyAbsent() = runVmTest {
        val viewModel = StandaloneViewModel(engine(), SinkRegistry(), null, null)

        assertEquals(emptyList(), viewModel.backdropPhotos.value)
        assertEquals(false, viewModel.canUsePhotoBackdrop.value)
    }
}
