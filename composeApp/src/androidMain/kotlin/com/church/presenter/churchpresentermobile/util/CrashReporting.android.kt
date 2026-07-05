package com.church.presenter.churchpresentermobile.util

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User

/**
 * Reports to both Firebase Crashlytics and Sentry so existing dashboards keep
 * working while Sentry (already used by the desktop app) becomes the primary
 * cross-platform crash backend.
 */
actual object CrashReporting {
    actual fun init() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }

    /**
     * Starts the Sentry Android SDK. Called once from [ChurchPresenterApp.onCreate],
     * which runs before any Activity — needs a [Context], so it's not part of the
     * shared [init] contract used by the other platforms.
     */
    fun initSentry(context: Context) {
        SentryAndroid.init(context) { options ->
            options.dsn = SENTRY_DSN
            options.environment = if (isDebugBuild) "development" else "production"
            options.release = appVersion
            options.isAttachStacktrace = true
            options.isAttachThreads = false
            // Tracing/performance transactions consume a separate quota from error
            // events — off in production to avoid burning it on auto-instrumented
            // activity/app-start transactions we don't otherwise use.
            options.tracesSampleRate = if (isDebugBuild) 1.0 else 0.0
        }
    }

    actual fun log(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
        Sentry.addBreadcrumb(message)
    }

    actual fun recordException(throwable: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable)
        Sentry.captureException(throwable)
    }

    actual fun setUserId(userId: String) {
        FirebaseCrashlytics.getInstance().setUserId(userId)
        Sentry.setUser(User().apply { id = userId })
    }

    actual fun setCustomKey(key: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        Sentry.setTag(key, value)
    }
}

