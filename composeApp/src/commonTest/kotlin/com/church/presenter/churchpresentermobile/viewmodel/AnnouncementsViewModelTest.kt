package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AnnouncementAnimation
import com.church.presenter.churchpresentermobile.model.AnnouncementType
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ScheduleItem
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests [AnnouncementsViewModel.preload] (structured + label-fallback + regex timer
 * recovery) and the saved-announcement JSON persistence round-trip.
 */
class AnnouncementsViewModelTest {

    private fun vmWith(storage: InMemorySettingsStorage = InMemorySettingsStorage()): Pair<AnnouncementsViewModel, AppSettings> {
        val settings = AppSettings(storage)
        return AnnouncementsViewModel(settings, ServerEventService(settings)) to settings
    }

    private fun item(
        displayText: String? = null,
        text: String? = null,
        isTimer: Boolean? = null,
        timerMode: String? = null,
        timerHours: Int? = null,
        timerMinutes: Int? = null,
        timerSeconds: Int? = null,
        targetHour: Int? = null,
        targetMinute: Int? = null,
        animationType: String? = null,
        fontSize: Int? = null,
    ) = ScheduleItem(
        type = "announcement",
        displayTextCamel = displayText,
        text = text,
        isTimer = isTimer,
        timerMode = timerMode,
        timerHours = timerHours,
        timerMinutes = timerMinutes,
        timerSeconds = timerSeconds,
        targetHour = targetHour,
        targetMinute = targetMinute,
        animationType = animationType,
        fontSize = fontSize,
    )

    // ── preload: structured isTimer/timerMode ────────────────────────────────

    @Test
    fun preloadStructuredTimerModes() {
        val (vm, _) = vmWith()
        vm.preload(item(isTimer = true, timerMode = "count_up"))
        assertEquals(AnnouncementType.COUNT_UP, vm.form.value.type)
        vm.preload(item(isTimer = true, timerMode = "clock_display"))
        assertEquals(AnnouncementType.CLOCK, vm.form.value.type)
        vm.preload(item(isTimer = true, timerMode = "clock"))
        assertEquals(AnnouncementType.COUNTDOWN_TO_TIME, vm.form.value.type)
        vm.preload(item(isTimer = true, timerMode = "duration"))
        assertEquals(AnnouncementType.COUNTDOWN, vm.form.value.type)
        vm.preload(item(isTimer = false, displayText = "Timer 05:00"))
        assertEquals(AnnouncementType.TEXT, vm.form.value.type)
    }

    // ── preload: label fallbacks (older desktop, no structured fields) ───────

    @Test
    fun preloadInfersTypeFromLabel() {
        val (vm, _) = vmWith()
        vm.preload(item(displayText = "Until 12:30:00"))
        assertEquals(AnnouncementType.COUNTDOWN_TO_TIME, vm.form.value.type)
        vm.preload(item(displayText = "Clock"))
        assertEquals(AnnouncementType.CLOCK, vm.form.value.type)
        vm.preload(item(displayText = "Timer 05:00"))
        assertEquals(AnnouncementType.COUNTDOWN, vm.form.value.type)
        vm.preload(item(displayText = "Welcome friends"))
        assertEquals(AnnouncementType.TEXT, vm.form.value.type)
    }

    @Test
    fun preloadRecoversCountdownDigitsFromLabel() {
        val (vm, _) = vmWith()
        vm.preload(item(displayText = "Timer 01:05:30")) // h:m:s
        vm.form.value.let {
            assertEquals(1, it.hours)
            assertEquals(5, it.minutes)
            assertEquals(30, it.seconds)
        }
        vm.preload(item(displayText = "Timer 07:15")) // m:s
        vm.form.value.let {
            assertEquals(0, it.hours)
            assertEquals(7, it.minutes)
            assertEquals(15, it.seconds)
        }
    }

    @Test
    fun preloadRecoversTargetTimeFromLabel() {
        val (vm, _) = vmWith()
        vm.preload(item(displayText = "Until 09:45:00"))
        assertEquals(9, vm.form.value.targetHour)
        assertEquals(45, vm.form.value.targetMinute)
    }

    @Test
    fun preloadStructuredFieldsOverrideLabelDigits() {
        val (vm, _) = vmWith()
        vm.preload(item(displayText = "Timer 01:05:30", isTimer = true, timerMode = "duration",
            timerHours = 2, timerMinutes = 3, timerSeconds = 4))
        vm.form.value.let {
            assertEquals(2, it.hours)
            assertEquals(3, it.minutes)
            assertEquals(4, it.seconds)
        }
    }

