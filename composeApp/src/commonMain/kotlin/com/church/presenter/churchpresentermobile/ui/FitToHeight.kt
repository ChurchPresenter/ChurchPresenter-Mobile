package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

/** Below this the type gets too small to read; the caller's own scroll takes over instead. */
internal const val FIT_MIN_SCALE = 0.6f

/**
 * The density factor that makes content of [natural] px fit in [available] px — never above 1
 * (content is never enlarged) and never below [minScale].
 */
internal fun fitScale(natural: Int, available: Int, minScale: Float = FIT_MIN_SCALE): Float =
    if (natural <= 0 || available <= 0 || natural <= available) {
        1f
    } else {
        (available.toFloat() / natural).coerceIn(minScale, 1f)
    }

/**
 * Shrinks [content] uniformly so it fits the available height without scrolling.
 *
 * The content is first measured at its natural size (with unbounded height and no semantics, so
 * tests never see the measuring copy), then composed again under a density scaled by
 * [fitScale]. Every dp and sp shrinks together, so the design's proportions are kept, just
 * smaller. [content] receives whether it was clamped at [FIT_MIN_SCALE] and still overflows,
 * so it can add a scroll as a last resort.
 */
@Composable
internal fun FitToHeight(modifier: Modifier = Modifier, content: @Composable (overflows: Boolean) -> Unit) {
    val density = LocalDensity.current
    SubcomposeLayout(modifier) { constraints ->
        val probe = Constraints(maxWidth = constraints.maxWidth)
        val naturalHeight = subcompose(SLOT_MEASURE) {
            Box(Modifier.clearAndSetSemantics {}) { content(false) }
        }.maxOfOrNull { it.measure(probe).height } ?: 0
        val bounded = constraints.hasBoundedHeight
        val scale = if (bounded) fitScale(naturalHeight, constraints.maxHeight) else 1f
        val overflows = bounded && naturalHeight * scale > constraints.maxHeight
        val placeables = subcompose(SLOT_FIT) {
            CompositionLocalProvider(LocalDensity provides Density(density.density * scale, density.fontScale)) {
                content(overflows)
            }
        }.map { it.measure(constraints.copy(minHeight = 0)) }
        val height = if (bounded) constraints.maxHeight else placeables.maxOfOrNull { it.height } ?: 0
        layout(constraints.maxWidth, height) { placeables.forEach { it.place(0, 0) } }
    }
}

private const val SLOT_MEASURE = "measure"
private const val SLOT_FIT = "fit"
