package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.status_connected
import churchpresentermobile.composeapp.generated.resources.status_connecting
import churchpresentermobile.composeapp.generated.resources.status_continue
import churchpresentermobile.composeapp.generated.resources.status_continue_anyway
import churchpresentermobile.composeapp.generated.resources.status_endpoint_unavailable
import churchpresentermobile.composeapp.generated.resources.status_error_title
import churchpresentermobile.composeapp.generated.resources.status_info_bibles
import churchpresentermobile.composeapp.generated.resources.status_info_features
import churchpresentermobile.composeapp.generated.resources.status_info_songbooks
import churchpresentermobile.composeapp.generated.resources.status_issues_detected
import churchpresentermobile.composeapp.generated.resources.status_limited_functionality
import churchpresentermobile.composeapp.generated.resources.status_open_settings
import churchpresentermobile.composeapp.generated.resources.status_permission_blocked
import churchpresentermobile.composeapp.generated.resources.status_permission_present
import churchpresentermobile.composeapp.generated.resources.status_permission_schedule
import churchpresentermobile.composeapp.generated.resources.status_permission_upload
import churchpresentermobile.composeapp.generated.resources.status_permissions_title
import churchpresentermobile.composeapp.generated.resources.status_retry
import churchpresentermobile.composeapp.generated.resources.status_server_version
import churchpresentermobile.composeapp.generated.resources.status_warn_missing_endpoint_body
import churchpresentermobile.composeapp.generated.resources.status_warn_missing_endpoint_title
import churchpresentermobile.composeapp.generated.resources.status_warn_no_api_key_body
import churchpresentermobile.composeapp.generated.resources.status_warn_no_api_key_title
import churchpresentermobile.composeapp.generated.resources.status_warn_no_bibles_body
import churchpresentermobile.composeapp.generated.resources.status_warn_no_bibles_title
import churchpresentermobile.composeapp.generated.resources.status_warn_no_songbooks_body
import churchpresentermobile.composeapp.generated.resources.status_warn_no_songbooks_title
import churchpresentermobile.composeapp.generated.resources.status_warn_present_blocked_body
import churchpresentermobile.composeapp.generated.resources.status_warn_present_blocked_title
import churchpresentermobile.composeapp.generated.resources.status_warn_schedule_blocked_body
import churchpresentermobile.composeapp.generated.resources.status_warn_schedule_blocked_title
import churchpresentermobile.composeapp.generated.resources.status_warn_unknown_version_body
import churchpresentermobile.composeapp.generated.resources.status_warn_unknown_version_title
import churchpresentermobile.composeapp.generated.resources.status_warn_upload_blocked_body
import churchpresentermobile.composeapp.generated.resources.status_warn_upload_blocked_title
import com.church.presenter.churchpresentermobile.model.DevicePermissions
import com.church.presenter.churchpresentermobile.model.StatusWarning
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import com.church.presenter.churchpresentermobile.viewmodel.StatusViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Startup status / compatibility screen.
 *
 * Shown once after the splash screen. If the server responds with no warnings it
 * auto-advances after 3 seconds. If there are warnings or an error the user
 * must explicitly tap Continue (or Open Settings).
 *
 * @param viewModel      The [StatusViewModel] that owns the network call.
 * @param onContinue     Called when the user (or the auto-advance timer) dismisses the screen.
 * @param onOpenSettings Called when the user taps "Open Settings" from the error state.
 */
@Composable
fun StatusScreen(
    viewModel: StatusViewModel,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    // Auto-advance when there are no warnings — hold for 3 seconds so users
    // can read the permissions summary before being moved on automatically.
    LaunchedEffect(uiState) {
        if (uiState is StatusUiState.Success) {
            val warnings = (uiState as StatusUiState.Success).warnings
            if (warnings.isEmpty()) {
                delay(3000)
                onContinue()
            }
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = uiState) {
                is StatusUiState.Loading -> LoadingContent()
                is StatusUiState.Error   -> ErrorContent(
                    message        = state.message,
                    onRetry        = { viewModel.recheck() },
                    onContinue     = onContinue,
                    onOpenSettings = onOpenSettings,
                )
                is StatusUiState.Success -> {
                    if (state.warnings.isEmpty()) {
                        AllGoodContent(
                            permissions       = state.status.permissions,
                            endpointAvailable = state.status.endpointAvailable,
                            onContinue        = onContinue,
                        )
                    } else {
                        WarningsContent(
                            appVersion     = state.status.appVersion,
                            warnings       = state.warnings,
                            permissions    = state.status.permissions,
                            bibles         = state.status.bibles,
                            songbooks      = state.status.songbooks,
                            features       = state.status.features,
                            onContinue     = onContinue,
                            onOpenSettings = onOpenSettings,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.status_connecting), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AllGoodContent(
    permissions: DevicePermissions = DevicePermissions(),
    endpointAvailable: Boolean = true,
    onContinue: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.status_connected), style = MaterialTheme.typography.titleMedium)
        if (!endpointAvailable) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.status_endpoint_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            Spacer(Modifier.height(16.dp))
            PermissionsSummaryCard(permissions)
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.status_continue))
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.status_error_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.status_retry))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.status_open_settings))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onContinue) {
            Text(stringResource(Res.string.status_continue_anyway))
        }
    }
}

