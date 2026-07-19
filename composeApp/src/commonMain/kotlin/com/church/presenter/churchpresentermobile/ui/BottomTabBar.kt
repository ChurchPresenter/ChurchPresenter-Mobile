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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
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
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors

private data class TabSpec(val tab: AppTab, val label: String, val icon: ImageVector)

private val tabSpecs = listOf(
    TabSpec(AppTab.SONGS, "Songs", Icons.Outlined.MusicNote),
    TabSpec(AppTab.BIBLE, "Bible", Icons.AutoMirrored.Outlined.MenuBook),
    TabSpec(AppTab.PICTURES, "Media", Icons.Outlined.Image),
    TabSpec(AppTab.PRESENTATION, "Present", Icons.Outlined.DesktopWindows),
    TabSpec(AppTab.QA_ADMIN, "Q&A", Icons.Outlined.ChatBubbleOutline),
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
) {
    val colors = LocalAppColors.current
    val barBg = if (colors.isDark) colors.background else colors.surface

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
            tabSpecs.forEach { spec ->
                val active = spec.tab == selectedTab
                val contentColor = if (active) colors.accent else colors.dim
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
                            contentDescription = spec.label,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = spec.label,
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
