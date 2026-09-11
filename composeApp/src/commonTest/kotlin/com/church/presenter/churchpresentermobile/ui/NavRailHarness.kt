package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppTab

/**
 * [NavRail] with a tab set and a selection, ready to be tapped.
 *
 * Boxed to a height because the rail fills the one it is given, and the test
 * surface would otherwise hand it zero — every tab laid out at zero height, and
 * every tap landing on nothing.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showRail(
    selected: AppTab = AppTab.SONGS,
    tabs: List<AppTab> = AppTab.forMode(AppMode.REMOTE),
    onTabSelected: (AppTab) -> Unit = {},
) = showScreen {
    Box(Modifier.height(600.dp)) {
        NavRail(selectedTab = selected, onTabSelected = onTabSelected, tabs = tabs)
    }
}
