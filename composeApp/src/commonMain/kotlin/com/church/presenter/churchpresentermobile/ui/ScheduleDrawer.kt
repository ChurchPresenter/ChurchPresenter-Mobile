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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.schedule_drawer_empty
import churchpresentermobile.composeapp.generated.resources.schedule_drawer_refresh
import churchpresentermobile.composeapp.generated.resources.schedule_drawer_title
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ScheduleItem
import com.church.presenter.churchpresentermobile.viewmodel.ScheduleViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Side-drawer content that displays the service schedule loaded from /api/schedule.
 *
 * @param appSettings        Shared [AppSettings] used to create the [ScheduleViewModel].
 * @param isDemoMode         When true, shows pre-built demo schedule items instead of live API data.
 * @param settingsSaveToken  Incremented when settings are saved; triggers a reload.
 * @param scheduleRefreshToken Incremented whenever a song is added to the schedule from
 *                           another screen; triggers a silent reload.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDrawerContent(
    appSettings: AppSettings,
    isDemoMode: Boolean = false,
    settingsSaveToken: Int,
    scheduleRefreshToken: Int = 0,
    onItemClick: (ScheduleItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: ScheduleViewModel = viewModel(key = isDemoMode.toString()) { ScheduleViewModel(appSettings, isDemoMode) }

    LaunchedEffect(settingsSaveToken) {
        if (settingsSaveToken > 0) viewModel.onSettingsSaved()
    }

    LaunchedEffect(scheduleRefreshToken) {
        if (scheduleRefreshToken > 0) viewModel.loadSchedule()
    }

    val allItems by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Only surface the four content types the app currently supports.
    // Uses contains() rather than exact equality so the filter is tolerant of
    // longer type strings the server may return (e.g. "SongItem", "BibleVerseItem",
    // fully-qualified class names, etc.).
    val visibleItems = remember(allItems) {
        allItems.filter { item ->
            val type = item.type?.lowercase() ?: return@filter false
            type.contains("song") ||
            type.contains("bible") ||
            type.contains("picture") ||
            type.contains("image") ||
            type.contains("presentation")
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // ── Drawer header ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.schedule_drawer_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            IconButton(
                onClick = { viewModel.loadSchedule() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(Res.string.schedule_drawer_refresh),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))

        // ── Error banner ──────────────────────────────────────────────────
        if (error != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
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
                            modifier = Modifier.fillMaxSize().verticalScrollbar(listState)
                        ) {
                            items(visibleItems) { item ->
                                ScheduleItemRow(item = item, onClick = { onItemClick(item) })
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                !isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.schedule_drawer_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single row in the schedule drawer.
 */
@Composable
private fun ScheduleItemRow(item: ScheduleItem, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (item.active) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type icon badge
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (item.active) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.typeIcon, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (item.active) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (item.active)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
            if (!item.details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.details,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (item.active)
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!item.type.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.type.lowercase().replaceFirstChar { it.uppercaseChar() },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.active)
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // Active indicator dot
        if (item.active) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

