package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BebliaCatalog
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleDownloads
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleWebCatalog
import com.church.presenter.churchpresentermobile.bibleimport.catalog.EBibleCatalog
import com.church.presenter.churchpresentermobile.bibleimport.catalog.FakeWebFetcher
import com.church.presenter.churchpresentermobile.bibleimport.catalog.ok
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.tagged
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import com.church.presenter.churchpresentermobile.viewmodel.BibleChoiceViewModel
import com.church.presenter.churchpresentermobile.viewmodel.ConvertBibleViewModel
import com.church.presenter.churchpresentermobile.viewmodel.GetBiblesViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Bibles screen end to end, over its real ViewModels and a fake network: the catalogue
 * arrives, a row asks for the copyright confirmation, confirming installs, and "Open in Bible"
 * leaves the new translation as the one read. The phone's one page at a time, and the tablet's
 * three panes.
 *
 * ViewModel-backed, so it is named in the build's list of UI tests the wasm runtime skips: the
 * catalogue load completes on a real dispatcher that the wasm test clock cannot wait for.
 */
@OptIn(ExperimentalTestApi::class)
class BiblesScreenTest {

    private val xml = """
        <bible translation="Luther 1912" status="Public Domain">
          <testament name="Old"><book number="1"><chapter number="1">
            <verse number="1">Am Anfang schuf Gott Himmel und Erde.</verse>
          </chapter></book></testament>
        </bible>
    """.trimIndent().encodeToByteArray()

    private val csv = """
        |languageCode,translationId,languageNameInEnglish,languageName,Redistributable,downloadable,shortTitle,OTbooks,NTbooks
        |eng,engkjv,English,English,True,True,King James Version,39,27
        |eng,engwebp,English,English,True,True,World English Bible,39,27
    """.trimMargin().encodeToByteArray()

    private val lutherKey = "BEBLIA:c0ffee/GermanLutherBible.xml"

    private class Screen(val repository: LocalBibleRepository, val vms: BiblesViewModels) {
        var closed = 0
        var opened = 0
    }

    private fun screen(): Screen {
        // No hash listed, so the install skips that check; the size still has to match.
        val manifest = """{"commit":"c0ffee","bibles":[{"file":"GermanLutherBible.xml","sha":"",""" +
            """"size":${xml.size},"title":"Luther 1912","id":"LUTH1912","lang":"DEU","langName":"German","ot":39}]}"""
        val fetcher = FakeWebFetcher(
            mapOf(
                EBibleCatalog.CATALOG_URL to ok(csv),
                BebliaCatalog.CATALOG_URL to ok(manifest.encodeToByteArray()),
                BebliaCatalog.downloadUrl("c0ffee/GermanLutherBible.xml") to ok(xml),
            ),
        )
        val repository = LocalBibleRepository(InMemoryFileStorage())
        repository.install(
            "ENG_KJV.spb",
            "##Title:\tKing James Version\n1\tGenesis\t50\n-----\nB001C001V001\t1\t1\t1\tIn the beginning",
        )
        val catalog = BibleWebCatalog(fetcher, InMemoryFileStorage(), now = { 0L })
        val downloads = BibleDownloads(repository, fetcher, clock = { 0L })
        val vms = BiblesViewModels(
            get = GetBiblesViewModel(catalog, downloads, repository),
            convert = ConvertBibleViewModel(repository),
            choice = BibleChoiceViewModel(repository),
        )
        return Screen(repository, vms)
    }

    private fun ComposeUiTest.show(
        screen: Screen,
        page: BiblesPage,
        twoPane: Boolean = false,
        theme: ThemeMode = ThemeMode.DARK,
    ) {
        val content: @Composable () -> Unit = {
            BiblesScreen(
                page = page,
                twoPane = twoPane,
                repository = screen.repository,
                downloads = BibleDownloads(screen.repository, FakeWebFetcher(emptyMap()), clock = { 0L }),
                catalog = BibleWebCatalog(FakeWebFetcher(emptyMap()), InMemoryFileStorage(), now = { 0L }),
                onClose = { screen.closed++ },
                onOpenedBible = { screen.opened++ },
                providedViewModels = screen.vms,
            )
        }
        setContent { AppTheme(themeMode = theme) { content() } }
    }

    private fun ComposeUiTest.awaitRow(key: String) = awaitThat { exists(BiblesTags.catalogRow(key)) }

    // ── Phone ────────────────────────────────────────────────────────────────

