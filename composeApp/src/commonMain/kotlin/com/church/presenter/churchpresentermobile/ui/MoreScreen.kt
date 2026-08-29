package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.more_announcements_subtitle
import churchpresentermobile.composeapp.generated.resources.more_announcements_title
import churchpresentermobile.composeapp.generated.resources.more_notices_subtitle
import churchpresentermobile.composeapp.generated.resources.more_notices_title
import churchpresentermobile.composeapp.generated.resources.more_dictionary_subtitle
import churchpresentermobile.composeapp.generated.resources.more_dictionary_title
import churchpresentermobile.composeapp.generated.resources.more_photos_subtitle
import churchpresentermobile.composeapp.generated.resources.more_photos_title
import churchpresentermobile.composeapp.generated.resources.more_qa_subtitle
import churchpresentermobile.composeapp.generated.resources.more_qa_title
import churchpresentermobile.composeapp.generated.resources.more_web_subtitle
import churchpresentermobile.composeapp.generated.resources.more_web_title
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

private data class MoreEntry(
    val destination: MoreDestination,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

/**
 * Launcher grid/list for secondary destinations reached from the "More" tab.
 * Keeps the bottom bar at the Material-recommended 5 slots while leaving room
 * for future tools (Presentation Remote, Lower Thirds, …).
 *
 * Only what [mode] can serve is listed: photos, Q&A, the dictionary and the web
 * viewer all read from a desktop, and in standalone they opened a screen that
 * could never fill itself.
 */
@Composable
fun MoreScreen(
    mode: AppMode,
    onSelect: (MoreDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val available = MoreDestination.forMode(mode)
    val entries = listOf(
        MoreEntry(MoreDestination.PICTURES, stringResource(Res.string.more_photos_title), stringResource(Res.string.more_photos_subtitle), Icons.Outlined.Image),
        MoreEntry(MoreDestination.QA, stringResource(Res.string.more_qa_title), stringResource(Res.string.more_qa_subtitle), Icons.Outlined.ChatBubbleOutline),
        MoreEntry(MoreDestination.DICTIONARY, stringResource(Res.string.more_dictionary_title), stringResource(Res.string.more_dictionary_subtitle), Icons.AutoMirrored.Outlined.MenuBook),
        // Same slot, two screens: the desktop's announcements, or this device's
        // own notices. The labels follow, so the subtitle does not promise
        // clocks and countdowns that only the remote screen has.
        MoreEntry(
            MoreDestination.ANNOUNCEMENTS,
            stringResource(if (mode == AppMode.STANDALONE) Res.string.more_notices_title else Res.string.more_announcements_title),
            stringResource(if (mode == AppMode.STANDALONE) Res.string.more_notices_subtitle else Res.string.more_announcements_subtitle),
            Icons.Outlined.Campaign,
        ),
        MoreEntry(MoreDestination.WEB, stringResource(Res.string.more_web_title), stringResource(Res.string.more_web_subtitle), Icons.Outlined.Public),
    ).filter { it.destination in available }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(entries.size) { i ->
            MoreRow(entry = entries[i], onClick = { onSelect(entries[i].destination) })
        }
    }
}

@Composable
private fun MoreRow(entry: MoreEntry, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(entry.icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(entry.subtitle, color = colors.muted, fontSize = 12.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.dim,
            modifier = Modifier.size(20.dp),
        )
    }
}
