package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlFormat
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogBible
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallFailure
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallProgress
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallState
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import com.church.presenter.churchpresentermobile.viewmodel.ConvertPhase
import com.church.presenter.churchpresentermobile.viewmodel.DetectedBible
import com.church.presenter.churchpresentermobile.viewmodel.PickedBible
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Bible tab's way in — the selector, its sheet on a phone and its popover on a tablet — and
 * the screens again in the light theme, whose colours are a separate set of choices.
 */
@OptIn(ExperimentalTestApi::class)
class BibleTabEntryTest {

    private fun module(
        title: String,
    ) = "##Title:\t$title\n1\tGenesis\t50\n-----\nB001C001V001\t1\t1\t1\tIn the beginning"

    private fun repository() = LocalBibleRepository(InMemoryFileStorage()).also {
        it.install("ENG_KJV.spb", module("King James Version"))
        it.install("RUS_RST.spb", module("Синодальный перевод"))
    }

    private fun ComposeUiTest.show(theme: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) =
        setContent { AppTheme(themeMode = theme) { content() } }

    @Test
    fun onAPhoneTheSelectorOpensASheetWhoseRowsChooseTheTranslation() = runComposeUiTest {
        val bibles = repository()
        show { BibleTranslationPicker(bibles, twoPane = false, onOpenBibles = {}) }

        click(BiblesTags.SELECTOR)
        awaitThat { exists(BiblesTags.translation("RUS_RST")) }
        click(BiblesTags.translation("RUS_RST"))

        assertEquals("RUS_RST", bibles.index.value.active?.id)
    }

    @Test
    fun onATabletTheSelectorOpensAPopoverWhoseRowsChooseTheTranslation() = runComposeUiTest {
        val bibles = repository()
        show(ThemeMode.LIGHT) { BibleTranslationPicker(bibles, twoPane = true, onOpenBibles = {}) }

        click(BiblesTags.SELECTOR)
        awaitThat { exists(BiblesTags.translation("RUS_RST")) }
        click(BiblesTags.translation("RUS_RST"))

        assertEquals("RUS_RST", bibles.index.value.active?.id)
    }

    @Test
    fun manageAndGetMoreOpenTheirPages() = runComposeUiTest {
        val opened = mutableListOf<BiblesPage>()
        show { BibleTranslationPicker(repository(), twoPane = false, onOpenBibles = { opened += it }) }

        click(BiblesTags.SELECTOR)
        awaitThat { exists(BiblesTags.MANAGE) }
        click(BiblesTags.MANAGE)
        click(BiblesTags.SELECTOR)
        awaitThat { exists(BiblesTags.GET_MORE) }
        click(BiblesTags.GET_MORE)

        assertEquals(listOf(BiblesPage.INSTALLED, BiblesPage.GET), opened)
    }

    @Test
    fun withNothingInstalledAndNowhereToGetOneThereIsNoSelector() = runComposeUiTest {
        val empty = LocalBibleRepository(InMemoryFileStorage())
        show { BibleTranslationPicker(empty, twoPane = false, onOpenBibles = null) }

        assertTrue(!exists(BiblesTags.SELECTOR))
    }

    @Test
    fun theEmptyTabOffersGetBiblesWhereItCanAndTheDesktopCopyOnTheWeb() = runComposeUiTest {
        val opened = mutableListOf<BiblesPage>()
        show { BibleTabEmpty(onOpenBibles = { opened += it }) }
        click(BiblesTags.EMPTY_GET)
        assertEquals(listOf(BiblesPage.GET), opened)

        show(ThemeMode.LIGHT) { BibleTabEmpty(onOpenBibles = null) }
        assertTrue(!exists(BiblesTags.EMPTY_GET))
        assertTrue(exists(BiblesTags.EMPTY))
    }

    // ── The rest, in the light theme ────────────────────────────────────────

    private val bible = CatalogBible(
        source = BibleSource.ZEFANIA, downloadKey = "x", sizeBytes = 1_500_000, language = "RUS",
        languageName = "Russian", identifier = "RST", displayName = "Синодальный перевод", fileStem = "RUS_RST",
    )

    private val noActions = InstallActions({}, {}, {}, {}, {})

