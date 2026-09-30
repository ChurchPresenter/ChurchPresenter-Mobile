package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.bible_catalog_empty_hint
import churchpresentermobile.composeapp.generated.resources.bible_catalog_error_generic
import churchpresentermobile.composeapp.generated.resources.bible_catalog_error_network
import churchpresentermobile.composeapp.generated.resources.bible_catalog_error_rate_limited
import churchpresentermobile.composeapp.generated.resources.bible_catalog_stale_notice
import churchpresentermobile.composeapp.generated.resources.bibles_all_languages
import churchpresentermobile.composeapp.generated.resources.bibles_catalog_failed_title
import churchpresentermobile.composeapp.generated.resources.bibles_catalog_loading
import churchpresentermobile.composeapp.generated.resources.bibles_catalog_subtitle
import churchpresentermobile.composeapp.generated.resources.bibles_catalog_updated_days
import churchpresentermobile.composeapp.generated.resources.bibles_catalog_updated_today
import churchpresentermobile.composeapp.generated.resources.bibles_done
import churchpresentermobile.composeapp.generated.resources.bibles_get
import churchpresentermobile.composeapp.generated.resources.bibles_language
import churchpresentermobile.composeapp.generated.resources.bibles_no_match
import churchpresentermobile.composeapp.generated.resources.bibles_on_this_device
import churchpresentermobile.composeapp.generated.resources.bibles_row_get
import churchpresentermobile.composeapp.generated.resources.bibles_row_installed
import churchpresentermobile.composeapp.generated.resources.bibles_row_queued
import churchpresentermobile.composeapp.generated.resources.bibles_row_retry
import churchpresentermobile.composeapp.generated.resources.bibles_search_catalog
import churchpresentermobile.composeapp.generated.resources.bibles_search_catalog_count
import churchpresentermobile.composeapp.generated.resources.bibles_search_languages
import churchpresentermobile.composeapp.generated.resources.bibles_source_all
import churchpresentermobile.composeapp.generated.resources.bibles_source_holy_short
import churchpresentermobile.composeapp.generated.resources.bibles_translations_count
import churchpresentermobile.composeapp.generated.resources.bibles_try_again
import churchpresentermobile.composeapp.generated.resources.cd_back
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogSnapshot
import com.church.presenter.churchpresentermobile.bibleimport.catalog.SourceStatus
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.CatalogRow
import com.church.presenter.churchpresentermobile.viewmodel.CatalogView
import com.church.presenter.churchpresentermobile.viewmodel.LanguageOption
import com.church.presenter.churchpresentermobile.viewmodel.RowStatus
import org.jetbrains.compose.resources.stringResource

/**
 * Get Bibles on a phone — design 2a–2f: header, source tabs, search, language, then the list.
 *
 * The header block is fixed and the list scrolls under it, as in the design.
 */
@Composable
internal fun CatalogScreen(ui: CatalogUi, actions: CatalogActions, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    var picking by remember { mutableStateOf(false) }
    Column(modifier.testTag(BiblesTags.CATALOG).fillMaxSize().background(colors.background)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.background)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        ) {
            BackHeader(
                title = stringResource(Res.string.bibles_get),
                subtitle = stringResource(Res.string.bibles_catalog_subtitle),
                onBack = onBack,
                backTag = BiblesTags.CATALOG_BACK,
            )
            Box(Modifier.height(12.dp))
            SourceTabs(ui.filter.source, actions.onSource, fontSize = 11)
            Box(Modifier.height(10.dp))
            CatalogSearch(ui.filter.query, actions.onQuery, stringResource(Res.string.bibles_search_catalog))
            Box(Modifier.height(10.dp))
            LanguageButton(ui.view, ui.filter.language, open = picking) { picking = true }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardEdge))
        CatalogList(ui, actions, Modifier.weight(1f))
    }
    if (picking) {
        LanguageSheet(ui.view, ui.filter.language, onPick = actions.onLanguage, onDismiss = { picking = false })
    }
}

