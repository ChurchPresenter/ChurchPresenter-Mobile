package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_field_copyright
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_unknown
import churchpresentermobile.composeapp.generated.resources.bibles_detail_empty_body
import churchpresentermobile.composeapp.generated.resources.bibles_detail_empty_title
import churchpresentermobile.composeapp.generated.resources.bibles_field_format
import churchpresentermobile.composeapp.generated.resources.bibles_field_language
import churchpresentermobile.composeapp.generated.resources.bibles_field_source
import churchpresentermobile.composeapp.generated.resources.bibles_get
import churchpresentermobile.composeapp.generated.resources.bibles_nav_convert
import churchpresentermobile.composeapp.generated.resources.bibles_nav_installed
import churchpresentermobile.composeapp.generated.resources.bibles_row_get
import churchpresentermobile.composeapp.generated.resources.bibles_source_converts
import churchpresentermobile.composeapp.generated.resources.bibles_sources
import churchpresentermobile.composeapp.generated.resources.bibles_storage
import churchpresentermobile.composeapp.generated.resources.bibles_storage_used
import churchpresentermobile.composeapp.generated.resources.bibles_title
import churchpresentermobile.composeapp.generated.resources.cd_back
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogBible
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.BibleChoiceViewModel
import com.church.presenter.churchpresentermobile.viewmodel.CatalogRow
import com.church.presenter.churchpresentermobile.viewmodel.ConvertBibleViewModel
import com.church.presenter.churchpresentermobile.viewmodel.GetBiblesViewModel
import com.church.presenter.churchpresentermobile.viewmodel.RowStatus
import org.jetbrains.compose.resources.stringResource

/**
 * Design 5a: navigation and sources on the left, the catalogue in the middle, and the chosen
 * translation — with its live install steps — on the right.
 */
@Composable
internal fun BiblesTablet(
    page: BiblesPage,
    onPage: (BiblesPage) -> Unit,
    getVm: GetBiblesViewModel,
    convertVm: ConvertBibleViewModel,
    choiceVm: BibleChoiceViewModel,
    onClose: () -> Unit,
    onOpenedBible: () -> Unit,
) {
    val colors = LocalAppColors.current
    val ui = catalogUi(getVm)
    Row(Modifier.fillMaxSize().statusBarsPadding()) {
        TabletNav(page, onPage, ui, getVm, choiceVm, onClose)
        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.cardEdge))
        when (page) {
            BiblesPage.GET -> TabletCatalog(ui, getVm, onOpenedBible)
            BiblesPage.CONVERT -> Box(Modifier.weight(1f)) { ConvertPane(convertVm, onClose, onOpenedBible) }
            BiblesPage.INSTALLED -> Box(Modifier.weight(1f)) { InstalledPane(choiceVm) }
        }
    }
}

@Composable
private fun TabletNav(
    page: BiblesPage,
    onPage: (BiblesPage) -> Unit,
    ui: CatalogUi,
    getVm: GetBiblesViewModel,
    choiceVm: BibleChoiceViewModel,
    onClose: () -> Unit,
) {
    val colors = LocalAppColors.current
    val installed by choiceVm.installed.collectAsState()
    Column(Modifier.width(TABLET_NAV_WIDTH).fillMaxHeight()) {
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onClose, modifier = Modifier.testTag(BiblesTags.CATALOG_BACK).size(28.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(Res.string.cd_back),
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    stringResource(Res.string.bibles_title),
                    color = colors.text,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.03).em,
                )
            }
            Text(
                installedSummary(choiceVm, getVm),
                color = colors.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 9.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NavItem(
                Icons.AutoMirrored.Outlined.MenuBook,
                stringResource(Res.string.bibles_nav_installed),
                installed.size.toString(),
                page == BiblesPage.INSTALLED,
                BiblesTags.NAV_INSTALLED,
            ) { onPage(BiblesPage.INSTALLED) }
            NavItem(
                Icons.Outlined.Language,
                stringResource(Res.string.bibles_get),
                null,
                page == BiblesPage.GET,
                BiblesTags.NAV_GET,
            ) { onPage(BiblesPage.GET) }
            NavItem(
                Icons.Outlined.Description,
                stringResource(Res.string.bibles_nav_convert),
                null,
                page == BiblesPage.CONVERT,
                BiblesTags.NAV_CONVERT,
            ) { onPage(BiblesPage.CONVERT) }
        }
        Overline(
            stringResource(Res.string.bibles_sources),
            Modifier.padding(start = 26.dp, top = 14.dp, bottom = 12.dp),
        )
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BibleSource.entries.forEach { source ->
                SourceCard(source, ui.snapshot.count(source), page == BiblesPage.GET && ui.filter.source == source) {
                    onPage(BiblesPage.GET)
                    getVm.setSource(if (ui.filter.source == source) null else source)
                }
            }
        }
        Box(Modifier.weight(1f))
        StorageCard(installed.sumOf { it.sizeBytes })
    }
}

