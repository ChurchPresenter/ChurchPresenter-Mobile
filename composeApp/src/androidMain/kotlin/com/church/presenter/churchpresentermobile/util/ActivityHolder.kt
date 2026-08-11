package com.church.presenter.churchpresentermobile.util

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Weakly holds the foreground [Activity].
 *
 * `android.app.Presentation` is a `Dialog`, and a `Dialog` shown with an
 * application context throws `BadTokenException`. The presentation window is
 * created by an output sink that lives outside the Compose tree and has no
 * context of its own, so it needs somewhere to find the real Activity.
 *
 * Held weakly and cleared in `onDestroy` so a rotation or a finished Activity
 * cannot be leaked by this reference.
 */
object ActivityHolder {
    private var ref: WeakReference<Activity>? = null

    /** The current Activity, or null when none is attached. */
    val current: Activity? get() = ref?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }

    fun attach(activity: Activity) {
        ref = WeakReference(activity)
    }

    /** Clears the reference if [activity] is the one currently held. */
    fun detach(activity: Activity) {
        if (ref?.get() === activity) ref = null
    }
}
