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

/** Not browsed yet, so an iPhone's TV list stays empty; see [PhoneAdvert]. */
actual class ServiceFinder actual constructor(serviceType: String) {
    actual val found: StateFlow<List<FoundService>> = MutableStateFlow(emptyList())
    actual fun start() = Unit
    actual fun stop() = Unit
}

/** TV mode is Android-only; nothing listens here. */
actual class TvControlServer actual constructor() {
    actual val requested: StateFlow<TvSource?> = MutableStateFlow(null)
    actual val home: StateFlow<Int> = MutableStateFlow(0)
    actual fun start() = Unit
    actual fun stop() = Unit
    actual fun consume() = Unit
}

/** TV mode is Android-only; nothing to keep awake here. */
@Composable
actual fun KeepScreenOn() = Unit
