package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Web browsers have no system back button to intercept; no-op here.
}

