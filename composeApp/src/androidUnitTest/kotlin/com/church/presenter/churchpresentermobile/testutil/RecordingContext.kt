package com.church.presenter.churchpresentermobile.testutil

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences

/**
 * A [Context] that answers for preferences and remembers what was asked of it.
 *
 * Subclasses [ContextWrapper] over a null base deliberately: on the unit-test
 * JVM every inherited method is an `android.jar` stub returning a default, so
 * nothing delegates anywhere and only the overrides below do real work.
 *
 * @param failStarts When true, every attempt to start a service is refused the
 *   way Android 12+ refuses a background foreground-service start.
 * @param appContext What [getApplicationContext] hands back. Defaults to this
 *   same instance; pass a different one to tell the two apart, which is how a
 *   real Activity behaves.
 */
class RecordingContext(
    private val failStarts: Boolean = false,
    private val appContext: Context? = null,
) : ContextWrapper(null) {

    /** Preference files handed out so far, keyed by the name that was asked for. */
    val preferenceFiles: MutableMap<String, FakePreferences> = mutableMapOf()

    /** Services asked to start, in order, and how they were asked. */
    val startedServices: MutableList<String> = mutableListOf()

    /** How many times a service was asked to stop. */
    var stopCount: Int = 0
        private set

    override fun getApplicationContext(): Context = appContext ?: this

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
        preferenceFiles.getOrPut(name.orEmpty()) { FakePreferences() }

    override fun startService(service: Intent?): ComponentName? = record("startService")

    override fun startForegroundService(service: Intent?): ComponentName? =
        record("startForegroundService")

    override fun stopService(name: Intent?): Boolean {
        if (failStarts) error("service not allowed to stop")
        stopCount += 1
        return true
    }

    private fun record(how: String): ComponentName? {
        if (failStarts) error("ForegroundServiceStartNotAllowed")
        startedServices += how
        return null
    }
}
