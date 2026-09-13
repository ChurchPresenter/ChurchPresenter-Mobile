package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The colour wheel's coordinates for a colour — each sector of the hue
 * arithmetic, and the two colours that have no hue at all.
 */
class RgbToHsvTest {

    private fun assertHsv(c: Color, h: Float, s: Float, v: Float) {
        val (hue, sat, value) = rgbToHsv(c)
        assertTrue(abs(hue - h) < 0.5f, "hue of $c: expected $h, was $hue")
        assertTrue(abs(sat - s) < 0.01f, "saturation of $c: expected $s, was $sat")
        assertTrue(abs(value - v) < 0.01f, "value of $c: expected $v, was $value")
    }

    @Test
    fun redSitsAtTheTopOfTheWheel() = assertHsv(Color.Red, h = 0f, s = 1f, v = 1f)

    @Test
    fun greenIsAThirdOfTheWayRound() = assertHsv(Color.Green, h = 120f, s = 1f, v = 1f)

    @Test
    fun blueIsTwoThirdsRound() = assertHsv(Color.Blue, h = 240f, s = 1f, v = 1f)

    @Test
    fun yellowSitsBetweenRedAndGreen() = assertHsv(Color(1f, 1f, 0f), h = 60f, s = 1f, v = 1f)

    @Test
    fun cyanSitsBetweenGreenAndBlue() = assertHsv(Color(0f, 1f, 1f), h = 180f, s = 1f, v = 1f)

    @Test
    fun magentaWrapsBelowZeroBackToTheTop() {
        // Red is the maximum and blue exceeds green, so the raw hue is negative
        // and has to come round the wheel rather than stay below zero.
        assertHsv(Color(1f, 0f, 1f), h = 300f, s = 1f, v = 1f)
    }

    @Test
    fun greyHasNoHueAndNoSaturation() = assertHsv(Color(0.5f, 0.5f, 0.5f), h = 0f, s = 0f, v = 0.5f)

    @Test
    fun blackHasNothingToDivideBy() {
        // Value is zero, so saturation cannot be d / max; it is defined as zero.
        assertHsv(Color.Black, h = 0f, s = 0f, v = 0f)
    }

    @Test
    fun aPaleColourKeepsItsHueAtLowSaturation() = assertHsv(Color(1f, 0.75f, 0.75f), h = 0f, s = 0.25f, v = 1f)

    @Test
    fun theResultIsAlwaysThreeNumbers() {
        assertEquals(3, rgbToHsv(Color.White).size)
    }
}
