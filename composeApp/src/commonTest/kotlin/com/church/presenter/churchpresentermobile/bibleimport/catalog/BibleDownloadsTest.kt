package com.church.presenter.churchpresentermobile.bibleimport.catalog

import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** A fetcher that serves canned bodies by URL, and can be told to hold a request open. */
internal class FakeWebFetcher(private val bodies: Map<String, WebResponse>) : WebFetcher {
    val requested = mutableListOf<String>()
    var gate: CompletableDeferred<Unit>? = null
    var failWith: Exception? = null

    override suspend fun get(
        url: String,
        headers: Map<String,
        String>,
        onProgress: (Long, Long?) -> Unit,
    ): WebResponse {
        requested += url
        failWith?.let { throw it }
        gate?.await()
        val response = bodies[url] ?: WebResponse(404, emptyMap(), ByteArray(0))
        onProgress(response.body.size.toLong() / 2, response.body.size.toLong())
        onProgress(response.body.size.toLong(), response.body.size.toLong())
        return response
    }
}

internal fun ok(body: ByteArray, headers: Map<String, String> = emptyMap()) = WebResponse(200, headers, body)

@OptIn(ExperimentalCoroutinesApi::class)
class BibleDownloadsTest {

    private val xml = """
        <bible translation="Luther 1912" status="Public Domain">
          <testament name="Old"><book number="1"><chapter number="1">
            <verse number="1">Am Anfang schuf Gott Himmel und Erde.</verse>
            <verse number="2">Und die Erde war wüst und leer.</verse>
          </chapter></book></testament>
        </bible>
    """.trimIndent().encodeToByteArray()

    private suspend fun luther(sha: String? = null) = CatalogBible(
        source = BibleSource.BEBLIA,
        downloadKey = "c0ffee/GermanLutherBible.xml",
        checksum = sha ?: gitBlobSha1(xml),
        sizeBytes = xml.size.toLong(),
        language = "DEU",
        languageName = "German",
        identifier = "LUTH1912",
        displayName = "Luther 1912",
        copyright = "Public Domain",
        otBookCount = 39,
        fileStem = "DEU_LUTH1912",
    )

    private val url = BebliaCatalog.downloadUrl("c0ffee/GermanLutherBible.xml")

    @Test
    fun `the git blob hash matches git's own`() = runTest {
        assertEquals("ce013625030ba8dba906f756967f9e9ca394464a", gitBlobSha1("hello\n".encodeToByteArray()))
    }

