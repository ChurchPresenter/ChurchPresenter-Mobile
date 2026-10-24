package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The canvas's picture store: keeping, reading back, refusing and pruning pictures. */
class CanvasPicturesTest {

    private val bytes = byteArrayOf(1, 2, 3, 4, 5)
    private var next = 0

    private fun pictures(
        storage: InMemoryFileStorage = InMemoryFileStorage(),
        measure: (ByteArray) -> Pair<Int, Int>? = { 800 to 600 },
    ) = CanvasPictures(storage = storage, newKey = { "k${++next}" }, prepare = { it }, measure = measure)

    @Test
    fun aPictureIsKeptWithItsSizeAndReadBackAfterARestart() {
        val storage = InMemoryFileStorage()
        val picture = assertNotNull(pictures(storage).put(bytes))

        assertEquals(800, picture.width)
        assertEquals(600, picture.height)
        assertTrue(picture.key.startsWith(CANVAS_PICTURE_PREFIX))
        assertContentEquals(bytes, pictures(storage).get(picture.key), "read from the disk, not the cache")
    }

    @Test
    fun somethingThatIsNotAPictureIsNotKept() {
        val storage = InMemoryFileStorage()
        assertNull(pictures(storage, measure = { null }).put(bytes))
        assertTrue(storage.list().isEmpty())
    }

    @Test
    fun picturesNoLongerPlacedArePrunedAndTheRestOfTheStoreIsLeftAlone() {
        val storage = InMemoryFileStorage(mapOf(CANVAS_FILE to "{}"))
        val store = pictures(storage)
        val kept = assertNotNull(store.put(bytes))
        val dropped = assertNotNull(store.put(bytes))

        store.prune(setOf(kept.key))

        assertNotNull(store.get(kept.key))
        assertNull(store.get(dropped.key))
        assertTrue(CANVAS_FILE in storage.list(), "the canvas itself is not a picture")
    }

    @Test
    fun anUnknownKeyHasNoPicture() {
        assertNull(pictures().get("${CANVAS_PICTURE_PREFIX}missing"))
    }
}
