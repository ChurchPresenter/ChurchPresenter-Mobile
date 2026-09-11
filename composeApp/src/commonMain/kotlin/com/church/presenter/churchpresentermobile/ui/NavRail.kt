package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.app_title
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * The tablet's side navigation rail — the same tabs [BottomTabBar] shows, moved
 * off the bottom edge and given room for their labels.
 *
 * Above the tabs sits the brand mark, which the bottom strip has nowhere to put:
 * on a tablet the app is one window among several, so it has to say whose window
 * it is.
 *
 * Draws from the same [tabSpecs] as the bottom strip and carries the same
 * [UiTags.tab] tags, so a test names a tab the same way in either layout. Only
 * one of the two is ever composed — see [usesNavRail].
 */
@Composable
fun NavRail(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<AppTab> = AppTab.forMode(AppMode.REMOTE),
) {
    val colors = LocalAppColors.current
    // Preserve the caller's order rather than tabSpecs' declaration order, so the
    // rail reads the way AppTab.forMode arranged it.
    val visibleSpecs = tabs.mapNotNull { tab -> tabSpecs.firstOrNull { it.tab == tab } }

    Row(modifier = modifier.fillMaxHeight().width(NavRailWidth)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                // A touch lifted off the background, as in the design — surface is
                // translucent white over it in dark, a tinted card in light.
                .background(colors.surface)
                // Both insets, because the rail spans the full height: the status
                // bar at the top and, on gesture navigation, the home indicator at
                // the bottom — which the bottom strip used to keep clear on its own.
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 22.dp),
        ) {
            BrandMark(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 22.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                visibleSpecs.forEach { spec ->
                    NavRailItem(
                        spec = spec,
                        active = spec.tab == selectedTab,
                        onClick = { onTabSelected(spec.tab) },
                    )
                }
            }
        }
        // Hairline against the content, matching the bottom strip's top hairline.
        Spacer(Modifier.fillMaxHeight().width(1.dp).background(colors.borderSubtle))
    }
}

/** Cross + wordmark, stacked the way the design's rail header stacks them. */
@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier.testTag(UiTags.NAV_RAIL_BRAND),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        CrossIcon(
            brush = SolidColor(colors.accent),
            modifier = Modifier.size(width = 18.dp, height = 36.dp),
        )
        // Allowed to wrap: the rail's width is fixed and a longer localised name
        // has to go somewhere, and two lines of wordmark beats one ellipsised.
        Text(
            text = stringResource(Res.string.app_title),
            color = colors.text,
            fontSize = 15.sp,
            lineHeight = (15 * 1.2).sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.03).em,
        )
    }
}

@Composable
private fun NavRailItem(spec: TabSpec, active: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val label = stringResource(spec.label)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) colors.accentTint else Color.Transparent)
            .testTag(UiTags.tab(spec.tab))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = label,
            tint = if (active) colors.accent else colors.muted,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = (-0.01).em,
            color = if (active) colors.accent else colors.secondary,
        )
    }
}
