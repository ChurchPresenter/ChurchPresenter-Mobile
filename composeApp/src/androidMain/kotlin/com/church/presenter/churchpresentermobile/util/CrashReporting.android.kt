package com.church.presenter.churchpresentermobile.util

import android.content.Context
import com.church.presenter.churchpresentermobile.model.getAppContext
import com.church.presenter.churchpresentermobile.network.isExpectedConnectivityError
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User

/**
 * Sentry is the sole crash reporter; Crashlytics collection stays off so its
 * own native crash handler never races Sentry's for the same crash (this is
 * what caused CHURCH-PRESENTER-MOBILE-1P on iOS, where both installed a mach
 * exception handler). The `log`/`setCustomKey`/etc. calls below still reach
 * Crashlytics, but with collection disabled they're inert no-ops — kept in
 * place only so re-enabling collection later needs no call-site changes.
 */
actual object CrashReporting {
    actual fun init() {
        runCatching { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false) }
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
            // Last line of defence against connectivity noise.
            //
            // Call sites already ask shouldReportAsNonFatal() before reporting,
            // but that cannot cover what the SDK captures on its own: an
            // exception escaping an OkHttp dispatcher thread reaches the
            // uncaught-exception integration without passing through any of our
            // code. That is how "No route to host" and connect timeouts to a LAN
            // address kept arriving from builds whose call-site checks already
            // excluded them.
            //
            // An unreachable companion server is this client's normal state, not
            // a defect, so it is dropped wherever it is raised.
            options.setBeforeSend { event, _ ->
                if (event.throwable?.isExpectedConnectivityError() == true) null else event
            }
        }
    }

    /**
     * Toggles crash reporting per the user's privacy preference. Crashlytics
     * collection stays off regardless (see the class doc); Sentry doesn't have
     * a native runtime flag, so we [Sentry.close] it when disabling and re-run
     * [initSentry] (using the context stashed by [getAppContext]) when re-enabling.
     */
    actual fun setEnabled(enabled: Boolean) {
        runCatching { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false) }
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

