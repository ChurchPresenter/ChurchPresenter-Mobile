package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.util.appVersion
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.status_connected
import churchpresentermobile.composeapp.generated.resources.status_connecting
import churchpresentermobile.composeapp.generated.resources.status_version_line
import churchpresentermobile.composeapp.generated.resources.status_continue
import churchpresentermobile.composeapp.generated.resources.status_continue_anyway
import churchpresentermobile.composeapp.generated.resources.status_endpoint_unavailable
import churchpresentermobile.composeapp.generated.resources.status_error_title
import churchpresentermobile.composeapp.generated.resources.status_info_bibles
import churchpresentermobile.composeapp.generated.resources.status_info_features
import churchpresentermobile.composeapp.generated.resources.status_info_songbooks
import churchpresentermobile.composeapp.generated.resources.status_issues_detected
import churchpresentermobile.composeapp.generated.resources.status_limited_functionality
import churchpresentermobile.composeapp.generated.resources.status_not_churchpresenter_body
import churchpresentermobile.composeapp.generated.resources.status_not_churchpresenter_title
import churchpresentermobile.composeapp.generated.resources.status_open_settings
import churchpresentermobile.composeapp.generated.resources.status_permission_present
import churchpresentermobile.composeapp.generated.resources.status_permission_schedule
import churchpresentermobile.composeapp.generated.resources.status_permission_upload
import churchpresentermobile.composeapp.generated.resources.status_permissions_title
import churchpresentermobile.composeapp.generated.resources.status_retry
import churchpresentermobile.composeapp.generated.resources.status_server_version
import churchpresentermobile.composeapp.generated.resources.status_unauthorized_body
import churchpresentermobile.composeapp.generated.resources.status_unauthorized_title
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
                is StatusUiState.Loading -> LoadingContent(onOpenSettings = onOpenSettings)
                is StatusUiState.Error   -> ErrorContent(
                    title          = stringResource(Res.string.status_error_title),
                    message        = state.message,
                    onRetry        = { viewModel.recheck() },
                    onContinue     = onContinue,
                    onOpenSettings = onOpenSettings,
                )
                is StatusUiState.Unauthorized -> ErrorContent(
                    title          = stringResource(Res.string.status_unauthorized_title),
                    message        = stringResource(Res.string.status_unauthorized_body),
                    onRetry        = { viewModel.recheck() },
                    onContinue     = onContinue,
                    onOpenSettings = onOpenSettings,
                )
                is StatusUiState.NotChurchPresenter -> ErrorContent(
                    title          = stringResource(Res.string.status_not_churchpresenter_title),
                    message        = stringResource(Res.string.status_not_churchpresenter_body),
                    onRetry        = { viewModel.recheck() },
                    onContinue     = onContinue,
                    onOpenSettings = onOpenSettings,
                )
                is StatusUiState.Success -> {
                    if (state.warnings.isEmpty()) {
                        AllGoodContent(
                            permissions       = state.status.permissions,
                            serverVersion     = state.status.appVersion,
                            bibles            = state.status.bibles,
                            songbooks         = state.status.songbooks,
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
private fun LoadingContent(onOpenSettings: () -> Unit) {
    // If the check is still loading after a few seconds (e.g. an unreachable
    // host that hangs rather than failing fast), reveal a way out instead of
    // leaving the user stuck on a bare spinner with no escape.
    var showEscapeHatch by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(5000)
        showEscapeHatch = true
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(UiTags.STATUS_LOADING),
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.status_connecting), style = MaterialTheme.typography.bodyLarge)
        if (showEscapeHatch) {
            Spacer(Modifier.height(24.dp))
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag(UiTags.STATUS_OPEN_SETTINGS),
            ) {
                Text(stringResource(Res.string.status_open_settings))
            }
        }
    }
}

