package com.church.presenter.churchpresentermobile.network

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
}
