package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.qr_scan_button
import org.jetbrains.compose.resources.stringResource

/**
 * Android implementation: uses the Google Play Services Code Scanner.
 * No camera permission is required — Google provides the full scanning UI.
 */
@Composable
actual fun QrScanButton(onScanned: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current

    OutlinedButton(
        onClick = {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            GmsBarcodeScanning.getClient(context, options)
                .startScan()
                .addOnSuccessListener { barcode ->
                    barcode.rawValue?.let { onScanned(it) }
                }
                .addOnFailureListener {
                    // User cancelled or scanner unavailable — no-op
                }
        },
        modifier = modifier
    ) {
        Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.qr_scan_button))
    }
}

