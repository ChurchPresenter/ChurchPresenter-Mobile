package com.church.presenter.churchpresentermobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.LinearGradientShader
import com.church.presenter.churchpresentermobile.ui.theme.SplashField
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.app_title
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * The sizes the splash is drawn at. The design draws the tablet's cross, title
 * and tagline about one-and-a-half times the phone's; the gradient field
 * behind them is the screen's own size on both.
 */
private class SplashMetrics(
    val crossWidth: Dp,
    val title: TextUnit,
    val tagline: TextUnit,
    val taglineTracking: TextUnit,
    val gapAfterCross: Dp,
    val gapAfterTitle: Dp,
) {
    /** The cross is drawn from a 56×112 path, so it is always twice as tall as wide. */
    val crossHeight: Dp get() = crossWidth * 2

    companion object {
        val Phone = SplashMetrics(
            crossWidth = 56.dp, title = 28.sp, tagline = 12.sp, taglineTracking = 0.1.em,
            gapAfterCross = 36.dp, gapAfterTitle = 14.dp,
        )
        val Tablet = SplashMetrics(
            crossWidth = 86.dp, title = 54.sp, tagline = 15.sp, taglineTracking = 0.22.em,
            gapAfterCross = 52.dp, gapAfterTitle = 22.dp,
        )
    }
}

/** Where the ambient wash is centred, as fractions of the screen: the design's `at 50% 34%`. */
private const val AMBIENT_CENTER_Y = 0.34f

/** The ambient ellipse's radii as fractions of the screen: the design's `115% 78%`. */
private const val AMBIENT_RADIUS_X = 1.15f
private const val AMBIENT_RADIUS_Y = 0.78f

/** Where the core glow sits: the design's `circle at 50% 40%`. */
private const val CORE_CENTER_Y = 0.40f

/** How many screen heights the ambient rect spans, so the squash cannot uncover an edge. */
private const val AMBIENT_COVER = 3f

/**
 * Draws the three-layer gradient field behind the splash.
 *
 * Compose's radial gradient is a circle, and the ambient wash is an ellipse,
 * so that layer is drawn as a circle under a vertical squash. The core glow's
 * radius is the distance to the farthest corner, which is what CSS gives a
 * `circle` with no size — so the glow spans the screen rather than a box.
 */
private fun Modifier.splashField(field: SplashField): Modifier = drawBehind {
    val ambientCenter = Offset(size.width / 2, size.height * AMBIENT_CENTER_Y)
    val ambientRadius = size.width * AMBIENT_RADIUS_X
    val squash = (size.height * AMBIENT_RADIUS_Y) / ambientRadius
    scale(scaleX = 1f, scaleY = squash, pivot = ambientCenter) {
        // Under the squash the rect has to be taller than the screen to still
        // cover it; a screen height either side is more than enough.
        drawRect(
            brush = ShaderBrush(
                RadialGradientShader(ambientCenter, ambientRadius, field.ambient.colors, field.ambient.stops),
            ),
            topLeft = Offset(0f, -size.height / squash),
            size = Size(size.width, size.height * AMBIENT_COVER / squash),
        )
    }
    val coreCenter = Offset(size.width / 2, size.height * CORE_CENTER_Y)
    val farthest = maxOf(
        (coreCenter - Offset(0f, 0f)).getDistance(),
        (coreCenter - Offset(size.width, 0f)).getDistance(),
        (coreCenter - Offset(0f, size.height)).getDistance(),
        (coreCenter - Offset(size.width, size.height)).getDistance(),
    )
    drawRect(brush = ShaderBrush(RadialGradientShader(coreCenter, farthest, field.core.colors, field.core.stops)))
    drawRect(
        brush = ShaderBrush(
            LinearGradientShader(Offset.Zero, Offset(0f, size.height), field.wash.colors, field.wash.stops),
        ),
    )
}

/**
 * @param twoPane The tablet's sizes, see [usesTwoPaneLayout]. The field behind
 *   the cross needs no such flag: it is drawn to whatever size the screen is.
 */
@Composable
fun SplashScreen(onComplete: () -> Unit, twoPane: Boolean = false) {
    val colors = LocalAppColors.current
    val m = if (twoPane) SplashMetrics.Tablet else SplashMetrics.Phone
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(0.82f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
        delay(2100)
        alpha.animateTo(0f, animationSpec = tween(450))
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.splashBackground)
            .splashField(colors.splashField),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CrossIcon(
                brush = colors.crossBrush,
                modifier = Modifier.size(width = m.crossWidth, height = m.crossHeight)
            )

            Spacer(modifier = Modifier.height(m.gapAfterCross))

            Text(
                text = stringResource(Res.string.app_title),
                fontSize = m.title,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.035).em,
                color = colors.splashTitle
            )

            Spacer(modifier = Modifier.height(m.gapAfterTitle))

            // Tagline: three per-word colors with dot separators
            Row(verticalAlignment = Alignment.CenterVertically) {
                TaglineWord("WORSHIP", colors.taglineWorship, m)
                TaglineDot(colors.taglineDot, m)
                TaglineWord("PRESENT", colors.taglinePresent, m)
                TaglineDot(colors.taglineDot, m)
                TaglineWord("CONNECT", colors.taglineConnect, m)
            }
        }
    }
}

@Composable
private fun TaglineWord(text: String, color: Color, m: SplashMetrics) {
    Text(
        text = text,
        fontSize = m.tagline,
        fontWeight = FontWeight.Bold,
        letterSpacing = m.taglineTracking,
        color = color
    )
}

@Composable
private fun TaglineDot(color: Color, m: SplashMetrics) {
    Text(
        text = "  ·  ",
        fontSize = m.tagline,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

/**
 * Latin cross rendered from the design's exact vector path
 * (`M24 0h8v28h24v8H32v76H24V36H0V28h24V0z`, viewBox 56×112), filled with a
 * vertical gradient [brush].
 *
 * `internal` rather than private so the side rail's brand mark is the same
 * vector as the splash's, drawn with a solid accent instead of the gradient.
 */
@Composable
internal fun CrossIcon(brush: Brush, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(24f, 0f)
            lineTo(32f, 0f)
            lineTo(32f, 28f)
            lineTo(56f, 28f)
            lineTo(56f, 36f)
            lineTo(32f, 36f)
            lineTo(32f, 112f)
            lineTo(24f, 112f)
            lineTo(24f, 36f)
            lineTo(0f, 36f)
            lineTo(0f, 28f)
            lineTo(24f, 28f)
            close()
        }
        val sx = size.width / 56f
        val sy = size.height / 112f
        scale(sx, sy, pivot = Offset.Zero) {
            drawPath(path = path, brush = brush, style = Fill)
        }
    }
}
