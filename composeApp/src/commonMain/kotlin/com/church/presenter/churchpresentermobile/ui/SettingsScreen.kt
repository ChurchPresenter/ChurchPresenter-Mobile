package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.cd_close
import churchpresentermobile.composeapp.generated.resources.settings_active_url_label
import churchpresentermobile.composeapp.generated.resources.settings_api_key_label
import churchpresentermobile.composeapp.generated.resources.settings_api_key_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_device_name_hint
import churchpresentermobile.composeapp.generated.resources.settings_device_name_label
import churchpresentermobile.composeapp.generated.resources.settings_display_name_placeholder
import churchpresentermobile.composeapp.generated.resources.mode_remote_body
import churchpresentermobile.composeapp.generated.resources.mode_remote_title
import churchpresentermobile.composeapp.generated.resources.mode_section_title
import churchpresentermobile.composeapp.generated.resources.mode_standalone_body
import churchpresentermobile.composeapp.generated.resources.mode_standalone_title
import churchpresentermobile.composeapp.generated.resources.mode_switch_cancel
import churchpresentermobile.composeapp.generated.resources.mode_switch_confirm_action
import churchpresentermobile.composeapp.generated.resources.mode_switch_confirm_body
import churchpresentermobile.composeapp.generated.resources.mode_switch_confirm_title
import churchpresentermobile.composeapp.generated.resources.mode_switch_to_remote_body
import churchpresentermobile.composeapp.generated.resources.mode_switch_to_remote_title
import churchpresentermobile.composeapp.generated.resources.settings_appearance_section
import churchpresentermobile.composeapp.generated.resources.settings_cancel
import churchpresentermobile.composeapp.generated.resources.settings_check_status
import churchpresentermobile.composeapp.generated.resources.settings_check_status_description
import churchpresentermobile.composeapp.generated.resources.settings_draft_url_label
import churchpresentermobile.composeapp.generated.resources.settings_developer_section
import churchpresentermobile.composeapp.generated.resources.settings_send_test_error
import churchpresentermobile.composeapp.generated.resources.settings_test_error_sent
import churchpresentermobile.composeapp.generated.resources.settings_host_empty
import churchpresentermobile.composeapp.generated.resources.settings_host_label
import churchpresentermobile.composeapp.generated.resources.settings_host_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_invalid_port
import churchpresentermobile.composeapp.generated.resources.settings_port_label
import churchpresentermobile.composeapp.generated.resources.settings_port_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_privacy_section
import churchpresentermobile.composeapp.generated.resources.settings_reset_to_default
import churchpresentermobile.composeapp.generated.resources.settings_save
import churchpresentermobile.composeapp.generated.resources.settings_server_section
import churchpresentermobile.composeapp.generated.resources.settings_status_bibles
import churchpresentermobile.composeapp.generated.resources.settings_status_mobile_version
import churchpresentermobile.composeapp.generated.resources.settings_status_none
import churchpresentermobile.composeapp.generated.resources.settings_status_recheck
import churchpresentermobile.composeapp.generated.resources.settings_status_server_version
import churchpresentermobile.composeapp.generated.resources.settings_status_songbooks
import churchpresentermobile.composeapp.generated.resources.settings_telemetry_description
import churchpresentermobile.composeapp.generated.resources.settings_telemetry_label
import churchpresentermobile.composeapp.generated.resources.settings_theme_dark
import churchpresentermobile.composeapp.generated.resources.settings_theme_light
import churchpresentermobile.composeapp.generated.resources.settings_theme_system
import churchpresentermobile.composeapp.generated.resources.settings_title
import churchpresentermobile.composeapp.generated.resources.settings_active_server
import churchpresentermobile.composeapp.generated.resources.status_connected
import churchpresentermobile.composeapp.generated.resources.status_connecting
import churchpresentermobile.composeapp.generated.resources.status_error_title
import churchpresentermobile.composeapp.generated.resources.status_limited_functionality
import churchpresentermobile.composeapp.generated.resources.status_not_churchpresenter_body
import churchpresentermobile.composeapp.generated.resources.status_not_churchpresenter_title
import churchpresentermobile.composeapp.generated.resources.status_unauthorized_body
import churchpresentermobile.composeapp.generated.resources.status_unauthorized_title
import churchpresentermobile.composeapp.generated.resources.status_permission_present
import churchpresentermobile.composeapp.generated.resources.status_permission_schedule
import churchpresentermobile.composeapp.generated.resources.status_permission_upload
import churchpresentermobile.composeapp.generated.resources.status_permissions_title
import com.church.presenter.churchpresentermobile.DeepLinkHandler
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.deviceName
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppModeHolder
import com.church.presenter.churchpresentermobile.model.supportsStandalone
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.util.CrashReporting
import com.church.presenter.churchpresentermobile.util.appVersion
import com.church.presenter.churchpresentermobile.util.isDebugBuild
import com.church.presenter.churchpresentermobile.viewmodel.SettingsViewModel
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import com.church.presenter.churchpresentermobile.viewmodel.StatusViewModel
import org.jetbrains.compose.resources.stringResource

