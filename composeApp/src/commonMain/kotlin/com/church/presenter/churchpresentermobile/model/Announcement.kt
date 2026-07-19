package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/** The kind of announcement/timer the user is composing. */
@Serializable
enum class AnnouncementType(val label: String) {
    TEXT("Text"),
    COUNTDOWN("Countdown"),
    COUNT_UP("Count up"),
    CLOCK("Clock"),
    COUNTDOWN_TO_TIME("Countdown to time");

    val isTimer: Boolean get() = this != TEXT

    /** Server timer-mode string (matches the desktop `Constants.TIMER_MODE_*`). */
    val timerMode: String
        get() = when (this) {
            COUNTDOWN -> "duration"
            COUNT_UP -> "count_up"
            CLOCK -> "clock_display"
            COUNTDOWN_TO_TIME -> "clock"
            TEXT -> "duration"
        }
}

/** Announcement animation options (values match the desktop's announcement animation constants). */
@Serializable
enum class AnnouncementAnimation(val label: String, val value: String) {
    NONE("None", "NONE"),
    FADE("Fade", "FADE"),
    SLIDE_BOTTOM("Slide up", "SLIDE_FROM_BOTTOM"),
    SLIDE_TOP("Slide down", "SLIDE_FROM_TOP"),
    SLIDE_LEFT("Slide from left", "SLIDE_FROM_LEFT"),
    SLIDE_RIGHT("Slide from right", "SLIDE_FROM_RIGHT"),
}

/**
 * Flat payload for `POST /api/schedule/add`. The desktop's `RemoteItemDto.toScheduleItem()`
 * infers an `AnnouncementItem` from [announcementText] (always present; "" for a pure timer).
 */
@Serializable
data class AnnouncementItemPayload(
    val type: String = "announcement",
    val id: String,
    val announcementText: String,
    val textColor: String = "#FFFFFF",
    val backgroundColor: String = "#000000",
    val fontSize: Int = 48,
    val animationType: String = "SLIDE_FROM_BOTTOM",
    val animationDuration: Int = 500,
    val isTimer: Boolean = false,
    val timerHours: Int = 0,
    val timerMinutes: Int = 0,
    val timerSeconds: Int = 0,
    val timerMode: String = "duration",
    val targetHour: Int = 0,
    val targetMinute: Int = 0,
    val targetSecond: Int = 0,
    val timerTextColor: String = "#FFFFFF",
    val timerExpiredText: String = "",
    val liveClockFormat: String = "HH:mm:ss",
    val displayText: String = "",
)

/** Wrapper request body for the schedule-add endpoint. */
@Serializable
data class AnnouncementRequest(val item: AnnouncementItemPayload)
