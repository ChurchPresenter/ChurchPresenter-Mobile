package com.church.presenter.churchpresentermobile.calendar

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * A date as the calendar writes it: the long form over a day pane, the short form on a card, and
 * the weekday names down the month grid.
 *
 * Beside [CalendarDates], which is the grid's arithmetic. Nothing here decides anything; every
 * function is a date in, a string out.
 */

/** How many letters an abbreviated month and day name keep: `Sep`, `SUN`. */
private const val MONTH_ABBREVIATION = 3
private const val DAY_ABBREVIATION = 3

/** `Sunday, September 20, 2026`. */
fun longDate(date: LocalDate): String =
    "${dayName(date.dayOfWeek)}, ${monthName(date.month)} ${date.day}, ${date.year}"

/** `Sep 20, 2026`. */
fun shortDate(date: LocalDate): String =
    "${monthName(date.month).take(MONTH_ABBREVIATION)} ${date.day}, ${date.year}"

/** `SUNDAY, SEP 20` — the day pane's overline. */
fun dayOverline(date: LocalDate): String =
    "${dayName(date.dayOfWeek)}, ${monthName(date.month).take(MONTH_ABBREVIATION)} ${date.day}"

fun dayName(day: DayOfWeek): String = day.name.lowercase().replaceFirstChar { it.uppercase() }

fun dayShortName(day: DayOfWeek): String = day.name.take(DAY_ABBREVIATION)
