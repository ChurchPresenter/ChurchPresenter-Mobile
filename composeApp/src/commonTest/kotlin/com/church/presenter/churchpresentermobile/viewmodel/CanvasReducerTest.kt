package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.image
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.ink
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.shape
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.sticky
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.text
import com.church.presenter.churchpresentermobile.model.CanvasPage
import com.church.presenter.churchpresentermobile.model.CanvasPoint
import com.church.presenter.churchpresentermobile.model.StoredPicture
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.EraserMode
import com.church.presenter.churchpresentermobile.model.ImageAdjust
import com.church.presenter.churchpresentermobile.model.ImageSource
import com.church.presenter.churchpresentermobile.model.InkKind
import com.church.presenter.churchpresentermobile.model.PenType
import com.church.presenter.churchpresentermobile.model.ShapeFill
import com.church.presenter.churchpresentermobile.model.StickyColor
import com.church.presenter.churchpresentermobile.model.TextOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Every edit the canvas takes, as [reduce] applies it. The selection, editing and
 * tool-picking rules here are what the operator experiences as the canvas's
 * behaviour, so each branch is checked rather than assumed.
 */
class CanvasReducerTest {

    private var next = 0
    private val newId: () -> String = { "new-${++next}" }

    private fun state(vararg elements: CanvasElement, tool: CanvasTool = CanvasTool.SELECT) =
        CanvasUiState(document = CanvasDocument(pages = listOf(CanvasPage(elements.toList()))), tool = tool)

    private fun CanvasUiState.go(vararg intents: CanvasIntent): CanvasUiState =
        intents.fold(this) { s, intent -> s.reduce(intent, newId) }

    private fun CanvasUiState.ids() = page.elements.map { it.id }

    private fun CanvasUiState.element(id: String) = page.elements.first { it.id == id }

    private inline fun <reified T : CanvasElement> CanvasUiState.el(id: String): T = element(id) as T

    private val points = listOf(CanvasPoint(0f, 0f), CanvasPoint(10f, 5f), CanvasPoint(20f, 0f), CanvasPoint(30f, 5f))

    // ── Tools and panels ─────────────────────────────────────────────────

    @Test
    fun `picking a tool opens its options at once, and picking it again closes them`() {
        val picked = state().go(CanvasIntent.PickTool(CanvasTool.PEN))
        assertEquals(CanvasTool.PEN, picked.tool)
        assertTrue(picked.optionsOpen)
        assertFalse(picked.go(CanvasIntent.PickTool(CanvasTool.PEN)).optionsOpen)
        assertFalse(picked.go(CanvasIntent.CloseOptions).optionsOpen)
    }

    @Test
    fun `drawing tools drop the selection, the rest keep it`() {
        val selected = state(text("a")).go(CanvasIntent.Select("a"))
        assertEquals(listOf("a"), selected.go(CanvasIntent.PickTool(CanvasTool.TEXT)).selectedIds)
        assertEquals(listOf("a"), selected.go(CanvasIntent.PickTool(CanvasTool.LASSO)).selectedIds)
        assertTrue(selected.go(CanvasIntent.PickTool(CanvasTool.PEN)).selectedIds.isEmpty())
        assertTrue(selected.go(CanvasIntent.PickTool(CanvasTool.ERASER)).selectedIds.isEmpty())
    }

    @Test
    fun `the image tool brings its insert panel, and leaving it takes it away`() {
        val image = state().go(CanvasIntent.PickTool(CanvasTool.IMAGE))
        assertEquals(CanvasPanel.INSERT_IMAGE, image.panel)
        assertEquals(CanvasPanel.NONE, image.go(CanvasIntent.PickTool(CanvasTool.PEN)).panel)
        val layers = state().go(CanvasIntent.ShowPanel(CanvasPanel.LAYERS), CanvasIntent.PickTool(CanvasTool.PEN))
        assertEquals(CanvasPanel.LAYERS, layers.panel, "the layers panel stays open across tools")
    }

