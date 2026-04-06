package com.church.presenter.churchpresentermobile.util

/**
 * True when the app is running as a debug build.
 * Used to suppress Remote Config overrides (e.g. demo mode) that should
 * never affect developers during local development.
 */
expect val isDebugBuild: Boolean

