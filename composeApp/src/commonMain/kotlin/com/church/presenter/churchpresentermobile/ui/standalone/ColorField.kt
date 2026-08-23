package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors

/**
 * One colour of the look, as a live swatch beside the hex that produced it.
 *
 * A hex field rather than a wheel: the operator usually arrives with a colour already in mind —
 * the church's own, or one copied off the desktop — and typing six characters beats hunting for
 * it in a gradient square. The swatch is what makes that honest, updating on every keystroke that
 * parses so a wrong colour is seen before it reaches the audience screen.
 *
 * Half-typed input is kept on screen but not published: "#2A1D" is on the way to something, not a
 * colour to project.
 */
@Composable
internal fun ColorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    // Local draft so a half-typed value survives; the parsed ones go up as they arrive.
    var draft by remember(value) { mutableStateOf(value) }
    val parsed = parseHexColorOrNull(draft)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), color = colors.muted, fontSize = 10.sp, letterSpacing = 0.08.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimens.radiusCard))
                .background(colors.surface)
                .padding(horizontal = AppDimens.space12, vertical = 10.dp),
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(parsed ?: Color.Transparent)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
            )
            BasicTextField(
                value = draft,
                onValueChange = { typed ->
                    draft = typed
                    parseHexColorOrNull(typed)?.let { onValueChange(normaliseHex(typed)) }
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.text,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** `#RGB` or `#RRGGBB`, with or without the hash. Null while the operator is still typing. */
internal fun parseHexColorOrNull(hex: String): Color? {
    val cleaned = hex.trim().removePrefix("#")
    val expanded = when (cleaned.length) {
        3 -> cleaned.map { "$it$it" }.joinToString("")
        6 -> cleaned
        else -> return null
    }
    if (!expanded.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return runCatching { Color(("FF$expanded").toLong(16)) }.getOrNull()
}

/** What gets stored and sent on the wire, so every renderer sees the same shape. */
internal fun normaliseHex(hex: String): String {
    val cleaned = hex.trim().removePrefix("#")
    val expanded = if (cleaned.length == 3) cleaned.map { "$it$it" }.joinToString("") else cleaned
    return "#${expanded.uppercase()}"
}
