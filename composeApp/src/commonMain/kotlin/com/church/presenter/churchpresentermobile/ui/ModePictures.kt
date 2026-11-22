package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.mode_diagram_computer
import churchpresentermobile.composeapp.generated.resources.mode_diagram_screen
import churchpresentermobile.composeapp.generated.resources.start_diagram_phone
import churchpresentermobile.composeapp.generated.resources.start_diagram_tv
import churchpresentermobile.composeapp.generated.resources.start_sample_day
import churchpresentermobile.composeapp.generated.resources.start_sample_minutes
import churchpresentermobile.composeapp.generated.resources.start_sample_scripture
import churchpresentermobile.composeapp.generated.resources.start_sample_sermon
import churchpresentermobile.composeapp.generated.resources.start_sample_service
import churchpresentermobile.composeapp.generated.resources.start_sample_welcome
import churchpresentermobile.composeapp.generated.resources.start_sample_worship
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.ui.theme.AppColors
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ModePicture(mode: AppMode, tint: Color, large: Boolean) {
    val gap = if (large) 14.dp else 8.dp
    when (mode) {
        AppMode.REMOTE -> Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Top) {
            Device(Icons.Outlined.Smartphone, stringResource(Res.string.start_diagram_phone), tint, large)
            Link(Icons.AutoMirrored.Filled.ArrowForward, tint, large)
            Device(Icons.Outlined.Computer, stringResource(Res.string.mode_diagram_computer), tint, large)
            Link(Icons.AutoMirrored.Filled.ArrowForward, tint, large)
            Device(Icons.Outlined.Slideshow, stringResource(Res.string.mode_diagram_screen), tint, large)
        }
        AppMode.STANDALONE -> Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Top,
        ) {
            Device(Icons.Outlined.Smartphone, stringResource(Res.string.start_diagram_phone), tint, large)
            Link(Icons.Filled.Cast, tint, large)
            Device(Icons.Outlined.Tv, stringResource(Res.string.start_diagram_tv), tint, large)
        }
        AppMode.CALENDAR -> SampleRunOfShow(large)
    }
}

@Composable
internal fun Device(icon: ImageVector, label: String, tint: Color, large: Boolean) {
    val look = LocalAppColors.current
    val shape = RoundedCornerShape(if (large) 16.dp else 12.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (large) 8.dp else 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (large) 64.dp else 46.dp)
                .clip(shape)
                .background(look.surfaceStrong)
                .border(1.dp, tint.copy(alpha = 0.35f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(if (large) 30.dp else 22.dp))
        }
        Text(label, color = look.muted, fontSize = if (large) 13.sp else 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun Link(icon: ImageVector, tint: Color, large: Boolean) {
    Box(modifier = Modifier.height(if (large) 64.dp else 46.dp), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
    }
}

internal class SampleItem(
    val time: String,
    val title: StringResource,
    val minutes: Int,
    val color: (AppColors) -> Color,
)

internal val sampleItems = listOf(
    SampleItem("9:00", Res.string.start_sample_welcome, 5) { it.accent },
    SampleItem("9:05", Res.string.start_sample_worship, 20) { it.greekAccent },
    SampleItem("9:25", Res.string.start_sample_scripture, 5) { it.amber },
    SampleItem("9:30", Res.string.start_sample_sermon, 35) { it.accent },
)

@Composable
internal fun SampleRunOfShow(large: Boolean) {
    val look = LocalAppColors.current
    val small: TextUnit = if (large) 12.sp else 11.sp
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (large) 6.dp else 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.start_sample_day).uppercase(),
                color = look.amber,
                fontSize = if (large) 11.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.08.em,
            )
            Text(
                stringResource(Res.string.start_sample_service),
                color = look.muted,
                fontSize = small,
                fontWeight = FontWeight.SemiBold,
            )
        }
        val shown = if (large) sampleItems else sampleItems.dropLast(1)
        shown.forEach { item -> SampleRow(item, large, small) }
    }
}

@Composable
internal fun SampleRow(item: SampleItem, large: Boolean, small: TextUnit) {
    val look = LocalAppColors.current
    val shape = RoundedCornerShape(9.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (large) 34.dp else 28.dp)
            .clip(shape)
            .background(look.surfaceStrong)
            .border(1.dp, look.border, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = if (large) 20.dp else 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(item.color(look)),
        )
        Text(
            item.time,
            color = look.muted,
            fontSize = small,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(40.dp),
        )
        Text(
            stringResource(item.title),
            color = look.text,
            fontSize = if (large) 13.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(Res.string.start_sample_minutes, item.minutes),
            color = look.muted,
            fontSize = small,
            fontWeight = FontWeight.Medium,
        )
    }
}
