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
        settings.customDeviceName = "Sound desk"

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
        settings.customDeviceName = "Sound desk"
        settings.customDeviceName = ""

        assertEquals(deviceName().trim(), settings.reportedDeviceName)
    }

    @Test
    fun namingThePersonDoesNotRenameTheDevice() {
        // The desktop shows a question's author and the device it came from on
        // separate lines; one field feeding both is what this split undid.
        val settings = AppSettings(InMemorySettingsStorage())
        settings.displayName = "Ada"

        assertEquals(deviceName().trim(), settings.reportedDeviceName)
    }

    @Test
    fun namingTheDeviceDoesNotRenameThePerson() {
        val settings = AppSettings(InMemorySettingsStorage())
        settings.customDeviceName = "Sound desk"

        assertEquals("", settings.displayName)
    }

    @Test
    fun theNameNeverArrivesPaddedWithWhitespace() {
        val settings = AppSettings(InMemorySettingsStorage())
        settings.customDeviceName = "\t Stage left \n"

        assertEquals("Stage left", settings.reportedDeviceName)
        assertTrue(settings.reportedDeviceName == settings.reportedDeviceName.trim())
    }
}
