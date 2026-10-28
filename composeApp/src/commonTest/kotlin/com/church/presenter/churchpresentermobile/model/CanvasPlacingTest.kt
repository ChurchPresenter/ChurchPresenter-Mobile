package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.image
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What each placing tool puts on the page, built from its options. */
class CanvasPlacingTest {

    private val start = CanvasPoint(10f, 20f)
    private val end = CanvasPoint(110f, 20f)

    @Test
    fun `a shape fills the bounds with the tool's options`() {
        val options = ShapeOptions(
            shape = CanvasShape.STAR,
            fill = ShapeFill.SKY,
            border = BorderStyle.DASHED,
            borderWidth = 6f,
        )
        val shape = shapeElement("s", CanvasBounds(1f, 2f, 51f, 32f), options)
        assertEquals(1f, shape.x)
        assertEquals(2f, shape.y)
        assertEquals(50f, shape.width)
        assertEquals(30f, shape.height)
        assertEquals(CanvasShape.STAR, shape.shape)
        assertEquals(ShapeFill.SKY, shape.fill)
        assertEquals(BorderStyle.DASHED, shape.border)
        assertEquals(6f, shape.borderWidth)
        assertEquals(CanvasInk.SKY, shape.ink)
    }

    @Test
    fun `each fill picks an outline of its own color family`() {
        assertEquals(CanvasInk.CONTRAST, outlineFor(ShapeFill.NONE))
        assertEquals(CanvasInk.GREEN, outlineFor(ShapeFill.GREEN))
        assertEquals(CanvasInk.SKY, outlineFor(ShapeFill.SKY))
        assertEquals(CanvasInk.AMBER, outlineFor(ShapeFill.YELLOW))
        assertEquals(CanvasInk.RED, outlineFor(ShapeFill.PINK))
    }

    @Test
    fun `a custom fill colors the outline too`() {
        val shape = shapeElement("s", CanvasBounds(0f, 0f, 10f, 10f), ShapeOptions(fillCustom = "#123456"))
        assertEquals("#123456", shape.fillCustom)
        assertEquals("#123456", shape.custom)
    }

    @Test
    fun `a straight line with no caps is one segment`() {
        val options = LineOptions(style = LineStyle.STRAIGHT, start = LineEnd.NONE, end = LineEnd.NONE)
        assertEquals("M10 20 L110 20", linePathData(start, end, options))
    }

    @Test
    fun `a curve bows through a quadratic control point`() {
        val path = linePathData(start, end, LineOptions(style = LineStyle.CURVE, end = LineEnd.NONE))
        assertTrue(path.startsWith("M10 20 Q"), path)
        assertTrue(path.endsWith("110 20"), path)
    }

    @Test
    fun `an elbow runs across, down and across`() {
        val path = linePathData(start, CanvasPoint(110f, 80f), LineOptions(style = LineStyle.ELBOW, end = LineEnd.NONE))
        assertEquals("M10 20 H60 V80 H110", path)
    }

    @Test
    fun `an arrow cap adds two wings meeting at the tip`() {
        val path = linePathData(start, end, LineOptions(start = LineEnd.NONE, end = LineEnd.ARROW, width = 4f))
        val cap = path.removePrefix("M10 20 L110 20")
        assertTrue(cap.startsWith(" M"), path)
        assertTrue(cap.contains(" L110 20 L"), "the wings meet at the tip: $path")
    }

    @Test
    fun `a dot cap is a ring of two arcs round the end`() {
        val path = linePathData(start, end, LineOptions(start = LineEnd.DOT, end = LineEnd.NONE, width = 4f))
        assertEquals(2, Regex(" A").findAll(path).count(), path)
    }

    @Test
    fun `a line element carries the tool's ink, width and dashes`() {
        val options = LineOptions(ink = CanvasInk.VIOLET, width = 7f, dashed = true, custom = "#ABCDEF")
        val line = lineElement("l", start, end, options)
        assertEquals(InkKind.ARROW, line.kind)
        assertEquals(CanvasInk.VIOLET, line.ink)
        assertEquals(7f, line.width)
        assertTrue(line.dashed)
        assertEquals("#ABCDEF", line.custom)
    }

    @Test
    fun `a text box starts empty at the tap, in the tool's format`() {
        val format = TextOptions(size = 48, italic = true)
        val text = textElement("t", CanvasPoint(5f, 6f), format)
        assertEquals("", text.text)
        assertEquals(5f, text.x)
        assertEquals(6f, text.y)
        assertEquals(format, text.format)
    }

    @Test
    fun `a sticky note is centred on the tap and sized by the tool`() {
        val small = stickyElement("n", CanvasPoint(100f, 100f), StickyOptions(size = StickySize.SMALL, tilt = false))
        val large = stickyElement("n", CanvasPoint(100f, 100f), StickyOptions(size = StickySize.LARGE))
        assertTrue(small.width < large.width)
        assertEquals(100f, small.x + small.width / 2)
        assertEquals(0f, small.rotation)
        assertTrue(large.rotation != 0f, "tilted notes are turned")
        assertEquals("", small.text)
    }

    @Test
    fun `a sticky note keeps the tool's color and hand`() {
        val options = StickyOptions(color = StickyColor.PINK, font = StickyFont.NEAT)
        val note = stickyElement("n", CanvasPoint(0f, 0f), options)
        assertEquals(StickyColor.PINK, note.color)
        assertEquals(StickyFont.NEAT, note.font)
    }

    @Test
    fun `a picture is centred on the point, sized by its placement`() {
        val at = CanvasPoint(400f, 300f)
        val fit = imageElement("i", at, ImageSource.PHOTOS, ImageOptions(placement = ImagePlacement.FIT))
        val fill = imageElement("i", at, ImageSource.PHOTOS, ImageOptions(placement = ImagePlacement.FILL))
        val original = imageElement("i", at, ImageSource.PHOTOS, ImageOptions(placement = ImagePlacement.ORIGINAL))
        assertTrue(original.width < fit.width && fit.width < fill.width)
        assertEquals(400f, fit.x + fit.width / 2)
        assertEquals(300f, fit.y + fit.height / 2)
        assertEquals(ImageSource.PHOTOS, fit.source)
        assertEquals("", fit.fileName)
    }

    @Test
    fun `a picture takes the image tool's rounding, shadow and opacity`() {
        val options = ImageOptions(rounded = false, shadow = true, opacity = 0.5f)
        val image = imageElement("i", CanvasPoint(0f, 0f), ImageSource.FILES, options)
        assertFalse(image.rounded)
        assertTrue(image.shadow)
        assertEquals(0.5f, image.opacity)
    }

    @Test
    fun `crop trims to a centred square`() {
        val cropped = adjusted(image("i", x = 0f, y = 0f, width = 300f, height = 200f), ImageAdjust.CROP)
        assertEquals(200f, cropped.width)
        assertEquals(200f, cropped.height)
        assertEquals(50f, cropped.x)
        assertEquals(0f, cropped.y)
    }

    @Test
    fun `flip, rotate and remove background toggle and turn`() {
        val picture = image("i")
        assertTrue(adjusted(picture, ImageAdjust.FLIP).flipped)
        assertFalse(adjusted(adjusted(picture, ImageAdjust.FLIP), ImageAdjust.FLIP).flipped)
        assertEquals(90f, adjusted(picture, ImageAdjust.ROTATE).angle)
        assertEquals(0f, (1..4).fold(picture) { p, _ -> adjusted(p, ImageAdjust.ROTATE) }.angle)
        assertTrue(adjusted(picture, ImageAdjust.REMOVE_BACKGROUND).cutOut)
    }
}
