package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.cd_close
import churchpresentermobile.composeapp.generated.resources.label_live
import churchpresentermobile.composeapp.generated.resources.schedule_drawer_empty
import churchpresentermobile.composeapp.generated.resources.schedule_drawer_error
import churchpresentermobile.composeapp.generated.resources.schedule_drawer_item_count
import churchpresentermobile.composeapp.generated.resources.schedule_drawer_title
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ScheduleItem
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource
import com.church.presenter.churchpresentermobile.viewmodel.ScheduleViewModel

/**
 * Side-drawer content that displays the service schedule loaded from /api/schedule.
 * Restyled to the redesign: 320-wide drawer, header w/ subtitle + close, type-colored
 * icon tiles, active item marked with an accent left-border + "Live" marker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDrawerContent(
    appSettings: AppSettings,
    isDemoMode: Boolean = false,
    settingsSaveToken: Int,
    scheduleRefreshToken: Int = 0,
    onItemClick: (ScheduleItem) -> Unit = {},
    onClose: () -> Unit = {},
    providedViewModel: ScheduleViewModel? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val viewModel: ScheduleViewModel = providedViewModel
        ?: viewModel(key = isDemoMode.toString()) { ScheduleViewModel(appSettings, ServerEventService(appSettings), isDemoMode) }

    LaunchedEffect(settingsSaveToken) {
        if (settingsSaveToken > 0) viewModel.onSettingsSaved()
    }

    LaunchedEffect(scheduleRefreshToken) {
        if (scheduleRefreshToken > 0) viewModel.loadSchedule()
    }

    val allItems by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val visibleItems = remember(allItems) {
        allItems.filter { item ->
            val type = item.type?.lowercase() ?: return@filter false
            // Show every schedule item except the ones the mobile app can't drive:
            // lower-thirds and canvas/scene items.
            !type.contains("lower") && !type.contains("scene") && !type.contains("canvas")
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(colors.background)
    ) {
        // ── Drawer header ─────────────────────────────────────────────────
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
                    color = colors.text
                )
                Text(
                    text = stringResource(Res.string.schedule_drawer_item_count, visibleItems.size),
                    fontSize = 11.sp,
                    color = colors.muted
                )
            }
            IconTileButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(Res.string.cd_close),
                tint = colors.muted,
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            )
        }

        HorizontalDivider(color = colors.borderSubtle)

        // ── Error banner ──────────────────────────────────────────────────
        val currentError = error
        if (currentError != null) {
            Text(
                text = stringResource(Res.string.schedule_drawer_error, currentError),
                color = colors.danger,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // ── Body ──────────────────────────────────────────────────────────
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadSchedule() },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when {
                visibleItems.isNotEmpty() -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        // Pad the bottom so the last item clears the system navigation bar.
                        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                        modifier = Modifier.fillMaxSize().verticalScrollbar(listState)
                    ) {
                        items(visibleItems) { item ->
                            ScheduleItemRow(item = item, onClick = { onItemClick(item) })
                        }
                    }
                }
                !isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.schedule_drawer_empty),
                            color = colors.muted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private data class ScheduleTypeStyle(val icon: ImageVector, val fg: Color, val bg: Color)

@Composable
private fun scheduleTypeStyleFor(type: String?): ScheduleTypeStyle {
    val colors = LocalAppColors.current
    val t = type?.lowercase() ?: ""
    return when {
        t.contains("song") -> ScheduleTypeStyle(Icons.Outlined.MusicNote, colors.scheduleSongFg, colors.scheduleSongBg)
        t.contains("bible") -> ScheduleTypeStyle(Icons.AutoMirrored.Outlined.MenuBook, colors.scheduleBibleFg, colors.scheduleBibleBg)
        t.contains("picture") || t.contains("image") -> ScheduleTypeStyle(Icons.Outlined.Image, colors.schedulePictureFg, colors.schedulePictureBg)
        t.contains("dictionary") -> ScheduleTypeStyle(Icons.Outlined.Translate, colors.accent, colors.accentTint)
        t.contains("announcement") -> ScheduleTypeStyle(Icons.Outlined.Campaign, colors.accent, colors.accentTint)
        t.contains("website") || t.contains("web") -> ScheduleTypeStyle(Icons.Outlined.Public, colors.accent, colors.accentTint)
        t.contains("media") -> ScheduleTypeStyle(Icons.Outlined.PlayCircleOutline, colors.accent, colors.accentTint)
        else -> ScheduleTypeStyle(Icons.Outlined.DesktopWindows, colors.muted, colors.inputBg)
    }
}

@Composable
private fun ScheduleItemRow(item: ScheduleItem, onClick: () -> Unit = {}) {
    val colors = LocalAppColors.current
    val style = scheduleTypeStyleFor(item.type)
    val leftBorder = if (item.active) colors.accent else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (item.active) colors.accentTint else colors.background)
            .drawBehind {
                drawRect(color = leftBorder, size = Size(3.dp.toPx(), size.height))
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        // Type icon tile
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(style.bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = style.icon, contentDescription = null, tint = style.fg, modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = colors.text
            )
            val subtitle = item.details?.takeIf { it.isNotBlank() }
                ?: item.type?.lowercase()?.replaceFirstChar { it.uppercaseChar() }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.muted
                )
            }
        }

        // Active "Live" marker
        if (item.active) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.accent))
                Text(stringResource(Res.string.label_live), color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
