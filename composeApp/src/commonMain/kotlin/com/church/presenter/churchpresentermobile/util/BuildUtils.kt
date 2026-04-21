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
