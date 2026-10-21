package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.display_color_custom
import churchpresentermobile.composeapp.generated.resources.display_color_swatch
import churchpresentermobile.composeapp.generated.resources.display_standalone_badge
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/** A tappable colour square. [selected] rings it in the accent colour. */
@Composable
internal fun ColorChip(
    hex: String,
    onClick: () -> Unit,
    tag: String,
    selected: Boolean = false,
    size: Dp = 30.dp,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(size * CHIP_RADIUS_RATIO)
    val description = stringResource(Res.string.display_color_swatch, hex)
    Box(
        modifier = Modifier
            .testTag(tag)
            .semantics {
                contentDescription = description
                this.selected = selected
            }
            .size(size)
            .then(if (selected) Modifier.border(2.dp, colors.accent, shape).padding(3.dp) else Modifier)
            .clip(RoundedCornerShape((size - if (selected) 6.dp else 0.dp) * CHIP_RADIUS_RATIO))
            .background(CHECKER)
            .background(parseHexColor(hex, Color.Transparent))
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(size * CHIP_RADIUS_RATIO))
            .clickable(onClick = onClick),
    )
}

/** The rainbow square that opens the full picker. */
@Composable
internal fun CustomColorChip(onClick: () -> Unit, tag: String, size: Dp = 30.dp) {
    val description = stringResource(Res.string.display_color_custom)
    Box(
        modifier = Modifier
            .testTag(tag)
            .semantics { contentDescription = description }
            .size(size)
            .clip(RoundedCornerShape(size * CHIP_RADIUS_RATIO))
            .background(Brush.sweepGradient(RAINBOW))
            .clickable(onClick = onClick),
    )
}

/** "STANDALONE": the reminder that this styles the phone's own output, not the desktop's. */
@Composable
internal fun StandaloneBadge(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Text(
        text = stringResource(Res.string.display_standalone_badge),
        color = colors.accent,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.12.em,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.accentTint)
            .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Reset beside Save style. */
@Composable
internal fun DisplayActions(
    onReset: () -> Unit,
    onSave: () -> Unit,
    resetLabel: String,
    saveLabel: String,
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (leading != null) {
            leading()
        } else {
            ActionButton(resetLabel, onReset, DisplayTextTags.RESET, filled = false, Modifier.weight(1f))
        }
        ActionButton(
            saveLabel,
            onSave,
            DisplayTextTags.SAVE,
            filled = true,
            Modifier.weight(if (leading != null) 1f else 2f),
        )
    }
}

/** The two kinds of button on this screen: the quiet one and the one that saves. */
@Composable
internal fun ActionButton(
    label: String,
    onClick: () -> Unit,
    tag: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .testTag(tag)
            .height(48.dp)
            .clip(shape)
            .background(if (filled) colors.accent else colors.surface)
            .then(if (filled) Modifier else Modifier.border(1.dp, colors.border, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (filled) colors.onAccent else colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private const val CHIP_RADIUS_RATIO = 0.27f
/** Under a swatch, so a part-transparent colour reads as part-transparent rather than as darker. */
private val CHECKER = Color(0xFF8A8A8A)
private val RAINBOW = listOf(
    Color(0xFFF87171), Color(0xFFFBBF24), Color(0xFF86EFAC), Color(0xFF60A5FA), Color(0xFFC084FC), Color(0xFFF87171),
)
