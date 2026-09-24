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
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_cancel
import churchpresentermobile.composeapp.generated.resources.cd_close
import churchpresentermobile.composeapp.generated.resources.qr_camera_denied_body
import churchpresentermobile.composeapp.generated.resources.qr_camera_denied_title
import churchpresentermobile.composeapp.generated.resources.qr_camera_open_settings
import churchpresentermobile.composeapp.generated.resources.qr_scan_button
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import org.jetbrains.compose.resources.stringResource
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoOrientation
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.Foundation.iOSAppOnMac
import platform.Foundation.macCatalystApp
import platform.QuartzCore.CATransaction
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIColor
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

/**
 * iOS implementation: shows a full-screen AVFoundation camera overlay inside
 * the current Compose hierarchy (works within a Dialog because Compose Dialogs
 * on iOS fill the whole screen).
 *
 * Camera permission is requested lazily on first tap. When it has been denied,
 * the tap explains why nothing can be scanned and offers the Settings app —
 * iOS never shows the system prompt a second time.
 *
 * Renders nothing when the iPad app runs on a Mac — see [runningOnMac].
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrScanButton(onScanned: (String) -> Unit, modifier: Modifier) {
    if (runningOnMac) return
    var showScanner by remember { mutableStateOf(false) }
    var showDenied by remember { mutableStateOf(false) }
    val currentOnScanned by rememberUpdatedState(onScanned)

    OutlinedButton(
        onClick = {
            when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
                AVAuthorizationStatusAuthorized -> showScanner = true
                // The completion runs on an arbitrary queue, not necessarily main.
                AVAuthorizationStatusNotDetermined ->
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        dispatch_async(dispatch_get_main_queue()) {
                            if (granted) showScanner = true else showDenied = true
                        }
                    }
                else -> showDenied = true
            }
        },
        modifier = modifier
    ) {
        Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.qr_scan_button))
    }

    if (showDenied) {
        CameraDeniedDialog(onDismiss = { showDenied = false })
    }

    if (showScanner) {
        // Keep AVFoundation objects alive for the lifetime of the overlay
        val session  = remember { AVCaptureSession() }
        val delegate = remember {
            QrMetadataDelegate { value ->
                stopSession(session)
                showScanner = false
                currentOnScanned(value)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            UIKitView(
                factory  = {
                    startSession(session, delegate)
                    CameraPreviewView(session)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Compose close button — positioned above any iOS status bar content
            IconButton(
                onClick = { showScanner = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 52.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector  = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.cd_close),
                    tint         = Color.White
                )
            }
        }

        // Guarantee the session is stopped when the overlay leaves composition
        DisposableEffect(session) {
            onDispose { stopSession(session) }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun hasCameraAvailable(): Boolean =
    remember { !runningOnMac && AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null }

/**
 * True when this iPad build is running on a Mac (Apple silicon "Designed for
 * iPad", or Catalyst).
 *
 * The live camera is switched off there: every Sentry crash in the Skia draw
 * path (CHURCH-PRESENTER-MOBILE-1N, -1V, -1X) came from a Mac with the capture
 * pipeline streaming, and nowhere else. Pointing a Mac's webcam at a QR code on
 * the Mac's own screen is not a real workflow either, so callers fall back to
 * manual entry, as they already do on a device with no camera.
 */
private val runningOnMac: Boolean by lazy {
    NSProcessInfo.processInfo.let { it.iOSAppOnMac || it.macCatalystApp }
}

@Composable
private fun CameraDeniedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.qr_camera_denied_title)) },
        text = { Text(stringResource(Res.string.qr_camera_denied_body)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
                    UIApplication.sharedApplication.openURL(
                        url,
                        options = emptyMap<Any?, Any>(),
                        completionHandler = null
                    )
                }
            }) { Text(stringResource(Res.string.qr_camera_open_settings)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        }
    )
}

// ── AVFoundation helpers ───────────────────────────────────────────────────

/**
 * Every configure / start / stop goes through this one serial queue. Apple
 * requires them off the main thread (each blocks while the capture graph is
 * built or torn down), and serialising them guarantees a stop queued on
 * dismissal runs after a start that is still pending.
 */
private val sessionQueue = dispatch_queue_create("churchpresenter.qr.session", null)

@OptIn(ExperimentalForeignApi::class)
private fun startSession(session: AVCaptureSession, delegate: QrMetadataDelegate) {
    dispatch_async(sessionQueue) {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return@dispatch_async
        // deviceInputWithDevice:error: — pass null for the error pointer; returns nil on failure
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return@dispatch_async

        session.beginConfiguration()
        if (session.canAddInput(input)) session.addInput(input)
        val metaOutput = AVCaptureMetadataOutput()
        if (session.canAddOutput(metaOutput)) {
            session.addOutput(metaOutput)
            metaOutput.setMetadataObjectsDelegate(delegate, queue = dispatch_get_main_queue())
            metaOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
        }
        session.commitConfiguration()
        session.startRunning()
    }
}

private fun stopSession(session: AVCaptureSession) {
    dispatch_async(sessionQueue) {
        if (session.running) session.stopRunning()
    }
}

/**
 * Hosts the camera preview and sizes it on every layout pass.
 *
 * The layer used to be sized from the `UIKitView` update block, which runs on
 * recomposition — before the view has been laid out, and never again after.
 * The preview was left 0×0 over a black view: the camera ran and nobody could
 * see it (ChurchPresenter-Mobile#26).
 */
@OptIn(ExperimentalForeignApi::class)
private class CameraPreviewView(session: AVCaptureSession) : UIView(frame = CGRectZero.readValue()) {

    private val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    init {
        backgroundColor = UIColor.blackColor
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        // No implicit animation: the layer should snap to the new size.
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        previewLayer.setFrame(bounds)
        CATransaction.commit()

        val interfaceOrientation = window?.windowScene?.interfaceOrientation ?: return
        previewLayer.connection?.let { rotate(it, interfaceOrientation) }
    }

    /** Keeps the picture upright when an iPad is held in landscape. */
    @Suppress("DEPRECATION")
    private fun rotate(connection: AVCaptureConnection, orientation: UIInterfaceOrientation) {
        if (connection.isVideoOrientationSupported()) {
            connection.videoOrientation = videoOrientationFor(orientation)
        }
    }
}

private fun videoOrientationFor(orientation: UIInterfaceOrientation): AVCaptureVideoOrientation =
    when (orientation) {
        UIInterfaceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeLeft
        UIInterfaceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeRight
        UIInterfaceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
        else -> AVCaptureVideoOrientationPortrait
    }

// ── AVCaptureMetadataOutputObjectsDelegate ─────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
private class QrMetadataDelegate(
    private val onDetected: (String) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    // The session stops asynchronously, so a few more frames can still arrive
    // after the first code; report only one.
    private var delivered = false

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        if (delivered) return
        val obj = didOutputMetadataObjects.firstOrNull()
            as? AVMetadataMachineReadableCodeObject ?: return
        val value = obj.stringValue ?: return
        delivered = true
        onDetected(value)
    }
}
