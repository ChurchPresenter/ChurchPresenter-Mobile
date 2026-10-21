package com.church.presenter.churchpresentermobile.model

/**
 * A canvas: a whiteboard of pages the operator sketches on and puts on screen.
 *
 * Tablet-only, and for now in memory only — nothing here is saved or sent anywhere
 * yet. The shapes are plain data so that the day a canvas is stored or projected,
 * this is what gets stored or projected.
 *
 * @param title What the operator called it. Null for one never named, which the
 *   screen shows as "Untitled canvas" in the operator's own language.
 */
data class CanvasDocument(
    val title: String? = null,
    val pages: List<CanvasPage> = listOf(CanvasPage()),
)

/** A point on a canvas page, in dp at 100% zoom from the page's top-left corner. */
data class CanvasPoint(val x: Float, val y: Float)

/** One page of a [CanvasDocument]: its elements, bottom-most first. */
data class CanvasPage(val elements: List<CanvasElement> = emptyList())

/**
 * One thing on a page.
 *
 * Positions and sizes are in dp on the page at 100% zoom, measured from its
 * top-left corner, so the board scales them rather than the model knowing about zoom.
 *
 * [label] is what the Layers panel lists it as. Null means "name it by its kind",
 * which the panel does in the operator's language — the operator only types a name
 * for the ones worth telling apart.
 */
sealed interface CanvasElement {
    val id: String
    val label: String?
    val locked: Boolean
    val hidden: Boolean

    /** A copy with the editing flags changed — every element has them, whatever else it holds. */
    fun withFlags(locked: Boolean = this.locked, hidden: Boolean = this.hidden): CanvasElement

    /** A copy moved by ([dx], [dy]) and given [newId], for Duplicate. */
    fun duplicated(newId: String, dx: Float, dy: Float): CanvasElement

    /** Typed text; [style] picks between a heading and a line of body text. */
    data class Text(
        override val id: String,
        val x: Float,
        val y: Float,
        val text: String,
        val style: CanvasTextStyle = CanvasTextStyle.BODY,
        override val label: String? = null,
        override val locked: Boolean = false,
        override val hidden: Boolean = false,
    ) : CanvasElement {
        override fun withFlags(locked: Boolean, hidden: Boolean) = copy(locked = locked, hidden = hidden)
        override fun duplicated(newId: String, dx: Float, dy: Float) = copy(id = newId, x = x + dx, y = y + dy)
    }

    /**
     * Ink: a pen or highlighter stroke, or a drawn arrow.
     *
     * The stroke is SVG path data relative to ([x], [y]) — the same notation the
     * design is drawn in, and one a hand-drawn stroke turns into without loss
     * ("M x y L x y …"). A dot is a zero-length segment, drawn by its round cap.
     */
    data class Ink(
        override val id: String,
        val x: Float,
        val y: Float,
        val pathData: String,
        val ink: CanvasInk,
        val width: Float,
        val opacity: Float = 1f,
        val kind: InkKind = InkKind.PEN,
        /** A highlighter's colour, which takes the place of [ink]; null for every other stroke. */
        val highlight: HighlightColor? = null,
        override val label: String? = null,
        override val locked: Boolean = false,
        override val hidden: Boolean = false,
    ) : CanvasElement {
        override fun withFlags(locked: Boolean, hidden: Boolean) = copy(locked = locked, hidden = hidden)
        override fun duplicated(newId: String, dx: Float, dy: Float) = copy(id = newId, x = x + dx, y = y + dy)
    }

    /** A placed picture. Shown as a placeholder naming [fileName] until images are loaded for real. */
    data class Image(
        override val id: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val fileName: String,
        override val label: String? = null,
        override val locked: Boolean = false,
        override val hidden: Boolean = false,
    ) : CanvasElement {
        override fun withFlags(locked: Boolean, hidden: Boolean) = copy(locked = locked, hidden = hidden)
        override fun duplicated(newId: String, dx: Float, dy: Float) = copy(id = newId, x = x + dx, y = y + dy)
    }

    /** An outlined shape with optional text centred in it. [filled] tints the inside with the outline's ink. */
    data class Shape(
        override val id: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val shape: CanvasShape,
        val ink: CanvasInk,
        val text: String = "",
        val filled: Boolean = false,
        override val label: String? = null,
        override val locked: Boolean = false,
        override val hidden: Boolean = false,
    ) : CanvasElement {
        override fun withFlags(locked: Boolean, hidden: Boolean) = copy(locked = locked, hidden = hidden)
        override fun duplicated(newId: String, dx: Float, dy: Float) = copy(id = newId, x = x + dx, y = y + dy)
    }

    /** A sticky note, slightly turned by [rotation] degrees so it reads as stuck on. */
    data class Sticky(
        override val id: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val text: String,
        val color: StickyColor = StickyColor.YELLOW,
        val rotation: Float = 0f,
        override val label: String? = null,
        override val locked: Boolean = false,
        override val hidden: Boolean = false,
    ) : CanvasElement {
        override fun withFlags(locked: Boolean, hidden: Boolean) = copy(locked = locked, hidden = hidden)
        override fun duplicated(newId: String, dx: Float, dy: Float) = copy(id = newId, x = x + dx, y = y + dy)
    }
}

enum class CanvasTextStyle { TITLE, BODY }

/** What a stroke of [CanvasElement.Ink] was drawn with — it decides its icon in the Layers panel. */
enum class InkKind { PEN, HIGHLIGHTER, ARROW }

enum class CanvasShape { RECTANGLE, ROUNDED, ELLIPSE, TRIANGLE, STAR, DIAMOND, HEXAGON, SPEECH }

/**
 * The ink colours a stroke or outline can take.
 *
 * Named rather than stored as a colour because the board is drawn in both themes:
 * [CONTRAST] is white ink on the dark board and black ink on the light one, and the
 * bright inks darken a step on white paper so they still read. The screen maps each
 * to its colour for the theme it is drawing in.
 */
enum class CanvasInk { CONTRAST, INVERSE, RED, AMBER, GREEN, SKY, VIOLET }

enum class StickyColor { YELLOW, GREEN, BLUE, PINK, ORANGE, PURPLE }
