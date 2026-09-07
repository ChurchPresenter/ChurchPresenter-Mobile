package com.church.presenter.churchpresentermobile.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Analytics and crash reporting where there is no backend to send to.
 *
 * These are called from the middle of ordinary work — a tab change, a failed
 * sync, a projected song — on every target, including the ones with no Firebase
 * at all. So the contract that matters is not what they send but that they never
 * interrupt: a throw here would turn a missing analytics backend into a crash
 * on the surface the operator is using.
 */
class TelemetryStubTest {

    // ── Analytics ────────────────────────────────────────────────────────

    @Test
    fun `starting analytics on a target without it is harmless`() {
        Analytics.init()
    }

    @Test
    fun `switching analytics on is harmless`() {
        // The privacy toggle writes through on every change.
        Analytics.setEnabled(true)
    }

    @Test
    fun `switching analytics off is harmless`() {
        Analytics.setEnabled(false)
    }

    @Test
    fun `switching analytics repeatedly is harmless`() {
        repeat(3) {
            Analytics.setEnabled(true)
            Analytics.setEnabled(false)
        }
    }

    @Test
    fun `logging an event is harmless`() {
        Analytics.logEvent(AnalyticsEvent.SONG_PROJECTED)
    }

    @Test
    fun `logging an event with parameters is harmless`() {
        Analytics.logEvent(AnalyticsEvent.TAB_SELECTED, mapOf(AnalyticsParam.TAB_NAME to "Songs"))
    }

    @Test
    fun `logging an event with an over-long value is harmless`() {
        // Firebase truncates at 100 characters; a caller does not have to know.
        Analytics.logEvent(AnalyticsEvent.SONG_OPENED, mapOf("title" to "x".repeat(500)))
    }

    @Test
    fun `logging an event with an empty name is harmless`() {
        Analytics.logEvent("")
    }

    @Test
    fun `logging an event with many parameters is harmless`() {
        Analytics.logEvent(
            AnalyticsEvent.SETTINGS_SAVED,
            (1..25).associate { "key$it" to "value$it" },
        )
    }

    @Test
    fun `logging an event with accented text is harmless`() {
        Analytics.logEvent(AnalyticsEvent.SONG_OPENED, mapOf("title" to "Święty"))
    }

    @Test
    fun `logging a screen view is harmless`() {
        Analytics.logScreenView(AnalyticsScreen.LIBRARY)
    }

    @Test
    fun `logging a screen view with an empty name is harmless`() {
        Analytics.logScreenView("")
    }

    @Test
    fun `logging before init is harmless`() {
        // Startup order is not something every call site can depend on.
        Analytics.logEvent(AnalyticsEvent.TAB_SELECTED)
        Analytics.logScreenView(AnalyticsScreen.SONGS)
    }

    // ── Crash reporting ──────────────────────────────────────────────────

    @Test
    fun `starting crash reporting on a target without it is harmless`() {
        CrashReporting.init()
    }

    @Test
    fun `switching crash reporting on is harmless`() {
        CrashReporting.setEnabled(true)
    }

    @Test
    fun `switching crash reporting off is harmless`() {
        CrashReporting.setEnabled(false)
    }

    @Test
    fun `leaving a breadcrumb is harmless`() {
        CrashReporting.log("sync started")
    }

    @Test
    fun `leaving an empty breadcrumb is harmless`() {
        CrashReporting.log("")
    }

    @Test
    fun `recording an exception is harmless`() {
        // Called from catch blocks that must not throw again.
        CrashReporting.recordException(IllegalStateException("something went wrong"))
    }

    @Test
    fun `recording an exception with no message is harmless`() {
        CrashReporting.recordException(RuntimeException())
    }

    @Test
    fun `recording a nested exception is harmless`() {
        CrashReporting.recordException(IllegalStateException("outer", RuntimeException("inner")))
    }

    @Test
    fun `naming the user is harmless`() {
        CrashReporting.setUserId("device-1234")
    }

    @Test
    fun `naming an empty user is harmless`() {
        CrashReporting.setUserId("")
    }

    @Test
    fun `setting a custom key is harmless`() {
        CrashReporting.setCustomKey("mode", "standalone")
    }

    @Test
    fun `setting a custom key with an empty value is harmless`() {
        CrashReporting.setCustomKey("mode", "")
    }

