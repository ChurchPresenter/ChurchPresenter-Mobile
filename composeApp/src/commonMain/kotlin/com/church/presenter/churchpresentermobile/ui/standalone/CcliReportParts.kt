package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.report_bible_verses
import churchpresentermobile.composeapp.generated.resources.report_busiest_period
import churchpresentermobile.composeapp.generated.resources.report_songs_presented
import churchpresentermobile.composeapp.generated.resources.report_songs_summary
import churchpresentermobile.composeapp.generated.resources.report_top_books
import churchpresentermobile.composeapp.generated.resources.report_top_songs
import churchpresentermobile.composeapp.generated.resources.report_total_plays
import churchpresentermobile.composeapp.generated.resources.report_unique_songs
import churchpresentermobile.composeapp.generated.resources.report_unique_verses
import churchpresentermobile.composeapp.generated.resources.report_verses
import churchpresentermobile.composeapp.generated.resources.report_verses_summary
import com.church.presenter.churchpresentermobile.model.BookSummary
import com.church.presenter.churchpresentermobile.model.SongSummary
import com.church.presenter.churchpresentermobile.model.VerseSummary
import com.church.presenter.churchpresentermobile.ui.OverlineRow
import com.church.presenter.churchpresentermobile.ui.theme.AppColors
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.ReportData
import com.church.presenter.churchpresentermobile.viewmodel.ReportTab
import org.jetbrains.compose.resources.stringResource

/** How faint the tint behind a coloured tile or bar is. */
private const val TINT_ALPHA = 0.12f

/** Between an author and a songbook, a translation and a date. */
private const val SEPARATOR = " · "

/** Songs are drawn in the accent, verses in the schedule's Bible blue — the same split the schedule uses. */
internal val AppColors.verseTint: Color get() = scheduleBibleFg

/** One number with its caption, tinted for what it counts. */
@Composable
internal fun StatTile(value: String, label: String, tint: Color, modifier: Modifier = Modifier, tag: String? = null) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier = modifier
            .then(tag?.let { Modifier.testTag(it) } ?: Modifier)
            // One node — "36 Total plays" — for a screen reader and for a test alike.
            .semantics(mergeDescendants = true) {}
            .clip(shape)
            .background(tint.copy(alpha = TINT_ALPHA))
            .border(1.dp, tint.copy(alpha = TINT_ALPHA * 2), shape)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(value, color = tint, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
        Text(label, color = colors.muted, fontSize = 11.sp)
    }
}

/** The totals for the open view. */
@Composable
internal fun ReportTiles(report: ReportData) {
    val colors = LocalAppColors.current
    when (report.filters.tab) {
        ReportTab.SONGS -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                value = report.songPlays.toString(),
                label = stringResource(Res.string.report_total_plays),
                tint = colors.accent,
                modifier = Modifier.weight(1f),
                tag = ReportTags.SONG_PLAYS,
            )
            StatTile(
                value = report.songs.size.toString(),
                label = stringResource(Res.string.report_unique_songs),
                tint = colors.accent,
                modifier = Modifier.weight(1f),
                tag = ReportTags.UNIQUE_SONGS,
            )
        }
        ReportTab.BIBLE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                value = report.versePlays.toString(),
                label = stringResource(Res.string.report_total_plays),
                tint = colors.verseTint,
                modifier = Modifier.weight(1f),
                tag = ReportTags.VERSE_PLAYS,
            )
            StatTile(
                value = report.verses.size.toString(),
                label = stringResource(Res.string.report_unique_verses),
                tint = colors.verseTint,
                modifier = Modifier.weight(1f),
                tag = ReportTags.UNIQUE_VERSES,
            )
        }
        ReportTab.ACTIVITY -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    value = report.songPlays.toString(),
                    label = stringResource(Res.string.report_songs_presented),
                    tint = colors.accent,
                    modifier = Modifier.weight(1f),
                    tag = ReportTags.SONG_PLAYS,
                )
                StatTile(
                    value = report.versePlays.toString(),
                    label = stringResource(Res.string.report_bible_verses),
                    tint = colors.verseTint,
                    modifier = Modifier.weight(1f),
                    tag = ReportTags.VERSE_PLAYS,
                )
            }
            StatTile(
                value = report.busiest?.let { "${it.label} · ${it.total}" } ?: "—",
                label = stringResource(Res.string.report_busiest_period),
                tint = colors.warning,
                modifier = Modifier.fillMaxWidth(),
                tag = ReportTags.BUSIEST,
            )
        }
    }
}

