package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.announcements_title
import churchpresentermobile.composeapp.generated.resources.contact_us_title
import churchpresentermobile.composeapp.generated.resources.more_pane_empty_body
import churchpresentermobile.composeapp.generated.resources.more_pane_empty_title
import churchpresentermobile.composeapp.generated.resources.strongs_dictionary_title
import churchpresentermobile.composeapp.generated.resources.tab_more
import churchpresentermobile.composeapp.generated.resources.tab_qa_admin
import churchpresentermobile.composeapp.generated.resources.web_title
import churchpresentermobile.composeapp.generated.resources.more_announcements_subtitle
import churchpresentermobile.composeapp.generated.resources.more_announcements_title
import churchpresentermobile.composeapp.generated.resources.more_contact_subtitle
import churchpresentermobile.composeapp.generated.resources.more_contact_title
import churchpresentermobile.composeapp.generated.resources.more_notices_subtitle
import churchpresentermobile.composeapp.generated.resources.more_notices_title
import churchpresentermobile.composeapp.generated.resources.more_dictionary_subtitle
import churchpresentermobile.composeapp.generated.resources.more_dictionary_title
import churchpresentermobile.composeapp.generated.resources.more_photos_subtitle
import churchpresentermobile.composeapp.generated.resources.more_photos_title
import churchpresentermobile.composeapp.generated.resources.more_qa_subtitle
import churchpresentermobile.composeapp.generated.resources.more_qa_title
import churchpresentermobile.composeapp.generated.resources.more_report_subtitle
import churchpresentermobile.composeapp.generated.resources.more_report_title
import churchpresentermobile.composeapp.generated.resources.more_web_subtitle
import churchpresentermobile.composeapp.generated.resources.more_web_title
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import androidx.compose.material.icons.outlined.CalendarMonth
import churchpresentermobile.composeapp.generated.resources.more_calendar_subtitle
import churchpresentermobile.composeapp.generated.resources.more_calendar_title
import churchpresentermobile.composeapp.generated.resources.more_new_badge
import androidx.compose.ui.unit.em
import org.jetbrains.compose.resources.stringResource

private data class MoreEntry(
    val destination: MoreDestination,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    /** A small tag after the title — `New` on a tool that has just arrived. */
    val badge: String? = null,
)

/**
 * Launcher grid/list for secondary destinations reached from the "More" tab.
 * Keeps the bottom bar at the Material-recommended 5 slots while leaving room
 * for future tools (Presentation Remote, Lower Thirds, …).
 *
 * Only what [mode] can serve is listed: photos, Q&A, the dictionary and the web
 * viewer all read from a desktop, and in standalone they opened a screen that
 * could never fill itself.
 *
 * The tiles flow into as many columns as the width allows, so the same launcher
 * is a single list on a phone and a grid in a tablet's left-hand pane.
 *
 * @param selected Drawn as the open tile. Only meaningful beside a detail pane —
 *   on a phone, opening a destination replaces this screen, so there is never a
 *   selected tile to see.
 */
