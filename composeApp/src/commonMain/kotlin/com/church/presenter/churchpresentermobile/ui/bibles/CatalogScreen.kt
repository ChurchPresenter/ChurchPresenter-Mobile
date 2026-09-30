package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.bibles_catalog_subtitle
import churchpresentermobile.composeapp.generated.resources.bibles_get
import churchpresentermobile.composeapp.generated.resources.bibles_search_catalog
import churchpresentermobile.composeapp.generated.resources.bibles_search_catalog_count
import churchpresentermobile.composeapp.generated.resources.bibles_source_all
import churchpresentermobile.composeapp.generated.resources.bibles_source_holy_short
import churchpresentermobile.composeapp.generated.resources.cd_back
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.CatalogRow
import com.church.presenter.churchpresentermobile.viewmodel.CatalogView
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
internal fun BackHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    backTag: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
            Text(
                subtitle,
                color = colors.dim.takeIf { !colors.isDark } ?: colors.muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 5.dp),
            )
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
            .background(colors.trackFill)
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
                            colors.isDark -> colors.segmentActiveFill
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
            if (query.isEmpty()) Text(
                placeholder,
                color = if (colors.isDark) colors.muted else colors.dim,
                fontSize = 14.sp,
                maxLines = 1,
            )
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

/** A catalogue row's own status names the install the sheet should show, if any. */
internal fun CatalogRow.hasInstall(): Boolean = status !is RowStatus.Available && status !is RowStatus.Installed

/** The placeholder the tablet search uses, with the chosen source's size in it (5a). */
@Composable
internal fun countedSearchPlaceholder(view: CatalogView): String =
    stringResource(Res.string.bibles_search_catalog_count, grouped(view.sourceTotal))
