package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.CanvasShape
import com.church.presenter.churchpresentermobile.model.HighlightColor
import com.church.presenter.churchpresentermobile.model.HighlightPalette
import com.church.presenter.churchpresentermobile.model.ShapeFill
import com.church.presenter.churchpresentermobile.model.inkPalette
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
            val row = inkPalette(board.isDark)
            assertEquals(PALETTE_SIZE, row.map { board.inkSwatch(it) }.toSet().size)
            assertEquals(PALETTE_SIZE, row.map { board.inkColor(it) }.toSet().size)
        }
    }

    @Test
    fun `the bright inks drop a shade on the white board so a thin line still reads`() {
        listOf(CanvasInk.GRAY, CanvasInk.ORANGE, CanvasInk.YELLOW, CanvasInk.BLUE, CanvasInk.PURPLE).forEach { ink ->
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
            assertEquals(PALETTE_SIZE, inkPalette(board.isDark).map { it.label(board.isDark) }.toSet().size)
        }
        assertEquals(PALETTE_SIZE, HighlightPalette.map { it.label() }.toSet().size)
        assertEquals(CanvasInk.CONTRAST.label(isDark = true), CanvasInk.INVERSE.label(isDark = false))
        // Only black and white swap names between the boards; every color is called the same on both.
        CanvasInk.entries.forEach { ink ->
            val swaps = ink == CanvasInk.CONTRAST || ink == CanvasInk.INVERSE
            assertEquals(!swaps, ink.label(isDark = true) == ink.label(isDark = false), "$ink")
        }
        assertEquals(HighlightColor.entries.size, HighlightColor.entries.map { it.label() }.toSet().size)
    }

    @Test
    fun `the row's swatches are the design's colors, white first on either board`() {
        val design = listOf(
            0xFFFAFAFA, 0xFF18181B, 0xFF9CA3AF, 0xFFEF4444, 0xFFF97316,
            0xFFFACC15, 0xFF86EFAC, 0xFF38BDF8, 0xFF3B82F6, 0xFFA855F7,
        ).map { Color(it) }
        boards.forEach { board ->
            assertEquals(design, inkPalette(board.isDark).map { board.inkSwatch(it) }, "board dark=${board.isDark}")
        }
    }

    @Test
    fun `the fill row is the design's ten, no fill first`() {
        assertEquals(10, ShapeFill.entries.size)
        assertEquals(ShapeFill.NONE, ShapeFill.entries.first())
        assertEquals(ShapeFill.entries.size - 1, ShapeFill.entries.drop(1).map(::shapeFillColor).toSet().size)
        assertEquals(ShapeFill.entries.size, ShapeFill.entries.map { it.label() }.toSet().size)
    }

    @Test
    fun `every shape's outline fills its box`() {
        val size = Size(200f, 100f)
        CanvasShape.entries.forEach { shape ->
            val outline = canvasShape(shape).createOutline(size, LayoutDirection.Ltr, Density(1f))
            val bounds = outline.bounds
            assertTrue(bounds.width > 0f && bounds.height > 0f, "$shape has an outline")
            val inside = bounds.right <= size.width + 0.5f && bounds.bottom <= size.height + 0.5f
            assertTrue(inside, "$shape stays in its box")
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
