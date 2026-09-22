package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.library.FileStore
import com.church.presenter.churchpresentermobile.library.createFileStore
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDurations
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "SongCatalogStore"
internal const val SONG_CATALOG_FILE = "song-catalog.json"
private const val SONGBOOK_CHARS = 120
private const val TITLE_CHARS = 200
private const val NUMBER_CHARS = 32
private const val SONGS_PER_RECORD = 2_000
private const val RECORDS_MAX = 200
private const val MAX_SECONDS = 24 * 60 * 60

@Serializable
private data class StoredCatalog(val records: Map<String, CatalogRecord> = emptyMap())

/**
 * The desktop's songbooks as this phone last received them -- from the relay while planning away
 * from church, from the LAN when a desktop answers -- so a service can be planned with the real
 * song list and each song's usual length without the library ever having been copied here.
 *
 * Its own file: the catalog is rewritten only when a book changes, never on a plan edit.
 */
class SongCatalogStore(private val storage: FileStore = createFileStore()) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    private val _records = MutableStateFlow<Map<String, CatalogRecord>>(emptyMap())
    val records: StateFlow<Map<String, CatalogRecord>> = _records.asStateFlow()

    private var loaded = false

    fun load(): Map<String, CatalogRecord> {
        if (loaded) return _records.value
        val text = storage.read(SONG_CATALOG_FILE)
        val stored = if (text.isNullOrBlank()) {
            StoredCatalog()
        } else {
            runCatching { json.decodeFromString(StoredCatalog.serializer(), text) }
                .onFailure { Logger.e(TAG, "song-catalog.json unreadable, starting empty: ${it.message}") }
                .getOrDefault(StoredCatalog())
        }
        _records.value = stored.records.mapValues { (_, record) -> clean(record) }
        loaded = true
        return _records.value
    }

    /** The whole library as a desktop just answered it over the LAN; replaces everything held. */
    fun replaceAll(books: List<CatalogRecord>) {
        load()
        commit(books.take(RECORDS_MAX).associateBy { recordId(it) }.mapValues { (_, r) -> clean(r) })
    }

    /** What a relay pull brought: records by id, and the ids of records that are gone. */
    fun merge(put: Map<String, CatalogRecord>, removed: Set<String>) {
        load()
        if (put.isEmpty() && removed.isEmpty()) return
        val next = _records.value.toMutableMap()
        removed.forEach(next::remove)
        put.forEach { (id, record) -> next[id] = clean(record) }
        commit(next.entries.take(RECORDS_MAX).associate { it.key to it.value })
    }

    /** Every song the catalog holds, by songbook then number, as the picker lists songs. */
    fun songs(): List<Song> {
        load()
        val seen = HashSet<String>()
        return _records.value.values
            .sortedWith(compareBy({ it.songbook }, { it.part }))
            .flatMap { record ->
                record.songs.mapNotNull { song ->
                    val key = "cat:${record.songbook}:${song.n}:${song.t}"
                    if (!seen.add(key)) return@mapNotNull null
                    Song(
                        number = song.n,
                        title = song.t,
                        bookName = record.songbook,
                        localId = key,
                        secondaryTitle = song.t2,
                    )
                }
            }
    }

    /** The measured lengths the catalog carries. */
    fun durations(): SongDurations {
        load()
        val measured = _records.value.values.flatMap { record ->
            record.songs.mapNotNull { song ->
                song.s?.let { Song(number = song.n, title = song.t, bookName = record.songbook) to it }
            }
        }
        return SongDurations.of(measured)
    }

    val isEmpty: Boolean
        get() = load().isEmpty()

    /** The desktop's id for a book -- an ASCII slug plus a hash of the real name -- built the same way here. */
    private fun recordId(record: CatalogRecord): String {
        val slug = record.songbook.map { if (isSlugChar(it)) it else '_' }.joinToString("").trim('_').take(SLUG_CHARS)
        val key = (if (slug.isEmpty()) "" else "$slug-") + fnv1a(record.songbook)
        return if (record.part == 0) "$CATALOG_PREFIX$key" else "$CATALOG_PREFIX$key:${record.part}"
    }

    private fun clean(record: CatalogRecord): CatalogRecord = CatalogRecord(
        songbook = Sanitize.text(record.songbook, SONGBOOK_CHARS),
        part = record.part.coerceAtLeast(0),
        songs = record.songs.take(SONGS_PER_RECORD).map { song ->
            CatalogSong(
                n = Sanitize.text(song.n, NUMBER_CHARS),
                t = Sanitize.text(song.t, TITLE_CHARS),
                s = song.s?.coerceIn(0, MAX_SECONDS),
                t2 = song.t2?.let { Sanitize.text(it, TITLE_CHARS) }?.takeIf { it.isNotEmpty() },
            )
        }.filter { it.t.isNotEmpty() },
    )

    private fun commit(next: Map<String, CatalogRecord>) {
        _records.value = next
        val text = json.encodeToString(StoredCatalog.serializer(), StoredCatalog(next))
        runCatching { storage.write(SONG_CATALOG_FILE, text) }
            .onFailure { Logger.e(TAG, "song-catalog.json write failed: ${it.message}") }
    }
}

private fun isSlugChar(c: Char): Boolean = c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c in "_-."

/** FNV-1a over the UTF-8 bytes as eight hex characters; the desktop computes the same. */
private fun fnv1a(text: String): String {
    var hash = FNV_OFFSET
    for (byte in text.encodeToByteArray()) hash = ((hash xor (byte.toLong() and BYTE_MASK)) * FNV_PRIME) and UINT_MASK
    return hash.toString(HEX).padStart(HEX_CHARS, '0')
}

private const val SLUG_CHARS = 32
private const val FNV_OFFSET = 0x811c9dc5L
private const val FNV_PRIME = 0x01000193L
private const val BYTE_MASK = 0xffL
private const val UINT_MASK = 0xffffffffL
private const val HEX = 16
private const val HEX_CHARS = 8
