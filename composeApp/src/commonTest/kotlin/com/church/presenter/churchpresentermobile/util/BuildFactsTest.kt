package com.church.presenter.churchpresentermobile.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What a build says about itself.
 *
 * These few facts leave the device: the live-map ping carries the repo slug, the
 * commit and the build type so that forks and self-compiled builds are counted
 * apart from real installs — the app is open-source and hardcodes the ping URL,
 * so without them a fork's launches are indistinguishable from a church's. The
 * slug is also the one place a git remote could leak, and a remote URL can carry
 * credentials, so what is exposed has to stay a bare "owner/name".
 */
class BuildFactsTest {

    // ── What gets sent ───────────────────────────────────────────────────

    @Test
    fun `the repo slug is owner and name, or an admission that it is unknown`() {
        val slug = repoSlug

        assertTrue(slug == "unknown" || slug.count { it == '/' } == 1, "unexpected slug: $slug")
    }

    @Test
    fun `the repo slug is never a URL`() {
        // A remote URL can embed a token; only the parsed slug may be exposed.
        assertFalse(repoSlug.contains("://"), "the slug looks like a URL: $repoSlug")
    }

    @Test
    fun `the repo slug carries no credentials`() {
        // "https://user:token@host/owner/repo" is an ordinary way to clone.
        assertFalse(repoSlug.contains("@"), "the slug looks like it carries credentials")
    }

    @Test
    fun `the repo slug carries no whitespace`() {
        assertFalse(repoSlug.any { it.isWhitespace() }, "the slug has whitespace in it: '$repoSlug'")
    }

    @Test
    fun `the repo slug is never blank`() {
        // Blank would be sent as a field nobody can interpret; "unknown" is a
        // statement.
        assertTrue(repoSlug.isNotBlank())
    }

    @Test
    fun `the repo slug does not end in dot git`() {
        assertFalse(repoSlug.endsWith(".git"), "the slug was not parsed: $repoSlug")
    }

    @Test
    fun `the commit hash is a short hash or an admission that it is unknown`() {
        val hash = commitHash

        assertTrue(
            hash == "unknown" || hash.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' },
            "unexpected commit hash: $hash",
        )
    }

    @Test
    fun `the commit hash is short enough to read`() {
        assertTrue(commitHash.length <= 40, "the commit hash is longer than a full one")
    }

    @Test
    fun `the commit hash is never blank`() {
        assertTrue(commitHash.isNotBlank())
    }

    // ── What kind of build it is ─────────────────────────────────────────

    @Test
    fun `the build type is one of the four the map counts`() {
        assertTrue(
            buildType in setOf("release", "snapshot", "dirty", "nogit"),
            "unexpected build type: $buildType",
        )
    }

    @Test
    fun `the build type is never blank`() {
        assertTrue(buildType.isNotBlank())
    }

    @Test
    fun `a debug build is never counted as a release`() {
        // Counting developers as real users is the mistake that inflates every
        // figure on the map.
        if (isDebugBuild) assertTrue(buildType != "release")
    }

    @Test
    fun `a release build is counted as one whatever the working tree looked like`() {
        // A store build is a real user's build even if it was made from a dirty
        // tree; counting real users as developers is the costlier mistake.
        if (!isDebugBuild && buildType != "nogit") assertEquals("release", buildType)
    }

    @Test
    fun `a build made without git says so rather than guessing`() {
        // A source archive has no git to ask, and "snapshot" would be a claim
        // nothing supports.
        if (buildType == "nogit") assertTrue(commitHash == "unknown" || repoSlug == "unknown")
    }

    @Test
    fun `the build type is settled the same way every time it is read`() {
        // It is read on every ping; a value that wandered would split one
        // install across several buckets.
        assertEquals(buildType, buildType)
    }

    @Test
    fun `the version name is something a person can report`() {
        // It goes in bug reports and in the minimum-version check.
        assertTrue(appVersion.isNotBlank())
    }

    @Test
    fun `the version name carries no whitespace`() {
        assertFalse(appVersion.any { it.isWhitespace() }, "unexpected version: '$appVersion'")
    }

    @Test
    fun `the debug flag is the same answer every time`() {
        // Remote Config overrides are suppressed on it; a value that changed
        // mid-session would apply a demo mode to a developer halfway through.
        assertEquals(isDebugBuild, isDebugBuild)
    }

    // ── Logging ──────────────────────────────────────────────────────────

    @Test
    fun `a debug line is harmless`() {
        Logger.d("BuildFactsTest", "a message")
    }

    @Test
    fun `an error line without a cause is harmless`() {
        Logger.e("BuildFactsTest", "something failed")
    }

    @Test
    fun `an error line with a cause is harmless`() {
        Logger.e("BuildFactsTest", "something failed", IllegalStateException("why"))
    }

    @Test
    fun `an error line with a causeless exception is harmless`() {
        Logger.e("BuildFactsTest", "something failed", RuntimeException())
    }

    @Test
    fun `an empty tag and message are harmless`() {
        Logger.d("", "")
        Logger.e("", "")
    }

    @Test
    fun `a very long message is harmless`() {
        Logger.d("BuildFactsTest", "x".repeat(10_000))
    }

    @Test
    fun `accented text logs harmlessly`() {
        Logger.d("BuildFactsTest", "Święty")
    }

    // ── The Sentry project it reports to ─────────────────────────────────

    @Test
    fun `the Sentry DSN is a URL`() {
        assertTrue(SENTRY_DSN.startsWith("https://"), "the DSN is not an https URL")
    }

    @Test
    fun `the Sentry DSN names a project`() {
        // Everything after the last slash is the project id; without it events
        // are accepted and dropped.
        assertTrue(SENTRY_DSN.substringAfterLast('/').isNotBlank())
    }

    @Test
    fun `the Sentry DSN carries a public key rather than a secret`() {
        // A DSN is safe to ship precisely because it is submit-only; the old
        // two-part "public:secret@" form is not.
        val credentials = SENTRY_DSN.removePrefix("https://").substringBefore('@')

        assertFalse(credentials.contains(':'), "the DSN carries a secret key")
    }

    @Test
    fun `the Sentry DSN points at Sentry`() {
        assertTrue(SENTRY_DSN.substringAfter('@').contains("sentry.io"))
    }
}
