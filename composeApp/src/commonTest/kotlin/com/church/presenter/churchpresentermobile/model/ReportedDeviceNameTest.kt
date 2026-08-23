package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.deviceName
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Which name this device reports, and which one wins. */
class ReportedDeviceNameTest {

    @Test
    fun aTypedNameBeatsTheOneTheOsGives() {
        // The operator typed it precisely because the OS name was blank, useless
        // ("iPhone"), or the wrong thing to call this device in this building.
        val settings = AppSettings(InMemorySettingsStorage())
        settings.displayName = "Sound desk"

        assertEquals("Sound desk", settings.reportedDeviceName)
    }

    @Test
    fun withNothingTypedTheOsNameIsUsed() {
        val settings = AppSettings(InMemorySettingsStorage())

        assertEquals(deviceName().trim(), settings.reportedDeviceName)
    }

    @Test
    fun clearingTheFieldFallsBackRatherThanReportingNothing() {
        val settings = AppSettings(InMemorySettingsStorage())
        settings.displayName = "Sound desk"
        settings.displayName = ""

        assertEquals(deviceName().trim(), settings.reportedDeviceName)
    }

    @Test
    fun theNameNeverArrivesPaddedWithWhitespace() {
        val settings = AppSettings(InMemorySettingsStorage())
        settings.displayName = "\t Stage left \n"

        assertEquals("Stage left", settings.reportedDeviceName)
        assertTrue(settings.reportedDeviceName == settings.reportedDeviceName.trim())
    }
}
