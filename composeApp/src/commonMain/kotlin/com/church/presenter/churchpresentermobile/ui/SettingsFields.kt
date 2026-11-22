package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.cd_hide_key
import churchpresentermobile.composeapp.generated.resources.cd_show_key
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StatusDot(color: Color, size: Dp = 10.dp) {
    Box(
        modifier = Modifier
            .size(size + 8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .padding(4.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
internal fun LabelledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    placeholder: String = "",
    mono: Boolean = false,
    password: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisible: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    hint: String? = null,
    error: String? = null,
) {
    val look = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Column(modifier = modifier.padding(vertical = 12.dp)) {
        Text(label, color = look.secondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(look.inputBg)
                .border(1.5.dp, if (error != null) look.danger else look.border, shape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val style = TextStyle(
                color = look.text,
                fontSize = 15.sp,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = style.copy(color = look.muted), maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = style,
                    cursorBrush = SolidColor(look.accent),
                    visualTransformation = if (password && !passwordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                    modifier = fieldModifier.fillMaxWidth(),
                )
            }
            if (password && onTogglePasswordVisible != null) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(
                        if (passwordVisible) Res.string.cd_hide_key else Res.string.cd_show_key,
                    ),
                    tint = look.muted,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onTogglePasswordVisible)
                        .padding(4.dp),
                )
            }
        }
        when {
            error != null -> Text(
                error,
                color = look.danger,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            hint != null -> Text(
                hint,
                color = look.muted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
internal fun KeyValueRow(label: String, value: String, mono: Boolean = true, modifier: Modifier = Modifier) {
    val look = LocalAppColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = look.secondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(
            value,
            color = look.muted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        )
    }
}

@Composable
internal fun Footnote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = LocalAppColors.current.muted,
        fontSize = 13.5.sp,
        lineHeight = 19.5.sp,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}
