package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlFormat
import com.church.presenter.churchpresentermobile.bibleimport.ConvertProgress
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleDownloads
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleWebCatalog
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogBible
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogSnapshot
import com.church.presenter.churchpresentermobile.bibleimport.catalog.EBibleCatalog
import com.church.presenter.churchpresentermobile.bibleimport.catalog.FakeWebFetcher
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallFailure
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallProgress
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallState
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallStep
import com.church.presenter.churchpresentermobile.bibleimport.catalog.SourceStatus
import com.church.presenter.churchpresentermobile.bibleimport.catalog.ok
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.model.InstalledBibleBook
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.bibles.BiblesPage
import com.church.presenter.churchpresentermobile.ui.bibles.BiblesTablet
import com.church.presenter.churchpresentermobile.ui.bibles.BiblesTags
import com.church.presenter.churchpresentermobile.ui.bibles.CatalogActions
import com.church.presenter.churchpresentermobile.ui.bibles.CatalogScreen
import com.church.presenter.churchpresentermobile.ui.bibles.CatalogUi
import com.church.presenter.churchpresentermobile.ui.bibles.ConvertActions
import com.church.presenter.churchpresentermobile.ui.bibles.ConvertForm
import com.church.presenter.churchpresentermobile.ui.bibles.ConvertUi
import com.church.presenter.churchpresentermobile.ui.bibles.InstallActions
import com.church.presenter.churchpresentermobile.ui.bibles.InstallView
import com.church.presenter.churchpresentermobile.ui.bibles.InstalledList
import com.church.presenter.churchpresentermobile.ui.bibles.LanguagePicker
import com.church.presenter.churchpresentermobile.ui.bibles.LicenceDialog
import com.church.presenter.churchpresentermobile.ui.bibles.NoBibleInstalled
import com.church.presenter.churchpresentermobile.ui.bibles.TranslationList
import com.church.presenter.churchpresentermobile.ui.bibles.TranslationSelector
import com.church.presenter.churchpresentermobile.viewmodel.BibleChoiceViewModel
import com.church.presenter.churchpresentermobile.viewmodel.CatalogFilter
import com.church.presenter.churchpresentermobile.viewmodel.ConvertBibleViewModel
import com.church.presenter.churchpresentermobile.viewmodel.ConvertPhase
import com.church.presenter.churchpresentermobile.viewmodel.DetectedBible
import com.church.presenter.churchpresentermobile.viewmodel.GetBiblesViewModel
import com.church.presenter.churchpresentermobile.viewmodel.PickedBible
import com.church.presenter.churchpresentermobile.viewmodel.buildCatalogView
import kotlin.test.Test
import kotlin.time.Clock

/**
 * Getting Bibles onto the phone — design "Church Presenter Bible Downloads".
 *
 * One golden per state the design draws: the selector and its list (1), the catalogue with every
 * kind of row, the language picker and the ways the list can fail to arrive (2), each step of an
 * install and how it ends (3), converting a file and the file that will not convert (4), the
 * copyright confirmation in front of both, and the tablet's three panes (5).
 */
class BibleDownloadsScreenshotTest {

    // ── Fixtures ─────────────────────────────────────────────────────────

    private fun installed(id: String, title: String, abbreviation: String, language: String, active: Boolean = false) =
        InstalledBible(
            id = id, fileName = "$id.spb", title = title, verseCount = 31_102, sizeBytes = 4_600_000,
            abbreviation = abbreviation, languageName = language, origin = if (active) "eBible.org" else "",
            books = listOf(
                InstalledBibleBook(1, "Genesis", 50),
                InstalledBibleBook(40, "Matthew", 28),
            ),
        )

    private val kjv = installed("ENG_KJV", "King James Version", "KJV", "English", active = true)
    private val rst = installed("RUS_RST", "Синодальный перевод", "RST", "Russian")

    private fun row(
        name: String,
        language: String,
        source: BibleSource,
        size: Long,
        ot: Int = 39,
        nt: Int = 27,
    ) = CatalogBible(
        source = source,
        downloadKey = name,
        sizeBytes = size,
        language = language.take(3).uppercase(),
        languageName = language,
        identifier = name,
        displayName = name,
        copyright = "Public Domain",
        otBookCount = ot,
        ntBookCount = nt,
        fileStem = "${language.take(3).uppercase()}_$name",
    )

    private val bsb = row("Berean Standard Bible", "English", BibleSource.EBIBLE, 0, ot = 0)
    private val catalogue = listOf(
        row("King James Version", "English", BibleSource.EBIBLE, 0),
        row("World English Bible", "English", BibleSource.EBIBLE, 0),
        bsb,
        row("American Standard Version", "English", BibleSource.ZEFANIA, 4_200_000),
        row("Синодальный перевод", "Russian", BibleSource.ZEFANIA, 5_100_000),
        row("Luther 1912", "German", BibleSource.BEBLIA, 4_800_000, nt = 0),
        row("Reina-Valera 1909", "Spanish", BibleSource.BEBLIA, 4_500_000),
    )

