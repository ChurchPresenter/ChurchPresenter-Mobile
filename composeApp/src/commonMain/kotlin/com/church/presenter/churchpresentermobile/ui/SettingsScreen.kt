package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.settings_api_key_label
import churchpresentermobile.composeapp.generated.resources.settings_api_key_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_appearance_section
import churchpresentermobile.composeapp.generated.resources.settings_cancel
import churchpresentermobile.composeapp.generated.resources.settings_host_empty
import churchpresentermobile.composeapp.generated.resources.settings_host_label
import churchpresentermobile.composeapp.generated.resources.settings_host_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_invalid_port
import churchpresentermobile.composeapp.generated.resources.settings_port_label
import churchpresentermobile.composeapp.generated.resources.settings_port_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_save
import churchpresentermobile.composeapp.generated.resources.settings_server_section
import churchpresentermobile.composeapp.generated.resources.settings_theme_dark
import churchpresentermobile.composeapp.generated.resources.settings_theme_light
import churchpresentermobile.composeapp.generated.resources.settings_theme_system
import churchpresentermobile.composeapp.generated.resources.settings_title
import androidx.lifecycle.viewmodel.compose.viewModel
import com.church.presenter.churchpresentermobile.DeepLinkHandler
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen settings screen shown as a dialog.
 *
 * @param appSettings Shared [AppSettings] used to create and configure [SettingsViewModel].
 * @param onDismiss Called when the screen should be closed (Cancel or successful Save).
 * @param onSaved Called after settings are validated and persisted successfully.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onCertSetup: (() -> Unit)? = null,
) {
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel(appSettings) }
    val host by viewModel.host.collectAsState()
    val port by viewModel.port.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val hostError by viewModel.hostError.collectAsState()
    val portError by viewModel.portError.collectAsState()
    val activeUrl by viewModel.activeUrl.collectAsState()
    val draftBaseUrl by viewModel.draftBaseUrl.collectAsState()
    val urlChanged by viewModel.urlChanged.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    // Show/hide state for the API key field
    var apiKeyVisible by remember { mutableStateOf(false) }

    // Directly observe the global deep-link counter.
    // This fires reliably even when the dialog is open — no token-passing required.
    val deepLinkCount by DeepLinkHandler.appliedCount.collectAsState()
    LaunchedEffect(deepLinkCount) {
        if (deepLinkCount > 0) viewModel.reloadFromStorage()
    }

    val emptyHostError = stringResource(Res.string.settings_host_empty)
    val invalidPortError = stringResource(Res.string.settings_invalid_port)


    Dialog(
        onDismissRequest = {
            viewModel.cancel()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(Res.string.settings_title)) },
                        navigationIcon = {
                            TextButton(
                                onClick = {
                                    viewModel.cancel()
                                    onDismiss()
                                }
                            ) {
                                Text(stringResource(Res.string.settings_cancel))
                            }
                        },
                        actions = {
                            Button(
                                onClick = {
                                    viewModel.save(
                                        onSuccess = {
                                            onSaved()
                                            onDismiss()
                                        },
                                        emptyHostError = emptyHostError,
                                        invalidPortError = invalidPortError
                                    )
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(stringResource(Res.string.settings_save))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Active URL ────────────────────────────────────────
                    Text(
                        text = "Currently connecting to:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$activeUrl/songs",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    // ── Server section ────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_server_section),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { viewModel.resetToDefaults() }) {
                            Text(
                                text = "Reset to default",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    HorizontalDivider()

                    // Host field
                    OutlinedTextField(
                        value = host,
                        onValueChange = { viewModel.setHost(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.settings_host_label)) },
                        placeholder = { Text(stringResource(Res.string.settings_host_placeholder)) },
                        isError = hostError != null,
                        supportingText = hostError?.let { msg ->
                            { Text(msg, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Port field
                    OutlinedTextField(
                        value = port,
                        onValueChange = { viewModel.setPort(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.settings_port_label)) },
                        placeholder = { Text(stringResource(Res.string.settings_port_placeholder)) },
                        isError = portError != null,
                        supportingText = portError?.let { msg ->
                            { Text(msg, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )

                    // API Key field
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { viewModel.setApiKey(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.settings_api_key_label)) },
                        placeholder = { Text(stringResource(Res.string.settings_api_key_placeholder)) },
                        singleLine = true,
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    // QR scanner — fills in host, port and API key from a churchpresenter:// QR code
                    QrScanButton(
                        onScanned = { url -> DeepLinkHandler.handle(url, appSettings) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── Appearance ────────────────────────────────────────
                    HorizontalDivider()

                    Text(
                        text = stringResource(Res.string.settings_appearance_section),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    ThemeModeSelector(
                        selected = themeMode,
                        onSelect = { viewModel.setThemeMode(it) }
                    )

                    // ── Certificate Setup ─────────────────────────────────
                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Certificate Trust",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (appSettings.isCertTrusted) "✓ Certificate installed" else "Not yet installed",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (appSettings.isCertTrusted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (onCertSetup != null) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.cancel()
                                    onDismiss()
                                    onCertSetup()
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = if (appSettings.isCertTrusted) "Re-run setup" else "Setup",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    // ── Draft URL preview ─────────────────────────────────
                    if (urlChanged) {
                        HorizontalDivider()
                        Text(
                            text = "Will connect to after saving:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$draftBaseUrl/songs",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A row of three buttons for picking between System, Light, and Dark theme modes.
 *
 * @param selected The currently active [ThemeMode].
 * @param onSelect Called when the user taps a different mode.
 */
@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val options = listOf(
        ThemeMode.SYSTEM to stringResource(Res.string.settings_theme_system),
        ThemeMode.LIGHT to stringResource(Res.string.settings_theme_light),
        ThemeMode.DARK to stringResource(Res.string.settings_theme_dark),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (mode, label) ->
            val isSelected = selected == mode
            if (isSelected) {
                Button(
                    onClick = { onSelect(mode) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(mode) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
