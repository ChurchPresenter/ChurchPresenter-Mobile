package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.ui.theme.AppColors
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors

internal fun AppColors.modeTint(mode: AppMode): Color = when (mode) {
    AppMode.REMOTE -> accent
    AppMode.STANDALONE -> greekAccent
    AppMode.CALENDAR -> amber
}

internal fun Modifier.halo(color: Color, radius: Dp, width: Dp = 4.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(-w / 2, -w / 2),
        size = Size(size.width + w, size.height + w),
        cornerRadius = CornerRadius(radius.toPx() + w / 2),
        style = Stroke(width = w),
    )
}

@Composable
internal fun LogoTile(size: Dp, radius: Dp, crossHeight: Dp) {
    val look = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.radialGradient(listOf(look.accent.copy(alpha = 0.28f), look.accent.copy(alpha = 0.06f))),
            )
            .border(1.dp, look.accent.copy(alpha = 0.25f), RoundedCornerShape(radius)),
        contentAlignment = Alignment.Center,
    ) {
        CrossIcon(
            brush = LocalAppColors.current.crossBrush,
            modifier = Modifier.size(width = crossHeight / 2, height = crossHeight),
        )
    }
}

@Composable
internal fun IconTile(icon: ImageVector, tint: Color, size: Dp, radius: Dp, iconSize: Dp, alpha: Float = 0.15f) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(tint.copy(alpha = alpha)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
internal fun Eyebrow(text: String, color: Color, size: TextUnit = 11.sp, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = size,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.14.em,
        modifier = modifier,
    )
}

@Composable
internal fun GroupLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Eyebrow(text, LocalAppColors.current.muted, size = 12.sp)
        trailing?.invoke()
    }
}

@Composable
internal fun LookCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val look = LocalAppColors.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(look.surface)
            .border(1.dp, look.border, shape)
            .padding(padding),
        content = content,
    )
}

@Composable
internal fun LookDivider() {
    HorizontalDivider(color = LocalAppColors.current.border)
}

@Composable
internal fun TintButton(
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp,
    fontSize: TextUnit = 14.5.sp,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    radius: Dp = 12.dp,
) {
    val look = LocalAppColors.current
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(tint)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = look.onAccent, modifier = Modifier.size(18.dp))
        }
        Text(label, color = look.onAccent, fontSize = fontSize, fontWeight = FontWeight.ExtraBold)
        if (trailing != null) {
            Icon(trailing, contentDescription = null, tint = look.onAccent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun PlainButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp,
    fontSize: TextUnit = 14.5.sp,
    leading: ImageVector? = null,
) {
    val look = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(look.surface)
            .border(1.dp, look.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) Icon(leading, contentDescription = null, tint = look.text, modifier = Modifier.size(18.dp))
        Text(label, color = look.text, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}