    @Test
    fun preloadTextAndAnimationAndDefaults() {
        val (vm, _) = vmWith()
        vm.preload(item(displayText = "ignored label", text = "Real body", animationType = "FADE"))
        assertEquals("Real body", vm.form.value.text)
        assertEquals(AnnouncementAnimation.FADE, vm.form.value.animation)

        // Unknown animation → default; missing fontSize → 48.
        vm.preload(item(displayText = "Hi", animationType = "NOPE"))
        assertEquals(AnnouncementAnimation.SLIDE_BOTTOM, vm.form.value.animation)
        assertEquals(48, vm.form.value.fontSize)
    }

    // ── Saved announcements persistence ──────────────────────────────────────

    @Test
    fun saveCurrentPersistsAndSurvivesReload() {
        val storage = InMemorySettingsStorage()
        val (vm, _) = vmWith(storage)
        vm.update { it.copy(type = AnnouncementType.TEXT, text = "Coffee after service") }
        vm.saveCurrent()
        assertEquals(1, vm.saved.value.size)
        assertEquals("Saved", vm.message.value)

        // A fresh ViewModel over the same storage reloads the saved list.
        val (vm2, _) = vmWith(storage)
        assertEquals(1, vm2.saved.value.size)
        assertEquals("Coffee after service", vm2.saved.value[0].label)
    }

    @Test
    fun deleteSavedRemovesAndPersists() {
        val storage = InMemorySettingsStorage()
        val (vm, _) = vmWith(storage)
        vm.update { it.copy(text = "A") }
        vm.saveCurrent()
        val id = vm.saved.value[0].id
        vm.deleteSaved(id)
        assertEquals(0, vm.saved.value.size)
        assertEquals(0, vmWith(storage).first.saved.value.size)
    }

    @Test
    fun loadSavedByIdRestoresForm() {
        val (vm, _) = vmWith()
        vm.update { it.copy(type = AnnouncementType.CLOCK) }
        vm.saveCurrent()
        val id = vm.saved.value[0].id
        vm.update { it.copy(type = AnnouncementType.TEXT, text = "changed") }
        vm.loadSaved(id)
        assertEquals(AnnouncementType.CLOCK, vm.form.value.type)
    }

    @Test
    fun savedLabelUsesTextOrTypeLabel() {
        val (vm, _) = vmWith()
        vm.update { it.copy(type = AnnouncementType.TEXT, text = "") }
        vm.saveCurrent()
        assertEquals("Untitled", vm.saved.value[0].label)

        vm.update { it.copy(type = AnnouncementType.CLOCK) }
        vm.saveCurrent()
        assertEquals(AnnouncementType.CLOCK.label, vm.saved.value[1].label)
    }

    // ── The payload sent to the desktop ──────────────────────────────────
    //
    // buildPayload is private, so it is exercised through the two actions that
    // use it. What matters is the shape on the wire: the desktop reads these
    // fields by name and decides from them whether it is drawing text or
    // running a clock.

    private fun sendingVm(): Pair<AnnouncementsViewModel, FakeWsSender> {
        val ws = FakeWsSender()
        return AnnouncementsViewModel(AppSettings(InMemorySettingsStorage()), ws) to ws
    }

