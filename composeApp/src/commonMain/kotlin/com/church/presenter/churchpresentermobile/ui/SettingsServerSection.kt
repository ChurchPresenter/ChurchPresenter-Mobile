package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.qr_scan_button
import churchpresentermobile.composeapp.generated.resources.settings_allowed
import churchpresentermobile.composeapp.generated.resources.settings_api_key_hint
import churchpresentermobile.composeapp.generated.resources.settings_api_key_label
import churchpresentermobile.composeapp.generated.resources.settings_api_key_not_accepted
import churchpresentermobile.composeapp.generated.resources.settings_api_key_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_device_can
import churchpresentermobile.composeapp.generated.resources.settings_draft_url_label
import churchpresentermobile.composeapp.generated.resources.settings_host_label
import churchpresentermobile.composeapp.generated.resources.settings_host_no_answer
import churchpresentermobile.composeapp.generated.resources.settings_host_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_none_found
import churchpresentermobile.composeapp.generated.resources.settings_not_allowed
import churchpresentermobile.composeapp.generated.resources.settings_on_the_computer
import churchpresentermobile.composeapp.generated.resources.settings_or_type_it
import churchpresentermobile.composeapp.generated.resources.settings_port_label
import churchpresentermobile.composeapp.generated.resources.settings_port_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_qr_body
import churchpresentermobile.composeapp.generated.resources.settings_qr_title
import churchpresentermobile.composeapp.generated.resources.settings_qr_title_fastest
import churchpresentermobile.composeapp.generated.resources.settings_reset_to_default
import churchpresentermobile.composeapp.generated.resources.settings_server_address_version
import churchpresentermobile.composeapp.generated.resources.settings_status_address_label
import churchpresentermobile.composeapp.generated.resources.settings_status_bibles
import churchpresentermobile.composeapp.generated.resources.settings_status_recheck
import churchpresentermobile.composeapp.generated.resources.settings_status_songbooks
import churchpresentermobile.composeapp.generated.resources.settings_try_again
import churchpresentermobile.composeapp.generated.resources.status_permission_present
import churchpresentermobile.composeapp.generated.resources.status_permission_schedule
import churchpresentermobile.composeapp.generated.resources.status_permission_upload
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ServerStatus
import com.church.presenter.churchpresentermobile.DeepLinkHandler
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import org.jetbrains.compose.resources.stringResource

/**
 * The server fields as the sheet is editing them.
 *
 * Plain values rather than the ViewModel that holds them, so the form can be
 * drawn by a composable the sheet does not hand its ViewModel to (AGENT.md,
 * "ViewModel ownership"). The errors ride along because the field that shows
 * one is the field that holds the value.
 */
internal data class ServerDraft(
    val host: String,
    val port: String,
    val apiKey: String,
    val customDeviceName: String,
    val displayName: String,
    val hostError: String? = null,
    val portError: String? = null,
)

/** What typing into each of [ServerDraft]'s fields does, and the reset beside them. */
internal class ServerDraftEdits(
    val onHost: (String) -> Unit,
    val onPort: (String) -> Unit,
    val onApiKey: (String) -> Unit,
    val onCustomDeviceName: (String) -> Unit,
    val onDisplayName: (String) -> Unit,
    val onReset: () -> Unit,
)

