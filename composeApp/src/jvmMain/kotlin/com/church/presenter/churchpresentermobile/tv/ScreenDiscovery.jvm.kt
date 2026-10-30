package com.church.presenter.churchpresentermobile.tv

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** No service discovery here; a TV can still be given this phone's address by hand. */
actual object PhoneAdvert {
    actual fun start(port: Int) = Unit
    actual fun stop() = Unit
}

/** No service discovery here: the picker offers only "Enter address". */
actual class PhoneFinder actual constructor() {
    actual val found: StateFlow<List<PhoneSource>> = MutableStateFlow(emptyList())
    actual fun start() = Unit
    actual fun stop() = Unit
}

/** TV mode is Android-only; nothing to keep awake here. */
@Composable
actual fun KeepScreenOn() = Unit
