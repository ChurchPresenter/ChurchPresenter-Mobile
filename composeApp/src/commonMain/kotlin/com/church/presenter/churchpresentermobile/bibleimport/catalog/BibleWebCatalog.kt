package com.church.presenter.churchpresentermobile.bibleimport.catalog

import com.church.presenter.churchpresentermobile.library.FileStore
import com.church.presenter.churchpresentermobile.library.createFileStore
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "BibleWebCatalog"

/** One source's list and how it was obtained. */
private typealias Loaded = Pair<List<CatalogBible>, SourceStatus>

/** How one source's list was obtained this time. */
sealed interface SourceStatus {
    /** Fetched now, or read from a copy younger than the cache's lifetime. */
    data class Fresh(val fetchedAtMs: Long) : SourceStatus

    /** The source could not be reached; this is the copy from [fetchedAtMs]. */
    data class Stale(val fetchedAtMs: Long) : SourceStatus

    /** GitHub is refusing this network for now, and nothing was cached to fall back on. */
    data class RateLimited(val resetEpochSeconds: Long?) : SourceStatus

    /** Unreachable, and nothing cached. */
    data object Offline : SourceStatus

    /** Reached, but what came back was not a usable catalogue. */
    data object Failed : SourceStatus
}

/** Every downloadable Bible, and how each source's part of the list was obtained. */
data class CatalogSnapshot(
    val bibles: List<CatalogBible>,
    val sources: Map<BibleSource, SourceStatus>,
) {
    fun count(source: BibleSource): Int = bibles.count { it.source == source }

    /** True when not a single source produced a list — the screen's "catalog failed to load". */
    val isUnavailable: Boolean get() = bibles.isEmpty()

    /** The oldest time any listed source was fetched, for "Catalog updated today". */
    val fetchedAtMs: Long?
        get() = sources.values.mapNotNull {
            when (it) {
                is SourceStatus.Fresh -> it.fetchedAtMs
                is SourceStatus.Stale -> it.fetchedAtMs
                else -> null
            }
        }.minOrNull()

    companion object {
        val EMPTY = CatalogSnapshot(emptyList(), emptyMap())
    }
}

/**
 * The three archives' catalogues, fetched and cached.
 *
 * Each list is kept for a week, as on the desktop. The archives change a few times a year, and
 * the Zefania listing comes from GitHub's API, which allows 60 unauthenticated requests an hour
 * per address — one church's shared Wi-Fi could exhaust that on its own. A fetch that fails
 * falls back to the last good copy, so an offline hall still sees the list it saw last time;
 * the failure then surfaces on the install, which is far more useful than an empty screen.
 *
 * eBible is read first because its catalogue names every language code in both spellings, and
 * the other two sources borrow those names.
 *
 * @param now Supplies the clock, so a test can age the cache.
 * @param work Where parsing and cache I/O run — a 1,300-row CSV is not work for the frame the
 *   operator is looking at.
 */