    private val installs = mapOf(
        bsb.key to InstallState.Running(
            bsb,
            InstallProgress(
                step = InstallStep.CONVERT,
                downloadMs = 3_000,
                convert = ConvertProgress(0.62f, 41, 20_000),
            ),
        ),
    )

    private fun catalogUi(
        rows: List<CatalogBible> = catalogue,
        filter: CatalogFilter = CatalogFilter(),
        snapshot: CatalogSnapshot = CatalogSnapshot(
            rows,
            BibleSource.entries.associateWith { SourceStatus.Fresh(NOW) },
        ),
        isLoading: Boolean = false,
    ) = CatalogUi(
        snapshot = snapshot,
        view = buildCatalogView(
            snapshot.bibles,
            filter,
            listOf(installed("ENG_KJV", "King James Version", "KJV", "English").copy(catalogKey = catalogue[0].key)),
            installs,
        ),
        isLoading = isLoading,
        filter = filter,
        nowMs = NOW,
    )

    private val noActions = CatalogActions({}, {}, {}, {}, {})
    private val noInstallActions = InstallActions({}, {}, {}, {}, {})

    // ── 1 · The Bible tab's entry point ─────────────────────────────────

    @Test
    fun selector() = screenshot("bible-selector__active") {
        TranslationSelector(kjv, installedCount = 2, onClick = {}, modifier = Modifier.padding(16.dp))
    }

    @Test
    fun selectorWithNothingInstalled() = screenshot("bible-selector__desktop") {
        TranslationSelector(null, installedCount = 0, onClick = {}, modifier = Modifier.padding(16.dp))
    }

    @Test
    fun translationSheet() = screenshot("bible-selector__sheet") {
        TranslationList(
            listOf(kjv, rst),
            kjv.id,
            {},
            onManage = {},
            onGetMore = {},
            showHeader = true,
            modifier = Modifier.padding(16.dp),
        )
    }

    @Test
    fun translationPopover() = screenshot("bible-selector__popover", width = 360.dp) {
        TranslationList(
            listOf(kjv, rst),
            kjv.id,
            {},
            onManage = {},
            onGetMore = {},
            showHeader = false,
            compact = true,
            modifier = Modifier.padding(14.dp),
        )
    }

    @Test
    fun noBibleInstalled() = screenshot("bible-empty__get-bibles") {
        NoBibleInstalled(onGetBibles = {})
    }

    // ── 2 · The catalogue ───────────────────────────────────────────────

    @Test
    fun catalogueAllSources() = screenshot("bible-catalog__all-sources") {
        CatalogScreen(catalogUi(), noActions, onBack = {})
    }

    @Test
    fun catalogueOneSource() = screenshot("bible-catalog__zefania") {
        CatalogScreen(catalogUi(filter = CatalogFilter(source = BibleSource.ZEFANIA)), noActions, onBack = {})
    }

    @Test
    fun catalogueOneLanguage() = screenshot("bible-catalog__english") {
        CatalogScreen(catalogUi(filter = CatalogFilter(language = "ENG")), noActions, onBack = {})
    }

    @Test
    fun catalogueLoading() = screenshot("bible-catalog__loading") {
        CatalogScreen(catalogUi(rows = emptyList(), isLoading = true), noActions, onBack = {})
    }

    @Test
    fun catalogueOffline() = screenshot("bible-catalog__offline") {
        val offline = CatalogSnapshot(emptyList(), BibleSource.entries.associateWith { SourceStatus.Offline })
        CatalogScreen(catalogUi(rows = emptyList(), snapshot = offline), noActions, onBack = {})
    }

    @Test
    fun catalogueStale() = screenshot("bible-catalog__stale") {
        val stale = CatalogSnapshot(catalogue, BibleSource.entries.associateWith { SourceStatus.Stale(NOW) })
        CatalogScreen(catalogUi(snapshot = stale), noActions, onBack = {})
    }

    @Test
    fun catalogueNoMatch() = screenshot("bible-catalog__no-match") {
        CatalogScreen(catalogUi(filter = CatalogFilter(query = "klingon")), noActions, onBack = {})
    }

    @Test
    fun languagePicker() = screenshot("bible-catalog__language-picker") {
        val ui = catalogUi()
        LanguagePicker(ui.view, "ENG", onPick = {}, onDone = {}, modifier = Modifier.padding(16.dp))
    }

    // ── 3 · Installing ──────────────────────────────────────────────────

    @Composable
    private fun install(state: InstallState) = InstallView(state, noInstallActions, Modifier.padding(20.dp))

    @Test
    fun downloading() = screenshot("bible-install__downloading") {
        install(
            InstallState.Running(
                bsb,
                InstallProgress(downloadName = "engbsb_usfx.zip", downloadedBytes = 2_100_000, totalBytes = 4_300_000),
            ),
        )
    }

    @Test
    fun converting() = screenshot("bible-install__converting") {
        install(
            InstallState.Running(
                bsb.copy(otBookCount = 39),
                InstallProgress(
                    step = InstallStep.CONVERT,
                    downloadName = "engbsb_usfx.zip",
                    downloadedBytes = 4_300_000,
                    totalBytes = 4_300_000,
                    downloadMs = 3_000,
                    convert = ConvertProgress(0.62f, 41, 19_400),
                ),
            ),
        )
    }

