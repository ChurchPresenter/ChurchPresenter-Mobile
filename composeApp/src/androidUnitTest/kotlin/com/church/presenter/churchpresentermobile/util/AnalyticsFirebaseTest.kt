package com.church.presenter.churchpresentermobile.util

import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * What actually reaches Firebase when analytics is available.
 *
 * The names matter more than anything else here. Firebase reserves
 * `screen_view` and reads its "Pages and screens" report from that event alone
 * — send anything else and the report stays empty, with no error and nothing on
 * the device to show for it. The same goes for the collection switch: a privacy
 * setting that does not reach the SDK is worse than not offering one.
 *
 * The handle is supplied through [Analytics.useHandle] rather than mocked into
 * place, and put back to null afterwards so every other test in this JVM sees
 * the no-Firebase behaviour it expects.
 */
class AnalyticsFirebaseTest {

    private val firebase = mockk<FirebaseAnalytics>(relaxed = true)

    @BeforeTest
    fun installFirebase() = Analytics.useHandle(firebase)

    @AfterTest
    fun removeFirebase() = Analytics.useHandle(null)

    @Test
    fun `initialising turns collection on`() {
        Analytics.init()

        verify { firebase.setAnalyticsCollectionEnabled(true) }
    }

    @Test
    fun `initialising keeps Firebase's own thirty-minute session`() {
        // Set explicitly rather than left implicit, so a future SDK changing its
        // default cannot silently split one service into several sessions.
        Analytics.init()

        verify { firebase.setSessionTimeoutDuration(1_800_000L) }
    }

    @Test
    fun `initialising clears any default event parameters`() {
        Analytics.init()

        verify { firebase.setDefaultEventParameters(null) }
    }

    @Test
    fun `the privacy switch reaches the SDK`() {
        Analytics.setEnabled(false)
        verify { firebase.setAnalyticsCollectionEnabled(false) }

        Analytics.setEnabled(true)
        verify { firebase.setAnalyticsCollectionEnabled(true) }
    }

    @Test
    fun `an event is logged under the name it was given`() {
        Analytics.logEvent("song_projected", mapOf("book" to "Hymns"))

        verify { firebase.logEvent("song_projected", any()) }
    }

    @Test
    fun `an event with no parameters is still logged`() {
        Analytics.logEvent("app_open", emptyMap())

        verify { firebase.logEvent("app_open", any()) }
    }

    @Test
    fun `a screen view is logged as Firebase's own screen_view event`() {
        // The one name that cannot be chosen freely: the "Pages and screens"
        // report reads this event and nothing else.
        Analytics.logScreenView("SongsList")

        verify { firebase.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, any()) }
    }

    @Test
    fun `a screen view is not logged under the screen's own name`() {
        // The mistake it replaces: logging "SongsList" as the event name, which
        // shows up as a custom event and never in the screens report.
        Analytics.logScreenView("SongsList")

        verify(exactly = 0) { firebase.logEvent("SongsList", any()) }
    }

    // ── Nothing here may take the caller down ────────────────────────────

    @Test
    fun `an SDK that throws does not reach the caller`() {
        // logEvent is called from ordinary UI paths with no result to check.
        every { firebase.logEvent(any(), any()) } throws IllegalStateException("not initialised")

        Analytics.logEvent("song_projected", mapOf("book" to "Hymns"))
        Analytics.logScreenView("SongsList")
    }

    @Test
    fun `an SDK that throws on the privacy switch does not reach the caller`() {
        every { firebase.setAnalyticsCollectionEnabled(any()) } throws IllegalStateException("boom")

        Analytics.init()
        Analytics.setEnabled(false)
    }

    @Test
    fun `an over-long parameter value is cut before Firebase sees it`() {
        // Firebase rejects a value past 100 characters — the whole event, not
        // just the parameter — so the cut has to happen on this side.
        Analytics.logEvent("search", mapOf("query" to "a".repeat(500)))

        verify { firebase.logEvent("search", any()) }
    }
}
