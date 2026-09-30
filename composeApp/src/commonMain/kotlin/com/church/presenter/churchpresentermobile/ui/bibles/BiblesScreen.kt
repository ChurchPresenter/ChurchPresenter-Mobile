package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.bibles_convert_subtitle
import churchpresentermobile.composeapp.generated.resources.bibles_convert_title
import churchpresentermobile.composeapp.generated.resources.bibles_installed_downloading
import churchpresentermobile.composeapp.generated.resources.bibles_installed_summary
import churchpresentermobile.composeapp.generated.resources.bibles_title
import kotlinx.coroutines.cancel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleDownloads
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleWebCatalog
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallState
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.ui.AppBackHandler
import com.church.presenter.churchpresentermobile.ui.BinaryDocumentPicker
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.BibleChoiceViewModel
import com.church.presenter.churchpresentermobile.viewmodel.CatalogRow
import com.church.presenter.churchpresentermobile.viewmodel.ConvertBibleViewModel
import com.church.presenter.churchpresentermobile.viewmodel.ConvertPhase
import com.church.presenter.churchpresentermobile.viewmodel.convertedFileName
import com.church.presenter.churchpresentermobile.viewmodel.GetBiblesViewModel
import com.church.presenter.churchpresentermobile.viewmodel.InstallSheetController
import com.church.presenter.churchpresentermobile.viewmodel.RowStatus
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource

/**
 * Getting, converting and managing Bibles — designs 2, 3, 4 and 5.
 *
 * Owns its three ViewModels. [downloads], [catalog] and [repository] are the app shell's, because
 * an install outlives this screen ("Run in background") and the catalogue is worth keeping for the
 * session.
 *
 * @param providedViewModels Supplied by tests only, so the screen can be driven without a
 *   ViewModelStoreOwner; the app lets the screen create its own.
 * @param onOpenedBible Called after "Open in Bible", once the new translation is the active one;
 *   the shell closes this screen and shows the Bible tab.
 */
@Composable
fun BiblesScreen(
    page: BiblesPage,
    twoPane: Boolean,
    repository: LocalBibleRepository,
    downloads: BibleDownloads,
    catalog: BibleWebCatalog,
    onClose: () -> Unit,
    onOpenedBible: () -> Unit,
    modifier: Modifier = Modifier,
    providedViewModels: BiblesViewModels? = null,
) {
    val getVm: GetBiblesViewModel = providedViewModels?.get
        ?: viewModel(key = "get_bibles") { GetBiblesViewModel(catalog, downloads, repository) }
    val convertVm: ConvertBibleViewModel = providedViewModels?.convert
        ?: viewModel(key = "convert_bible") { ConvertBibleViewModel(repository) }
    val choiceVm: BibleChoiceViewModel = providedViewModels?.choice
        ?: viewModel(key = "bibles_choice") { BibleChoiceViewModel(repository) }
    var current by remember(page) { mutableStateOf(page) }

    AppBackHandler(enabled = true, onBack = onClose)

    val colors = LocalAppColors.current
    Box(modifier.fillMaxSize().background(colors.background)) {
        LicenceGate(getVm, convertVm)
        if (twoPane) {
            BiblesTablet(current, { current = it }, getVm, convertVm, choiceVm, onClose, onOpenedBible)
        } else {
            when (current) {
                BiblesPage.GET -> PhoneCatalog(getVm, onClose, onOpenedBible)
                BiblesPage.CONVERT -> SubPage(
                    stringResource(Res.string.bibles_convert_title),
                    stringResource(Res.string.bibles_convert_subtitle),
                    onClose,
                ) { ConvertPane(convertVm, onClose, onOpenedBible) }
                BiblesPage.INSTALLED -> SubPage(
                    stringResource(Res.string.bibles_title),
                    installedSummary(choiceVm, getVm),
                    onClose,
                ) { InstalledPane(choiceVm) }
            }
        }
    }
}

/** The copyright confirmation for whichever of the two paths asked for it. */
@Composable
private fun LicenceGate(getVm: GetBiblesViewModel, convertVm: ConvertBibleViewModel) {
    val licenceFor by getVm.licenceFor.collectAsState()
    licenceFor?.let { bible ->
        LicenceDialog(
            name = bible.displayName,
            identifier = bible.identifier,
            copyright = bible.copyright,
            source = bible.source,
            language = bible.language,
            isReinstall = getVm.isReinstall(bible),
            onConfirm = getVm::confirmLicence,
            onDismiss = getVm::dismissLicence,
        )
    }
    val convertPending by convertVm.licencePending.collectAsState()
    if (convertPending) {
        val title by convertVm.title.collectAsState()
        LicenceDialog(
            name = title,
            identifier = "",
            copyright = convertVm.fileRights,
            source = null,
            language = "",
            isReinstall = convertVm.replacesInstalled(),
            onConfirm = convertVm::confirmLicence,
            onDismiss = convertVm::dismissLicence,
        )
    }
}

