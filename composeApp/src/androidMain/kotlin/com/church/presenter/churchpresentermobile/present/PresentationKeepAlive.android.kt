package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.service.PresentationForegroundService
import com.church.presenter.churchpresentermobile.util.ActivityHolder

/**
 * Runs a foreground service holding a CPU wake lock and a high-performance
 * Wi-Fi lock, so the display keeps its connection with the phone in a pocket.
 */
actual object PresentationKeepAlive {
    actual fun start(detail: String?) {
        val context = ActivityHolder.current?.applicationContext ?: return
        PresentationForegroundService.start(context, detail)
    }

    actual fun stop() {
        val context = ActivityHolder.current?.applicationContext ?: return
        PresentationForegroundService.stop(context)
    }
}
