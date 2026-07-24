package com.church.presenter.churchpresentermobile.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [isExpectedConnectivityError] (crash-report noise suppression) and
 * [toFriendlyNetworkMessage] (UI copy). Several cases reproduce the exact
 * exception shapes seen in production Sentry reports for an unreachable server.
 */
class NetworkErrorMapperTest {

    // Exceptions whose simpleName matches an entry in EXPECTED_CONNECTIVITY_EXCEPTIONS.
    private class ConnectException(message: String) : Exception(message)
    private class SocketTimeoutException(message: String) : Exception(message)

    // ── isExpectedConnectivityError ──────────────────────────────────────────

    @Test
    fun connectTimeoutMessageIsExpected() {
        assertTrue(Exception("Connect timeout has expired [url=http://x/api/status]").isExpectedConnectivityError())
    }

    @Test
    fun okhttpFailedToConnectMessageIsExpected() {
        // Exact shape from Sentry — note it contains no "timeout"/"timed out" substring.
        val msg = "failed to connect to /192.168.1.100 (port 8765) from /10.0.2.16 (port 43636) after 10000ms"
        assertTrue(Exception(msg).isExpectedConnectivityError())
    }

    @Test
    fun etimedoutInCauseIsExpected() {
        // Top exception carries no marker; the connectivity signal is only on the cause
        // (mirrors the SocketTimeoutException -> ErrnoException(ETIMEDOUT) chain from Sentry).
        val cause = Exception("isConnected failed: ETIMEDOUT (Connection timed out)")
        val top = Exception("some generic wrapper without a marker", cause)
        assertTrue(top.isExpectedConnectivityError())
    }

    @Test
    fun connectionRefusedIsExpected() {
        assertTrue(Exception("Connection refused").isExpectedConnectivityError())
        assertTrue(Exception("ECONNREFUSED (Connection refused)").isExpectedConnectivityError())
    }

    @Test
    fun unresolvedHostIsExpected() {
        assertTrue(Exception("Unable to resolve host \"nope.local\"").isExpectedConnectivityError())
    }

    @Test
    fun iosNsurlNotConnectedIsExpected() {
        assertTrue(Exception("Error Domain=NSURLErrorDomain Code=-1009 not connected").isExpectedConnectivityError())
    }

    @Test
    fun wsUpgradeRejectedIsExpected() {
        assertTrue(Exception("Expected HTTP 101 response but was '504 Gateway Timeout'").isExpectedConnectivityError())
    }

    @Test
    fun classNameMatchIsExpected() {
        // Message has no marker; classification must come from the exception's simpleName.
        assertTrue(ConnectException("boom").isExpectedConnectivityError())
        assertTrue(SocketTimeoutException("boom").isExpectedConnectivityError())
    }

    @Test
    fun classNameMatchInCauseIsExpected() {
        val top = Exception("nothing useful here", ConnectException("boom"))
        assertTrue(top.isExpectedConnectivityError())
    }

    @Test
    fun genuineBugIsNotExpected() {
        assertFalse(IllegalStateException("index 5 out of bounds for length 3").isExpectedConnectivityError())
        assertFalse(Exception("Unexpected server payload shape").isExpectedConnectivityError())
    }

    @Test
    fun nullMessageNoCauseIsNotExpected() {
        assertFalse(Exception().isExpectedConnectivityError())
    }

    @Test
    fun deepCauseChainWithoutMarkersTerminates() {
        // A chain longer than the walk depth with no markers stays false and doesn't hang.
        var e: Throwable = Exception("leaf")
        repeat(20) { i -> e = Exception("wrap$i", e) }
        assertFalse(e.isExpectedConnectivityError())
    }

    // ── toFriendlyNetworkMessage ─────────────────────────────────────────────

    @Test
    fun friendlyAndroidConnectionRefused() {
        assertEquals(
            "Server not reachable. Check the IP address and port.",
            Exception("Connection refused").toFriendlyNetworkMessage(),
        )
    }

    @Test
    fun friendlyAndroidUnresolvedHost() {
        assertEquals(
            "Invalid server address. Check the IP address.",
            Exception("Unable to resolve host").toFriendlyNetworkMessage(),
        )
    }

    @Test
    fun friendlyAndroidTimeout() {
        assertEquals(
            "Connection timed out. Make sure the server is running.",
            Exception("SocketTimeoutException: timeout").toFriendlyNetworkMessage(),
        )
    }

    @Test
    fun friendlyAndroidSslError() {
        assertEquals(
            "SSL error: could not establish a secure connection. Check server settings.",
            Exception("Handshake failed").toFriendlyNetworkMessage(),
        )
    }

    @Test
    fun friendlyIosTimeout() {
        val msg = "Exception in http request: Error Domain=NSURLErrorDomain Code=-1001 \"timed out\""
        assertEquals(
            "Connection timed out. Make sure the server is running.",
            Exception(msg).toFriendlyNetworkMessage(),
        )
    }

    @Test
    fun friendlyIosCouldNotConnect() {
        val msg = "Exception in http request: Error Domain=NSURLErrorDomain Code=-1004 \"Could not connect to the server\""
        assertEquals(
            "Server not reachable. Check the IP address and port.",
            Exception(msg).toFriendlyNetworkMessage(),
        )
    }

    @Test
    fun friendlyFallbackReturnsShortMessage() {
        assertEquals("boom", Exception("boom").toFriendlyNetworkMessage())
    }

    @Test
    fun friendlyNullMessage() {
        assertEquals("Connection error", Exception().toFriendlyNetworkMessage())
    }
}
