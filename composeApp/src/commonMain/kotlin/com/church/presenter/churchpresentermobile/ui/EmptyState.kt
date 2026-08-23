package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors

/**
 * A screen with nothing on it yet, and the thing to do about it.
 *
 * Every empty state in the app used to be two lines of grey text — accurate, and a dead end. The
 * action is the point: on a phone with no desktop, "copy it from your computer" is only useful
 * if the screen can also take you there.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.space8),
        ) {
            Text(
                text = title,
                color = colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && actionIcon != null && onAction != null) {
                // Bounded so the button does not stretch the width of a tablet.
                Box(Modifier.padding(top = AppDimens.space8).widthIn(max = 300.dp)) {
                    OutlineActionButton(
                        label = actionLabel,
                        icon = actionIcon,
                        onClick = onAction,
                    )
                }
            }
            if (secondaryLabel != null && onSecondary != null) {
                Text(
                    text = secondaryLabel,
                    color = colors.accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onSecondary),
                )
            }
        }
    }
}
