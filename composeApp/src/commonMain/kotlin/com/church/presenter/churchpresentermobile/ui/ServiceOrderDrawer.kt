package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_clear
import churchpresentermobile.composeapp.generated.resources.announcements_title
import churchpresentermobile.composeapp.generated.resources.cd_close
import churchpresentermobile.composeapp.generated.resources.cd_delete
import churchpresentermobile.composeapp.generated.resources.schedule_drawer_empty
import churchpresentermobile.composeapp.generated.resources.schedule_drawer_item_count
import churchpresentermobile.composeapp.generated.resources.schedule_drawer_title
import churchpresentermobile.composeapp.generated.resources.service_order_move_down
import churchpresentermobile.composeapp.generated.resources.service_order_move_up
import churchpresentermobile.composeapp.generated.resources.tab_bible
import churchpresentermobile.composeapp.generated.resources.tab_songs
import com.church.presenter.churchpresentermobile.model.LocalSetlistEntry
import com.church.presenter.churchpresentermobile.model.SetlistEntryType
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The standalone running order, in the same drawer remote uses for the
 * desktop's schedule.
 *
 * Same gesture, same place, same shape — the operator should not have to learn
 * a second idea of "what we're doing today" just because the phone is doing the
 * presenting. What differs is that this list is editable here: it is this
 * device's list, so removing and reordering belong to the drawer rather than to
 * a machine somewhere else.
 *
 * @param entries The running order, in order.
 * @param liveTitle The title of what is on the screens now, or null; its row is
 *   marked the way the remote drawer marks the desktop's active item.
 * @param onItemClick Opens the entry on the tab that owns it.
 */
@Composable
fun ServiceOrderDrawerContent(
    entries: List<LocalSetlistEntry>,
    liveTitle: String? = null,
    onItemClick: (LocalSetlistEntry) -> Unit = {},
    onMove: (from: Int, to: Int) -> Unit = { _, _ -> },
    onRemove: (index: Int) -> Unit = {},
    onClear: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.schedule_drawer_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                )
                Text(
                    text = stringResource(Res.string.schedule_drawer_item_count, entries.size),
                    fontSize = 11.sp,
                    color = colors.muted,
                    modifier = Modifier.testTag(UiTags.DRAWER_COUNT),
                )
            }
            if (entries.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.action_clear),
                    fontSize = 12.sp,
                    color = colors.muted,
                    modifier = Modifier
                        .testTag(UiTags.DRAWER_CLEAR)
                        .clickable(onClick = onClear)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            IconTileButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(Res.string.cd_close),
                tint = colors.muted,
                onClick = onClose,
                modifier = Modifier.size(32.dp).testTag(UiTags.DRAWER_CLOSE),
            )
        }

        HorizontalDivider(color = colors.borderSubtle)

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.schedule_drawer_empty),
                    color = colors.muted,
                    fontSize = 14.sp,
                    modifier = Modifier.testTag(UiTags.DRAWER_EMPTY),
                )
            }
            return@Column
        }

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScrollbar(listState),
        ) {
            items(entries.size) { index ->
                ServiceOrderRow(
                    entry = entries[index],
                    index = index,
                    isFirst = index == 0,
                    isLast = index == entries.lastIndex,
                    isLive = entries[index].title == liveTitle,
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
    isLive: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalAppColors.current
    val style = styleFor(entry.type)
    val leftBorder = if (isLive) colors.accent else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTags.orderRow(index))
            .background(if (isLive) colors.accentTint else colors.background)
            .drawBehind { drawRect(color = leftBorder, size = Size(3.dp.toPx(), size.height)) }
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(style.bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.fg,
                modifier = Modifier.size(18.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = colors.text,
            )
            Text(
                text = stringResource(style.label),
                fontSize = 11.sp,
                maxLines = 1,
                color = if (isLive) colors.accent else colors.muted,
            )
        }

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

/** The type tile and label, in the same colours the remote drawer gives each kind of item. */
private class EntryStyle(val icon: ImageVector, val fg: Color, val bg: Color, val label: StringResource)

@Composable
private fun styleFor(type: SetlistEntryType): EntryStyle {
    val colors = LocalAppColors.current
    return when (type) {
        SetlistEntryType.SONG ->
            EntryStyle(Icons.Outlined.MusicNote, colors.scheduleSongFg, colors.scheduleSongBg, Res.string.tab_songs)
        SetlistEntryType.BIBLE -> EntryStyle(
            Icons.AutoMirrored.Outlined.MenuBook, colors.scheduleBibleFg, colors.scheduleBibleBg, Res.string.tab_bible,
        )
        SetlistEntryType.ANNOUNCEMENT ->
            EntryStyle(Icons.Outlined.Campaign, colors.accent, colors.accentTint, Res.string.announcements_title)
    }
}
