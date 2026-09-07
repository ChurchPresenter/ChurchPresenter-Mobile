package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Which sites will not appear on the browser screen.
 *
 * The rule is the browser's, enforced on the site's behalf, so the app cannot
 * change the outcome — only find out first and say so while the operator is
 * still looking at the phone rather than at the wall.
 */
class FramingCheckTest {

    @Test
    fun denyRefuses() {
        assertTrue(FramingCheck.refusesFramingFrom("DENY", null))
    }

    @Test
    fun sameoriginRefuses() {
        // Google's answer, and the one that prompted this.
        assertTrue(FramingCheck.refusesFramingFrom("SAMEORIGIN", null))
    }

    @Test
    fun theHeaderIsReadWhateverCaseItArrivesIn() {
        assertTrue(FramingCheck.refusesFramingFrom("sameorigin", null))
        assertTrue(FramingCheck.refusesFramingFrom(" Deny ", null))
    }

    @Test
    fun noHeadersMeansNoWarning() {
        // Most sites say nothing at all, and they frame perfectly well.
        assertFalse(FramingCheck.refusesFramingFrom(null, null))
    }

    @Test
    fun frameAncestorsNoneRefuses() {
        assertTrue(FramingCheck.refusesFramingFrom(null, "default-src 'self'; frame-ancestors 'none'"))
    }

    @Test
    fun aListOfOriginsRefuses() {
        // This phone's address on a hall network will not be on anyone's list.
        assertTrue(FramingCheck.refusesFramingFrom(null, "frame-ancestors https://example.org"))
    }

    @Test
    fun aWildcardAllowsAnyone() {
        assertFalse(FramingCheck.refusesFramingFrom(null, "frame-ancestors *"))
    }

    @Test
    fun aPolicyWithoutFrameAncestorsSaysNothingAboutFraming() {
        // Warning on this would be a false alarm about a site that works.
        assertFalse(FramingCheck.refusesFramingFrom(null, "default-src 'self'; script-src 'self'"))
    }

    // ── The wrapper that actually asks the site ──────────────────────────

    private fun clientAnswering(vararg headers: Pair<String, String>) =
        mockClient {
            respond(
                content = "",
                headers = headersOf(*headers.map { (k, v) -> k to listOf(v) }.toTypedArray()),
            )
        }

    @Test
    fun refusesFramingReadsTheHeadersOffTheResponse() = runTest {
        val client = clientAnswering("X-Frame-Options" to "SAMEORIGIN")

        assertTrue(FramingCheck.refusesFraming("https://example.org", client))
    }

    @Test
    fun refusesFramingReadsTheContentSecurityPolicyToo() = runTest {
        val client = clientAnswering("Content-Security-Policy" to "frame-ancestors 'none'")

        assertTrue(FramingCheck.refusesFraming("https://example.org", client))
    }

    @Test
    fun aSiteThatSaysNothingIsAllowed() = runTest {
        assertFalse(FramingCheck.refusesFraming("https://example.org", clientAnswering()))
    }

    @Test
    fun anUnreachableSiteIsNotReportedAsRefusing() = runTest {
        // Being unable to ask is a different problem, and one the operator will
        // see for themselves the moment they project it — so it must not raise
        // the "this site won't display" warning.
        val client = mockClient { throw IllegalStateException("no route to host") }

        assertFalse(FramingCheck.refusesFraming("https://example.org", client))
    }
}
