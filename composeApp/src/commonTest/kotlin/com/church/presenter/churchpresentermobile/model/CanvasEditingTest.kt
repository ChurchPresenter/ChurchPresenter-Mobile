package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.ids
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.image
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.ink
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.shape
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.sticky
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.text
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

/** Document-level edits, stroke data, smoothing, zoom steps and the saved form. */
class CanvasEditingTest {

    private val doc = CanvasDocument(
        title = "Sermon",
        pages = listOf(CanvasPage(listOf(text("a"))), CanvasPage(listOf(text("b")))),
    )

    @Test
    fun `editing a page leaves the others alone`() {
        val edited = doc.editPage(1) { it + text("c") }
        assertEquals(doc.pages[0], edited.pages[0])
        assertEquals(listOf("b", "c"), edited.pages[1].elements.map { it.id })
    }

    @Test
    fun `a new page goes on the end, blank`() {
        val added = doc.withNewPage()
        assertEquals(3, added.pages.size)
        assertEquals(CanvasPage(), added.pages.last())
    }

    @Test
    fun `a duplicated page follows its original, with fresh ids`() {
        val duplicated = doc.withPageDuplicated(0, ids("copy"))
        assertEquals(3, duplicated.pages.size)
        assertEquals(listOf("copy-1"), duplicated.pages[1].elements.map { it.id })
        assertEquals("Hello", (duplicated.pages[1].elements.single() as CanvasElement.Text).text)
        assertSame(doc, doc.withPageDuplicated(5, ids()))
    }

    @Test
    fun `deleting a page keeps at least one`() {
        assertEquals(listOf("b"), doc.withoutPage(0).pages.single().elements.map { it.id })
        val single = CanvasDocument()
        assertSame(single, single.withoutPage(0))
        assertSame(doc, doc.withoutPage(9))
    }

    @Test
    fun `stroke data moves to the first point and draws to the rest`() {
        val path = strokePathData(listOf(CanvasPoint(0f, 0f), CanvasPoint(10.26f, -3.5f), CanvasPoint(-0.4f, 2f)))
        assertEquals("M0 0 L10.3 -3.5 L-0.4 2", path)
        assertEquals("", strokePathData(emptyList()))
    }

    @Test
    fun `a single point is a dot - a zero-length segment`() {
        assertEquals("M5 5 L5 5", strokePathData(listOf(CanvasPoint(5f, 5f))))
    }

    @Test
    fun `coordinates are written to a tenth`() {
        assertEquals("10", 10f.svgNumber())
        assertEquals("1.3", 1.26f.svgNumber())
        assertEquals("-1.5", (-1.5f).svgNumber())
        assertEquals("-0.4", (-0.4f).svgNumber())
    }

    @Test
    fun `smoothing evens out a zigzag but keeps both ends`() {
        val zigzag = (0..8).map { CanvasPoint(it * 10f, if (it % 2 == 0) 0f else 10f) }
        val smooth = smoothed(zigzag, 1f)
        assertEquals(zigzag.first(), smooth.first())
        assertEquals(zigzag.last(), smooth.last())
        assertNotEquals(zigzag[4], smooth[4])
        assertSame(zigzag, smoothed(zigzag, 0f))
        val short = zigzag.take(2)
        assertSame(short, smoothed(short, 1f))
    }

    @Test
    fun `zoom steps through the ladder and stops at either end`() {
        assertEquals(125, zoomedIn(100))
        assertEquals(100, zoomedIn(90))
        assertEquals(400, zoomedIn(400))
        assertEquals(75, zoomedOut(100))
        assertEquals(100, zoomedOut(110))
        assertEquals(25, zoomedOut(25))
    }

    @Test
    fun `a document with every kind of element survives the saved form`() {
        val every = CanvasDocument(
            title = "All",
            pages = listOf(
                CanvasPage(
                    listOf(
                        text("t").copy(format = TextOptions(bold = false, font = CanvasFont.MONO)),
                        ink("k", kind = InkKind.HIGHLIGHTER).copy(highlight = HighlightColor.PINK, dashed = true),
                        image("i").copy(source = ImageSource.CAMERA, rotation = 90, cutOut = true),
                        shape("s", group = "g").copy(fill = ShapeFill.YELLOW, border = BorderStyle.NONE),
                        sticky("n").copy(color = StickyColor.PURPLE, font = StickyFont.PLAIN, rotation = -2f),
                    ),
                ),
                CanvasPage(),
            ),
        )
        val json = Json { encodeDefaults = true }
        val text = json.encodeToString(CanvasDocument.serializer(), every)
        assertEquals(every, json.decodeFromString(CanvasDocument.serializer(), text))
    }
}
