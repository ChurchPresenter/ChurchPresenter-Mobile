package com.church.presenter.churchpresentermobile.calendar

import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.RowTiming

private const val SECONDS_PER_MINUTE = 60
private const val MAX_DURATION_MINUTES = 24 * 60

/** `4:30` ⇄ 270 seconds. Accepts `4:30`, `4`, `0:45`, `1:02:00`. */
fun formatDuration(seconds: Int): String {
    val total = seconds.coerceAtLeast(0)
    val hours = total / (SECONDS_PER_MINUTE * SECONDS_PER_MINUTE)
    val minutes = (total / SECONDS_PER_MINUTE) % SECONDS_PER_MINUTE
    val secs = total % SECONDS_PER_MINUTE
    val mm = minutes.toString().padStart(if (hours > 0) 2 else 1, '0')
    val ss = secs.toString().padStart(2, '0')
    return if (hours > 0) "$hours:$mm:$ss" else "$mm:$ss"
}

fun parseDuration(text: String): Int? {
    val parts = text.trim().split(':')
    if (parts.isEmpty() || parts.any { it.isBlank() || it.any { c -> !c.isDigit() } }) return null
    val numbers = parts.map { it.toInt() }
    val seconds = when (numbers.size) {
        1 -> numbers[0] * SECONDS_PER_MINUTE
        2 -> numbers[0] * SECONDS_PER_MINUTE + numbers[1]
        3 -> (numbers[0] * SECONDS_PER_MINUTE + numbers[1]) * SECONDS_PER_MINUTE + numbers[2]
        else -> return null
    }
    return seconds.takeIf { it in 0..MAX_DURATION_MINUTES * SECONDS_PER_MINUTE }
}

/** `69 min` — the header's whole-service length. */
fun minutesText(seconds: Int): String = "${(seconds + SECONDS_PER_MINUTE / 2) / SECONDS_PER_MINUTE} min"

/** One row with its projected wall-clock time, in minutes past midnight, or null when nothing pins it. */
data class ClockedRow(val row: PlanRow, val startMinutes: Int?, val seconds: Int?, val timing: RowTiming)

/**
 * Each row's projected clock time: the service start, then each timed row's length added on, with
 * a row that starts on its own pinning the clock to its time. Sections take no time.
 */
fun clockedRows(service: PlannedService): List<ClockedRow> {
    var clock = minutesOfDay(service.startTime)
    var carriedSeconds = 0
    return service.rows.map { row ->
        val timing = service.timingFor(row.id)
        val seconds = service.plannedSecondsFor(row.id)
        if (row is PlanRow.Section) return@map ClockedRow(row, null, null, timing)
        val pinned = timing.startAt.takeIf { it.isNotEmpty() }?.let(::minutesOfDay)
        if (pinned != null) {
            clock = pinned
            carriedSeconds = 0
        } else if (clock != null) {
            clock += carriedSeconds / SECONDS_PER_MINUTE
            carriedSeconds %= SECONDS_PER_MINUTE
        }
        val start = clock
        carriedSeconds += (seconds ?: 0) * (if (timing.repeats > 1) timing.repeats else 1)
        ClockedRow(row, start, seconds, timing)
    }
}

/** Planned seconds across every timed row. */
fun totalSeconds(service: PlannedService): Int =
    service.rows.filterNot { it is PlanRow.Section }.sumOf { row ->
        val repeats = service.timingFor(row.id).repeats.takeIf { it > 1 } ?: 1
        (service.plannedSecondsFor(row.id) ?: 0) * repeats
    }

fun autoStartCount(service: PlannedService): Int = service.timing.values.count { it.startsWithoutCue() }

/** The projected end, from the start plus the total, in minutes past midnight. */
fun endMinutes(service: PlannedService): Int? =
    minutesOfDay(service.startTime)?.let { it + totalSeconds(service) / SECONDS_PER_MINUTE }