// ─────────────────────────────────────────────────────────────────────────────
// Settings screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel(appSettings) }
    val host         by viewModel.host.collectAsState()
    val port         by viewModel.port.collectAsState()
    val apiKey       by viewModel.apiKey.collectAsState()
    val displayName  by viewModel.displayName.collectAsState()
    val hostError    by viewModel.hostError.collectAsState()
    val portError    by viewModel.portError.collectAsState()
    val activeUrl    by viewModel.activeUrl.collectAsState()
    val draftBaseUrl by viewModel.draftBaseUrl.collectAsState()
    val urlChanged   by viewModel.urlChanged.collectAsState()
    val themeMode    by viewModel.themeMode.collectAsState()
    val telemetryEnabled by viewModel.telemetryEnabled.collectAsState()

    // Show/hide state for the API key field
    var apiKeyVisible    by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    // Mode the user has tapped but not yet confirmed. Switching mode redirects
    // where everything projects, so it asks first.
    var pendingMode      by remember { mutableStateOf<AppMode?>(null) }
    var testErrorSent    by remember { mutableStateOf(false) }

    // Inline server-status check
    val statusViewModel: StatusViewModel =
        viewModel(key = "settings_status") { StatusViewModel(appSettings) }

    // Directly observe the global deep-link counter.
    // This fires reliably even when the dialog is open — no token-passing required.
    val deepLinkCount by DeepLinkHandler.appliedCount.collectAsState()
    LaunchedEffect(deepLinkCount) { if (deepLinkCount > 0) viewModel.reloadFromStorage() }

    val emptyHostError   = stringResource(Res.string.settings_host_empty)
    val invalidPortError = stringResource(Res.string.settings_invalid_port)

    val appMode by AppModeHolder.mode.collectAsState()
    // Everything about a server — its address, its key, its status, the QR
    // code that configures it — belongs to a mode that has one. Standalone
    // presents from this device, so the whole block is absent rather than
    // greyed out: the mode selector above brings it straight back.
    val hasDesktop = appMode == AppMode.REMOTE
    pendingMode?.let { target ->
        ModeSwitchDialog(
            target = target,
            onConfirm = { AppModeHolder.set(appSettings, target); pendingMode = null },
            onDismiss = { pendingMode = null },
        )
    }

    if (showStatusDialog) {
        ServerStatusDialog(
            statusViewModel = statusViewModel,
            onDismiss       = { showStatusDialog = false },
        )
    }

    val colors = LocalAppColors.current
    Dialog(
        onDismissRequest = { viewModel.cancel(); onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = false,
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Modal header: Cancel / Settings / Save pill ───────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.settings_cancel),
                        color = colors.muted,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { viewModel.cancel(); onDismiss() }
                    )
                    Text(
                        text = stringResource(Res.string.settings_title),
                        color = colors.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(Res.string.settings_save),
                        color = colors.background,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.text)
                            .clickable {
                                viewModel.save(
                                    onSuccess        = { onSaved(); onDismiss() },
                                    emptyHostError   = emptyHostError,
                                    invalidPortError = invalidPortError,
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }
                HorizontalDivider(color = colors.borderSubtle)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (hasDesktop) {
                        // ── Active-server card ────────────────────────────────────
                        // Shows which server the app is configured to use (the saved
                        // host/port). This is NOT a live connection check — use
                        // "Check status" for that. Labelled + iconed accordingly so it
                        // doesn't read as a live "Connected" indicator.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Dns,
                                contentDescription = null,
                                tint = colors.muted,
                                modifier = Modifier.size(18.dp),
                            )
                            Column {
                                Text(stringResource(Res.string.settings_active_server),
                                    color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(activeUrl, color = colors.muted, fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // ── Mode ──────────────────────────────────────────────────
                    // Only offered where a standalone output sink can exist; the
                    // web build has none, so it never sees a choice it can't honour.
                    if (supportsStandalone) {
                        Text(stringResource(Res.string.mode_section_title),
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                        val modeOptions = listOf(AppMode.REMOTE, AppMode.STANDALONE)
                        SegmentedControl(
                            options = listOf(
                                stringResource(Res.string.mode_remote_title),
                                stringResource(Res.string.mode_standalone_title),
                            ),
                            selectedIndex = modeOptions.indexOf(appMode).coerceAtLeast(0),
                            onSelect = { index ->
                                val target = modeOptions[index]
                                if (target != appMode) pendingMode = target
                            },
                        )
                        Text(
                            text = if (appMode == AppMode.STANDALONE) {
                                stringResource(Res.string.mode_standalone_body)
                            } else {
                                stringResource(Res.string.mode_remote_body)
                            },
                            fontSize = 12.sp,
                            color = colors.muted,
                        )
                        HorizontalDivider(color = colors.borderSubtle)
                    }

                    if (hasDesktop) {
                        // ── Server section header ─────────────────────────────────
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(stringResource(Res.string.settings_server_section),
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                            Text(stringResource(Res.string.settings_reset_to_default),
                                fontSize = 12.sp, color = colors.muted,
                                modifier = Modifier.clickable { viewModel.resetToDefaults() })
                        }

                        SettingsField(
                            label = stringResource(Res.string.settings_host_label),
                            value = host, onValueChange = { viewModel.setHost(it) },
                            placeholder = stringResource(Res.string.settings_host_placeholder),
                            mono = true,
                            keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next,
                            error = hostError,
                        )
                        SettingsField(
                            label = stringResource(Res.string.settings_port_label),
                            value = port, onValueChange = { viewModel.setPort(it) },
                            placeholder = stringResource(Res.string.settings_port_placeholder),
                            mono = true,
                            keyboardType = KeyboardType.Number, imeAction = ImeAction.Next,
                            error = portError,
                        )
                        SettingsField(
                            label = stringResource(Res.string.settings_api_key_label),
                            value = apiKey, onValueChange = { viewModel.setApiKey(it) },
                            placeholder = stringResource(Res.string.settings_api_key_placeholder),
                            password = true, passwordVisible = apiKeyVisible,
                            onTogglePasswordVisible = { apiKeyVisible = !apiKeyVisible },
                            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                        )
                        // The placeholder is what the desktop will be told if this
                        // is left blank, so the operator can see the OS name and
                        // decide whether it is good enough — "iPhone" usually isn't
                        // when there are three in the building.
                        SettingsField(
                            label = stringResource(Res.string.settings_device_name_label),
                            value = displayName, onValueChange = { viewModel.setDisplayName(it) },
                            placeholder = deviceName().ifBlank {
                                stringResource(Res.string.settings_display_name_placeholder)
                            },
                            keyboardType = KeyboardType.Text, imeAction = ImeAction.Done,
                        )
                        Text(
                            text = stringResource(Res.string.settings_device_name_hint),
                            fontSize = 11.sp,
                            color = colors.muted,
                        )

                        // QR scanner (platform button)
                        QrScanButton(onScanned = { url -> DeepLinkHandler.handle(url, appSettings) },
                            modifier = Modifier.fillMaxWidth())

                        // Check Server Status → opens full-screen dialog
                        OutlineActionButton(
                            label = stringResource(Res.string.settings_check_status),
                            icon = Icons.Filled.Wifi,
                            onClick = { statusViewModel.recheck(); showStatusDialog = true },
                        )
                    }

                    // ── Appearance (segmented, drives theme live) ─────────────
                    // The mode block above already closes with a divider, so in
                    // standalone — where the server block between them is absent —
                    // this one would double it up.
                    if (hasDesktop) HorizontalDivider(color = colors.borderSubtle)
                    Text(stringResource(Res.string.settings_appearance_section),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                    val themeOptions = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                    SegmentedControl(
                        options = listOf(
                            stringResource(Res.string.settings_theme_system),
                            stringResource(Res.string.settings_theme_light),
                            stringResource(Res.string.settings_theme_dark),
                        ),
                        selectedIndex = themeOptions.indexOf(themeMode).coerceAtLeast(0),
                        onSelect = { viewModel.setThemeMode(themeOptions[it]) },
                    )

                    // ── Privacy ───────────────────────────────────────────────
                    HorizontalDivider(color = colors.borderSubtle)
                    Text(stringResource(Res.string.settings_privacy_section),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(Res.string.settings_telemetry_label), color = colors.text, fontSize = 14.sp)
                            Text(
                                text = stringResource(Res.string.settings_telemetry_description),
                                fontSize = 12.sp,
                                color = colors.muted,
                            )
                        }
                        Switch(
                            checked = telemetryEnabled,
                            onCheckedChange = { viewModel.setTelemetryEnabled(it) },
                        )
                    }

                    // Draft URL preview
                    if (hasDesktop && urlChanged) {
                        HorizontalDivider(color = colors.borderSubtle)
                        Text(stringResource(Res.string.settings_draft_url_label),
                            fontSize = 9.sp, letterSpacing = 0.05.em, color = colors.muted)
                        Text(
                            text = "$draftBaseUrl/songs", fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                            color = colors.text,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.inputBg)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }

                    // Developer — debug builds only
                    if (isDebugBuild) {
                        HorizontalDivider(color = colors.borderSubtle)
                        Text(stringResource(Res.string.settings_developer_section),
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                        OutlineActionButton(
                            label = stringResource(Res.string.settings_send_test_error),
                            icon = Icons.Filled.Warning,
                            onClick = {
                                CrashReporting.recordException(
                                    RuntimeException("Test error — ChurchPresenter Mobile v$appVersion")
                                )
                                testErrorSent = true
                            },
                        )
                        if (testErrorSent) {
                            Text(
                                text = stringResource(Res.string.settings_test_error_sent),
                                fontSize = 12.sp,
                                color = colors.accent,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Server Status dialog
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerStatusDialog(
    statusViewModel: StatusViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by statusViewModel.uiState.collectAsState()

    val subtitle = when (val s = uiState) {
        is StatusUiState.Loading           -> stringResource(Res.string.status_connecting)
        is StatusUiState.Error             -> stringResource(Res.string.status_error_title)
        is StatusUiState.Unauthorized      -> stringResource(Res.string.status_unauthorized_title)
        is StatusUiState.NotChurchPresenter -> stringResource(Res.string.status_not_churchpresenter_title)
        is StatusUiState.Success -> if (s.warnings.isEmpty())
            stringResource(Res.string.status_connected)
        else
            stringResource(Res.string.status_limited_functionality)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = true,
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(stringResource(Res.string.settings_check_status),
                                    style = MaterialTheme.typography.titleMedium)
                                Text(subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.cd_close))
                            }
                        },
                        actions = {
                            IconButton(onClick = { statusViewModel.recheck() }) {
                                Icon(Icons.Filled.Refresh,
                                    contentDescription = stringResource(Res.string.settings_status_recheck))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor             = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor          = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionIconContentColor     = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (val state = uiState) {

                        // ── Loading ───────────────────────────────────────────
                        is StatusUiState.Loading -> {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                verticalArrangement   = Arrangement.spacedBy(16.dp),
                                horizontalAlignment   = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator()
                                Text(stringResource(Res.string.status_connecting),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // ── Error ─────────────────────────────────────────────
                        is StatusUiState.Error -> {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                verticalArrangement   = Arrangement.spacedBy(12.dp),
                                horizontalAlignment   = Alignment.CenterHorizontally,
                            ) {
                                Icon(Icons.Filled.Warning, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp))
                                Text(stringResource(Res.string.status_error_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center)
                                Text(state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center)
                            }
                            StatusRecheckButton { statusViewModel.recheck() }
                        }

                        // ── Unauthorized (API key rejected) ───────────────────
                        is StatusUiState.Unauthorized -> {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                verticalArrangement   = Arrangement.spacedBy(12.dp),
                                horizontalAlignment   = Alignment.CenterHorizontally,
                            ) {
                                Icon(Icons.Filled.Lock, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp))
                                Text(stringResource(Res.string.status_unauthorized_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center)
                                Text(stringResource(Res.string.status_unauthorized_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center)
                            }
                            StatusRecheckButton { statusViewModel.recheck() }
                        }

                        // ── Not a ChurchPresenter server ──────────────────────
                        is StatusUiState.NotChurchPresenter -> {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                verticalArrangement   = Arrangement.spacedBy(12.dp),
                                horizontalAlignment   = Alignment.CenterHorizontally,
                            ) {
                                Icon(Icons.Filled.Warning, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp))
                                Text(stringResource(Res.string.status_not_churchpresenter_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center)
                                Text(stringResource(Res.string.status_not_churchpresenter_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center)
                            }
                            StatusRecheckButton { statusViewModel.recheck() }
                        }

                        // ── Success ───────────────────────────────────────────
                        is StatusUiState.Success -> {
                            val status   = state.status
                            val warnings = state.warnings

                            // Connection header card
                            val headerColor = if (warnings.isEmpty())
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.tertiaryContainer
                            StatusCard(containerColor = headerColor) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (warnings.isEmpty()) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                        null,
                                        tint = if (warnings.isEmpty()) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Spacer(Modifier.size(12.dp))
                                    Column {
                                        Text(
                                            text = if (warnings.isEmpty())
                                                stringResource(Res.string.status_connected)
                                            else
                                                stringResource(Res.string.status_limited_functionality),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        // Server version — from API response
                                        if (status.appVersion != null) {
                                            Text(
                                                stringResource(Res.string.settings_status_server_version, status.appVersion),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        // Mobile app version
                                        Text(
                                            stringResource(Res.string.settings_status_mobile_version, appVersion),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            // Permissions
                            StatusCard {
                                StatusLabel(stringResource(Res.string.status_permissions_title))
                                Spacer(Modifier.height(8.dp))
                                StatusPermissionRow(stringResource(Res.string.status_permission_present),
                                    status.permissions.canPresent)
                                StatusPermissionRow(stringResource(Res.string.status_permission_schedule),
                                    status.permissions.canAddToSchedule)
                                StatusPermissionRow(stringResource(Res.string.status_permission_upload),
                                    status.permissions.canUploadFiles)
                            }

                            // Content (bibles + songbooks)
                            StatusCard {
                                StatusLabel(stringResource(Res.string.settings_status_bibles))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (status.bibles.isEmpty()) stringResource(Res.string.settings_status_none)
                                           else status.bibles.joinToString("\n") { "• $it" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(12.dp))
                                StatusLabel(stringResource(Res.string.settings_status_songbooks))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (status.songbooks.isEmpty()) stringResource(Res.string.settings_status_none)
                                           else status.songbooks.joinToString("\n") { "• $it" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }


                            // Warnings
                            if (warnings.isNotEmpty()) {
                                StatusCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                    warnings.forEach { warning ->
                                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                            Icon(Icons.Filled.Warning, null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(14.dp).padding(top = 1.dp))
                                            Spacer(Modifier.size(8.dp))
                                            Text(
                                                warning::class.simpleName ?: warning.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                            )
                                        }
                                    }
                                }
                            }

                            StatusRecheckButton { statusViewModel.recheck() }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared composables for the status dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusCard(
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(Modifier.padding(12.dp)) { content() }
    }
}

@Composable
private fun StatusLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun StatusPermissionRow(label: String, granted: Boolean) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning, null,
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = if (granted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
        Text(
            if (granted) "true" else "false",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun StatusRecheckButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(stringResource(Res.string.settings_status_recheck))
    }
}


/**
 * Confirms a mode switch before it takes effect.
 *
 * Switching mode redirects where every projection action goes, which is not
 * something to discover mid-service — so the dialog names the consequence
 * rather than asking a generic "are you sure?".
 */
@Composable
private fun ModeSwitchDialog(
    target: AppMode,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val toStandalone = target == AppMode.STANDALONE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (toStandalone) stringResource(Res.string.mode_switch_confirm_title)
                else stringResource(Res.string.mode_switch_to_remote_title)
            )
        },
        text = {
            Text(
                if (toStandalone) stringResource(Res.string.mode_switch_confirm_body)
                else stringResource(Res.string.mode_switch_to_remote_body)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.mode_switch_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.mode_switch_cancel))
            }
        },
    )
}
