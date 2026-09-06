package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.settings_api_key_label
import churchpresentermobile.composeapp.generated.resources.settings_api_key_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_host_label
import churchpresentermobile.composeapp.generated.resources.settings_host_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_invalid_host
import churchpresentermobile.composeapp.generated.resources.settings_invalid_port
import churchpresentermobile.composeapp.generated.resources.settings_port_label
import churchpresentermobile.composeapp.generated.resources.settings_port_placeholder
import churchpresentermobile.composeapp.generated.resources.sync_address_hint
import churchpresentermobile.composeapp.generated.resources.sync_address_needs_key
import com.church.presenter.churchpresentermobile.DeepLinkHandler
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DesktopAddress
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.DesktopAddressViewModel
import androidx.compose.runtime.LaunchedEffect
import org.jetbrains.compose.resources.stringResource

/**
 * Where the computer is: host, port, a scanned code, and the API key when one turns out to be
 * needed.
 *
 * Shared by the sync sheet and by standalone Settings so the two cannot disagree about what a
 * valid address is. Owns its own ViewModel — nothing is passed in or out but [settings] and a
 * flag, per the ownership rule.
 *
 * The key field stays hidden until [DesktopAddressViewModel.keyRequired], because most desktops
 * have no key set and a third field asking for a secret reads as a requirement rather than an
 * option. A 401 from a copy attempt reveals it, as does a key already saved.
 *
 * @param showHint The one-line explanation of what this address is for. Off in Settings, where
 *   the section already carries its own explanation.
 */
@Composable
fun DesktopAddressFields(
    settings: AppSettings,
    modifier: Modifier = Modifier,
    showHint: Boolean = true,
) {
    val viewModel: DesktopAddressViewModel = viewModel(key = "desktop_address") {
        DesktopAddressViewModel(settings)
    }
    val colors = LocalAppColors.current

    val host by viewModel.host.collectAsState()
    val port by viewModel.port.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val keyRequired by viewModel.keyRequired.collectAsState()
    val portError by viewModel.portError.collectAsState()
    val hostError by viewModel.hostError.collectAsState()

    // An address changed anywhere else — a scanned code, or the other surface — must not leave
    // a stale draft on screen here.
    val scanned by DeepLinkHandler.appliedCount.collectAsState()
    val edited by DesktopAddress.changeCount.collectAsState()
    LaunchedEffect(scanned, edited) { viewModel.reloadFromStorage() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
            SettingsField(
                label = stringResource(Res.string.settings_host_label),
                value = host,
                onValueChange = { viewModel.setHost(it) },
                placeholder = stringResource(Res.string.settings_host_placeholder),
                mono = true,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
                error = if (hostError) stringResource(Res.string.settings_invalid_host) else null,
                modifier = Modifier.weight(1f).testTag(UiTags.ADDRESS_HOST),
            )
            SettingsField(
                label = stringResource(Res.string.settings_port_label),
                value = port,
                onValueChange = { viewModel.setPort(it) },
                placeholder = stringResource(Res.string.settings_port_placeholder),
                mono = true,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                error = if (portError) stringResource(Res.string.settings_invalid_port) else null,
                modifier = Modifier.width(112.dp).testTag(UiTags.ADDRESS_PORT),
            )
        }

        if (keyRequired) {
            SettingsField(
                label = stringResource(Res.string.settings_api_key_label),
                value = apiKey,
                onValueChange = { viewModel.setApiKey(it) },
                placeholder = stringResource(Res.string.settings_api_key_placeholder),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                modifier = Modifier.testTag(UiTags.ADDRESS_API_KEY),
            )
        } else {
            Text(
                text = stringResource(Res.string.sync_address_needs_key),
                color = colors.accent,
                fontSize = 12.sp,
                modifier = Modifier
                    .testTag(UiTags.ADDRESS_REVEAL_KEY)
                    .clickable { viewModel.revealKeyField() },
            )
        }

        QrScanButton(
            onScanned = { url -> viewModel.applyScannedUrl(url) },
            modifier = Modifier.fillMaxWidth(),
        )

        if (showHint) {
            Text(
                text = stringResource(Res.string.sync_address_hint),
                color = colors.muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.testTag(UiTags.ADDRESS_HINT),
            )
        }
    }
}
