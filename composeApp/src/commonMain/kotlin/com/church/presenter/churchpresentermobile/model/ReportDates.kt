package com.church.presenter.churchpresentermobile.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val MONTH_ABBREVIATION_LENGTH = 3
private const val SHORT_YEAR_DIGITS = 2

/**
 * Days and their labels, for the CCLI report.
 *
 * Every conversion takes the zone as an argument rather than reading the
 * device's, so the report and its tests count the same days.
 */
@OptIn(ExperimentalTime::class)
object ReportDates {

    /** The day [epochMs] falls on in [zone]. */
    fun dayOf(epochMs: Long, zone: TimeZone): LocalDate =
        Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone).date

    /** The first instant of [day] in [zone], in epoch milliseconds. */
    fun startOf(day: LocalDate, zone: TimeZone): Long = day.atStartOfDayIn(zone).toEpochMilliseconds()

    /** The last instant of [day] in [zone], in epoch milliseconds. */
    fun endOf(day: LocalDate, zone: TimeZone): Long = startOf(day.plus(1, DateTimeUnit.DAY), zone) - 1

    /** "Jan" — English, the way the export headers are English. */
    fun monthLabel(day: LocalDate): String =
        day.month.name.take(MONTH_ABBREVIATION_LENGTH).lowercase().replaceFirstChar { it.uppercase() }

    /** "Jan 26" — a monthly chart tick. */
    fun monthYearLabel(day: LocalDate): String =
        "${monthLabel(day)} ${day.year.toString().takeLast(SHORT_YEAR_DIGITS)}"

    /** "Jan 4" — a weekly chart tick. */
    fun dayLabel(day: LocalDate): String = "${monthLabel(day)} ${day.day}"

    /** "Jan 4, 2026" — a date in the report's rows. */
    fun dateLabel(day: LocalDate): String = "${dayLabel(day)}, ${day.year}"

    /** "2026-01-04" — a date in an export, sortable and unambiguous. */
    fun isoDate(day: LocalDate): String = day.toString()
}
