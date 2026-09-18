package com.church.presenter.churchpresentermobile.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/** Plays in one bucket of the activity chart. */
data class ActivityPoint(val label: String, val songCount: Int, val verseCount: Int) {
    val total: Int get() = songCount + verseCount
}

/** How wide the activity chart's buckets are, chosen from the range's length. */
enum class ActivityGranularity {
    WEEKLY,
    MONTHLY,
    YEARLY,
}

/** Ranges up to this many days chart by week. */
private const val WEEKLY_MAX_DAYS = 90

/** Ranges up to this many days chart by month; longer ones by year. */
private const val MONTHLY_MAX_DAYS = 730

/** The "presentations over time" chart, as pure functions over a [PlayLog]. */
object ReportActivity {

    /** Bucket width for a range of [days]: weeks for a quarter, months for two years, else years. */
    fun granularityFor(days: Int): ActivityGranularity = when {
        days <= WEEKLY_MAX_DAYS -> ActivityGranularity.WEEKLY
        days <= MONTHLY_MAX_DAYS -> ActivityGranularity.MONTHLY
        else -> ActivityGranularity.YEARLY
    }

    /**
     * Plays over time, in buckets sized to the range.
     *
     * Weekly buckets start on [ReportRange.from] and step seven days, so the
     * first label is the first day of the report rather than some Thursday.
     * Monthly and yearly buckets start on the first of their month or year.
     */
    fun points(log: PlayLog, range: ReportRange, zone: TimeZone): List<ActivityPoint> {
        if (!range.isValid) return emptyList()
        val days = range.from.daysUntil(range.to) + 1
        val buckets = when (granularityFor(days)) {
            ActivityGranularity.WEEKLY -> steps(range, zone, DateTimeUnit.WEEK, ReportDates::dayLabel)
            ActivityGranularity.MONTHLY -> steps(
                range.copy(from = firstOfMonth(range.from)), zone, DateTimeUnit.MONTH, ReportDates::monthYearLabel,
            )
            ActivityGranularity.YEARLY ->
                steps(range.copy(from = LocalDate(range.from.year, 1, 1)), zone, DateTimeUnit.YEAR) {
                    it.year.toString()
                }
        }
        return buckets.map { (label, span) ->
            ActivityPoint(
                label = label,
                songCount = log.songs.count { it.at in span },
                verseCount = log.verses.count { it.at in span },
            )
        }
    }

    private fun steps(
        range: ReportRange,
        zone: TimeZone,
        unit: DateTimeUnit.DateBased,
        label: (LocalDate) -> String,
    ): List<Pair<String, ClosedRange<Long>>> {
        val out = mutableListOf<Pair<String, ClosedRange<Long>>>()
        var cursor = range.from
        val end = ReportDates.endOf(range.to, zone)
        while (cursor <= range.to) {
            val next = cursor.plus(1, unit)
            out += label(cursor) to (ReportDates.startOf(cursor, zone)..minOf(ReportDates.startOf(next, zone) - 1, end))
            cursor = next
        }
        return out
    }

    private fun firstOfMonth(day: LocalDate): LocalDate = LocalDate(day.year, day.month, 1)
}
