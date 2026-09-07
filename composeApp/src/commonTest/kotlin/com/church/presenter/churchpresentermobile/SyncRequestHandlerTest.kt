package com.church.presenter.churchpresentermobile

import com.church.presenter.churchpresentermobile.ui.library.SyncSection
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the cross-tab request for the Library sync sheet.
 *
 * The object is a singleton shared by the whole app, so every test both starts
 * and ends by clearing it — a leftover request would reopen the sheet in an
 * unrelated test.
 */
class SyncRequestHandlerTest {

    @BeforeTest fun setUp() = SyncRequestHandler.consume()

    @AfterTest fun tearDown() = SyncRequestHandler.consume()

    @Test
    fun `nothing is pending initially`() {
        assertNull(SyncRequestHandler.requested.value)
    }

    @Test
    fun `request publishes the section`() {
        SyncRequestHandler.request(SyncSection.SONGS)

        assertEquals(SyncSection.SONGS, SyncRequestHandler.requested.value)
    }

    @Test
    fun `consume clears the request so a later visit does not reopen the sheet`() {
        SyncRequestHandler.request(SyncSection.BIBLE)

        SyncRequestHandler.consume()

        assertNull(SyncRequestHandler.requested.value)
    }

    @Test
    fun `a second request replaces the first`() {
        SyncRequestHandler.request(SyncSection.SONGS)
        SyncRequestHandler.request(SyncSection.BIBLE)

        assertEquals(SyncSection.BIBLE, SyncRequestHandler.requested.value)
    }

    @Test
    fun `consuming when nothing is pending is harmless`() {
        SyncRequestHandler.consume()
        SyncRequestHandler.consume()

        assertNull(SyncRequestHandler.requested.value)
    }
}