@Composable
internal fun ServerSection(
    appSettings: AppSettings,
    activeUrl: String,
    draft: ServerDraft,
    edits: ServerDraftEdits,
    draftUrl: String?,
    status: StatusUiState?,
    onCheckStatus: () -> Unit,
    twoPane: Boolean,
) {
    val colors = LocalAppColors.current
    var apiKeyVisible by remember { mutableStateOf(false) }
    val verdict = status?.let { serverVerdict(it) }
    val address = "${appSettings.host}:${appSettings.port}"
    if (verdict != null) {
        StatusCard(
            verdict = verdict,
            address = (status as? StatusUiState.Success)?.status?.appVersion
                ?.let { stringResource(Res.string.settings_server_address_version, address, it) }
                ?: address,
            onRecheck = onCheckStatus,
            twoPane = twoPane,
        )
    } else {
        Text(
            activeUrl,
            color = colors.muted,
            fontSize = 13.5.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 4.dp).testTag(UiTags.SETTINGS_ACTIVE_URL),
        )
    }
    val connected = verdict?.tone == ConnectionTone.GOOD
    val noAnswer = stringResource(Res.string.settings_host_no_answer)
    val keyRefused = stringResource(Res.string.settings_api_key_not_accepted)
    if (hasCameraAvailable()) {
        QrCard(
            tint = colors.accent,
            prominent = !connected,
            twoPane = twoPane,
            onScanned = { url -> DeepLinkHandler.handle(url, appSettings) },
        )
        if (!connected) OrTypeIt()
    }
    Column {
        GroupLabel(
            stringResource(Res.string.settings_status_address_label),
            modifier = Modifier.testTag(UiTags.SETTINGS_SERVER_SECTION),
        ) {
            Text(
                stringResource(Res.string.settings_reset_to_default),
                color = colors.accent,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag(UiTags.SETTINGS_RESET).clickable { edits.onReset() },
            )
        }
        LookCard(padding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LabelledField(
                    label = stringResource(Res.string.settings_host_label),
                    value = draft.host,
                    onValueChange = edits.onHost,
                    placeholder = stringResource(Res.string.settings_host_placeholder),
                    mono = true,
                    keyboardType = KeyboardType.Uri,
                    error = draft.hostError
                        ?: noAnswer.takeIf { verdict?.blame == FieldBlame.HOST },
                    fieldModifier = Modifier.testTag(UiTags.SETTINGS_HOST),
                    modifier = Modifier.weight(2f),
                )
                LabelledField(
                    label = stringResource(Res.string.settings_port_label),
                    value = draft.port,
                    onValueChange = edits.onPort,
                    placeholder = stringResource(Res.string.settings_port_placeholder),
                    mono = true,
                    keyboardType = KeyboardType.Number,
                    error = draft.portError,
                    fieldModifier = Modifier.testTag(UiTags.SETTINGS_PORT),
                    modifier = Modifier.weight(1f),
                )
            }
            LabelledField(
                label = stringResource(Res.string.settings_api_key_label),
                value = draft.apiKey,
                onValueChange = edits.onApiKey,
                placeholder = stringResource(Res.string.settings_api_key_placeholder),
                password = true,
                passwordVisible = apiKeyVisible,
                onTogglePasswordVisible = { apiKeyVisible = !apiKeyVisible },
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                hint = stringResource(Res.string.settings_api_key_hint),
                error = keyRefused.takeIf { verdict?.blame == FieldBlame.KEY },
                fieldModifier = Modifier.testTag(UiTags.SETTINGS_API_KEY),
            )
        }
        if (draftUrl != null) DraftUrlPreview(draftUrl)
    }
    val serverStatus = (status as? StatusUiState.Success)?.status
    if (serverStatus != null) {
        PermissionsCard(serverStatus)
        LibraryCard(serverStatus)
    }
}

