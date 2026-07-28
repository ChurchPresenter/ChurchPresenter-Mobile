package com.church.presenter.churchpresentermobile.network

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [PingReporter] fires an anonymous city-level ping on launch. The launch itself
 * is network bound, but assembling the url is pure: the platform/os/version
 * tags, the dev-build and connected splits, and the build-provenance params that
 * let the server count forks and self-compiled builds separately from official
 * releases (see api/ping.ts in the website repo).
 */
class PingReporterTest {

    @Test
    fun `the ping url carries platform, os and version`() {
        val url = PingReporter.buildPingUrl(
            os = "android", version = "1.0.14", isDevBuild = false, connected = false,
        )
        assertTrue(url.startsWith("https://www.churchpresenter.org/api/ping?"), url)
        assertTrue("platform=mobile" in url, url)
        assertTrue("os=android" in url, url)
        assertTrue("version=1.0.14" in url, url)
    }

    @Test
    fun `a debug build is tagged with the dev source, a store build is not`() {
        val dev = PingReporter.buildPingUrl("ios", "1.0.14", isDevBuild = true, connected = false)
        assertTrue("src=dev" in dev, dev)

        val store = PingReporter.buildPingUrl("ios", "1.0.14", isDevBuild = false, connected = false)
        assertFalse("src=dev" in store, store)
    }

    @Test
    fun `the connected flag is only sent on the paired ping`() {
        val open = PingReporter.buildPingUrl("android", "1.0.14", isDevBuild = false, connected = false)
        assertFalse("connected=true" in open, open)

        val paired = PingReporter.buildPingUrl("android", "1.0.14", isDevBuild = false, connected = true)
        assertTrue("connected=true" in paired, paired)
    }

    @Test
    fun `build provenance is included when known and omitted when not`() {
        val known = PingReporter.buildPingUrl(
            "android", "1.0.14", isDevBuild = false, connected = false,
            repoSlug = "churchpresenter/churchpresenter-mobile",
            commit = "a1b2c3d4e5f6",
            buildType = "release",
        )
        assertTrue("repo=churchpresenter/churchpresenter-mobile" in known, known)
        assertTrue("commit=a1b2c3d4e5f6" in known, known)
        assertTrue("build=release" in known, known)

        // A source archive has no git at all — the server treats the missing
        // params the same as unrecognised ones and still counts the ping.
        val unknown = PingReporter.buildPingUrl(
            "android", "1.0.14", isDevBuild = false, connected = false,
            repoSlug = "unknown", commit = "unknown", buildType = "nogit",
        )
        assertFalse("repo=" in unknown, unknown)
        assertFalse("commit=" in unknown, unknown)
        assertTrue("build=nogit" in unknown, unknown)
    }

    @Test
    fun `a fork build reports its own repo slug`() {
        val fork = PingReporter.buildPingUrl(
            "ios", "1.0.14", isDevBuild = false, connected = false,
            repoSlug = "someone/cp-mobile-fork", commit = "deadbeef", buildType = "release",
        )
        assertTrue("repo=someone/cp-mobile-fork" in fork, fork)
    }
}
