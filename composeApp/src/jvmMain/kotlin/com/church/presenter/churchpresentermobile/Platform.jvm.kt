package com.church.presenter.churchpresentermobile

/**
 * The JVM target exists to run the Compose UI tests on a JaCoCo-visible JVM
 * (see the "Where each kind of test runs" table in AGENT.md). It ships no
 * app, so most of these actuals are the same deliberate stubs the web target
 * carries — present so the shared code compiles, never reached by a user.
 */
class JvmPlatform : Platform {
    override val name: String = "JVM test host"
    override val os: String = "jvm"
}

actual fun getPlatform(): Platform = JvmPlatform()

actual fun generateUUID(): String = java.util.UUID.randomUUID().toString()

actual fun deviceName(): String = ""

actual fun openUrl(url: String) = Unit