    @Test
    fun theInstallStepsDrawInTheLightTheme() = runComposeUiTest {
        show(ThemeMode.LIGHT) { InstallView(InstallState.Queued(bible), noActions) }
        assertTrue(exists(BiblesTags.INSTALL_BACKGROUND))

        show(ThemeMode.LIGHT) {
            InstallView(InstallState.Failed(bible, InstallFailure.CHECKSUM, "", InstallProgress()), noActions)
        }
        assertTrue(exists(BiblesTags.INSTALL_RETRY))

        val installed = InstalledBible("RUS_RST", "RUS_RST.spb", "Синодальный перевод", 31_102, 1_500_000)
        show(ThemeMode.LIGHT) { InstallView(InstallState.Done(bible, installed, 66, 31_102), noActions) }
        assertTrue(exists(BiblesTags.INSTALL_OPEN))
    }

    @Test
    fun everyFailureHasItsOwnExplanation() = runComposeUiTest {
        InstallFailure.entries.forEach { failure ->
            show { InstallView(InstallState.Failed(bible, failure, "", InstallProgress()), noActions) }
            assertTrue(exists(BiblesTags.INSTALL_RETRY), failure.name)
        }
    }

    private fun convertUi(phase: ConvertPhase) = ConvertUi(
        file = PickedBible("bible.xml", 212_000),
        phase = phase,
        title = "Біблія",
        abbreviation = "UKR",
        savedFileName = "UKR.spb",
        replaces = false,
    )

    @Test
    fun theConvertFormDrawsEachOfItsStatesInTheLightTheme() = runComposeUiTest {
        var done = 0
        var opened = 0
        val actions = ConvertActions({}, {}, {}, {}, { done++ }, { opened++ })
        val detected = DetectedBible(BibleXmlFormat.BEBLIA, "Ukrainian", 1, 1, null)

        show(ThemeMode.LIGHT) { ConvertForm(convertUi(ConvertPhase.Reading), actions) }
        assertTrue(exists(BiblesTags.CONVERT_INSTALL), "the button waits, disabled, while the file is read")

        show(ThemeMode.LIGHT) { ConvertForm(convertUi(ConvertPhase.Ready(detected)), actions) }
        assertTrue(exists(BiblesTags.CONVERT_TITLE))

        show(ThemeMode.LIGHT) { ConvertForm(convertUi(ConvertPhase.Unreadable(InstallFailure.ENCODING)), actions) }
        assertTrue(exists(BiblesTags.CONVERT_ERROR))

        show(ThemeMode.LIGHT) { ConvertForm(convertUi(ConvertPhase.Installing), actions) }
        assertTrue(exists(BiblesTags.CONVERT_INSTALL))

        val installed = InstalledBible("UKR", "UKR.spb", "Біблія", 1, 1)
        show { ConvertForm(convertUi(ConvertPhase.Installed(installed)), actions) }
        click(BiblesTags.INSTALL_OPEN)
        click(BiblesTags.INSTALL_DONE)
        assertEquals(1, opened)
        assertEquals(1, done)
    }

    @Test
    fun theLicenceDialogForAFileDrawsInTheLightTheme() = runComposeUiTest {
        show(ThemeMode.LIGHT) {
            LicenceDialog(
                "Біблія",
                "",
                "",
                source = null,
                language = "",
                isReinstall = true,
                onConfirm = {},
                onDismiss = {},
            )
        }
        assertTrue(exists(BiblesTags.LICENCE_ACCEPT))

        show(ThemeMode.LIGHT) {
            LicenceDialog(
                "Luther",
                "L",
                "PD",
                BibleSource.BEBLIA,
                "XYZ",
                isReinstall = false,
                onConfirm = {},
                onDismiss = {},
            )
        }
        assertTrue(exists(BiblesTags.LICENCE_ACCEPT))
    }

    @Test
    fun theInstalledListDrawsInTheLightThemeAndSaysWhenItIsEmpty() = runComposeUiTest {
        show(ThemeMode.LIGHT) { InstalledList(emptyList(), "", {}, {}) }
        assertTrue(exists(BiblesTags.INSTALLED))

        val file = InstalledBible("A", "A.spb", "A", 1, 1, origin = "file")
        val downloaded = InstalledBible("B", "B.spb", "B", 1, 1, origin = "Zefania")
        show(ThemeMode.LIGHT) { InstalledList(listOf(file, downloaded), "A", {}, {}) }
        assertTrue(exists(BiblesTags.installedRow("B")))
    }
}
