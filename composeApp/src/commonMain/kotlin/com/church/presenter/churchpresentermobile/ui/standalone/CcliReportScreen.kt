package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.report_empty_body
import churchpresentermobile.composeapp.generated.resources.report_empty_range
import churchpresentermobile.composeapp.generated.resources.report_empty_title
import churchpresentermobile.composeapp.generated.resources.report_export_failed
import com.church.presenter.churchpresentermobile.library.PlayLogRepository
import com.church.presenter.churchpresentermobile.ui.BinaryDocumentExporter
import com.church.presenter.churchpresentermobile.ui.EmptyState
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.CcliReportViewModel
import com.church.presenter.churchpresentermobile.viewmodel.ReportData
import com.church.presenter.churchpresentermobile.viewmodel.ReportExportFormat
import com.church.presenter.churchpresentermobile.viewmodel.ReportTab
import org.jetbrains.compose.resources.stringResource

/** Above this width the rankings sit beside a full table, the way the desktop lays the report out. */
internal val ReportTwoPaneMinWidth: Dp = 900.dp

/** The rankings pane on a wide screen. */
internal val ReportRankingsPaneWidth: Dp = 340.dp

/**
 * The CCLI report for what this device has projected.
 *
 * Standalone only: in remote mode the desktop does the projecting and keeps
 * its own report. What is counted, and when, is decided by
 * [com.church.presenter.churchpresentermobile.present.PlayRecorder]; this
 * screen only reads the result, filters it and exports it.
 *
 * @param providedViewModel Supplied by tests only; the screen owns its own otherwise.
 */
@Composable
fun CcliReportScreen(
    repository: PlayLogRepository,
    modifier: Modifier = Modifier,
    providedViewModel: CcliReportViewModel? = null,
) {
    val colors = LocalAppColors.current
    val vm: CcliReportViewModel = providedViewModel
        ?: viewModel(key = "ccli_report") { CcliReportViewModel(repository) }
    val report by vm.report.collectAsState()
    var exportError by remember { mutableStateOf<String?>(null) }
    var confirmingClear by remember { mutableStateOf(false) }

    if (report.isLogEmpty) {
        EmptyState(
            title = stringResource(Res.string.report_empty_title),
            body = stringResource(Res.string.report_empty_body),
            modifier = modifier.fillMaxSize().background(colors.background).testTag(ReportTags.EMPTY),
        )
        return
    }

    BinaryDocumentExporter(onError = { exportError = it }) { share ->
        BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.background)) {
            val wide = maxWidth >= ReportTwoPaneMinWidth
            Column(modifier = Modifier.fillMaxSize()) {
                ReportFilters(vm = vm, report = report, wide = wide, modifier = Modifier.fillMaxWidth())
                HorizontalDivider(color = colors.borderSubtle)
                Box(modifier = Modifier.weight(1f)) {
                    if (wide) ReportTwoPane(report) else ReportStacked(report)
                }
                exportError?.let { message ->
                    Text(
                        text = stringResource(Res.string.report_export_failed, message),
                        color = colors.danger,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                val export: (ReportExportFormat) -> Unit = { format ->
                    val file = vm.export(format)
                    share(file.bytes, file.fileName, file.mimeType)
                }
                ReportActionBar(
                    wide = wide,
                    onExportCsv = { export(ReportExportFormat.CSV) },
                    onExportXls = { export(ReportExportFormat.XLSX) },
                    onClear = { confirmingClear = true },
                )
            }
        }
    }

    if (confirmingClear) {
        ClearStatisticsDialog(
            onConfirm = { vm.clearStatistics(); confirmingClear = false },
            onDismiss = { confirmingClear = false },
        )
    }
}

/** The phone arrangement: one scrolling column of whichever view is open. */
@Composable
private fun ReportStacked(report: ReportData) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (report.filters.tab) {
            ReportTab.SONGS -> {
                ReportTiles(report)
                if (report.songs.isEmpty()) EmptyRange() else SongCards(report.songs)
            }
            ReportTab.BIBLE -> {
                ReportTiles(report)
                if (report.verses.isEmpty()) EmptyRange() else {
                    BookBars(report.books)
                    VerseRows(report.verses)
                }
            }
            ReportTab.ACTIVITY -> {
                ReportTiles(report)
                ActivityChart(report.activity, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * The tablet arrangement, mirroring the desktop: rankings on the left, the
 * full table on the right. Activity has no table, so it takes the whole width.
 */
@Composable
private fun ReportTwoPane(report: ReportData) {
    val colors = LocalAppColors.current
    if (report.filters.tab == ReportTab.ACTIVITY) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReportTiles(report)
            ActivityChart(report.activity, modifier = Modifier.fillMaxWidth())
        }
        return
    }
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.width(ReportRankingsPaneWidth).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (report.filters.tab == ReportTab.SONGS) SongRankings(report) else BookRankings(report)
        }
        VerticalDivider(color = colors.borderSubtle)
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when {
                report.filters.tab == ReportTab.SONGS && report.songs.isEmpty() -> EmptyRange(Modifier.padding(20.dp))
                report.filters.tab == ReportTab.SONGS -> SongTable(report.songs)
                report.verses.isEmpty() -> EmptyRange(Modifier.padding(20.dp))
                else -> VerseTable(report.verses)
            }
        }
    }
}

@Composable
private fun EmptyRange(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Text(
        text = stringResource(Res.string.report_empty_range),
        color = colors.muted,
        fontSize = 13.sp,
        modifier = modifier.testTag(ReportTags.EMPTY_RANGE),
    )
}
