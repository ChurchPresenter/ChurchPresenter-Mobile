package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.shape
import com.church.presenter.churchpresentermobile.model.CanvasPage
import com.church.presenter.churchpresentermobile.model.CanvasToolOptions
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The canvas on the device's disk: reading, writing, and what an unreadable file does. */
class CanvasRepositoryTest {

    private val canvas = SavedCanvas(
        document = CanvasDocument(title = "Sermon", pages = listOf(CanvasPage(listOf(shape("s"))), CanvasPage())),
        pageIndex = 1,
        options = CanvasToolOptions().let { it.copy(pen = it.pen.copy(width = 7f)) },
    )

    @Test
    fun `nothing saved is no canvas`() {
        assertNull(CanvasRepository(InMemoryFileStorage()).load())
        assertNull(CanvasRepository(InMemoryFileStorage(mapOf(CANVAS_FILE to "  "))).load())
    }

    @Test
    fun `a saved canvas comes back the same`() {
        val files = InMemoryFileStorage()
        assertTrue(CanvasRepository(files).save(canvas))
        assertEquals(canvas, CanvasRepository(files).load())
    }

    @Test
    fun `an unreadable file is no canvas, and is kept aside rather than overwritten`() {
        val files = InMemoryFileStorage(mapOf(CANVAS_FILE to "{ not json"))
        val repository = CanvasRepository(files)
        assertNull(repository.load())
        assertTrue(repository.save(canvas))
        assertEquals("{ not json", files.read("canvas.unreadable.json"))
        assertEquals(canvas, CanvasRepository(files).load())
    }

    @Test
    fun `a saved canvas is put right if it could not be shown`() {
        val files = InMemoryFileStorage()
        CanvasRepository(files).save(SavedCanvas(document = CanvasDocument(pages = emptyList()), pageIndex = 5))
        val loaded = CanvasRepository(files).load()!!
        assertEquals(1, loaded.document.pages.size, "at least one page")
        assertEquals(0, loaded.pageIndex, "and an open page that exists")
    }

    @Test
    fun `a write that fails says so`() {
        val failing = object : FileStore by InMemoryFileStorage() {
            override fun write(name: String, text: String) = error("disk full")
        }
        assertFalse(CanvasRepository(failing).save(canvas))
    }

    @Test
    fun `fields an older file lacks take their defaults`() {
        val files = InMemoryFileStorage(mapOf(CANVAS_FILE to """{"document":{"pages":[{"elements":[]}]}}"""))
        val loaded = CanvasRepository(files).load()!!
        assertEquals(CANVAS_FILE_VERSION, loaded.version)
        assertEquals(CanvasToolOptions(), loaded.options)
    }
}
