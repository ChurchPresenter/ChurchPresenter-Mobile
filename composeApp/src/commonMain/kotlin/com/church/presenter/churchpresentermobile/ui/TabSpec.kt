package com.church.presenter.churchpresentermobile.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.ui.graphics.vector.ImageVector
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.tab_bible
import churchpresentermobile.composeapp.generated.resources.tab_library
import churchpresentermobile.composeapp.generated.resources.tab_media
import churchpresentermobile.composeapp.generated.resources.tab_more
import churchpresentermobile.composeapp.generated.resources.tab_present
import churchpresentermobile.composeapp.generated.resources.tab_songs
import com.church.presenter.churchpresentermobile.model.AppTab
import org.jetbrains.compose.resources.StringResource

/** Label and icon for one tab, shared by the bottom strip and the side [NavRail]. */
internal data class TabSpec(val tab: AppTab, val label: StringResource, val icon: ImageVector)

/**
 * Every tab's label and icon, in declaration order.
 *
 * `internal` rather than private because the side rail draws the same tabs with
 * the same icons — two copies of this list would drift the moment a tab's icon
 * changed, and only one of the two layouts is on screen to notice.
 */
internal val tabSpecs = listOf(
    TabSpec(AppTab.PRESENT, Res.string.tab_present, Icons.Outlined.Cast),
    TabSpec(AppTab.SONGS, Res.string.tab_songs, Icons.Outlined.MusicNote),
    TabSpec(AppTab.BIBLE, Res.string.tab_bible, Icons.AutoMirrored.Outlined.MenuBook),
    TabSpec(AppTab.MEDIA, Res.string.tab_media, Icons.Outlined.PlayCircleOutline),
    TabSpec(AppTab.PRESENTATION, Res.string.tab_present, Icons.Outlined.DesktopWindows),
    TabSpec(AppTab.LIBRARY, Res.string.tab_library, Icons.Outlined.LibraryMusic),
    TabSpec(AppTab.MORE, Res.string.tab_more, Icons.Filled.MoreHoriz),
)
