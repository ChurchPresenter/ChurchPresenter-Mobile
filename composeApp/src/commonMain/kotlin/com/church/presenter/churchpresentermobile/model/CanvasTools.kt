package com.church.presenter.churchpresentermobile.model

/** The ten tools down the canvas's tool rail, top to bottom. */
enum class CanvasTool { SELECT, LASSO, PEN, HIGHLIGHTER, ERASER, SHAPES, LINE, TEXT, STICKY, IMAGE }

/**
 * How every tool is set up — what its options panel shows and what it would draw with.
 *
 * One value per tool rather than one flat bag, so each panel takes only its own
 * part and cannot reach another tool's settings. Every default is the value the
 * design shows, which is also what a sensible first stroke looks like.
 */
data class CanvasToolOptions(
    val select: SelectOptions = SelectOptions(),
    val lasso: LassoOptions = LassoOptions(),
    val pen: PenOptions = PenOptions(),
    val highlighter: HighlighterOptions = HighlighterOptions(),
    val eraser: EraserOptions = EraserOptions(),
    val shapes: ShapeOptions = ShapeOptions(),
    val line: LineOptions = LineOptions(),
    val text: TextOptions = TextOptions(),
    val sticky: StickyOptions = StickyOptions(),
    val image: ImageOptions = ImageOptions(),
)

data class SelectOptions(
    val snapToGrid: Boolean = true,
    val smartGuides: Boolean = true,
    val selectLocked: Boolean = false,
)

enum class LassoShape { FREEFORM, RECTANGLE }

enum class LassoPicks { EVERYTHING, INK, OBJECTS }

data class LassoOptions(
    val shape: LassoShape = LassoShape.FREEFORM,
    val picks: LassoPicks = LassoPicks.EVERYTHING,
    val partial: Boolean = true,
)

enum class PenType { BALLPOINT, FOUNTAIN, BRUSH, PENCIL }

/**
 * The pen. [width] is in dp; [opacity] and [smoothing] run 0–1.
 *
 * The dock under the board edits the same value as the pen's options panel, so
 * the two can never disagree about what the next stroke looks like.
 */
data class PenOptions(
    val type: PenType = PenType.BALLPOINT,
    val ink: CanvasInk = CanvasInk.GREEN,
    val width: Float = 4f,
    val opacity: Float = 1f,
    val smoothing: Float = 0.6f,
    val pressure: Boolean = true,
)

enum class HighlightColor { YELLOW, GREEN, SKY, PINK, ORANGE }

data class HighlighterOptions(
    val color: HighlightColor = HighlightColor.YELLOW,
    val width: Float = 18f,
    val opacity: Float = 0.4f,
    val straightOnly: Boolean = false,
    val behindInk: Boolean = true,
)

enum class EraserMode { STROKE, PIXEL, AREA }

data class EraserOptions(
    val mode: EraserMode = EraserMode.STROKE,
    val size: Float = 24f,
    val highlighterOnly: Boolean = false,
)

enum class ShapeFill { NONE, GREEN, SKY, YELLOW, PINK }

enum class BorderStyle { SOLID, DASHED, NONE }

data class ShapeOptions(
    val shape: CanvasShape = CanvasShape.ROUNDED,
    val fill: ShapeFill = ShapeFill.GREEN,
    val border: BorderStyle = BorderStyle.SOLID,
    val borderWidth: Float = 3f,
    val snap: Boolean = true,
)

enum class LineStyle { STRAIGHT, CURVE, ELBOW }

enum class LineEnd { NONE, ARROW, DOT }

data class LineOptions(
    val style: LineStyle = LineStyle.STRAIGHT,
    val start: LineEnd = LineEnd.NONE,
    val end: LineEnd = LineEnd.ARROW,
    val ink: CanvasInk = CanvasInk.CONTRAST,
    val width: Float = 4f,
    val dashed: Boolean = false,
)

enum class CanvasTextAlign { LEFT, CENTER, RIGHT }

data class TextOptions(
    val size: Int = 32,
    val bold: Boolean = true,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val align: CanvasTextAlign = CanvasTextAlign.LEFT,
    val ink: CanvasInk = CanvasInk.CONTRAST,
    val highlight: Boolean = false,
    val bulleted: Boolean = false,
)

enum class StickySize { SMALL, MEDIUM, LARGE }

enum class StickyFont { MARKER, NEAT, PLAIN }

data class StickyOptions(
    val color: StickyColor = StickyColor.YELLOW,
    val size: StickySize = StickySize.MEDIUM,
    val font: StickyFont = StickyFont.MARKER,
    val tilt: Boolean = true,
)

enum class ImageSource { CLIPBOARD, PHOTOS, CAMERA, FILES }

enum class ImagePlacement { FIT, FILL, ORIGINAL }

data class ImageOptions(
    val source: ImageSource = ImageSource.CLIPBOARD,
    val placement: ImagePlacement = ImagePlacement.FIT,
    val rounded: Boolean = true,
    val shadow: Boolean = false,
    val opacity: Float = 1f,
)
