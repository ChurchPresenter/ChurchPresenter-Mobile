package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
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
 * What is drawn here is recorded into a layer on its [paper] color: the elements
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
    // Read as state inside the draw, so going live redraws the board and records it. Read as a
    // plain value, nothing redrew the board when Go Live was pressed on an iPad — only the top
    // bar changed — so a page drawn before going live went out as an empty layer: a blank screen.
    val recording by rememberUpdatedState(live)
    // How many times the page has been recorded. Not snapshot state: a draw must not write any.
    val recordings = remember { IntArray(1) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                if (recording) {
                    layer.record {
                        drawRect(paper)
                        this@drawWithContent.drawContent()
                    }
                    recordings[0]++
                }
                drawContent()
            },
    ) { content() }
    LaunchedEffect(live, changes, paper) {
        if (!live) return@LaunchedEffect
        val before = recordings[0]
        // Long enough for a flurry of changes to settle into one picture.
        delay(FRAME_DELAY_MS)
        // And never before the page has been drawn into the layer since this change.
        var waited = 0
        while (recordings[0] == before && waited < MAX_FRAMES_TO_WAIT) {
            withFrameNanos { }
            waited++
        }
        frame(layer.toImageBitmap())
    }
}

private const val FRAME_DELAY_MS = 150L

/** How many frames to wait for the page to be recorded before sending what there is. */
private const val MAX_FRAMES_TO_WAIT = 10

/** [this] as `#RRGGBB`, the form a slide carries colors in. */
internal fun Color.toHex(): String {
    val rgb = toArgb() and RGB_MASK
    return "#" + rgb.toString(HEX).uppercase().padStart(HEX_DIGITS, '0')
}

private const val RGB_MASK = 0xFFFFFF
private const val HEX = 16
private const val HEX_DIGITS = 6