/** The back arrow, a title and a one-line subtitle — the design's sub-page header. */
@Composable
internal fun BackHeader(title: String, subtitle: String, onBack: () -> Unit, backTag: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.testTag(backTag).size(28.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.cd_back),
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
            Text(subtitle, color = colors.dim.takeIf { !colors.isDark } ?: colors.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

/** All · eBible.org · Zefania · Holy Bible — a segmented track with an elevated pill. */
@Composable
internal fun SourceTabs(selected: BibleSource?, onSelect: (BibleSource?) -> Unit, fontSize: Int) {
    val colors = LocalAppColors.current
    val options: List<Pair<BibleSource?, String>> = listOf(
        null to stringResource(Res.string.bibles_source_all),
        BibleSource.EBIBLE to BibleSource.EBIBLE.label,
        BibleSource.ZEFANIA to BibleSource.ZEFANIA.label,
        BibleSource.BEBLIA to stringResource(Res.string.bibles_source_holy_short),
    )
    val track = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(track)
            .background(if (colors.isDark) Color(0x0DFFFFFF) else Color(0x0A000000))
            .border(1.dp, colors.cardEdge, track)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (source, label) ->
            val active = source == selected
            Box(
                Modifier
                    .weight(1f)
                    .testTag(BiblesTags.source(source?.name ?: "ALL"))
                    .semantics { this.selected = active }
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        when {
                            !active -> Color.Transparent
                            colors.isDark -> Color(0x1FFFFFFF)
                            else -> Color.White
                        },
                    )
                    .clickable { onSelect(source) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) colors.text else if (colors.isDark) colors.muted else colors.dim,
                    fontSize = fontSize.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun CatalogSearch(query: String, onQuery: (String) -> Unit, placeholder: String, height: Int = 42) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(shape)
            .background(colors.searchFill)
            .border(1.dp, colors.cardEdge, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(15.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text(placeholder, color = if (colors.isDark) colors.muted else colors.dim, fontSize = 14.sp, maxLines = 1)
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth().testTag(BiblesTags.CATALOG_SEARCH),
            )
        }
    }
}

/** The search and language fields' fill: faint white on dark, `#f8f8f6` on light. */
internal val com.church.presenter.churchpresentermobile.ui.theme.AppColors.searchFill: Color
    get() = if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8F8F6)

