package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS handles back gestures natively through the navigation stack; no-op here.
}

