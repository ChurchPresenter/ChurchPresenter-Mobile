package com.church.presenter.churchpresentermobile

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests [DeepLinkHandler.handle] — the churchpresenter://connect deep-link parser. */
class DeepLinkHandlerTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    @Test
    fun validLinkAppliesHostPortApiKeyAndBumpsCount() {
        val s = settings()
        val before = DeepLinkHandler.appliedCount.value
        val ok = DeepLinkHandler.handle("churchpresenter://connect?host=10.0.0.5&port=9000&apikey=secret", s)
        assertTrue(ok)
        assertEquals("10.0.0.5", s.host)
        assertEquals(9000, s.port)
        assertEquals("secret", s.apiKey)
        assertEquals(before + 1, DeepLinkHandler.appliedCount.value)
    }

    @Test
    fun apiKeyIsOptional() {
        val s = settings()
        assertTrue(DeepLinkHandler.handle("CHURCHPRESENTER://connect?host=1.2.3.4&port=8765", s))
        assertEquals("1.2.3.4", s.host)
        assertEquals("", s.apiKey)
    }

    @Test
    fun rejectsWrongSchemeMissingQueryAndInvalidHostOrPort() {
        val s = settings()
        assertFalse(DeepLinkHandler.handle("https://example.com", s))
        assertFalse(DeepLinkHandler.handle("churchpresenter://connect", s))
        assertFalse(DeepLinkHandler.handle("churchpresenter://connect?host=h&port=99999", s))
        assertFalse(DeepLinkHandler.handle("churchpresenter://connect?host=h&port=abc", s))
        assertFalse(DeepLinkHandler.handle("churchpresenter://connect?port=8765", s)) // no host
    }
}
