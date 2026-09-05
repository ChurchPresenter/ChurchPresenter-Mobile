package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppModeHolderTest {

    @BeforeTest fun setUp() = AppModeHolder.resetForTest()

    @AfterTest fun tearDown() = AppModeHolder.resetForTest()

    private fun settings() = AppSettings(InMemorySettingsStorage())

    @Test
    fun `defaults to remote before init`() {
        assertEquals(AppMode.REMOTE, AppModeHolder.mode.value)
    }

    @Test
    fun `init seeds from persisted settings`() {
        val settings = settings()
        settings.appMode = AppMode.STANDALONE

        AppModeHolder.init(settings)

        assertEquals(settings.appMode, AppModeHolder.mode.value)
    }

    @Test
    fun `set persists and publishes`() {
        val settings = settings()

        AppModeHolder.set(settings, AppMode.STANDALONE)

        // AppSettings coerces on platforms without an output sink, and the
        // holder must agree with what was actually stored rather than with what
        // was requested.
        assertEquals(settings.appMode, AppModeHolder.mode.value)
        assertEquals(if (supportsStandalone) AppMode.STANDALONE else AppMode.REMOTE, AppModeHolder.mode.value)
    }

    @Test
    fun `switching back to remote is published`() {
        val settings = settings()
        AppModeHolder.set(settings, AppMode.STANDALONE)

        AppModeHolder.set(settings, AppMode.REMOTE)

        assertEquals(AppMode.REMOTE, AppModeHolder.mode.value)
        assertEquals(AppMode.REMOTE, settings.appMode)
    }
}
