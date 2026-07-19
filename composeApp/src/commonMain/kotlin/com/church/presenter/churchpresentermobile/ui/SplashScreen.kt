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

@Composable
fun SplashScreen(onComplete: () -> Unit) {
    val colors = LocalAppColors.current

    // Start fully visible — no blank frame before the animation begins
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
            .background(colors.splashBackground),
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
            // Cross with radial glow behind it
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(colors.splashGlow, Color.Transparent),
                                radius = 320f
                            )
                        )
                )
                CrossIcon(
                    brush = colors.crossBrush,
                    modifier = Modifier.size(width = 64.dp, height = 128.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(Res.string.app_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.7).sp,
                color = colors.splashTitle
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tagline: three per-word colors with dot separators
            Row(verticalAlignment = Alignment.CenterVertically) {
                TaglineWord("WORSHIP", colors.taglineWorship)
                TaglineDot(colors.taglineDot)
                TaglineWord("PRESENT", colors.taglinePresent)
                TaglineDot(colors.taglineDot)
                TaglineWord("CONNECT", colors.taglineConnect)
            }
        }
    }
}

@Composable
private fun TaglineWord(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.em,
        color = color
    )
}

@Composable
private fun TaglineDot(color: Color) {
    Text(
        text = "  ·  ",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

/**
 * Latin cross rendered from the design's exact vector path
 * (`M24 0h8v28h24v8H32v76H24V36H0V28h24V0z`, viewBox 56×112), filled with a
 * vertical gradient [brush].
 */
@Composable
private fun CrossIcon(brush: Brush, modifier: Modifier = Modifier) {
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