    @Test
    fun `picking a tool finishes typing and drops a box left empty`() {
        val typing = state(tool = CanvasTool.TEXT).go(CanvasIntent.PlaceText(CanvasPoint(0f, 0f)))
        assertEquals(1, typing.page.elements.size)
        val left = typing.go(CanvasIntent.PickTool(CanvasTool.SELECT))
        assertNull(left.editingId)
        assertTrue(left.page.elements.isEmpty())
    }

    @Test
    fun `zoom, panels and live are plain settings`() {
        val s = state().go(
            CanvasIntent.SetZoom(150),
            CanvasIntent.ShowPanel(CanvasPanel.LAYERS),
            CanvasIntent.SetLive(true),
        )
        assertEquals(150, s.zoom)
        assertEquals(CanvasPanel.LAYERS, s.panel)
        assertTrue(s.live)
        assertSame(s, s.go(CanvasIntent.Undo, CanvasIntent.Redo), "undo and redo are the ViewModel's")
    }

    @Test
    fun `renaming trims, and a blank name goes back to untitled`() {
        assertEquals("Sermon", state().go(CanvasIntent.Rename("  Sermon ")).document.title)
        assertNull(state().go(CanvasIntent.Rename("Sermon"), CanvasIntent.Rename("   ")).document.title)
    }

    // ── Pages ────────────────────────────────────────────────────────────

    @Test
    fun `pages are added, turned to, duplicated and deleted`() {
        val start = state(text("a")).go(CanvasIntent.Select("a"))
        val added = start.go(CanvasIntent.AddPage)
        assertEquals(2, added.document.pages.size)
        assertEquals(1, added.pageIndex)
        assertTrue(added.selectedIds.isEmpty())

        val back = added.go(CanvasIntent.TurnToPage(0))
        assertEquals(0, back.pageIndex)
        assertSame(back, back.go(CanvasIntent.TurnToPage(7)), "a page that does not exist is ignored")

        val duplicated = back.go(CanvasIntent.DuplicatePage)
        assertEquals(3, duplicated.document.pages.size)
        assertEquals(1, duplicated.pageIndex)
        assertEquals("Hello", (duplicated.page.elements.single() as CanvasElement.Text).text)

        val deleted = duplicated.go(CanvasIntent.TurnToPage(2), CanvasIntent.DeletePage)
        assertEquals(2, deleted.document.pages.size)
        assertEquals(1, deleted.pageIndex, "the open page slides back to one that exists")
    }

    @Test
    fun `clearing the page empties it, clearing the canvas starts again`() {
        val s = state(text("a"), text("b"))
            .go(CanvasIntent.Rename("Talk"), CanvasIntent.AddPage, CanvasIntent.TurnToPage(0))
        assertTrue(s.go(CanvasIntent.ClearPage).page.elements.isEmpty())
        val cleared = s.go(CanvasIntent.Select("a"), CanvasIntent.ClearCanvas)
        assertEquals(CanvasDocument(), cleared.document)
        assertEquals(0, cleared.pageIndex)
        assertTrue(cleared.selectedIds.isEmpty())
    }

    // ── Selection ────────────────────────────────────────────────────────

    @Test
    fun `selecting picks up the whole group`() {
        val s = state(text("a", group = "g"), text("b", group = "g"), text("c"))
        assertEquals(listOf("a", "b"), s.go(CanvasIntent.Select("b")).selectedIds)
        assertTrue(s.go(CanvasIntent.Select("a"), CanvasIntent.Select(null)).selectedIds.isEmpty())
    }

    @Test
    fun `a long-press adds to the selection, and again takes out`() {
        val s = state(text("a"), text("b", group = "g"), text("c", group = "g"))
        val both = s.go(CanvasIntent.Select("a"), CanvasIntent.ToggleSelect("b"))
        assertEquals(listOf("a", "b", "c"), both.selectedIds)
        assertEquals(listOf("a"), both.go(CanvasIntent.ToggleSelect("c")).selectedIds)
    }

