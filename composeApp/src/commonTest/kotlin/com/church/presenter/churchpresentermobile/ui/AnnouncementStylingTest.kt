package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AnnouncementAnimation
import com.church.presenter.churchpresentermobile.model.AnnouncementType
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.viewmodel.AnnouncementsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The half of the announcement composer that decides how a notice *looks* —
 * the timers, the colours, the size and the animation.
 *
 * The timer types are the part worth pinning hardest: each one asks for a
 * different set of numbers, and a stepper that runs past its limit sends the
 * desktop a countdown of 60 minutes or −1 seconds. The preview is the operator's
 * only sight of the result before a room full of people gets it.
 */
@OptIn(ExperimentalTestApi::class)
class AnnouncementStylingTest {

    private fun vm(): AnnouncementsViewModel =
        AnnouncementsViewModel(AppSettings(InMemorySettingsStorage()), FakeWsSender())

    private fun ComposeUiTest.showComposer(viewModel: AnnouncementsViewModel) = showScreen {
        AnnouncementsScreen(viewModel = viewModel)
    }

    private fun ComposeUiTest.chooseType(type: AnnouncementType) =
        click(UiTags.announceType(type.name))

    // ── Which fields each kind of notice asks for ────────────────────────

    @Test
    fun aTextNoticeAsksForWords() = runComposeUiTest {
        showComposer(vm())

        assertTrue(exists(UiTags.ANNOUNCE_TEXT))
    }

    @Test
    fun aTextNoticeAsksForNoTimes() = runComposeUiTest {
        showComposer(vm())

        assertFalse(exists(UiTags.ANNOUNCE_COUNTDOWN_FIELDS))
        assertFalse(exists(UiTags.ANNOUNCE_UNTIL_FIELDS))
    }

    @Test
    fun aCountdownAsksForADuration() = runComposeUiTest {
        showComposer(vm())

        chooseType(AnnouncementType.COUNTDOWN)

        assertTrue(exists(UiTags.ANNOUNCE_COUNTDOWN_FIELDS))
    }

    @Test
    fun aCountdownAsksForHoursMinutesAndSeconds() = runComposeUiTest {
        showComposer(vm())

        chooseType(AnnouncementType.COUNTDOWN)

        assertTrue(exists(UiTags.ANNOUNCE_HOURS))
        assertTrue(exists(UiTags.ANNOUNCE_MINUTES))
        assertTrue(exists(UiTags.ANNOUNCE_SECONDS))
    }

    @Test
    fun aCountdownAsksForNoWords() = runComposeUiTest {
        showComposer(vm())

        chooseType(AnnouncementType.COUNTDOWN)

        assertFalse(exists(UiTags.ANNOUNCE_TEXT))
    }

    @Test
    fun aCountdownToATimeAsksForThatTime() = runComposeUiTest {
        showComposer(vm())

        chooseType(AnnouncementType.COUNTDOWN_TO_TIME)

        assertTrue(exists(UiTags.ANNOUNCE_UNTIL_FIELDS))
    }

    @Test
    fun aCountdownToATimeAsksForAnHourAndAMinute() = runComposeUiTest {
        showComposer(vm())

        chooseType(AnnouncementType.COUNTDOWN_TO_TIME)

        assertTrue(exists(UiTags.ANNOUNCE_TARGET_HOUR))
        assertTrue(exists(UiTags.ANNOUNCE_TARGET_MINUTE))
    }

    @Test
    fun aCountdownToATimeAsksForNoDuration() = runComposeUiTest {
        // The two are different questions; showing both would let the operator
        // fill in one and have the other sent.
        showComposer(vm())

        chooseType(AnnouncementType.COUNTDOWN_TO_TIME)

        assertFalse(exists(UiTags.ANNOUNCE_COUNTDOWN_FIELDS))
    }