class BibleWebCatalog(
    private val fetcher: WebFetcher = KtorWebFetcher(),
    private val storage: FileStore = createFileStore(),
    private val now: () -> Long,
    private val work: CoroutineDispatcher = Dispatchers.Default,
) {
    private var memory: CatalogSnapshot? = null

    /**
     * The catalogue, from memory or storage when young enough, else from the network.
     *
     * @param refresh Skips the lifetime check — the operator asked for a fresh list.
     */
    suspend fun load(refresh: Boolean = false): CatalogSnapshot = withContext(work) { loadNow(refresh) }

    private suspend fun loadNow(refresh: Boolean): CatalogSnapshot {
        if (!refresh) memory?.let { return it }
        val ebible = loadSource(BibleSource.EBIBLE, EBibleCatalog.CATALOG_URL, refresh) { body ->
            EBibleCatalog.parse(body).takeIf { it.isNotEmpty() }
        }
        val names = BibleLanguageNames.resolve(EBibleCatalog.languageNames(ebible.first))
        // eBible's rows carry their own spelling each; relabel them from the settled table so a
        // language appears once in the picker, not once per spelling.
        val ebibleRows = ebible.first.map { row ->
            names[row.language]?.let { row.copy(languageName = it.english, languageNativeName = it.native) } ?: row
        }
        val zefania = loadSource(BibleSource.ZEFANIA, ZefaniaCatalog.CATALOG_URL, refresh, GITHUB_HEADERS) { body ->
            ZefaniaCatalog.parse(body, names)
        }
        val beblia = loadSource(BibleSource.BEBLIA, BebliaCatalog.CATALOG_URL, refresh) { body ->
            BebliaCatalog.parse(body, names)
        }
        val snapshot = CatalogSnapshot(
            bibles = ebibleRows + zefania.first + beblia.first,
            sources = mapOf(
                BibleSource.EBIBLE to ebible.second,
                BibleSource.ZEFANIA to zefania.second,
                BibleSource.BEBLIA to beblia.second,
            ),
        )
        memory = snapshot
        return snapshot
    }

    private suspend fun loadSource(
        source: BibleSource,
        url: String,
        refresh: Boolean,
        headers: Map<String, String> = emptyMap(),
        parse: (String) -> List<CatalogBible>?,
    ): Loaded {
        val cached = readCache(source)?.let { entry -> parse(entry.body)?.let { Cached(entry, it) } }
        val young = cached != null && now() - cached.entry.fetchedAtMs < CACHE_LIFETIME_MS
        return if (!refresh && young && cached != null) {
            cached.bibles to SourceStatus.Fresh(cached.entry.fetchedAtMs)
        } else {
            fetchSource(source, url, headers, cached, parse)
        }
    }

    /** The source's list from the network, or — when that fails — the cached copy as stale. */
    private suspend fun fetchSource(
        source: BibleSource,
        url: String,
        headers: Map<String, String>,
        cached: Cached?,
        parse: (String) -> List<CatalogBible>?,
    ): Loaded {
        val etag = cached?.entry?.etag?.takeIf { it.isNotBlank() }
        val response = fetchOrNull(source, url, if (etag != null) headers + ("If-None-Match" to etag) else headers)
            ?: return fallback(cached, SourceStatus.Offline)
        return when {
            // A 304 costs nothing against GitHub's rate limit, so a returning user spends no quota.
            response.status == HTTP_NOT_MODIFIED && cached != null -> {
                writeCache(source, cached.entry.body, cached.entry.etag)
                cached.bibles to SourceStatus.Fresh(now())
            }
            response.status == HTTP_FORBIDDEN && response.headers["x-ratelimit-remaining"] == "0" ->
                fallback(cached, SourceStatus.RateLimited(response.headers["x-ratelimit-reset"]?.toLongOrNull()))
            !response.isSuccess -> fallback(cached, SourceStatus.Failed)
            else -> {
                val body = response.body.decodeToString()
                parse(body)?.let { parsed ->
                    writeCache(source, body, response.headers["etag"].orEmpty())
                    parsed to SourceStatus.Fresh(now())
                } ?: fallback(cached, SourceStatus.Failed)
            }
        }
    }

    private suspend fun fetchOrNull(source: BibleSource, url: String, headers: Map<String, String>): WebResponse? = try {
        fetcher.get(url, headers) { _, _ -> }
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        // Ktor surfaces a dropped connection as several unrelated exception types per engine.
        Logger.e(TAG, "${source.name} catalogue fetch failed: ${e.message}")
        null
    }

    /** What a failed fetch leaves: the cached list as stale, or nothing and the reason. */
    private fun fallback(cached: Cached?, failure: SourceStatus): Loaded =
        cached?.let { it.bibles to SourceStatus.Stale(it.entry.fetchedAtMs) } ?: (emptyList<CatalogBible>() to failure)

    /** A cache file and the list it parses to. */
    private class Cached(val entry: CacheEntry, val bibles: List<CatalogBible>)

    private class CacheEntry(val body: String, val fetchedAtMs: Long, val etag: String)

    private fun readCache(source: BibleSource): CacheEntry? {
        val body = storage.read(bodyFile(source)) ?: return null
        val meta = storage.read(metaFile(source)).orEmpty().lines()
        return CacheEntry(body, meta.getOrNull(0)?.toLongOrNull() ?: 0L, meta.getOrNull(1).orEmpty())
    }

    private fun writeCache(source: BibleSource, body: String, etag: String) {
        runCatching {
            storage.write(bodyFile(source), body)
            storage.write(metaFile(source), "${now()}\n$etag")
        }.onFailure { Logger.e(TAG, "could not cache the ${source.name} catalogue: ${it.message}") }
    }

    // Not `bible_…`: LocalBibleRepository.clearAll sweeps that prefix as orphaned modules.
    private fun bodyFile(source: BibleSource) = "catalog_${source.name.lowercase()}.txt"

    private fun metaFile(source: BibleSource) = "catalog_${source.name.lowercase()}.meta"

    private companion object {
        const val CACHE_LIFETIME_MS = 7L * 24 * 60 * 60 * 1000
        const val HTTP_NOT_MODIFIED = 304
        const val HTTP_FORBIDDEN = 403
        val GITHUB_HEADERS = mapOf(
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
        )
    }
}