    @Test
    fun `a lasso selects what it picked up, with groupmates`() {
        val s = state(text("a", group = "g"), text("b", group = "g"), text("c"))
        assertEquals(listOf("a", "b", "c"), s.go(CanvasIntent.SelectMany(listOf("a", "c"))).selectedIds)
    }

    // ── Strokes ──────────────────────────────────────────────────────────

    @Test
    fun `the pen draws with its ink and width`() {
        val s = state(tool = CanvasTool.PEN)
        val options = s.options.copy(pen = s.options.pen.copy(ink = CanvasInk.SKY, width = 6f, pressure = false))
        val drawn = s.go(CanvasIntent.SetOptions(options), CanvasIntent.Stroke(points))
        val ink = drawn.page.elements.single() as CanvasElement.Ink
        assertEquals(InkKind.PEN, ink.kind)
        assertEquals(CanvasInk.SKY, ink.ink)
        assertEquals(6f, ink.width)
        assertTrue(ink.pathData.startsWith("M0 0"))
    }

    @Test
    fun `pen types and pressure change the line`() {
        fun widthWith(type: PenType, pressure: Boolean, force: Float): Float {
            val s = state(tool = CanvasTool.PEN)
            val pen = s.options.pen.copy(type = type, width = 4f, pressure = pressure)
            val drawn = s.go(CanvasIntent.SetOptions(s.options.copy(pen = pen)), CanvasIntent.Stroke(points, force))
            return (drawn.page.elements.single() as CanvasElement.Ink).width
        }
        val pens = PenType.entries
        val ballpoint = widthWith(pens[0], pressure = false, force = 1f)
        assertEquals(4f, ballpoint)
        assertTrue(widthWith(PenType.BRUSH, false, 1f) > ballpoint)
        assertTrue(widthWith(PenType.FOUNTAIN, false, 1f) > ballpoint)
        assertEquals(6f, widthWith(pens[0], pressure = true, force = 3f), "pressure is capped at one and a half times")
        assertEquals(2f, widthWith(pens[0], pressure = true, force = 0.1f), "and floored at a half")
        assertEquals(4f, widthWith(pens[0], pressure = false, force = 3f), "without the setting, pressure does nothing")
    }

    @Test
    fun `a pencil draws fainter than a pen`() {
        val s = state(tool = CanvasTool.PEN)
        val pen = s.options.pen.copy(type = PenType.PENCIL, opacity = 1f)
        val drawn = s.go(CanvasIntent.SetOptions(s.options.copy(pen = pen)), CanvasIntent.Stroke(points))
        assertTrue((drawn.page.elements.single() as CanvasElement.Ink).opacity < 1f)
    }

    @Test
    fun `the highlighter goes behind ink, and can draw straight`() {
        val s = state(ink("written"), tool = CanvasTool.HIGHLIGHTER)
        val behind = s.go(CanvasIntent.Stroke(points))
        assertEquals("written", behind.ids().last(), "the highlight goes under the writing")
        val marker = behind.page.elements.first() as CanvasElement.Ink
        assertEquals(InkKind.HIGHLIGHTER, marker.kind)
        assertEquals(s.options.highlighter.color, marker.highlight)

        val options = s.options.copy(highlighter = s.options.highlighter.copy(behindInk = false, straightOnly = true))
        val straight = s.go(CanvasIntent.SetOptions(options), CanvasIntent.Stroke(points))
        val line = straight.page.elements.last() as CanvasElement.Ink
        assertEquals(InkKind.HIGHLIGHTER, line.kind, "drawn on top when not behind ink")
        assertEquals("M0 0 L30 5", line.pathData, "straight: first point to last")
    }

    @Test
    fun `the pixel eraser paints a stroke of its own, the other modes draw nothing`() {
        val pixel = state(tool = CanvasTool.ERASER)
        val options = pixel.options.copy(eraser = pixel.options.eraser.copy(mode = EraserMode.PIXEL, size = 30f))
        val erased = pixel.go(CanvasIntent.SetOptions(options), CanvasIntent.Stroke(points))
        val stroke = erased.page.elements.single() as CanvasElement.Ink
        assertEquals(InkKind.ERASER, stroke.kind)
        assertEquals(30f, stroke.width)

        assertTrue(state(tool = CanvasTool.ERASER).go(CanvasIntent.Stroke(points)).page.elements.isEmpty())
        assertTrue(state(tool = CanvasTool.SELECT).go(CanvasIntent.Stroke(points)).page.elements.isEmpty())
        assertTrue(state(tool = CanvasTool.PEN).go(CanvasIntent.Stroke(emptyList())).page.elements.isEmpty())
    }

