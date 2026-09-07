package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AnnouncementType
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.viewmodel.AnnouncementsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The announcement composer — the screen used to put "Service starts in 5
 * minutes" on the wall before anyone is in the room.
 *
 * What it sends is what the congregation reads, so these assert on the payload
 * that reaches the desktop, through a stand-in sender. The rest is which
 * controls a given kind of announcement shows: a countdown needs its duration
 * fields and a clock needs none, and offering the wrong ones is how an operator
 * ends up sending a timer set to zero.
 */
@OptIn(ExperimentalTestApi::class)
class AnnouncementsScreenTest {

    private fun vmWith(sender: FakeWsSender = FakeWsSender()) =
        AnnouncementsViewModel(AppSettings(InMemorySettingsStorage()), sender)

    private fun ComposeUiTest.showAnnouncements(vm: AnnouncementsViewModel) =
        showScreen { AnnouncementsScreen(viewModel = vm) }

    private fun type(t: AnnouncementType) = UiTags.announceType(t.name)

    // ── Choosing a kind ──────────────────────────────────────────────────

    @Test
    fun everyKindOfAnnouncementIsOffered() = runComposeUiTest {
        showAnnouncements(vmWith())

        AnnouncementType.entries.forEach { t ->
            assertTrue(exists(type(t)), t.name)
        }
    }

