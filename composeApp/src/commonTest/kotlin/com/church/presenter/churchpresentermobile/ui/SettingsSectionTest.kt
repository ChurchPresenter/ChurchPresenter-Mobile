package com.church.presenter.churchpresentermobile.ui

import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.settings_connection_checking
import churchpresentermobile.composeapp.generated.resources.settings_connection_connected
import churchpresentermobile.composeapp.generated.resources.settings_connection_limited
import churchpresentermobile.composeapp.generated.resources.settings_connection_rejected
import churchpresentermobile.composeapp.generated.resources.settings_connection_unreachable
import churchpresentermobile.composeapp.generated.resources.settings_connection_wrong_server
import com.church.presenter.churchpresentermobile.model.DevicePermissions
import com.church.presenter.churchpresentermobile.model.ServerStatus
import com.church.presenter.churchpresentermobile.model.StatusWarning
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The settings menu's contents, and the one-word connection summary at its
 * foot — both plain functions over plain values, so they are pinned down
 * without a Skia surface (AGENT.md, "reach for a seam").
 */
class SettingsSectionTest {

    // ── Which sections exist ─────────────────────────────────────────────

    @Test
    fun remoteModeListsTheServerAndTheDevice() {
        val sections = settingsSections(hasDesktop = true, supportsStandalone = true)

        assertEquals(
            listOf(
                SettingsSection.MODE,
                SettingsSection.SERVER,
                SettingsSection.DEVICE,
                SettingsSection.APPEARANCE,
                SettingsSection.DIAGNOSTICS,
                SettingsSection.ABOUT,
            ),
            sections,
        )
    }

    @Test
    fun standaloneModeListsTheComputerAndNoDevicePage() {
        // The device names only mean something to a desktop that asks for
        // them; standalone has no desktop, only a computer to copy from.
        val sections = settingsSections(hasDesktop = false, supportsStandalone = true)

        assertEquals(
            listOf(
                SettingsSection.MODE,
                SettingsSection.COMPUTER,
                SettingsSection.APPEARANCE,
                SettingsSection.DIAGNOSTICS,
                SettingsSection.ABOUT,
            ),
            sections,
        )
    }

    @Test
    fun aBuildThatCannotPresentHasNoModePage() {
        // The web build has no output sink, so the form never draws the mode
        // switch — and the menu must not offer a page the sheet lacks.
        val sections = settingsSections(hasDesktop = true, supportsStandalone = false)

        assertEquals(SettingsSection.SERVER, sections.first())
    }

    @Test
    fun aboutIsAlwaysLast() {
        listOf(true, false).forEach { desktop ->
            listOf(true, false).forEach { standalone ->
                assertEquals(
                    SettingsSection.ABOUT,
                    settingsSections(hasDesktop = desktop, supportsStandalone = standalone).last(),
                )
            }
        }
    }

    // ── The connection in one word ───────────────────────────────────────

    private val permitted = DevicePermissions(canPresent = true, canAddToSchedule = true, canUploadFiles = true)

    @Test
    fun aDesktopWithNoWarningsIsConnected() {
        val summary = connectionSummary(StatusUiState.Success(ServerStatus(permissions = permitted), emptyList()))

        assertEquals(Res.string.settings_connection_connected, summary.label)
        assertEquals(ConnectionTone.GOOD, summary.tone)
    }

    @Test
    fun aDesktopWithWarningsIsLimited() {
        val summary = connectionSummary(
            StatusUiState.Success(ServerStatus(permissions = permitted), listOf(StatusWarning.NoBibles)),
        )

        assertEquals(Res.string.settings_connection_limited, summary.label)
        assertEquals(ConnectionTone.WARNING, summary.tone)
    }

    @Test
    fun aFailedRequestIsUnreachable() {
        val summary = connectionSummary(StatusUiState.Error("Connection refused"))

        assertEquals(Res.string.settings_connection_unreachable, summary.label)
        assertEquals(ConnectionTone.BAD, summary.tone)
    }

    @Test
    fun aRefusedKeyIsRejected() {
        val summary = connectionSummary(StatusUiState.Unauthorized(401))

        assertEquals(Res.string.settings_connection_rejected, summary.label)
        assertEquals(ConnectionTone.BAD, summary.tone)
    }

    @Test
    fun aStrangerAnsweringIsNamedAsSuch() {
        val summary = connectionSummary(StatusUiState.NotChurchPresenter("hello from nginx"))

        assertEquals(Res.string.settings_connection_wrong_server, summary.label)
        assertEquals(ConnectionTone.BAD, summary.tone)
    }

    @Test
    fun aRequestInFlightIsChecking() {
        val summary = connectionSummary(StatusUiState.Loading)

        assertEquals(Res.string.settings_connection_checking, summary.label)
        assertEquals(ConnectionTone.PENDING, summary.tone)
    }
}