@Composable
private fun AllGoodContent(
    permissions: DevicePermissions = DevicePermissions(),
    serverVersion: String? = null,
    bibles: List<String> = emptyList(),
    songbooks: List<String> = emptyList(),
    endpointAvailable: Boolean = true,
    onContinue: () -> Unit = {},
) {
    val colors = LocalAppColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTags.STATUS_ALL_GOOD)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(24.dp))
        // 72×72 accent-tint circle + checkmark
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.status_connected),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.025).em,
            color = colors.text,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.status_version_line, serverVersion ?: "—", appVersion),
            fontSize = 13.sp,
            color = colors.muted,
        )
        if (!endpointAvailable) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.status_endpoint_unavailable),
                fontSize = 12.sp,
                color = colors.muted,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(28.dp))
        PermissionsSummaryCard(permissions)
        if (bibles.isNotEmpty() || songbooks.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ContentSummaryCard(bibles = bibles, songbooks = songbooks)
        }
        Spacer(Modifier.height(20.dp))
        OutlineActionButton(
            modifier = Modifier.testTag(UiTags.STATUS_CONTINUE),
            label = stringResource(Res.string.status_continue),
            icon = Icons.Filled.CheckCircle,
            onClick = onContinue,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ContentSummaryCard(bibles: List<String>, songbooks: List<String>) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        if (bibles.isNotEmpty()) {
            Text(stringResource(Res.string.status_info_bibles), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
            Spacer(Modifier.height(6.dp))
            bibles.forEach { Text(it, fontSize = 13.sp, color = colors.muted) }
        }
        if (songbooks.isNotEmpty()) {
            if (bibles.isNotEmpty()) Spacer(Modifier.height(12.dp))
            Text(stringResource(Res.string.status_info_songbooks), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
            Spacer(Modifier.height(6.dp))
            songbooks.forEach { Text(it, fontSize = 13.sp, color = colors.muted) }
        }
    }
}

@Composable
private fun ErrorContent(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(UiTags.STATUS_ERROR),
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
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
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().testTag(UiTags.STATUS_RETRY),
        ) {
            Text(stringResource(Res.string.status_retry))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth().testTag(UiTags.STATUS_OPEN_SETTINGS),
        ) {
            Text(stringResource(Res.string.status_open_settings))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onContinue,
            modifier = Modifier.testTag(UiTags.STATUS_CONTINUE),
        ) {
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
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTags.STATUS_WARNINGS)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        // Lilac warning triangle inside a tinted circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.warningTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = colors.warning,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.status_limited_functionality),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.025).em,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        if (appVersion != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.status_server_version, appVersion),
                fontSize = 13.sp,
                color = colors.muted,
            )
        }
        Spacer(Modifier.height(24.dp))
        PermissionsSummaryCard(permissions)
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.status_issues_detected),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = colors.muted,
            lineHeight = (13 * 1.5).sp,
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
        // Primary Continue + subtle Open Settings, in the redesign palette
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(UiTags.STATUS_CONTINUE)
                .clip(RoundedCornerShape(13.dp))
                .background(colors.accent)
                .clickable(onClick = onContinue),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.status_continue),
                color = colors.onAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag(UiTags.STATUS_OPEN_SETTINGS)
                .clip(RoundedCornerShape(13.dp))
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.status_open_settings),
                color = colors.muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WarningCard(warning: StatusWarning) {
    val colors = LocalAppColors.current
    val (icon, title, body) = warningDetails(warning)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.danger.copy(alpha = 0.10f))
            .border(1.dp, colors.danger.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.danger,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(body, color = colors.muted, fontSize = 12.sp, lineHeight = (12 * 1.5).sp)
        }
    }
}

@Composable
private fun PermissionsSummaryCard(permissions: DevicePermissions) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(Res.string.status_permissions_title),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.text,
        )
        Spacer(Modifier.height(10.dp))
        PermissionRow(label = stringResource(Res.string.status_permission_present),  granted = permissions.canPresent)
        Spacer(Modifier.height(10.dp))
        PermissionRow(label = stringResource(Res.string.status_permission_schedule), granted = permissions.canAddToSchedule)
        Spacer(Modifier.height(10.dp))
        PermissionRow(label = stringResource(Res.string.status_permission_upload),   granted = permissions.canUploadFiles)
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (granted) colors.accent else colors.danger,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (granted) colors.text else colors.danger,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (granted) "true" else "false",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (granted) colors.accent else colors.danger,
        )
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.amber.copy(alpha = 0.10f))
            .border(1.dp, colors.amber.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.amber,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(body, color = colors.muted, fontSize = 12.sp, lineHeight = (12 * 1.5).sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Warning metadata — resolved in composable scope so stringResource() works
// ─────────────────────────────────────────────────────────────────────────────

/** The words and icon one [StatusWarning] is shown with. */
internal data class WarningDetails(val icon: ImageVector, val title: String, val body: String)

/**
 * Resolves a [StatusWarning] to what the operator reads.
 *
 * `internal` because each warning sends someone somewhere different — to the
 * desktop's permissions, to its Bible import, to its version — and a warning
 * that resolves to the wrong words sends them to the wrong place. Eight
 * branches, none of them reachable from a screen test without eight different
 * server responses.
 */
@Composable
internal fun warningDetails(warning: StatusWarning): WarningDetails = when (warning) {
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
