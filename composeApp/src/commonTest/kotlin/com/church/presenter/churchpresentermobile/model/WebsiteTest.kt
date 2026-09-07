package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the wire shape of the website payload.
 *
 * The desktop infers a `WebsiteItem` from the presence of `url` and reads the
 * remaining fields by name, so the JSON key names are a contract rather than an
 * implementation detail — renaming one silently stops the item from projecting.
 */
class WebsiteTest {

    /** The same encoder [WebService] uses — `encodeDefaults` is what puts `type` on the wire. */
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test
    fun `a request serialises to the shape the desktop reads`() {
        val request = WebsiteRequest(
            WebsiteItemPayload(
                url = "https://example.org",
                websiteTitle = "Example",
                displayText = "Example",
            ),
        )

        val item = json.parseToJsonElement(json.encodeToString(request)).jsonObject["item"]!!.jsonObject

        assertEquals("website", item["type"]?.jsonPrimitive?.content)
        assertEquals("https://example.org", item["url"]?.jsonPrimitive?.content)
        assertEquals("Example", item["websiteTitle"]?.jsonPrimitive?.content)
        assertEquals("Example", item["displayText"]?.jsonPrimitive?.content)
    }

    @Test
    fun `id defaults to blank so the server assigns one`() {
        assertEquals("", WebsiteItemPayload(url = "https://example.org").id)
    }

    @Test
    fun `type defaults to website, and reaches the desktop even though it is a default`() {
        assertEquals("website", WebsiteItemPayload(url = "https://example.org").type)

        // Without encodeDefaults the field is dropped and the desktop sees no type at all,
        // which is why WebService configures its Json that way.
        val encoded = json.encodeToString(WebsiteRequest(WebsiteItemPayload(url = "https://example.org")))
        val item = json.parseToJsonElement(encoded).jsonObject["item"]!!.jsonObject
        assertEquals("website", item["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `only the url is required`() {
        val payload = json.decodeFromString<WebsiteItemPayload>("""{"url":"https://example.org"}""")

        assertEquals("https://example.org", payload.url)
        assertEquals("website", payload.type)
        assertEquals("", payload.websiteTitle)
        assertEquals("", payload.displayText)
    }

    @Test
    fun `a request round-trips`() {
        val request = WebsiteRequest(
            WebsiteItemPayload(id = "abc", url = "https://example.org", websiteTitle = "Example"),
        )

        assertEquals(request, json.decodeFromString<WebsiteRequest>(json.encodeToString(request)))
    }
}
