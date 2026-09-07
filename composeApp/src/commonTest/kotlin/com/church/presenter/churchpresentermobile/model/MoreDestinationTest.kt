package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests [MoreDestination.forMode] — which secondary screens each mode offers.
 *
 * Standalone has no desktop, so several of these would only time out there. The
 * gate is what keeps them off the launcher rather than letting the operator find
 * out by tapping one mid-service.
 */
class MoreDestinationTest {

    private val remote = MoreDestination.forMode(AppMode.REMOTE)
    private val standalone = MoreDestination.forMode(AppMode.STANDALONE)

    @Test
    fun remoteOffersEveryDestination() {
        assertEquals(MoreDestination.entries.toSet(), remote.toSet())
    }

    @Test
    fun standaloneOffersFewer() {
        assertTrue(standalone.size < remote.size)
        assertTrue(standalone.isNotEmpty(), "standalone still needs a More launcher")
    }

    @Test
    fun everyStandaloneDestinationIsAlsoARemoteOne() {
        // Standalone is a subset; a destination unique to it would have no remote
        // equivalent and no way to be reached from the usual mode.
        assertTrue(remote.containsAll(standalone), "standalone-only: ${standalone - remote.toSet()}")
    }

    @Test
    fun theDestinationsThatNeedTheDesktopsDataAreExcludedFromStandalone() {
        // Both read something only the computer holds.
        assertFalse(MoreDestination.QA in standalone, "Q&A is hosted by the desktop")
        assertFalse(MoreDestination.DICTIONARY in standalone, "Strong's data lives on the desktop")
    }

    @Test
    fun announcementsSurviveIntoStandaloneAsADifferentScreen() {
        // Same destination, two screens: remote adds to the desktop's schedule and
        // has timer types with no local renderer; standalone gets Notices, which
        // projects a notice written in the Library onto this device's own outputs.
        assertTrue(MoreDestination.ANNOUNCEMENTS in standalone)
        assertTrue(MoreDestination.ANNOUNCEMENTS in remote)
    }

    @Test
    fun photosAndWebSurviveIntoStandalone() {
        // Both have local equivalents: pick from this device, present from here.
        assertTrue(MoreDestination.PICTURES in standalone)
        assertTrue(MoreDestination.WEB in standalone)
    }

    @Test
    fun contactIsAlwaysReachable() {
        // It posts to a public endpoint, so it works with no desktop at all — and
        // it is how someone reports that the rest is broken.
        assertTrue(MoreDestination.CONTACT in remote)
        assertTrue(MoreDestination.CONTACT in standalone)
    }

    @Test
    fun neitherModeListsADestinationTwice() {
        assertEquals(remote.size, remote.toSet().size)
        assertEquals(standalone.size, standalone.toSet().size)
    }

    @Test
    fun theOrderIsStableAcrossCalls() {
        // The launcher is drawn from this list; a shifting order moves rows under
        // the operator's finger between visits.
        assertEquals(remote, MoreDestination.forMode(AppMode.REMOTE))
        assertEquals(standalone, MoreDestination.forMode(AppMode.STANDALONE))
    }

    @Test
    fun everyModeIsAnswered() {
        for (mode in AppMode.entries) {
            assertTrue(MoreDestination.forMode(mode).isNotEmpty(), "$mode has no More entries")
        }
    }
}
