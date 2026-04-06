package com.church.presenter.churchpresentermobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onComplete: () -> Unit) {
    // Start fully visible — no blank frame before the animation begins
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(0.82f) }

    LaunchedEffect(Unit) {
        // Scale up gently on entry (content is already visible)
        scale.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
        // Hold — 1 second longer than before (was 1 100 ms, now 2 100 ms)
        delay(2100)
        // Fade out
        alpha.animateTo(0f, animationSpec = tween(450))
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
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
            CrossIcon(modifier = Modifier.size(320.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Church Presenter",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Worship  •  Present  •  Connect",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
            )
        }
    }
}

/**
 * Classic Latin cross drawn with Canvas.
 *
 * Proportions chosen to resemble a traditional church cross:
 *  - Slim bar (11 % of icon width)
 *  - Horizontal beam centred and placed at ~30 % from the top
 *  - Bottom arm roughly twice the length of the top arm
 *  - Arm width slightly wider than the vertical bar for visual balance
 */
@Composable
private fun CrossIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // All constants derived directly from ic_splash_cross.xml (viewport 24×24):
        //   barW      = 2.44 / 24  ≈ 0.1017
        //   armLength = 4.00 / 24  ≈ 0.1667   ← top arm = left arm = right arm ✓
        //   armW      = barW + 2×armLength = 10.44 / 24 ≈ 0.435
        //   vertTop   = 5.33 / 24  ≈ 0.2221
        //   armY      = 9.33 / 24  ≈ 0.3888   (= vertTop + armLength ✓)
        //   vertEnd   = 18.89 / 24 ≈ 0.7871   → extended to 0.88 (longer lower leg)
        val barW      = size.width  * 0.1017f
        val armLength = size.width  * 0.1667f
        val armW      = barW + 2f * armLength          // 0.435 × size
        val vertTop   = size.height * 0.2221f
        val armY      = vertTop + armLength            // top arm == armLength ✓
        val vertEnd   = size.height * 0.88f
        val r         = CornerRadius(barW / 2.2f)

        // ── Vertical bar ─────────────────────────────────────────────────
        drawRoundRect(
            color        = Color.White,
            topLeft      = Offset((size.width - barW) / 2f, vertTop),
            size         = Size(barW, vertEnd - vertTop),
            cornerRadius = r
        )
        // ── Horizontal arm ───────────────────────────────────────────────
        drawRoundRect(
            color        = Color.White,
            topLeft      = Offset((size.width - armW) / 2f, armY),
            size         = Size(armW, barW),           // height = barW → equal thickness
            cornerRadius = r
        )
    }
}

