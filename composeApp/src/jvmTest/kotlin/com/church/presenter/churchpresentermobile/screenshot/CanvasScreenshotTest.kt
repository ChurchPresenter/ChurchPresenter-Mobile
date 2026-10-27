package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasFont
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.CanvasPage
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.CanvasToolOptions
import com.church.presenter.churchpresentermobile.model.TextOptions
import com.church.presenter.churchpresentermobile.ui.canvas.CanvasEditor
import com.church.presenter.churchpresentermobile.ui.canvas.CanvasTags
import com.church.presenter.churchpresentermobile.viewmodel.CanvasPanel
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState
import com.church.presenter.churchpresentermobile.viewmodel.SaveStatus
import kotlin.test.Test

/**
 * The canvas, in every state the operator can put it in — on the design's own
 * "Sunday sermon sketch" ([CanvasSketch]), at the iPad size the design is drawn at,
 * so each golden can be laid beside its design frame.
 *
 * Every state is a fixed [CanvasUiState] handed to the stateless [CanvasEditor]:
 * no ViewModel, so what a golden shows is the editor's own doing. The `…` menu and
 * its dialogs are the exception — they are the menu's own state, so those tests
 * press the button first and photograph what opened.
 */
@OptIn(ExperimentalTestApi::class)
class CanvasScreenshotTest {

    /** The sketch as the design frames it: page 2 of 4, saved, the pen out with the dock. */
    private val sketch = CanvasUiState(
        document = CanvasSketch.document,
        pageIndex = CanvasSketch.SKETCH_PAGE,
        tool = CanvasTool.PEN,
        options = CanvasToolOptions().let { it.copy(pen = it.pen.copy(opacity = DESIGN_OPACITY)) },
        saveStatus = SaveStatus.SAVED,
    )

    // ── The board ────────────────────────────────────────────────────────

    @Test
    fun emptyCanvas() = canvas("canvas__empty", CanvasUiState(saveStatus = SaveStatus.SAVED))

    /** Design 1a/1b: the sketch with the pen and its dock. */
    @Test
    fun sketchWithThePen() = canvas("canvas__pen-dock", sketch)

    @Test
    fun highlighterDock() = canvas("canvas__highlighter-dock", sketch.copy(tool = CanvasTool.HIGHLIGHTER))

    @Test
    fun zoomedIn() = canvas("canvas__zoomed-in", sketch.copy(zoom = 150))

    @Test
    fun zoomedOut() = canvas("canvas__zoomed-out", sketch.copy(zoom = 50))

    @Test
    fun manyPages() = canvas(
        "canvas__many-pages",
        sketch.copy(document = sketch.document.copy(pages = sketch.document.pages + List(4) { CanvasPage() })),
    )

    // ── Every tool's options (design 5a/5b) ─────────────────────────────

    @Test fun selectOptions() = options(CanvasTool.SELECT)

    @Test fun lassoOptions() = options(CanvasTool.LASSO)

    @Test fun penOptions() = options(CanvasTool.PEN)

    @Test fun highlighterOptions() = options(CanvasTool.HIGHLIGHTER)

    @Test fun eraserOptions() = options(CanvasTool.ERASER)

    @Test fun shapesOptions() = options(CanvasTool.SHAPES)

    @Test fun lineOptions() = options(CanvasTool.LINE)

    @Test fun textOptions() = options(CanvasTool.TEXT)

    @Test fun stickyOptions() = options(CanvasTool.STICKY)

    /** The image tool brings its Insert panel with it, so its options are captured with it out. */
    @Test fun imageOptions() = options(CanvasTool.IMAGE, panel = CanvasPanel.INSERT_IMAGE)

    /** A color mixed in the picker: the rainbow swatch shows it, ringed, and no named ink is. */
    @Test
    fun customColour() = canvas(
        "canvas__custom-colour",
        sketch.copy(
            optionsOpen = true,
            options = sketch.options.copy(pen = sketch.options.pen.copy(custom = "#F97316")),
        ),
    )

    // ── Selection ────────────────────────────────────────────────────────

    /** Design 3a: the photo selected, with its handles, knob and bar, and the Insert panel out. */
    @Test
    fun imageSelectedWithInsertPanel() = canvas(
        "canvas__insert-image",
        sketch.copy(
            tool = CanvasTool.SELECT,
            selectedIds = listOf(CanvasSketch.PHOTO),
            panel = CanvasPanel.INSERT_IMAGE,
        ),
    )

