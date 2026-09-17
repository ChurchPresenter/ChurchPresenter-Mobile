package com.church.presenter.churchpresentermobile.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.qr_scan_button
import org.jetbrains.compose.resources.stringResource

/**
 * Android implementation: uses the Google Play Services Code Scanner.
 * No camera permission is required — Google provides the full scanning UI.
 * Falls back gracefully on devices without Google Play Services.
 */
@Composable
actual fun QrScanButton(onScanned: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current

    OutlinedButton(
        onClick = { startBarcodeScan(context, onScanned) },
        modifier = modifier
    ) {
        Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.qr_scan_button))
    }
}

/**
 * Checks the scanner module is actually installed before starting the scan.
 * Skipping this check let `startScan()` launch the delegate activity even
 * when the module wasn't ready, which crashed with an uncatchable
 * `ActivityNotFoundException` deep inside that activity rather than
 * surfacing here (CHURCH-PRESENTER-MOBILE-1G).
 */
private fun startBarcodeScan(context: Context, onScanned: (String) -> Unit) {
    try {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = GmsBarcodeScanning.getClient(context, options)
        ModuleInstall.getClient(context).areModulesAvailable(scanner)
            .addOnSuccessListener { response ->
                if (response.areModulesAvailable()) {
                    scanner.startScan()
                        .addOnSuccessListener { barcode -> barcode.rawValue?.let(onScanned) }
                        .addOnFailureListener { showScannerUnavailableToast(context) }
                } else {
                    showScannerUnavailableToast(context)
                }
            }
            .addOnFailureListener { showScannerUnavailableToast(context) }
    } catch (e: ActivityNotFoundException) {
        showScannerUnavailableToast(context)
    }
}

private fun showScannerUnavailableToast(context: Context) {
    Toast.makeText(
        context,
        "QR scanning requires Google Play Services, which is not available on this device.",
        Toast.LENGTH_LONG
    ).show()
}

@Composable
actual fun hasCameraAvailable(): Boolean {
    val context = LocalContext.current
    return remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
}
