package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlFormat
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogBible
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogSnapshot
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallFailure
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallProgress
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallState
import com.church.presenter.churchpresentermobile.bibleimport.catalog.SourceStatus
import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.ui.tagged
import com.church.presenter.churchpresentermobile.viewmodel.CatalogFilter
import com.church.presenter.churchpresentermobile.viewmodel.CatalogRow
import com.church.presenter.churchpresentermobile.viewmodel.CatalogView
import com.church.presenter.churchpresentermobile.viewmodel.ConvertPhase
import com.church.presenter.churchpresentermobile.viewmodel.DetectedBible
import com.church.presenter.churchpresentermobile.viewmodel.PickedBible
import com.church.presenter.churchpresentermobile.viewmodel.RowStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Bible downloads screens as the operator uses them: what is on screen, what can be pressed,
 * and which callback a press reaches — with which argument.
 */
@OptIn(ExperimentalTestApi::class)
class BiblesUiTest {

    // ── The copyright confirmation ───────────────────────────────────────────

    private fun ComposeUiTest.showLicence(copyright: String, onConfirm: () -> Unit = {}, onDismiss: () -> Unit = {}) =
        showScreen {
            LicenceDialog(
                name = "Berean Standard Bible",
                identifier = "engbsb",
                copyright = copyright,
                source = BibleSource.EBIBLE,
                language = "ENG",
                isReinstall = false,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }

    /** Long enough that the dialog's text cannot fit on any test window without scrolling. */
    private val longCopyright = List(80) { "Copyright line $it of the publisher's terms." }.joinToString(" ")

    @Test
    fun acceptIsDisabledUntilTheTextHasBeenScrolledToTheEnd() = runComposeUiTest {
        showLicence(longCopyright)

        tagged(BiblesTags.LICENCE_ACCEPT).assertIsNotEnabled()
    }

    @Test
    fun acceptIsEnabledOnceTheEndHasBeenReached() = runComposeUiTest {
        showLicence(longCopyright)

        onNode(hasScrollAction()).performScrollToNode(hasTestTag(BiblesTags.LICENCE_END))
        waitForIdle()

        tagged(BiblesTags.LICENCE_ACCEPT).assertIsEnabled()
    }

    @Test
    fun accepterAndCancellerReachTheirCallbacks() = runComposeUiTest {
        var confirmed = 0
        var dismissed = 0
        showLicence("Public domain", onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
        waitForIdle()

        click(BiblesTags.LICENCE_CANCEL)
        assertEquals(1, dismissed)

        tagged(BiblesTags.LICENCE_ACCEPT).assertIsEnabled()
        click(BiblesTags.LICENCE_ACCEPT)
        assertEquals(1, confirmed)
    }

    // ── The translation list ────────────────────────────────────────────────

    private fun bible(id: String, title: String) = InstalledBible(
        id = id, fileName = "$id.spb", title = title, verseCount = 1, sizeBytes = 1, abbreviation = id.uppercase(),
    )

    @Test
    fun choosingATranslationPassesItsIdAndTheChosenOneIsMarked() = runComposeUiTest {
        var chosen: String? = null
        showScreen {
            TranslationList(
                installed = listOf(bible("kjv", "King James Version"), bible("rst", "Синодальный перевод")),
                activeId = "kjv",
                onChoose = { chosen = it },
                onManage = {},
                onGetMore = {},
                showHeader = true,
            )
        }

        tagged(BiblesTags.translation("kjv")).assertIsSelected()
        click(BiblesTags.translation("rst"))

        assertEquals("rst", chosen, "the second row must choose the second translation, not the first")
    }

    @Test
    fun manageAndGetMoreAreOfferedOnlyWhereTheyCanDoSomething() = runComposeUiTest {
        var managed = 0
        var gotMore = 0
        showScreen {
            TranslationList(emptyList(), "", {}, onManage = { managed++ }, onGetMore = { gotMore++ }, showHeader = true)
        }
        click(BiblesTags.MANAGE)
        click(BiblesTags.GET_MORE)
        assertEquals(1, managed)
        assertEquals(1, gotMore)

        showScreen { TranslationList(emptyList(), "", {}, onManage = null, onGetMore = null, showHeader = true) }
        assertFalse(exists(BiblesTags.MANAGE))
        assertFalse(exists(BiblesTags.GET_MORE))
    }

    @Test
    fun theSelectorOpensTheList() = runComposeUiTest {
        var opened = 0
        val kjv = bible("kjv", "King James Version")
        showScreen { TranslationSelector(kjv, installedCount = 2, onClick = { opened++ }) }

        click(BiblesTags.SELECTOR)

        assertEquals(1, opened)
    }

    @Test
    fun withNoBibleTheEmptyStateOffersGetBiblesAndOtherwiseTheFallback() = runComposeUiTest {
        var asked = 0
        showScreen { NoBibleInstalled(onGetBibles = { asked++ }) }
        click(BiblesTags.EMPTY_GET)
        assertEquals(1, asked)

        showScreen { NoBibleInstalled(onGetBibles = null, fallbackAction = { TranslationSelector(null, 0, {}) }) }
        assertFalse(exists(BiblesTags.EMPTY_GET))
        assertTrue(exists(BiblesTags.SELECTOR), "the web's fallback is shown in its place")
    }

    // ── The catalogue ───────────────────────────────────────────────────────

    private fun catalogBible(name: String, sizeBytes: Long = 4_600_000) = CatalogBible(
        source = BibleSource.EBIBLE,
        downloadKey = name,
        sizeBytes = sizeBytes,
        language = "ENG",
        languageName = "English",
        identifier = name,
        displayName = name,
        fileStem = "ENG_$name",
    )

    private fun ui(rows: List<CatalogRow>, snapshot: CatalogSnapshot = snapshotOf(rows)) = CatalogUi(
        snapshot = snapshot,
        view = CatalogView(rows = rows, sourceTotal = rows.size),
        isLoading = false,
        filter = CatalogFilter(),
        nowMs = 0L,
    )

    private fun snapshotOf(rows: List<CatalogRow>) =
        CatalogSnapshot(rows.map { it.bible }, mapOf(BibleSource.EBIBLE to SourceStatus.Fresh(0L)))

    private fun actions(onRow: (CatalogRow) -> Unit = {}, onRetry: () -> Unit = {}) =
        CatalogActions({}, {}, {}, onRow, onRetry)

    @Test
    fun tappingARowPassesThatRow() = runComposeUiTest {
        val rows = listOf(
            CatalogRow(catalogBible("KJV"), RowStatus.Installed),
            CatalogRow(catalogBible("WEB"), RowStatus.Available),
        )
        var tapped: CatalogRow? = null
        showScreen { CatalogList(ui(rows), actions(onRow = { tapped = it })) }

        click(BiblesTags.catalogRow(rows[1].bible.key))

        assertEquals("WEB", tapped?.bible?.displayName)
    }

    @Test
    fun eachRowShowsTheControlForItsState() = runComposeUiTest {
        val rows = listOf(
            CatalogRow(catalogBible("A"), RowStatus.Installed),
            CatalogRow(catalogBible("B"), RowStatus.Installing(0.62f)),
            CatalogRow(catalogBible("C"), RowStatus.Failed),
            CatalogRow(catalogBible("D", sizeBytes = 0), RowStatus.Available),
        )
        showScreen { CatalogList(ui(rows), actions()) }

        rows.forEach { row -> assertTrue(exists(BiblesTags.rowAction(row.bible.key)), row.bible.displayName) }
    }

    @Test
    fun nothingMatchingSaysSo() = runComposeUiTest {
        val someCatalogue = snapshotOf(listOf(CatalogRow(catalogBible("X"), RowStatus.Available)))
        showScreen { CatalogList(ui(emptyList(), someCatalogue), actions()) }

        assertTrue(exists(BiblesTags.CATALOG_NO_MATCH))
    }

    @Test
    fun aCatalogueThatDidNotLoadOffersARetry() = runComposeUiTest {
        var retried = 0
        val offline = CatalogSnapshot(emptyList(), mapOf(BibleSource.EBIBLE to SourceStatus.Offline))
        showScreen { CatalogList(ui(emptyList(), offline), actions(onRetry = { retried++ })) }

        assertTrue(exists(BiblesTags.CATALOG_FAILED))
        click(BiblesTags.CATALOG_RETRY)
        assertEquals(1, retried)
    }

    @Test
    fun aListFromAnEarlierVisitSaysItMayBeOutOfDate() = runComposeUiTest {
        val rows = listOf(CatalogRow(catalogBible("KJV"), RowStatus.Available))
        val stale = CatalogSnapshot(rows.map { it.bible }, mapOf(BibleSource.EBIBLE to SourceStatus.Stale(0L)))
        showScreen { CatalogList(ui(rows, stale), actions()) }

        assertTrue(exists(BiblesTags.CATALOG_STALE))
    }

    // ── Installing ───────────────────────────────────────────────────────────

    private class Pressed {
        var background = 0
        var cancel = 0
        var retry = 0
        var done = 0
        var open = 0
        val actions get() = InstallActions({ background++ }, { cancel++ }, { retry++ }, { done++ }, { open++ })
    }

    @Test
    fun aRunningInstallCanBeSentToTheBackgroundOrCancelled() = runComposeUiTest {
        val pressed = Pressed()
        val running = InstallState.Running(catalogBible("BSB"), InstallProgress(downloadedBytes = 1, totalBytes = 2))
        showScreen { InstallView(running, pressed.actions) }

        click(BiblesTags.INSTALL_BACKGROUND)
        click(BiblesTags.INSTALL_CANCEL)

        assertEquals(1, pressed.background)
        assertEquals(1, pressed.cancel)
        assertFalse(exists(BiblesTags.INSTALL_OPEN))
    }

    @Test
    fun aFailedInstallOffersRetry() = runComposeUiTest {
        val pressed = Pressed()
        val failed = InstallState.Failed(catalogBible("BSB"), InstallFailure.NETWORK, "offline", InstallProgress())
        showScreen { InstallView(failed, pressed.actions) }

        click(BiblesTags.INSTALL_RETRY)

        assertEquals(1, pressed.retry)
        assertFalse(exists(BiblesTags.INSTALL_BACKGROUND))
    }

    @Test
    fun aFinishedInstallOffersOpenInBible() = runComposeUiTest {
        val pressed = Pressed()
        val installed = InstalledBible("ENG_BSB", "ENG_BSB.spb", "Berean Standard Bible", 31_086, 4_600_000)
        showScreen { InstallView(InstallState.Done(catalogBible("BSB"), installed, 66, 31_086), pressed.actions) }

        click(BiblesTags.INSTALL_OPEN)
        click(BiblesTags.INSTALL_DONE)

        assertEquals(1, pressed.open)
        assertEquals(1, pressed.done)
    }

    // ── Converting a file ───────────────────────────────────────────────────

    private fun convertUi(phase: ConvertPhase, title: String = "Біблія", replaces: Boolean = false) = ConvertUi(
        file = if (phase == ConvertPhase.Empty) null else PickedBible("SF_UKR_OGI.xml", 5_400_000),
        phase = phase,
        title = title,
        abbreviation = "UKR_OGI",
        savedFileName = "UKR_OGI.spb",
        replaces = replaces,
    )

    private fun convertActions(onPick: () -> Unit = {}, onConvert: () -> Unit = {}) =
        ConvertActions(onPick, {}, {}, onConvert, {}, {})

    private val ready = ConvertPhase.Ready(
        DetectedBible(BibleXmlFormat.ZEFANIA, "Ukrainian", 66, 31_170, "John 3:16" to "Бо так"),
    )

    @Test
    fun withNoFileTheScreenOffersThePicker() = runComposeUiTest {
        var picked = 0
        showScreen { ConvertForm(convertUi(ConvertPhase.Empty), convertActions(onPick = { picked++ })) }

        click(BiblesTags.CONVERT_PICK)

        assertEquals(1, picked)
        assertFalse(exists(BiblesTags.CONVERT_INSTALL))
    }

    @Test
    fun aReadFileCanBeConvertedOnlyWithATitle() = runComposeUiTest {
        var converted = 0
        showScreen { ConvertForm(convertUi(ready), convertActions(onConvert = { converted++ })) }
        tagged(BiblesTags.CONVERT_INSTALL).assertIsEnabled()
        click(BiblesTags.CONVERT_INSTALL)
        assertEquals(1, converted)

        showScreen { ConvertForm(convertUi(ready, title = " "), convertActions()) }
        tagged(BiblesTags.CONVERT_INSTALL).assertIsNotEnabled()
    }

    @Test
    fun anUnreadableFileSaysSoAndOffersAnother() = runComposeUiTest {
        showScreen { ConvertForm(convertUi(ConvertPhase.Unreadable(InstallFailure.NOT_A_BIBLE)), convertActions()) }

        assertTrue(exists(BiblesTags.CONVERT_ERROR))
        assertTrue(exists(BiblesTags.CONVERT_PICK))
        assertFalse(exists(BiblesTags.CONVERT_INSTALL))
    }

    @Test
    fun replacingAnInstalledTranslationIsWarnedAboutAndOnlyThen() = runComposeUiTest {
        showScreen { ConvertForm(convertUi(ready, replaces = true), convertActions()) }
        assertTrue(exists(BiblesTags.CONVERT_REPLACES))

        showScreen { ConvertForm(convertUi(ready, replaces = false), convertActions()) }
        assertFalse(exists(BiblesTags.CONVERT_REPLACES))
    }

    // ── Managing what is installed ──────────────────────────────────────────

    @Test
    fun removingATranslationAsksFirstAndPassesItsId() = runComposeUiTest {
        var removed: String? = null
        var used: String? = null
        showScreen {
            InstalledList(
                installed = listOf(bible("kjv", "King James Version"), bible("rst", "Синодальный перевод")),
                activeId = "kjv",
                onUse = { used = it },
                onRemove = { removed = it },
            )
        }

        click(BiblesTags.remove("rst"))
        assertEquals(null, removed, "nothing is removed before the confirmation")
        click(BiblesTags.REMOVE_CONFIRM)
        assertEquals("rst", removed)

        click(BiblesTags.installedRow("rst"))
        assertEquals("rst", used)
    }
}
