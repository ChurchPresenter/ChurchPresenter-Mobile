package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.CanvasShape
import com.church.presenter.churchpresentermobile.model.HighlightColor
import com.church.presenter.churchpresentermobile.model.HighlightPalette
import com.church.presenter.churchpresentermobile.model.InkPalette
import com.church.presenter.churchpresentermobile.model.PALETTE_SIZE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** The colors each ink and highlighter color stands for, what they are called, and each shape's outline. */
class CanvasPaletteColorsTest {

    private val boards = listOf(DarkCanvasColors, LightCanvasColors)

    @Test
    fun `the ten palette inks are ten different swatches on either board`() {
        boards.forEach { board ->
            assertEquals(PALETTE_SIZE, InkPalette.map { board.inkSwatch(it) }.toSet().size)
            assertEquals(PALETTE_SIZE, InkPalette.map { board.inkColor(it) }.toSet().size)
        }
    }

    @Test
    fun `the new inks drop a shade on the white board so a thin line still reads`() {
        listOf(CanvasInk.ORANGE, CanvasInk.TEAL, CanvasInk.BLUE, CanvasInk.PINK, CanvasInk.BROWN).forEach { ink ->
            assertNotEquals(DarkCanvasColors.inkColor(ink), LightCanvasColors.inkColor(ink), "$ink")
        }
    }

    @Test
    fun `contrast ink is the board's own text color and inverse is the other way round`() {
        boards.forEach { board ->
            assertEquals(board.text, board.inkColor(CanvasInk.CONTRAST))
            assertNotEquals(board.inkColor(CanvasInk.CONTRAST), board.inkColor(CanvasInk.INVERSE))
        }
    }

    @Test
    fun `a mixed color takes the place of the named ink`() {
        assertNotEquals(DarkCanvasColors.inkColor(CanvasInk.RED), DarkCanvasColors.inkColor(CanvasInk.RED, "#00FF00"))
        assertEquals(DarkCanvasColors.inkColor(CanvasInk.RED), DarkCanvasColors.inkColor(CanvasInk.RED, null))
    }

    @Test
    fun `the ten highlighter colors are ten different colors`() {
        assertEquals(PALETTE_SIZE, HighlightPalette.map(::highlightColor).toSet().size)
    }

    @Test
    fun `every ink and highlighter color is named, and no two in a row share a name`() {
        boards.forEach { board ->
            assertEquals(PALETTE_SIZE, InkPalette.map { it.label(board.isDark) }.toSet().size, "light blue is not blue")
        }
        assertEquals(PALETTE_SIZE, HighlightPalette.map { it.label() }.toSet().size)
        assertEquals(CanvasInk.CONTRAST.label(isDark = true), CanvasInk.INVERSE.label(isDark = false))
        assertEquals(CanvasInk.entries.size, CanvasInk.entries.count { true }, "every ink has a label")
        HighlightColor.entries.forEach { it.label() }
    }

    @Test
    fun `the dock leads with contrast ink and keeps white last on the light board`() {
        assertEquals(listOf(CanvasInk.CONTRAST, CanvasInk.INVERSE), DarkCanvasColors.dockLeadingInks())
        assertEquals(emptyList(), DarkCanvasColors.dockTrailingInks())
        assertEquals(listOf(CanvasInk.CONTRAST), LightCanvasColors.dockLeadingInks())
        assertEquals(listOf(CanvasInk.INVERSE), LightCanvasColors.dockTrailingInks())
    }

    @Test
    fun `every shape's outline fills its box`() {
        val size = Size(200f, 100f)
        CanvasShape.entries.forEach { shape ->
            val outline = canvasShape(shape).createOutline(size, LayoutDirection.Ltr, Density(1f))
            val bounds = outline.bounds
            assertTrue(bounds.width > 0f && bounds.height > 0f, "$shape has an outline")
            assertTrue(bounds.right <= size.width + 0.5f && bounds.bottom <= size.height + 0.5f, "$shape stays in its box")
        }
    }

    @Test
    fun `the speech bubble's tail reaches the bottom of its box`() {
        val size = Size(200f, 100f)
        val outline = canvasShape(CanvasShape.SPEECH).createOutline(size, LayoutDirection.Ltr, Density(1f))

        assertTrue(outline is Outline.Generic, "a drawn path, not a plain rounded box")
        assertEquals(size.height, outline.bounds.bottom, 0.5f)
        assertEquals(size.width, outline.bounds.right, 0.5f)
    }
}
