package com.church.presenter.churchpresentermobile.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.settings_status_bibles
import churchpresentermobile.composeapp.generated.resources.settings_status_mobile_version
import churchpresentermobile.composeapp.generated.resources.settings_status_none
import churchpresentermobile.composeapp.generated.resources.settings_status_recheck
import churchpresentermobile.composeapp.generated.resources.settings_status_server_version
import churchpresentermobile.composeapp.generated.resources.settings_status_songbooks
import churchpresentermobile.composeapp.generated.resources.status_connected
import churchpresentermobile.composeapp.generated.resources.status_connecting
import churchpresentermobile.composeapp.generated.resources.status_error_title
import churchpresentermobile.composeapp.generated.resources.status_limited_functionality
import churchpresentermobile.composeapp.generated.resources.status_not_churchpresenter_body
import churchpresentermobile.composeapp.generated.resources.status_not_churchpresenter_title
import churchpresentermobile.composeapp.generated.resources.status_permission_present
import churchpresentermobile.composeapp.generated.resources.status_permission_schedule
import churchpresentermobile.composeapp.generated.resources.status_permission_upload
import churchpresentermobile.composeapp.generated.resources.status_permissions_title
import churchpresentermobile.composeapp.generated.resources.status_unauthorized_body
import churchpresentermobile.composeapp.generated.resources.status_unauthorized_title
import com.church.presenter.churchpresentermobile.util.appVersion
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import org.jetbrains.compose.resources.stringResource

/**
 * The Check Server Status modal's body on a phone: one card under the next,
 * and Recheck after them. [ServerStatusTablet] is the same states across a
 * tablet.
 */
@Composable
internal fun ServerStatusPhone(
    state: StatusUiState,
    onRecheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state) {
            is StatusUiState.Loading -> PhoneLoading()
            is StatusUiState.Error -> PhoneFailure(
                icon = Icons.Filled.Warning,
                title = stringResource(Res.string.status_error_title),
                body = state.message,
                tag = UiTags.STATUS_DIALOG_ERROR,
            )
            is StatusUiState.Unauthorized -> PhoneFailure(
                icon = Icons.Filled.Lock,
                title = stringResource(Res.string.status_unauthorized_title),
                body = stringResource(Res.string.status_unauthorized_body),
                tag = UiTags.STATUS_DIALOG_UNAUTHORIZED,
            )
            is StatusUiState.NotChurchPresenter -> PhoneFailure(
                icon = Icons.Filled.Warning,
                title = stringResource(Res.string.status_not_churchpresenter_title),
                body = stringResource(Res.string.status_not_churchpresenter_body),
                tag = UiTags.STATUS_DIALOG_NOT_CHURCHPRESENTER,
            )
            is StatusUiState.Success -> PhoneAnswered(state)
        }
        if (state !is StatusUiState.Loading) StatusRecheckButton(onRecheck)
    }
}

@Composable
private fun PhoneLoading() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        verticalArrangement   = Arrangement.spacedBy(16.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag(UiTags.STATUS_DIALOG_LOADING))
        Text(stringResource(Res.string.status_connecting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A failure: the same three-line message whichever way the desktop refused. */
@Composable
private fun PhoneFailure(icon: ImageVector, title: String, body: String, tag: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        verticalArrangement   = Arrangement.spacedBy(12.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp))
        Text(title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(tag))
        Text(body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
    }
}

/** A desktop that answered: the verdict, its permissions, its content, and its warnings if any. */
@Composable
private fun PhoneAnswered(state: StatusUiState.Success) {
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
                    modifier = Modifier.testTag(UiTags.STATUS_DIALOG_CONNECTED),
                )
                // Server version — from API response
                if (status.appVersion != null) {
                    Text(
                        stringResource(Res.string.settings_status_server_version, status.appVersion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(UiTags.STATUS_DIALOG_SERVER_VERSION),
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
    StatusCard(modifier = Modifier.testTag(UiTags.STATUS_DIALOG_PERMISSIONS)) {
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
            modifier = Modifier.testTag(UiTags.STATUS_DIALOG_BIBLES),
        )
        Spacer(Modifier.height(12.dp))
        StatusLabel(stringResource(Res.string.settings_status_songbooks))
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (status.songbooks.isEmpty()) stringResource(Res.string.settings_status_none)
                   else status.songbooks.joinToString("\n") { "• $it" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(UiTags.STATUS_DIALOG_SONGBOOKS),
        )
    }


    // Warnings
    if (warnings.isNotEmpty()) {
        StatusCard(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.testTag(UiTags.STATUS_DIALOG_WARNINGS),
        ) {
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
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared composables for the status dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusCard(
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = containerColor)) {
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
