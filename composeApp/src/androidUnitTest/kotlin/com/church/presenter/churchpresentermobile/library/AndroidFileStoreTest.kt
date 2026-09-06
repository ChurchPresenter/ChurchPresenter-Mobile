package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.initSettingsContext
import com.church.presenter.churchpresentermobile.testutil.RecordingContext
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The on-device library store — where a church's own songs, Bibles and running
 * orders live when there is no desktop.
 *
 * This is the only copy of that data. A church types a hundred songs into the
 * Library tab and the bytes never leave the phone, so a write that truncates on
 * a kill, or a read that silently returns nothing, loses work that cannot be
 * recovered from anywhere. The store is small enough to test exhaustively and
 * important enough to be worth it.
 *
 * Runs against a real temporary directory: only `Context.filesDir` is faked, so
 * every file operation below is the real one.
 */
class AndroidFileStoreTest {

    private lateinit var filesDir: File

    @BeforeTest
    fun useATempDirectory() {
        filesDir = Files.createTempDirectory("filestore").toFile()
        initSettingsContext(RecordingContext(files = filesDir))
    }

    @AfterTest
    fun cleanUp() {
        filesDir.deleteRecursively()
    }

    private val store: FileStore get() = AndroidFileStore()

    /** Where the store puts things, for asserting on the layout rather than through the API. */
    private val library: File get() = File(filesDir, "library")

    // ── Round trips ──────────────────────────────────────────────────────

    @Test
    fun `what is written is what is read back`() {
        store.write("songs.json", """{"songs":[]}""")

        assertEquals("""{"songs":[]}""", store.read("songs.json"))
    }

    @Test
    fun `a file that was never written reads as nothing`() {
        // Distinct from an empty file — the caller starts a fresh library on null.
        assertNull(store.read("songs.json"))
    }

    @Test
    fun `an empty file is not the same as a missing one`() {
        store.write("songs.json", "")

        assertEquals("", store.read("songs.json"))
    }

    @Test
    fun `writing twice keeps the second version`() {
        store.write("songs.json", "first")
        store.write("songs.json", "second")

        assertEquals("second", store.read("songs.json"))
    }

    @Test
    fun `text outside ASCII survives the round trip`() {
        // Russian and Ukrainian songbooks are the common case, not an edge one.
        val cyrillic = "Велика Ти, Боже мій — em dash — and a curly ’"

        store.write("songs.json", cyrillic)

        assertEquals(cyrillic, store.read("songs.json"))
    }

    @Test
    fun `a large document survives the round trip`() {
        // A downloaded Bible module is several megabytes.
        val big = "verse text ".repeat(200_000)

        store.write("bible.spb", big)

        assertEquals(big, store.read("bible.spb"))
    }

    // ── Where it puts things ─────────────────────────────────────────────

    @Test
    fun `everything lands in the library folder, not loose in the app's files`() {
        // Shared with SharedPreferences and other app data; loose files here
        // would be indistinguishable from someone else's.
        store.write("songs.json", "x")

        assertTrue(File(library, "songs.json").exists())
    }

    @Test
    fun `the library folder is created on first use`() {
        assertTrue(!library.exists() || library.listFiles().orEmpty().isEmpty())

        store.write("songs.json", "x")

        assertTrue(library.isDirectory)
    }

    @Test
    fun `no temporary file is left behind after a write`() {
        // The write goes to a .tmp and is renamed; one left over would show up in
        // list() as a phantom document.
        store.write("songs.json", "x")

        assertTrue(library.listFiles().orEmpty().none { it.name.endsWith(".tmp") })
    }

    @Test
    fun `an interrupted write leaves the previous version intact`() {
        // The reason for write-then-rename: the bytes only become the real file
        // once they are all on disk.
        store.write("songs.json", "good")
        File(library, "songs.json.tmp").writeText("half writ")

        assertEquals("good", store.read("songs.json"))
    }

    // ── Listing ──────────────────────────────────────────────────────────

    @Test
    fun `listing names what has been written`() {
        store.write("songs.json", "a")
        store.write("order.json", "b")

        assertEquals(setOf("songs.json", "order.json"), store.list().toSet())
    }

    @Test
    fun `listing an empty library gives nothing rather than failing`() {
        assertEquals(emptyList(), store.list())
    }

    @Test
    fun `a folder inside the library is not listed as a document`() {
        store.write("songs.json", "a")
        File(library, "nested").mkdirs()

        assertEquals(listOf("songs.json"), store.list())
    }

    @Test
    fun `a deleted file stops being listed`() {
        store.write("songs.json", "a")
        store.write("order.json", "b")

        store.delete("songs.json")

        assertEquals(listOf("order.json"), store.list())
    }

    // ── Deleting ─────────────────────────────────────────────────────────

    @Test
    fun `a deleted file reads as nothing`() {
        store.write("songs.json", "a")

        store.delete("songs.json")

        assertNull(store.read("songs.json"))
    }

    @Test
    fun `deleting a file that is not there is harmless`() {
        store.delete("never-existed.json")

        assertEquals(emptyList(), store.list())
    }

    // ── Sizes ────────────────────────────────────────────────────────────

    @Test
    fun `the size reported is the size on disk`() {
        // Shown to the operator on the sync screen, in bytes.
        store.write("songs.json", "12345")

        assertEquals(5L, store.sizeBytes("songs.json"))
    }

    @Test
    fun `a missing file has no size rather than an error`() {
        assertEquals(0L, store.sizeBytes("never-existed.json"))
    }

    @Test
    fun `an empty file has no size`() {
        store.write("songs.json", "")

        assertEquals(0L, store.sizeBytes("songs.json"))
    }

    @Test
    fun `a multi-byte character counts as the bytes it takes`() {
        // The sync screen would otherwise under-report a Cyrillic songbook by half.
        store.write("songs.json", "Ж")

        assertEquals(2L, store.sizeBytes("songs.json"))
    }

    // ── Before the app has a files directory ─────────────────────────────

    @Test
    fun `with nowhere to write, the store stays quiet rather than throwing`() {
        // Reachable if anything touches the library before the Application has
        // finished starting; losing the write is survivable, a crash is not.
        initSettingsContext(RecordingContext(files = null))

        store.write("songs.json", "x")

        assertNull(store.read("songs.json"))
        assertEquals(emptyList(), store.list())
        assertEquals(0L, store.sizeBytes("songs.json"))
    }

    @Test
    fun `deleting with nowhere to write is harmless`() {
        initSettingsContext(RecordingContext(files = null))

        store.delete("songs.json")
    }
}
