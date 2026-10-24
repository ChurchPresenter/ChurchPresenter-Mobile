package com.church.presenter.churchpresentermobile.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * What each placing tool puts on the page, built from the tool's options. Plain
 * functions so a test can check a line's arrowhead or a note's size without a board.
 */

/** A shape filling [bounds], drawn with the shape tool's current options. */
internal fun shapeElement(id: String, bounds: CanvasBounds, options: ShapeOptions): CanvasElement.Shape =
    CanvasElement.Shape(
        id = id,
        x = bounds.left,
        y = bounds.top,
        width = bounds.width,
        height = bounds.height,
        shape = options.shape,
        ink = outlineFor(options.fill),
        fill = options.fill,
        fillCustom = options.fillCustom,
        border = options.border,
        borderWidth = options.borderWidth,
        custom = options.fillCustom,
    )

/** The outline that goes with a fill: the same colour family, so a shape reads as one thing. */
internal fun outlineFor(fill: ShapeFill): CanvasInk = when (fill) {
    ShapeFill.NONE -> CanvasInk.CONTRAST
    ShapeFill.GREEN -> CanvasInk.GREEN
    ShapeFill.SKY -> CanvasInk.SKY
    ShapeFill.YELLOW -> CanvasInk.AMBER
    ShapeFill.PINK -> CanvasInk.RED
}

/** A line from [start] to [end], shaped and capped as the line tool is set. */
internal fun lineElement(id: String, start: CanvasPoint, end: CanvasPoint, options: LineOptions): CanvasElement.Ink =
    CanvasElement.Ink(
        id = id,
        x = 0f,
        y = 0f,
        pathData = linePathData(start, end, options),
        ink = options.ink,
        custom = options.custom,
        width = options.width,
        kind = InkKind.ARROW,
        dashed = options.dashed,
    )

/**
 * SVG path data for a line: straight, a gentle curve bowing to its left, or an
 * elbow that runs across, down and across again — then each end's cap.
 */
internal fun linePathData(start: CanvasPoint, end: CanvasPoint, options: LineOptions): String {
    val (sx, sy) = start.x.svgNumber() to start.y.svgNumber()
    val (ex, ey) = end.x.svgNumber() to end.y.svgNumber()
    val body = when (options.style) {
        LineStyle.STRAIGHT -> "M$sx $sy L$ex $ey"
        LineStyle.CURVE -> {
            val bow = hypot(end.x - start.x, end.y - start.y) * CURVE_BOW
            val angle = atan2(end.y - start.y, end.x - start.x)
            val cx = (start.x + end.x) / 2 + sin(angle) * bow
            val cy = (start.y + end.y) / 2 - cos(angle) * bow
            "M$sx $sy Q${cx.svgNumber()} ${cy.svgNumber()} $ex $ey"
        }
        LineStyle.ELBOW -> {
            val mid = ((start.x + end.x) / 2).svgNumber()
            "M$sx $sy H$mid V$ey H$ex"
        }
    }
    // Each cap points along the line's last leg, which for an elbow is level.
    val level = options.style == LineStyle.ELBOW
    val startFrom = if (level) CanvasPoint(end.x, start.y) else end
    val endFrom = if (level) CanvasPoint(start.x, end.y) else start
    return body +
        endCap(start, startFrom, options.start, options.width) +
        endCap(end, endFrom, options.end, options.width)
}

/** How far a curve bows out, as a share of its length. */
private const val CURVE_BOW = 0.25f

/** The cap at [tip] of a line arriving from [from]: nothing, an open arrowhead, or a ring. */
private fun endCap(tip: CanvasPoint, from: CanvasPoint, end: LineEnd, width: Float): String {
    val size = width * CAP_SCALE + CAP_BASE
    return when (end) {
        LineEnd.NONE -> ""
        LineEnd.ARROW -> {
            val angle = atan2(tip.y - from.y, tip.x - from.x)
            val wing = { turn: Double ->
                val a = angle + turn
                "${(tip.x - cos(a) * size).toFloat().svgNumber()} ${(tip.y - sin(a) * size).toFloat().svgNumber()}"
            }
            " M${wing(ARROW_SPREAD)} L${tip.x.svgNumber()} ${tip.y.svgNumber()} L${wing(-ARROW_SPREAD)}"
        }
        LineEnd.DOT -> {
            val r = (size / 2).svgNumber()
            val left = (tip.x - size / 2).svgNumber()
            val right = (tip.x + size / 2).svgNumber()
            val y = tip.y.svgNumber()
            " M$left $y A$r $r 0 1 0 $right $y A$r $r 0 1 0 $left $y"
        }
    }
}

