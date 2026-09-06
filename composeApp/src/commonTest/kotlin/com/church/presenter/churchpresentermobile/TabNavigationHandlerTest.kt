package com.church.presenter.churchpresentermobile

import com.church.presenter.churchpresentermobile.model.AppTab
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the shortcut-driven tab request.
 *
 * The object is a singleton the platform layer writes to and [App] drains, so
 * each test clears it either side to keep the pending request from leaking.
 */
class TabNavigationHandlerTest {

    @BeforeTest fun setUp() = TabNavigationHandler.consume()

    @AfterTest fun tearDown() = TabNavigationHandler.consume()

    @Test
    fun `nothing is pending initially`() {
        assertNull(TabNavigationHandler.requestedTab.value)
    }

    @Test
    fun `navigateTo publishes the tab`() {
        TabNavigationHandler.navigateTo(AppTab.BIBLE)

        assertEquals(AppTab.BIBLE, TabNavigationHandler.requestedTab.value)
    }

    @Test
    fun `consume marks the request handled so it is not applied twice`() {
        TabNavigationHandler.navigateTo(AppTab.SONGS)

        TabNavigationHandler.consume()

        assertNull(TabNavigationHandler.requestedTab.value)
    }

    @Test
    fun `a newer shortcut wins over an unconsumed one`() {
        TabNavigationHandler.navigateTo(AppTab.SONGS)
        TabNavigationHandler.navigateTo(AppTab.MORE)

        assertEquals(AppTab.MORE, TabNavigationHandler.requestedTab.value)
    }

    @Test
    fun `every tab can be requested`() {
        for (tab in AppTab.entries) {
            TabNavigationHandler.navigateTo(tab)
            assertEquals(tab, TabNavigationHandler.requestedTab.value)
            TabNavigationHandler.consume()
        }
    }

    @Test
    fun `consuming when nothing is pending is harmless`() {
        TabNavigationHandler.consume()

        assertNull(TabNavigationHandler.requestedTab.value)
    }
}
