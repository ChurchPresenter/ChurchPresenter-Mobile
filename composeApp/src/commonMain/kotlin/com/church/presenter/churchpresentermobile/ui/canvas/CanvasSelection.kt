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
import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.resizable
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState
import com.church.presenter.churchpresentermobile.viewmodel.SelectionAction
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The selection's frame — an accent outline round everything selected, eight
 * square handles and a rotate knob above — with the bar of what can be done to
 * it underneath. The bar steps aside while a text box is typed into.
 *
 * The handles and knob are drawn only on a single element they work on, and the
 * select tool's gestures ([SelectGestures]) do the dragging and turning.
 */
@Composable
internal fun SelectionOverlay(state: CanvasUiState, bounds: CanvasBounds, onIntent: (CanvasIntent) -> Unit) {
    val colors = canvasColors
    Box(
        modifier = Modifier
            .offset(bounds.left.dp, bounds.top.dp)
            .size(bounds.width.dp, bounds.height.dp)
            .border(2.dp, colors.accent, RoundedCornerShape(4.dp)),
    )
    // Handles only where they do something: resizing a single picture, shape or note,
    // and the knob, which turns a picture a quarter at a time.
    val single = state.selected?.takeIf { !it.locked }
    if (single is CanvasElement.Image) {
        Box(
            Modifier
                .offset((bounds.centreX - 1).dp, (bounds.top - STALK_TOP).dp)
                .size(2.dp, STALK_LENGTH.dp)
                .background(colors.accent),
        )
        Handle(bounds.centreX, bounds.top - KNOB_CENTRE, KNOB.dp, CircleShape)
    }
    if (single != null && resizable(single)) {
        handlePoints(bounds).forEach { (x, y) -> Handle(x, y, HANDLE.dp, RoundedCornerShape(4.dp)) }
    }
    if (state.editingId == null) {
        SelectionBar(state, x = bounds.left, y = bounds.bottom + BAR_GAP, onIntent = onIntent)
    }
}

/** The stalk runs from this far above the frame… */
private const val STALK_TOP = 34f
private const val STALK_LENGTH = 22f
/** …to the knob, centred this far above it. */
internal const val KNOB_CENTRE = 37f
private const val KNOB = 18f
private const val HANDLE = 14f
private const val BAR_GAP = 14f

private val CanvasBounds.centreX: Float get() = (left + right) / 2
private val CanvasBounds.centreY: Float get() = (top + bottom) / 2

/** The four corners and four edge midpoints of [bounds]. */
internal fun handlePoints(bounds: CanvasBounds): List<Pair<Float, Float>> = listOf(
    bounds.left to bounds.top,
    bounds.centreX to bounds.top,
    bounds.right to bounds.top,
    bounds.right to bounds.centreY,
    bounds.right to bounds.bottom,
    bounds.centreX to bounds.bottom,
    bounds.left to bounds.bottom,
    bounds.left to bounds.centreY,
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
 * mean something for one picture, so anything else goes without them. Lock reads
 * Unlock once everything selected is locked.
 */
@Composable
private fun SelectionBar(state: CanvasUiState, x: Float, y: Float, onIntent: (CanvasIntent) -> Unit) {
    val colors = canvasColors
    val shape = RoundedCornerShape(12.dp)
    val image = state.selected is CanvasElement.Image
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
            if (state.selection.all { it.locked }) Res.string.canvas_unlock else Res.string.canvas_lock,
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
