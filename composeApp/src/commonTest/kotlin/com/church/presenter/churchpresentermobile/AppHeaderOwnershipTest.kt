package com.church.presenter.churchpresentermobile

import com.church.presenter.churchpresentermobile.model.AppTab
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Which tabs hang their own header over their panes, and which leave it to the
 * shell.
 *
 * Wrong in either direction is visible and silly: say a tab draws its own when
 * it does not and the screen loses its title, its schedule button and its
 * settings gear outright; say it does not when it does and the operator gets two
 * stacked headers, one of them naming the wrong thing.
 */
class AppHeaderOwnershipTest {

    @Test
    fun `on a phone the shell draws every tab's header`() {
        AppTab.entries.forEach { tab ->
            assertFalse(tabDrawsOwnHeader(tab, twoPane = false), "$tab claims its header on a phone")
        }
    }

    @Test
    fun `a split tab draws its own`() {
        listOf(AppTab.SONGS, AppTab.BIBLE, AppTab.MEDIA, AppTab.MORE, AppTab.LIBRARY, AppTab.PRESENT)
            .forEach { tab ->
                assertTrue(tabDrawsOwnHeader(tab, twoPane = true), "$tab lays itself out in panes")
            }
    }

    @Test
    fun `the desktop decks tab keeps the shell's header even when split`() {
        // PRESENTATION is the one tab the tablet design does not cover, so it
        // keeps the single bar and the one-screen layout until it does. If this
        // starts failing, someone split it and forgot its header.
        assertFalse(tabDrawsOwnHeader(AppTab.PRESENTATION, twoPane = true))
    }
}
