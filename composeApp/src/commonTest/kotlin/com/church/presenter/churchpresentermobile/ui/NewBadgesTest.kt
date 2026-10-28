package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** "New" badges last one session, and the canvas is offered on a tablet either way up. */
class NewBadgesTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    @Test
    fun aFirstSessionShowsEveryNewBadge() {
        assertEquals(NewFeatures, NewBadges(settings(), seenAtLaunch = emptySet()).showing)
    }

    @Test
    fun aBadgeSeenStaysUpForTheRestOfTheSession() {
        val settings = settings()
        val session = NewBadges(settings, seenAtLaunch = settings.seenNewBadges)
        session.shown(setOf(MoreDestination.CANVAS))
        assertTrue(MoreDestination.CANVAS in session.showing, "not taken down the moment it is drawn")
    }

    @Test
    fun theNextSessionLeavesOffWhatTheLastOneShowed() {
        val settings = settings()
        NewBadges(settings, seenAtLaunch = settings.seenNewBadges).shown(setOf(MoreDestination.CANVAS))

        val next = NewBadges(settings, seenAtLaunch = settings.seenNewBadges)

        assertEquals(setOf(MoreDestination.CALENDAR), next.showing, "only the one never seen is left")
    }

    @Test
    fun seenBadgesSurviveBeingWrittenAndReadBack() {
        val settings = settings()
        settings.seenNewBadges = setOf("CANVAS", "CALENDAR")
        assertEquals(setOf("CANVAS", "CALENDAR"), settings.seenNewBadges)
        assertEquals(emptySet(), settings().seenNewBadges, "a fresh install has seen none")
    }

    @Test
    fun aTabletIsATabletEitherWayUp() {
        assertTrue(isTabletWindow(820.dp, 1180.dp), "iPad upright")
        assertTrue(isTabletWindow(1180.dp, 820.dp), "iPad on its side")
        assertFalse(isTabletWindow(932.dp, 430.dp), "a phone on its side is still a phone")
        assertFalse(isTabletWindow(320.dp, 1180.dp), "an iPad in narrow Split View")
    }
}
