package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.church.presenter.churchpresentermobile.ui.standalone.parseHexColor
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors

internal val CardShape = RoundedCornerShape(14.dp)
internal val ChipShape = RoundedCornerShape(20.dp)
internal val ButtonShape = RoundedCornerShape(13.dp)
internal val ButtonHeight: Dp = 48.dp
internal val PagePadding: Dp = 16.dp

/** A small caps label over a group, optionally with a count on the right. */
@Composable
internal fun CalendarOverline(label: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val colors = LocalAppColors.current
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label.uppercase(),
            color = colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.06.em,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) Text(trailing, color = colors.muted, fontSize = 12.sp)
    }
}

/** A pill that is either on or off — the filter tabs and the timing chips. */
@Composable
internal fun CalendarChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dim: Boolean = false,
) {
    val colors = LocalAppColors.current
    val fill = when {
        selected && !dim -> colors.accent
        selected -> colors.accentTint
        else -> colors.surface
    }
    val stroke = if (selected) colors.accent else colors.borderSubtle
    val text = when {
        selected && !dim -> colors.onAccent
        selected -> colors.accent
        else -> colors.secondary
    }
    Box(
        modifier = modifier
            .clip(ChipShape)
            .background(fill)
            .border(1.dp, stroke, ChipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        Text(label, color = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** A bordered surface card, the row and list-item shape everywhere in the planner. */
@Composable
internal fun CalendarCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(if (selected) colors.accentTint else colors.surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) colors.accent else colors.borderSubtle, CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** The accent-filled bar that is a screen's one primary action. */
@Composable
internal fun CalendarPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .height(ButtonHeight)
            .clip(ButtonShape)
            .background(if (enabled) colors.accent else colors.dim)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(18.dp))
        Text(label, color = colors.onAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** A bordered surface button for the secondary action beside a primary one. */
private const val DISABLED_ALPHA = 0.45f

/** A tile's corner is a third of its side, which keeps the squircle looking the same at any size. */
private const val CORNER_OF_SIZE = 3

@Composable
internal fun CalendarSecondaryButton(
    label: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .height(ButtonHeight)
            .clip(ButtonShape)
            .background(colors.surface)
            .border(1.dp, colors.border, ButtonShape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = if (label == null) 15.dp else 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = colors.text,
                modifier = Modifier.size(18.dp),
            )
        }
        if (label != null) {
            Text(label, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** A row's kind, as a tinted square icon. */
@Composable
internal fun KindBadge(kind: String, modifier: Modifier = Modifier, size: Dp = 30.dp) {
    val look = kindLook(kind)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / CORNER_OF_SIZE))
            .background(look.tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(look.icon, contentDescription = null, tint = look.tint, modifier = Modifier.size(size / 2 + 1.dp))
    }
}

/** A small round swatch, for service types and section colors. */
@Composable
internal fun ColorDot(hex: String, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    val colors = LocalAppColors.current
    Box(modifier = modifier.size(size).clip(RoundedCornerShape(size)).background(parseHexColor(hex, colors.accent)))
}

@Composable
internal fun colorOf(hex: String): Color = parseHexColor(hex, LocalAppColors.current.accent)

@Composable
internal fun TitleText(text: String, modifier: Modifier = Modifier, size: Int = 15) {
    Text(
        text = text,
        color = LocalAppColors.current.text,
        fontSize = size.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
internal fun MutedText(text: String, modifier: Modifier = Modifier, size: Int = 12, maxLines: Int = 1) {
    Text(
        text = text,
        color = LocalAppColors.current.muted,
        fontSize = size.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
