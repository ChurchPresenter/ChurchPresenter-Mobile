package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A section's heading, with an optional control at its far end.
 *
 * [tag] goes on the text rather than the row, because the tests that look for
 * a heading were written against the text and a tag on a wrapper is not a tag
 * on the thing inside it.
 */
@Composable
internal fun SectionTitle(
    title: StringResource,
    tag: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(title),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.accent,
            modifier = if (tag != null) Modifier.testTag(tag) else Modifier,
        )
        trailing?.invoke()
    }
}

/**
 * A bordered surface card, the container for a group of rows on a settings
 * page — and, with the tablet's larger radius and padding, in the status modal.
 */
@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    radius: Dp = 14.dp,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, shape)
            .padding(padding),
    ) { content() }
}

/** A label on the left and a value on the right, as the About and Diagnostics cards show them. */
@Composable
internal fun ValueRow(label: String, value: String, mono: Boolean = false) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(
            value,
            color = colors.muted,
            fontSize = 13.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        )
    }
}
