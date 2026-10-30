package com.church.presenter.churchpresentermobile.tv

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The TV's list of phones as they advertise and disappear. A phone that moved
 * port must not be listed twice — the TV would offer a dead entry beside the
 * live one — and one that left must drop off.
 */
class PhoneListTest {

    private val pixel = PhoneSource("ChurchPresenter · Pixel", "192.168.1.20", 8766)

    @Test
    fun `a new phone is added`() {
        assertEquals(listOf(pixel), emptyList<PhoneSource>().withFound(pixel))
    }

    @Test
    fun `a phone found again at a new port replaces its old entry`() {
        val moved = pixel.copy(port = 8767)

        assertEquals(listOf(moved), listOf(pixel).withFound(moved))
    }

    @Test
    fun `a phone that stops advertising drops off`() {
        val other = PhoneSource("ChurchPresenter · Galaxy", "192.168.1.21", 8766)

        assertEquals(listOf(other), listOf(pixel, other).withoutLost(pixel.name))
    }
}
