package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.delay

/**
 * The page's elements, and — while [live] — a picture of them for the outputs.
 *
 * What is drawn here is recorded into a layer on its [paper] colour: the elements
 * alone, without the dot grid behind them or the selection, handles and previews
 * drawn over them, which are the operator's and not the audience's. Recording
 * happens only while live, so a canvas nobody is watching costs nothing extra.
 *
 * A frame is taken [FRAME_DELAY_MS] after the page last changed ([changes]), so a
 * flurry of strokes or keystrokes sends one picture, not one per touch.
 */
@Composable
internal fun CapturedPage(
    live: Boolean,
    paper: Color,
    changes: Any,
    onFrame: (ImageBitmap) -> Unit,
    content: @Composable () -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val frame by rememberUpdatedState(onFrame)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                if (live) {
                    layer.record {
                        drawRect(paper)
                        this@drawWithContent.drawContent()
                    }
                }
                drawContent()
            },
    ) { content() }
    LaunchedEffect(live, changes, paper) {
        if (!live) return@LaunchedEffect
        // Long enough for the change to have been drawn into the layer above.
        delay(FRAME_DELAY_MS)
        frame(layer.toImageBitmap())
    }
}

private const val FRAME_DELAY_MS = 150L

/** [this] as `#RRGGBB`, the form a slide carries colours in. */
internal fun Color.toHex(): String {
    val rgb = toArgb() and RGB_MASK
    return "#" + rgb.toString(HEX).uppercase().padStart(HEX_DIGITS, '0')
}

private const val RGB_MASK = 0xFFFFFF
private const val HEX = 16
private const val HEX_DIGITS = 6