    @Test
    fun `erasing takes the loose elements and leaves locked ones`() {
        val s = state(ink("a"), ink("b", locked = true), ink("c")).go(CanvasIntent.SelectMany(listOf("a", "c")))
        val erased = s.go(CanvasIntent.Erase(listOf("a", "b")))
        assertEquals(listOf("b", "c"), erased.ids())
        assertEquals(listOf("c"), erased.selectedIds)
    }

    // ── Placing ──────────────────────────────────────────────────────────

    @Test
    fun `placing selects the new element, and text and notes open for typing`() {
        val shaped = state().go(CanvasIntent.PlaceShape(CanvasBounds(0f, 0f, 50f, 50f)))
        assertTrue(shaped.page.elements.single() is CanvasElement.Shape)
        assertEquals(shaped.ids(), shaped.selectedIds)
        assertNull(shaped.editingId)

        val lined = state().go(CanvasIntent.PlaceLine(CanvasPoint(0f, 0f), CanvasPoint(9f, 9f)))
        assertEquals(InkKind.ARROW, (lined.page.elements.single() as CanvasElement.Ink).kind)

        val typed = state().go(CanvasIntent.PlaceText(CanvasPoint(1f, 2f)))
        assertEquals(typed.ids().single(), typed.editingId)
        val noted = state().go(CanvasIntent.PlaceSticky(CanvasPoint(1f, 2f)))
        assertTrue(noted.page.elements.single() is CanvasElement.Sticky)
        assertEquals(noted.ids().single(), noted.editingId)
    }

    @Test
    fun `typing edits the box, and finishing keeps it unless it is empty`() {
        val typed = state().go(CanvasIntent.PlaceText(CanvasPoint(0f, 0f)))
        val id = typed.editingId!!
        val written = typed.go(CanvasIntent.EditText(id, "Grace"), CanvasIntent.FinishEditing)
        assertEquals("Grace", written.el<CanvasElement.Text>(id).text)
        assertNull(written.editingId)
        assertTrue(typed.go(CanvasIntent.FinishEditing).page.elements.isEmpty())

        val note = state().go(CanvasIntent.PlaceSticky(CanvasPoint(0f, 0f)))
        val noteId = note.editingId!!
        assertEquals("Pray", note.go(CanvasIntent.EditText(noteId, "Pray")).el<CanvasElement.Sticky>(noteId).text)
        val shapeState = state(shape("s"))
        val typedIntoShape = shapeState.go(CanvasIntent.EditText("s", "x"))
        assertEquals(shapeState.document, typedIntoShape.document, "a shape has no text to type")
    }

    @Test
    fun `starting to type in the box already open is not a restart`() {
        val typed = state().go(CanvasIntent.PlaceText(CanvasPoint(0f, 0f)))
        val again = typed.go(CanvasIntent.StartEditing(typed.editingId!!))
        assertSame(typed, again, "an empty box being typed in must survive a tap on itself")
    }

    @Test
    fun `starting to type elsewhere finishes the box before`() {
        val s = state(text("kept")).go(CanvasIntent.PlaceText(CanvasPoint(0f, 0f)), CanvasIntent.StartEditing("kept"))
        assertEquals(listOf("kept"), s.ids(), "the empty box was dropped")
        assertEquals("kept", s.editingId)
        assertEquals(listOf("kept"), s.selectedIds)
    }

