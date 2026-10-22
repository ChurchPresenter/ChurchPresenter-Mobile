package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.ids
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.shape
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Edits to a page's list of elements: the stacking order, positions and groups. */
class CanvasArrangeTest {

    private val a = text("a")
    private val b = text("b")
    private val c = text("c")
    private val d = text("d")
    private val page = listOf(a, b, c, d)

    private fun List<CanvasElement>.ids() = map { it.id }

    @Test
    fun `without removes exactly the named elements`() {
        assertEquals(listOf("a", "d"), page.without(listOf("b", "c")).ids())
        assertEquals(page, page.without(listOf("missing")))
    }

    @Test
    fun `bringing to front moves the named elements to the top keeping their order`() {
        assertEquals(listOf("b", "d", "a", "c"), page.broughtToFront(listOf("c", "a")).ids())
    }

    @Test
    fun `sending to back moves the named elements to the bottom keeping their order`() {
        assertEquals(listOf("a", "c", "b", "d"), page.sentToBack(listOf("c", "a")).ids())
    }

    @Test
    fun `updated transforms only the named elements`() {
        val moved = page.updated(listOf("b")) { it.moved(5f, 6f) }
        assertEquals(a, moved[0])
        val movedB = moved[1] as CanvasElement.Text
        assertEquals(5f, movedB.x)
        assertEquals(6f, movedB.y)
    }

    @Test
    fun `a duplicate lands just above its original, nudged, under a new id`() {
        val (list, copies) = page.withDuplicates(listOf("b"), ids())
        assertEquals(listOf("a", "b", "new-1", "c", "d"), list.ids())
        assertEquals(listOf("new-1"), copies)
        val copy = list[2] as CanvasElement.Text
        assertEquals(DUPLICATE_OFFSET, copy.x)
        assertEquals(DUPLICATE_OFFSET, copy.y)
        assertEquals("Hello", copy.text)
    }

    @Test
    fun `duplicating a group makes a new group of the copies`() {
        val grouped = listOf(text("a", group = "g"), text("b", group = "g"), text("c"))
        val (list, copies) = grouped.withDuplicates(listOf("a", "b"), ids())
        val copied = list.filter { it.id in copies }
        assertEquals(2, copied.size)
        val newGroup = copied.first().group
        assertTrue(newGroup != null && newGroup != "g", "copies must not join the original group")
        assertTrue(copied.all { it.group == newGroup }, "copies of one group share one new group")
        assertNull(list.last().group)
    }

    @Test
    fun `align lines up left edges with the left-most`() {
        val bounds = mapOf(
            "a" to CanvasBounds(40f, 0f, 60f, 10f),
            "b" to CanvasBounds(10f, 20f, 30f, 30f),
        )
        val aligned = listOf(text("a", x = 40f), text("b", x = 10f)).aligned(listOf("a", "b"), bounds)
        assertEquals(10f, (aligned[0] as CanvasElement.Text).x)
        assertEquals(10f, (aligned[1] as CanvasElement.Text).x)
    }

    @Test
    fun `align with nothing measured leaves the page alone`() {
        assertSame(page, page.aligned(listOf("a"), emptyMap()))
    }

    @Test
    fun `distribute spaces three or more evenly, keeping the outer two`() {
        val elements = listOf(
            shape("a", x = 0f, width = 10f),
            shape("b", x = 15f, width = 10f),
            shape("c", x = 90f, width = 10f),
        )
        val bounds = elements.associate { it.id to CanvasBounds(it.x, 0f, it.x + it.width, 10f) }
        val spread = elements.distributed(elements.map { it.id }, bounds).map { (it as CanvasElement.Shape).x }
        assertEquals(listOf(0f, 45f, 90f), spread)
    }

    @Test
    fun `distribute needs three`() {
        val two = listOf(shape("a"), shape("b", x = 50f))
        val bounds = two.associate { it.id to CanvasBounds(it.x, 0f, it.x + 10f, 10f) }
        assertSame(two, two.distributed(two.map { it.id }, bounds))
    }

    @Test
    fun `grouping sets and clears the group`() {
        val grouped = page.grouped(listOf("a", "c"), "g")
        assertEquals(listOf("g", null, "g", null), grouped.map { it.group })
        assertTrue(grouped.grouped(listOf("a", "c"), null).all { it.group == null })
    }

    @Test
    fun `moving in the stack steps and stops at the ends`() {
        assertEquals(listOf("a", "c", "b", "d"), page.movedInStack("b", 1).ids())
        assertEquals(listOf("b", "a", "c", "d"), page.movedInStack("b", -5).ids())
        assertEquals(listOf("a", "b", "c", "d"), page.movedInStack("d", 3).ids())
        assertSame(page, page.movedInStack("missing", 1))
    }

    @Test
    fun `groupmates come along with any member`() {
        val grouped = listOf(text("a", group = "g"), text("b"), text("c", group = "g"))
        assertEquals(listOf("a", "c"), grouped.withGroupmates(listOf("c")))
        assertEquals(listOf("b"), grouped.withGroupmates(listOf("b")))
    }
}
