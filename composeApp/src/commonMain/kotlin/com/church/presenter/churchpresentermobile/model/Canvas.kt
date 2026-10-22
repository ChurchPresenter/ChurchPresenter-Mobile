package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
@Serializable
data class CanvasDocument(
    val title: String? = null,
    val pages: List<CanvasPage> = listOf(CanvasPage()),
)

/** A point on a canvas page, in dp at 100% zoom from the page's top-left corner. */
@Serializable
data class CanvasPoint(val x: Float, val y: Float)

/** A rectangle on a canvas page, in the same units as [CanvasPoint]. */
@Serializable
data class CanvasBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** One page of a [CanvasDocument]: its elements, bottom-most first. */
@Serializable
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
 *
 * [group] ties elements together: picking one picks them all, as Group promises.
 *
 * Colours are a [CanvasInk] with an optional `custom` override — a "#RRGGBB" the
 * operator mixed in the colour picker — so the named inks can still follow the theme.
 */
@Serializable
sealed interface CanvasElement {
    val id: String
    val label: String?
    val locked: Boolean
    val hidden: Boolean
    val group: String?

    /** A copy with the editing flags changed — every element has them, whatever else it holds. */
    fun withFlags(locked: Boolean = this.locked, hidden: Boolean = this.hidden): CanvasElement

    fun withGroup(group: String?): CanvasElement

    /** A copy moved by ([dx], [dy]). */
    fun moved(dx: Float, dy: Float): CanvasElement

    fun withId(id: String): CanvasElement

    /**
     * Typed text. [style] picks between the heading and body looks; [format], when
     * set, is what the text tool typed it with and takes their place.
     */
    @Serializable
    @SerialName("text")
    data class Text(
        override val id: String,
        val x: Float,
        val y: Float,
        val text: String,
        val style: CanvasTextStyle = CanvasTextStyle.BODY,
        val format: TextOptions? = null,
        override val label: String? = null,
        override val locked: Boolean = false,
        override val hidden: Boolean = false,
        override val group: String? = null,
    ) : CanvasElement {
        override fun withFlags(locked: Boolean, hidden: Boolean) = copy(locked = locked, hidden = hidden)
        override fun withGroup(group: String?) = copy(group = group)
        override fun moved(dx: Float, dy: Float) = copy(x = x + dx, y = y + dy)
        override fun withId(id: String) = copy(id = id)
    }

    /**
     * Ink: a pen or highlighter stroke, a drawn line or arrow, or a pixel eraser's
     * stroke.
     *
     * The stroke is SVG path data relative to ([x], [y]) — the same notation the
     * design is drawn in, and one a hand-drawn stroke turns into without loss
     * ("M x y L x y …"). A dot is a zero-length segment, drawn by its round cap.
     */
    @Serializable
    @SerialName("ink")
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
        val custom: String? = null,
        val dashed: Boolean = false,
        override val label: String? = null,
        override val locked: Boolean = false,
        override val hidden: Boolean = false,
        override val group: String? = null,
    ) : CanvasElement {
        override fun withFlags(locked: Boolean, hidden: Boolean) = copy(locked = locked, hidden = hidden)
        override fun withGroup(group: String?) = copy(group = group)
        override fun moved(dx: Float, dy: Float) = copy(x = x + dx, y = y + dy)
        override fun withId(id: String) = copy(id = id)
    }

    /**
     * A placed picture. Shown as a placeholder naming [fileName] — or, for one
     * inserted before pictures are loaded for real, the [source] it came from.
     *
     * @param rotation Quarter turns clockwise, in degrees.
     * @param cutOut Whether Remove background has been applied: the placeholder
     *   drops its backdrop.
     */
    @Serializable
    @SerialName("image")
    data class Image(
        override val id: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val fileName: String,
        val source: ImageSource? = null,
        val rounded: Boolean = true,
        val shadow: Boolean = false,
        val opacity: Float = 1f,
        val flipped: Boolean = false,
        val rotation: Int = 0,
        val cutOut: Boolean = false,
        override val label: String? = null,
        override val locked: Boolean = false,
        override val hidden: Boolean = false,
        override val group: String? = null,
    ) : CanvasElement {
        override fun withFlags(locked: Boolean, hidden: Boolean) = copy(locked = locked, hidden = hidden)
        override fun withGroup(group: String?) = copy(group = group)
        override fun moved(dx: Float, dy: Float) = copy(x = x + dx, y = y + dy)
        override fun withId(id: String) = copy(id = id)
    }

    /** An outlined shape with optional text centred in it. */
    @Serializable
    @SerialName("shape")
    data class Shape(
        override val id: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val shape: CanvasShape,
        val ink: CanvasInk,
        val text: String = "",
        val fill: ShapeFill = ShapeFill.NONE,
        val fillCustom: String? = null,
        val border: BorderStyle = BorderStyle.SOLID,
        val borderWidth: Float = 4f,
        val custom: String? = null,
        override val label: String? = null,
        override val locked: Boolean = false,
        override val hidden: Boolean = false,
        override val group: String? = null,
    ) : CanvasElement {
        override fun withFlags(locked: Boolean, hidden: Boolean) = copy(locked = locked, hidden = hidden)
        override fun withGroup(group: String?) = copy(group = group)
        override fun moved(dx: Float, dy: Float) = copy(x = x + dx, y = y + dy)
        override fun withId(id: String) = copy(id = id)
    }

    /** A sticky note, slightly turned by [rotation] degrees so it reads as stuck on. */
    @Serializable
    @SerialName("sticky")
    data class Sticky(
        override val id: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val text: String,
        val color: StickyColor = StickyColor.YELLOW,
        val rotation: Float = 0f,
        val font: StickyFont = StickyFont.MARKER,
        override val label: String? = null,
        override val locked: Boolean = false,
        override val hidden: Boolean = false,
        override val group: String? = null,
    ) : CanvasElement {
        override fun withFlags(locked: Boolean, hidden: Boolean) = copy(locked = locked, hidden = hidden)
        override fun withGroup(group: String?) = copy(group = group)
        override fun moved(dx: Float, dy: Float) = copy(x = x + dx, y = y + dy)
        override fun withId(id: String) = copy(id = id)
    }
}

enum class CanvasTextStyle { TITLE, BODY }

/** What a stroke of [CanvasElement.Ink] was drawn with — it decides its icon in the Layers panel. */
enum class InkKind { PEN, HIGHLIGHTER, ARROW, ERASER }

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
