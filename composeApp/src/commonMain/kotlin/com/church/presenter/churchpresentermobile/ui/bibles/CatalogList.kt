package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.bible_catalog_empty_hint
import churchpresentermobile.composeapp.generated.resources.bible_catalog_error_generic
import churchpresentermobile.composeapp.generated.resources.bible_catalog_error_network
import churchpresentermobile.composeapp.generated.resources.bible_catalog_error_rate_limited
import churchpresentermobile.composeapp.generated.resources.bible_catalog_stale_notice
import churchpresentermobile.composeapp.generated.resources.bibles_catalog_failed_title
import churchpresentermobile.composeapp.generated.resources.bibles_catalog_loading
import churchpresentermobile.composeapp.generated.resources.bibles_catalog_updated_days
import churchpresentermobile.composeapp.generated.resources.bibles_catalog_updated_today
import churchpresentermobile.composeapp.generated.resources.bibles_no_match
import churchpresentermobile.composeapp.generated.resources.bibles_row_get
import churchpresentermobile.composeapp.generated.resources.bibles_row_installed
import churchpresentermobile.composeapp.generated.resources.bibles_row_queued
import churchpresentermobile.composeapp.generated.resources.bibles_row_retry
import churchpresentermobile.composeapp.generated.resources.bibles_translations_count
import churchpresentermobile.composeapp.generated.resources.bibles_try_again
import com.church.presenter.churchpresentermobile.ui.verticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogSnapshot
import com.church.presenter.churchpresentermobile.bibleimport.catalog.SourceStatus
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.CatalogRow
import com.church.presenter.churchpresentermobile.viewmodel.RowStatus
import org.jetbrains.compose.resources.stringResource

/** The list itself, or what stands in for it: loading, failed, nothing matching. */
@Composable
internal fun CatalogList(
    ui: CatalogUi,
    actions: CatalogActions,
    modifier: Modifier = Modifier,
    horizontalPadding: Int = 16,
) {
    val colors = LocalAppColors.current
    when {
        ui.snapshot.isUnavailable && ui.isLoading -> Box(
            modifier.fillMaxWidth().testTag(BiblesTags.CATALOG_LOADING),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator(color = colors.accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                Text(stringResource(Res.string.bibles_catalog_loading), color = colors.muted, fontSize = 13.sp)
            }
        }
        ui.snapshot.isUnavailable -> CatalogFailed(ui.snapshot, actions.onRetry, modifier)
        else -> {
            val state = rememberLazyListState()
            LazyColumn(
                state = state,
                modifier = modifier.fillMaxWidth().verticalScrollbar(state),
                contentPadding = PaddingValues(
                    start = horizontalPadding.dp,
                    end = horizontalPadding.dp,
                    bottom = 22.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "overline") { CatalogOverline(ui) }
                if (ui.snapshot.sources.values.any { it is SourceStatus.Stale }) {
                    item(
                        key = "stale",
                    ) { Notice(stringResource(Res.string.bible_catalog_stale_notice), BiblesTags.CATALOG_STALE) }
                }
                val zefaniaLimited = ui.snapshot.sources[BibleSource.ZEFANIA] is SourceStatus.RateLimited
                if (ui.filter.source == BibleSource.ZEFANIA && zefaniaLimited) {
                    item(key = "limited") { Notice(stringResource(Res.string.bible_catalog_error_rate_limited), null) }
                }
                if (ui.view.rows.isEmpty()) {
                    item(key = "none") { NoMatch() }
                }
                items(ui.view.rows, key = { it.bible.key }) { row -> CatalogRowCard(row) { actions.onRow(row) } }
            }
        }
    }
}

