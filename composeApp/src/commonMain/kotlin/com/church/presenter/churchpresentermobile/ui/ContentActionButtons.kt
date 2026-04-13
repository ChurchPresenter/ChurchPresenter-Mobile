package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_add_to_schedule
import churchpresentermobile.composeapp.generated.resources.action_project_to_screen
import churchpresentermobile.composeapp.generated.resources.action_stop_projecting
import org.jetbrains.compose.resources.stringResource

/**
 * Shared FAB column shown in the bottom-right corner of every content screen.
 *
 * Layout (top → bottom):
 *   [extraLeadingContent] — optional screen-specific buttons (e.g. "Pick from Device" in PicturesScreen)
 *   [Add to Schedule FAB] — PlaylistAdd icon; turns primary-filled once the item is added
 *   [Cast FAB]            — Cast icon; turns primary-filled while projecting; shows [castBadgeCount]
 *                           badge when > 0 (used by BibleDetailScreen for multi-verse selection)
 *
 * @param isProjecting         True while content is being projected to the display.
 * @param scheduleAdded        True once the current item has been added to the schedule.
 * @param onToggleProjecting   Called when the user taps the Cast FAB.
 * @param onAddToSchedule      Called when the user taps the Add-to-Schedule FAB.
 * @param modifier             Applied to the outer [Column].
 * @param castBadgeCount       When > 0 a red circle badge with this number is shown on the Cast FAB.
 * @param extraLeadingContent  Optional slot rendered above the two standard FABs. Use this for
 *                             screen-specific actions (e.g. photo picker in PicturesScreen).
 */
@Composable
fun ContentActionButtons(
    isProjecting: Boolean,
    scheduleAdded: Boolean,
    onToggleProjecting: () -> Unit,
    onAddToSchedule: () -> Unit,
    modifier: Modifier = Modifier,
    castBadgeCount: Int = 0,
    extraLeadingContent: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .padding(end = 16.dp, bottom = 72.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Screen-specific extra buttons (e.g. photo picker) ─────────────
        extraLeadingContent()

        // ── Add to Schedule ───────────────────────────────────────────────
        FloatingActionButton(
            onClick = { if (!scheduleAdded) onAddToSchedule() },
            containerColor = if (scheduleAdded)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (scheduleAdded)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = stringResource(Res.string.action_add_to_schedule)
            )
        }

        // ── Cast / Project to Screen ──────────────────────────────────────
        Box {
            FloatingActionButton(
                onClick = { onToggleProjecting() },
                containerColor = if (isProjecting)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isProjecting)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector        = Icons.Filled.Cast,
                    contentDescription = if (isProjecting)
                        stringResource(Res.string.action_stop_projecting)
                    else
                        stringResource(Res.string.action_project_to_screen)
                )
            }

            // Badge — shown when multiple items are selected (e.g. Bible multi-verse)
            if (castBadgeCount > 0) {
                Surface(
                    shape    = CircleShape,
                    color    = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text  = "$castBadgeCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }
}

