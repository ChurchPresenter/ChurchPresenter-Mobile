package com.church.presenter.churchpresentermobile.bibleimport.catalog

import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.library.StoredZip
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The download paths the happy-path tests do not take: eBible's zips, stale caches, odd rows. */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogEdgeCasesTest {

    private val usfx = """
        <usfx><languageCode>eng</languageCode>
        <book id="JHN"><c id="3"/><v id="16"/>For God so loved the world.<ve/></book></usfx>
    """.trimIndent().encodeToByteArray()

    private val bookNames = """<BookNames><book code="JHN" short="John (WEB)"/></BookNames>""".encodeToByteArray()

    private val web = CatalogBible(
        source = BibleSource.EBIBLE, downloadKey = "engwebp", language = "ENG", languageName = "English",
        identifier = "engwebp", displayName = "World English Bible", copyright = "", fileStem = "ENG_WEBP",
    )

    private fun TestScope.downloads(repository: LocalBibleRepository, body: ByteArray) = BibleDownloads(
        repository,
        FakeWebFetcher(mapOf(EBibleCatalog.downloadUrl("engwebp") to ok(body))),
        clock = { 0L },
        scope = this,
        convertDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private suspend fun BibleDownloads.finish(bible: CatalogBible): InstallState {
        install(bible)
        return states.first { it[bible.key] is InstallState.Done || it[bible.key] is InstallState.Failed }.getValue(
            bible.key,
        )
    }

    @Test
    fun `an eBible zip installs with the book names it ships`() = runTest {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        val zip = StoredZip.write(listOf("engwebp_usfx.xml" to usfx, "BookNames.xml" to bookNames))

        val done = assertIs<InstallState.Done>(downloads(repository, zip).finish(web))

        assertEquals("John (WEB)", done.installed.books.single().name)
        assertEquals("eBible.org", done.installed.origin)
        assertEquals("", done.installed.license, "the catalogue stated none and neither did the file")
    }

    @Test
    fun `a damaged BookNames file costs the names, not the Bible`() = runTest {
        val zip = StoredZip.write(listOf("engwebp_usfx.xml" to usfx, "BookNames.xml" to "<broken".encodeToByteArray()))

        val done = assertIs<InstallState.Done>(downloads(LocalBibleRepository(InMemoryFileStorage()), zip).finish(web))

        assertEquals("John", done.installed.books.single().name)
    }

    @Test
    fun `a zip with no USFX inside is corrupt`() = runTest {
        val zip = StoredZip.write(listOf("readme.xml" to "<a/>".encodeToByteArray()))

        val failed = assertIs<InstallState.Failed>(
            downloads(LocalBibleRepository(InMemoryFileStorage()), zip).finish(web),
        )

        assertEquals(InstallFailure.CORRUPT, failed.failure)
    }

    @Test
    fun `a Bible in an encoding the app cannot read says so`() = runTest {
        val xml = """<?xml version="1.0" encoding="Shift_JIS"?><usfx/>""".encodeToByteArray()
        val zip = StoredZip.write(listOf("engwebp_usfx.xml" to xml))

        val failed = assertIs<InstallState.Failed>(
            downloads(LocalBibleRepository(InMemoryFileStorage()), zip).finish(web),
        )

        assertEquals(InstallFailure.ENCODING, failed.failure)
    }

    @Test
    fun `a Bible with no verses in it is refused`() = runTest {
        val zip = StoredZip.write(listOf("engwebp_usfx.xml" to "<usfx><book id=\"GEN\"/></usfx>".encodeToByteArray()))

        val failed = assertIs<InstallState.Failed>(
            downloads(LocalBibleRepository(InMemoryFileStorage()), zip).finish(web),
        )

        assertEquals(InstallFailure.NO_VERSES, failed.failure)
    }

    @Test
    fun `a download of the wrong size is refused before its hash is even checked`() = runTest {
        val sized = web.copy(sizeBytes = 999_999)

        val failed = assertIs<InstallState.Failed>(
            downloads(LocalBibleRepository(InMemoryFileStorage()), usfx).finish(sized),
        )

        assertEquals(InstallFailure.CHECKSUM, failed.failure)
    }

    @Test
    fun `a file read as another language is labelled by what it turned out to be`() = runTest {
        val russianFolder = web.copy(language = "RUS", languageName = "Russian")
        val zip = StoredZip.write(listOf("engwebp_usfx.xml" to usfx))

        val done = assertIs<InstallState.Done>(
            downloads(LocalBibleRepository(InMemoryFileStorage()), zip).finish(russianFolder),
        )

        assertEquals("Russian", done.installed.languageName, "the catalogue's language is the one passed to USFX")
    }

    @Test
    fun `asking to install what is already queued changes nothing and failed installs alone can retry`() = runTest {
        val fetcher = FakeWebFetcher(emptyMap()).apply { gate = kotlinx.coroutines.CompletableDeferred() }
        val downloads = BibleDownloads(
            LocalBibleRepository(InMemoryFileStorage()), fetcher, clock = { 0L },
            scope = this, convertDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        downloads.install(web)
        downloads.install(web)
        testScheduler.advanceUntilIdle()
        assertEquals(1, fetcher.requested.size)

        downloads.retry(web.key)
        downloads.dismiss(web.key)
        assertIs<InstallState.Running>(
            downloads.states.value[web.key],
            "a running install is neither retried nor dismissed",
        )

        downloads.cancel(web.key)
        fetcher.gate!!.complete(Unit)
    }

    // ── The catalogue's cache ───────────────────────────────────────────────

    private val csv = """
        |languageCode,translationId,languageNameInEnglish,languageName,Redistributable,downloadable,shortTitle,title
        |eng,engkjv,English,English,True,True,,King James Version
        |eng,,English,English,True,True,Nameless,
        |eng,engid,,,True,True,,
    """.trimMargin().encodeToByteArray()

    @Test
    fun `a row with no id is dropped and names fall back title then id`() {
        val rows = EBibleCatalog.parse(csv.decodeToString())
        assertEquals(listOf("King James Version", "engid"), rows.map { it.displayName })
        assertEquals("ENG", rows.last().languageLabel, "no language name: the code stands in")
    }

    @Test
    fun `a catalogue with no id or language column lists nothing`() {
        assertTrue(EBibleCatalog.parse("title,shortTitle\nA,B").isEmpty())
        assertTrue(EBibleCatalog.parse("").isEmpty())
    }

    @Test
    fun `a second load is answered from memory and a refresh goes back to the network`() = runTest {
        val fetcher = FakeWebFetcher(mapOf(EBibleCatalog.CATALOG_URL to ok(csv)))
        val catalog = BibleWebCatalog(
            fetcher,
            InMemoryFileStorage(),
            now = { 0L },
            work = UnconfinedTestDispatcher(testScheduler),
        )

        catalog.load()
        val afterFirst = fetcher.requested.size
        catalog.load()
        assertEquals(afterFirst, fetcher.requested.size)

        catalog.load(refresh = true)
        assertEquals(afterFirst * 2, fetcher.requested.size)
    }

    @Test
    fun `an error status or an unreadable body falls back to the cached list as stale`() = runTest {
        val storage = InMemoryFileStorage()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        BibleWebCatalog(
            FakeWebFetcher(mapOf(EBibleCatalog.CATALOG_URL to ok(csv))),
            storage,
            now = { 0L },
            work = dispatcher,
        ).load()

        val erroring = FakeWebFetcher(mapOf(EBibleCatalog.CATALOG_URL to WebResponse(500, emptyMap(), ByteArray(0))))
        val afterError = BibleWebCatalog(erroring, storage, now = { 1L }, work = dispatcher).load(refresh = true)
        assertEquals(SourceStatus.Stale(0L), afterError.sources[BibleSource.EBIBLE])

        val garbage = FakeWebFetcher(mapOf(EBibleCatalog.CATALOG_URL to ok("no,columns".encodeToByteArray())))
        val afterGarbage = BibleWebCatalog(garbage, storage, now = { 2L }, work = dispatcher).load(refresh = true)
        assertIs<SourceStatus.Stale>(afterGarbage.sources[BibleSource.EBIBLE])
        assertEquals(2, afterGarbage.count(BibleSource.EBIBLE))
    }

    @Test
    fun `a rate limit with a cached list serves the cached list`() = runTest {
        val storage = InMemoryFileStorage()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val tree = """{"truncated":false,"tree":[""" +
            """{"path":"zefania-sharp-sourceforge-backup/Bibles/ENG/a/b.zip","type":"blob","sha":"s","size":1}]}"""
        BibleWebCatalog(
            FakeWebFetcher(mapOf(ZefaniaCatalog.CATALOG_URL to ok(tree.encodeToByteArray()))),
            storage,
            now = { 0L },
            work = dispatcher,
        ).load()

        val limited = FakeWebFetcher(
            mapOf(ZefaniaCatalog.CATALOG_URL to WebResponse(403, mapOf("x-ratelimit-remaining" to "0"), ByteArray(0))),
        )
        val snapshot = BibleWebCatalog(limited, storage, now = { 1L }, work = dispatcher).load(refresh = true)

        assertEquals(SourceStatus.Stale(0L), snapshot.sources[BibleSource.ZEFANIA])
        assertEquals(1, snapshot.count(BibleSource.ZEFANIA))
    }

    @Test
    fun `a Zefania file name outside the convention is still listed and a file at the top is not`() {
        val tree = """{"tree":[
            |{"path":"zefania-sharp-sourceforge-backup/Bibles/DEU/luther/Luther_1545.zip",
            |"type":"blob","sha":"a","size":1},
            |{"path":"zefania-sharp-sourceforge-backup/Bibles/loose.zip",
            |"type":"blob","sha":"b","size":1}]}""".trimMargin()
        val rows = ZefaniaCatalog.parse(tree, emptyMap())!!
        assertEquals(listOf("Luther 1545"), rows.map { it.displayName })
        assertEquals("1545", rows.single().identifier)
        assertEquals("DEU", rows.single().languageLabel)
    }

    @Test
    fun `a Holy Bible XML entry with no language is filed as unknown`() {
        val manifest = """{"commit":"c","bibles":[{"file":"X.xml","title":"X","lang":""}]}"""
        val row = BebliaCatalog.parse(manifest, emptyMap())!!.single()
        assertEquals("UND", row.language)
        assertEquals("", row.languageName)
    }
}
