package com.church.presenter.churchpresentermobile

import androidx.compose.ui.unit.LayoutDirection
import com.church.presenter.churchpresentermobile.ui.thumbX
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
