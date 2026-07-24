package com.church.presenter.churchpresentermobile.util

import android.content.Context
import com.church.presenter.churchpresentermobile.model.getAppContext
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
        runCatching { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true) }
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

    /**
     * Toggles crash reporting per the user's privacy preference. Firebase Crashlytics
     * has a native runtime flag; Sentry doesn't, so we [Sentry.close] it when disabling
     * and re-run [initSentry] (using the context stashed by [getAppContext]) when
     * re-enabling.
     */
    actual fun setEnabled(enabled: Boolean) {
        runCatching { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled) }
        if (enabled) {
            runCatching { getAppContext()?.let { initSentry(it) } }
        } else {
            runCatching { Sentry.close() }
        }
    }

    actual fun log(message: String) {
        runCatching { FirebaseCrashlytics.getInstance().log(message) }
        runCatching { Sentry.addBreadcrumb(message) }
    }

    actual fun recordException(throwable: Throwable) {
        runCatching { FirebaseCrashlytics.getInstance().recordException(throwable) }
        runCatching { Sentry.captureException(throwable) }
    }

    actual fun setUserId(userId: String) {
        runCatching { FirebaseCrashlytics.getInstance().setUserId(userId) }
        runCatching { Sentry.setUser(User().apply { id = userId }) }
    }

    actual fun setCustomKey(key: String, value: String) {
        runCatching { FirebaseCrashlytics.getInstance().setCustomKey(key, value) }
        runCatching { Sentry.setTag(key, value) }
    }
}