/** "Language  All languages ........ 1,248 ⌄" */
@Composable
internal fun LanguageButton(view: CatalogView, language: String?, open: Boolean, height: Int = 42, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    val chosen = view.languages.firstOrNull { it.code == language }
    val name = if (language == null) stringResource(Res.string.bibles_all_languages) else chosen?.name ?: language
    val count = chosen?.count ?: view.sourceTotal
    Row(
        Modifier
            .testTag(BiblesTags.LANGUAGE_BUTTON)
            .fillMaxWidth()
            .height(height.dp)
            .clip(shape)
            .background(colors.searchFill)
            .border(1.dp, if (open) colors.accent else colors.cardEdge, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Language, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
        Text(stringResource(Res.string.bibles_language), color = if (colors.isDark) colors.muted else colors.dim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(name, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(grouped(count), color = if (colors.isDark) colors.muted else colors.dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Icon(
            if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** The list itself, or what stands in for it: loading, failed, nothing matching. */
@Composable
internal fun CatalogList(ui: CatalogUi, actions: CatalogActions, modifier: Modifier = Modifier, horizontalPadding: Int = 16) {
    val colors = LocalAppColors.current
    when {
        ui.snapshot.isUnavailable && ui.isLoading -> Box(modifier.fillMaxWidth().testTag(BiblesTags.CATALOG_LOADING), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                contentPadding = PaddingValues(start = horizontalPadding.dp, end = horizontalPadding.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "overline") { CatalogOverline(ui) }
                if (ui.snapshot.sources.values.any { it is SourceStatus.Stale }) {
                    item(key = "stale") { Notice(stringResource(Res.string.bible_catalog_stale_notice), BiblesTags.CATALOG_STALE) }
                }
                if (ui.filter.source == BibleSource.ZEFANIA && ui.snapshot.sources[BibleSource.ZEFANIA] is SourceStatus.RateLimited) {
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
    val label = ui.filter.source?.label ?: stringResource(Res.string.bibles_translations_count, grouped(ui.view.rows.size))
    val fetchedAt = ui.snapshot.fetchedAtMs
    val days = if (fetchedAt == null) 0L else (ui.nowMs - fetchedAt).coerceAtLeast(0L) / MS_PER_DAY
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Overline(label)
        Text(
            if (days == 0L) stringResource(Res.string.bibles_catalog_updated_today) else stringResource(Res.string.bibles_catalog_updated_days, days.toInt()),
            color = colors.muted,
            fontSize = 11.sp,
        )
    }
}

private const val MS_PER_DAY = 24L * 60 * 60 * 1000

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
        Text(stringResource(Res.string.bibles_no_match), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
        Text(stringResource(Res.string.bibles_catalog_failed_title), color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(reason, color = colors.muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        BiblesButton(stringResource(Res.string.bibles_try_again), onRetry, kind = ButtonKind.QUIET, tag = BiblesTags.CATALOG_RETRY, height = 46.dp)
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
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
            Text(stringResource(Res.string.bibles_row_installed), color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            Text(stringResource(Res.string.bibles_row_queued), color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        RowStatus.Failed -> Pill(tag, colors.danger.copy(alpha = 0.12f), colors.danger.copy(alpha = 0.3f)) {
            Text(stringResource(Res.string.bibles_row_retry), color = colors.danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        RowStatus.Available -> Pill(tag, if (colors.isDark) Color(0x0FFFFFFF) else Color.White, colors.cardEdge) {
            Icon(Icons.Filled.Download, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
            Text(
                // eBible's catalogue publishes no sizes, so those rows say what the button does.
                if (row.bible.sizeBytes > 0) sizeLabel(row.bible.sizeBytes) else stringResource(Res.string.bibles_row_get),
                color = colors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private const val PERCENT = 100

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

/** The language picker — design 2e, a tall sheet over the catalogue. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguageSheet(view: CatalogView, language: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LocalAppColors.current.sheetBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        LanguagePicker(
            view = view,
            language = language,
            onPick = { onPick(it); onDismiss() },
            onDone = onDismiss,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp).navigationBarsPadding(),
        )
    }
}

/**
 * The picker's content, shared by the phone's sheet and the tablet's dropdown (5a).
 *
 * @param onDone Null hides the header — the tablet dropdown has none.
 */
@Composable
internal fun LanguagePicker(
    view: CatalogView,
    language: String?,
    onPick: (String?) -> Unit,
    onDone: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var filter by remember { mutableStateOf("") }
    val languages = view.languages.drop(1)
    val shown = languages.filter { filter.isBlank() || it.name.contains(filter.trim(), ignoreCase = true) || it.code.orEmpty().contains(filter.trim(), ignoreCase = true) }
    Column(modifier.testTag(BiblesTags.LANGUAGE_SHEET)) {
        if (onDone != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.bibles_language), color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.025).em)
                Text(
                    stringResource(Res.string.bibles_done),
                    color = colors.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(BiblesTags.LANGUAGE_DONE).clickable(onClick = onDone),
                )
            }
        }
        CatalogSearch(filter, { filter = it }, stringResource(Res.string.bibles_search_languages, languages.size), height = 38)
        if (onDone != null && view.deviceLanguages.isNotEmpty() && filter.isBlank()) {
            Overline(stringResource(Res.string.bibles_on_this_device), Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp))
            Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                view.deviceLanguages.forEach { option ->
                    val shape = RoundedCornerShape(18.dp)
                    Text(
                        option.name,
                        color = colors.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(shape)
                            .background(colors.cardFill)
                            .border(1.dp, colors.cardEdge, shape)
                            .clickable { onPick(option.code) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
            Overline(stringResource(Res.string.bibles_all_languages), Modifier.padding(start = 4.dp, top = 14.dp, bottom = 4.dp))
        } else {
            Box(Modifier.height(8.dp))
        }
        val state = rememberLazyListState()
        LazyColumn(state = state, modifier = Modifier.weight(1f, fill = false).verticalScrollbar(state)) {
            if (filter.isBlank()) {
                item(key = "all") {
                    LanguageRow(LanguageOption(null, stringResource(Res.string.bibles_all_languages), view.sourceTotal), language == null) { onPick(null) }
                }
            }
            items(shown, key = { it.code.orEmpty() }) { option -> LanguageRow(option, option.code == language) { onPick(option.code) } }
        }
    }
}

@Composable
private fun LanguageRow(option: LanguageOption, chosen: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .testTag(BiblesTags.language(option.code))
            .semantics { selected = chosen }
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (chosen) colors.chosenFill else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(16.dp)) {
            if (chosen) Icon(Icons.Filled.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
        }
        Text(
            option.name,
            color = if (chosen) colors.accent else colors.text,
            fontSize = 15.sp,
            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(grouped(option.count), color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** A catalogue row's own status names the install the sheet should show, if any. */
internal fun CatalogRow.hasInstall(): Boolean = status !is RowStatus.Available && status !is RowStatus.Installed

/** The placeholder the tablet search uses, with the chosen source's size in it (5a). */
@Composable
internal fun countedSearchPlaceholder(view: CatalogView): String =
    stringResource(Res.string.bibles_search_catalog_count, grouped(view.sourceTotal))