    /** Design 4a: the Layers panel, with the photo's row selected. */
    @Test
    fun layersPanel() = canvas(
        "canvas__layers",
        sketch.copy(tool = CanvasTool.LASSO, selectedIds = listOf(CanvasSketch.PHOTO), panel = CanvasPanel.LAYERS),
    )

    /** Several things selected: one frame round them all, no handles, no Crop or Replace. */
    @Test
    fun multiSelection() = canvas(
        "canvas__multi-selection",
        sketch.copy(
            tool = CanvasTool.SELECT,
            selectedIds = listOf(CanvasSketch.QUOTE, CanvasSketch.STICKY, CanvasSketch.ELLIPSE),
        ),
    )

    /** A locked picture: no handles, and the bar offers Unlock. */
    @Test
    fun lockedSelection() = canvas(
        "canvas__locked",
        sketch.copy(
            document = CanvasSketch.withElement(CanvasSketch.PHOTO) { it.withFlags(locked = true) },
            tool = CanvasTool.SELECT,
            selectedIds = listOf(CanvasSketch.PHOTO),
            panel = CanvasPanel.LAYERS,
        ),
    )

    /** A shape selected: resize handles but no turning knob, which only a picture has. */
    @Test
    fun shapeSelected() = canvas(
        "canvas__shape-selected",
        sketch.copy(tool = CanvasTool.SELECT, selectedIds = listOf(CanvasSketch.ELLIPSE)),
    )

    /** A text box being typed into, in the text tool's format, with its options out. */
    @Test
    fun editingText() {
        val format = TextOptions(font = CanvasFont.SANS, size = 40, ink = CanvasInk.SKY)
        val typed = CanvasElement.Text(id = "typed", x = 1000f, y = 600f, text = "Psalm 23:1", format = format)
        canvas(
            "canvas__editing-text",
            sketch.copy(
                document = CanvasSketch.plus(typed),
                tool = CanvasTool.TEXT,
                selectedIds = listOf(typed.id),
                editingId = typed.id,
                options = sketch.options.copy(text = format),
            ),
        )
    }

    // ── Projection and saving ────────────────────────────────────────────

    @Test
    fun live() = canvas("canvas__live", sketch.copy(live = true))

    /** Go Live faded: remote mode, or the presentation server is not running. */
    @Test
    fun cannotGoLive() = canvas("canvas__go-live-off", sketch, canProject = false)

    @Test
    fun saving() = canvas("canvas__saving", sketch.copy(saveStatus = SaveStatus.SAVING))

    @Test
    fun notSaved() = canvas("canvas__not-saved", sketch.copy(saveStatus = SaveStatus.FAILED))

    @Test
    fun untitled() = canvas(
        "canvas__untitled",
        sketch.copy(document = CanvasDocument(pages = sketch.document.pages)),
    )

    // ── The … menu and its dialogs ───────────────────────────────────────

    @Test
    fun moreMenu() = canvas("canvas__more-menu", sketch, popup = true) { click(CanvasTags.MORE) }

    @Test
    fun renameDialog() = canvas("canvas__rename-dialog", sketch, popup = true) {
        click(CanvasTags.MORE)
        click(CanvasTags.MENU_RENAME)
    }

    @Test
    fun clearCanvasDialog() = canvas("canvas__clear-dialog", sketch, popup = true) {
        click(CanvasTags.MORE)
        click(CanvasTags.MENU_CLEAR_CANVAS)
    }

    // ── Frame ────────────────────────────────────────────────────────────

    /** [tool]'s options panel open beside the rail, over the sketch. */
    private fun options(tool: CanvasTool, panel: CanvasPanel = CanvasPanel.NONE) = canvas(
        "canvas__options-${tool.name.lowercase()}",
        sketch.copy(tool = tool, optionsOpen = true, panel = panel),
    )

    /** The editor in [state], on an iPad-sized window, with every callback a no-op. */
    private fun canvas(
        name: String,
        state: CanvasUiState,
        canProject: Boolean = true,
        popup: Boolean = false,
        prepare: (ComposeUiTest.() -> Unit)? = null,
    ) = screenshot(name = name, width = null, surface = IPAD_SURFACE, popup = popup, prepare = prepare) {
        CanvasEditor(state = state, onIntent = {}, onBack = {}, canProject = canProject)
    }

    private fun ComposeUiTest.click(tag: String) {
        onNodeWithTag(tag).performClick()
        waitForIdle()
    }

    private companion object {
        /** The design's frame: a 12.9" iPad in landscape. */
        val IPAD_SURFACE = DpSize(1366.dp, 1024.dp)

        /** The dock's opacity in design 1a. */
        const val DESIGN_OPACITY = 0.85f
    }
}