    @Test
    fun `a picture is inserted where asked, or in the middle`() {
        val at = state().go(CanvasIntent.InsertImage(ImageSource.CAMERA, CanvasPoint(500f, 400f)))
        val image = at.page.elements.single() as CanvasElement.Image
        assertEquals(ImageSource.CAMERA, image.source)
        assertEquals(500f, image.x + image.width / 2)
        assertEquals(at.ids(), at.selectedIds)
        val middle = state().go(CanvasIntent.InsertImage(ImageSource.FILES))
        assertTrue(middle.page.elements.single() is CanvasElement.Image)
    }

    @Test
    fun `replace swaps the picture in place`() {
        val s = state(image("i", x = 10f)).go(
            CanvasIntent.Select("i"),
            CanvasIntent.OnSelection(SelectionAction.REPLACE),
        )
        assertEquals(CanvasPanel.INSERT_IMAGE, s.panel)
        assertEquals("i", s.replaceTarget)
        val replaced = s.go(CanvasIntent.InsertImage(ImageSource.PHOTOS))
        val image = replaced.page.elements.single() as CanvasElement.Image
        assertEquals("i", image.id)
        assertEquals(10f, image.x)
        assertEquals(ImageSource.PHOTOS, image.source)
        assertEquals("", image.fileName)
        assertNull(replaced.replaceTarget)
        assertEquals(CanvasPanel.NONE, replaced.panel)
        val closed = s.go(CanvasIntent.ShowPanel(CanvasPanel.NONE))
        assertNull(closed.replaceTarget, "closing the panel forgets the replace")
    }

    // ── The selection bar ────────────────────────────────────────────────

    @Test
    fun `crop works on a single picture, replace needs exactly one`() {
        val s = state(image("i", width = 300f, height = 200f)).go(CanvasIntent.Select("i"))
        assertEquals(200f, s.go(CanvasIntent.OnSelection(SelectionAction.CROP)).el<CanvasElement.Image>("i").width)
        val two = state(image("i"), image("j")).go(CanvasIntent.SelectMany(listOf("i", "j")))
        assertSame(two, two.go(CanvasIntent.OnSelection(SelectionAction.REPLACE)))
        val nothing = state().go(CanvasIntent.OnSelection(SelectionAction.DELETE))
        assertEquals(state(), nothing, "nothing selected, nothing done")
    }

    @Test
    fun `front, duplicate and delete leave locked elements alone`() {
        val s = state(text("a"), image("locked", locked = true), text("b"))
            .go(CanvasIntent.SelectMany(listOf("a", "locked")))
        assertEquals(listOf("locked", "b", "a"), s.go(CanvasIntent.OnSelection(SelectionAction.BRING_TO_FRONT)).ids())

        val duplicated = s.go(CanvasIntent.OnSelection(SelectionAction.DUPLICATE))
        assertEquals(4, duplicated.page.elements.size)
        assertEquals(listOf("new-1"), duplicated.selectedIds, "only the loose one was copied, and the copy is selected")

        val deleted = s.go(CanvasIntent.OnSelection(SelectionAction.DELETE))
        assertEquals(listOf("locked", "b"), deleted.ids())
        assertEquals(listOf("locked"), deleted.selectedIds)
    }

    @Test
    fun `lock locks everything selected, or unlocks it once all are locked`() {
        val s = state(text("a"), image("b", locked = true)).go(CanvasIntent.SelectMany(listOf("a", "b")))
        val locked = s.go(CanvasIntent.OnSelection(SelectionAction.LOCK))
        assertTrue(locked.page.elements.all { it.locked })
        assertTrue(locked.go(CanvasIntent.OnSelection(SelectionAction.LOCK)).page.elements.none { it.locked })
    }

    // ── Arrange ──────────────────────────────────────────────────────────

    private val arranged = state(shape("a", x = 0f), shape("b", x = 40f), shape("c", x = 200f))
        .go(CanvasIntent.SelectMany(listOf("a", "b", "c")))
    private val measured = arranged.page.elements.associate {
        val s = it as CanvasElement.Shape
        it.id to CanvasBounds(s.x, s.y, s.x + s.width, s.y + s.height)
    }

    private fun CanvasUiState.xs() = page.elements.map { (it as CanvasElement.Shape).x }

