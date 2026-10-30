package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_clear
import churchpresentermobile.composeapp.generated.resources.cd_delete
import churchpresentermobile.composeapp.generated.resources.schedule_drawer_item_count
import churchpresentermobile.composeapp.generated.resources.schedule_empty_body
import churchpresentermobile.composeapp.generated.resources.schedule_empty_title
import churchpresentermobile.composeapp.generated.resources.service_order_move_down
import churchpresentermobile.composeapp.generated.resources.service_order_move_up
import com.church.presenter.churchpresentermobile.model.LocalSetlistEntry
import com.church.presenter.churchpresentermobile.model.SetlistEntryType
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * Standalone's Schedule tab: the running order this device will present.
 *
 * It was a drawer — the remote one, reused — but a drawer opened by swiping
 * right on whichever tab happened to be first fought the pager, and nothing else
 * in the app worked that way. The running order is what a service is *run*
 * from, so it is a tab, and the first one.
 *
 * The list is this device's own, so it is edited in place: reordered, trimmed,
 * cleared. Tapping an entry opens it on the tab that owns it, where its verses
 * go live with a tap.
 *
 * @param entries The running order, in order.
 * @param onItemClick Opens the entry on the tab that owns it.
 */
@Composable
fun ServiceScheduleScreen(
    entries: List<LocalSetlistEntry>,
    onItemClick: (LocalSetlistEntry) -> Unit = {},
    onMove: (from: Int, to: Int) -> Unit = { _, _ -> },
    onRemove: (index: Int) -> Unit = {},
    onClear: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    if (entries.isEmpty()) {
        EmptyState(
            title = stringResource(Res.string.schedule_empty_title),
            body = stringResource(Res.string.schedule_empty_body),
            modifier = modifier.background(colors.background).testTag(UiTags.SCHEDULE_EMPTY),
        )
        return
    }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.schedule_drawer_item_count, entries.size),
                fontSize = 12.sp,
                color = colors.muted,
                modifier = Modifier.weight(1f).testTag(UiTags.SCHEDULE_COUNT),
            )
            Text(
                text = stringResource(Res.string.action_clear),
                fontSize = 12.sp,
                color = colors.muted,
                modifier = Modifier
                    .testTag(UiTags.SCHEDULE_CLEAR)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        HorizontalDivider(color = colors.borderSubtle)

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScrollbar(listState),
        ) {
            items(entries.size) { index ->
                ServiceOrderRow(
                    entry = entries[index],
                    index = index,
                    isFirst = index == 0,
                    isLast = index == entries.lastIndex,
                    onClick = { onItemClick(entries[index]) },
                    onMoveUp = { onMove(index, index - 1) },
                    onMoveDown = { onMove(index, index + 1) },
                    onRemove = { onRemove(index) },
                )
            }
        }
    }
}

@Composable
private fun ServiceOrderRow(
    entry: LocalSetlistEntry,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTags.orderRow(index))
            .background(colors.background)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconFor(entry.type),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(17.dp),
            )
        }

        Text(
            text = entry.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )

        // Reordering by arrows rather than drag: a running order is edited in a
        // hurry, often one-handed, and a mis-drop during a service costs more
        // than the extra tap.
        if (!isFirst) {
            IconTileButton(
                icon = Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(Res.string.service_order_move_up),
                tint = colors.muted,
                onClick = onMoveUp,
                modifier = Modifier.size(28.dp).testTag(UiTags.orderMoveUp(index)),
            )
        }
        if (!isLast) {
            IconTileButton(
                icon = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(Res.string.service_order_move_down),
                tint = colors.muted,
                onClick = onMoveDown,
                modifier = Modifier.size(28.dp).testTag(UiTags.orderMoveDown(index)),
            )
        }
        IconTileButton(
            icon = Icons.Outlined.Delete,
            contentDescription = stringResource(Res.string.cd_delete),
            tint = colors.muted,
            onClick = onRemove,
            modifier = Modifier.size(28.dp).testTag(UiTags.orderRemove(index)),
        )
    }
}

private fun iconFor(type: SetlistEntryType): ImageVector = when (type) {
    SetlistEntryType.SONG -> Icons.Outlined.MusicNote
    SetlistEntryType.BIBLE -> Icons.AutoMirrored.Outlined.MenuBook
    SetlistEntryType.ANNOUNCEMENT -> Icons.Outlined.Campaign
}
