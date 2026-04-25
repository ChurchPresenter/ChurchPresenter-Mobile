package com.church.presenter.churchpresentermobile.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

actual object Analytics {

    private val fa: FirebaseAnalytics by lazy { Firebase.analytics }

    actual fun init() {
        fa.setAnalyticsCollectionEnabled(true)
        // Disable automatic Activity-class screen reporting so only our
        // manual logScreenView() calls appear in "Pages and screens".
        fa.setSessionTimeoutDuration(1800_000L) // keep default 30-min session
        fa.setDefaultEventParameters(null)
    }

    actual fun logEvent(name: String, params: Map<String, String>) {
        val bundle = Bundle()
        params.forEach { (k, v) -> bundle.putString(k, v.take(100)) }
        fa.logEvent(name, bundle)
        Logger.d("Analytics", "logEvent: $name $params")
    }

    actual fun logScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME,  screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        fa.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        Logger.d("Analytics", "screenView: $screenName")
    }
}
