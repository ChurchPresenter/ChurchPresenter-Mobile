package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.church.presenter.churchpresentermobile.model.RowKind
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors

internal class KindLook(val icon: ImageVector, val tint: Color)

/** The icon and color a row kind is drawn with — one place, so a song looks the same in every list. */
@Composable
internal fun kindLook(kind: String): KindLook {
    val colors = LocalAppColors.current
    return when (kind) {
        RowKind.SONG -> KindLook(Icons.Outlined.MusicNote, colors.scheduleSongFg)
        RowKind.BIBLE -> KindLook(Icons.AutoMirrored.Outlined.MenuBook, colors.scheduleBibleFg)
        RowKind.MINISTRY -> KindLook(Icons.Outlined.Person, colors.accent)
        RowKind.PICTURES -> KindLook(Icons.Outlined.Image, colors.schedulePictureFg)
        RowKind.PRESENTATION -> KindLook(Icons.Outlined.Slideshow, colors.warning)
        RowKind.MEDIA -> KindLook(Icons.Outlined.PlayCircleOutline, colors.warning)
        RowKind.SCENE -> KindLook(Icons.Outlined.Layers, colors.greekAccent)
        RowKind.TIMER -> KindLook(Icons.Outlined.Timer, colors.amber)
        RowKind.ANNOUNCEMENT -> KindLook(Icons.Outlined.Campaign, colors.amber)
        RowKind.LOWER_THIRD -> KindLook(Icons.Outlined.Subtitles, colors.greekAccent)
        RowKind.WEBSITE -> KindLook(Icons.Outlined.Public, colors.greekAccent)
        RowKind.CUE -> KindLook(Icons.Outlined.Bolt, colors.amber)
        RowKind.PRESET -> KindLook(Icons.Outlined.Star, colors.amber)
        else -> KindLook(Icons.Outlined.Widgets, colors.muted)
    }
}
