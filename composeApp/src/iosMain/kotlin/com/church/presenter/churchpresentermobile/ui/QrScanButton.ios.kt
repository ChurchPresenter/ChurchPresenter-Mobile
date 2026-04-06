package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.darwin.NSObject
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation: shows a full-screen AVFoundation camera overlay inside
 * the current Compose hierarchy (works within a Dialog because Compose Dialogs
 * on iOS fill the whole screen).
 *
 * Camera permission is requested lazily on first tap.
 * The Apple docs guarantee the requestAccessForMediaType completion block is
 * dispatched on the main thread, so we can safely mutate Compose state there.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrScanButton(onScanned: (String) -> Unit, modifier: Modifier) {
    var showScanner by remember { mutableStateOf(false) }
    val currentOnScanned by rememberUpdatedState(onScanned)

    OutlinedButton(
        onClick = {
            val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
            when (status) {
                // Already authorised — show immediately
                AVAuthorizationStatusAuthorized -> showScanner = true
                // First request — Apple calls the completion on the main thread
                AVAuthorizationStatusNotDetermined ->
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        if (granted) showScanner = true
                    }
                else -> { /* denied / restricted — silently no-op */ }
            }
        },
        modifier = modifier
    ) {
        Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Scan QR Code")
    }

    if (showScanner) {
        // Keep AVFoundation objects alive for the lifetime of the overlay
        val session  = remember { AVCaptureSession() }
        val delegate = remember {
            QrMetadataDelegate { value ->
                session.stopRunning()
                showScanner = false
                currentOnScanned(value)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Camera live preview embedded as a UIView
            UIKitView(
                factory  = { buildCameraView(session, delegate) },
                update   = { view ->
                    // Keep the AVCaptureVideoPreviewLayer frame in sync with the view
                    (view.layer.sublayers?.firstOrNull() as? AVCaptureVideoPreviewLayer)
                        ?.frame = view.bounds
                },
                modifier = Modifier.fillMaxSize()
            )

            // Compose close button — positioned above any iOS status bar content
            IconButton(
                onClick = {
                    session.stopRunning()
                    showScanner = false
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 52.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector  = Icons.Filled.Close,
                    contentDescription = "Close scanner",
                    tint         = Color.White
                )
            }
        }

        // Guarantee the session is stopped when the overlay leaves composition
        DisposableEffect(session) {
            onDispose { session.stopRunning() }
        }
    }
}

// ── AVFoundation helpers ───────────────────────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
private fun buildCameraView(
    session:  AVCaptureSession,
    delegate: QrMetadataDelegate
): UIView {
    val view = UIView()
    view.backgroundColor = UIColor.blackColor

    val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return view

    // deviceInputWithDevice:error: — pass null for the error pointer; returns nil on failure
    val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) as? AVCaptureDeviceInput
        ?: return view

    if (session.canAddInput(input))  session.addInput(input)

    val metaOutput = AVCaptureMetadataOutput()
    if (session.canAddOutput(metaOutput)) {
        session.addOutput(metaOutput)
        metaOutput.setMetadataObjectsDelegate(delegate, queue = dispatch_get_main_queue())
        metaOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
    }

    val layer = AVCaptureVideoPreviewLayer(session = session)
    layer.videoGravity = AVLayerVideoGravityResizeAspectFill
    view.layer.addSublayer(layer)

    session.startRunning()
    return view
}

// ── AVCaptureMetadataOutputObjectsDelegate ─────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
private class QrMetadataDelegate(
    private val onDetected: (String) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    @Suppress("UNCHECKED_CAST")
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: platform.AVFoundation.AVCaptureConnection
    ) {
        val obj = didOutputMetadataObjects.firstOrNull()
            as? AVMetadataMachineReadableCodeObject ?: return
        obj.stringValue?.let { onDetected(it) }
    }
}
