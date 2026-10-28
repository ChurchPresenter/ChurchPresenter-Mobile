package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.CastConnected
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_end_live
import churchpresentermobile.composeapp.generated.resources.canvas_go_live
import churchpresentermobile.composeapp.generated.resources.canvas_layers
import churchpresentermobile.composeapp.generated.resources.canvas_live_failed
import churchpresentermobile.composeapp.generated.resources.canvas_live_unsupported
import churchpresentermobile.composeapp.generated.resources.canvas_picture_failed
import churchpresentermobile.composeapp.generated.resources.canvas_not_approved
import churchpresentermobile.composeapp.generated.resources.canvas_not_saved
import churchpresentermobile.composeapp.generated.resources.canvas_redo
import churchpresentermobile.composeapp.generated.resources.canvas_saved
import churchpresentermobile.composeapp.generated.resources.canvas_saving
import churchpresentermobile.composeapp.generated.resources.canvas_subtitle
import churchpresentermobile.composeapp.generated.resources.canvas_undo
import churchpresentermobile.composeapp.generated.resources.canvas_untitled
import churchpresentermobile.composeapp.generated.resources.canvas_waiting_approval
import churchpresentermobile.composeapp.generated.resources.canvas_zoom_in
import churchpresentermobile.composeapp.generated.resources.canvas_zoom_out
import churchpresentermobile.composeapp.generated.resources.canvas_zoom_value
import churchpresentermobile.composeapp.generated.resources.tab_more
import com.church.presenter.churchpresentermobile.model.zoomedIn
import com.church.presenter.churchpresentermobile.model.zoomedOut
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasPanel
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState
import com.church.presenter.churchpresentermobile.viewmodel.LiveNote
import com.church.presenter.churchpresentermobile.viewmodel.SaveStatus
import org.jetbrains.compose.resources.stringResource

/**
 * The canvas's bar: back to More, the canvas's name and page, undo and redo on the
 * left; zoom, layers, more and Go Live on the right.
 *
 * Draws its own back link rather than using the app's [com.church.presenter.churchpresentermobile.ui.ScreenHeader]:
 * the design names where Back goes ("‹ More") beside a two-line title, which the
 * shared header has no shape for.
 */
@Composable
internal fun CanvasTopBar(
    state: CanvasUiState,
    onIntent: (CanvasIntent) -> Unit,
    onBack: () -> Unit,
    canProject: Boolean,
) {
    val colors = canvasColors
    Column(modifier = Modifier.fillMaxWidth().background(colors.chrome).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BackLink(onBack)
            Box(Modifier.width(1.dp).height(26.dp).background(colors.border))
            Column(
                modifier = Modifier.padding(start = 6.dp).widthIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = state.document.title ?: stringResource(Res.string.canvas_untitled),
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.025).em,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val saveNote = when (state.saveStatus) {
                    SaveStatus.NONE -> null
                    SaveStatus.SAVED -> Res.string.canvas_saved
                    SaveStatus.SAVING -> Res.string.canvas_saving
                    SaveStatus.FAILED -> Res.string.canvas_not_saved
                }?.let { stringResource(it) }
                val liveNote = when (state.liveNote) {
                    LiveNote.DECLINED -> Res.string.canvas_not_approved
                    LiveNote.FAILED -> Res.string.canvas_live_failed
                    LiveNote.UNSUPPORTED -> Res.string.canvas_live_unsupported
                    null -> null
                }?.let { stringResource(it) }
                Text(
                    text = listOfNotNull(
                        stringResource(Res.string.canvas_subtitle, state.pageIndex + 1, state.document.pages.size),
                        saveNote,
                        stringResource(Res.string.canvas_picture_failed).takeIf { state.pictureFailed },
                    ).joinToString(" · "),
                    color = colors.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                // Why Go Live did not stay up, in the danger red: in the grey subtitle
                // it read as the button merely flashing.
                if (liveNote != null) {
                    Text(
                        text = liveNote,
                        color = colors.danger,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(CanvasTags.LIVE_NOTE),
                    )
                }
            }
            Row(modifier = Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                BarIcon(
                    icon = Icons.AutoMirrored.Outlined.Undo,
                    description = stringResource(Res.string.canvas_undo),
                    tag = CanvasTags.UNDO,
                    enabled = state.canUndo,
                    onClick = { onIntent(CanvasIntent.Undo) },
                )
                BarIcon(
                    icon = Icons.AutoMirrored.Outlined.Redo,
                    description = stringResource(Res.string.canvas_redo),
                    tag = CanvasTags.REDO,
                    enabled = state.canRedo,
                    onClick = { onIntent(CanvasIntent.Redo) },
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZoomControl(state.zoom, onIntent)
                BarIcon(
                    icon = Icons.Outlined.Layers,
                    description = stringResource(Res.string.canvas_layers),
                    tag = CanvasTags.LAYERS,
                    active = state.panel == CanvasPanel.LAYERS,
                    onClick = {
                        val next = if (state.panel == CanvasPanel.LAYERS) CanvasPanel.NONE else CanvasPanel.LAYERS
                        onIntent(CanvasIntent.ShowPanel(next))
                    },
                )
                CanvasMoreMenu(state, onIntent)
                // End Live always works; Go Live only where there is somewhere to project to.
                GoLiveButton(
                    live = state.live,
                    waiting = state.awaitingApproval,
                    enabled = state.live || canProject,
                ) {
                    onIntent(CanvasIntent.SetLive(!state.live))
                }
            }
        }
        HorizontalDivider(color = colors.border)
    }
}

