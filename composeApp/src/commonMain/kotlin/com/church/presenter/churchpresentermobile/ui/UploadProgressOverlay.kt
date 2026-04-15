package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.upload_overlay_subtitle
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

/**
 * Non-dismissible full-screen overlay shown while a file upload is in progress.
 *
 * Rendered as a platform Dialog with back-press and outside-click dismissal both disabled,
 * so the user cannot accidentally cancel a long-running upload. The overlay disappears
 * automatically once the ViewModel sets `isUploading` to false.
 *
 * @param title    Primary label (e.g. "Uploading photos…" / "Uploading presentation…").
 * @param progress Current upload progress 0.0–1.0, or **null** for an indeterminate bar.
 * @param subtitle Optional secondary line. Defaults to "Please wait, do not close the app".
 * @param detail   Optional tertiary detail (e.g. "Photo 2 of 5").
 */
@Composable
fun UploadProgressOverlay(
    title: String,
    progress: Float?,
    subtitle: String? = null,
    detail: String? = null,
) {
    Dialog(
        onDismissRequest = { /* intentionally empty — cannot be dismissed */ },
        properties = DialogProperties(
            dismissOnBackPress    = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors    = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(4.dp))

                if (progress != null) {
                    LinearProgressIndicator(
                        progress    = { progress.coerceIn(0f, 1f) },
                        modifier    = Modifier.fillMaxWidth(),
                        trackColor  = MaterialTheme.colorScheme.surfaceVariant,
                        color       = MaterialTheme.colorScheme.primary,
                    )

                    Text(
                        text  = "${(progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier    = Modifier.fillMaxWidth(),
                        trackColor  = MaterialTheme.colorScheme.surfaceVariant,
                        color       = MaterialTheme.colorScheme.primary,
                    )
                }

                if (detail != null) {
                    Text(
                        text  = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text  = subtitle ?: stringResource(Res.string.upload_overlay_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
