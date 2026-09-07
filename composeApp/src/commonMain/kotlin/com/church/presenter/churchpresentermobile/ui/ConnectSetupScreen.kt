package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.connect_setup_connected
import churchpresentermobile.composeapp.generated.resources.connect_setup_done_button
import churchpresentermobile.composeapp.generated.resources.connect_setup_intro
import churchpresentermobile.composeapp.generated.resources.connect_setup_manual_apply_button
import churchpresentermobile.composeapp.generated.resources.connect_setup_manual_entry_toggle
import churchpresentermobile.composeapp.generated.resources.connect_setup_no_camera_note
import churchpresentermobile.composeapp.generated.resources.connect_setup_scan_instead_toggle
import churchpresentermobile.composeapp.generated.resources.connect_setup_skip_button
import churchpresentermobile.composeapp.generated.resources.connect_setup_skip_note
import churchpresentermobile.composeapp.generated.resources.connect_setup_step1_body
import churchpresentermobile.composeapp.generated.resources.connect_setup_step1_title
import churchpresentermobile.composeapp.generated.resources.connect_setup_step2_body
import churchpresentermobile.composeapp.generated.resources.connect_setup_step2_title
import churchpresentermobile.composeapp.generated.resources.connect_setup_step3_body
import churchpresentermobile.composeapp.generated.resources.connect_setup_step3_title
import churchpresentermobile.composeapp.generated.resources.connect_setup_subtitle
import churchpresentermobile.composeapp.generated.resources.connect_setup_title
import churchpresentermobile.composeapp.generated.resources.settings_host_empty
import churchpresentermobile.composeapp.generated.resources.settings_host_label
import churchpresentermobile.composeapp.generated.resources.settings_host_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_api_key_label
import churchpresentermobile.composeapp.generated.resources.settings_api_key_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_invalid_port
import churchpresentermobile.composeapp.generated.resources.settings_port_label
import churchpresentermobile.composeapp.generated.resources.settings_port_placeholder
import com.church.presenter.churchpresentermobile.model.AppSettings
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen onboarding screen that walks users through scanning the
 * connection QR code from ChurchPresenter desktop to configure server
 * IP, port, and optional API key.
 *
 * @param appSettings Shared [AppSettings] — written directly when QR is scanned.
 * @param onDone      Called when the user taps "Done".
 * @param onSkip      Called when the user taps "Skip for now".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectSetupScreen(
    appSettings: AppSettings,
    onDone: () -> Unit,
    onSkip: () -> Unit,
) {
    // Track whether a valid QR was scanned (or manually applied) this session
    var scannedHost by remember { mutableStateOf("") }
    var scannedPort by remember { mutableStateOf(0) }
    val connected = scannedHost.isNotBlank()

    // Manual entry fallback — shown automatically when no camera is available,
    // or on demand if the user prefers not to scan.
    val cameraAvailable = hasCameraAvailable()
    var showManualEntry by remember(cameraAvailable) { mutableStateOf(!cameraAvailable) }
    var hostInput by remember { mutableStateOf(appSettings.host) }
    var portInput by remember { mutableStateOf(appSettings.port.toString()) }
    var apiKeyInput by remember { mutableStateOf(appSettings.apiKey) }
    var manualHostError by remember { mutableStateOf<String?>(null) }
    var manualPortError by remember { mutableStateOf<String?>(null) }

    val hostEmptyError   = stringResource(Res.string.settings_host_empty)
    val invalidPortError = stringResource(Res.string.settings_invalid_port)

    fun applyManualEntry() {
        val host = hostInput.trim()
        val port = portInput.trim().toIntOrNull()
        manualHostError = if (host.isBlank()) hostEmptyError else null
        manualPortError = if (port == null || port !in 1..65535) invalidPortError else null
        if (manualHostError == null && manualPortError == null && port != null) {
            appSettings.host = host
            appSettings.port = port
            appSettings.apiKey = apiKeyInput.trim()
            scannedHost = host
            scannedPort = port
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.connect_setup_title),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(Res.string.connect_setup_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(
                        modifier = Modifier.testTag(UiTags.CONNECT_SKIP_TOP),
                        onClick = onSkip,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(stringResource(Res.string.connect_setup_skip_button))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Hero intro row ─────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = stringResource(Res.string.connect_setup_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // ── Step 1 ─────────────────────────────────────────────────────────
            ConnectStep(number = "1", title = stringResource(Res.string.connect_setup_step1_title)) {
                Text(
                    text = stringResource(Res.string.connect_setup_step1_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Step 2 ─────────────────────────────────────────────────────────
            ConnectStep(number = "2", title = stringResource(Res.string.connect_setup_step2_title)) {
                Text(
                    text = stringResource(Res.string.connect_setup_step2_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Step 3 — Scan or enter manually ─────────────────────────────────
            ConnectStep(number = "3", title = stringResource(Res.string.connect_setup_step3_title)) {
                Text(
                    text = stringResource(Res.string.connect_setup_step3_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                if (!showManualEntry) {
                    QrScanButton(
                        onScanned = { url ->
                            if (url.lowercase().startsWith("churchpresenter://connect")) {
                                val query = url.substringAfter("?", "")
                                val params = query.split("&").mapNotNull { pair ->
                                    val idx = pair.indexOf('=')
                                    if (idx < 0) null else pair.substring(0, idx).lowercase() to pair.substring(idx + 1)
                                }.toMap()
                                val host = params["host"]?.trim()?.takeIf { it.isNotBlank() }
                                val port = params["port"]?.trim()?.toIntOrNull()
                                if (host != null && port != null && port in 1..65535) {
                                    appSettings.host = host
                                    appSettings.port = port
                                    params["apikey"]?.trim()?.let { appSettings.apiKey = it }
                                    scannedHost = host
                                    scannedPort = port
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { showManualEntry = true }) {
                        Text(stringResource(Res.string.connect_setup_manual_entry_toggle))
                    }
                } else {
                    if (!cameraAvailable) {
                        Text(
                            text = stringResource(Res.string.connect_setup_no_camera_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag(UiTags.CONNECT_NO_CAMERA),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = hostInput, onValueChange = { hostInput = it },
                        modifier = Modifier.fillMaxWidth().testTag(UiTags.CONNECT_HOST),
                        label = { Text(stringResource(Res.string.settings_host_label)) },
                        placeholder = { Text(stringResource(Res.string.settings_host_placeholder)) },
                        isError = manualHostError != null,
                        supportingText = manualHostError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = portInput, onValueChange = { portInput = it },
                        modifier = Modifier.fillMaxWidth().testTag(UiTags.CONNECT_PORT),
                        label = { Text(stringResource(Res.string.settings_port_label)) },
                        placeholder = { Text(stringResource(Res.string.settings_port_placeholder)) },
                        isError = manualPortError != null,
                        supportingText = manualPortError?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKeyInput, onValueChange = { apiKeyInput = it },
                        modifier = Modifier.fillMaxWidth().testTag(UiTags.CONNECT_API_KEY),
                        label = { Text(stringResource(Res.string.settings_api_key_label)) },
                        placeholder = { Text(stringResource(Res.string.settings_api_key_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { applyManualEntry() },
                            modifier = Modifier.testTag(UiTags.CONNECT_APPLY),
                        ) {
                            Text(stringResource(Res.string.connect_setup_manual_apply_button))
                        }
                        if (cameraAvailable) {
                            TextButton(onClick = { showManualEntry = false }) {
                                Text(stringResource(Res.string.connect_setup_scan_instead_toggle))
                            }
                        }
                    }
                }

                // Success banner shown after a successful scan or manual apply
                if (connected) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth().testTag(UiTags.CONNECT_CONNECTED)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(
                                    Res.string.connect_setup_connected,
                                    scannedHost,
                                    scannedPort.toString()
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Done / Skip buttons ────────────────────────────────────────────
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().testTag(UiTags.CONNECT_DONE),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(Res.string.connect_setup_done_button),
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth().testTag(UiTags.CONNECT_SKIP),
            ) {
                Text(stringResource(Res.string.connect_setup_skip_button))
            }

            Text(
                text = stringResource(Res.string.connect_setup_skip_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun ConnectStep(
    number: String,
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
        }
        Column(modifier = Modifier.padding(start = 36.dp)) {
            content()
        }
    }
}