private val TABLET_NAV_WIDTH = 290.dp
private val TABLET_CATALOG_WIDTH = 470.dp

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    count: String?,
    active: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .testTag(tag)
            .semantics { selected = active }
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (active) colors.chosenFill else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) colors.accent else colors.muted,
            modifier = Modifier.size(19.dp),
        )
        Text(
            label,
            color = if (active) colors.accent else colors.secondary,
            fontSize = 15.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (count != null) Text(count, color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SourceCard(source: BibleSource, count: Int, active: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val hue = colors.hueOf(source)
    val shape = RoundedCornerShape(13.dp)
    Row(
        Modifier
            .testTag(BiblesTags.source("card_${source.name}"))
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) hue.tint else colors.cardFill)
            .border(1.dp, if (active) hue.edge else colors.cardEdge, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(hue.fg))
        Column(Modifier.weight(1f)) {
            Text(source.label, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(Res.string.bibles_source_converts, source.format.label),
                color = colors.muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        Text(grouped(count), color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** How much the Bibles on this device take up. Text only: the platform gives no budget to draw a bar against. */
@Composable
private fun StorageCard(bytes: Long) {
    val colors = LocalAppColors.current
    BiblesCard(Modifier.padding(16.dp).fillMaxWidth(), radius = 13.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                stringResource(Res.string.bibles_storage),
                color = colors.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(Res.string.bibles_storage_used, sizeLabel(bytes)),
                color = colors.muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun TabletCatalog(ui: CatalogUi, vm: GetBiblesViewModel, onOpenedBible: () -> Unit) {
    val colors = LocalAppColors.current
    var picking by remember { mutableStateOf(false) }
    var selectedKey by remember { mutableStateOf<String?>(null) }
    val sheetKey by vm.sheet.key.collectAsState()
    val installs by vm.installs.collectAsState()
    // A row just confirmed for install is the one the detail pane follows.
    val shownKey = sheetKey ?: selectedKey
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(TABLET_CATALOG_WIDTH).fillMaxHeight()) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 14.dp)) {
                SourceTabs(ui.filter.source, vm::setSource, fontSize = 12)
                Box(Modifier.height(12.dp))
                CatalogSearch(ui.filter.query, vm::setQuery, countedSearchPlaceholder(ui.view), height = 44)
                Box(Modifier.height(12.dp))
                Box {
                    LanguageButton(ui.view, ui.filter.language, open = picking, height = 44) { picking = !picking }
                    if (picking) {
                        Popup(onDismissRequest = { picking = false }, properties = PopupProperties(focusable = true)) {
                            Box(
                                Modifier
                                    .padding(top = 52.dp)
                                    .width(TABLET_CATALOG_WIDTH - 44.dp)
                                    .height(LANGUAGE_DROPDOWN_HEIGHT)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.sheetBackground)
                                    .border(1.dp, colors.cardEdge, RoundedCornerShape(14.dp))
                                    .padding(10.dp),
                            ) {
                                LanguagePicker(
                                    ui.view,
                                    ui.filter.language,
                                    onPick = { vm.setLanguage(it); picking = false },
                                    onDone = null,
                                )
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
            CatalogList(
                ui,
                CatalogActions(
                    vm::setSource,
                    vm::setQuery,
                    vm::setLanguage,
                    { row -> selectedKey = row.bible.key; vm.sheet.hide() },
                ) { vm.load(refresh = true) },
                Modifier.weight(1f),
                horizontalPadding = 22,
            )
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.cardEdge))
        Box(Modifier.weight(1f).fillMaxHeight()) {
            val install = shownKey?.let { installs[it] }
            val row = shownKey?.let { key -> ui.view.rows.firstOrNull { it.bible.key == key } }
            when {
                install != null -> InstallView(
                    install,
                    installActions(vm.sheet, install.bible.key, onOpenedBible),
                    Modifier.verticalScroll(rememberScrollState()).padding(28.dp),
                )
                row != null -> CatalogDetail(row) { vm.onRowTapped(row) }
                else -> DetailEmpty()
            }
        }
    }
}

private val LANGUAGE_DROPDOWN_HEIGHT = 360.dp

/** The chosen translation before it is installed: what it is, and the button that gets it. */
@Composable
private fun CatalogDetail(row: CatalogRow, onGet: () -> Unit) {
    val colors = LocalAppColors.current
    val bible: CatalogBible = row.bible
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TestamentTile(bible.testament, size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    bible.displayName,
                    color = colors.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.025).em,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    Modifier.padding(top = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(bible.languageLabel, color = colors.muted, fontSize = 12.sp)
                    FormatBadge(bible.source.format)
                }
            }
        }
        FactRows(
            listOf(
                stringResource(Res.string.bibles_field_language) to { FactValue(bible.languageLabel) },
                stringResource(Res.string.bibles_field_source) to { FactValue(bible.source.label) },
                stringResource(Res.string.bibles_field_format) to { FormatBadge(bible.source.format) },
                stringResource(Res.string.bible_catalog_license_field_copyright) to {
                    FactValue(bible.copyright.ifBlank { stringResource(Res.string.bible_catalog_license_unknown) })
                },
            ),
            Modifier.padding(top = 22.dp),
        )
        if (row.status == RowStatus.Available) {
            BiblesButton(
                label = if (bible.sizeBytes > 0) {
                    "${stringResource(Res.string.bibles_row_get)} · ${sizeLabel(bible.sizeBytes)}"
                } else {
                    stringResource(Res.string.bibles_row_get)
                },
                onClick = onGet,
                icon = Icons.Filled.Download,
                tag = BiblesTags.rowAction("detail"),
                modifier = Modifier.padding(top = 22.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DetailEmpty() {
    val colors = LocalAppColors.current
    Column(
        Modifier.testTag(BiblesTags.DETAIL_EMPTY).fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(Res.string.bibles_detail_empty_title),
            color = colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(Res.string.bibles_detail_empty_body),
            color = colors.muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
