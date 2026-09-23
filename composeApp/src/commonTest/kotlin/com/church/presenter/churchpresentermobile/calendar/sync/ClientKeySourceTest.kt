package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClientKeySourceTest {

    private val day = 24 * 60 * 60 * 1000L
    private val settings = AppSettings(InMemorySettingsStorage())
    private var fetches = 0
    private var clock = 1_000_000L

    private val freshConfig = """{"clientKey":"freshkeyfreshkey1234","relayUrl":"x"}"""

    private fun source(status: HttpStatusCode = HttpStatusCode.OK, body: String = freshConfig) =
        ClientKeySource(
            settings,
            HttpClient(
                MockEngine { request ->
                    fetches++
                    assertEquals("https://www.churchpresenter.org/api/relay-config", request.url.toString())
                    respond(body, status)
                },
            ),
            now = { clock },
        )

    @Test
    fun aFreshCachedKeyIsUsedWithoutAskingTheWebsite() = runTest {
        settings.relayClientKey = "cachedkeycachedkey12"
        settings.relayClientKeyFetchedAt = clock - day / 2
        assertEquals("cachedkeycachedkey12", source().current())
        assertEquals(0, fetches)
    }

    @Test
    fun aStaleOrMissingKeyIsFetchedAndCached() = runTest {
        assertEquals("freshkeyfreshkey1234", source().current())
        assertEquals("freshkeyfreshkey1234", settings.relayClientKey)
        assertEquals(clock, settings.relayClientKeyFetchedAt)
        settings.relayClientKeyFetchedAt = clock - day - 1
        assertEquals("freshkeyfreshkey1234", source().current())
        assertEquals(2, fetches)
    }

    @Test
    fun whenTheWebsiteIsDownTheCachedKeyStands() = runTest {
        settings.relayClientKey = "cachedkeycachedkey12"
        settings.relayClientKeyFetchedAt = 0
        assertEquals("cachedkeycachedkey12", source(HttpStatusCode.BadGateway, "").current())
        assertNull(source(HttpStatusCode.BadGateway, "").refresh())
        assertEquals("cachedkeycachedkey12", settings.relayClientKey)
    }

    @Test
    fun aKeyOfTheWrongShapeIsNotKept() = runTest {
        assertNull(source(body = """{"clientKey":"short"}""").refresh())
        assertNull(source(body = """{"clientKey":"has spaces in it and more"}""").refresh())
        assertNull(source(body = "<html>").refresh())
        assertEquals("", settings.relayClientKey)
    }
}
