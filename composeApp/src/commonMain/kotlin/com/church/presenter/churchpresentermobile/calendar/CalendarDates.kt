package com.church.presenter.churchpresentermobile.calendar

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private const val DAYS_IN_WEEK = 7
private const val GRID_ROWS = 6
private const val MINUTES_PER_HOUR = 60
private const val NOON = 12
private const val TWO_DIGITS = 10

/** A month as the grid shows it: the first of the month is enough to know it. */
data class YearMonthRef(val year: Int, val month: Int) {
    val firstDay: LocalDate get() = LocalDate(year, month, 1)
    fun next(): YearMonthRef = of(firstDay.plus(1, DateTimeUnit.MONTH))
    fun previous(): YearMonthRef = of(firstDay.minus(1, DateTimeUnit.MONTH))
    fun contains(date: LocalDate): Boolean = date.year == year && date.month.number == month

    companion object {
        fun of(date: LocalDate): YearMonthRef = YearMonthRef(date.year, date.month.number)
    }
}

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/** The 42 dates of a six-row month grid, starting on [weekStart]. */
fun monthGridDates(month: YearMonthRef, weekStart: DayOfWeek = DayOfWeek.SUNDAY): List<LocalDate> {
    val first = month.firstDay
    val offset = (first.dayOfWeek.isoDayNumber - weekStart.isoDayNumber + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val start = first.minus(offset, DateTimeUnit.DAY)
    return List(DAYS_IN_WEEK * GRID_ROWS) { start.plus(it, DateTimeUnit.DAY) }
}

/** The weekday order of one grid row, starting on [weekStart]. */
fun weekDays(weekStart: DayOfWeek = DayOfWeek.SUNDAY): List<DayOfWeek> =
    List(DAYS_IN_WEEK) { DayOfWeek.entries[(weekStart.ordinal + it) % DAYS_IN_WEEK] }

fun storedDate(date: LocalDate): String = date.toString()

fun parseStoredDate(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()

/** `September` — the month's English name, as a fallback until the platform names are wired in. */
fun monthName(month: Month): String = month.name.lowercase().replaceFirstChar { it.uppercase() }

fun monthTitle(month: YearMonthRef): String = "${monthName(Month(month.month))} ${month.year}"

/** `Sunday, September 20, 2026`. */
fun longDate(date: LocalDate): String =
    "${dayName(date.dayOfWeek)}, ${monthName(date.month)} ${date.day}, ${date.year}"

/** `Sep 20, 2026`. */
fun shortDate(date: LocalDate): String = "${monthName(date.month).take(3)} ${date.day}, ${date.year}"

/** `SUNDAY, SEP 20` — the day pane's overline. */
fun dayOverline(date: LocalDate): String = "${dayName(date.dayOfWeek)}, ${monthName(date.month).take(3)} ${date.day}"

fun dayName(day: DayOfWeek): String = day.name.lowercase().replaceFirstChar { it.uppercase() }

fun dayShortName(day: DayOfWeek): String = day.name.take(3)

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
    val trimmed = text.trim().lowercase()
    if (trimmed.isEmpty()) return null
    val match = CLOCK_PATTERN.matchEntire(trimmed) ?: return null
    var hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].ifEmpty { "0" }.toIntOrNull() ?: return null
    val suffix = match.groupValues[3]
    if (suffix.startsWith("p") && hour < NOON) hour += NOON
    if (suffix.startsWith("a") && hour == NOON) hour = 0
    if (hour !in 0..23 || minute !in 0..59) return null
    return LocalTime(hour, minute)
}

private val CLOCK_PATTERN = Regex("""(\d{1,2})[:.]?(\d{2})?\s*(am|pm|a|p)?""")

/** Minutes past midnight of a stored `HH:mm`, for arithmetic on the run clock. */
fun minutesOfDay(stored: String): Int? = parseStoredTime(stored)?.let { it.hour * MINUTES_PER_HOUR + it.minute }

fun timeFromMinutes(minutes: Int): String {
    val wrapped = ((minutes % (MINUTES_PER_HOUR * 24)) + MINUTES_PER_HOUR * 24) % (MINUTES_PER_HOUR * 24)
    return "${twoDigits(wrapped / MINUTES_PER_HOUR)}:${twoDigits(wrapped % MINUTES_PER_HOUR)}"
}

private fun twoDigits(value: Int): String = if (value < TWO_DIGITS) "0$value" else value.toString()

fun nowIso(): String = Clock.System.now().toString()
