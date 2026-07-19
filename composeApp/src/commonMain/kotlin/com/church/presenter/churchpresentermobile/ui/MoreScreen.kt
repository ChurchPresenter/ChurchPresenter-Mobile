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
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors

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
 */
@Composable
fun MoreScreen(
    onSelect: (MoreDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val entries = listOf(
        MoreEntry(MoreDestination.QA, "Q&A", "Review and project audience questions", Icons.Outlined.ChatBubbleOutline),
        MoreEntry(MoreDestination.DICTIONARY, "Dictionary", "Strong's Hebrew & Greek lexicon", Icons.AutoMirrored.Outlined.MenuBook),
        MoreEntry(MoreDestination.ANNOUNCEMENTS, "Announcements", "Text, clocks & countdown timers", Icons.Outlined.Campaign),
        MoreEntry(MoreDestination.WEB, "Web", "Project a web page on screen", Icons.Outlined.Public),
    )
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
