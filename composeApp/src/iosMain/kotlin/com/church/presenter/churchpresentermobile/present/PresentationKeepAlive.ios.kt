package com.church.presenter.churchpresentermobile.present

import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Only stops the screen auto-locking.
 *
 * iOS gives no supported way to keep a TCP listener alive once the app is
 * suspended, so the presentation server is foreground-only and the UI says so.
 * Keeping the screen awake is what makes "leave the app open" workable in
 * practice rather than a trap.
 */
actual object PresentationKeepAlive {
    actual fun start(detail: String?) = setIdleTimerDisabled(true)

    actual fun stop() = setIdleTimerDisabled(false)

    private fun setIdleTimerDisabled(disabled: Boolean) {
        dispatch_async(dispatch_get_main_queue()) {
            UIApplication.sharedApplication.idleTimerDisabled = disabled
        }
    }
}