    @Test
    fun `align and distribute move the selection`() {
        assertEquals(listOf(0f, 0f, 0f), arranged.go(CanvasIntent.Arrange(ArrangeAction.ALIGN, measured)).xs())
        val spread = arranged.go(CanvasIntent.Arrange(ArrangeAction.DISTRIBUTE, measured)).xs()
        assertEquals(listOf(0f, 100f, 200f), spread)
    }

    @Test
    fun `group ties the selection together, and again sets it loose`() {
        val grouped = arranged.go(CanvasIntent.Arrange(ArrangeAction.GROUP, measured))
        val group = grouped.page.elements.first().group
        assertTrue(group != null && grouped.page.elements.all { it.group == group })
        val ungrouped = grouped.go(CanvasIntent.Arrange(ArrangeAction.GROUP, measured))
        assertTrue(ungrouped.page.elements.all { it.group == null })
    }

    @Test
    fun `front and back restack the selection`() {
        val one = arranged.go(CanvasIntent.Select("a"))
        assertEquals(listOf("b", "c", "a"), one.go(CanvasIntent.Arrange(ArrangeAction.FRONT, measured)).ids())
        val last = arranged.go(CanvasIntent.Select("c"))
        assertEquals(listOf("c", "a", "b"), last.go(CanvasIntent.Arrange(ArrangeAction.BACK, measured)).ids())
    }

    // ── Pictures, layers, moving ─────────────────────────────────────────

    @Test
    fun `adjusting works on the selected pictures that are not locked`() {
        val s = state(image("i"), image("locked", locked = true), text("t"))
            .go(CanvasIntent.SelectMany(listOf("i", "locked", "t")))
        val flipped = s.go(CanvasIntent.AdjustImage(ImageAdjust.FLIP))
        assertTrue(flipped.el<CanvasElement.Image>("i").flipped)
        assertFalse(flipped.el<CanvasElement.Image>("locked").flipped)
    }

    @Test
    fun `the layers panel locks, hides and restacks`() {
        val s = state(text("a"), text("b"), text("c"))
        assertTrue(s.go(CanvasIntent.ToggleLock("b")).element("b").locked)
        assertFalse(s.go(CanvasIntent.ToggleLock("b"), CanvasIntent.ToggleLock("b")).element("b").locked)
        assertTrue(s.go(CanvasIntent.ToggleHidden("c")).element("c").hidden)
        assertEquals(listOf("b", "c", "a"), s.go(CanvasIntent.MoveLayer("a", 5)).ids())
    }

    @Test
    fun `move and resize leave locked elements where they are`() {
        val s = state(shape("a"), shape("locked").let { it.copy(locked = true) })
        val moved = s.go(CanvasIntent.Move(listOf("a", "locked"), 5f, 6f, session = 1))
        assertEquals(5f, moved.el<CanvasElement.Shape>("a").x)
        assertEquals(0f, moved.el<CanvasElement.Shape>("locked").x)

        val bounds = CanvasBounds(1f, 2f, 61f, 42f)
        assertEquals(60f, s.go(CanvasIntent.Resize("a", bounds, 1)).el<CanvasElement.Shape>("a").width)
        assertEquals(100f, s.go(CanvasIntent.Resize("locked", bounds, 1)).el<CanvasElement.Shape>("locked").width)
    }

    // ── Options and the selection ────────────────────────────────────────