    @Test
    fun aClockAsksForNothingButExplainsItself() = runComposeUiTest {
        showComposer(vm())

        chooseType(AnnouncementType.CLOCK)

        assertTrue(exists(UiTags.ANNOUNCE_TIMER_DESC))
        assertFalse(exists(UiTags.ANNOUNCE_TEXT))
    }

    @Test
    fun aCountUpAsksForNothingButExplainsItself() = runComposeUiTest {
        showComposer(vm())

        chooseType(AnnouncementType.COUNT_UP)

        assertTrue(exists(UiTags.ANNOUNCE_TIMER_DESC))
    }

    @Test
    fun aClockAsksForNoTimes() = runComposeUiTest {
        showComposer(vm())

        chooseType(AnnouncementType.CLOCK)

        assertFalse(exists(UiTags.ANNOUNCE_COUNTDOWN_FIELDS))
        assertFalse(exists(UiTags.ANNOUNCE_UNTIL_FIELDS))
    }

    @Test
    fun goingBackToTextAsksForWordsAgain() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN)

        chooseType(AnnouncementType.TEXT)

        assertTrue(exists(UiTags.ANNOUNCE_TEXT))
    }

    @Test
    fun everyKindOfNoticeIsOffered() = runComposeUiTest {
        showComposer(vm())

        AnnouncementType.entries.forEach { type ->
            assertTrue(exists(UiTags.announceType(type.name)), "missing ${type.name}")
        }
    }

    // ── The steppers ─────────────────────────────────────────────────────

    @Test
    fun aCountdownOpensOnFiveMinutes() = runComposeUiTest {
        // The default a service actually uses; starting at zero would mean
        // every countdown began with the operator tapping.
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN)

        assertEquals(0, viewModel.form.value.hours)
        assertEquals(5, viewModel.form.value.minutes)
    }

    @Test
    fun addingMinutesRaisesTheCountdown() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN)

        val before = viewModel.form.value.minutes
        click(UiTags.stepperUp(UiTags.ANNOUNCE_MINUTES))

        assertEquals(before + 1, viewModel.form.value.minutes)
    }

    @Test
    fun addingMinutesTwiceRaisesItTwice() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN)

        val before = viewModel.form.value.minutes
        click(UiTags.stepperUp(UiTags.ANNOUNCE_MINUTES))
        click(UiTags.stepperUp(UiTags.ANNOUNCE_MINUTES))

        assertEquals(before + 2, viewModel.form.value.minutes)
    }

    @Test
    fun takingMinutesOffLowersTheCountdown() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN)
        val before = viewModel.form.value.minutes
        click(UiTags.stepperUp(UiTags.ANNOUNCE_MINUTES))
        click(UiTags.stepperUp(UiTags.ANNOUNCE_MINUTES))

        click(UiTags.stepperDown(UiTags.ANNOUNCE_MINUTES))

        assertEquals(before + 1, viewModel.form.value.minutes)
    }

    @Test
    fun aCountdownNeverGoesBelowZero() = runComposeUiTest {
        // A negative duration is not a countdown the desktop can run.
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN)
        repeat(10) { click(UiTags.stepperDown(UiTags.ANNOUNCE_MINUTES)) }

        assertEquals(0, viewModel.form.value.minutes)
    }

    @Test
    fun secondsStopAtFiftyNine() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN)
        repeat(62) { click(UiTags.stepperUp(UiTags.ANNOUNCE_SECONDS)) }

        assertEquals(59, viewModel.form.value.seconds)
    }

    @Test
    fun hoursStopAtTwentyThree() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN)
        repeat(26) { click(UiTags.stepperUp(UiTags.ANNOUNCE_HOURS)) }

        assertEquals(23, viewModel.form.value.hours)
    }

    @Test
    fun theStepperShowsTheNumberItHolds() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN)

        repeat(3) { click(UiTags.stepperUp(UiTags.ANNOUNCE_MINUTES)) }

        assertTrue(exists(UiTags.stepperValue(UiTags.ANNOUNCE_MINUTES)))
        assertTrue(isShowing(viewModel.form.value.minutes.toString()))
    }

    @Test
    fun eachStepperMovesOnlyItsOwnNumber() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN)

        val minutes = viewModel.form.value.minutes
        val seconds = viewModel.form.value.seconds
        click(UiTags.stepperUp(UiTags.ANNOUNCE_HOURS))

        assertEquals(1, viewModel.form.value.hours)
        assertEquals(minutes, viewModel.form.value.minutes)
        assertEquals(seconds, viewModel.form.value.seconds)
    }

    @Test
    fun theTargetHourStopsAtTwentyThree() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN_TO_TIME)
        repeat(30) { click(UiTags.stepperUp(UiTags.ANNOUNCE_TARGET_HOUR)) }

        assertEquals(23, viewModel.form.value.targetHour)
    }

    @Test
    fun theTargetMinuteStopsAtFiftyNine() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN_TO_TIME)
        repeat(65) { click(UiTags.stepperUp(UiTags.ANNOUNCE_TARGET_MINUTE)) }

        assertEquals(59, viewModel.form.value.targetMinute)
    }

    @Test
    fun theTargetTimeNeverGoesBelowMidnight() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN_TO_TIME)
        repeat(20) { click(UiTags.stepperDown(UiTags.ANNOUNCE_TARGET_HOUR)) }

        assertEquals(0, viewModel.form.value.targetHour)
    }

    // ── Font size ────────────────────────────────────────────────────────

    @Test
    fun theFontSizeStepperIsOffered() = runComposeUiTest {
        showComposer(vm())

        assertTrue(exists(UiTags.ANNOUNCE_FONT_SIZE))
    }

    @Test
    fun theFontSizeGoesUpInStepsOfFour() = runComposeUiTest {
        // Four keeps the ladder short enough to walk with a thumb.
        val viewModel = vm()
        showComposer(viewModel)
        val before = viewModel.form.value.fontSize

        click(UiTags.stepperUp(UiTags.ANNOUNCE_FONT_SIZE))

        assertEquals(before + 4, viewModel.form.value.fontSize)
    }

    @Test
    fun theFontSizeGoesDownInStepsOfFour() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        val before = viewModel.form.value.fontSize

        click(UiTags.stepperDown(UiTags.ANNOUNCE_FONT_SIZE))

        assertEquals(before - 4, viewModel.form.value.fontSize)
    }

    @Test
    fun theFontSizeStopsAtOneSixty() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        repeat(60) { click(UiTags.stepperUp(UiTags.ANNOUNCE_FONT_SIZE)) }

        assertEquals(160, viewModel.form.value.fontSize)
    }

    @Test
    fun theFontSizeStopsAtSixteen() = runComposeUiTest {
        // Below this nobody at the back can read it.
        val viewModel = vm()
        showComposer(viewModel)
        repeat(60) { click(UiTags.stepperDown(UiTags.ANNOUNCE_FONT_SIZE)) }

        assertEquals(16, viewModel.form.value.fontSize)
    }

    // ── Colours ──────────────────────────────────────────────────────────

    @Test
    fun theTextColourSwatchesAreOffered() = runComposeUiTest {
        showComposer(vm())

        assertTrue(exists(UiTags.announceCustomSwatch("text")))
    }

    @Test
    fun theBackgroundColourSwatchesAreOffered() = runComposeUiTest {
        showComposer(vm())

        assertTrue(exists(UiTags.announceCustomSwatch("background")))
    }

    @Test
    fun pickingATextColourKeepsIt() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        val swatch = TEXT_SWATCHES.last()

        click(UiTags.announceSwatch("text", swatch))

        assertEquals(swatch, viewModel.form.value.textColor)
    }

    @Test
    fun pickingABackgroundColourKeepsIt() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        val swatch = BG_SWATCHES.last()

        click(UiTags.announceSwatch("background", swatch))

        assertEquals(swatch, viewModel.form.value.backgroundColor)
    }

    @Test
    fun aTextColourDoesNotChangeTheBackground() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        val before = viewModel.form.value.backgroundColor

        click(UiTags.announceSwatch("text", TEXT_SWATCHES.last()))

        assertEquals(before, viewModel.form.value.backgroundColor)
    }

    @Test
    fun noColourPickerIsOpenToBeginWith() = runComposeUiTest {
        showComposer(vm())

        assertFalse(exists(UiTags.COLOR_PICKER))
    }

    @Test
    fun theCustomTileOpensTheColourPicker() = runComposeUiTest {
        showComposer(vm())

        click(UiTags.announceCustomSwatch("text"))

        assertTrue(exists(UiTags.COLOR_PICKER))
    }

    @Test
    fun theColourPickerShowsTheColourItWouldUse() = runComposeUiTest {
        showComposer(vm())

        click(UiTags.announceCustomSwatch("text"))

        assertTrue(exists(UiTags.COLOR_PICKER_HEX))
    }

    @Test
    fun theColourPickerOffersThreeSliders() = runComposeUiTest {
        showComposer(vm())

        click(UiTags.announceCustomSwatch("background"))

        assertTrue(exists(UiTags.COLOR_PICKER_HUE))
        assertTrue(exists(UiTags.COLOR_PICKER_SATURATION))
        assertTrue(exists(UiTags.COLOR_PICKER_BRIGHTNESS))
    }

    @Test
    fun cancellingTheColourPickerChangesNothing() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        val before = viewModel.form.value.textColor
        click(UiTags.announceCustomSwatch("text"))

        click(UiTags.COLOR_PICKER_CANCEL)

        assertEquals(before, viewModel.form.value.textColor)
    }

    @Test
    fun cancellingClosesTheColourPicker() = runComposeUiTest {
        showComposer(vm())
        click(UiTags.announceCustomSwatch("text"))

        click(UiTags.COLOR_PICKER_CANCEL)

        assertFalse(exists(UiTags.COLOR_PICKER))
    }

    @Test
    fun usingTheChosenColourClosesThePicker() = runComposeUiTest {
        showComposer(vm())
        click(UiTags.announceCustomSwatch("text"))

        click(UiTags.COLOR_PICKER_USE)

        assertFalse(exists(UiTags.COLOR_PICKER))
    }

    @Test
    fun usingTheChosenColourSetsTheTextColour() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        click(UiTags.announceCustomSwatch("text"))

        click(UiTags.COLOR_PICKER_USE)

        assertTrue(viewModel.form.value.textColor.startsWith("#"))
    }

    @Test
    fun theBackgroundPickerSetsTheBackgroundColour() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        click(UiTags.announceCustomSwatch("background"))

        click(UiTags.COLOR_PICKER_USE)

        assertTrue(viewModel.form.value.backgroundColor.startsWith("#"))
    }

    @Test
    fun theBackgroundPickerLeavesTheTextColourAlone() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        val before = viewModel.form.value.textColor
        click(UiTags.announceCustomSwatch("background"))

        click(UiTags.COLOR_PICKER_USE)

        assertEquals(before, viewModel.form.value.textColor)
    }

    // ── Animation ────────────────────────────────────────────────────────

    @Test
    fun theAnimationChoiceIsOffered() = runComposeUiTest {
        showComposer(vm())

        assertTrue(exists(UiTags.ANNOUNCE_ANIMATION))
    }

    @Test
    fun theAnimationMenuIsClosedToBeginWith() = runComposeUiTest {
        showComposer(vm())

        assertFalse(exists(UiTags.announceAnimation(AnnouncementAnimation.entries.last().name)))
    }

    @Test
    fun tappingTheAnimationOpensItsMenu() = runComposeUiTest {
        showComposer(vm())

        click(UiTags.ANNOUNCE_ANIMATION)

        assertTrue(exists(UiTags.announceAnimation(AnnouncementAnimation.entries.first().name)))
    }

    @Test
    fun theAnimationMenuListsEveryOption() = runComposeUiTest {
        showComposer(vm())

        click(UiTags.ANNOUNCE_ANIMATION)

        AnnouncementAnimation.entries.forEach {
            assertTrue(exists(UiTags.announceAnimation(it.name)), "missing ${it.name}")
        }
    }

    @Test
    fun choosingAnAnimationKeepsIt() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        val choice = AnnouncementAnimation.entries.last()
        click(UiTags.ANNOUNCE_ANIMATION)

        click(UiTags.announceAnimation(choice.name))

        assertEquals(choice, viewModel.form.value.animation)
    }

    @Test
    fun choosingAnAnimationClosesTheMenu() = runComposeUiTest {
        showComposer(vm())
        val choice = AnnouncementAnimation.entries.last()
        click(UiTags.ANNOUNCE_ANIMATION)

        click(UiTags.announceAnimation(choice.name))

        assertFalse(exists(UiTags.announceAnimation(choice.name)))
    }

    @Test
    fun theAnimationDurationSliderIsOffered() = runComposeUiTest {
        showComposer(vm())

        assertTrue(exists(UiTags.ANNOUNCE_DURATION))
    }

    // ── The preview ──────────────────────────────────────────────────────

    @Test
    fun aPreviewIsAlwaysShown() = runComposeUiTest {
        showComposer(vm())

        assertTrue(exists(UiTags.ANNOUNCE_PREVIEW))
    }

    @Test
    fun thePreviewShowsTheWordsBeingTyped() = runComposeUiTest {
        showComposer(vm())

        type(UiTags.ANNOUNCE_TEXT, "Welcome to church")

        assertTrue(isShowing("Welcome to church"))
    }

    @Test
    fun thePreviewShowsACountdownAsATime() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN)
        repeat(20) { click(UiTags.stepperDown(UiTags.ANNOUNCE_MINUTES)) }

        click(UiTags.stepperUp(UiTags.ANNOUNCE_MINUTES))

        assertTrue(isShowing("00:01:00"))
    }

    @Test
    fun thePreviewCountsTheHoursToo() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN)
        repeat(20) { click(UiTags.stepperDown(UiTags.ANNOUNCE_MINUTES)) }

        click(UiTags.stepperUp(UiTags.ANNOUNCE_HOURS))

        assertTrue(isShowing("01:00:00"))
    }

    @Test
    fun thePreviewShowsAClockAsAClock() = runComposeUiTest {
        showComposer(vm())

        chooseType(AnnouncementType.CLOCK)

        assertTrue(isShowing("12:00:00"))
    }

    @Test
    fun thePreviewShowsACountUpFromZero() = runComposeUiTest {
        showComposer(vm())

        chooseType(AnnouncementType.COUNT_UP)

        assertTrue(isShowing("00:00"))
    }

    @Test
    fun thePreviewShowsTheTimeBeingCountedTo() = runComposeUiTest {
        val viewModel = vm()
        showComposer(viewModel)
        chooseType(AnnouncementType.COUNTDOWN_TO_TIME)
        repeat(20) { click(UiTags.stepperDown(UiTags.ANNOUNCE_TARGET_HOUR)) }

        repeat(10) { click(UiTags.stepperUp(UiTags.ANNOUNCE_TARGET_HOUR)) }

        assertTrue(isShowing("10:00"))
    }

    @Test
    fun thePreviewSurvivesAnEmptyNotice() = runComposeUiTest {
        showComposer(vm())

        type(UiTags.ANNOUNCE_TEXT, "")

        assertTrue(exists(UiTags.ANNOUNCE_PREVIEW))
    }
}
