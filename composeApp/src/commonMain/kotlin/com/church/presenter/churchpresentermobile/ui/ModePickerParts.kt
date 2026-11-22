package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.start_continue_with
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun cardBrush(selected: Boolean, tint: Color): Brush {
    val card = LocalAppColors.current.surface
    return if (selected) {
        Brush.verticalGradient(listOf(tint.copy(alpha = 0.13f), tint.copy(alpha = 0.04f)))
    } else {
        Brush.verticalGradient(listOf(card, card))
    }
}

@Composable
internal fun RadioMark(selected: Boolean, tint: Color) {
    val look = LocalAppColors.current
    if (selected) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = look.onAccent, modifier = Modifier.size(16.dp))
        }
    } else {
        Box(modifier = Modifier.size(24.dp).border(2.dp, look.borderStrong, CircleShape))
    }
}

@Composable
internal fun ContinueButton(mode: AppMode, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TintButton(
        label = stringResource(Res.string.start_continue_with, stringResource(mode.titleRes())),
        tint = tint,
        onClick = onClick,
        height = 56.dp,
        fontSize = 17.sp,
        radius = 16.dp,
        trailing = Icons.AutoMirrored.Filled.ArrowForward,
        modifier = modifier.testTag(UiTags.MODE_CONTINUE),
    )
}
