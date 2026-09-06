package com.church.presenter.churchpresentermobile.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

actual object Analytics {

    private var handle: FirebaseAnalytics? = null
    private var resolved = false

    // Nullable + guarded: Firebase isn't available on a bare JVM (unit tests) or before
    // FirebaseApp is initialised — analytics must never crash the caller. Resolved
    // once, then remembered, however it turned out.
    private val fa: FirebaseAnalytics?
        get() {
            if (!resolved) {
                handle = runCatching { Firebase.analytics }.getOrNull()
                resolved = true
            }
            return handle
        }

    /**
     * Supplies the handle directly instead of looking Firebase up.
     *
     * `internal` purely so the calls below can be checked without an emulator —
     * see the seam guidance in AGENT.md. Passing null puts it back to unresolved,
     * so the ordinary Firebase lookup happens again on the next call — which is
     * what every test that is not about analytics should leave behind.
     */
    internal fun useHandle(analytics: FirebaseAnalytics?) {
        handle = analytics
        resolved = analytics != null
    }

    actual fun init() {
        runCatching {
            fa?.setAnalyticsCollectionEnabled(true)
            // Disable automatic Activity-class screen reporting so only our
            // manual logScreenView() calls appear in "Pages and screens".
            fa?.setSessionTimeoutDuration(1800_000L) // keep default 30-min session
            fa?.setDefaultEventParameters(null)
        }
    }

    actual fun setEnabled(enabled: Boolean) {
        runCatching { fa?.setAnalyticsCollectionEnabled(enabled) }
    }

    actual fun logEvent(name: String, params: Map<String, String>) {
        runCatching {
            val bundle = Bundle()
            params.forEach { (k, v) -> bundle.putString(k, v.take(100)) }
            fa?.logEvent(name, bundle)
        }
        Logger.d("Analytics", "logEvent: $name $params")
    }

    actual fun logScreenView(screenName: String) {
        runCatching {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME,  screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
            fa?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        }
        Logger.d("Analytics", "screenView: $screenName")
    }
}
