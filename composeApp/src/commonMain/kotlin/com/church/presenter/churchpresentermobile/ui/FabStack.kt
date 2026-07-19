package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.cd_cast
import churchpresentermobile.composeapp.generated.resources.cd_select
import churchpresentermobile.composeapp.generated.resources.label_add_to_schedule
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * Vertical FAB stack shown bottom-right on content screens (Songs detail,
 * Bible verses, Media, Presentation). Order top→bottom:
 *   1. Select (multi-select) — elevated neutral surface, checklist icon.
 *   2. Add to schedule — amber, list+plus icon (primary queue action).
 *   3. Cast / Project — accent, monitor+wifi icon, with a red count badge.
 *
 * Each action is optional; pass null to hide that FAB.
 */
@Composable
fun FabStack(
    modifier: Modifier = Modifier,
    onSelect: (() -> Unit)? = null,
    onAddToSchedule: (() -> Unit)? = null,
    onCast: (() -> Unit)? = null,
    castBadgeCount: Int = 0,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        onSelect?.let {
            SquareFab(
                icon = Icons.Outlined.Checklist,
                contentDescription = stringResource(Res.string.cd_select),
                containerColor = colors.surfaceElevated,
                iconColor = colors.accent,
                shadowColor = if (colors.isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.18f),
                onClick = it,
            )
        }
        onAddToSchedule?.let {
            SquareFab(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = stringResource(Res.string.label_add_to_schedule),
                containerColor = colors.amber,
                iconColor = colors.amberStroke,
                shadowColor = colors.amber.copy(alpha = if (colors.isDark) 0.4f else 0.45f),
                onClick = it,
            )
        }
        onCast?.let {
            Box {
                SquareFab(
                    icon = Icons.Filled.Cast,
                    contentDescription = stringResource(Res.string.cd_cast),
                    containerColor = colors.accent,
                    iconColor = colors.onAccent,
                    shadowColor = colors.accent.copy(alpha = if (colors.isDark) 0.35f else 0.3f),
                    onClick = it,
                )
                if (castBadgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
                            .background(colors.background, CircleShape) // 2px border effect via padding
                            .padding(2.dp)
                            .background(colors.danger, CircleShape)
                            .padding(horizontal = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = castBadgeCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/** 50×50 rounded-square FAB with a colored drop shadow — the redesign's FAB shape. */
@Composable
fun SquareFab(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    iconColor: Color,
    shadowColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppDimens.radiusFab)
    Box(
        modifier = modifier
            .size(AppDimens.fabSize)
            .shadow(elevation = 14.dp, shape = shape, spotColor = shadowColor, ambientColor = shadowColor)
            .clip(shape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
    }
}