    @Test
    fun `a download is converted and installed under its catalogue name`() = runTest {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        val downloads = BibleDownloads(
            repository, FakeWebFetcher(mapOf(url to ok(xml))), clock = { 0L },
            scope = this, convertDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val bible = luther()
        downloads.install(bible)
        val done = downloads.states.first { it[bible.key] is InstallState.Done }[bible.key] as InstallState.Done

        assertEquals(1, done.books)
        assertEquals(2, done.verses)
        val installed = repository.index.value.bibles.single()
        assertEquals("DEU_LUTH1912.spb", installed.fileName)
        assertEquals("Luther 1912", installed.title)
        assertEquals("German", installed.languageName)
        assertEquals("Holy Bible XML", installed.origin)
        assertEquals(bible.key, installed.catalogKey)
        assertEquals("1. Mose", installed.books.single().name)
        assertEquals("Und die Erde war wüst und leer.", repository.openActive()!!.chapter(1, 1)[1].text)
    }

    @Test
    fun `a body whose hash does not match the catalogue is refused and nothing is stored`() = runTest {
        val repository = LocalBibleRepository(InMemoryFileStorage())
        val downloads = BibleDownloads(
            repository, FakeWebFetcher(mapOf(url to ok(xml))), clock = { 0L },
            scope = this, convertDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val bible = luther(sha = "0000000000000000000000000000000000000000")
        downloads.install(bible)
        val failed = downloads.states.first { it[bible.key] is InstallState.Failed }[bible.key] as InstallState.Failed

        assertEquals(InstallFailure.CHECKSUM, failed.failure)
        assertEquals(InstallStep.DOWNLOAD, failed.progress.step)
        assertTrue(repository.index.value.isEmpty)
    }

    @Test
    fun `an error page is an HTTP failure`() = runTest {
        val downloads = BibleDownloads(
            LocalBibleRepository(InMemoryFileStorage()), FakeWebFetcher(emptyMap()), clock = { 0L },
            scope = this, convertDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val bible = luther()
        downloads.install(bible)
        val failed = downloads.states.first { it[bible.key] is InstallState.Failed }[bible.key] as InstallState.Failed
        assertEquals(InstallFailure.HTTP, failed.failure)
    }

    @Test
    fun `no connection is a network failure that can be retried`() = runTest {
        val fetcher = FakeWebFetcher(mapOf(url to ok(xml))).apply { failWith = IllegalStateException("offline") }
        val downloads = BibleDownloads(
            LocalBibleRepository(InMemoryFileStorage()), fetcher, clock = { 0L },
            scope = this, convertDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val bible = luther()
        downloads.install(bible)
        val failed = downloads.states.first { it[bible.key] is InstallState.Failed }[bible.key] as InstallState.Failed
        assertEquals(InstallFailure.NETWORK, failed.failure)

        fetcher.failWith = null
        downloads.retry(bible.key)
        downloads.states.first { it[bible.key] is InstallState.Done }
    }

    @Test
    fun `something that is not a Bible is refused as such`() = runTest {
        val html = "<html><body>Sign in to the Wi-Fi</body></html>".encodeToByteArray()
        val downloads = BibleDownloads(
            LocalBibleRepository(InMemoryFileStorage()), FakeWebFetcher(mapOf(url to ok(html))), clock = { 0L },
            scope = this, convertDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val bible = luther().copy(checksum = "", sizeBytes = 0)
        downloads.install(bible)
        val failed = downloads.states.first { it[bible.key] is InstallState.Failed }[bible.key] as InstallState.Failed
        assertEquals(InstallFailure.NOT_A_BIBLE, failed.failure)
    }

    @Test
    fun `a zip source that is not a zip is corrupt`() = runTest {
        val zefania = CatalogBible(
            source = BibleSource.ZEFANIA, downloadKey = "Bibles/DEU/x.zip", language = "DEU",
            identifier = "X", displayName = "X", fileStem = "DEU_X",
        )
        val downloads = BibleDownloads(
            LocalBibleRepository(InMemoryFileStorage()),
            FakeWebFetcher(mapOf(ZefaniaCatalog.downloadUrl(zefania.downloadKey) to ok(xml))),
            clock = { 0L }, scope = this, convertDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        downloads.install(zefania)
        val states = downloads.states.first { it[zefania.key] is InstallState.Failed }
        val failed = states[zefania.key] as InstallState.Failed
        assertEquals(InstallFailure.CORRUPT, failed.failure)
    }

    @Test
    fun `installs run one at a time and the second waits queued`() = runTest {
        val fetcher = FakeWebFetcher(mapOf(url to ok(xml))).apply { gate = CompletableDeferred() }
        val downloads = BibleDownloads(
            LocalBibleRepository(InMemoryFileStorage()), fetcher, clock = { 0L },
            scope = this, convertDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val first = luther()
        val second = first.copy(downloadKey = "c0ffee/Other.xml", fileStem = "DEU_OTHER")
        downloads.install(first)
        downloads.install(second)
        testScheduler.advanceUntilIdle()

        assertIs<InstallState.Running>(downloads.states.value[first.key])
        assertIs<InstallState.Queued>(downloads.states.value[second.key])
        assertEquals(1, fetcher.requested.size)

        fetcher.gate!!.complete(Unit)
        downloads.states.first { it[first.key] is InstallState.Done }
        downloads.states.first { it[second.key] is InstallState.Failed }
    }

    @Test
    fun `a cancelled install disappears and stores nothing`() = runTest {
        val fetcher = FakeWebFetcher(mapOf(url to ok(xml))).apply { gate = CompletableDeferred() }
        val repository = LocalBibleRepository(InMemoryFileStorage())
        val downloads = BibleDownloads(
            repository, fetcher, clock = { 0L },
            scope = this, convertDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val bible = luther()
        downloads.install(bible)
        testScheduler.advanceUntilIdle()
        downloads.cancel(bible.key)
        fetcher.gate!!.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertFalse(bible.key in downloads.states.value)
        assertTrue(repository.index.value.isEmpty)
    }

    @Test
    fun `a finished install can be dismissed but a running one cannot`() = runTest {
        val downloads = BibleDownloads(
            LocalBibleRepository(InMemoryFileStorage()), FakeWebFetcher(mapOf(url to ok(xml))), clock = { 0L },
            scope = this, convertDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val bible = luther()
        downloads.install(bible)
        downloads.states.first { it[bible.key] is InstallState.Done }
        downloads.dismiss(bible.key)
        assertFalse(bible.key in downloads.states.value)
    }
}
