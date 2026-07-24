package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests the [AnnouncementType] flags/mapping and [AnnouncementAnimation] values. */
class AnnouncementModelTest {

    @Test
    fun isTimerIsTrueForEveryTypeExceptText() {
        assertFalse(AnnouncementType.TEXT.isTimer)
        assertTrue(AnnouncementType.COUNTDOWN.isTimer)
        assertTrue(AnnouncementType.COUNT_UP.isTimer)
        assertTrue(AnnouncementType.CLOCK.isTimer)
        assertTrue(AnnouncementType.COUNTDOWN_TO_TIME.isTimer)
    }

    @Test
    fun timerModeMapsEachType() {
        assertEquals("duration", AnnouncementType.COUNTDOWN.timerMode)
        assertEquals("count_up", AnnouncementType.COUNT_UP.timerMode)
        assertEquals("clock_display", AnnouncementType.CLOCK.timerMode)
        assertEquals("clock", AnnouncementType.COUNTDOWN_TO_TIME.timerMode)
        assertEquals("duration", AnnouncementType.TEXT.timerMode)
    }

    @Test
    fun labelsAreStable() {
        assertEquals("Text", AnnouncementType.TEXT.label)
        assertEquals("Countdown", AnnouncementType.COUNTDOWN.label)
    }

    @Test
    fun animationValuesMatchDesktopConstants() {
        assertEquals("SLIDE_FROM_BOTTOM", AnnouncementAnimation.SLIDE_BOTTOM.value)
        assertEquals("FADE", AnnouncementAnimation.FADE.value)
        assertEquals("NONE", AnnouncementAnimation.NONE.value)
    }
}