private const val CAP_SCALE = 3f
private const val CAP_BASE = 8f
private const val ARROW_SPREAD = 0.5

/** An empty text box at [at], typed with the text tool's current options. */
internal fun textElement(id: String, at: CanvasPoint, options: TextOptions): CanvasElement.Text =
    CanvasElement.Text(id = id, x = at.x, y = at.y, text = "", format = options)

/** A blank sticky note centred on [at], in the sticky tool's colour, size and hand. */
internal fun stickyElement(id: String, at: CanvasPoint, options: StickyOptions): CanvasElement.Sticky {
    val side = when (options.size) {
        StickySize.SMALL -> STICKY_SMALL
        StickySize.MEDIUM -> STICKY_MEDIUM
        StickySize.LARGE -> STICKY_LARGE
    }
    return CanvasElement.Sticky(
        id = id,
        x = at.x - side / 2,
        y = at.y - side / 2,
        width = side,
        height = side * STICKY_ASPECT,
        text = "",
        color = options.color,
        rotation = if (options.tilt) STICKY_TILT else 0f,
        font = options.font,
    )
}

private const val STICKY_SMALL = 160f
private const val STICKY_MEDIUM = 212f
private const val STICKY_LARGE = 260f
private const val STICKY_ASPECT = 0.92f
private const val STICKY_TILT = -2f

/** A picture kept for the canvas: its key in the picture store and its size in pixels. */
data class StoredPicture(val key: String, val width: Int, val height: Int)

/**
 * A picture from [source] centred on [at], sized for the image tool's "When placed"
 * choice: Fit and Fill keep the picture's shape inside a smaller or larger frame,
 * and Original shows it pixel for pixel, as far as the page has room. Without a
 * [picture] it is a placeholder card, and the three are sizes of the card.
 */
internal fun imageElement(
    id: String,
    at: CanvasPoint,
    source: ImageSource,
    options: ImageOptions,
    picture: StoredPicture? = null,
): CanvasElement.Image {
    val frame = when (options.placement) {
        ImagePlacement.FIT -> FIT_SIZE
        ImagePlacement.FILL -> FILL_SIZE
        ImagePlacement.ORIGINAL -> ORIGINAL_SIZE
    }
    val (width, height) = picture?.let { pictureSize(it, options.placement) } ?: frame
    return CanvasElement.Image(
        id = id,
        x = at.x - width / 2,
        y = at.y - height / 2,
        width = width,
        height = height,
        fileName = "",
        source = source,
        photo = picture?.key,
        rounded = options.rounded,
        shadow = options.shadow,
        opacity = options.opacity,
    )
}

private val FIT_SIZE = 300f to 210f
private val FILL_SIZE = 420f to 295f
private val ORIGINAL_SIZE = 240f to 168f

/**
 * How big [picture] is placed: as large as fits the placement's frame without
 * changing its shape, or for Original its own size, shrunk only to fit the page.
 */
internal fun pictureSize(picture: StoredPicture, placement: ImagePlacement): Pair<Float, Float> {
    val w = picture.width.coerceAtLeast(1).toFloat()
    val h = picture.height.coerceAtLeast(1).toFloat()
    val (frameW, frameH) = when (placement) {
        ImagePlacement.FIT -> FIT_SIZE
        ImagePlacement.FILL -> FILL_SIZE
        ImagePlacement.ORIGINAL -> return (minOf(1f, ORIGINAL_MAX_EDGE / maxOf(w, h))).let { w * it to h * it }
    }
    val scale = minOf(frameW / w, frameH / h)
    return w * scale to h * scale
}

/** The longest edge an Original picture is placed at, so a photo still lands on the page. */
private const val ORIGINAL_MAX_EDGE = 640f

/**
 * [image] after [adjust]: Crop trims it to a centred square, Flip mirrors it,
 * Rotate turns it a quarter clockwise, and Remove background drops its backdrop
 * (or brings it back).
 */
internal fun adjusted(image: CanvasElement.Image, adjust: ImageAdjust): CanvasElement.Image = when (adjust) {
    ImageAdjust.CROP -> {
        val side = minOf(image.width, image.height)
        image.copy(
            x = image.x + (image.width - side) / 2,
            y = image.y + (image.height - side) / 2,
            width = side,
            height = side,
        )
    }
    ImageAdjust.FLIP -> image.copy(flipped = !image.flipped)
    ImageAdjust.ROTATE -> image.copy(angle = quarterTurned(image.angle))
    ImageAdjust.REMOVE_BACKGROUND -> image.copy(cutOut = !image.cutOut)
}

