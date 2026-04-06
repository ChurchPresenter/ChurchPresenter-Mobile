package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

/**
 * Multiplatform back-press handler.
 *
 * On Android this intercepts the system back button / gesture.
 * On iOS it is a no-op because the platform handles back gestures natively.
 * On JS and WasmJS it is a no-op because web browsers have no system back button.
 *
 * @param enabled When false the handler is inactive and back events pass through.
 * @param onBack  Called when the system back action is triggered.
 */
@Composable
expect fun AppBackHandler(enabled: Boolean = true, onBack: () -> Unit)