@Composable
fun MoreScreen(
    mode: AppMode,
    onSelect: (MoreDestination) -> Unit,
    modifier: Modifier = Modifier,
    selected: MoreDestination? = null,
) {
    val colors = LocalAppColors.current
    val available = MoreDestination.forMode(mode)
    val entries = listOf(
        MoreEntry(
            MoreDestination.CALENDAR,
            stringResource(Res.string.more_calendar_title),
            stringResource(Res.string.more_calendar_subtitle),
            Icons.Outlined.CalendarMonth,
            badge = stringResource(Res.string.more_new_badge),
        ),
        MoreEntry(MoreDestination.PICTURES, stringResource(Res.string.more_photos_title), stringResource(Res.string.more_photos_subtitle), Icons.Outlined.Image),
        MoreEntry(MoreDestination.QA, stringResource(Res.string.more_qa_title), stringResource(Res.string.more_qa_subtitle), Icons.Outlined.ChatBubbleOutline),
        MoreEntry(MoreDestination.DICTIONARY, stringResource(Res.string.more_dictionary_title), stringResource(Res.string.more_dictionary_subtitle), Icons.AutoMirrored.Outlined.MenuBook),
        // Same slot, two screens: the desktop's announcements, or this device's
        // own notices. The labels follow, so the subtitle does not promise
        // clocks and countdowns that only the remote screen has.
        MoreEntry(
            MoreDestination.ANNOUNCEMENTS,
            stringResource(if (mode == AppMode.STANDALONE) Res.string.more_notices_title else Res.string.more_announcements_title),
            stringResource(if (mode == AppMode.STANDALONE) Res.string.more_notices_subtitle else Res.string.more_announcements_subtitle),
            Icons.Outlined.Campaign,
        ),
        MoreEntry(MoreDestination.WEB, stringResource(Res.string.more_web_title), stringResource(Res.string.more_web_subtitle), Icons.Outlined.Public),
        // Standalone only: what this device itself has projected, for licence reporting.
        MoreEntry(
            MoreDestination.REPORT,
            stringResource(Res.string.more_report_title),
            stringResource(Res.string.more_report_subtitle),
            Icons.Outlined.BarChart,
        ),
        MoreEntry(MoreDestination.CONTACT, stringResource(Res.string.more_contact_title), stringResource(Res.string.more_contact_subtitle), Icons.Outlined.MailOutline),
    ).filter { it.destination in available }
    // Laid out by hand rather than with LazyVerticalGrid: a lazy grid sizes each
    // tile to its own text, so one subtitle wrapping to a second line made its
    // tile taller than the rest of its row. Seven tiles need no laziness, and a
    // Row can size itself to its tallest child.
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.background)) {
        val columns = ((maxWidth - MoreGridPadding * 2 + MoreGridGap) / (MoreTileMinWidth + MoreGridGap))
            .toInt().coerceAtLeast(1)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(MoreGridPadding),
            verticalArrangement = Arrangement.spacedBy(MoreGridGap),
        ) {
            entries.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(MoreGridGap),
                ) {
                    row.forEach { entry ->
                        MoreRow(
                            entry = entry,
                            isSelected = entry.destination == selected,
                            onClick = { onSelect(entry.destination) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    // A short last row keeps its tiles the width of the others.
                    repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

private val MoreGridPadding: Dp = 16.dp
private val MoreGridGap: Dp = 9.dp

@Composable
private fun MoreRow(entry: MoreEntry, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .testTag(UiTags.moreRow(entry.destination))
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) colors.accentTint else colors.surface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) colors.accent else colors.borderSubtle,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(entry.icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = entry.title,
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (entry.badge != null) {
                    Text(
                        text = entry.badge.uppercase(),
                        color = colors.accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.08.em,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.accentTint)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            // Two lines at most: the row is as tall as its tallest tile, so a
            // subtitle may wrap, but a third line would be a tile of mostly text.
            Text(
                text = entry.subtitle,
                color = colors.muted,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isSelected) colors.accent else colors.dim,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * The title a More destination shows in a header.
 *
 * One `when`, because two layouts need the same answer: the phone puts it in the
 * shell's top bar over the open tool, the tablet puts it over the tool's pane.
 * Two copies drifted the moment a destination was renamed for one of them.
 *
 * Two of these are not [MoreEntry]'s launcher label. The dictionary tile reads
 * "Dictionary" and its screen "Strong's Dictionary"; announcements is a
 * different screen per mode, and the header names the one actually open.
 */
@Composable
fun moreDestinationTitle(destination: MoreDestination?, mode: AppMode): String =
    when (destination) {
        MoreDestination.CALENDAR -> stringResource(Res.string.more_calendar_title)
        MoreDestination.PICTURES -> stringResource(Res.string.more_photos_title)
        MoreDestination.QA -> stringResource(Res.string.tab_qa_admin)
        MoreDestination.DICTIONARY -> stringResource(Res.string.strongs_dictionary_title)
        MoreDestination.ANNOUNCEMENTS ->
            if (mode == AppMode.STANDALONE) stringResource(Res.string.more_notices_title)
            else stringResource(Res.string.announcements_title)
        MoreDestination.WEB -> stringResource(Res.string.web_title)
        MoreDestination.REPORT -> stringResource(Res.string.more_report_title)
        MoreDestination.CONTACT -> stringResource(Res.string.contact_us_title)
        null -> ""
    }

/**
 * The tablet's arrangement for the More tab: the launcher on the left, the tool
 * it opened filling what is left.
 *
 * On a phone opening a tool *replaces* the launcher, and the shell's top bar
 * grows a back arrow to undo that. Here both are on screen at once, so there is
 * no back arrow and no single title that could name both halves — which is why
 * each pane carries its own header.
 *
 * @param tool The open destination's screen. Built by the caller, because these
 *   are six unrelated screens with six unrelated sets of collaborators; this
 *   composable's job is where they go, not what they are.
 */
@Composable
fun MoreTwoPane(
    mode: AppMode,
    selected: MoreDestination?,
    onSelect: (MoreDestination) -> Unit,
    tool: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onMenu: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Row(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.width(MoreTilePaneWidth).fillMaxHeight()) {
            ScreenHeader(
                title = stringResource(Res.string.tab_more),
                onMenu = onMenu,
                onSettings = onSettings,
            )
            Box(modifier = Modifier.weight(1f)) {
                MoreScreen(mode = mode, onSelect = onSelect, selected = selected)
            }
        }

        VerticalDivider(color = colors.borderSubtle)

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (selected != null) {
                // No gear here: the launcher's header beside it already carries
                // one, and two gears on one screen open the same settings.
                ScreenHeader(title = moreDestinationTitle(selected, mode))
                HorizontalDivider(color = colors.borderSubtle)
                Box(modifier = Modifier.weight(1f)) { tool() }
            } else {
                EmptyState(
                    title = stringResource(Res.string.more_pane_empty_title),
                    body = stringResource(Res.string.more_pane_empty_body),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}
