package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.tab_bible
import churchpresentermobile.composeapp.generated.resources.tab_library
import churchpresentermobile.composeapp.generated.resources.tab_media
import churchpresentermobile.composeapp.generated.resources.tab_more
import churchpresentermobile.composeapp.generated.resources.tab_present
import churchpresentermobile.composeapp.generated.resources.tab_songs
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class TabSpec(val tab: AppTab, val label: StringResource, val icon: ImageVector)

private val tabSpecs = listOf(
    TabSpec(AppTab.PRESENT, Res.string.tab_present, Icons.Outlined.Cast),
    TabSpec(AppTab.SONGS, Res.string.tab_songs, Icons.Outlined.MusicNote),
    TabSpec(AppTab.BIBLE, Res.string.tab_bible, Icons.AutoMirrored.Outlined.MenuBook),
    TabSpec(AppTab.MEDIA, Res.string.tab_media, Icons.Outlined.PlayCircleOutline),
    TabSpec(AppTab.PRESENTATION, Res.string.tab_present, Icons.Outlined.DesktopWindows),
    TabSpec(AppTab.LIBRARY, Res.string.tab_library, Icons.Outlined.LibraryMusic),
    TabSpec(AppTab.MORE, Res.string.tab_more, Icons.Filled.MoreHoriz),
)

/**
 * Persistent bottom navigation bar for the redesign.
 *
 * Height 72 + a 1px top hairline. The active tab's icon sits in an accent-tinted
 * rounded pill (radius 14) with an accent 10/600 label; inactive tabs show a
 * dim icon + 10/500 label.
 */
@Composable
fun BottomTabBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<AppTab> = AppTab.forMode(AppMode.REMOTE),
) {
    val colors = LocalAppColors.current
    val barBg = if (colors.isDark) colors.background else colors.surface
    // Preserve the caller's order rather than the declaration order of tabSpecs,
    // so the strip reads the way AppTab.forMode arranged it.
    val visibleSpecs = tabs.mapNotNull { tab -> tabSpecs.firstOrNull { it.tab == tab } }

    Column(modifier = modifier.fillMaxWidth().background(barBg)) {
        // Top hairline
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(AppDimens.tabBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            visibleSpecs.forEach { spec ->
                val active = spec.tab == selectedTab
                val contentColor = if (active) colors.accent else colors.dim
                val label = stringResource(spec.label)
                val interaction = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                        ) { onTabSelected(spec.tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (active) colors.accentTint else androidx.compose.ui.graphics.Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = spec.icon,
                            contentDescription = label,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = contentColor,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
        }
    }
}
