package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_image_camera
import churchpresentermobile.composeapp.generated.resources.canvas_image_clipboard
import churchpresentermobile.composeapp.generated.resources.canvas_image_file
import churchpresentermobile.composeapp.generated.resources.canvas_image_photo
import churchpresentermobile.composeapp.generated.resources.canvas_pasted_image
import com.church.presenter.churchpresentermobile.model.BorderStyle
import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.ImageSource
import com.church.presenter.churchpresentermobile.model.InkKind
import com.church.presenter.churchpresentermobile.model.ShapeFill
import com.church.presenter.churchpresentermobile.model.StickyFont
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import org.jetbrains.compose.resources.stringResource

/**
 * Draws one [element] at its place on the page. Positions are in dp; the board
 * scales the whole page for zoom, so nothing here knows the zoom level.
 *
 * @param editing Whether this text box or note is being typed into, which swaps
 *   its text for a field.
 * @param onTextBounds Where a text box landed once laid out, for its selection frame.
 */
@Composable
internal fun CanvasElementView(
    element: CanvasElement,
    editing: Boolean,
    paths: InkPaths,
    onIntent: (CanvasIntent) -> Unit,
    onTextBounds: (String, CanvasBounds) -> Unit,
) {
    val tagged = Modifier.testTag(CanvasTags.element(element.id))
    when (element) {
        is CanvasElement.Text -> TextElement(element, editing, onIntent, onTextBounds, tagged)
        is CanvasElement.Ink -> InkElement(element, paths, tagged)
        is CanvasElement.Image -> ImageElement(element, tagged)
        is CanvasElement.Shape -> ShapeElement(element, tagged)
        is CanvasElement.Sticky -> StickyElement(element, editing, onIntent, tagged)
    }
}

/** A stroke, drawn over the whole page so it can go anywhere on it. */
@Composable
private fun InkElement(element: CanvasElement.Ink, paths: InkPaths, modifier: Modifier) {
    val colors = canvasColors
    val path = paths.path(element.pathData)
    val color = when {
        // A pixel eraser paints the board back over whatever it crossed.
        element.kind == InkKind.ERASER -> colors.board
        element.highlight != null && element.custom == null -> highlightColor(element.highlight)
        else -> colors.inkColor(element.ink, element.custom)
    }
    Canvas(modifier.fillMaxSize()) {
        translate(element.x.dp.toPx(), element.y.dp.toPx()) {
            scale(density, pivot = Offset.Zero) {
                drawPath(
                    path = path,
                    color = color,
                    alpha = element.opacity,
                    style = Stroke(
                        width = element.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (element.dashed) {
                            PathEffect.dashPathEffect(floatArrayOf(element.width * DASH, element.width * GAP))
                        } else {
                            null
                        },
                    ),
                )
            }
        }
    }
}

private const val DASH = 3f
private const val GAP = 2.5f

/** A dashed outline's dash and gap, as multiples of its width. */
private const val OUTLINE_DASH = 3f
private const val OUTLINE_GAP = 2f

/**
 * A placed picture. The image pipeline does not exist yet, so this is the design's
 * placeholder: a striped card naming the file, or where it came from. Rounded
 * corners, shadow, opacity, flip, rotation and Remove background all show on it.
 */