    @Test
    fun `reporting before init is harmless`() {
        CrashReporting.log("before anything was set up")
        CrashReporting.recordException(IllegalStateException("early"))
    }

    // ── The names that reach the console ─────────────────────────────────

    @Test
    fun `every event name is one Firebase will accept`() {
        // Max 40 characters, letters digits and underscores, starting with a
        // letter. A name Firebase rejects is dropped silently at the far end,
        // which is indistinguishable from the event never firing.
        val rule = Regex("^[a-zA-Z][a-zA-Z0-9_]{0,39}$")

        eventNames().forEach { name ->
            assertTrue(rule.matches(name), "'$name' is not a name Firebase accepts")
        }
    }

    @Test
    fun `no two events share a name`() {
        // Two call sites writing to one name make both reports meaningless.
        val names = eventNames()

        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `every screen name is something a person would read`() {
        // These land in the "Pages and screens" report, where a raw class name
        // is what this constant exists to avoid.
        screenNames().forEach { name ->
            assertTrue(name.isNotBlank(), "a screen name is blank")
            assertTrue(name.first().isUpperCase(), "'$name' does not read as a title")
        }
    }

    @Test
    fun `no two screens share a name`() {
        val names = screenNames()

        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `every parameter key is one Firebase will accept`() {
        val rule = Regex("^[a-zA-Z][a-zA-Z0-9_]{0,39}$")
        val keys = listOf(AnalyticsParam.TAB_NAME, AnalyticsParam.VERSE_INDEX, AnalyticsParam.SLIDE_INDEX)

        keys.forEach { key ->
            assertTrue(rule.matches(key), "'$key' is not a parameter key Firebase accepts")
        }
    }

    private fun eventNames() = listOf(
        AnalyticsEvent.TAB_SELECTED,
        AnalyticsEvent.SCHEDULE_DRAWER_OPENED,
        AnalyticsEvent.SONG_OPENED,
        AnalyticsEvent.SONG_PROJECTED,
        AnalyticsEvent.SONG_DISPLAY_CLEARED,
        AnalyticsEvent.SONG_VERSE_SELECTED,
        AnalyticsEvent.SONG_ADDED_TO_SCHEDULE,
        AnalyticsEvent.BIBLE_BOOK_SELECTED,
        AnalyticsEvent.BIBLE_CHAPTER_SELECTED,
        AnalyticsEvent.BIBLE_PROJECTED,
        AnalyticsEvent.BIBLE_DISPLAY_CLEARED,
        AnalyticsEvent.BIBLE_ADDED_TO_SCHEDULE,
        AnalyticsEvent.PICTURE_FOLDER_OPENED,
        AnalyticsEvent.PICTURE_SELECTED,
        AnalyticsEvent.PICTURE_ADDED_TO_SCHEDULE,
        AnalyticsEvent.PHOTO_UPLOADED,
        AnalyticsEvent.SLIDE_SELECTED,
        AnalyticsEvent.PRESENTATION_ADDED,
        AnalyticsEvent.PRESENTATION_UPLOADED,
        AnalyticsEvent.SETTINGS_SAVED,
    )

    private fun screenNames() = listOf(
        AnalyticsScreen.SONGS,
        AnalyticsScreen.SONG_DETAIL,
        AnalyticsScreen.BIBLE_BOOKS,
        AnalyticsScreen.BIBLE_CHAPTERS,
        AnalyticsScreen.BIBLE_VERSES,
        AnalyticsScreen.PICTURES,
        AnalyticsScreen.MEDIA,
        AnalyticsScreen.PRESENTATIONS,
        AnalyticsScreen.SETTINGS,
        AnalyticsScreen.SCHEDULE,
        AnalyticsScreen.CONNECT_SETUP,
        AnalyticsScreen.STATUS,
        AnalyticsScreen.QA_ADMIN,
        AnalyticsScreen.MORE,
        AnalyticsScreen.DICTIONARY,
        AnalyticsScreen.ANNOUNCEMENTS,
        AnalyticsScreen.WEB,
        AnalyticsScreen.STANDALONE,
        AnalyticsScreen.MODE_PICKER,
        AnalyticsScreen.LIBRARY,
    )
}
