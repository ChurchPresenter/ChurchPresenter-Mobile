package com.church.presenter.churchpresentermobile

import androidx.compose.ui.unit.LayoutDirection
import com.church.presenter.churchpresentermobile.ui.ScrollThumb
import com.church.presenter.churchpresentermobile.ui.scrollThumb
import com.church.presenter.churchpresentermobile.ui.thumbX
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Which edge the scroll thumb lands on.
 *
 * It was pinned to `size.width - width`, which is the right edge always. In a
 * right-to-left locale the content's own edge is the left one, so the thumb sat
 * *over* the Arabic text rather than beside it.
 */
class ScrollbarThumbTest {

    @Test
    fun `left to right puts the thumb on the right edge`() {
        assertEquals(396f, thumbX(LayoutDirection.Ltr, totalWidth = 400f, thumbWidth = 4f))
    }

    @Test
    fun `right to left puts it on the left edge`() {
        assertEquals(0f, thumbX(LayoutDirection.Rtl, totalWidth = 400f, thumbWidth = 4f))
    }

    @Test
    fun `the thumb is always fully on screen`() {
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            val x = thumbX(direction, totalWidth = 400f, thumbWidth = 4f)
            assertEquals(true, x >= 0f && x + 4f <= 400f, "$direction put the thumb at $x")
        }
    }

    // ── Thumb length and travel, for a plain scrolling column ────────────
    //
    // Every dialog body that can run past the screen draws this thumb, and it
    // is the operator's only sign that there is more below the fold.

    @Test
    fun `content that fits draws no thumb`() {
        assertNull(scrollThumb(viewportHeight = 600f, maxScroll = 0f, scrollOffset = 0f, minThumbHeight = 32f))
    }

    @Test
    fun `a viewport not yet laid out draws no thumb`() {
        assertNull(scrollThumb(viewportHeight = 0f, maxScroll = 300f, scrollOffset = 0f, minThumbHeight = 32f))
    }

    @Test
    fun `the thumb is the viewport's share of the content`() {
        // 600 visible of 1200 total: half the content, so half the track.
        val thumb = scrollThumb(viewportHeight = 600f, maxScroll = 600f, scrollOffset = 0f, minThumbHeight = 32f)
        assertEquals(ScrollThumb(top = 0f, height = 300f), thumb)
    }

    @Test
    fun `scrolled to the end the thumb sits at the bottom`() {
        val thumb = scrollThumb(viewportHeight = 600f, maxScroll = 600f, scrollOffset = 600f, minThumbHeight = 32f)
        assertEquals(ScrollThumb(top = 300f, height = 300f), thumb)
    }

    @Test
    fun `halfway down the thumb is halfway along its travel`() {
        val thumb = scrollThumb(viewportHeight = 600f, maxScroll = 600f, scrollOffset = 300f, minThumbHeight = 32f)
        assertEquals(150f, thumb?.top)
    }

    @Test
    fun `a very long body still gets a thumb you can see`() {
        // 600 of 60,600 would be a 6px sliver; the floor keeps it at 32.
        val thumb = scrollThumb(
            viewportHeight = 600f,
            maxScroll = 60_000f,
            scrollOffset = 60_000f,
            minThumbHeight = 32f,
        )
        assertEquals(ScrollThumb(top = 568f, height = 32f), thumb)
    }

    @Test
    fun `the floor never makes the thumb taller than the viewport`() {
        val thumb = scrollThumb(viewportHeight = 20f, maxScroll = 100f, scrollOffset = 100f, minThumbHeight = 32f)
        assertEquals(ScrollThumb(top = 0f, height = 20f), thumb)
    }

    @Test
    fun `an overscrolled offset is clamped to the track`() {
        val over = scrollThumb(viewportHeight = 600f, maxScroll = 600f, scrollOffset = 900f, minThumbHeight = 32f)
        val under = scrollThumb(viewportHeight = 600f, maxScroll = 600f, scrollOffset = -50f, minThumbHeight = 32f)
        assertEquals(300f, over?.top)
        assertEquals(0f, under?.top)
    }
}