@Composable
private fun ImageElement(element: CanvasElement.Image, modifier: Modifier) {
    val colors = canvasColors
    val shape = if (element.rounded) RoundedCornerShape(14.dp) else RectangleShape
    val name = element.fileName.ifEmpty { element.source?.let { stringResource(it.placeholderName()) }.orEmpty() }
    Box(
        modifier = modifier
            .offset(element.x.dp, element.y.dp)
            .size(element.width.dp, element.height.dp)
            .rotate(element.rotation.toFloat())
            .alpha(element.opacity)
            .then(if (element.shadow) Modifier.shadow(10.dp, shape) else Modifier)
            .clip(shape)
            .then(if (element.cutOut) Modifier else Modifier.stripes(colors))
            .then(
                if (element.cutOut) {
                    Modifier.dashedOutline(colors.border, shape)
                } else {
                    Modifier.border(1.dp, colors.border, shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = if (element.flipped) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier,
        ) {
            Icon(Icons.Outlined.Image, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(Res.string.canvas_pasted_image) + "\n" + name,
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 15.6.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** What a picture brought in from [this] is called until it has a file name of its own. */
private fun ImageSource.placeholderName() = when (this) {
    ImageSource.CLIPBOARD -> Res.string.canvas_image_clipboard
    ImageSource.PHOTOS -> Res.string.canvas_image_photo
    ImageSource.CAMERA -> Res.string.canvas_image_camera
    ImageSource.FILES -> Res.string.canvas_image_file
}

/** The design's diagonal stripes behind a placeholder picture. */
private fun Modifier.stripes(colors: CanvasColors): Modifier = drawBehind {
    drawRect(colors.imageStripe)
    val stripe = 14.dp.toPx()
    clipRect {
        var start = -size.height
        while (start < size.width) {
            val band = Path().apply {
                moveTo(start, 0f)
                lineTo(start + stripe, 0f)
                lineTo(start + stripe + size.height, size.height)
                lineTo(start + size.height, size.height)
                close()
            }
            drawPath(band, colors.imageStripeAlt)
            start += stripe * 2
        }
    }
}

/** A dashed edge along [shape], for a shape with a dashed border or a picture cut out of its backdrop. */
private fun Modifier.dashedOutline(color: Color, shape: Shape, width: Float = 1.5f): Modifier = drawBehind {
    val stroke = width.dp.toPx()
    drawOutline(
        outline = shape.createOutline(size, layoutDirection, this),
        color = color,
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * OUTLINE_DASH, stroke * OUTLINE_GAP)),
        ),
    )
}

@Composable
private fun ShapeElement(element: CanvasElement.Shape, modifier: Modifier) {
    val colors = canvasColors
    val outline = colors.inkColor(element.ink, element.custom)
    val fill = element.fillCustom?.let { colors.inkColor(element.ink, it) }
        ?: shapeFillColor(element.fill).takeIf { element.fill != ShapeFill.NONE }
    val shape = canvasShape(element.shape)
    val border = when (element.border) {
        BorderStyle.SOLID -> Modifier.border(element.borderWidth.dp, outline, shape)
        BorderStyle.DASHED -> Modifier.dashedOutline(outline, shape, element.borderWidth)
        BorderStyle.NONE -> Modifier
    }
    Box(
        modifier = modifier
            .offset(element.x.dp, element.y.dp)
            .size(element.width.dp, element.height.dp)
            .clip(shape)
            .background(fill?.copy(alpha = SHAPE_FILL_ALPHA) ?: Color.Transparent)
            .then(border),
        contentAlignment = Alignment.Center,
    ) {
        if (element.text.isNotEmpty()) {
            Text(
                text = element.text,
                color = if (fill != null) outline else colors.text,
                fontSize = if (fill != null) 22.sp else 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

/** How strongly a shape is tinted with its fill. */
private const val SHAPE_FILL_ALPHA = 0.18f

@Composable
private fun StickyElement(
    element: CanvasElement.Sticky,
    editing: Boolean,
    onIntent: (CanvasIntent) -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 22.dp, bottomStart = 4.dp)
    val style = TextStyle(
        color = StickyInk,
        fontSize = 19.sp,
        lineHeight = 25.6.sp,
        fontWeight = if (element.font == StickyFont.PLAIN) FontWeight.Medium else FontWeight.Bold,
        fontFamily = when (element.font) {
            StickyFont.MARKER -> FontFamily.Cursive
            StickyFont.NEAT -> FontFamily.Serif
            StickyFont.PLAIN -> FontFamily.Default
        },
    )
    Box(
        modifier = modifier
            .offset(element.x.dp, element.y.dp)
            .size(element.width.dp, element.height.dp)
            .rotate(element.rotation)
            .shadow(8.dp, shape)
            .background(stickyColor(element.color), shape)
            .padding(18.dp),
    ) {
        if (editing) {
            EditableText(element.id, element.text, style, onIntent, Modifier.fillMaxSize())
        } else {
            Text(text = element.text, style = style)
        }
    }
}
