package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors

// The building blocks Display text is drawn from. Kept to this screen: their sizes follow its
// design rather than the settings pages', and widening them for general use would be a change to
// every screen that does not need one.

/** The uppercase label above a card. */
@Composable
internal fun DisplayOverline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = LocalAppColors.current.muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.em,
        modifier = modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
    )
}

/** A bordered group of rows. */
@Composable
internal fun DisplayCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(CARD_RADIUS)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, shape)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        content = content,
    )
}

/** The hairline between two rows of a [DisplayCard]. */
@Composable
internal fun CardDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalAppColors.current.borderSubtle))
}

/** A label, an optional line under it, and whatever control sits on the right. */
@Composable
internal fun LabeledRow(
    label: String,
    hint: String? = null,
    trailing: @Composable RowScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            hint?.let { Text(it, color = colors.muted, fontSize = 11.sp) }
        }
        trailing()
    }
}

/** A label above a control that takes the card's whole width. */
@Composable
internal fun StackedRow(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = LocalAppColors.current.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        content()
    }
}

/** A segmented switch, taller than the settings pages' so a thumb finds it beside a live preview. */
@Composable
internal fun DisplaySegments(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    tag: (Int) -> String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.inputBg)
            .border(1.dp, colors.borderSubtle, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .testTag(tag(index))
                    .semantics { selected = active }
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) colors.surfaceElevated else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = if (compact) 7.dp else 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (active) colors.text else colors.muted,
                    fontSize = if (compact) 13.sp else 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A value with a menu of alternatives under it — the font, a position, a format. */
@Composable
internal fun DisplayDropdown(
    selectedIndex: Int,
    options: List<String>,
    onSelect: (Int) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    /** Take the whole width it is given, as the tablet's row of preview options does. */
    fill: Boolean = false,
) {
    val colors = LocalAppColors.current
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(modifier) {
        Row(
            modifier = Modifier
                .testTag(tag)
                .then(if (fill) Modifier.fillMaxWidth() else Modifier.widthIn(min = 120.dp))
                .clip(shape)
                .background(colors.inputBg)
                .border(1.dp, colors.border, shape)
                .clickable { open = true }
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = options.getOrElse(selectedIndex) { "" },
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        open = false
                        onSelect(index)
                    },
                    modifier = Modifier.testTag(DisplayTextTags.option(tag, index)),
                )
            }
        }
    }
}

private val CARD_RADIUS = 14.dp

