package com.church.presenter.churchpresentermobile.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/** [ServerDraft.address]: what the status card and the sidebar name while the fields are edited. */
class ServerDraftAddressTest {

    private fun draft(host: String, port: String) =
        ServerDraft(host, port, apiKey = "", customDeviceName = "", displayName = "")

    @Test
    fun theTypedAddressIsNamed() {
        assertEquals("10.0.0.9:9000", draft("10.0.0.9", "9000").address(storedSettings(host = "192.168.1.50")))
    }

    @Test
    fun surroundingSpacesAreNotShown() {
        assertEquals("10.0.0.9:8765", draft(" 10.0.0.9 ", " 8765 ").address(storedSettings()))
    }

    @Test
    fun aBlankFieldFallsBackToTheSavedValue() {
        val saved = storedSettings(host = "192.168.1.50", port = 8765)
        assertEquals("192.168.1.50:9000", draft("", "9000").address(saved))
        assertEquals("10.0.0.9:8765", draft("10.0.0.9", " ").address(saved))
    }
}