@Composable
private fun StatusCard(
    verdict: ServerVerdict,
    address: String,
    onRecheck: () -> Unit,
    twoPane: Boolean,
) {
    val colors = LocalAppColors.current
    val tone = colors.toneColor(verdict.tone)
    val shape = RoundedCornerShape(18.dp)
    val body: @Composable (Modifier) -> Unit = { modifier ->
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.padding(top = 1.dp)) { StatusDot(tone, size = 14.dp) }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(verdict.title),
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).em,
                )
                Text(
                    address,
                    color = colors.muted,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 5.dp).testTag(UiTags.SETTINGS_ACTIVE_URL),
                )
                if (verdict.reasons.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        verdict.reasons.forEach { reason ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    Modifier.padding(top = 8.dp).size(5.dp).clip(CircleShape).background(tone),
                                )
                                Text(
                                    stringResource(reason),
                                    color = colors.secondary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    val button: @Composable () -> Unit = {
        val label = stringResource(
            if (verdict.retry) Res.string.settings_try_again else Res.string.settings_status_recheck,
        )
        val tag = Modifier.testTag(UiTags.SETTINGS_CHECK_STATUS)
        if (verdict.tone == ConnectionTone.BAD) {
            TintButton(label = label, tint = tone, onClick = onRecheck, modifier = tag)
        } else {
            PlainButton(label = label, onClick = onRecheck, modifier = tag)
        }
    }
    val container = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(tone.copy(alpha = 0.1f))
        .border(1.dp, tone.copy(alpha = 0.35f), shape)
        .padding(horizontal = 22.dp, vertical = 20.dp)
    if (twoPane) {
        Row(modifier = container, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            body(Modifier.weight(1f))
            button()
        }
    } else {
        Column(modifier = container, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            body(Modifier)
            button()
        }
    }
}

@Composable
internal fun QrCard(
    tint: Color,
    prominent: Boolean,
    twoPane: Boolean,
    onScanned: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(18.dp)
    val frame = if (prominent) {
        Modifier.clip(shape).background(tint.copy(alpha = 0.12f)).border(1.5.dp, tint.copy(alpha = 0.45f), shape)
    } else {
        Modifier
            .clip(shape)
            .background(tint.copy(alpha = 0.06f))
            .drawBehind {
                val w = 1.5.dp.toPx()
                drawRoundRect(
                    color = tint.copy(alpha = 0.45f),
                    topLeft = Offset(w / 2, w / 2),
                    size = Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            }
    }
    val text: @Composable (Modifier) -> Unit = { modifier ->
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconTile(Icons.Filled.QrCodeScanner, tint, size = 52.dp, radius = 15.dp, iconSize = 26.dp, alpha = 0.16f)
            Column {
                Text(
                    stringResource(
                        if (prominent) Res.string.settings_qr_title_fastest else Res.string.settings_qr_title,
                    ),
                    color = colors.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).em,
                )
                Text(
                    stringResource(Res.string.settings_qr_body),
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
    val scan: @Composable (Modifier) -> Unit = { modifier ->
        QrScanButton(onScanned = onScanned) { launch ->
            TintButton(
                label = stringResource(Res.string.qr_scan_button),
                tint = tint,
                onClick = launch,
                height = 44.dp,
                fontSize = 15.sp,
                leading = Icons.Filled.QrCodeScanner,
                modifier = modifier,
            )
        }
    }
    if (twoPane) {
        Row(
            modifier = Modifier.fillMaxWidth().then(frame).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            text(Modifier.weight(1f))
            scan(Modifier)
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().then(frame).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            text(Modifier)
            scan(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun OrTypeIt() {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(colors.border))
        Eyebrow(stringResource(Res.string.settings_or_type_it), colors.muted, size = 12.sp)
        Box(Modifier.weight(1f).height(1.dp).background(colors.border))
    }
}

@Composable
private fun PermissionsCard(status: ServerStatus) {
    Column {
        GroupLabel(stringResource(Res.string.settings_device_can))
        LookCard {
            PermissionRow(stringResource(Res.string.status_permission_present), status.permissions.canPresent)
            LookDivider()
            PermissionRow(stringResource(Res.string.status_permission_schedule), status.permissions.canAddToSchedule)
            LookDivider()
            PermissionRow(stringResource(Res.string.status_permission_upload), status.permissions.canUploadFiles)
        }
    }
}

@Composable
private fun PermissionRow(label: String, allowed: Boolean) {
    val colors = LocalAppColors.current
    val tone = if (allowed) colors.accent else colors.danger
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(
            if (allowed) Icons.Filled.Check else Icons.Filled.Close,
            tone,
            size = 24.dp,
            radius = 12.dp,
            iconSize = 14.dp,
            alpha = 0.16f,
        )
        Text(
            label,
            color = colors.secondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(if (allowed) Res.string.settings_allowed else Res.string.settings_not_allowed),
            color = tone,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LibraryCard(status: ServerStatus) {
    Column {
        GroupLabel(stringResource(Res.string.settings_on_the_computer))
        LookCard {
            LibraryRow(stringResource(Res.string.settings_status_bibles), status.bibles)
            LookDivider()
            LibraryRow(stringResource(Res.string.settings_status_songbooks), status.songbooks)
        }
    }
}

@Composable
private fun LibraryRow(label: String, names: List<String>) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                label,
                color = colors.secondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (names.isEmpty()) {
                Text(
                    stringResource(Res.string.settings_none_found),
                    color = colors.danger,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        if (names.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                names.forEach { name ->
                    Text(
                        name,
                        color = colors.secondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceStrong)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftUrlPreview(draftBaseUrl: String) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp)) {
        Text(stringResource(Res.string.settings_draft_url_label), fontSize = 12.sp, color = colors.muted)
        Text(
            text = "$draftBaseUrl/songs",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = colors.text,
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .testTag(UiTags.SETTINGS_DRAFT_URL)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.inputBg)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
