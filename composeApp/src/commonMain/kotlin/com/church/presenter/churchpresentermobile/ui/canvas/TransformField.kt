package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_section_transform
import churchpresentermobile.composeapp.generated.resources.canvas_skew_horizontal
import churchpresentermobile.composeapp.generated.resources.canvas_skew_vertical
import churchpresentermobile.composeapp.generated.resources.canvas_transform_height
import churchpresentermobile.composeapp.generated.resources.canvas_transform_rotation
import churchpresentermobile.composeapp.generated.resources.canvas_transform_width
import churchpresentermobile.composeapp.generated.resources.canvas_transform_x
import churchpresentermobile.composeapp.generated.resources.canvas_transform_y
import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasToolOptions
import com.church.presenter.churchpresentermobile.model.resizable
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/*
 * Transform: exact numbers for the one selected item — where it is, how big, how far it is
 * turned and, for a shape, how far it leans. Not in the design, so drawn in its panel style.
 */

/** A number the Transform section edits. */
internal enum class TransformField { X, Y, WIDTH, HEIGHT, ROTATION }

/**
 * The edit typing [value] into [field] makes to [element], whose own upright box is [box] —
 * or null when there is nothing to do: the value is what is there already, or the element
 * sizes itself. Each commit is its own [session], so each is one step on the undo history.
 */
internal fun transformEdit(
    element: CanvasElement,
    box: CanvasBounds,
    field: TransformField,
    value: Float,
    session: Int,
): CanvasIntent? = when (field) {
    TransformField.X -> (value - box.left).takeIf { it != 0f }
        ?.let { CanvasIntent.Move(listOf(element.id), it, 0f, session) }
    TransformField.Y -> (value - box.top).takeIf { it != 0f }
        ?.let { CanvasIntent.Move(listOf(element.id), 0f, it, session) }
    TransformField.WIDTH -> resizedTo(element, box, value.coerceAtLeast(MIN_SIDE), box.height, session)
    TransformField.HEIGHT -> resizedTo(element, box, box.width, value.coerceAtLeast(MIN_SIDE), session)
    TransformField.ROTATION -> normalisedAngle(value).takeIf { it != normalisedAngle(element.angle) }
        ?.let { CanvasIntent.Rotate(element.id, it, session) }
}

private fun resizedTo(element: CanvasElement, box: CanvasBounds, width: Float, height: Float, session: Int) =
    if (!resizable(element) || (width == box.width && height == box.height)) {
        null
    } else {
        CanvasIntent.Resize(element.id, CanvasBounds(box.left, box.top, box.left + width, box.top + height), session)
    }

/** [degrees] as the canvas keeps a turn: 0 up to, but not including, 360. */
internal fun normalisedAngle(degrees: Float): Float = ((degrees % FULL_TURN) + FULL_TURN) % FULL_TURN

/** The least a side can be typed down to, in dp — anything smaller cannot be grabbed again. */
private const val MIN_SIDE = 8f
private const val FULL_TURN = 360f

/**
 * The Transform section for [element], whose upright box is [box]. Width and height are
 * read-only for what sizes itself — text and ink. A shape adds its lean, through the same
 * shape options the Shapes panel edits, so the two can never disagree.
 */
@Composable
internal fun TransformSection(
    element: CanvasElement,
    box: CanvasBounds,
    options: CanvasToolOptions,
    onIntent: (CanvasIntent) -> Unit,
) {
    val commit: (TransformField, Float) -> Unit = { field, value ->
        transformEdit(element, box, field, value, GestureSessions.next())?.let(onIntent)
    }
    val sized = resizable(element)
    PanelSectionLabel(stringResource(Res.string.canvas_section_transform))
    Column(verticalArrangement = Arrangement.spacedBy(FIELD_GAP)) {
        Row(horizontalArrangement = Arrangement.spacedBy(FIELD_GAP)) {
            NumberField(stringResource(Res.string.canvas_transform_x), box.left, CanvasTags.TRANSFORM_X) {
                commit(TransformField.X, it)
            }
            NumberField(stringResource(Res.string.canvas_transform_y), box.top, CanvasTags.TRANSFORM_Y) {
                commit(TransformField.Y, it)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FIELD_GAP)) {
            val width = stringResource(Res.string.canvas_transform_width)
            val height = stringResource(Res.string.canvas_transform_height)
            NumberField(width, box.width, CanvasTags.TRANSFORM_WIDTH, sized) { commit(TransformField.WIDTH, it) }
            NumberField(height, box.height, CanvasTags.TRANSFORM_HEIGHT, sized) { commit(TransformField.HEIGHT, it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FIELD_GAP)) {
            NumberField(
                stringResource(Res.string.canvas_transform_rotation),
                element.angle,
                CanvasTags.TRANSFORM_ROTATION,
                suffix = DEGREES,
            ) { commit(TransformField.ROTATION, it) }
        }
        if (element is CanvasElement.Shape) {
            val shapes = options.shapes
            Row(horizontalArrangement = Arrangement.spacedBy(FIELD_GAP)) {
                NumberField(
                    stringResource(Res.string.canvas_skew_horizontal),
                    element.skewX,
                    CanvasTags.TRANSFORM_SKEW_X,
                    suffix = DEGREES,
                ) { onIntent(CanvasIntent.SetOptions(options.copy(shapes = shapes.copy(skewX = it.coerceIn(SKEW))))) }
                NumberField(
                    stringResource(Res.string.canvas_skew_vertical),
                    element.skewY,
                    CanvasTags.TRANSFORM_SKEW_Y,
                    suffix = DEGREES,
                ) { onIntent(CanvasIntent.SetOptions(options.copy(shapes = shapes.copy(skewY = it.coerceIn(SKEW))))) }
            }
        }
    }
}

/**
 * A labelled whole number in a field the size of a chip. What is typed is kept until it is
 * committed — Done on the keyboard, or leaving the field — and anything that is not a number
 * puts the field back to the value it shows.
 */
@Composable
private fun RowScope.NumberField(
    label: String,
    value: Float,
    tag: String,
    enabled: Boolean = true,
    suffix: String = "",
    onCommit: (Float) -> Unit,
) {
    val colors = canvasColors
    val shown = value.roundToInt().toString()
    var typed by remember(shown) { mutableStateOf(shown) }
    val send = {
        typed.trim().toFloatOrNull()?.takeIf { typed.trim() != shown }?.let(onCommit)
        typed = shown
    }
    val shape = RoundedCornerShape(9.dp)
    Row(
        modifier = Modifier
            .weight(1f)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(shape)
            .background(colors.control)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        BasicTextField(
            value = typed,
            onValueChange = { typed = it },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            ),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { send() }),
            modifier = Modifier
                .weight(1f)
                .testTag(tag)
                .onFocusChanged { if (!it.isFocused && typed != shown) send() },
        )
        if (suffix.isNotEmpty()) Text(suffix, color = colors.muted, fontSize = 12.sp)
    }
}

private val FIELD_GAP = 8.dp
private const val DEGREES = "°"
