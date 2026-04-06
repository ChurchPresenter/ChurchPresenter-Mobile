package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific button that launches a QR-code scanner and returns the decoded string.
 *
 * Android : Uses Google Play Services Code Scanner (no camera permission required,
 *           Google provides the scanning UI via Play Services).
 * iOS     : Presents a full-screen AVFoundation camera overlay.
 * Web     : Hidden — browser camera scanning is not supported in this context.
 *
 * @param onScanned Called on the main thread with the raw decoded QR string.
 */
@Composable
expect fun QrScanButton(
    onScanned: (String) -> Unit,
    modifier: Modifier = Modifier
)

