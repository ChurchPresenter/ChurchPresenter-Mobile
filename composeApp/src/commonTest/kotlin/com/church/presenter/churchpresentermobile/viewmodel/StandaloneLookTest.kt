package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideFont
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.showsReference
import com.church.presenter.churchpresentermobile.model.SlideTextAlign
import com.church.presenter.churchpresentermobile.model.SlideVerticalAlign
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    // ── Alignment ────────────────────────────────────────────────────────────

    @Test
    fun theWordsStartCentredOnBothAxes() = runVmTest {
        val (viewModel, _) = vm()

        assertEquals(SlideTextAlign.CENTER, viewModel.theme.value.textAlign)
        assertEquals(SlideVerticalAlign.MIDDLE, viewModel.theme.value.verticalAlign)
    }

    @Test
    fun analignmentChangeReachesTheSlideEveryOutputSees() = runVmTest {
        // The theme travels inside each slide, so this is the one check that
        // covers the phone, an attached screen and a watching browser at once.
        val engine = engine()
        val viewModel = StandaloneViewModel(engine, SinkRegistry(), null)

        viewModel.updateTheme { it.copy(textAlign = SlideTextAlign.LEFT, verticalAlign = SlideVerticalAlign.TOP) }

        assertEquals(SlideTextAlign.LEFT, engine.currentSlide.value.theme.textAlign)
        assertEquals(SlideVerticalAlign.TOP, engine.currentSlide.value.theme.verticalAlign)
    }

    @Test
    fun theChosenAlignmentSurvivesARestart() = runVmTest {
        val settings = AppSettings(InMemorySettingsStorage())
        val (first, _) = vm(settings)
        first.updateTheme { it.copy(textAlign = SlideTextAlign.RIGHT, verticalAlign = SlideVerticalAlign.BOTTOM) }

        // A second ViewModel over the same storage is what a restart looks like.
        val (second, _) = vm(settings)

        assertEquals(SlideTextAlign.RIGHT, second.theme.value.textAlign)
        assertEquals(SlideVerticalAlign.BOTTOM, second.theme.value.verticalAlign)
    }

    @Test
    fun aThemeSavedBeforeAlignmentExistedStillReads() = runVmTest {
        // The stored JSON predates these fields. Defaults must fill them in
        // rather than the whole theme failing to parse and reverting to stock.
        val settings = AppSettings(InMemorySettingsStorage())
        settings.slideThemeJson = """{"font":"SANS","textColor":"#ABCDEF"}"""

        val (viewModel, _) = vm(settings)

        assertEquals(SlideFont.SANS, viewModel.theme.value.font, "the old fields must survive")
        assertEquals(SlideTextAlign.CENTER, viewModel.theme.value.textAlign)
        assertEquals(SlideVerticalAlign.MIDDLE, viewModel.theme.value.verticalAlign)
    }

    // ── Themes and the chord toggle ──────────────────────────────────────────

    @Test
    fun applyingAPresetReplacesTheWholeLook() = runVmTest {
        val settings = AppSettings(InMemorySettingsStorage())
        val engine = engine()
        val viewModel = StandaloneViewModel(engine, SinkRegistry(), settings)
        viewModel.updateTheme { it.copy(textColor = "#123456", font = SlideFont.SANS) }

        val black = viewModel.presets.first { it.name == "Black" }
        viewModel.applyTheme(black)

        assertEquals(black.theme, engine.theme.value, "a preset is the whole look, not a patch")
    }

    @Test
    fun theDefaultsAreAPresetSoPuttingItBackIsATap() = runVmTest {
        val (viewModel, _) = vm()
        assertEquals(SlideTheme(), viewModel.presets.first().theme)
    }

    @Test
    fun aSavedLookComesBackAfterARestart() = runVmTest {
        val settings = AppSettings(InMemorySettingsStorage())
        val first = StandaloneViewModel(engine(), SinkRegistry(), settings)
        first.updateTheme { it.copy(textColor = "#ABCDEF") }
        first.saveCurrentTheme("Evening")

        val second = StandaloneViewModel(engine(), SinkRegistry(), settings)

        assertEquals(listOf("Evening"), second.savedThemes.value.map { it.name })
        assertEquals("#ABCDEF", second.savedThemes.value.single().theme.textColor)
    }

    @Test
    fun deletingASavedLookLeavesTheScreenAlone() = runVmTest {
        // Applying copies the values in, so the live look does not reference the
        // saved entry — deleting one mid-service must not repaint the screen.
        val settings = AppSettings(InMemorySettingsStorage())
        val engine = engine()
        val viewModel = StandaloneViewModel(engine, SinkRegistry(), settings)
        viewModel.updateTheme { it.copy(textColor = "#ABCDEF") }
        viewModel.saveCurrentTheme("Evening")
        viewModel.applyTheme(viewModel.savedThemes.value.single())

        viewModel.deleteSavedTheme("Evening")

        assertEquals("#ABCDEF", engine.theme.value.textColor)
        assertTrue(viewModel.savedThemes.value.isEmpty())
    }

    @Test
    fun savingUnderAnExistingNameReplacesIt() = runVmTest {
        val settings = AppSettings(InMemorySettingsStorage())
        val viewModel = StandaloneViewModel(engine(), SinkRegistry(), settings)
        viewModel.updateTheme { it.copy(textColor = "#111111") }
        viewModel.saveCurrentTheme("Evening")
        viewModel.updateTheme { it.copy(textColor = "#222222") }
        viewModel.saveCurrentTheme("Evening")

        assertEquals(1, viewModel.savedThemes.value.size)
        assertEquals("#222222", viewModel.savedThemes.value.single().theme.textColor)
    }

    @Test
    fun aSavedListFromAnOlderBuildStillReads() = runVmTest {
        // Written before alignment existed. Defaults must fill the gaps rather
        // than the whole list failing to parse and a church losing its looks.
        val settings = AppSettings(InMemorySettingsStorage())
        settings.savedThemesJson =
            """[{"name":"Old","theme":{"font":"SANS","textColor":"#ABCDEF"}}]"""

        val viewModel = StandaloneViewModel(engine(), SinkRegistry(), settings)

        val saved = viewModel.savedThemes.value.single()
        assertEquals("Old", saved.name)
        assertEquals(SlideFont.SANS, saved.theme.font)
        assertEquals(SlideTextAlign.CENTER, saved.theme.textAlign)
    }

    @Test
    fun theChordToggleIsOffUntilAskedForAndThenRemembered() = runVmTest {
        val settings = AppSettings(InMemorySettingsStorage())
        val engine = engine()
        val viewModel = StandaloneViewModel(engine, SinkRegistry(), settings)
        assertEquals(false, viewModel.showChords.value, "the audience's words come first")

        viewModel.setShowChords(true)

        advanceUntilIdle()
        assertEquals(true, viewModel.showChords.value)
        // Part of the theme, so it reaches every output rather than the phone alone.
        assertEquals(true, engine.theme.value.showChords)

        // A second ViewModel over the same storage is what a restart looks like.
        val restarted = StandaloneViewModel(engine(), SinkRegistry(), settings)
        advanceUntilIdle()
        assertEquals(true, restarted.showChords.value, "and it survives a restart")
    }

    @Test
    fun aReferenceTurnedOffBeforeTheSplitStaysOff() = runVmTest {
        // The single showReference flag became three. Without carrying the old
        // value across, a church that had turned headings off would find every
        // one of them back the next Sunday.
        val settings = AppSettings(InMemorySettingsStorage())
        settings.slideThemeJson = """{"textColor":"#FFFFFF","showReference":false}"""

        val (viewModel, _) = vm(settings)

        assertEquals(false, viewModel.theme.value.showSongReference)
        assertEquals(false, viewModel.theme.value.showBibleReference)
        assertEquals(false, viewModel.theme.value.showOtherReference)
    }

    @Test
    fun aReferenceLeftOnBeforeTheSplitStaysOn() = runVmTest {
        val settings = AppSettings(InMemorySettingsStorage())
        settings.slideThemeJson = """{"showReference":true}"""

        val (viewModel, _) = vm(settings)

        assertEquals(true, viewModel.theme.value.showSongReference)
        assertEquals(true, viewModel.theme.value.showBibleReference)
    }

    @Test
    fun songsAndScriptureAreAskedSeparately() = runVmTest {
        // The point of the split: no heading over a hymn, chapter and verse over
        // scripture.
        val theme = SlideTheme(showSongReference = false, showBibleReference = true)

        assertEquals(false, Slide(kind = SlideKind.SONG, theme = theme).showsReference())
        assertEquals(true, Slide(kind = SlideKind.BIBLE, theme = theme).showsReference())
        assertEquals(true, Slide(kind = SlideKind.ANNOUNCEMENT, theme = theme).showsReference())
    }

    // ── Saved looks ──────────────────────────────────────────────────────
    //
    // A church sets its colours once and expects them next Sunday. The store is
    // a JSON blob in settings, written by whatever version of the app last ran,
    // so reading it has to survive anything.

    @Test
    fun `a look saved under a name that already exists replaces it`() = runVmTest {
        // Otherwise "Evening" quietly becomes two entries and the operator picks
        // the stale one.
        val (viewModel, _) = vm()
        viewModel.updateTheme { it.copy(textColor = "#111111") }
        viewModel.saveCurrentTheme("Evening")

        viewModel.updateTheme { it.copy(textColor = "#222222") }
        viewModel.saveCurrentTheme("Evening")

        assertEquals(1, viewModel.savedThemes.value.size)
        assertEquals("#222222", viewModel.savedThemes.value.single().theme.textColor)
    }

    @Test
    fun `a name that is only spaces is not saved`() {
        // The dialog lets an empty field through; a nameless entry in the list
        // could never be picked again.
        val (viewModel, _) = vm()

        viewModel.saveCurrentTheme("   ")

        assertTrue(viewModel.savedThemes.value.isEmpty())
    }

    @Test
    fun `a name is trimmed before it is saved`() {
        val (viewModel, _) = vm()

        viewModel.saveCurrentTheme("  Evening  ")

        assertEquals("Evening", viewModel.savedThemes.value.single().name)
    }

    @Test
    fun `deleting a look that was never saved is harmless`() {
        val (viewModel, _) = vm()
        viewModel.saveCurrentTheme("Evening")

        viewModel.deleteSavedTheme("Morning")

        assertEquals(listOf("Evening"), viewModel.savedThemes.value.map { it.name })
    }

    @Test
    fun `several looks are kept, newest last`() {
        // The order the list is drawn in; reversing it would move the buttons
        // under the operator between services.
        val (viewModel, _) = vm()

        viewModel.saveCurrentTheme("Morning")
        viewModel.saveCurrentTheme("Evening")

        assertEquals(listOf("Morning", "Evening"), viewModel.savedThemes.value.map { it.name })
    }

    @Test
    fun `a saved-looks blob this build cannot read opens as no looks`() {
        // Written by a previous version, or truncated by a kill mid-write. The
        // controller has to open either way — losing the list is recoverable,
        // a crash on the presenting screen is not.
        for (stored in listOf("not json", "{}", """[{"name":1}]""", "")) {
            val settings = AppSettings(InMemorySettingsStorage()).apply { savedThemesJson = stored }

            val (viewModel, _) = vm(settings)

            assertTrue(viewModel.savedThemes.value.isEmpty(), stored)
        }
    }

    @Test
    fun `a look can still be saved after an unreadable blob was found`() {
        // The recovery path: the bad blob is replaced by the next write rather
        // than blocking every later save.
        val settings = AppSettings(InMemorySettingsStorage()).apply { savedThemesJson = "not json" }
        val (viewModel, _) = vm(settings)

        viewModel.saveCurrentTheme("Evening")

        assertEquals(listOf("Evening"), viewModel.savedThemes.value.map { it.name })
    }

    @Test
    fun `deleting a look leaves the screen showing it`() {
        // Applying copies the values in rather than remembering where they came
        // from, so removing the entry must not change what is projected.
        val (viewModel, _) = vm()
        viewModel.updateTheme { it.copy(textColor = "#ABCDEF") }
        viewModel.saveCurrentTheme("Evening")

        viewModel.deleteSavedTheme("Evening")

        assertEquals("#ABCDEF", viewModel.theme.value.textColor)
    }
}