    @Test
    fun theComposerOpensOnPlainText() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        assertEquals(AnnouncementType.TEXT, vm.form.value.type)
    }

    @Test
    fun choosingCountdownReportsThatKind() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        click(type(AnnouncementType.COUNTDOWN))

        assertEquals(AnnouncementType.COUNTDOWN, vm.form.value.type)
    }

    @Test
    fun choosingClockReportsThatKind() = runComposeUiTest {
        // Each chip has to carry its own kind — one wired to the wrong constant
        // puts a stopwatch on the wall where a clock was wanted.
        val vm = vmWith()
        showAnnouncements(vm)

        click(type(AnnouncementType.CLOCK))

        assertEquals(AnnouncementType.CLOCK, vm.form.value.type)
    }

    @Test
    fun choosingCountUpReportsThatKind() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        click(type(AnnouncementType.COUNT_UP))

        assertEquals(AnnouncementType.COUNT_UP, vm.form.value.type)
    }

    @Test
    fun choosingCountdownToATimeReportsThatKind() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        click(type(AnnouncementType.COUNTDOWN_TO_TIME))

        assertEquals(AnnouncementType.COUNTDOWN_TO_TIME, vm.form.value.type)
    }

    // ── The controls each kind needs ─────────────────────────────────────

    @Test
    fun plainTextOffersSomewhereToTypeIt() = runComposeUiTest {
        showAnnouncements(vmWith())

        assertTrue(exists(UiTags.ANNOUNCE_TEXT))
    }

    @Test
    fun aCountdownOffersItsDurationFields() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        click(type(AnnouncementType.COUNTDOWN))

        assertTrue(exists(UiTags.ANNOUNCE_COUNTDOWN_FIELDS))
    }

    @Test
    fun aCountdownDoesNotOfferTheTextBox() = runComposeUiTest {
        // There is nothing to type; a text box here would be sent and ignored.
        val vm = vmWith()
        showAnnouncements(vm)

        click(type(AnnouncementType.COUNTDOWN))

        assertFalse(exists(UiTags.ANNOUNCE_TEXT))
    }

    @Test
    fun aCountdownToATimeOffersATargetRatherThanADuration() = runComposeUiTest {
        // "Until 12:00" and "for 5 minutes" are different questions.
        val vm = vmWith()
        showAnnouncements(vm)

        click(type(AnnouncementType.COUNTDOWN_TO_TIME))

        assertTrue(exists(UiTags.ANNOUNCE_UNTIL_FIELDS))
        assertFalse(exists(UiTags.ANNOUNCE_COUNTDOWN_FIELDS))
    }

    @Test
    fun aClockOffersNeitherDurationNorTarget() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        click(type(AnnouncementType.CLOCK))

        assertFalse(exists(UiTags.ANNOUNCE_COUNTDOWN_FIELDS))
        assertFalse(exists(UiTags.ANNOUNCE_UNTIL_FIELDS))
    }

    @Test
    fun goingBackToTextBringsTheTextBoxBack() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        click(type(AnnouncementType.COUNTDOWN))
        click(type(AnnouncementType.TEXT))

        assertTrue(exists(UiTags.ANNOUNCE_TEXT))
    }

    // ── Typing the words ─────────────────────────────────────────────────

    @Test
    fun typingTheAnnouncementIsKept() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        type(UiTags.ANNOUNCE_TEXT, "Service starts in 5 minutes")

        assertEquals("Service starts in 5 minutes", vm.form.value.text)
    }

    @Test
    fun theWordsTypedAreShownBack() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        type(UiTags.ANNOUNCE_TEXT, "Welcome")

        assertTrue(isShowing("Welcome"))
    }

    // ── Sending it ───────────────────────────────────────────────────────

    @Test
    fun goingLiveSendsTheAnnouncement() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = vmWith(sender)
        showAnnouncements(vm)

        type(UiTags.ANNOUNCE_TEXT, "Welcome")
        click(UiTags.ANNOUNCE_GO_LIVE)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.PROJECT, sender.lastType)
    }

    @Test
    fun theTypedWordsReachTheDesktop() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = vmWith(sender)
        showAnnouncements(vm)

        type(UiTags.ANNOUNCE_TEXT, "Service starts in 5 minutes")
        click(UiTags.ANNOUNCE_GO_LIVE)

        awaitThat { sender.calls.isNotEmpty() }
        assertTrue("Service starts in 5 minutes" in sender.lastPayload, sender.lastPayload)
    }

    @Test
    fun addingToTheScheduleGoesDownTheScheduleRoute() = runComposeUiTest {
        // Queued, not projected — the difference between preparing and going
        // live in front of the congregation.
        val sender = FakeWsSender()
        val vm = vmWith(sender)
        showAnnouncements(vm)

        type(UiTags.ANNOUNCE_TEXT, "Welcome")
        click(UiTags.ANNOUNCE_ADD_TO_SCHEDULE)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.ADD_TO_SCHEDULE, sender.lastType)
    }

    @Test
    fun clearingTellsTheDesktopToBlank() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = vmWith(sender)
        showAnnouncements(vm)

        click(UiTags.ANNOUNCE_CLEAR)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.CLEAR, sender.lastType)
    }

    @Test
    fun theChosenKindReachesTheDesktop() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = vmWith(sender)
        showAnnouncements(vm)

        click(type(AnnouncementType.CLOCK))
        click(UiTags.ANNOUNCE_GO_LIVE)

        awaitThat { sender.calls.isNotEmpty() }
        assertTrue(
            AnnouncementType.CLOCK.timerMode in sender.lastPayload,
            sender.lastPayload,
        )
    }

    // ── Saving one to reuse ──────────────────────────────────────────────

    @Test
    fun nothingIsSavedToStartWith() = runComposeUiTest {
        showAnnouncements(vmWith())

        assertTrue(exists(UiTags.ANNOUNCE_NO_SAVED))
    }

    @Test
    fun savingTheCurrentAnnouncementAddsIt() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        type(UiTags.ANNOUNCE_TEXT, "Welcome")
        click(UiTags.ANNOUNCE_SAVE)

        awaitThat { vm.saved.value.isNotEmpty() }
        assertEquals(1, vm.saved.value.size)
    }

    @Test
    fun aSavedAnnouncementIsListed() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        type(UiTags.ANNOUNCE_TEXT, "Welcome")
        click(UiTags.ANNOUNCE_SAVE)

        awaitThat { vm.saved.value.isNotEmpty() }
        assertTrue(exists(UiTags.savedAnnouncement(vm.saved.value.first().id)))
    }

    @Test
    fun theEmptyNoticeGoesOnceSomethingIsSaved() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        click(UiTags.ANNOUNCE_SAVE)

        awaitThat { vm.saved.value.isNotEmpty() }
        assertFalse(exists(UiTags.ANNOUNCE_NO_SAVED))
    }

    @Test
    fun aSavedAnnouncementCanBeLoadedBack() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        type(UiTags.ANNOUNCE_TEXT, "Welcome")
        click(UiTags.ANNOUNCE_SAVE)
        awaitThat { vm.saved.value.isNotEmpty() }
        val id = vm.saved.value.first().id
        type(UiTags.ANNOUNCE_TEXT, "Something else")
        click(UiTags.savedAnnouncement(id))

        awaitThat { vm.form.value.text == "Welcome" }
        assertEquals("Welcome", vm.form.value.text)
    }

    @Test
    fun aSavedAnnouncementCanBeDeleted() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        click(UiTags.ANNOUNCE_SAVE)
        awaitThat { vm.saved.value.isNotEmpty() }
        val id = vm.saved.value.first().id
        click(UiTags.savedAnnouncementDelete(id))

        awaitThat { vm.saved.value.isEmpty() }
        assertTrue(vm.saved.value.isEmpty())
    }

    @Test
    fun deletingOneSavedAnnouncementLeavesTheOthers() = runComposeUiTest {
        val vm = vmWith()
        showAnnouncements(vm)

        type(UiTags.ANNOUNCE_TEXT, "First")
        click(UiTags.ANNOUNCE_SAVE)
        awaitThat { vm.saved.value.size == 1 }
        type(UiTags.ANNOUNCE_TEXT, "Second")
        click(UiTags.ANNOUNCE_SAVE)
        awaitThat { vm.saved.value.size == 2 }

        val first = vm.saved.value.first().id
        click(UiTags.savedAnnouncementDelete(first))

        awaitThat { vm.saved.value.size == 1 }
        assertEquals("Second", vm.saved.value.first().form.text)
    }

    @Test
    fun deletingIsNotAlsoALoad() = runComposeUiTest {
        // The delete button sits inside a clickable row.
        val vm = vmWith()
        showAnnouncements(vm)

        type(UiTags.ANNOUNCE_TEXT, "Saved words")
        click(UiTags.ANNOUNCE_SAVE)
        awaitThat { vm.saved.value.isNotEmpty() }
        val id = vm.saved.value.first().id
        type(UiTags.ANNOUNCE_TEXT, "Current words")
        click(UiTags.savedAnnouncementDelete(id))

        awaitThat { vm.saved.value.isEmpty() }
        assertEquals("Current words", vm.form.value.text)
    }
}
