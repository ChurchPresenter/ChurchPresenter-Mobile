package com.church.presenter.churchpresentermobile.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_cancel
import churchpresentermobile.composeapp.generated.resources.cd_close
import churchpresentermobile.composeapp.generated.resources.qr_camera_denied_body
import churchpresentermobile.composeapp.generated.resources.qr_camera_denied_title
import churchpresentermobile.composeapp.generated.resources.qr_camera_open_settings
import churchpresentermobile.composeapp.generated.resources.qr_scan_button
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.jetbrains.compose.resources.stringResource
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android implementation: a full-screen camera overlay built into the app — CameraX for the
 * camera, ZXing to decode — so scanning works on any phone with a camera, with or without
 * Google Play Services. The same shape as the iOS scanner.
 *
 * Camera permission is requested on first tap. When it has been refused, the tap explains why
 * nothing can be scanned and offers the app's system settings, as iOS does.
 */
@Composable
actual fun QrScanButton(onScanned: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    var showScanner by remember { mutableStateOf(false) }
    var showDenied by remember { mutableStateOf(false) }
    val currentOnScanned by rememberUpdatedState(onScanned)
    val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showScanner = true else showDenied = true
    }

    OutlinedButton(
        onClick = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            // Once refused for good, the system answers `false` at once without asking again,
            // which lands on the explanation below.
            if (granted) showScanner = true else requestCamera.launch(Manifest.permission.CAMERA)
        },
        modifier = modifier
    ) {
        Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.qr_scan_button))
    }

    if (showDenied) {
        CameraDeniedDialog(context, onDismiss = { showDenied = false })
    }

    if (showScanner) {
        ScannerOverlay(
            onClose = { showScanner = false },
            onScanned = { value ->
                showScanner = false
                currentOnScanned(value)
            },
        )
    }
}

@Composable
actual fun hasCameraAvailable(): Boolean {
    val context = LocalContext.current
    return remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
}

@Composable
private fun CameraDeniedDialog(context: Context, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.qr_camera_denied_title)) },
        text = { Text(stringResource(Res.string.qr_camera_denied_body)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                val appDetails = Uri.fromParts("package", context.packageName, null)
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appDetails)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }) { Text(stringResource(Res.string.qr_camera_open_settings)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        }
    )
}

/** The live camera, full screen, until a QR code is read or the overlay is closed. */
@Composable
private fun ScannerOverlay(onClose: () -> Unit, onScanned: (String) -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val currentOnScanned by rememberUpdatedState(onScanned)
        val currentOnClose by rememberUpdatedState(onClose)
        val previewView = remember { PreviewView(context) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.cd_close),
                    tint = Color.White
                )
            }
        }

        DisposableEffect(lifecycleOwner) {
            val mainExecutor = ContextCompat.getMainExecutor(context)
            val analysisExecutor = Executors.newSingleThreadExecutor()
            val providerFuture = ProcessCameraProvider.getInstance(context)
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor, QrAnalyzer { value ->
                        mainExecutor.execute { currentOnScanned(value) }
                    })
                }
            var disposed = false

            providerFuture.addListener({
                // The overlay can be closed before the provider is ready; binding then would
                // leave the camera running behind a dialog that is already gone.
                if (disposed) return@addListener
                val provider = providerFuture.get()
                val camera = listOf(CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA)
                    .firstOrNull(provider::hasCamera)
                if (camera == null) {
                    currentOnClose()
                    return@addListener
                }
                provider.bindToLifecycle(lifecycleOwner, camera, preview, analysis)
            }, mainExecutor)

            onDispose {
                disposed = true
                if (providerFuture.isDone) providerFuture.get().unbind(preview, analysis)
                analysis.clearAnalyzer()
                analysisExecutor.shutdown()
            }
        }
    }
}

/**
 * Decodes each camera frame's brightness plane with ZXing and hands over the first QR code
 * found, once. A QR code reads the same at any rotation, so frames are decoded as they come.
 */
private class QrAnalyzer(private val onFound: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = QRCodeReader()
    private val hints = mapOf(DecodeHintType.TRY_HARDER to true)
    private val found = AtomicBoolean(false)

    override fun analyze(image: ImageProxy) {
        image.use { frame ->
            if (found.get()) return
            val luma = frame.planes[0]
            val bytes = ByteArray(luma.buffer.remaining()).also { luma.buffer.get(it) }
            val source = PlanarYUVLuminanceSource(
                bytes, luma.rowStride, frame.height, 0, 0, frame.width, frame.height, false
            )
            val text = try {
                reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
            } catch (_: ReaderException) {
                null
            } finally {
                reader.reset()
            }
            if (text != null && found.compareAndSet(false, true)) onFound(text)
        }
    }
}
