package com.church.presenter.churchpresentermobile.util

/**
 * True when the app is running as a debug build.
 * Used to suppress Remote Config overrides (e.g. demo mode) that should
 * never affect developers during local development.
 */
expect val isDebugBuild: Boolean

/**
 * The human-readable version name of this build, e.g. "1.0.5".
 * Sourced from versionName (Android), CFBundleShortVersionString (iOS),
 * or a fallback string on web targets.
 */
expect val appVersion: String

/**
 * The git origin this build was made from, as "owner/name", or "unknown".
 *
 * Sent with the live-map ping so forks and self-compiled builds are counted
 * separately from official releases — the app is open-source and hardcodes the
 * ping URL, so without this a fork's launches are indistinguishable from real
 * installs. Only the parsed slug is ever exposed, never the remote URL, which
 * can embed credentials.
 */
val repoSlug: String get() = BuildProvenance.REPO_SLUG

/** Short git commit hash this build was made from, or "unknown". */
val commitHash: String get() = BuildProvenance.COMMIT_HASH

/**
 * Build kind for the live-map ping: "release", "snapshot", "dirty" or "nogit".
 *
 * Combines the build-time git facts with the per-target [isDebugBuild], which is
 * a far more reliable release signal across three toolchains than guessing from
 * Gradle task names.
 *
 * `release` deliberately outranks `dirty`, matching the desktop app: a store
 * build is a real user's build whatever the working tree looked like, and
 * counting real users as developers is the costlier mistake.
 */
val buildType: String
    get() = when {
        !BuildProvenance.HAS_GIT -> "nogit" // source archive, git unavailable at build time
        !isDebugBuild -> "release"
        BuildProvenance.IS_DIRTY -> "dirty"
        else -> "snapshot"
    }
