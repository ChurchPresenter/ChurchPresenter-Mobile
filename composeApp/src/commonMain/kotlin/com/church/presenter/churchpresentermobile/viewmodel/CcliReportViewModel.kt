package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.library.CSV_MIME_TYPE
import com.church.presenter.churchpresentermobile.library.CcliExport
import com.church.presenter.churchpresentermobile.library.PlayLogRepository
import com.church.presenter.churchpresentermobile.library.XLSX_MIME_TYPE
import com.church.presenter.churchpresentermobile.model.ActivityPoint
import com.church.presenter.churchpresentermobile.model.BookSummary
import com.church.presenter.churchpresentermobile.model.CcliReport
import com.church.presenter.churchpresentermobile.model.PlayLog
import com.church.presenter.churchpresentermobile.model.ReportActivity
import com.church.presenter.churchpresentermobile.model.ReportPreset
import com.church.presenter.churchpresentermobile.model.ReportRange
import com.church.presenter.churchpresentermobile.model.SongSummary
import com.church.presenter.churchpresentermobile.model.VerseSummary
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TAG = "CcliReportViewModel"

/** The three views of the report. */
enum class ReportTab {
    SONGS,
    BIBLE,
    ACTIVITY,
}

/**
 * What the operator has asked to see.
 *
 * @param preset The quick range in force, or null once a date was edited by hand.
 * @param range The days covered — always concrete, so the pickers have a value to open on.
 * @param songbook Only songs from this book; null for every book.
 * @param bible Only verses from this translation; null for every translation.
 */
data class ReportFilters(
    val tab: ReportTab = ReportTab.SONGS,
    val preset: ReportPreset? = ReportPreset.LAST_3_MONTHS,
    val range: ReportRange,
    val songbook: String? = null,
    val bible: String? = null,
)

/** The report, computed for the current [ReportFilters]. */
data class ReportData(
    val filters: ReportFilters,
    val songs: List<SongSummary> = emptyList(),
    val verses: List<VerseSummary> = emptyList(),
    val books: List<BookSummary> = emptyList(),
    val activity: List<ActivityPoint> = emptyList(),
    /** Every songbook with a play on record, for the filter — not just those in range. */
    val songbooks: List<String> = emptyList(),
    val bibles: List<String> = emptyList(),
    /** Years with a play in them, newest first, for the Year menu. */
    val years: List<Int> = emptyList(),
    /** True when nothing has ever been recorded, which is a different screen from "nothing in range". */
    val isLogEmpty: Boolean = true,
) {
    val songPlays: Int get() = songs.sumOf { it.count }
    val versePlays: Int get() = verses.sumOf { it.count }

    /** The bucket with the most plays, for the "busiest period" tile. Null when nothing was played. */
    val busiest: ActivityPoint? get() = activity.filter { it.total > 0 }.maxByOrNull { it.total }
}

/**
 * The CCLI report for this device's own projections.
 *
 * Reads the play log and never writes to it — recording is
 * [com.church.presenter.churchpresentermobile.present.PlayRecorder]'s job —
 * except to empty it when the operator asks.
 *
 * @param now The clock, injected so "last 3 months" is testable.
 * @param zone The zone days are counted in; the device's, unless a test says otherwise.
 */
@OptIn(ExperimentalTime::class)
class CcliReportViewModel(
    private val repository: PlayLogRepository,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val _filters = MutableStateFlow(
        ReportFilters(range = CcliReport.rangeFor(ReportPreset.LAST_3_MONTHS, repository.log.value, now(), zone))
    )

    /** What the operator has asked to see. */
    val filters: StateFlow<ReportFilters> = _filters.asStateFlow()

    /** The report, recomputed whenever a play lands or a filter changes. */
    val report: StateFlow<ReportData> = combine(repository.log, _filters) { log, filters ->
        reportFor(log, filters, zone)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, reportFor(repository.log.value, _filters.value, zone))

    fun setTab(tab: ReportTab) = _filters.update { it.copy(tab = tab) }

    /** Applies a quick range. [ReportPreset.YEAR] means the current year; see [setYear] for another. */
    fun setPreset(preset: ReportPreset) = _filters.update {
        it.copy(preset = preset, range = CcliReport.rangeFor(preset, repository.log.value, now(), zone))
    }

    /** The whole of [year]. */
    fun setYear(year: Int) = _filters.update {
        it.copy(preset = ReportPreset.YEAR, range = CcliReport.yearRange(year))
    }

    /**
     * Moves the start of the range. A start after the end drags the end along
     * with it, so the range can never be turned inside out from the pickers.
     */
    fun setFrom(day: LocalDate) = _filters.update {
        it.copy(preset = null, range = ReportRange(day, maxOf(day, it.range.to)))
    }

    /** Moves the end of the range, dragging the start along if it would otherwise pass it. */
    fun setTo(day: LocalDate) = _filters.update {
        it.copy(preset = null, range = ReportRange(minOf(day, it.range.from), day))
    }

    fun setSongbook(songbook: String?) = _filters.update { it.copy(songbook = songbook) }

    fun setBible(bible: String?) = _filters.update { it.copy(bible = bible) }

    /** Empties the log. The screen has already asked; there is no undo. */
    fun clearStatistics() {
        repository.clear()
        Logger.d(TAG, "clearStatistics — every play forgotten")
    }

    /**
     * The report as a file to share, in [format], honouring the filters in force.
     *
     * The CSV is the songs sheet alone — the file CCLI's reporting page takes;
     * the workbook carries songs, verses and activity.
     */
    fun export(format: ReportExportFormat): ReportExport {
        val data = report.value
        val stem = CcliExport.fileStem(data.filters.range)
        return when (format) {
            ReportExportFormat.CSV -> ReportExport(
                bytes = CcliExport.csv(data.songs, zone).encodeToByteArray(),
                fileName = "$stem.csv",
                mimeType = CSV_MIME_TYPE,
            )
            ReportExportFormat.XLSX -> ReportExport(
                bytes = CcliExport.xlsx(data.songs, data.verses, data.activity, zone),
                fileName = "$stem.xlsx",
                mimeType = XLSX_MIME_TYPE,
            )
        }
    }
}

/** The two files the report can become. */
enum class ReportExportFormat {
    CSV,
    XLSX,
}

/** A file ready for the share sheet. */
class ReportExport(val bytes: ByteArray, val fileName: String, val mimeType: String)

/** The report for [filters] over [log]. A plain function so the ViewModel stays about state. */
internal fun reportFor(log: PlayLog, filters: ReportFilters, zone: TimeZone): ReportData {
    val verses = CcliReport.verses(log, filters.range, zone, filters.bible)
    return ReportData(
        filters = filters,
        songs = CcliReport.songs(log, filters.range, zone, filters.songbook),
        verses = verses,
        books = CcliReport.topBooks(verses),
        activity = ReportActivity.points(log, filters.range, zone),
        songbooks = CcliReport.songbooks(log),
        bibles = CcliReport.bibles(log),
        years = CcliReport.years(log, zone),
        isLogEmpty = log.isEmpty,
    )
}
