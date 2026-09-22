package com.church.presenter.churchpresentermobile.calendar

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
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

fun nowIso(): String = Clock.System.now().toString()

