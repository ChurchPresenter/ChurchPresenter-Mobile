package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.clickable
import churchpresentermobile.composeapp.generated.resources.settings_draft_url_label
import churchpresentermobile.composeapp.generated.resources.settings_active_server
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Dns
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.settings_api_key_label
import churchpresentermobile.composeapp.generated.resources.settings_api_key_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_check_status
import churchpresentermobile.composeapp.generated.resources.settings_host_label
import churchpresentermobile.composeapp.generated.resources.settings_host_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_port_label
import churchpresentermobile.composeapp.generated.resources.settings_port_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_reset_to_default
import com.church.presenter.churchpresentermobile.DeepLinkHandler
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
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

/**
 * The desktop's address and key: which server the app is using now, the
 * fields that change it, and the two ways to check the answer.
 *
 * @param twoPane Host beside port — the tablet's arrangement, where a field
 *   the width of the pane is mostly empty. The fields are the same; [Paired]
 *   only decides whether they share a row.
 * @param draftUrl The base URL the fields currently spell, or null when it is
 *   the one already saved — the preview only appears while they differ.
 */
@Composable
internal fun ServerSection(
    appSettings: AppSettings,
    activeUrl: String,
    draft: ServerDraft,
    edits: ServerDraftEdits,
    draftUrl: String?,
    onCheckStatus: () -> Unit,
    twoPane: Boolean,
) {
    val colors = LocalAppColors.current
    // Show/hide state for the API key field
    var apiKeyVisible by remember { mutableStateOf(false) }
    ActiveServerCard(activeUrl)
    SectionTitle(SettingsSection.SERVER.title, tag = UiTags.SETTINGS_SERVER_SECTION) {
        Text(stringResource(Res.string.settings_reset_to_default),
            fontSize = 12.sp, color = colors.muted,
            modifier = Modifier
                .testTag(UiTags.SETTINGS_RESET)
                .clickable { edits.onReset() })
    }
    Paired(
        sideBySide = twoPane,
        first = { modifier ->
            SettingsField(
                label = stringResource(Res.string.settings_host_label),
                value = draft.host, onValueChange = edits.onHost,
                placeholder = stringResource(Res.string.settings_host_placeholder),
                modifier = modifier.testTag(UiTags.SETTINGS_HOST),
                mono = true,
                keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next,
                error = draft.hostError,
            )
        },
        second = { modifier ->
            SettingsField(
                label = stringResource(Res.string.settings_port_label),
                value = draft.port, onValueChange = edits.onPort,
                placeholder = stringResource(Res.string.settings_port_placeholder),
                modifier = modifier.testTag(UiTags.SETTINGS_PORT),
                mono = true,
                keyboardType = KeyboardType.Number, imeAction = ImeAction.Next,
                error = draft.portError,
            )
        },
    )
    SettingsField(
        label = stringResource(Res.string.settings_api_key_label),
        value = draft.apiKey, onValueChange = edits.onApiKey,
        placeholder = stringResource(Res.string.settings_api_key_placeholder),
        modifier = Modifier.testTag(UiTags.SETTINGS_API_KEY),
        password = true, passwordVisible = apiKeyVisible,
        onTogglePasswordVisible = { apiKeyVisible = !apiKeyVisible },
        keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
    )
    if (draftUrl != null) DraftUrlPreview(draftUrl)

    // Check Server Status → opens full-screen dialog
    OutlineActionButton(
        label = stringResource(Res.string.settings_check_status),
        icon = Icons.Filled.Wifi,
        onClick = onCheckStatus,
        modifier = Modifier.testTag(UiTags.SETTINGS_CHECK_STATUS),
    )
    // QR scanner (platform button)
    QrScanButton(onScanned = { url -> DeepLinkHandler.handle(url, appSettings) },
        modifier = Modifier.fillMaxWidth())
}

/**
 * Shows which server the app is configured to use (the saved host/port). This
 * is NOT a live connection check — the connection line in the menu is that.
 * Labelled and iconed accordingly so it doesn't read as a live "Connected"
 * indicator.
 */
@Composable
private fun ActiveServerCard(activeUrl: String) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Dns,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(stringResource(Res.string.settings_active_server),
                color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(activeUrl, color = colors.muted, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag(UiTags.SETTINGS_ACTIVE_URL))
        }
    }
}

@Composable
private fun DraftUrlPreview(draftBaseUrl: String) {
    val colors = LocalAppColors.current
    Text(stringResource(Res.string.settings_draft_url_label),
        fontSize = 9.sp, letterSpacing = 0.05.em, color = colors.muted)
    Text(
        text = "$draftBaseUrl/songs", fontSize = 11.sp, fontFamily = FontFamily.Monospace,
        color = colors.text,
        modifier = Modifier.fillMaxWidth()
            .testTag(UiTags.SETTINGS_DRAFT_URL)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.inputBg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/**
 * Two fields on one row, or one under the other.
 *
 * The slot takes the modifier because a weight only exists inside the row: the
 * caller's field cannot ask for one, so the row hands it one when there is a
 * row and nothing when there is not.
 */
@Composable
private fun Paired(
    sideBySide: Boolean,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    if (sideBySide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    } else {
        first(Modifier)
        second(Modifier)
    }
}
