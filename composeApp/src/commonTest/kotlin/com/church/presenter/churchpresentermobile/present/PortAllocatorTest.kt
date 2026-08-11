package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.network.ApiConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PortAllocatorTest {

    private val candidates = ApiConstants.STANDALONE_PORT_CANDIDATES.toList()

    @Test
    fun `the preferred port wins when it is free`() {
        val chosen = PortAllocator.choose(8766, candidates) { true }
        assertEquals(8766, chosen)
    }

    @Test
    fun `a busy preferred port falls through to the next candidate`() {
        val chosen = PortAllocator.choose(8766, candidates) { port -> port != 8766 }
        assertEquals(8767, chosen)
    }

    @Test
    fun `the preferred port is never probed twice`() {
        val probed = mutableListOf<Int>()
        PortAllocator.choose(8768, candidates) { port -> probed += port; false }

        assertEquals(probed.distinct(), probed, "a port was probed more than once")
        assertEquals(8768, probed.first(), "the preferred port must be tried first")
    }

    @Test
    fun `a preferred port outside the candidate range is still tried first`() {
        val probed = mutableListOf<Int>()
        PortAllocator.choose(9000, candidates) { port -> probed += port; port == 9000 }

        assertEquals(9000, probed.first())
    }

    /** A null result means "use an ephemeral port", not "give up". */
    @Test
    fun `everything busy yields null`() {
        assertNull(PortAllocator.choose(8766, candidates) { false })
    }

    @Test
    fun `a probe that throws is treated as busy rather than crashing the service`() {
        val chosen = PortAllocator.choose(8766, candidates) { port ->
            if (port == 8766) throw IllegalStateException("permission denied")
            port == 8767
        }
        assertEquals(8767, chosen)
    }

    @Test
    fun `privileged and out-of-range ports are never chosen`() {
        assertNull(PortAllocator.choose(80, listOf(0, 443, 1023, 70000)) { true })
    }

    @Test
    fun `every candidate is a usable unprivileged port`() {
        assertTrue(candidates.all { it in PortAllocator.VALID_PORTS })
    }
}
