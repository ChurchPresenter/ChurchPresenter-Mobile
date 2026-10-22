package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_type_here
import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasFont
import com.church.presenter.churchpresentermobile.model.CanvasTextAlign
import com.church.presenter.churchpresentermobile.model.CanvasTextStyle
import com.church.presenter.churchpresentermobile.model.TextOptions
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import org.jetbrains.compose.resources.stringResource

/*
 * Text on the board: a text box in its own format or the heading and body looks,
 * and the field a box or sticky note turns into while it is typed into.
 */

@Composable
internal fun TextElement(
    element: CanvasElement.Text,
    editing: Boolean,
    onIntent: (CanvasIntent) -> Unit,
    onTextBounds: (String, CanvasBounds) -> Unit,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val style = textStyleFor(element)
    val highlight = element.format?.highlight == true
    val measured = modifier
        .offset(element.x.dp, element.y.dp)
        .then(if (highlight) Modifier.background(TextHighlight, RoundedCornerShape(4.dp)) else Modifier)
        .onSizeChanged { size ->
            with(density) {
                onTextBounds(
                    element.id,
                    CanvasBounds(
                        element.x,
                        element.y,
                        element.x + size.width.toDp().value,
                        element.y + size.height.toDp().value,
                    ),
                )
            }
        }
    if (editing) {
        EditableText(element.id, element.text, style, onIntent, measured.widthIn(min = 80.dp))
    } else {
        val bulleted = element.format?.bulleted == true
        Text(
            text = if (bulleted) element.text.lines().joinToString("\n") { "• $it" } else element.text,
            style = style,
            modifier = measured,
        )
    }
}

/** The look of a text box: its own format if the text tool typed it, otherwise the heading or body style. */
@Composable
private fun textStyleFor(element: CanvasElement.Text): TextStyle {
    val colors = canvasColors
    val format = element.format ?: return if (element.style == CanvasTextStyle.TITLE) {
        TextStyle(
            color = colors.text,
            fontSize = 54.sp,
            lineHeight = 54.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Serif,
            letterSpacing = (-0.02).em,
        )
    } else {
        TextStyle(color = colors.secondary, fontSize = 22.sp, lineHeight = 33.sp, fontWeight = FontWeight.Medium)
    }
    return formatStyle(format, colors.inkColor(format.ink, format.custom))
}

private fun formatStyle(format: TextOptions, color: Color): TextStyle = TextStyle(
    color = color,
    fontSize = format.size.sp,
    lineHeight = (format.size * LINE_HEIGHT).sp,
    fontWeight = if (format.bold) FontWeight.Bold else FontWeight.Normal,
    fontStyle = if (format.italic) FontStyle.Italic else FontStyle.Normal,
    fontFamily = fontFamily(format.font),
    textDecoration = TextDecoration.combine(
        listOfNotNull(
            TextDecoration.Underline.takeIf { format.underline },
            TextDecoration.LineThrough.takeIf { format.strikethrough },
        ),
    ),
    textAlign = when (format.align) {
        CanvasTextAlign.LEFT -> TextAlign.Left
        CanvasTextAlign.CENTER -> TextAlign.Center
        CanvasTextAlign.RIGHT -> TextAlign.Right
    },
)

private const val LINE_HEIGHT = 1.25f

/** Background highlight behind typed text: the highlighter's yellow, soft enough to read through. */
private val TextHighlight = Color(0x66FDE047)

internal fun fontFamily(font: CanvasFont): FontFamily = when (font) {
    CanvasFont.SERIF -> FontFamily.Serif
    CanvasFont.SANS -> FontFamily.SansSerif
    CanvasFont.MONO -> FontFamily.Monospace
    CanvasFont.HANDWRITING -> FontFamily.Cursive
}

/**
 * The field a text box or note becomes while it is typed into: focused as soon as
 * it appears, with Done on the keyboard to stop.
 */
@Composable
internal fun EditableText(
    id: String,
    text: String,
    style: TextStyle,
    onIntent: (CanvasIntent) -> Unit,
    modifier: Modifier,
) {
    val colors = canvasColors
    val focus = remember { FocusRequester() }
    LaunchedEffect(id) { focus.requestFocus() }
    BasicTextField(
        value = text,
        onValueChange = { onIntent(CanvasIntent.EditText(id, it)) },
        textStyle = style,
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onIntent(CanvasIntent.FinishEditing) }),
        modifier = modifier.focusRequester(focus),
        decorationBox = { field ->
            Box {
                if (text.isEmpty()) {
                    Text(stringResource(Res.string.canvas_type_here), style = style.copy(color = colors.muted))
                }
                field()
            }
        },
    )
}
