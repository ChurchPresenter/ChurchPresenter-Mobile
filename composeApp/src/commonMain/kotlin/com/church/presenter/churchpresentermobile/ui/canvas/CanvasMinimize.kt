package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_expand_panel
import churchpresentermobile.composeapp.generated.resources.canvas_hide_pen_bar
import churchpresentermobile.composeapp.generated.resources.canvas_hide_tools
import churchpresentermobile.composeapp.generated.resources.canvas_minimize_panel
import churchpresentermobile.composeapp.generated.resources.canvas_show_pen_bar
import churchpresentermobile.composeapp.generated.resources.canvas_show_tools
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.viewmodel.CanvasChrome
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/*
 * Tucking the floating pieces away to draw on more of the board: the tool rail, the pen dock
 * and an open options panel. Not in the design, so each minimized piece is the smallest form
 * of itself in the same panel style — the rail keeps the tool in hand, the dock its color.
 */

/** What a piece's minimize and restore buttons say, and which way their arrows point. */
private class ChromeButtons(
    val hide: StringResource,
    val show: StringResource,
    val hideIcon: ImageVector,
    val showIcon: ImageVector,
)

private fun CanvasChrome.buttons(): ChromeButtons = when (this) {
    CanvasChrome.RAIL -> ChromeButtons(
        Res.string.canvas_hide_tools, Res.string.canvas_show_tools,
        Icons.Outlined.KeyboardDoubleArrowLeft, Icons.Outlined.KeyboardDoubleArrowRight,
    )
    CanvasChrome.DOCK -> ChromeButtons(
        Res.string.canvas_hide_pen_bar, Res.string.canvas_show_pen_bar,
        Icons.Outlined.KeyboardArrowDown, Icons.Outlined.KeyboardArrowUp,
    )
    CanvasChrome.OPTIONS -> ChromeButtons(
        Res.string.canvas_minimize_panel, Res.string.canvas_expand_panel,
        Icons.Outlined.ExpandLess, Icons.Outlined.ExpandMore,
    )
}

/**
 * The button that tucks [part] away when it is showing, or brings it back when [minimized]:
 * a small round icon button in the panel's own quiet ink.
 */
@Composable
internal fun ChromeToggle(part: CanvasChrome, minimized: Boolean, onToggle: (Boolean) -> Unit, size: Int = 32) {
    val colors = canvasColors
    val buttons = part.buttons()
    Box(
        modifier = Modifier
            .testTag(CanvasTags.chrome(part, minimized))
            .size(size.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button) { onToggle(!minimized) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (minimized) buttons.showIcon else buttons.hideIcon,
            contentDescription = stringResource(if (minimized) buttons.show else buttons.hide),
            tint = colors.secondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * The tool rail tucked away: the tool in hand, still pressable to open its options, and
 * the way back to the whole rail under it.
 */
@Composable
internal fun MinimizedRail(
    active: CanvasTool,
    onPick: (CanvasTool) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier,
) {
    val colors = canvasColors
    Column(
        modifier = modifier
            .testTag(CanvasTags.MINIMIZED_RAIL)
            .floatingPanel(colors, RoundedCornerShape(18.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RailButton(tool = active, active = true, onClick = { onPick(active) })
        ChromeToggle(CanvasChrome.RAIL, minimized = true, onToggle = { onRestore() })
    }
}

/** The pen dock tucked away: the color in use, and the way back to the whole dock. */
@Composable
internal fun MinimizedDock(ink: Color, onRestore: () -> Unit, modifier: Modifier) {
    val colors = canvasColors
    Row(
        modifier = modifier
            .testTag(CanvasTags.MINIMIZED_DOCK)
            .floatingPanel(colors, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onRestore)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(DOCK_DOT).clip(CircleShape).background(ink))
        ChromeToggle(CanvasChrome.DOCK, minimized = true, onToggle = { onRestore() }, size = 28)
    }
}

private val DOCK_DOT = 22.dp