@Composable
private fun WarningsContent(
    appVersion: String?,
    warnings: List<StatusWarning>,
    permissions: DevicePermissions,
    bibles: List<String>,
    songbooks: List<String>,
    features: List<String>,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.status_limited_functionality),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (appVersion != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.status_server_version, appVersion),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        PermissionsSummaryCard(permissions)
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.status_issues_detected),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        warnings.forEach { warning ->
            WarningCard(warning)
            Spacer(Modifier.height(8.dp))
        }
        if (bibles.isNotEmpty()) {
            InfoCard(
                icon  = Icons.Filled.Info,
                title = stringResource(Res.string.status_info_bibles),
                body  = bibles.joinToString(", "),
            )
            Spacer(Modifier.height(8.dp))
        }
        if (songbooks.isNotEmpty()) {
            InfoCard(
                icon  = Icons.Filled.Info,
                title = stringResource(Res.string.status_info_songbooks),
                body  = songbooks.joinToString(", "),
            )
            Spacer(Modifier.height(8.dp))
        }
        if (features.isNotEmpty()) {
            InfoCard(
                icon  = Icons.Filled.Info,
                title = stringResource(Res.string.status_info_features),
                body  = features.joinToString(", "),
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.status_continue))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onOpenSettings) {
            Text(stringResource(Res.string.status_open_settings))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WarningCard(warning: StatusWarning) {
    val (icon, title, body) = warningDetails(warning)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun PermissionsSummaryCard(permissions: DevicePermissions) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(Res.string.status_permissions_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            PermissionRow(label = stringResource(Res.string.status_permission_present),  granted = permissions.canPresent)
            Spacer(Modifier.height(4.dp))
            PermissionRow(label = stringResource(Res.string.status_permission_schedule), granted = permissions.canAddToSchedule)
            Spacer(Modifier.height(4.dp))
            PermissionRow(label = stringResource(Res.string.status_permission_upload),   granted = permissions.canUploadFiles)
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (granted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        )
        if (!granted) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(Res.string.status_permission_blocked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Warning metadata — resolved in composable scope so stringResource() works
// ─────────────────────────────────────────────────────────────────────────────

private data class WarningDetails(val icon: ImageVector, val title: String, val body: String)

@Composable
private fun warningDetails(warning: StatusWarning): WarningDetails = when (warning) {
    is StatusWarning.NoApiKey -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_no_api_key_title),
        body  = stringResource(Res.string.status_warn_no_api_key_body),
    )
    is StatusWarning.NoBibles -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_no_bibles_title),
        body  = stringResource(Res.string.status_warn_no_bibles_body),
    )
    is StatusWarning.NoSongbooks -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_no_songbooks_title),
        body  = stringResource(Res.string.status_warn_no_songbooks_body),
    )
    is StatusWarning.PresentBlocked -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_present_blocked_title),
        body  = stringResource(Res.string.status_warn_present_blocked_body),
    )
    is StatusWarning.ScheduleBlocked -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_schedule_blocked_title),
        body  = stringResource(Res.string.status_warn_schedule_blocked_body),
    )
    is StatusWarning.UploadBlocked -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_upload_blocked_title),
        body  = stringResource(Res.string.status_warn_upload_blocked_body),
    )
    is StatusWarning.UnknownVersion -> WarningDetails(
        icon  = Icons.Filled.Info,
        title = stringResource(Res.string.status_warn_unknown_version_title),
        body  = stringResource(Res.string.status_warn_unknown_version_body, warning.version ?: "unknown"),
    )
    is StatusWarning.MissingEndpoint -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_missing_endpoint_title, warning.endpoint),
        body  = stringResource(Res.string.status_warn_missing_endpoint_body, warning.endpoint),
    )
}
