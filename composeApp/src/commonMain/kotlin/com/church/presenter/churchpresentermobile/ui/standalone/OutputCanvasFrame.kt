package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * A canvas page filling the audience screen, on its own [paper] colour.
 *
 * Every edit while the canvas is live sends a new picture, and an image view that
 * goes blank while each loads would flash the projector on every stroke. So the
 * frame on screen stays until the next has loaded over it, and only then gives way.
 */
@Composable
internal fun OutputCanvasFrame(url: String, paper: Color, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf<String?>(null) }
    Box(modifier = modifier.fillMaxSize().background(paper)) {
        shown?.let { frame ->
            AsyncImage(
                model = frame,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (url != shown) {
            // Draws nothing until it has loaded, so the frame underneath shows through.
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onSuccess = { shown = url },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** The paper round a frame that names none: black, as every other output's empty screen is. */
internal val DefaultCanvasPaper: Color = Color.Black
