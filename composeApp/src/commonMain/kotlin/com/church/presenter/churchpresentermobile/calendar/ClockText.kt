package com.church.presenter.churchpresentermobile.calendar

import kotlinx.datetime.LocalTime

/**
 * Times as they are stored, shown and typed.
 *
 * Split from [today] and the month grid next door because the two halves share nothing: a stored
 * time is `HH:mm` whatever the operator's clock shows, and everything that turns one into the
 * other lives here.
 */

private const val MINUTES_PER_HOUR = 60
private const val NOON = 12
private const val LAST_HOUR = 23
private const val LAST_MINUTE = 59
private const val TWO_DIGITS = 10

/** `10:00` (24-hour store form) → `10:00 AM`, or itself when the clock is 24-hour. */
fun clockText(stored: String, use24Hour: Boolean = false): String {
    val time = parseStoredTime(stored) ?: return stored
    if (use24Hour) return stored
    val hour12 = when {
        time.hour == 0 -> NOON
        time.hour > NOON -> time.hour - NOON
        else -> time.hour
    }
    val suffix = if (time.hour < NOON) "AM" else "PM"
    return "$hour12:${twoDigits(time.minute)} $suffix"
}

fun parseStoredTime(stored: String): LocalTime? = runCatching { LocalTime.parse(stored) }.getOrNull()

fun storedTime(time: LocalTime): String = "${twoDigits(time.hour)}:${twoDigits(time.minute)}"

/**
 * What a time field accepts: `18:30`, `6:30 PM`, `6:30pm`, `6 pm`, `1830`. Null for anything else.
 */
fun parseClockText(text: String): LocalTime? {
    val match = CLOCK_PATTERN.matchEntire(text.trim().lowercase().ifEmpty { return null }) ?: return null
    val minute = match.groupValues[2].ifEmpty { "0" }.toIntOrNull()
    val hour = match.groupValues[1].toIntOrNull()?.let { withMeridiem(it, match.groupValues[3]) }
    val inRange = hour in 0..LAST_HOUR && minute in 0..LAST_MINUTE
    return if (inRange) LocalTime(hour!!, minute!!) else null
}

/** `6` with `pm` is 18:00, `12` with `am` is midnight; anything else is the hour as typed. */
private fun withMeridiem(hour: Int, suffix: String): Int = when {
    suffix.startsWith("p") && hour < NOON -> hour + NOON
    suffix.startsWith("a") && hour == NOON -> 0
    else -> hour
}

private val CLOCK_PATTERN = Regex("""(\d{1,2})[:.]?(\d{2})?\s*(am|pm|a|p)?""")

/** Minutes past midnight of a stored `HH:mm`, for arithmetic on the run clock. */
fun minutesOfDay(stored: String): Int? = parseStoredTime(stored)?.let { it.hour * MINUTES_PER_HOUR + it.minute }

fun timeFromMinutes(minutes: Int): String {
    val wrapped = ((minutes % (MINUTES_PER_HOUR * 24)) + MINUTES_PER_HOUR * 24) % (MINUTES_PER_HOUR * 24)
    return "${twoDigits(wrapped / MINUTES_PER_HOUR)}:${twoDigits(wrapped % MINUTES_PER_HOUR)}"
}

private fun twoDigits(value: Int): String = if (value < TWO_DIGITS) "0$value" else value.toString()