/** "‹ More": the way back to the launcher, in the accent, named for where it goes. */
@Composable
private fun BackLink(onBack: () -> Unit) {
    val colors = canvasColors
    Row(
        modifier = Modifier
            .testTag(CanvasTags.BACK)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onBack)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // The label beside it says where Back goes, so the arrow needs no description of its own.
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBackIos,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            stringResource(Res.string.tab_more),
            color = colors.accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A 40dp square icon button of the bar; [active] wears the accent, as a toggled panel button should. */
@Composable
internal fun BarIcon(
    icon: ImageVector,
    description: String,
    tag: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
) {
    val colors = canvasColors
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .testTag(tag)
            .size(40.dp)
            .clip(shape)
            .background(if (active) colors.accentTint else Color.Transparent)
            .border(1.dp, if (active) colors.accentBorder else Color.Transparent, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = if (active) colors.accent else colors.secondary, modifier = Modifier.size(20.dp))
    }
}


/** − 100% +, in one rounded well. */
@Composable
private fun ZoomControl(zoom: Int, onIntent: (CanvasIntent) -> Unit) {
    val colors = canvasColors
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .background(colors.control)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ZoomStep(Icons.Outlined.Remove, stringResource(Res.string.canvas_zoom_out), CanvasTags.ZOOM_OUT) {
            onIntent(CanvasIntent.SetZoom(zoomedOut(zoom)))
        }
        Text(
            text = stringResource(Res.string.canvas_zoom_value, zoom),
            color = colors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 46.dp).testTag(CanvasTags.ZOOM_VALUE),
        )
        ZoomStep(Icons.Outlined.Add, stringResource(Res.string.canvas_zoom_in), CanvasTags.ZOOM_IN) {
            onIntent(CanvasIntent.SetZoom(zoomedIn(zoom)))
        }
    }
}

@Composable
private fun ZoomStep(icon: ImageVector, description: String, tag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .testTag(tag)
            .size(width = 30.dp, height = 30.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = canvasColors.secondary, modifier = Modifier.size(16.dp))
    }
}

/**
 * Go Live, which turns into End Live — in the danger red — while the canvas is on
 * screen, and reads "Waiting for approval…" while the desktop operator decides.
 * Pressing it then withdraws the request.
 */
@Composable
private fun GoLiveButton(live: Boolean, waiting: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = canvasColors
    val content = if (live) Color.White else colors.onAccent
    Row(
        modifier = Modifier
            .testTag(CanvasTags.GO_LIVE)
            .height(40.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(RoundedCornerShape(12.dp))
            .background(if (live) colors.danger else colors.accent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The words beside it say what it does; the icon is decoration.
        Icon(
            if (live) Icons.Outlined.CastConnected else Icons.Outlined.Cast,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp),
        )
        Text(
            stringResource(
                when {
                    waiting -> Res.string.canvas_waiting_approval
                    live -> Res.string.canvas_end_live
                    else -> Res.string.canvas_go_live
                },
            ),
            color = content,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
