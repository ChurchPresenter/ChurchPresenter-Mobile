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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.cert_setup_done_button
import churchpresentermobile.composeapp.generated.resources.cert_setup_fingerprint_body
import churchpresentermobile.composeapp.generated.resources.cert_setup_fingerprint_title
import churchpresentermobile.composeapp.generated.resources.cert_setup_fingerprint_unavailable
import churchpresentermobile.composeapp.generated.resources.cert_setup_intro
import churchpresentermobile.composeapp.generated.resources.cert_setup_scan_desktop_hint
import churchpresentermobile.composeapp.generated.resources.cert_setup_scan_title
import churchpresentermobile.composeapp.generated.resources.cert_setup_skip_button
import churchpresentermobile.composeapp.generated.resources.cert_setup_skip_note
import churchpresentermobile.composeapp.generated.resources.cert_setup_android_unknown_sources_body
import churchpresentermobile.composeapp.generated.resources.cert_setup_android_unknown_sources_title
import churchpresentermobile.composeapp.generated.resources.cert_setup_step2_android_body
import churchpresentermobile.composeapp.generated.resources.cert_setup_step2_android_title
import churchpresentermobile.composeapp.generated.resources.cert_setup_step2_ios_body
import churchpresentermobile.composeapp.generated.resources.cert_setup_step2_ios_title
import churchpresentermobile.composeapp.generated.resources.cert_setup_step3_ios_body
import churchpresentermobile.composeapp.generated.resources.cert_setup_step3_ios_title
import churchpresentermobile.composeapp.generated.resources.cert_setup_subtitle
import churchpresentermobile.composeapp.generated.resources.cert_setup_title
import com.church.presenter.churchpresentermobile.getPlatform
import com.church.presenter.churchpresentermobile.openUrl
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen onboarding screen that walks first-time users through downloading
 * and trusting the server's CA certificate.
 *
 * **Primary flow — QR scan (step 1):**
 *  1. User taps "Scan QR Code" — camera opens immediately.
 *  2. Scan the QR shown by ChurchPresenter desktop → Settings → Server → Download Cert.
 *  3. Cert URL resolves automatically; tap "Open Certificate URL" to install.
 *
 * **Fallback:** if host/port are already saved the URL is pre-filled; scanning is optional.
 *
 * @param certFingerprint SHA-256 fingerprint from the server, or null if unknown.
 * @param onDone          Called when the user taps "Done — Certificate Installed".
 * @param onSkip          Called when the user taps "Skip for now".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertSetupScreen(
    certFingerprint: String? = null,
    onDone: () -> Unit,
    onSkip: () -> Unit,
) {

    val platformName = remember { getPlatform().name }
    val isIos = platformName.startsWith("iOS", ignoreCase = true)
    val isAndroid = platformName.startsWith("Android", ignoreCase = true)


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.cert_setup_title),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(Res.string.cert_setup_subtitle),
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
                        Text(stringResource(Res.string.cert_setup_skip_button))
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
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = stringResource(Res.string.cert_setup_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // ── Step 1 — Scan QR code ──────────────────────────────────────────
            SetupStep(number = "1", title = stringResource(Res.string.cert_setup_scan_title)) {

                // Short instruction — where to find the QR code on desktop
                Text(
                    text = stringResource(Res.string.cert_setup_scan_desktop_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                // QrScanButton — tapping opens the camera immediately
                QrScanButton(
                    onScanned = { scanned ->
                        if (scanned.startsWith("http")) {
                            openUrl(scanned)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            // ── Step 2 & 3 — Platform install instructions ────────────────────
            if (!isAndroid) {
                SetupStep(number = "2", title = stringResource(Res.string.cert_setup_step2_ios_title)) {
                    Text(
                        text = stringResource(Res.string.cert_setup_step2_ios_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SetupStep(number = "3", title = stringResource(Res.string.cert_setup_step3_ios_title)) {
                    Text(
                        text = stringResource(Res.string.cert_setup_step3_ios_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isIos) {
                SetupStep(number = if (isAndroid) "2" else "4", title = stringResource(Res.string.cert_setup_step2_android_title)) {
                    Text(
                        text = stringResource(Res.string.cert_setup_step2_android_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SetupStep(number = if (isAndroid) "3" else "5", title = stringResource(Res.string.cert_setup_android_unknown_sources_title)) {
                    Text(
                        text = stringResource(Res.string.cert_setup_android_unknown_sources_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // ── Fingerprint verification ───────────────────────────────────────
            SetupStep(number = "✓", title = stringResource(Res.string.cert_setup_fingerprint_title)) {
                Text(
                    text = stringResource(Res.string.cert_setup_fingerprint_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                if (certFingerprint != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = certFingerprint,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = true,
                            overflow = TextOverflow.Visible,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.cert_setup_fingerprint_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
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
                    text = stringResource(Res.string.cert_setup_done_button),
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.cert_setup_skip_button))
            }

            Text(
                text = stringResource(Res.string.cert_setup_skip_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * A numbered step row — circle badge + title on one line, body content below indented.
 * The title uses [Modifier.weight(1f)] so it wraps rather than overflows on narrow screens.
 */
@Composable
private fun SetupStep(
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
                modifier = Modifier.weight(1f),   // ← prevents overflow on narrow screens
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
        }
        Column(modifier = Modifier.padding(start = 36.dp)) {
            content()
        }
    }
}
