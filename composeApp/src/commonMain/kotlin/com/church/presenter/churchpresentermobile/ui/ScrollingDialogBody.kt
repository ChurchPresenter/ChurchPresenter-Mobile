package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The `text` slot of an `AlertDialog`, made to scroll — with a visible thumb —
 * when it is taller than the room the dialog has.
 *
 * Material 3's `AlertDialog` does not scroll its body: it gives it whatever
 * height is left between the title and the buttons and clips the rest. At a
 * large font scale, in a long translation or on a phone held sideways, that
 * cut the sentence off — or the controls under it — with nothing on screen to
 * say there was more.
 *
 * A gutter is opened on the trailing edge only while the body actually
 * scrolls, so the thumb sits beside the text rather than over it, and a body
 * that fits is laid out exactly as before.
 */
@Composable
fun ScrollingDialogBody(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScrollWithScrollbar(scroll)
            .padding(end = if (scroll.maxValue > 0) DialogScrollbarGutter else 0.dp),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** Room at the trailing edge for the thumb, so it never sits on top of the text. */
private val DialogScrollbarGutter: Dp = 10.dp
