package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.report_col_author
import churchpresentermobile.composeapp.generated.resources.report_col_bible
import churchpresentermobile.composeapp.generated.resources.report_col_ccli
import churchpresentermobile.composeapp.generated.resources.report_col_first
import churchpresentermobile.composeapp.generated.resources.report_col_last
import churchpresentermobile.composeapp.generated.resources.report_col_rank
import churchpresentermobile.composeapp.generated.resources.report_col_songbook
import churchpresentermobile.composeapp.generated.resources.report_col_title
import churchpresentermobile.composeapp.generated.resources.report_col_used
import churchpresentermobile.composeapp.generated.resources.report_col_verse
import com.church.presenter.churchpresentermobile.model.SongSummary
import com.church.presenter.churchpresentermobile.model.VerseSummary
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/** A column of the tablet table: its heading and how wide it is. */
private data class ReportColumn(val title: String, val width: Dp, val alignEnd: Boolean = false)

private val RANK_WIDTH = 36.dp
private val TITLE_WIDTH = 240.dp
private val TEXT_WIDTH = 150.dp
private val NUMBER_WIDTH = 80.dp
private val DATE_WIDTH = 110.dp

/** The full song report, one row per song — the same columns as the desktop's table. */
@Composable
internal fun SongTable(songs: List<SongSummary>) {
    val columns = listOf(
        ReportColumn(stringResource(Res.string.report_col_rank), RANK_WIDTH),
        ReportColumn(stringResource(Res.string.report_col_title), TITLE_WIDTH),
        ReportColumn(stringResource(Res.string.report_col_author), TEXT_WIDTH),
        ReportColumn(stringResource(Res.string.report_col_songbook), TEXT_WIDTH),
        ReportColumn(stringResource(Res.string.report_col_ccli), NUMBER_WIDTH),
        ReportColumn(stringResource(Res.string.report_col_used), NUMBER_WIDTH, alignEnd = true),
        ReportColumn(stringResource(Res.string.report_col_first), DATE_WIDTH),
        ReportColumn(stringResource(Res.string.report_col_last), DATE_WIDTH),
    )
    val accent = LocalAppColors.current.accent
    ReportTable(
        columns = columns,
        rowCount = songs.size,
        tagFor = ReportTags::song,
        cells = { index ->
            val song = songs[index]
            listOf(
                (index + 1).toString(), song.credit.title, song.credit.author, song.credit.songbook,
                song.credit.ccliNumber.ifBlank { "—" }, song.count.toString(),
                reportDate(song.firstUsed), reportDate(song.lastUsed),
            )
        },
        emphasis = { column -> if (column == USED_COLUMN_SONGS) accent else null },
    )
}

/** The full verse report, one row per verse. */
@Composable
internal fun VerseTable(verses: List<VerseSummary>) {
    val columns = listOf(
        ReportColumn(stringResource(Res.string.report_col_rank), RANK_WIDTH),
        ReportColumn(stringResource(Res.string.report_col_verse), TEXT_WIDTH),
        ReportColumn(stringResource(Res.string.report_col_bible), TITLE_WIDTH),
        ReportColumn(stringResource(Res.string.report_col_used), NUMBER_WIDTH, alignEnd = true),
        ReportColumn(stringResource(Res.string.report_col_first), DATE_WIDTH),
        ReportColumn(stringResource(Res.string.report_col_last), DATE_WIDTH),
    )
    val tint = LocalAppColors.current.verseTint
    ReportTable(
        columns = columns,
        rowCount = verses.size,
        tagFor = ReportTags::verse,
        cells = { index ->
            val verse = verses[index]
            listOf(
                (index + 1).toString(), verse.credit.reference, verse.credit.bibleName, verse.count.toString(),
                reportDate(verse.firstUsed), reportDate(verse.lastUsed),
            )
        },
        emphasis = { column -> if (column == USED_COLUMN_VERSES) tint else null },
    )
}

private const val USED_COLUMN_SONGS = 5
private const val USED_COLUMN_VERSES = 3

/**
 * A header row and a lazy list of rows, scrolling sideways together when the
 * columns are wider than the pane.
 *
 * @param tagFor Names row [rank] (1-based) for a UI test.
 * @param emphasis The colour a column's cells are drawn in, or null for the default.
 */
@Composable
private fun ReportTable(
    columns: List<ReportColumn>,
    rowCount: Int,
    tagFor: (Int) -> String,
    cells: (Int) -> List<String>,
    emphasis: (Int) -> Color?,
) {
    val colors = LocalAppColors.current
    val sideways = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().horizontalScroll(sideways)) {
        Row(modifier = Modifier.background(colors.surface).padding(horizontal = 16.dp, vertical = 10.dp)) {
            columns.forEach { column ->
                Text(
                    text = column.title.uppercase(),
                    color = colors.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.05.em,
                    modifier = Modifier.width(column.width),
                )
            }
        }
        HorizontalDivider(color = colors.borderSubtle)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(List(rowCount) { it }) { index, _ ->
                Row(modifier = Modifier.testTag(tagFor(index + 1)).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    cells(index).forEachIndexed { columnIndex, cell ->
                        val column = columns[columnIndex]
                        val tint = emphasis(columnIndex)
                        val bold = tint != null || columnIndex == 1
                        Text(
                            text = cell,
                            color = tint ?: colors.text,
                            fontSize = 13.sp,
                            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(column.width).padding(end = 8.dp),
                        )
                    }
                }
                HorizontalDivider(color = colors.borderSubtle)
            }
        }
    }
}