    @Test
    fun `a text announcement carries its words and is not a timer`() = runVmTestUnconfined {
        val (vm, ws) = sendingVm()
        try {
            vm.update { it.copy(type = AnnouncementType.TEXT) }
            vm.update { it.copy(text = "Welcome to the service") }

            vm.addToSchedule()

            val payload = ws.lastPayload
            assertTrue(payload.contains("\"announcementText\":\"Welcome to the service\""), payload)
            assertTrue(payload.contains("\"isTimer\":false"), payload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a timer carries no announcement text, whatever was typed`() = runVmTestUnconfined {
        // The text field keeps its value while the user switches type, but a timer
        // that arrives carrying text is rendered as text by the desktop.
        val (vm, ws) = sendingVm()
        try {
            vm.update { it.copy(text = "left over from before") }
            vm.update { it.copy(type = AnnouncementType.COUNTDOWN) }

            vm.addToSchedule()

            val payload = ws.lastPayload
            assertTrue(payload.contains("\"announcementText\":\"\""), payload)
            assertTrue(payload.contains("\"isTimer\":true"), payload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a countdown carries its duration and no target clock time`() = runVmTestUnconfined {
        val (vm, ws) = sendingVm()
        try {
            vm.update { it.copy(type = AnnouncementType.COUNTDOWN) }
            vm.update { it.copy(hours = 1) }
            vm.update { it.copy(minutes = 2) }
            vm.update { it.copy(seconds = 3) }
            vm.update { it.copy(targetHour = 9) }
            vm.update { it.copy(targetMinute = 30) }

            vm.addToSchedule()

            val payload = ws.lastPayload
            assertTrue(payload.contains("\"timerHours\":1"), payload)
            assertTrue(payload.contains("\"timerMinutes\":2"), payload)
            assertTrue(payload.contains("\"timerSeconds\":3"), payload)
            assertTrue(payload.contains("\"timerMode\":\"duration\""), payload)
            // A stale target time from the other mode must not travel with it.
            assertTrue(payload.contains("\"targetHour\":0"), payload)
            assertTrue(payload.contains("\"targetMinute\":0"), payload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a countdown-to-time carries its target and no duration`() = runVmTestUnconfined {
        val (vm, ws) = sendingVm()
        try {
            vm.update { it.copy(type = AnnouncementType.COUNTDOWN_TO_TIME) }
            vm.update { it.copy(hours = 1) }
            vm.update { it.copy(minutes = 2) }
            vm.update { it.copy(targetHour = 9) }
            vm.update { it.copy(targetMinute = 30) }

            vm.addToSchedule()

            val payload = ws.lastPayload
            assertTrue(payload.contains("\"targetHour\":9"), payload)
            assertTrue(payload.contains("\"targetMinute\":30"), payload)
            assertTrue(payload.contains("\"timerMode\":\"clock\""), payload)
            assertTrue(payload.contains("\"timerHours\":0"), payload)
            assertTrue(payload.contains("\"timerMinutes\":0"), payload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `each type sends the desktop's own timer-mode string`() = runVmTestUnconfined {
        val expected = mapOf(
            AnnouncementType.TEXT to "duration",
            AnnouncementType.COUNTDOWN to "duration",
            AnnouncementType.COUNT_UP to "count_up",
            AnnouncementType.CLOCK to "clock_display",
            AnnouncementType.COUNTDOWN_TO_TIME to "clock",
        )
        for ((type, mode) in expected) {
            val (vm, ws) = sendingVm()
            try {
                vm.update { it.copy(type = type) }
                vm.addToSchedule()

                assertTrue(ws.lastPayload.contains("\"timerMode\":\"$mode\""), "$type → ${ws.lastPayload}")
            } finally {
                tearDown(vm)
            }
        }
    }

    @Test
    fun `the timer reuses the announcement's own text colour`() = runVmTestUnconfined {
        // One colour picker on screen drives both, so they must not diverge.
        val (vm, ws) = sendingVm()
        try {
            vm.update { it.copy(type = AnnouncementType.COUNTDOWN) }
            vm.update { it.copy(textColor = "#FF0000") }

            vm.addToSchedule()

            val payload = ws.lastPayload
            assertTrue(payload.contains("\"textColor\":\"#FF0000\""), payload)
            assertTrue(payload.contains("\"timerTextColor\":\"#FF0000\""), payload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `the id is left blank for the server to assign`() = runVmTestUnconfined {
        val (vm, ws) = sendingVm()
        try {
            vm.update { it.copy(type = AnnouncementType.TEXT) }
            vm.update { it.copy(text = "Hello") }

            vm.addToSchedule()

            assertTrue(ws.lastPayload.contains("\"id\":\"\""), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `showing on screen sends the same payload down the project path`() = runVmTestUnconfined {
        val (vm, ws) = sendingVm()
        try {
            vm.update { it.copy(type = AnnouncementType.TEXT) }
            vm.update { it.copy(text = "Now showing") }

            vm.showOnScreen()

            assertEquals(WsMessageType.PROJECT, ws.lastType)
            assertTrue(ws.lastPayload.contains("Now showing"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `clearing the screen sends clear and says so`() = runVmTestUnconfined {
        val (vm, ws) = sendingVm()
        try {
            vm.clearScreen()
            val message = vm.message.first { it != null }

            assertEquals(WsMessageType.CLEAR, ws.lastType)
            assertEquals("Cleared", message)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a consumed message is cleared so it shows once`() = runVmTestUnconfined {
        val (vm, _) = sendingVm()
        try {
            vm.clearScreen()
            vm.message.first { it != null }

            vm.clearMessage()

            assertNull(vm.message.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Label inference: the variants nothing else reaches ───────────────
    //
    // An older desktop sends no structured timer fields, so the type has to be
    // recovered from the row's display text. Each spelling below is one the
    // desktop actually produces; getting it wrong reopens a countdown as text.

    @Test
    fun `a duration timer label is recognised as count-up`() {
        val (vm, _) = vmWith()

        vm.preload(item(displayText = "Duration Timer"))

        assertEquals(AnnouncementType.COUNT_UP, vm.form.value.type)
    }

    @Test
    fun `the label spellings are matched whatever case they arrive in`() {
        val (vm, _) = vmWith()

        vm.preload(item(displayText = "duration timer"))
        assertEquals(AnnouncementType.COUNT_UP, vm.form.value.type)

        vm.preload(item(displayText = "CLOCK"))
        assertEquals(AnnouncementType.CLOCK, vm.form.value.type)
    }

    @Test
    fun `a bare Timer label needs its trailing space to be a countdown`() {
        // startsWith("Timer ") — the word alone is ordinary announcement text.
        val (vm, _) = vmWith()

        vm.preload(item(displayText = "Timer"))

        assertEquals(AnnouncementType.TEXT, vm.form.value.type)
    }

    @Test
    fun `an item that says it is not a timer is text whatever its label reads`() {
        // The structured field wins: a text announcement whose wording happens to
        // start with "Timer " must not reopen as a countdown.
        val (vm, _) = vmWith()

        vm.preload(item(displayText = "Timer maintenance this week", isTimer = false))

        assertEquals(AnnouncementType.TEXT, vm.form.value.type)
    }

    @Test
    fun `an item with no display text at all is text`() {
        val (vm, _) = vmWith()

        vm.preload(item())

        assertEquals(AnnouncementType.TEXT, vm.form.value.type)
    }

    @Test
    fun `a countdown-to-time with no digits falls back to noon`() {
        // "Until" with nothing parseable after it; the form still has to open on
        // a valid time rather than on hour zero.
        val (vm, _) = vmWith()

        vm.preload(item(displayText = "Until further notice"))

        assertEquals(AnnouncementType.COUNTDOWN_TO_TIME, vm.form.value.type)
        assertEquals(12, vm.form.value.targetHour)
        assertEquals(0, vm.form.value.targetMinute)
    }

    @Test
    fun `a countdown-to-time with only an hour keeps the minute at zero`() {
        val (vm, _) = vmWith()

        vm.preload(item(displayText = "Until 9"))

        assertEquals(9, vm.form.value.targetHour)
        assertEquals(0, vm.form.value.targetMinute)
    }

    @Test
    fun `a countdown label with no digits keeps the default duration`() {
        // Fewer than two numbers, so there is nothing to split into minutes and
        // seconds; the default five minutes stands.
        val (vm, _) = vmWith()

        vm.preload(item(displayText = "Timer soon"))

        assertEquals(AnnouncementType.COUNTDOWN, vm.form.value.type)
        assertEquals(0, vm.form.value.hours)
        assertEquals(5, vm.form.value.minutes)
        assertEquals(0, vm.form.value.seconds)
    }

    @Test
    fun `an unrecognised animation falls back to the default`() {
        // The desktop's animation vocabulary can outrun ours; an unknown value
        // must not leave the form with no animation selected.
        val (vm, _) = vmWith()

        vm.preload(item(displayText = "Welcome", animationType = "SPIN_AND_EXPLODE"))

        assertEquals(AnnouncementAnimation.SLIDE_BOTTOM, vm.form.value.animation)
    }

    @Test
    fun `each structured timer mode maps to its own type`() {
        val (vm, _) = vmWith()

        for ((mode, expected) in listOf(
            "count_up" to AnnouncementType.COUNT_UP,
            "clock_display" to AnnouncementType.CLOCK,
            "clock" to AnnouncementType.COUNTDOWN_TO_TIME,
            "duration" to AnnouncementType.COUNTDOWN,
            "something_new" to AnnouncementType.COUNTDOWN,
        )) {
            vm.preload(item(isTimer = true, timerMode = mode))

            assertEquals(expected, vm.form.value.type, "timerMode=$mode")
        }
    }

    // ── When the desktop refuses ─────────────────────────────────────────

    private fun failingVm(error: Throwable): Pair<AnnouncementsViewModel, FakeWsSender> {
        val ws = FakeWsSender()
        ws.failWith(error)
        return AnnouncementsViewModel(AppSettings(InMemorySettingsStorage()), ws) to ws
    }

    @Test
    fun `a failed add to schedule is reported`() = runVmTestUnconfined {
        val (vm, _) = failingVm(IllegalStateException("denied"))
        try {
            vm.update { it.copy(type = AnnouncementType.TEXT, text = "Welcome") }

            vm.addToSchedule()
            val message = vm.message.first { it != null }

            assertEquals("denied", message)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed show on screen is reported`() = runVmTestUnconfined {
        val (vm, _) = failingVm(IllegalStateException("Connection refused"))
        try {
            vm.update { it.copy(type = AnnouncementType.TEXT, text = "Welcome") }

            vm.showOnScreen()
            val message = vm.message.first { it != null }

            assertEquals("Connection refused", message)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed clear is reported`() = runVmTestUnconfined {
        val (vm, _) = failingVm(IllegalStateException("socket closed"))
        try {
            vm.clearScreen()
            val message = vm.message.first { it != null }

            assertEquals("socket closed", message)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failure with no message still says something`() = runVmTestUnconfined {
        val (vm, _) = failingVm(IllegalStateException())
        try {
            vm.addToSchedule()
            val message = vm.message.first { it != null }

            assertNotNull(message)
            assertTrue(message!!.isNotBlank())
        } finally {
            tearDown(vm)
        }
    }
}
