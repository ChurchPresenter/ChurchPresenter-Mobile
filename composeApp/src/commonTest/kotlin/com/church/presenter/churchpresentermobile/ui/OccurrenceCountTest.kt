package com.church.presenter.churchpresentermobile.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * How an occurrence count is written out.
 *
 * Every number on the dictionary screen goes through this — the count on a row,
 * the count in the entry sheet, and both halves of "25 of 2,606". The counts
 * are large enough that grouping is what makes them readable at a glance, and a
 * comma in the wrong place misreads by a factor of ten.
 */
class OccurrenceCountTest {

    @Test
    fun aSingleDigitIsLeftAlone() {
        assertEquals("7", 7.grouped())
    }

    @Test
    fun twoDigitsAreLeftAlone() {
        assertEquals("54", 54.grouped())
    }

    @Test
    fun threeDigitsAreLeftAlone() {
        assertEquals("116", 116.grouped())
    }

    @Test
    fun fourDigitsAreGroupedOnce() {
        assertEquals("2,606", 2606.grouped())
    }

    @Test
    fun theSmallestGroupedNumberIsAThousand() {
        assertEquals("1,000", 1000.grouped())
    }

    @Test
    fun theLargestUngroupedNumberIsNineHundredAndNinetyNine() {
        assertEquals("999", 999.grouped())
    }

    @Test
    fun fiveDigitsAreGroupedOnce() {
        assertEquals("38,000", 38000.grouped())
    }

    @Test
    fun sixDigitsAreGroupedOnce() {
        assertEquals("310,000", 310000.grouped())
    }

    @Test
    fun sevenDigitsAreGroupedTwice() {
        assertEquals("1,234,567", 1234567.grouped())
    }

    @Test
    fun zerosInsideANumberAreKept() {
        assertEquals("1,000,001", 1000001.grouped())
    }

    @Test
    fun zeroIsWrittenAsZero() {
        assertEquals("0", 0.grouped())
    }

    @Test
    fun theMinusSignIsNotMistakenForADigit() {
        // Not a count the desktop should ever send, but the grouping walks the
        // string it is given; a sign shifted into a group would read as a
        // different number entirely.
        assertEquals("-1,234", (-1234).grouped())
    }
}
