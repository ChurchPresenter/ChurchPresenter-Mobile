package com.church.presenter.churchpresentermobile.present

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers what [localIpAddress] is allowed to return.
 *
 * This is the number the operator reads off the phone and types into a TV, so
 * the shape of it is the whole contract: an IPv4 address another device on the
 * hall's network can reach, or nothing at all. A loopback or IPv6 address here
 * would be typed in and silently fail to connect, with no way for the operator
 * to tell why.
 *
 * The implementation walks `java.net.NetworkInterface`, which is real on the
 * unit-test JVM, so this runs against the machine's actual interfaces. It
 * therefore asserts the contract rather than a particular address — a build
 * agent with no network is a legitimate "no address" case.
 */
class NetworkInfoAndroidTest {

    private val ipv4 = Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")

    @Test
    fun `the answer is either an IPv4 address or nothing`() {
        val address = localIpAddress()

        assertTrue(address == null || ipv4.matches(address), "not an IPv4 address: $address")
    }

    @Test
    fun `loopback is never offered`() {
        // 127.0.0.1 is reachable only from the phone itself; a TV typing it in
        // would reach its own web server, or nothing.
        val address = localIpAddress()

        assertTrue(address == null || !address.startsWith("127."), "loopback offered: $address")
    }

    @Test
    fun `an IPv6 address is never offered`() {
        // The display page's URL is built by string concatenation, so an IPv6
        // address would need brackets it never gets.
        val address = localIpAddress()

        assertTrue(address == null || !address.contains(':'), "IPv6 offered: $address")
    }

    @Test
    fun `every octet is in range`() {
        val address = localIpAddress() ?: return

        assertTrue(address.split('.').all { it.toInt() in 0..255 }, "bad octet in $address")
    }

    @Test
    fun `asking twice gives the same answer`() {
        // Nothing is cached, so this is only true if the choice between several
        // interfaces is deterministic — otherwise the URL on the phone and the
        // one the server bound to could disagree.
        assertEquals(localIpAddress(), localIpAddress())
    }

    @Test
    fun `the address is never blank`() {
        // The implementation drops empty host strings rather than passing them on;
        // an empty one would build the URL "http://:8080".
        val address = localIpAddress()

        assertTrue(address == null || address.isNotBlank())
    }
}