@Composable
internal fun catalogUi(vm: GetBiblesViewModel): CatalogUi {
    val snapshot by vm.snapshot.collectAsState()
    val view by vm.view.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val filter by vm.filter.collectAsState()
    return CatalogUi(snapshot, view, isLoading, filter, Clock.System.now().toEpochMilliseconds())
}

internal fun installActions(sheet: InstallSheetController, key: String, onOpenedBible: () -> Unit) = InstallActions(
    onBackground = sheet::hide,
    onCancel = { sheet.cancel(key) },
    onRetry = { sheet.retry(key) },
    onDone = { sheet.finish(key) },
    onOpen = {
        sheet.openInBible(key)
        onOpenedBible()
    },
)

/** Tapping a row: an available one asks to install; one installing or failed shows its sheet. */
internal fun GetBiblesViewModel.onRowTapped(row: CatalogRow) {
    when (row.status) {
        RowStatus.Available -> requestInstall(row.bible)
        RowStatus.Installed -> Unit
        else -> sheet.show(row.bible.key)
    }
}

@Composable
private fun PhoneCatalog(vm: GetBiblesViewModel, onClose: () -> Unit, onOpenedBible: () -> Unit) {
    val ui = catalogUi(vm)
    val sheetKey by vm.sheet.key.collectAsState()
    val installs by vm.installs.collectAsState()
    CatalogScreen(
        ui = ui,
        actions = CatalogActions(
            vm::setSource,
            vm::setQuery,
            vm::setLanguage,
            vm::onRowTapped,
        ) { vm.load(refresh = true) },
        onBack = onClose,
    )
    val key = sheetKey
    val state = key?.let { installs[it] }
    if (key != null && state != null) InstallSheet(state, installActions(vm.sheet, key, onOpenedBible))
}

/** A phone sub-page: the back header, then [content]. */
@Composable
private fun SubPage(title: String, subtitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.background(colors.background).statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            BackHeader(title, subtitle, onBack, BiblesTags.CATALOG_BACK)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardEdge))
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
internal fun ConvertPane(vm: ConvertBibleViewModel, onClose: () -> Unit, onOpenedBible: () -> Unit) {
    val file by vm.file.collectAsState()
    val phase by vm.phase.collectAsState()
    val title by vm.title.collectAsState()
    val abbreviation by vm.abbreviation.collectAsState()
    BinaryDocumentPicker(
        onPicked = { picked -> picked?.let { vm.pick(it.fileName, it.bytes) } },
        onError = {},
    ) { launch ->
        ConvertForm(
            ui = ConvertUi(
                file = file,
                phase = phase,
                title = title,
                abbreviation = abbreviation,
                savedFileName = convertedFileName(abbreviation),
                replaces = phase is ConvertPhase.Ready && vm.replacesInstalled(abbreviation),
            ),
            actions = ConvertActions(
                onPick = launch,
                onTitle = vm::setTitle,
                onAbbreviation = vm::setAbbreviation,
                onConvert = vm::requestConvert,
                onDone = {
                    vm.reset()
                    onClose()
                },
                onOpen = {
                    vm.openInBible()
                    vm.reset()
                    onOpenedBible()
                },
            ),
        )
    }
}

@Composable
internal fun InstalledPane(vm: BibleChoiceViewModel) {
    val installed by vm.installed.collectAsState()
    val activeId by vm.activeId.collectAsState()
    InstalledList(installed, activeId, vm::setActive, vm::remove)
}

@Composable
internal fun installedSummary(choice: BibleChoiceViewModel, get: GetBiblesViewModel): String {
    val installed by choice.installed.collectAsState()
    val installs by get.installs.collectAsState()
    val downloading = installs.values.count { it is InstallState.Running || it is InstallState.Queued }
    return if (downloading > 0) {
        stringResource(Res.string.bibles_installed_downloading, installed.size, downloading)
    } else {
        stringResource(Res.string.bibles_installed_summary, installed.size)
    }
}
