package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.outputs_empty
import churchpresentermobile.composeapp.generated.resources.outputs_hint
import churchpresentermobile.composeapp.generated.resources.outputs_hint_airplay
import churchpresentermobile.composeapp.generated.resources.outputs_hint_cast
import churchpresentermobile.composeapp.generated.resources.outputs_open_cast_settings
import churchpresentermobile.composeapp.generated.resources.outputs_ios_foreground_warning
import churchpresentermobile.composeapp.generated.resources.outputs_scan_hint
import churchpresentermobile.composeapp.generated.resources.outputs_url_label
import churchpresentermobile.composeapp.generated.resources.outputs_state_attached
import churchpresentermobile.composeapp.generated.resources.outputs_state_error
import churchpresentermobile.composeapp.generated.resources.outputs_state_off
import churchpresentermobile.composeapp.generated.resources.outputs_state_waiting
import churchpresentermobile.composeapp.generated.resources.outputs_title
import com.church.presenter.churchpresentermobile.model.MirroringRoute
import com.church.presenter.churchpresentermobile.model.canOpenMirroringSettings
import com.church.presenter.churchpresentermobile.model.isForegroundOnlyServer
import com.church.presenter.churchpresentermobile.model.mirroringRoute
import com.church.presenter.churchpresentermobile.model.openMirroringSettings
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.present.sink.EXTERNAL_DISPLAY_SINK_ID
import com.church.presenter.churchpresentermobile.ui.QrCodeImage
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * Lists every screen the phone can project onto, and what each is doing.
 *
 * Sinks appear even when nothing is connected — an "External display — waiting
 * for a screen" row tells the operator the feature exists and what to do next,
 * where an empty list would read as "not supported".
 *
 * That row is also where AirPlay and Cast surface. Neither can be started from
 * inside the app — the operator has to go out to Control Centre or quick
 * settings — so while the external display is unattached its row carries the
 * platform's own instructions for getting there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutputTargetsSheet(
    sinks: List<SinkStatus>,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.sheetBackground,
    ) {
        OutputTargetsContent(sinks)
    }
}

/**
 * The sheet's list, without the sheet around it.
 *
 * `internal` so the rows can be exercised directly: which row carries the
 * address, and which carries the "go to Control Centre" guidance, is decided
 * per sink and cannot be reached through a `ModalBottomSheet`'s animation.
 */
@Composable
internal fun OutputTargetsContent(sinks: List<SinkStatus>) {
    val colors = LocalAppColors.current
    run {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppDimens.space20, vertical = AppDimens.space8)
                .padding(bottom = AppDimens.space16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            Text(
                text = stringResource(Res.string.outputs_title),
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            if (sinks.isEmpty()) {
                Text(
                    text = stringResource(Res.string.outputs_empty),
                    color = colors.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.testTag(StandaloneTags.OUTPUTS_EMPTY),
                )
                Text(
                    text = stringResource(Res.string.outputs_hint),
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            } else {
                sinks.forEach { status ->
                    SinkRow(status, Modifier.testTag(StandaloneTags.sink(status.id)))
                    // Only the browser sink has an address worth showing; the
                    // external display has nothing for the user to type.
                    status.webUrl()?.let { url ->
                        DisplayUrlCard(url, Modifier.testTag(StandaloneTags.sinkUrl(status.id)))
                    }
                    // Guidance belongs under the row it explains, and only while
                    // that row has nothing connected — once the TV is showing the
                    // slide, instructions for connecting it are just noise.
                    if (status.needsMirroringGuidance()) {
                        MirroringGuidanceCard(Modifier.testTag(StandaloneTags.OUTPUTS_GUIDANCE))
                    }
                }
            }
        }
    }
}

/**
 * True when this row is the external display and it has no screen yet.
 *
 * [MirroringRoute.NONE] platforms are excluded: with nothing to point the
 * operator at, a card would only say "connect a screen" twice.
 */
private fun SinkStatus.needsMirroringGuidance(): Boolean =
    id == EXTERNAL_DISPLAY_SINK_ID &&
        !isAttached &&
        mirroringRoute != MirroringRoute.NONE

/**
 * How to get a TV attached, in the platform's own words.
 *
 * AirPlay and Cast both arrive as an ordinary secondary display, so the sink
 * code does not care which one is in use — but the operator does, because the
 * two live behind completely different system gestures.
 */
@Composable
private fun MirroringGuidanceCard(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(colors.surface)
            .padding(AppDimens.space16),
        verticalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        Text(
            text = stringResource(
                when (mirroringRoute) {
                    MirroringRoute.AIRPLAY -> Res.string.outputs_hint_airplay
                    MirroringRoute.CAST -> Res.string.outputs_hint_cast
                    MirroringRoute.NONE -> Res.string.outputs_hint
                }
            ),
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        if (canOpenMirroringSettings) {
            TextButton(onClick = { openMirroringSettings() }) {
                Icon(
                    imageVector = Icons.Filled.Cast,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(Res.string.outputs_open_cast_settings),
                    color = colors.accent,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = AppDimens.space8),
                )
            }
        }
    }
}

/**
 * The address a browser opens to become a display, shown big enough to read
 * across a room and with a QR for anyone holding a phone.
 */
@Composable
private fun DisplayUrlCard(url: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(colors.surface)
            .padding(AppDimens.space16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        Text(
            text = stringResource(Res.string.outputs_url_label),
            color = colors.muted,
            fontSize = 11.sp,
        )
        Text(
            text = url,
            color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
        )
        QrCodeImage(content = url)
        Text(
            text = stringResource(Res.string.outputs_scan_hint),
            color = colors.muted,
            fontSize = 11.sp,
        )
        if (isForegroundOnlyServer) {
            Text(
                text = stringResource(Res.string.outputs_ios_foreground_warning),
                color = colors.warning,
                fontSize = 11.sp,
            )
        }
    }
}

/** The sink's detail line is a URL only when it is attached and actually serving one. */
private fun SinkStatus.webUrl(): String? =
    detail?.takeIf { isAttached && it.startsWith("http://") }

@Composable
private fun SinkRow(status: SinkStatus, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val tint = when (status.state) {
        SinkState.ATTACHED -> colors.accent
        SinkState.ERROR -> colors.danger
        else -> colors.muted
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(colors.surface)
            .padding(AppDimens.space14),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        Icon(
            imageVector = when (status.state) {
                SinkState.ATTACHED -> Icons.Filled.CastConnected
                SinkState.ERROR -> Icons.Filled.ErrorOutline
                else -> Icons.Filled.Cast
            },
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = status.displayName,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(
                    when (status.state) {
                        SinkState.ATTACHED -> Res.string.outputs_state_attached
                        SinkState.ATTACHING -> Res.string.outputs_state_waiting
                        SinkState.ERROR -> Res.string.outputs_state_error
                        SinkState.DETACHED -> Res.string.outputs_state_off
                    }
                ),
                color = tint,
                fontSize = 11.sp,
            )
        }
        status.detail?.takeIf { it.isNotBlank() }?.let { detail ->
            Text(
                text = detail,
                color = colors.muted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
