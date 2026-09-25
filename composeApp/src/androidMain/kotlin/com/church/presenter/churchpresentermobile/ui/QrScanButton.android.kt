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
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.qr_scan_button
import churchpresentermobile.composeapp.generated.resources.qr_scanner_downloading
import churchpresentermobile.composeapp.generated.resources.qr_scanner_unavailable
import org.jetbrains.compose.resources.stringResource

/**
 * Android implementation: uses the Google Play Services Code Scanner.
 * No camera permission is required — Google provides the full scanning UI.
 * Falls back gracefully on devices without Google Play Services.
 */
@Composable
actual fun QrScanButton(onScanned: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val messages = ScannerMessages(
        downloading = stringResource(Res.string.qr_scanner_downloading),
        unavailable = stringResource(Res.string.qr_scanner_unavailable),
    )

    OutlinedButton(
        onClick = { startBarcodeScan(context, messages, onScanned) },
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
 *
 * The module is downloaded on demand and is not there until some app asks for
 * it, so a missing module is requested rather than treated as a device without
 * Play Services — otherwise a phone no app had scanned on yet could never scan.
 */
private fun startBarcodeScan(context: Context, messages: ScannerMessages, onScanned: (String) -> Unit) {
    try {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = GmsBarcodeScanning.getClient(context, options)
        val startScan = {
            scanner.startScan()
                .addOnSuccessListener { barcode -> barcode.rawValue?.let(onScanned) }
                .addOnFailureListener { showToast(context, messages.unavailable) }
        }
        val modules = ModuleInstall.getClient(context)
        modules.areModulesAvailable(scanner)
            .addOnSuccessListener { response ->
                if (response.areModulesAvailable()) {
                    startScan()
                } else {
                    showToast(context, messages.downloading)
                    modules.installModules(ModuleInstallRequest.newBuilder().addApi(scanner).build())
                        .addOnSuccessListener { startScan() }
                        .addOnFailureListener { showToast(context, messages.unavailable) }
                }
            }
            .addOnFailureListener { showToast(context, messages.unavailable) }
    } catch (e: ActivityNotFoundException) {
        showToast(context, messages.unavailable)
    }
}

/** The scanner's toasts, resolved in composition since the scan runs outside it. */
private class ScannerMessages(val downloading: String, val unavailable: String)

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

@Composable
actual fun hasCameraAvailable(): Boolean {
    val context = LocalContext.current
    return remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
}