    @Test
    fun theCatalogueArrivesAndARowAsksForTheCopyrightConfirmation() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.GET)
        awaitRow(lutherKey)

        click(BiblesTags.catalogRow(lutherKey))

        awaitThat { exists(BiblesTags.LICENCE) }
        assertEquals(1, screen.repository.index.value.bibles.size, "nothing is installed before the confirmation")
    }

    @Test
    fun confirmingInstallsAndOpenInBibleMakesItTheOneRead() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.GET)
        awaitRow(lutherKey)
        click(BiblesTags.catalogRow(lutherKey))
        awaitThat { exists(BiblesTags.LICENCE_ACCEPT) }

        click(BiblesTags.LICENCE_ACCEPT)
        awaitThat { exists(BiblesTags.INSTALL_OPEN) }
        click(BiblesTags.INSTALL_OPEN)

        assertEquals(1, screen.opened)
        assertEquals("Luther 1912", screen.repository.index.value.active?.title)
    }

    @Test
    fun cancellingTheConfirmationInstallsNothing() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.GET)
        awaitRow(lutherKey)
        click(BiblesTags.catalogRow(lutherKey))
        awaitThat { exists(BiblesTags.LICENCE_CANCEL) }

        click(BiblesTags.LICENCE_CANCEL)

        awaitThat { !exists(BiblesTags.LICENCE) }
        assertFalse(exists(BiblesTags.INSTALL_SHEET))
        assertEquals(1, screen.repository.index.value.bibles.size)
    }

    @Test
    fun anInstalledRowDoesNothingWhenTapped() = runComposeUiTest {
        val screen = screen()
        screen.repository.install(
            "ENG_WEBP.spb",
            "##Title:\tWEB\n1\tGenesis\t50\n-----\nB001C001V001\t1\t1\t1\tIn the beginning",
        )
        show(screen, BiblesPage.GET)
        awaitRow("EBIBLE:engwebp")

        click(BiblesTags.catalogRow("EBIBLE:engwebp"))

        assertFalse(exists(BiblesTags.LICENCE), "an installed translation is not offered again from its row")
    }

    @Test
    fun backClosesTheScreen() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.GET)

        click(BiblesTags.CATALOG_BACK)

        assertEquals(1, screen.closed)
    }

    @Test
    fun theConvertPageOffersThePicker() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.CONVERT)

        assertTrue(exists(BiblesTags.CONVERT))
        assertTrue(exists(BiblesTags.CONVERT_PICK))
    }

    @Test
    fun theInstalledPageListsTheDevicesTranslations() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.INSTALLED)

        assertTrue(exists(BiblesTags.installedRow("ENG_KJV")))
    }

    @Test
    fun theCatalogueDrawsInTheLightThemeToo() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.GET, theme = ThemeMode.LIGHT)
        awaitRow(lutherKey)
        click(BiblesTags.catalogRow(lutherKey))

        awaitThat { exists(BiblesTags.LICENCE_ACCEPT) }
        click(BiblesTags.LICENCE_ACCEPT)
        awaitThat { exists(BiblesTags.INSTALL_DONE) }
        click(BiblesTags.INSTALL_DONE)

        awaitThat { !exists(BiblesTags.INSTALL_SHEET) }
    }

    // ── Tablet ───────────────────────────────────────────────────────────────

    @Test
    fun onATabletARowOpensItsDetailBeforeAnythingIsAsked() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.GET, twoPane = true)
        awaitRow(lutherKey)
        assertTrue(exists(BiblesTags.DETAIL_EMPTY))

        click(BiblesTags.catalogRow(lutherKey))
        assertFalse(exists(BiblesTags.LICENCE), "choosing a row only shows it on a tablet")

        click(BiblesTags.rowAction("detail"))
        awaitThat { exists(BiblesTags.LICENCE) }
    }

    @Test
    fun theTabletInstallsFromTheDetailPane() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.GET, twoPane = true, theme = ThemeMode.LIGHT)
        awaitRow(lutherKey)
        click(BiblesTags.catalogRow(lutherKey))
        click(BiblesTags.rowAction("detail"))
        awaitThat { exists(BiblesTags.LICENCE_ACCEPT) }

        click(BiblesTags.LICENCE_ACCEPT)

        awaitThat { exists(BiblesTags.INSTALL_OPEN) }
        click(BiblesTags.INSTALL_OPEN)
        assertEquals(1, screen.opened)
    }

    @Test
    fun theTabletNavigationSwitchesPages() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.GET, twoPane = true)

        click(BiblesTags.NAV_INSTALLED)
        awaitThat { exists(BiblesTags.INSTALLED) }
        tagged(BiblesTags.NAV_INSTALLED).assertIsSelected()

        click(BiblesTags.NAV_CONVERT)
        awaitThat { exists(BiblesTags.CONVERT) }

        click(BiblesTags.NAV_GET)
        awaitThat { exists(BiblesTags.CATALOG_SEARCH) }
    }

    @Test
    fun aSourceCardNarrowsTheCatalogueToThatSource() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.GET, twoPane = true)
        awaitRow(lutherKey)

        click(BiblesTags.source("card_EBIBLE"))

        awaitThat { !exists(BiblesTags.catalogRow(lutherKey)) }
        assertTrue(exists(BiblesTags.catalogRow("EBIBLE:engkjv")))
    }

    @Test
    fun theTabletsLanguageDropdownFiltersTheCatalogue() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.GET, twoPane = true)
        awaitRow(lutherKey)

        click(BiblesTags.LANGUAGE_BUTTON)
        awaitThat { exists(BiblesTags.language("DEU")) }
        click(BiblesTags.language("DEU"))

        awaitThat { !exists(BiblesTags.catalogRow("EBIBLE:engkjv")) }
        assertTrue(exists(BiblesTags.catalogRow(lutherKey)))
    }

    @Test
    fun theTabletBackClosesTheScreen() = runComposeUiTest {
        val screen = screen()
        show(screen, BiblesPage.INSTALLED, twoPane = true)

        click(BiblesTags.CATALOG_BACK)

        assertEquals(1, screen.closed)
    }
}
