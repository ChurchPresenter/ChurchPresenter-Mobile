package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.CanvasTool
import org.jetbrains.compose.resources.stringResource

/**
 * The floating rail of tools down the board's left edge, in three groups split by
 * hairlines.
 *
 * @param onActiveCentre Where the active tool's button sits, as a y in root
 *   coordinates — the options panel hangs its pointer there.
 */
@Composable
internal fun CanvasToolRail(
    active: CanvasTool,
    onPick: (CanvasTool) -> Unit,
    onActiveCentre: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = canvasColors
    Column(
        modifier = modifier
            .floatingPanel(colors, RoundedCornerShape(18.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CanvasToolGroups.forEachIndexed { index, group ->
            if (index > 0) {
                Box(
                    Modifier
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .width(32.dp)
                        .height(1.dp)
                        .background(colors.divider),
                )
            }
            group.forEach { tool ->
                RailButton(
                    tool = tool,
                    active = tool == active,
                    onClick = { onPick(tool) },
                    modifier = if (tool == active) {
                        Modifier.onGloballyPositioned { onActiveCentre(it.boundsInRoot().center.y) }
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

@Composable
private fun RailButton(tool: CanvasTool, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = canvasColors
    val spec = tool.spec()
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .testTag(CanvasTags.tool(tool))
            .size(44.dp)
            .clip(shape)
            .background(if (active) colors.accentTint else Color.Transparent)
            .border(1.dp, if (active) colors.accentBorder else Color.Transparent, shape)
            .semantics { selected = active }
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = stringResource(spec.label),
            tint = if (active) colors.accent else colors.secondary,
            modifier = Modifier
                .size(20.dp)
                .then(if (spec.mirrored) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier),
        )
    }
}