/** The phone's song list: rank, title, author · book, count, and when it was first and last used. */
@Composable
internal fun SongCards(songs: List<SongSummary>) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        OverlineRow(label = stringResource(Res.string.report_top_songs))
        songs.forEachIndexed { index, song ->
            val credit = song.credit
            Column(
                modifier = Modifier
                    .testTag(ReportTags.song(index + 1))
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    RankNumber(index + 1)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = credit.title,
                            color = colors.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val byline = listOf(credit.author, credit.songbook).filter { it.isNotBlank() }
                        Text(
                            text = byline.joinToString(SEPARATOR),
                            color = colors.muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(song.count.toString(), color = colors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** The phone's verse list: rank, reference, translation and dates, count. */
@Composable
internal fun VerseRows(verses: List<VerseSummary>) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OverlineRow(label = stringResource(Res.string.report_verses))
        verses.forEachIndexed { index, verse ->
            Row(
                modifier = Modifier
                    .testTag(ReportTags.verse(index + 1))
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(13.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RankNumber(index + 1)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = verse.credit.reference,
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (verse.credit.bibleName.isNotBlank()) {
                        Text(
                            text = verse.credit.bibleName,
                            color = colors.muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(verse.count.toString(), color = colors.verseTint, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RankNumber(rank: Int) {
    val colors = LocalAppColors.current
    Text(
        text = rank.toString(),
        color = colors.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.width(18.dp),
    )
}

/** One row of a [RankedBars] list. */
internal data class RankedRow(val name: String, val count: Int, val tag: String)

/** One bar per row, as long as its share of the largest count. */
@Composable
internal fun RankedBars(title: String, rows: List<RankedRow>, tint: Color, subtitle: String? = null) {
    val colors = LocalAppColors.current
    val most = rows.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OverlineRow(label = title)
        if (subtitle != null) Text(subtitle, color = colors.muted, fontSize = 12.sp)
        rows.forEach { row ->
            Column(modifier = Modifier.testTag(row.tag), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = row.name,
                        color = colors.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(row.count.toString(), color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                        .background(colors.inputBg),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(row.count.toFloat() / most)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(tint),
                    )
                }
            }
        }
    }
}

/** The tablet's left pane on the Songs view. */
@Composable
internal fun SongRankings(report: ReportData) {
    RankedBars(
        title = stringResource(Res.string.report_top_songs),
        subtitle = stringResource(Res.string.report_songs_summary, report.songs.size, report.songPlays),
        rows = report.songs.take(TOP_RANKINGS).mapIndexed { index, song ->
            RankedRow(song.credit.title, song.count, ReportTags.rankedSong(index + 1))
        },
        tint = LocalAppColors.current.accent,
    )
}

/** The tablet's left pane on the Bible view. */
@Composable
internal fun BookRankings(report: ReportData) {
    RankedBars(
        title = stringResource(Res.string.report_top_books),
        subtitle = stringResource(Res.string.report_verses_summary, report.verses.size, report.versePlays),
        rows = report.books.map { RankedRow(it.bookName, it.count, ReportTags.book(it.bookName)) },
        tint = LocalAppColors.current.verseTint,
    )
}

/** The phone's book list on the Bible view. */
@Composable
internal fun BookBars(books: List<BookSummary>) {
    RankedBars(
        title = stringResource(Res.string.report_top_books),
        rows = books.map { RankedRow(it.bookName, it.count, ReportTags.book(it.bookName)) },
        tint = LocalAppColors.current.verseTint,
    )
}

/** How many songs the tablet's rankings pane lists; the table beside it has them all. */
private const val TOP_RANKINGS = 10
