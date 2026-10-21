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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_pasted_image
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasTextStyle
import org.jetbrains.compose.resources.stringResource

/**
 * Draws one [element] at its place on the page. Positions are in dp; the board
 * scales the whole page for zoom, so nothing here knows the zoom level.
 */
@Composable
internal fun CanvasElementView(element: CanvasElement, modifier: Modifier = Modifier) {
    val tagged = modifier.testTag(CanvasTags.element(element.id))
    when (element) {
        is CanvasElement.Text -> TextElement(element, tagged)
        is CanvasElement.Ink -> InkElement(element, tagged)
        is CanvasElement.Image -> ImageElement(element, tagged)
        is CanvasElement.Shape -> ShapeElement(element, tagged)
        is CanvasElement.Sticky -> StickyElement(element, tagged)
    }
}

@Composable
private fun TextElement(element: CanvasElement.Text, modifier: Modifier) {
    val colors = canvasColors
    val title = element.style == CanvasTextStyle.TITLE
    Text(
        text = element.text,
        color = if (title) colors.text else colors.secondary,
        fontSize = if (title) 54.sp else 22.sp,
        lineHeight = if (title) 54.sp else 33.sp,
        fontWeight = if (title) FontWeight.ExtraBold else FontWeight.Medium,
        fontFamily = if (title) FontFamily.Serif else FontFamily.Default,
        letterSpacing = if (title) (-0.02).em else 0.em,
        modifier = modifier.offset(element.x.dp, element.y.dp),
    )
}

/** A stroke, drawn over the whole page so it can go anywhere on it. */
@Composable
private fun InkElement(element: CanvasElement.Ink, modifier: Modifier) {
    val colors = canvasColors
    val path = rememberInkPath(element.pathData)
    val color = element.highlight?.let(::highlightColor) ?: colors.inkColor(element.ink)
    Canvas(modifier.fillMaxSize()) {
        translate(element.x.dp.toPx(), element.y.dp.toPx()) {
            scale(density, pivot = Offset.Zero) {
                drawPath(
                    path = path,
                    color = color,
                    alpha = element.opacity,
                    style = Stroke(width = element.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

/** [pathData] parsed once per value, in dp units. */
@Composable
internal fun rememberInkPath(pathData: String): Path =
    remember(pathData) { PathParser().parsePathString(pathData).toPath() }

/**
 * Where an ink stroke lies on the page, in dp, grown by half its width so a thin
 * stroke still offers something to tap.
 */
internal fun inkBounds(element: CanvasElement.Ink, path: Path): Rect {
    val reach = maxOf(element.width / 2f, MIN_INK_REACH)
    return path.getBounds().translate(element.x, element.y).inflate(reach)
}

/** The least a stroke reaches beyond its line, in dp — a finger is wider than a 2dp line. */
private const val MIN_INK_REACH = 8f

/**
 * A placed picture. The image pipeline does not exist yet, so this is the design's
 * placeholder: a striped card naming the file.
 */
@Composable
private fun ImageElement(element: CanvasElement.Image, modifier: Modifier) {
    val colors = canvasColors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .offset(element.x.dp, element.y.dp)
            .size(element.width.dp, element.height.dp)
            .clip(shape)
            .drawBehind {
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
            .border(1.dp, colors.border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Image, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(Res.string.canvas_pasted_image) + "\n" + element.fileName,
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

@Composable
private fun ShapeElement(element: CanvasElement.Shape, modifier: Modifier) {
    val colors = canvasColors
    val ink = colors.inkColor(element.ink)
    val shape = canvasShape(element.shape)
    Box(
        modifier = modifier
            .offset(element.x.dp, element.y.dp)
            .size(element.width.dp, element.height.dp)
            .clip(shape)
            .background(if (element.filled) ink.copy(alpha = SHAPE_FILL_ALPHA) else Color.Transparent)
            .border(if (element.filled) 3.dp else 4.dp, ink, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (element.text.isNotEmpty()) {
            Text(
                text = element.text,
                color = if (element.filled) ink else colors.text,
                fontSize = if (element.filled) 22.sp else 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

/** How strongly a filled shape is tinted with its outline's ink. */
private const val SHAPE_FILL_ALPHA = 0.12f

@Composable
private fun StickyElement(element: CanvasElement.Sticky, modifier: Modifier) {
    val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 22.dp, bottomStart = 4.dp)
    Box(
        modifier = modifier
            .offset(element.x.dp, element.y.dp)
            .size(element.width.dp, element.height.dp)
            .rotate(element.rotation)
            .shadow(8.dp, shape)
            .background(stickyColor(element.color), shape)
            .padding(18.dp),
    ) {
        Text(
            text = element.text,
            color = StickyInk,
            fontSize = 19.sp,
            lineHeight = 25.6.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Cursive,
        )
    }
}