    @Test
    fun `changing a tool's options restyles what is selected`() {
        val s = state(text("t"), sticky("n"), shape("s"), image("i"), ink("line", kind = InkKind.ARROW), text("locked"))
            .go(
                CanvasIntent.ToggleLock("locked"),
                CanvasIntent.SelectMany(listOf("t", "n", "s", "i", "line", "locked")),
            )
        val o = s.options
        val restyled = s.go(
            CanvasIntent.SetOptions(
                o.copy(
                    text = o.text.copy(size = 64),
                    sticky = o.sticky.copy(color = StickyColor.BLUE, tilt = false),
                    shapes = o.shapes.copy(fill = ShapeFill.PINK),
                    image = o.image.copy(opacity = 0.5f),
                    line = o.line.copy(ink = CanvasInk.AMBER, width = 9f),
                ),
            ),
        )
        assertEquals(64, restyled.el<CanvasElement.Text>("t").format?.size)
        assertEquals(StickyColor.BLUE, restyled.el<CanvasElement.Sticky>("n").color)
        assertEquals(0f, restyled.el<CanvasElement.Sticky>("n").rotation)
        assertEquals(CanvasInk.RED, restyled.el<CanvasElement.Shape>("s").ink, "a pink fill brings a red outline")
        assertEquals(0.5f, restyled.el<CanvasElement.Image>("i").opacity)
        assertEquals(9f, restyled.el<CanvasElement.Ink>("line").width)
        assertNull(restyled.el<CanvasElement.Text>("locked").format, "locked elements are left alone")
    }

    @Test
    fun `changing options with nothing selected only changes the options`() {
        val s = state(text("t"))
        val o = s.options.copy(text = s.options.text.copy(size = 12))
        val changed = s.go(CanvasIntent.SetOptions(o))
        assertEquals(o, changed.options)
        assertEquals(s.document, changed.document)
    }

    @Test
    fun `the pen's options never restyle a selected pen stroke`() {
        val s = state(ink("k")).go(CanvasIntent.Select("k"))
        val o = s.options.copy(pen = s.options.pen.copy(width = 20f))
        assertEquals(4f, s.go(CanvasIntent.SetOptions(o)).el<CanvasElement.Ink>("k").width)
    }

    @Test
    fun `selecting one element loads its style into the options`() {
        val format = TextOptions(size = 80)
        assertEquals(format, state(text("t").copy(format = format)).go(CanvasIntent.Select("t")).options.text)

        val note = sticky("n").copy(color = StickyColor.ORANGE, rotation = 0f)
        val noteOptions = state(note).go(CanvasIntent.Select("n")).options.sticky
        assertEquals(StickyColor.ORANGE, noteOptions.color)
        assertFalse(noteOptions.tilt)

        val shapeElement = shape("s").copy(fill = ShapeFill.YELLOW, borderWidth = 7f)
        val shapeOptions = state(shapeElement).go(CanvasIntent.Select("s")).options.shapes
        assertEquals(ShapeFill.YELLOW, shapeOptions.fill)
        assertEquals(7f, shapeOptions.borderWidth)

        assertFalse(state(image("i").copy(rounded = false)).go(CanvasIntent.Select("i")).options.image.rounded)

        val arrow = ink("a", kind = InkKind.ARROW).copy(dashed = true)
        assertTrue(state(arrow).go(CanvasIntent.Select("a")).options.line.dashed)

        val pen = state(ink("k"))
        assertEquals(pen.options, pen.go(CanvasIntent.Select("k")).options, "a pen stroke has no options to load")
        val plain = state(text("t"))
        assertEquals(plain.options, plain.go(CanvasIntent.Select("t")).options, "text with no format of its own")
    }

    @Test
    fun `rotate turns an element and leaves a locked one alone`() {
        val s = state(shape("s"), shape("l").copy(locked = true))
        assertEquals(45f, s.go(CanvasIntent.Rotate("s", 45f)).element("s").angle)
        assertEquals(-90f, s.go(CanvasIntent.Rotate("s", 270f)).element("s").angle, "stored one way round")
        assertEquals(0f, s.go(CanvasIntent.Rotate("l", 45f)).element("l").angle)
    }

    @Test
    fun `a picked picture is placed with its key and its own shape`() {
        val picture = StoredPicture("canvas-picture-1", width = 1000, height = 500)
        val image = state().go(CanvasIntent.InsertImage(ImageSource.PHOTOS, CanvasPoint(500f, 400f), picture))
            .page.elements.single() as CanvasElement.Image
        assertEquals("canvas-picture-1", image.photo)
        assertEquals(2f, image.width / image.height, "keeps the picture's shape")
    }
}
