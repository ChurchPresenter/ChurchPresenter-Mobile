package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopAccessDisabled
import androidx.compose.material.icons.filled.Pause
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_clear_display
import churchpresentermobile.composeapp.generated.resources.action_hold_display
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * Shared FAB stack shown in the bottom-right corner of every content screen,
 * styled to the redesign (see [FabStack]).
 *
 * Standard stack, top → bottom:
 *   [extraLeadingContent] — optional screen-specific buttons (e.g. photo picker)
 *   [Clear Display]       — danger FAB, only while projecting
 *   [Hold / Freeze]       — neutral FAB, only while projecting
 *   [Select]              — elevated neutral, shown only when [onToggleMultiSelect] is provided
 *   [Add to Schedule]     — amber (primary queue action)
 *   [Cast / Project]      — accent, with [castBadgeCount] red badge
 */
@Composable
fun ContentActionButtons(
    isProjecting: Boolean,
    scheduleAdded: Boolean,
    onToggleProjecting: () -> Unit,
    /** Null hides the schedule button — standalone has no desktop schedule. */
    onAddToSchedule: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    castBadgeCount: Int = 0,
    isHolding: Boolean = false,
    onToggleHold: (() -> Unit)? = null,
    onClearDisplay: (() -> Unit)? = null,
    isMultiSelectMode: Boolean = false,
    onToggleMultiSelect: (() -> Unit)? = null,
    extraLeadingContent: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier.padding(end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Screen-specific extra buttons (e.g. photo picker) ─────────────
        extraLeadingContent()

        // ── Clear Display (danger) — only while projecting ────────────────
        if (onClearDisplay != null && isProjecting) {
            SquareFab(
                icon = Icons.Filled.DesktopAccessDisabled,
                contentDescription = stringResource(Res.string.action_clear_display),
                containerColor = colors.danger,
                iconColor = Color.White,
                shadowColor = colors.danger.copy(alpha = 0.4f),
                onClick = onClearDisplay,
            )
        }

        // ── Hold / Freeze — only while projecting ─────────────────────────
        if (onToggleHold != null && isProjecting) {
            SquareFab(
                icon = Icons.Filled.Pause,
                contentDescription = stringResource(Res.string.action_hold_display),
                containerColor = if (isHolding) colors.danger else colors.surfaceElevated,
                iconColor = if (isHolding) Color.White else colors.secondary,
                shadowColor = if (isHolding) colors.danger.copy(alpha = 0.4f)
                else if (colors.isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.18f),
                onClick = onToggleHold,
            )
        }

        // ── Select / Add / Cast — the design FAB stack ────────────────────
        FabStack(
            onSelect = onToggleMultiSelect,
            onAddToSchedule = onAddToSchedule,
            onCast = onToggleProjecting,
            castBadgeCount = castBadgeCount,
        )
    }
}
