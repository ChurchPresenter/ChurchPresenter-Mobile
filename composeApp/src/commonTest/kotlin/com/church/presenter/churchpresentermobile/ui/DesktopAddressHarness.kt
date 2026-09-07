package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage

/** Setup for the shared desktop-address fields. */
internal fun settings() = AppSettings(InMemorySettingsStorage())

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showFields(
    settings: AppSettings = settings(),
    showHint: Boolean = true,
) = showScreen { DesktopAddressFields(settings = settings, showHint = showHint) }
