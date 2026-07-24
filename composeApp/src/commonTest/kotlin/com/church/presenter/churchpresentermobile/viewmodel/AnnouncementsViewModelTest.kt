package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AnnouncementAnimation
import com.church.presenter.churchpresentermobile.model.AnnouncementType
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ScheduleItem
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
