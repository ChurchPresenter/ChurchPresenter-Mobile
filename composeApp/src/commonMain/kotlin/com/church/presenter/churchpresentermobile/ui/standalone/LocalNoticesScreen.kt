package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_clear_display
import churchpresentermobile.composeapp.generated.resources.action_go_live
import churchpresentermobile.composeapp.generated.resources.notices_empty
import churchpresentermobile.composeapp.generated.resources.standalone_no_output
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.ui.OutlineActionButton
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.LocalNoticesViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * A notice from this device's library, put on the audience screen.
 *
 * The remote Announcements screen drives a desktop; this one drives this
 * phone's own outputs. Notices are written and kept in the Library tab, and
 * going live is a deliberate press here — the Library row itself does nothing,
 * so browsing your own content cannot project it by accident.
 */
@Composable
fun LocalNoticesScreen(
    repository: LibraryRepository,
    presenter: StandaloneEngine?,
    hasOutput: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val vm: LocalNoticesViewModel = viewModel(key = "local_notices") {
        LocalNoticesViewModel(repository, presenter)
    }
    val notices by vm.notices.collectAsState()
    val liveId by vm.liveId.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().background(colors.background),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!hasOutput) {
            Text(
                text = stringResource(Res.string.standalone_no_output),
                color = colors.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            )
        }

        if (notices.isEmpty()) {
            Text(
                text = stringResource(Res.string.notices_empty),
                color = colors.muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            items(notices, key = { it.id }) { notice ->
                NoticeRow(
                    title = notice.title.ifBlank { notice.body.lineSequence().first() },
                    subtitle = notice.body.lineSequence().firstOrNull().orEmpty(),
                    isLive = notice.id == liveId,
                    onProject = { vm.project(notice) },
                    onClear = { vm.clear() },
                )
            }
        }
    }
}

@Composable
private fun NoticeRow(
    title: String,
    subtitle: String,
    isLive: Boolean,
    onProject: () -> Unit,
    onClear: () -> Unit,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isLive) colors.surfaceElevated else colors.surface)
            .clickable(onClick = onProject)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Campaign,
                contentDescription = null,
                tint = if (isLive) colors.accent else colors.muted,
                modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = colors.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlineActionButton(
                label = stringResource(Res.string.action_go_live),
                icon = Icons.Filled.Wifi,
                onClick = onProject,
                modifier = Modifier.weight(1f),
            )
            if (isLive) {
                OutlineActionButton(
                    label = stringResource(Res.string.action_clear_display),
                    icon = Icons.Outlined.Delete,
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
