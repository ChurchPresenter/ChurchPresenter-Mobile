package com.church.presenter.churchpresentermobile

import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSValue
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.UIKit.UIKeyboardAnimationDurationUserInfoKey
import platform.UIKit.UIKeyboardDidChangeFrameNotification
import platform.UIKit.UIKeyboardDidHideNotification
import platform.UIKit.UIKeyboardDidShowNotification
import platform.UIKit.UIKeyboardFrameEndUserInfoKey
import platform.UIKit.UIKeyboardWillChangeFrameNotification
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

/**
 * Works around Compose's keyboard inset going wrong when an iPad rotates with the keyboard up.
 *
 * Compose (1.11, `ComposeSceneKeyboardOffsetManager`) measures how much of the window the keyboard
 * covers only when a `UIKeyboardWillChangeFrame` notification arrives. During a rotation that
 * notification comes while the Compose view still has its old size, so the overlap is worked out
 * against the wrong frame — hundreds of points too large — and nothing measures it again once the
 * rotation finishes. The bottom of the screen stayed padded by a keyboard that was not there.
 *
 * Once the keyboard (or the rotation) has settled, this repeats the keyboard's last frame as a
 * zero-length `WillChangeFrame`. The frame is the one Compose already holds, so it does not
 * animate; it only measures again, now against the view's real size. Outside a rotation the
 * repeat produces the value already there, and Compose ignores an unchanged overlap.
 */
internal object KeyboardInsetResync {
    /** Long enough for an iPad's rotation animation to finish. */
    private const val AFTER_ROTATION_NANOS = 600_000_000L

    private var installed = false
    private var lastFrame: NSValue? = null

    fun install() {
        if (installed) return
        installed = true
        val center = NSNotificationCenter.defaultCenter
        val main = NSOperationQueue.mainQueue
        center.addObserverForName(UIKeyboardDidShowNotification, null, main) { note -> settled(note) }
        center.addObserverForName(UIKeyboardDidChangeFrameNotification, null, main) { note -> settled(note) }
        center.addObserverForName(UIKeyboardDidHideNotification, null, main) { lastFrame = null }
        UIDevice.currentDevice.beginGeneratingDeviceOrientationNotifications()
        center.addObserverForName(UIDeviceOrientationDidChangeNotification, null, main) {
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, AFTER_ROTATION_NANOS), dispatch_get_main_queue()) {
                repost()
            }
        }
    }

    private fun settled(note: NSNotification?) {
        lastFrame = note?.userInfo?.get(UIKeyboardFrameEndUserInfoKey) as? NSValue
        repost()
    }

    private fun repost() {
        val frame = lastFrame ?: return
        NSNotificationCenter.defaultCenter.postNotificationName(
            UIKeyboardWillChangeFrameNotification,
            `object` = null,
            userInfo = mapOf<Any?, Any?>(
                UIKeyboardFrameEndUserInfoKey to frame,
                UIKeyboardAnimationDurationUserInfoKey to 0.0,
            ),
        )
    }
}
