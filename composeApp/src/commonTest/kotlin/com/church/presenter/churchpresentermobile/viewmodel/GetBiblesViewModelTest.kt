package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.bibleimport.catalog.BebliaCatalog
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleDownloads
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleWebCatalog
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogBible
import com.church.presenter.churchpresentermobile.bibleimport.catalog.EBibleCatalog
import com.church.presenter.churchpresentermobile.bibleimport.catalog.FakeWebFetcher
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallFailure
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallProgress
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallState
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallStep
import com.church.presenter.churchpresentermobile.bibleimport.catalog.gitBlobSha1
import com.church.presenter.churchpresentermobile.bibleimport.catalog.ok
import com.church.presenter.churchpresentermobile.bibleimport.ConvertProgress
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The catalogue screen's rules: nothing downloads before the copyright is confirmed, the list is
 * alphabetical, and each row says where it stands on this device.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetBiblesViewModelTest {

    private val xml = """
        <bible translation="Luther 1912" status="Public Domain">
          <testament name="Old"><book number="1"><chapter number="1">
            <verse number="1">Am Anfang schuf Gott Himmel und Erde.</verse>
          </chapter></book></testament>
        </bible>
    """.trimIndent().encodeToByteArray()

    private val csv = """
        |languageCode,translationId,languageNameInEnglish,languageName,Redistributable,downloadable,shortTitle,OTbooks,NTbooks
        |eng,engwebp,English,English,True,True,World English Bible,39,27
        |aai,aai,'Auhelawa,'Auhelawa,True,True,'Auhelawa NT,0,27
        |deu,deu1912,German,Deutsch,True,True,Luther 1912,39,27
    """.trimMargin().encodeToByteArray()

    private suspend fun lutherRow(): CatalogBible = CatalogBible(
        source = BibleSource.BEBLIA,
        downloadKey = "c/GermanLutherBible.xml",
        checksum = gitBlobSha1(xml),
        sizeBytes = xml.size.toLong(),
        language = "DEU",
        languageName = "German",
        identifier = "LUTH1912",
        displayName = "Luther 1912",
        fileStem = "DEU_LUTH1912",
    )

    private suspend fun TestScope.vm(
        repository: LocalBibleRepository = LocalBibleRepository(InMemoryFileStorage()),
    ): GetBiblesViewModel {
        val fetcher = FakeWebFetcher(
            mapOf(
                EBibleCatalog.CATALOG_URL to ok(csv),
                BebliaCatalog.downloadUrl(lutherRow().downloadKey) to ok(xml),
            ),
        )
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val catalog = BibleWebCatalog(fetcher, InMemoryFileStorage(), now = { 0L }, work = dispatcher)
        val downloads = BibleDownloads(
            repository,
            fetcher,
            clock = { 0L },
            scope = backgroundScope,
            convertDispatcher = dispatcher,
        )
        return GetBiblesViewModel(catalog, downloads, repository)
    }

    @Test
    fun `asking to install shows the copyright confirmation and downloads nothing`() = runVmTestUnconfined {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        val vm = vm(repository)
        try {
            val luther = lutherRow()
            vm.requestInstall(luther)

            assertEquals(luther, vm.licenceFor.value)
            assertTrue(vm.installs.value.isEmpty(), "no install may start before the confirmation")
            assertNull(vm.sheet.key.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `declining the confirmation installs nothing`() = runVmTestUnconfined {
        val vm = vm()
        try {
            vm.requestInstall(lutherRow())
            vm.dismissLicence()

            assertNull(vm.licenceFor.value)
            assertTrue(vm.installs.value.isEmpty())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `confirming starts the install and opens its sheet`() = runVmTestUnconfined {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        val vm = vm(repository)
        try {
            val luther = lutherRow()
            vm.requestInstall(luther)
            vm.confirmLicence()

            assertNull(vm.licenceFor.value)
            assertEquals(luther.key, vm.sheet.key.value)
            vm.installs.first { it[luther.key] is InstallState.Done }
            assertEquals("DEU_LUTH1912.spb", repository.index.value.bibles.single().fileName)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `confirming with nothing pending does nothing`() = runVmTestUnconfined {
        val vm = vm()
        try {
            vm.confirmLicence()

            assertTrue(vm.installs.value.isEmpty())
            assertNull(vm.sheet.key.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `open in Bible makes the new translation the one read and closes the sheet`() = runVmTestUnconfined {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        repository.install("en_KJV.spb", "##Title:\tKJV\n1\tGenesis\t1\n-----\nB001C001V001\t1\t1\t1\tIn the beginning")
        val vm = vm(repository)
        try {
            val luther = lutherRow()
            vm.requestInstall(luther)
            vm.confirmLicence()
            vm.installs.first { it[luther.key] is InstallState.Done }

            vm.sheet.openInBible(luther.key)

            assertEquals("Luther 1912", repository.index.value.active?.title)
            assertNull(vm.sheet.key.value)
            assertFalse(luther.key in vm.installs.value, "a finished install is forgotten once seen")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `running in the background closes the sheet but not the install`() = runVmTestUnconfined {
        val vm = vm()
        try {
            vm.sheet.show("SOME:key")
            vm.sheet.hide()

            assertNull(vm.sheet.key.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a translation already installed is a reinstall by key or by file name`() = runVmTestUnconfined {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        val vm = vm(repository)
        try {
            val luther = lutherRow()
            assertFalse(vm.isReinstall(luther))

            repository.install(
                "DEU_LUTH1912.spb",
                "##Title:\tLuther\n1\tGenesis\t1\n-----\nB001C001V001\t1\t1\t1\tAm Anfang",
            )

            assertTrue(vm.isReinstall(luther))
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `the catalogue loads on open and filters narrow it`() = runVmTestUnconfined {
        val vm = vm()
        try {
            vm.view.first { it.rows.isNotEmpty() }
            assertEquals(3, vm.view.value.rows.size)

            vm.setLanguage("DEU")
            assertEquals(listOf("Luther 1912"), vm.view.value.rows.map { it.bible.displayName })

            vm.setLanguage(null)
            vm.setQuery("world")
            assertEquals(listOf("World English Bible"), vm.view.value.rows.map { it.bible.displayName })

            vm.setQuery("")
            vm.setSource(BibleSource.ZEFANIA)
            assertTrue(vm.view.value.rows.isEmpty())
        } finally {
            tearDown(vm)
        }
    }

    // ── buildCatalogView, the pure half ──────────────────────────────────────

    private fun row(name: String, language: String, languageName: String, source: BibleSource = BibleSource.EBIBLE) =
        CatalogBible(
            source = source,
            downloadKey = name,
            language = language,
            languageName = languageName,
            identifier = name,
            displayName = name,
            fileStem = "${language}_$name",
        )

    private val rows = listOf(
        row("Zulu Bible", "ZUL", "Zulu"),
        row("'Auhelawa NT", "KUD", "'Auhelawa"),
        row("World English Bible", "ENG", "English"),
        row("King James Version", "ENG", "English", BibleSource.ZEFANIA),
        row("Abau NT", "AAU", "Abau"),
    )

    @Test
    fun `rows are alphabetical by language then name ignoring a leading apostrophe`() {
        val view = buildCatalogView(rows, CatalogFilter(), emptyList(), emptyMap())

        assertEquals(
            listOf("Abau NT", "'Auhelawa NT", "King James Version", "World English Bible", "Zulu Bible"),
            view.rows.map { it.bible.displayName },
        )
    }

    @Test
    fun `the language picker is alphabetical with a count for each`() {
        val view = buildCatalogView(rows, CatalogFilter(), emptyList(), emptyMap())

        assertEquals(listOf(null, "AAU", "KUD", "ENG", "ZUL"), view.languages.map { it.code })
        assertEquals(2, view.languages.first { it.code == "ENG" }.count)
        assertEquals(5, view.languages.first { it.code == null }.count, "All languages counts the source")
    }

    @Test
    fun `a source narrows both the rows and the languages`() {
        val view = buildCatalogView(rows, CatalogFilter(source = BibleSource.ZEFANIA), emptyList(), emptyMap())

        assertEquals(listOf("King James Version"), view.rows.map { it.bible.displayName })
        assertEquals(1, view.sourceTotal)
        assertEquals(listOf(null, "ENG"), view.languages.map { it.code })
    }

    @Test
    fun `a row already on the device reads as installed by its key or its file name`() {
        val byKey = installed(catalogKey = rows[0].key, fileName = "other.spb")
        val byFile = installed(catalogKey = "", fileName = rows[4].fileName)
        val view = buildCatalogView(rows, CatalogFilter(), listOf(byKey, byFile), emptyMap())

        val status = view.rows.associate { it.bible.displayName to it.status }
        assertEquals(RowStatus.Installed, status["Zulu Bible"])
        assertEquals(RowStatus.Installed, status["Abau NT"])
        assertEquals(RowStatus.Available, status["World English Bible"])
    }

    @Test
    fun `installs in flight show as queued installing or failed`() {
        val queued = rows[0]
        val running = rows[1]
        val failed = rows[2]
        val installs = mapOf(
            queued.key to InstallState.Queued(queued),
            running.key to InstallState.Running(running, InstallProgress(downloadedBytes = 50, totalBytes = 100)),
            failed.key to InstallState.Failed(
                failed,
                InstallFailure.NETWORK,
                "offline",
                InstallProgress(),
            ),
        )
        val status = buildCatalogView(
            rows,
            CatalogFilter(),
            emptyList(),
            installs,
        ).rows.associate { it.bible.key to it.status }

        assertEquals(RowStatus.Queued, status[queued.key])
        assertEquals(RowStatus.Installing(0.25f), status[running.key])
        assertEquals(RowStatus.Failed, status[failed.key])
    }

    @Test
    fun `languages of installed Bibles are the picker's quick chips`() {
        val english = installed(catalogKey = "", fileName = "x.spb").copy(languageName = "English")
        val view = buildCatalogView(rows, CatalogFilter(), listOf(english), emptyMap())

        assertEquals(listOf("ENG"), view.deviceLanguages.map { it.code })
    }

    @Test
    fun `the progress bar is the download then the conversion`() {
        val bible = rows[0]
        val downloading = InstallState.Running(bible, InstallProgress(downloadedBytes = 30, totalBytes = 60))
        val converting = InstallState.Running(
            bible,
            InstallProgress(step = InstallStep.CONVERT, downloadMs = 10, convert = ConvertProgress(0.5f, 3, 100)),
        )
        val sizeUnknown = InstallState.Running(bible, InstallProgress(downloadedBytes = 30))

        assertEquals(0.25f, installFraction(downloading))
        assertEquals(0.75f, installFraction(converting))
        assertEquals(0f, installFraction(sizeUnknown))
    }

    private fun installed(catalogKey: String, fileName: String) = InstalledBible(
        id = fileName.substringBefore('.'),
        fileName = fileName,
        title = fileName,
        verseCount = 1,
        sizeBytes = 1,
        catalogKey = catalogKey,
    )
}
