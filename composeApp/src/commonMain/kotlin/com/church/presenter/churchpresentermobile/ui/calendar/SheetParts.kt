package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_close
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/** A sheet's title row: bold title, optional subtitle, and a round close button on the right. */
@Composable
internal fun SheetTitle(title: String, onClose: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    val colors = LocalAppColors.current
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = colors.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) MutedText(subtitle)
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(colors.surfaceStrong)
                .testTag(CalendarTags.SHEET_CLOSE)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(Res.string.calendar_close),
                tint = colors.secondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** A compact one-line field with its label above it — the planner's forms are dense. */
@Composable
internal fun CompactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    highlighted: Boolean = false,
) {
    val colors = LocalAppColors.current
    Column(modifier = modifier) {
        CalendarOverline(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colors.inputBg)
                .border(1.dp, if (highlighted) colors.accent else colors.border, RoundedCornerShape(11.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) Text(placeholder, color = colors.muted, fontSize = 14.sp, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A row of equal-width segments, one lit — the service type and repeat choosers. */
@Composable
internal fun SegmentRow(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tagFor: ((Int) -> String)? = null,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.inputBg)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (active) colors.accent else colors.background.copy(alpha = 0f))
                    .then(tagFor?.let { Modifier.testTag(it(index)) } ?: Modifier)
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (active) colors.onAccent else colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A selectable card with a radio dot, a title and a line under it — "Start from" options. */
@Composable
internal fun ChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    CalendarCard(modifier = modifier, selected = selected, onClick = onClick) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .border(1.5.dp, if (selected) colors.accent else colors.borderStrong, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.accent))
        }
        Column(modifier = Modifier.weight(1f)) {
            TitleText(title, size = 14)
            MutedText(subtitle)
        }
    }
}

/** A tick box row — the copy sheet's "Include" choices. */
@Composable
internal fun CheckCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    CalendarCard(modifier = modifier, onClick = onToggle) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) colors.accent else colors.inputBg)
                .border(1.dp, if (checked) colors.accent else colors.borderStrong, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            TitleText(title, size = 14)
            MutedText(subtitle)
        }
    }
}
