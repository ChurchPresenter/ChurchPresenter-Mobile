package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_bring_to_front
import churchpresentermobile.composeapp.generated.resources.canvas_crop
import churchpresentermobile.composeapp.generated.resources.canvas_delete
import churchpresentermobile.composeapp.generated.resources.canvas_duplicate
import churchpresentermobile.composeapp.generated.resources.canvas_lock
import churchpresentermobile.composeapp.generated.resources.canvas_replace
import churchpresentermobile.composeapp.generated.resources.canvas_unlock
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.SelectionAction
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The selected element's frame — an accent outline, eight square handles and a
 * rotate knob above — with the bar of what can be done to it underneath.
 *
 * The handles are drawn, not yet draggable: resizing and rotating come with the
 * editing pass that follows this UI.
 */
@Composable
internal fun SelectionOverlay(element: CanvasElement, bounds: Rect, onIntent: (CanvasIntent) -> Unit) {
    val colors = canvasColors
    Box(
        modifier = Modifier
            .offset(bounds.left.dp, bounds.top.dp)
            .size(bounds.width.dp, bounds.height.dp)
            .border(2.dp, colors.accent, RoundedCornerShape(4.dp)),
    )
    // The rotate knob: a stalk up from the top edge's middle, a round handle on it.
    Box(
        Modifier
            .offset((bounds.center.x - 1).dp, (bounds.top - STALK_TOP).dp)
            .size(2.dp, STALK_LENGTH.dp)
            .background(colors.accent),
    )
    Handle(bounds.center.x, bounds.top - KNOB_CENTRE, KNOB.dp, CircleShape)
    handlePoints(bounds).forEach { (x, y) -> Handle(x, y, HANDLE.dp, RoundedCornerShape(4.dp)) }
    SelectionBar(element, x = bounds.left, y = bounds.bottom + BAR_GAP, onIntent = onIntent)
}

/** The stalk runs from this far above the frame… */
private const val STALK_TOP = 34f
private const val STALK_LENGTH = 22f
/** …to the knob, centred this far above it. */
private const val KNOB_CENTRE = 37f
private const val KNOB = 18f
private const val HANDLE = 14f
private const val BAR_GAP = 14f

/** The four corners and four edge midpoints of [bounds]. */
private fun handlePoints(bounds: Rect): List<Pair<Float, Float>> = listOf(
    bounds.left to bounds.top,
    bounds.center.x to bounds.top,
    bounds.right to bounds.top,
    bounds.right to bounds.center.y,
    bounds.right to bounds.bottom,
    bounds.center.x to bounds.bottom,
    bounds.left to bounds.bottom,
    bounds.left to bounds.center.y,
)

/** A white handle with an accent edge, centred on ([x], [y]). */
@Composable
private fun Handle(x: Float, y: Float, size: Dp, shape: Shape) {
    val colors = canvasColors
    Box(
        Modifier
            .offset(x.dp - size / 2, y.dp - size / 2)
            .size(size)
            .clip(shape)
            .background(Color.White)
            .border(2.dp, colors.accent, shape),
    )
}

/**
 * Crop, Replace, Bring to front, Duplicate | Lock, Delete. Crop and Replace only
 * mean something for a picture, so other elements go without them.
 */
@Composable
private fun SelectionBar(element: CanvasElement, x: Float, y: Float, onIntent: (CanvasIntent) -> Unit) {
    val colors = canvasColors
    val shape = RoundedCornerShape(12.dp)
    val image = element is CanvasElement.Image
    Row(
        modifier = Modifier
            .testTag(CanvasTags.SELECTION_BAR)
            .offset(x.dp, y.dp)
            .floatingPanel(colors, shape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (image) {
            BarAction(SelectionAction.CROP, Res.string.canvas_crop, onIntent)
            BarAction(SelectionAction.REPLACE, Res.string.canvas_replace, onIntent)
        }
        BarAction(SelectionAction.BRING_TO_FRONT, Res.string.canvas_bring_to_front, onIntent)
        BarAction(SelectionAction.DUPLICATE, Res.string.canvas_duplicate, onIntent)
        Box(Modifier.width(1.dp).height(22.dp).background(colors.divider))
        BarAction(
            SelectionAction.LOCK,
            if (element.locked) Res.string.canvas_unlock else Res.string.canvas_lock,
            onIntent,
        )
        BarAction(SelectionAction.DELETE, Res.string.canvas_delete, onIntent, danger = true)
    }
}

@Composable
private fun BarAction(
    action: SelectionAction,
    label: StringResource,
    onIntent: (CanvasIntent) -> Unit,
    danger: Boolean = false,
) {
    val colors = canvasColors
    Text(
        text = stringResource(label),
        color = if (danger) colors.danger else colors.text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .testTag(CanvasTags.selection(action))
            .clip(RoundedCornerShape(9.dp))
            .clickable(role = Role.Button) { onIntent(CanvasIntent.OnSelection(action)) }
            .padding(horizontal = 13.dp, vertical = 9.dp),
    )
}
