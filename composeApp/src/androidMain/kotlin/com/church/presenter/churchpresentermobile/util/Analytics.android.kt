package com.church.presenter.churchpresentermobile.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

actual object Analytics {

    private val fa: FirebaseAnalytics by lazy { Firebase.analytics }

    actual fun init() {
        fa.setAnalyticsCollectionEnabled(true)
    }

    actual fun logEvent(name: String, params: Map<String, String>) {
        val bundle = Bundle()
        params.forEach { (k, v) -> bundle.putString(k, v.take(100)) }
        fa.logEvent(name, bundle)
        Logger.d("Analytics", "logEvent: $name $params")
    }
}

