package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** WasmJS/browser: camera QR scanning is not supported — render nothing. */
@Composable
actual fun QrScanButton(onScanned: (String) -> Unit, modifier: Modifier) = Unit

