package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.ActivityPoint
import com.church.presenter.churchpresentermobile.model.ReportDates
import com.church.presenter.churchpresentermobile.model.ReportRange
import com.church.presenter.churchpresentermobile.model.SongSummary
import com.church.presenter.churchpresentermobile.model.VerseSummary
import kotlinx.datetime.TimeZone

/** MIME type of the CSV export, for the share sheet. */
const val CSV_MIME_TYPE = "text/csv"

/** MIME type of the workbook export, for the share sheet. */
const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/**
 * The CCLI report as files.
 *
 * Column for column the desktop's `StatisticsManager.exportCcliCsv` and
 * `exportFilteredXls`, so a church running both can put the two side by side.
 */
object CcliExport {

    /** RFC 4180: every field quoted, embedded quotes doubled. */
    internal fun csvQuote(text: String): String = "\"" + text.replace("\"", "\"\"") + "\""

    /** The songs sheet as CSV — the file CCLI's reporting page accepts. */
    fun csv(songs: List<SongSummary>, zone: TimeZone): String = buildString {
        appendLine("Title,Author,Songbook,Song Number,CCLI Number,Times Used,First Used,Last Used")
        for (song in songs) {
            val c = song.credit
            append(csvQuote(c.title)).append(',')
            append(csvQuote(c.author)).append(',')
            append(csvQuote(c.songbook)).append(',')
            append(csvQuote(c.number)).append(',')
            append(csvQuote(c.ccliNumber)).append(',')
            append(song.count).append(',')
            append(date(song.firstUsed, zone)).append(',')
            appendLine(date(song.lastUsed, zone))
        }
    }

    /** Songs, verses and activity, one sheet each. */
    fun xlsx(
        songs: List<SongSummary>,
        verses: List<VerseSummary>,
        activity: List<ActivityPoint>,
        zone: TimeZone,
    ): ByteArray =
        XlsxWriter.write(listOf(songsSheet(songs, zone), versesSheet(verses, zone), activitySheet(activity)))

    private fun songsSheet(songs: List<SongSummary>, zone: TimeZone) = XlsxSheet(
        name = "Songs",
        header = listOf(
            "Rank", "Title", "Author", "Songbook", "Song #", "CCLI #", "Times Used", "First Used", "Last Used",
        ),
        rows = songs.mapIndexed { rank, song ->
            val c = song.credit
            listOf(
                number(rank + 1), text(c.title), text(c.author), text(c.songbook), text(c.number),
                text(c.ccliNumber), number(song.count),
                text(date(song.firstUsed, zone)), text(date(song.lastUsed, zone)),
            )
        },
    )

    private fun versesSheet(verses: List<VerseSummary>, zone: TimeZone) = XlsxSheet(
        name = "Bible Verses",
        header = listOf("Rank", "Bible", "Book", "Chapter", "Verse", "Times Used", "First Used", "Last Used"),
        rows = verses.mapIndexed { rank, verse ->
            val c = verse.credit
            listOf(
                number(rank + 1), text(c.bibleName), text(c.bookName), number(c.chapter), number(c.verse),
                number(verse.count), text(date(verse.firstUsed, zone)), text(date(verse.lastUsed, zone)),
            )
        },
    )

    private fun activitySheet(activity: List<ActivityPoint>) = XlsxSheet(
        name = "Activity",
        header = listOf("Period", "Song Presentations", "Bible Verse Presentations", "Total"),
        rows = activity.map { listOf(text(it.label), number(it.songCount), number(it.verseCount), number(it.total)) },
    )

    /** "CCLI-report-2026-01-01-to-2026-03-31" — the export's name, before its extension. */
    fun fileStem(range: ReportRange): String =
        "CCLI-report-${ReportDates.isoDate(range.from)}-to-${ReportDates.isoDate(range.to)}"

    private fun date(epochMs: Long, zone: TimeZone): String = ReportDates.isoDate(ReportDates.dayOf(epochMs, zone))

    private fun text(value: String) = XlsxCell.Text(value)

    private fun number(value: Int) = XlsxCell.Number(value.toDouble())
}
