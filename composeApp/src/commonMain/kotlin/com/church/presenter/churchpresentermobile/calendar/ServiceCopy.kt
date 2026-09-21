package com.church.presenter.churchpresentermobile.calendar

import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.RowKind
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.model.SavedTemplate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private const val DAYS_PER_WEEK = 7

/** How a copied service repeats into the future. */
enum class RepeatRule { ONCE, WEEKLY, FORTNIGHTLY, MONTHLY }

/** The dates [count] copies land on, the first one [rule] after [from]. */
fun repeatDates(from: LocalDate, rule: RepeatRule, count: Int): List<LocalDate> {
    if (rule == RepeatRule.ONCE) return listOf(from.plus(DAYS_PER_WEEK, DateTimeUnit.DAY))
    return List(count.coerceAtLeast(1)) { index ->
        val step = index + 1
        when (rule) {
            RepeatRule.WEEKLY -> from.plus(step * DAYS_PER_WEEK, DateTimeUnit.DAY)
            RepeatRule.FORTNIGHTLY -> from.plus(step * DAYS_PER_WEEK * 2, DateTimeUnit.DAY)
            RepeatRule.MONTHLY -> from.plus(step, DateTimeUnit.MONTH)
            RepeatRule.ONCE -> from
        }
    }
}

/**
 * A copy of [source] on [date], every row re-keyed so the two services never share a row id.
 * Cue rows — the desktop's automation — are dropped when [includeCues] is false.
 */
fun copyService(
    source: PlannedService,
    date: LocalDate,
    newId: () -> String,
    includeRows: Boolean = true,
    includeCues: Boolean = true,
    seriesId: String = source.seriesId,
    at: String,
): PlannedService {
    val kept = when {
        !includeRows -> emptyList()
        includeCues -> source.rows
        else -> source.rows.filterNot { it is PlanRow.Ref && it.kind == RowKind.CUE }
    }
    val rekeyed = rekeyRows(kept, source.plannedSeconds, source.timing, newId)
    return PlannedService(
        id = newId(),
        date = storedDate(date),
        name = source.name,
        startTime = source.startTime,
        kind = source.kind,
        rows = rekeyed.rows,
        plannedSeconds = rekeyed.seconds,
        timing = rekeyed.timing,
        armed = source.armed,
        seriesId = seriesId,
        updatedAt = at,
    )
}

/** A new service on [date] from a saved template. */
fun serviceFromTemplate(
    template: SavedTemplate?,
    date: LocalDate,
    name: String,
    startTime: String,
    kind: String,
    newId: () -> String,
    at: String,
): PlannedService {
    val rekeyed = template?.let { rekeyRows(it.rows, it.plannedSeconds, it.timing, newId) }
    return PlannedService(
        id = newId(),
        date = storedDate(date),
        name = name,
        startTime = startTime,
        kind = kind,
        rows = rekeyed?.rows.orEmpty(),
        plannedSeconds = rekeyed?.seconds.orEmpty(),
        timing = rekeyed?.timing.orEmpty(),
        updatedAt = at,
    )
}

fun templateFrom(service: PlannedService, name: String, newId: () -> String): SavedTemplate = SavedTemplate(
    id = newId(),
    name = name,
    startTime = service.startTime,
    kind = service.kind,
    rows = service.rows,
    plannedSeconds = service.plannedSeconds,
    timing = service.timing,
)

private class Rekeyed(val rows: List<PlanRow>, val seconds: Map<String, Int>, val timing: Map<String, RowTiming>)

private fun rekeyRows(
    rows: List<PlanRow>,
    seconds: Map<String, Int>,
    timing: Map<String, RowTiming>,
    newId: () -> String,
): Rekeyed {
    val ids = rows.associate { it.id to newId() }
    val rekeyed = rows.map { row -> row.withId(ids.getValue(row.id)) }
    return Rekeyed(
        rows = rekeyed,
        seconds = seconds.mapNotNull { (id, value) -> ids[id]?.let { it to value } }.toMap(),
        timing = timing.mapNotNull { (id, value) -> ids[id]?.let { it to value } }.toMap(),
    )
}

fun PlanRow.withId(newId: String): PlanRow = when (this) {
    is PlanRow.Section -> copy(id = newId)
    is PlanRow.Song -> copy(id = newId)
    is PlanRow.Bible -> copy(id = newId)
    is PlanRow.Ministry -> copy(id = newId)
    is PlanRow.Preset -> copy(id = newId)
    is PlanRow.Ref -> copy(id = newId)
}
