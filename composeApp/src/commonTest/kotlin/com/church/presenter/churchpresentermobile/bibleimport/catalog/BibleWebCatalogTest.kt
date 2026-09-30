package com.church.presenter.churchpresentermobile.bibleimport.catalog

import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BibleWebCatalogTest {

    private val csv = """
        |languageCode,translationId,languageNameInEnglish,languageName,Redistributable,downloadable,shortTitle,OTbooks,NTbooks
        |ukr,ukrogi,Ukrainian,Українська,True,True,Біблія (Огієнко),39,27
    """.trimMargin().encodeToByteArray()

    private val tree = """
        {"truncated":false,"tree":[{"path":"zefania-sharp-sourceforge-backup/Bibles/UKR/x/SF_2009-01-20_UKR_OGI_(OGIENKO).zip","type":"blob","sha":"a","size":1}]}
    """.trimIndent().encodeToByteArray()

    private val beblia = (
        """{"commit":"c","bibles":[""" +
            """{"file":"UkrainianBible.xml","sha":"s","size":1,"title":"Ukrainian","id":"UKR","lang":"UKR"}]}"""
        )
        .encodeToByteArray()

    private val all = mapOf(
        EBibleCatalog.CATALOG_URL to ok(csv),
        ZefaniaCatalog.CATALOG_URL to ok(tree, mapOf("etag" to "\"v1\"")),
        BebliaCatalog.CATALOG_URL to ok(beblia),
    )

    @Test
    fun `the three sources are merged and Zefania borrows eBible's language names`() = runTest {
        val snapshot = BibleWebCatalog(FakeWebFetcher(all), InMemoryFileStorage(), now = { 1_000L }).load()
        assertEquals(3, snapshot.bibles.size)
        assertEquals(1, snapshot.count(BibleSource.ZEFANIA))
        assertEquals("Ukrainian", snapshot.bibles.first { it.source == BibleSource.ZEFANIA }.languageName)
        assertEquals(SourceStatus.Fresh(1_000L), snapshot.sources[BibleSource.EBIBLE])
    }

    @Test
    fun `a young cache is used without touching the network`() = runTest {
        val storage = InMemoryFileStorage()
        var clock = 0L
        BibleWebCatalog(FakeWebFetcher(all), storage, now = { clock }).load()
        clock = 60_000L
        val offline = FakeWebFetcher(emptyMap()).apply { failWith = IllegalStateException("offline") }
        val snapshot = BibleWebCatalog(offline, storage, now = { clock }).load()
        assertTrue(offline.requested.isEmpty())
        assertEquals(3, snapshot.bibles.size)
    }

    @Test
    fun `an old cache is refreshed and an unreachable source falls back to it as stale`() = runTest {
        val storage = InMemoryFileStorage()
        BibleWebCatalog(FakeWebFetcher(all), storage, now = { 0L }).load()
        val offline = FakeWebFetcher(emptyMap()).apply { failWith = IllegalStateException("offline") }
        val eightDays = 8L * 24 * 60 * 60 * 1000
        val snapshot = BibleWebCatalog(offline, storage, now = { eightDays }).load()
        assertEquals(3, offline.requested.size)
        assertEquals(3, snapshot.bibles.size)
        assertEquals(SourceStatus.Stale(0L), snapshot.sources[BibleSource.ZEFANIA])
    }

    @Test
    fun `nothing cached and nothing reachable is an unavailable catalogue`() = runTest {
        val offline = FakeWebFetcher(emptyMap()).apply { failWith = IllegalStateException("offline") }
        val snapshot = BibleWebCatalog(offline, InMemoryFileStorage(), now = { 0L }).load()
        assertTrue(snapshot.isUnavailable)
        assertEquals(SourceStatus.Offline, snapshot.sources[BibleSource.EBIBLE])
    }

    @Test
    fun `GitHub's rate limit is reported as such when there is no cache`() = runTest {
        val limited = all + (
            ZefaniaCatalog.CATALOG_URL to WebResponse(
                403, mapOf("x-ratelimit-remaining" to "0", "x-ratelimit-reset" to "1700000000"), ByteArray(0),
            )
            )
        val snapshot = BibleWebCatalog(FakeWebFetcher(limited), InMemoryFileStorage(), now = { 0L }).load()
        assertEquals(SourceStatus.RateLimited(1_700_000_000L), snapshot.sources[BibleSource.ZEFANIA])
        assertEquals(2, snapshot.bibles.size)
    }

    @Test
    fun `a refresh sends the cached etag and a 304 keeps the cached list`() = runTest {
        val storage = InMemoryFileStorage()
        BibleWebCatalog(FakeWebFetcher(all), storage, now = { 0L }).load()
        var sentEtag: String? = null
        val notModified = WebFetcher { url, headers, _ ->
            if (url == ZefaniaCatalog.CATALOG_URL) {
                sentEtag = headers["If-None-Match"]
                WebResponse(304, emptyMap(), ByteArray(0))
            } else {
                all.getValue(url)
            }
        }
        val snapshot = BibleWebCatalog(notModified, storage, now = { 5L }).load(refresh = true)
        assertEquals("\"v1\"", sentEtag)
        assertIs<SourceStatus.Fresh>(snapshot.sources[BibleSource.ZEFANIA])
        assertEquals(1, snapshot.count(BibleSource.ZEFANIA))
    }

    @Test
    fun `a body that does not parse is a failed source`() = runTest {
        val broken = all + (BebliaCatalog.CATALOG_URL to ok("<html/>".encodeToByteArray()))
        val snapshot = BibleWebCatalog(FakeWebFetcher(broken), InMemoryFileStorage(), now = { 0L }).load()
        assertEquals(SourceStatus.Failed, snapshot.sources[BibleSource.BEBLIA])
    }
}
