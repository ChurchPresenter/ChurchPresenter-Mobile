package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.StatusService
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.viewmodel.StatusViewModel
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The server page's status card answers for the address in the fields: it names what is being
 * typed, Recheck probes that address, and Save checks the connection again.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsAddressStatusTest {

    /** Counts the probes of the saved address, which answers healthy; a typed one never answers. */
    private class Desktop(val settings: AppSettings) {
        var savedProbes = 0
        val draftProbes = mutableListOf<String>()

        val vm = StatusViewModel(
            settings,
            draftServiceFactory = { url, key ->
                draftProbes += url
                StatusService(url, key, "d", mockClient { throw IllegalStateException("Connection refused") })
            },
        ) {
            savedProbes++
            StatusService(
                baseUrl = it.apiBaseUrl,
                apiKey = it.apiKey,
                deviceId = it.deviceId,
                client = mockClient {
                    respond(
                        healthyDesktop,
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                },
            )
        }
    }

    @Test
    fun theStatusCardNamesTheAddressBeingTyped() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        showSettings(settings, status = Desktop(settings).vm)
        awaitThat { isShowing("192.168.1.10:8765 · server 1.4.2") }

        type(UiTags.SETTINGS_HOST, "10.0.0.9")

        awaitThat { isShowing("10.0.0.9:8765") }
        // The version was the saved server's; beside another address it would mislead.
        assertFalse(isShowing("10.0.0.9:8765 · server 1.4.2"))
    }

    @Test
    fun aClearedHostFieldStillNamesTheSavedHost() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        showSettings(settings, status = Desktop(settings).vm)
        awaitThat { isShowing("Connected to the computer") }

        type(UiTags.SETTINGS_HOST, "")

        awaitThat { isShowing("192.168.1.10:8765 · server 1.4.2") }
    }

    @Test
    fun recheckProbesTheTypedAddressNotTheSavedOne() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        val desktop = Desktop(settings)
        showSettings(settings, status = desktop.vm)
        awaitThat { isShowing("Connected to the computer") }

        type(UiTags.SETTINGS_HOST, "10.0.0.99")
        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { isShowing("Can’t reach the computer") }
        assertEquals(listOf("http://10.0.0.99:8765/api"), desktop.draftProbes)
        assertEquals("192.168.1.10", settings.host, "rechecking must not save the address")
    }

    @Test
    fun recheckWithTheSavedAddressChecksTheSavedConnection() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        val desktop = Desktop(settings)
        showSettings(settings, status = desktop.vm)
        awaitThat { isShowing("Connected to the computer") }

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { desktop.savedProbes == 2 }
        assertTrue(desktop.draftProbes.isEmpty())
    }

    @Test
    fun savingANewAddressChecksTheConnectionAgain() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        val desktop = Desktop(settings)
        showSettings(settings, status = desktop.vm)
        awaitThat { isShowing("Connected to the computer") }

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        click(UiTags.SETTINGS_SAVE)

        awaitThat { settings.host == "10.0.0.9" }
        assertEquals(2, desktop.savedProbes, "the status still described the old address")
    }
}
