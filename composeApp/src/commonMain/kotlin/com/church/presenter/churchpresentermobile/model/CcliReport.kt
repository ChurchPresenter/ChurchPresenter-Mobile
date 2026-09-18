package com.church.presenter.churchpresentermobile.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus

/** The quick ranges the report offers, plus one whole calendar year. */
enum class ReportPreset {
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    LAST_12_MONTHS,
    ALL_TIME,
    YEAR,
}

/**
 * A closed range of days: the report covers everything from the start of
 * [from] to the end of [to], in the device's own time zone.
 */
data class ReportRange(val from: LocalDate, val to: LocalDate) {
    val isValid: Boolean get() = from <= to
}

/** One song's plays within a range. */
data class SongSummary(
    val credit: SongCredit,
    val count: Int,
    val firstUsed: Long,
    val lastUsed: Long,
)

/** One verse's plays within a range. */
data class VerseSummary(
    val credit: VerseCredit,
    val count: Int,
    val firstUsed: Long,
    val lastUsed: Long,
)

/** How many verses of one book were shown. */
data class BookSummary(val bookName: String, val count: Int)

private const val MONTHS_IN_QUARTER = 3
private const val MONTHS_IN_HALF_YEAR = 6
private const val MONTHS_IN_YEAR = 12
private const val LAST_DAY_OF_DECEMBER = 31

/**
 * The CCLI report, as pure functions over a [PlayLog].
 *
 * Nothing here reads a clock or a zone on its own; both arrive as arguments
 * so the screen can pass the device's and a test can pass a fixed one. The
 * shapes mirror the desktop's `StatisticsManager` so the two apps' exports line
 * up column for column. The chart is [ReportActivity]; days are [ReportDates].
 */
object CcliReport {

    /**
     * The days a preset means, ending today.
     *
     * "All time" starts at the first play on record — or today, for an empty log,
     * so the pickers never open on a date nothing was ever recorded before.
     */
    fun rangeFor(preset: ReportPreset, log: PlayLog, nowMs: Long, zone: TimeZone): ReportRange {
        val today = ReportDates.dayOf(nowMs, zone)
        return when (preset) {
            ReportPreset.LAST_3_MONTHS -> ReportRange(today.minus(MONTHS_IN_QUARTER, DateTimeUnit.MONTH), today)
            ReportPreset.LAST_6_MONTHS -> ReportRange(today.minus(MONTHS_IN_HALF_YEAR, DateTimeUnit.MONTH), today)
            ReportPreset.LAST_12_MONTHS -> ReportRange(today.minus(MONTHS_IN_YEAR, DateTimeUnit.MONTH), today)
            ReportPreset.ALL_TIME -> ReportRange(log.earliest?.let { ReportDates.dayOf(it, zone) } ?: today, today)
            ReportPreset.YEAR -> yearRange(today.year)
        }
    }

    /** The whole of one calendar year. */
    fun yearRange(year: Int): ReportRange =
        ReportRange(LocalDate(year, 1, 1), LocalDate(year, MONTHS_IN_YEAR, LAST_DAY_OF_DECEMBER))

    /** The years the log has plays in, newest first — the choices under "Year". */
    fun years(log: PlayLog, zone: TimeZone): List<Int> =
        (log.songs.map { it.at } + log.verses.map { it.at })
            .map { ReportDates.dayOf(it, zone).year }
            .distinct()
            .sortedDescending()

    /** Songbooks named by any play, for the filter. Songs with no book are under an empty name. */
    fun songbooks(log: PlayLog): List<String> =
        log.songs.map { it.credit.songbook }.distinct().sortedBy { it.lowercase() }

    /** Translations named by any play, for the filter. */
    fun bibles(log: PlayLog): List<String> =
        log.verses.map { it.credit.bibleName }.distinct().sortedBy { it.lowercase() }

    /**
     * The songs played in [range], most played first.
     *
     * A song is one row per [SongCredit.key], so a title edited between two
     * plays of a library song still lands on one row.
     *
     * @param songbook Only plays from this book; null for all of them.
     */
    fun songs(log: PlayLog, range: ReportRange, zone: TimeZone, songbook: String? = null): List<SongSummary> {
        val fromMs = ReportDates.startOf(range.from, zone)
        val toMs = ReportDates.endOf(range.to, zone)
        return log.songs
            .filter { it.at in fromMs..toMs && (songbook == null || it.credit.songbook == songbook) }
            .groupBy { it.credit.key }
            .map { (_, plays) ->
                SongSummary(
                    // The newest play names the row, so an edited title shows as it is now.
                    credit = plays.maxBy { it.at }.credit,
                    count = plays.size,
                    firstUsed = plays.minOf { it.at },
                    lastUsed = plays.maxOf { it.at },
                )
            }
            .sortedWith(compareByDescending<SongSummary> { it.count }.thenBy { it.credit.title.lowercase() })
    }

    /** The verses shown in [range], most shown first. [bible] narrows to one translation. */
    fun verses(log: PlayLog, range: ReportRange, zone: TimeZone, bible: String? = null): List<VerseSummary> {
        val fromMs = ReportDates.startOf(range.from, zone)
        val toMs = ReportDates.endOf(range.to, zone)
        return log.verses
            .filter { it.at in fromMs..toMs && (bible == null || it.credit.bibleName == bible) }
            .groupBy { it.credit.key }
            .map { (_, plays) ->
                VerseSummary(
                    credit = plays.first().credit,
                    count = plays.size,
                    firstUsed = plays.minOf { it.at },
                    lastUsed = plays.maxOf { it.at },
                )
            }
            .sortedWith(
                compareByDescending<VerseSummary> { it.count }
                    .thenBy { it.credit.bookName }
                    .thenBy { it.credit.chapter }
                    .thenBy { it.credit.verse },
            )
    }

    /** The books behind [verses], by how many verse plays each had. */
    fun topBooks(verses: List<VerseSummary>): List<BookSummary> =
        verses.groupBy { it.credit.bookName }
            .map { (book, rows) -> BookSummary(book, rows.sumOf { it.count }) }
            .sortedWith(compareByDescending<BookSummary> { it.count }.thenBy { it.bookName })
}
