package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests the H/G prefix + numeric-value logic on [StrongsEntry]. */
class StrongsEntryTest {

    private fun entry(number: String) = StrongsEntry(number = number, word = "w")

    @Test
    fun hebrewIsDetectedByHPrefix() {
        val e = entry("H430")
        assertTrue(e.isHebrew)
        assertFalse(e.isGreek)
        assertEquals(430, e.numericValue)
    }

    @Test
    fun greekIsDetectedByGPrefix() {
        val e = entry("G26")
        assertTrue(e.isGreek)
        assertFalse(e.isHebrew)
        assertEquals(26, e.numericValue)
    }

    @Test
    fun unknownPrefixIsNeitherButStillParsesNumber() {
        val e = entry("X1")
        assertFalse(e.isHebrew)
        assertFalse(e.isGreek)
        assertEquals(1, e.numericValue)
    }

    @Test
    fun malformedNumberYieldsZero() {
        assertEquals(0, entry("H").numericValue)
        assertEquals(0, entry("Habc").numericValue)
    }
}
