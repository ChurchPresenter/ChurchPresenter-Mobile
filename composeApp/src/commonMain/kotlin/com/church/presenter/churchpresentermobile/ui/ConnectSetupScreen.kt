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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.connect_setup_connected
import churchpresentermobile.composeapp.generated.resources.connect_setup_done_button
import churchpresentermobile.composeapp.generated.resources.connect_setup_intro
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
    // Track whether a valid QR was scanned this session
    var scannedHost by remember { mutableStateOf("") }
    var scannedPort by remember { mutableStateOf(0) }
    val connected = scannedHost.isNotBlank()

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

            // ── Step 3 — Scan ──────────────────────────────────────────────────
            ConnectStep(number = "3", title = stringResource(Res.string.connect_setup_step3_title)) {
                Text(
                    text = stringResource(Res.string.connect_setup_step3_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
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
                // Success banner shown after a successful scan
                if (connected) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth(),
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

            OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
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