    @Test
    fun queued() = screenshot("bible-install__queued") { install(InstallState.Queued(bsb)) }

    @Test
    fun failed() = screenshot("bible-install__failed") {
        install(
            InstallState.Failed(
                bsb,
                InstallFailure.NETWORK,
                "offline",
                InstallProgress(downloadName = "engbsb_usfx.zip"),
            ),
        )
    }

    @Test
    fun ready() = screenshot("bible-install__ready") {
        val installed = InstalledBible(
            "ENG_BSB",
            "ENG_BSB.spb",
            "Berean Standard Bible",
            31_086,
            4_300_000,
            license = "Public domain",
        )
        install(InstallState.Done(bsb, installed, 66, 31_086))
    }

    @Test
    fun licenceForACatalogueBible() = screenshot("bible-licence__catalog", dialog = true) {
        LicenceDialog("Berean Standard Bible", "engbsb", "Public domain", BibleSource.EBIBLE, "ENG", false, {}, {})
    }

    @Test
    fun licenceForAFile() = screenshot("bible-licence__file", dialog = true) {
        LicenceDialog("Біблія (Огієнко)", "", "", null, "", isReinstall = true, onConfirm = {}, onDismiss = {})
    }

    // ── 4 · Converting a file ──────────────────────────────────────────

    private val detected = DetectedBible(
        BibleXmlFormat.ZEFANIA,
        "Ukrainian",
        66,
        31_170,
        "John 3:16" to "Бо так полюбив Бог світ, що дав Сина Свого Однородженого…",
    )

    @Composable
    private fun convert(phase: ConvertPhase, replaces: Boolean = false) = ConvertForm(
        ConvertUi(
            file = if (phase == ConvertPhase.Empty) null else PickedBible(
                "SF_2009-01-20_UKR_UKROGIENKO.xml",
                5_400_000,
            ),
            phase = phase,
            title = "Біблія (Огієнко)",
            abbreviation = "UKR_OGI",
            savedFileName = "UKR_OGI.spb",
            replaces = replaces,
        ),
        ConvertActions({}, {}, {}, {}, {}, {}),
    )

    @Test
    fun convertEmpty() = screenshot("bible-convert__empty") { convert(ConvertPhase.Empty) }

    @Test
    fun convertReading() = screenshot("bible-convert__reading") { convert(ConvertPhase.Reading) }

    @Test
    fun convertReady() = screenshot("bible-convert__ready") { convert(ConvertPhase.Ready(detected)) }

    @Test
    fun convertReplaces() = screenshot(
        "bible-convert__replaces",
    ) { convert(ConvertPhase.Ready(detected), replaces = true) }

    @Test
    fun convertUnreadable() = screenshot("bible-convert__unreadable") {
        convert(ConvertPhase.Unreadable(InstallFailure.NOT_A_BIBLE))
    }

    @Test
    fun installedList() = screenshot("bible-installed__list") {
        InstalledList(listOf(kjv, rst), kjv.id, {}, {})
    }

    // ── 5 · Tablet ──────────────────────────────────────────────────────

    @Test
    fun tabletCatalogue() {
        val csv = """
            |languageCode,translationId,languageNameInEnglish,languageName,Redistributable,downloadable,shortTitle,OTbooks,NTbooks
            |eng,engkjv,English,English,True,True,King James Version,39,27
            |eng,engwebp,English,English,True,True,World English Bible,39,27
            |eng,engbsb,English,English,True,True,Berean Standard Bible,0,27
            |rus,russyn,Russian,Русский,True,True,Синодальный перевод,39,27
        """.trimMargin().encodeToByteArray()
        val repository = LocalBibleRepository(InMemoryFileStorage())
        val fetcher = FakeWebFetcher(mapOf(EBibleCatalog.CATALOG_URL to ok(csv)))
        // The pane reads the wall clock for "Catalog updated today", so the catalogue must too.
        val wallClock = { Clock.System.now().toEpochMilliseconds() }
        val catalog = BibleWebCatalog(fetcher, InMemoryFileStorage(), now = wallClock)
        val downloads = BibleDownloads(repository, fetcher, clock = wallClock)
        val getVm = GetBiblesViewModel(catalog, downloads, repository)
        screenshot(
            "bible-tablet__catalog",
            width = null,
            surface = Screenshots.TABLET_SURFACE,
            until = {
                onAllNodes(hasTestTag(BiblesTags.catalogRow("EBIBLE:engkjv"))).fetchSemanticsNodes().isNotEmpty()
            },
        ) {
            BiblesTablet(
                page = BiblesPage.GET,
                onPage = {},
                getVm = getVm,
                convertVm = ConvertBibleViewModel(repository),
                choiceVm = BibleChoiceViewModel(repository),
                onClose = {},
                onOpenedBible = {},
            )
        }
    }

    private companion object {
        /** A fixed "now", so "Catalog updated today" never depends on when the suite runs. */
        const val NOW = 1_790_000_000_000L
    }
}