/** "1,248 TRANSLATIONS ........ Catalog updated today" — or the source's name when one is chosen. */
@Composable
private fun CatalogOverline(ui: CatalogUi) {
    val colors = LocalAppColors.current
    val label = ui.filter.source?.label ?: stringResource(
        Res.string.bibles_translations_count,
        grouped(ui.view.rows.size),
    )
    val fetchedAt = ui.snapshot.fetchedAtMs
    val days = if (fetchedAt == null) 0L else (ui.nowMs - fetchedAt).coerceAtLeast(0L) / MS_PER_DAY
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Overline(label)
        Text(
            if (days == 0L) stringResource(Res.string.bibles_catalog_updated_today) else stringResource(
                Res.string.bibles_catalog_updated_days,
                days.toInt(),
            ),
            color = colors.muted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun Notice(text: String, tag: String?) {
    val colors = LocalAppColors.current
    Text(
        text,
        color = colors.secondary,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.warningTint)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun NoMatch() {
    val colors = LocalAppColors.current
    Column(
        Modifier.fillMaxWidth().testTag(BiblesTags.CATALOG_NO_MATCH).padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.bibles_no_match),
            color = colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(Res.string.bible_catalog_empty_hint),
            color = colors.muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** No source produced a list: why, and a way to try again. */
@Composable
private fun CatalogFailed(snapshot: CatalogSnapshot, onRetry: () -> Unit, modifier: Modifier) {
    val colors = LocalAppColors.current
    val statuses = snapshot.sources.values
    val reason = when {
        statuses.any { it is SourceStatus.Offline } -> stringResource(Res.string.bible_catalog_error_network)
        statuses.any { it is SourceStatus.RateLimited } -> stringResource(Res.string.bible_catalog_error_rate_limited)
        else -> stringResource(Res.string.bible_catalog_error_generic)
    }
    Column(
        modifier.fillMaxWidth().testTag(BiblesTags.CATALOG_FAILED).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(Res.string.bibles_catalog_failed_title),
            color = colors.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            reason,
            color = colors.muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        BiblesButton(
            stringResource(Res.string.bibles_try_again),
            onRetry,
            kind = ButtonKind.QUIET,
            tag = BiblesTags.CATALOG_RETRY,
            height = 46.dp,
        )
    }
}

/** One translation — design 2a's card: testament tile, name, language and format, trailing action. */
@Composable
internal fun CatalogRowCard(row: CatalogRow, selected: Boolean = false, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val bible = row.bible
    val installing = row.status is RowStatus.Installing || row.status is RowStatus.Queued
    val highlighted = installing || selected
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .testTag(BiblesTags.catalogRow(bible.key))
            .fillMaxWidth()
            .clip(shape)
            .background(if (highlighted) colors.chosenFill else colors.cardFill)
            .border(1.dp, if (highlighted) colors.chosenEdge else colors.cardEdge, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TestamentTile(bible.testament)
        Column(Modifier.weight(1f)) {
            Text(
                bible.displayName,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    bible.languageLabel,
                    color = colors.muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                FormatBadge(bible.source.format)
            }
        }
        RowAction(row)
    }
}

@Composable
private fun RowAction(row: CatalogRow) {
    val colors = LocalAppColors.current
    val tag = BiblesTags.rowAction(row.bible.key)
    when (val status = row.status) {
        RowStatus.Installed -> Pill(tag, colors.chosenFill, null) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
            Text(
                stringResource(Res.string.bibles_row_installed),
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        is RowStatus.Installing -> Column(Modifier.testTag(tag).width(72.dp)) {
            Text(
                "${(status.fraction * PERCENT).toInt()}%",
                color = colors.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            ProgressTrack(status.fraction, height = 5.dp)
        }
        RowStatus.Queued -> Pill(tag, colors.chosenFill, null) {
            Text(
                stringResource(Res.string.bibles_row_queued),
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        RowStatus.Failed -> Pill(tag, colors.danger.copy(alpha = 0.12f), colors.danger.copy(alpha = 0.3f)) {
            Text(
                stringResource(Res.string.bibles_row_retry),
                color = colors.danger,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        RowStatus.Available -> Pill(tag, colors.quietFill, colors.cardEdge) {
            Icon(
                Icons.Filled.Download,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp),
            )
            Text(
                // eBible's catalogue publishes no sizes, so those rows say what the button does.
                if (row.bible.sizeBytes > 0) sizeLabel(row.bible.sizeBytes) else stringResource(
                    Res.string.bibles_row_get,
                ),
                color = colors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun Pill(tag: String, fill: Color, edge: Color?, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .testTag(tag)
            .height(32.dp)
            .clip(shape)
            .background(fill)
            .then(if (edge != null) Modifier.border(1.dp, edge, shape) else Modifier)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}

private const val MS_PER_DAY = 24L * 60 * 60 * 1000

private const val PERCENT = 100
