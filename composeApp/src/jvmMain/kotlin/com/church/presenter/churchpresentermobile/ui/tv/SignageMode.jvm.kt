package com.church.presenter.churchpresentermobile.ui.tv

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.model.AppSettings

/** TV mode is Android-only. */
actual val signageSupported: Boolean = false

@Composable
actual fun SignageMode(settings: AppSettings) = Unit
