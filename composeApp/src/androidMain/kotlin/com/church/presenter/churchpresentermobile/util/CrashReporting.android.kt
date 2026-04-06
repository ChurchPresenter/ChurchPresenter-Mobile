package com.church.presenter.churchpresentermobile.util

import com.google.firebase.crashlytics.FirebaseCrashlytics

actual object CrashReporting {
    actual fun init() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }

    actual fun log(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
    }

    actual fun recordException(throwable: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    actual fun setUserId(userId: String) {
        FirebaseCrashlytics.getInstance().setUserId(userId)
    }
}

