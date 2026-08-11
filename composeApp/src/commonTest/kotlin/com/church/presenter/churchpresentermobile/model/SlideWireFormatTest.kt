package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the JSON the phone pushes to the bundled display page.
 *
 * `app.js` reads these exact field names and enum spellings. Nothing in the
 * Kotlin type system connects the two, so renaming a field or changing an enum
 * case would silently blank the audience screen — this is the test that stops it.
 */
class SlideWireFormatTest {

    // Must match WebPageSink's encoder: the page relies on defaults being present.
    private val json = Json { encodeDefaults = true }

    private fun encode(envelope: SlideEnvelope) =
        json.parseToJsonElement(json.encodeToString(envelope)).jsonObject

    @Test
    fun `the envelope carries the fields app_js reads`() {
        val encoded = encode(
            SlideEnvelope(rev = 7, slide = Slide(body = "text", reference = "John 3:16"))
        )

        assertTrue("v" in encoded, "protocol version gates the 'update this screen' message")
        assertTrue("type" in encoded)
        assertTrue("rev" in encoded, "revision is how the page drops out-of-order frames")
        assertTrue("slide" in encoded)
    }

    @Test
    fun `the slide carries the fields app_js reads`() {
        val encoded = encode(SlideEnvelope(slide = Slide(body = "text"))).slideObject()

        listOf(
            "body", "reference", "footer", "textSize", "backdrop",
            "backdropUrl", "isBlank", "isLive", "theme",
        ).forEach { field ->
            assertTrue(field in encoded, "app.js reads slide.$field")
        }
    }

    @Test
    fun `the theme carries the fields app_js reads`() {
        val theme = encode(SlideEnvelope(slide = Slide(body = "t")))
            .slideObject()["theme"]!!.jsonObject

        listOf("font", "textColor", "accentColor", "brandLine", "showClock").forEach { field ->
            assertTrue(field in theme, "app.js reads slide.theme.$field")
        }
    }

    /** app.js compares against these literal strings. */
    @Test
    fun `enums serialize as the spellings the page switches on`() {
        val encoded = encode(
            SlideEnvelope(
                type = SlideMessageType.SLIDE,
                slide = Slide(
                    body = "t",
                    textSize = SlideTextSize.LARGE,
                    backdrop = SlideBackdrop.BLACK,
                    theme = SlideTheme(font = SlideFont.SANS),
                ),
            )
        )
        val slide = encoded.slideObject()

        assertEquals("SLIDE", encoded["type"]!!.jsonPrimitive.content)
        assertEquals("LARGE", slide["textSize"]!!.jsonPrimitive.content)
        assertEquals("BLACK", slide["backdrop"]!!.jsonPrimitive.content)
        assertEquals("SANS", slide["theme"]!!.jsonObject["font"]!!.jsonPrimitive.content)
    }

    /** app.js builds a CSS class as `size-` + textSize.toLowerCase(). */
    @Test
    fun `every text size maps onto a css class the stylesheet defines`() {
        val defined = setOf("size-small", "size-medium", "size-large")
        SlideTextSize.entries.forEach { size ->
            assertTrue("size-${size.name.lowercase()}" in defined, "no CSS rule for $size")
        }
    }

    /** app.js switches on 'BLACK' and 'IMAGE', defaulting everything else to the gradient. */
    @Test
    fun `every backdrop is handled by the page`() {
        val handled = setOf("BLACK", "IMAGE", "GRADIENT")
        SlideBackdrop.entries.forEach { backdrop ->
            assertTrue(backdrop.name in handled, "app.js has no branch for $backdrop")
        }
    }

    @Test
    fun `message types the page special-cases still exist`() {
        val names = SlideMessageType.entries.map { it.name }
        listOf("SLIDE", "CLEAR", "PING", "BYE").forEach {
            assertTrue(it in names, "app.js branches on $it")
        }
    }

    @Test
    fun `the protocol version the page accepts matches the one we send`() {
        // app.js: var PROTOCOL_VERSION = 1;
        assertEquals(1, SLIDE_PROTOCOL_VERSION)
    }

    private fun kotlinx.serialization.json.JsonObject.slideObject() = this["slide"]!!.jsonObject
}
