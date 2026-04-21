package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.settings_active_url_label
import churchpresentermobile.composeapp.generated.resources.settings_api_key_label
import churchpresentermobile.composeapp.generated.resources.settings_api_key_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_appearance_section
import churchpresentermobile.composeapp.generated.resources.settings_cancel
import churchpresentermobile.composeapp.generated.resources.settings_check_status
import churchpresentermobile.composeapp.generated.resources.settings_check_status_description
import churchpresentermobile.composeapp.generated.resources.settings_draft_url_label
import churchpresentermobile.composeapp.generated.resources.settings_host_empty
import churchpresentermobile.composeapp.generated.resources.settings_host_label
import churchpresentermobile.composeapp.generated.resources.settings_host_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_invalid_port
import churchpresentermobile.composeapp.generated.resources.settings_port_label
import churchpresentermobile.composeapp.generated.resources.settings_port_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_reset_to_default
import churchpresentermobile.composeapp.generated.resources.settings_save
import churchpresentermobile.composeapp.generated.resources.settings_server_section
import churchpresentermobile.composeapp.generated.resources.settings_status_bibles
import churchpresentermobile.composeapp.generated.resources.settings_status_mobile_version
import churchpresentermobile.composeapp.generated.resources.settings_status_none
import churchpresentermobile.composeapp.generated.resources.settings_status_recheck
import churchpresentermobile.composeapp.generated.resources.settings_status_server_version
import churchpresentermobile.composeapp.generated.resources.settings_status_songbooks
import churchpresentermobile.composeapp.generated.resources.settings_theme_dark
import churchpresentermobile.composeapp.generated.resources.settings_theme_light
import churchpresentermobile.composeapp.generated.resources.settings_theme_system
import churchpresentermobile.composeapp.generated.resources.settings_title
import churchpresentermobile.composeapp.generated.resources.status_connected
import churchpresentermobile.composeapp.generated.resources.status_connecting
import churchpresentermobile.composeapp.generated.resources.status_error_title
import churchpresentermobile.composeapp.generated.resources.status_limited_functionality
import churchpresentermobile.composeapp.generated.resources.status_permission_present
import churchpresentermobile.composeapp.generated.resources.status_permission_schedule
import churchpresentermobile.composeapp.generated.resources.status_permission_upload
import churchpresentermobile.composeapp.generated.resources.status_permissions_title
import com.church.presenter.churchpresentermobile.DeepLinkHandler
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.util.appVersion
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
    val hostError    by viewModel.hostError.collectAsState()
    val portError    by viewModel.portError.collectAsState()
    val activeUrl    by viewModel.activeUrl.collectAsState()
    val draftBaseUrl by viewModel.draftBaseUrl.collectAsState()
    val urlChanged   by viewModel.urlChanged.collectAsState()
    val themeMode    by viewModel.themeMode.collectAsState()

    // Show/hide state for the API key field
    var apiKeyVisible    by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }

    // Inline server-status check
    val statusViewModel: StatusViewModel =
        viewModel(key = "settings_status") { StatusViewModel(appSettings) }

    // Directly observe the global deep-link counter.
    // This fires reliably even when the dialog is open — no token-passing required.
    val deepLinkCount by DeepLinkHandler.appliedCount.collectAsState()
    LaunchedEffect(deepLinkCount) { if (deepLinkCount > 0) viewModel.reloadFromStorage() }

    val emptyHostError   = stringResource(Res.string.settings_host_empty)
    val invalidPortError = stringResource(Res.string.settings_invalid_port)

    if (showStatusDialog) {
        ServerStatusDialog(
            statusViewModel = statusViewModel,
            onDismiss       = { showStatusDialog = false },
        )
    }

    Dialog(
        onDismissRequest = { viewModel.cancel(); onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = false,
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(Res.string.settings_title)) },
                        navigationIcon = {
                            TextButton(onClick = { viewModel.cancel(); onDismiss() }) {
                                Text(stringResource(Res.string.settings_cancel))
                            }
                        },
                        actions = {
                            Button(
                                onClick = {
                                    viewModel.save(
                                        onSuccess        = { onSaved(); onDismiss() },
                                        emptyHostError   = emptyHostError,
                                        invalidPortError = invalidPortError,
                                    )
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) { Text(stringResource(Res.string.settings_save)) }
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Active URL
                    Text(stringResource(Res.string.settings_active_url_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$activeUrl/songs", fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )

                    // Server section header
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(stringResource(Res.string.settings_server_section),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { viewModel.resetToDefaults() }) {
                            Text(stringResource(Res.string.settings_reset_to_default),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    HorizontalDivider()

                    // Host
                    OutlinedTextField(
                        value = host, onValueChange = { viewModel.setHost(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.settings_host_label)) },
                        placeholder = { Text(stringResource(Res.string.settings_host_placeholder)) },
                        isError = hostError != null,
                        supportingText = hostError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    )

                    // Port
                    OutlinedTextField(
                        value = port, onValueChange = { viewModel.setPort(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.settings_port_label)) },
                        placeholder = { Text(stringResource(Res.string.settings_port_placeholder)) },
                        isError = portError != null,
                        supportingText = portError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    )

                    // API Key
                    OutlinedTextField(
                        value = apiKey, onValueChange = { viewModel.setApiKey(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.settings_api_key_label)) },
                        placeholder = { Text(stringResource(Res.string.settings_api_key_placeholder)) },
                        singleLine = true,
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(if (apiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                            }
                        },
                    )

                    // QR scanner
                    QrScanButton(onScanned = { url -> DeepLinkHandler.handle(url, appSettings) },
                        modifier = Modifier.fillMaxWidth())

                    // Check Server Status → opens full-screen dialog
                    OutlinedButton(
                        onClick = { statusViewModel.recheck(); showStatusDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Wifi, stringResource(Res.string.settings_check_status_description),
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(Res.string.settings_check_status))
                    }

                    // Appearance
                    HorizontalDivider()
                    Text(stringResource(Res.string.settings_appearance_section),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    ThemeModeSelector(selected = themeMode, onSelect = { viewModel.setThemeMode(it) })

                    // Draft URL preview
                    if (urlChanged) {
                        HorizontalDivider()
                        Text(stringResource(Res.string.settings_draft_url_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$draftBaseUrl/songs", fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
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
        is StatusUiState.Loading -> stringResource(Res.string.status_connecting)
        is StatusUiState.Error   -> stringResource(Res.string.status_error_title)
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
                                Icon(Icons.Filled.Close, contentDescription = "Close")
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

// ─────────────────────────────────────────────────────────────────────────────
// Theme mode selector
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to stringResource(Res.string.settings_theme_system),
        ThemeMode.LIGHT  to stringResource(Res.string.settings_theme_light),
        ThemeMode.DARK   to stringResource(Res.string.settings_theme_dark),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (mode, label) ->
            if (selected == mode) {
                Button(onClick = { onSelect(mode) }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary,
                    )
                ) { Text(label, style = MaterialTheme.typography.labelMedium) }
            } else {
                OutlinedButton(onClick = { onSelect(mode) }, modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
