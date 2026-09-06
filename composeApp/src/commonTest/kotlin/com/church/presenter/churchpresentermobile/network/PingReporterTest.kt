package com.church.presenter.churchpresentermobile.network

import kotlin.test.Test
import kotlin.test.assertEquals
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

    // ── What the live map is told ────────────────────────────────────────
    //
    // The app is open-source and hardcodes this URL, so a fork built from source
    // would otherwise ping with a payload identical to an official install. The
    // provenance parameters are what let the server tell them apart — and each is
    // omitted rather than sent as "unknown", so a source archive with no git is
    // simply quiet rather than noisy.

    private fun url(
        os: String = "android",
        version: String = "1.0.18",
        isDevBuild: Boolean = false,
        connected: Boolean = false,
        repoSlug: String = "churchpresenter/churchpresenter-mobile",
        commit: String = "abc123def456",
        buildType: String = "release",
    ) = PingReporter.buildPingUrl(os, version, isDevBuild, connected, repoSlug, commit, buildType)

    @Test
    fun everyLaunchReportsItsPlatformOsAndVersion() {
        val u = url()

        assertTrue(u.contains("platform=mobile"), u)
        assertTrue(u.contains("os=android"), u)
        assertTrue(u.contains("version=1.0.18"), u)
    }

    @Test
    fun aReleaseBuildIsNotMarkedAsDevelopment() {
        assertFalse(url(isDevBuild = false).contains("src=dev"))
    }

    @Test
    fun aDeveloperBuildIsMarked() {
        // So a developer's own launches do not count as installs.
        assertTrue(url(isDevBuild = true).contains("src=dev"))
    }

    @Test
    fun theConnectedFlagIsSentOnlyOnceADesktopIsFound() {
        assertFalse(url(connected = false).contains("connected=true"))
        assertTrue(url(connected = true).contains("connected=true"))
    }

    @Test
    fun theRepoIsReportedSoForksAreCountedSeparately() {
        assertTrue(url(repoSlug = "someone/their-fork").contains("repo=someone/their-fork"))
    }

    @Test
    fun anUnknownRepoIsOmittedRatherThanSent() {
        // A source archive with no git remote; "unknown" would be noise.
        assertFalse(url(repoSlug = "unknown").contains("repo="))
        assertFalse(url(repoSlug = "").contains("repo="))
    }

    @Test
    fun theCommitIsReportedWhenItIsKnown() {
        assertTrue(url(commit = "abc123def456").contains("commit=abc123def456"))
    }

    @Test
    fun anUnknownCommitIsOmitted() {
        assertFalse(url(commit = "unknown").contains("commit="))
        assertFalse(url(commit = "").contains("commit="))
    }

    @Test
    fun theBuildTypeIsReportedWhenItIsKnown() {
        for (type in listOf("release", "snapshot", "dirty", "nogit")) {
            assertTrue(url(buildType = type).contains("build=$type"), type)
        }
    }

    @Test
    fun anUnknownBuildTypeIsOmitted() {
        assertFalse(url(buildType = "unknown").contains("build="))
        assertFalse(url(buildType = "").contains("build="))
    }

    @Test
    fun aBuildWithNoProvenanceAtAllStillPings() {
        // The launch still counts; it just carries nothing identifying.
        val u = url(repoSlug = "unknown", commit = "unknown", buildType = "unknown")

        assertTrue(u.contains("platform=mobile"), u)
        assertFalse(u.contains("repo="), u)
        assertFalse(u.contains("commit="), u)
        assertFalse(u.contains("build="), u)
    }

    @Test
    fun theUrlIsWellFormedWithOneQueryStartAndNoStrayAmpersands() {
        val u = url()

        assertEquals(1, u.count { it == '?' }, u)
        assertFalse(u.contains("&&"), u)
        assertFalse(u.endsWith("&"), u)
    }

    @Test
    fun everyOptionalParameterCanBePresentAtOnce() {
        val u = url(isDevBuild = true, connected = true)

        for (part in listOf("src=dev", "connected=true", "repo=", "commit=", "build=")) {
            assertTrue(u.contains(part), "$part missing from $u")
        }
    }
}
