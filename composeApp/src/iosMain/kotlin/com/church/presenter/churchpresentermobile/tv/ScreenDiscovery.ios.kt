package com.church.presenter.churchpresentermobile.tv

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Not advertised yet: an iPhone presenting standalone is reached by typing its
 * address on the TV. NSNetService is the follow-up.
 */
actual object PhoneAdvert {
    actual fun start(port: Int) = Unit
    actual fun stop() = Unit
}

/** TV mode is Android-only; an iPhone never looks for phones. */
actual class PhoneFinder actual constructor() {
    actual val found: StateFlow<List<PhoneSource>> = MutableStateFlow(emptyList())
    actual fun start() = Unit
    actual fun stop() = Unit
}

/** TV mode is Android-only; nothing to keep awake here. */
@Composable
actual fun KeepScreenOn() = Unit
