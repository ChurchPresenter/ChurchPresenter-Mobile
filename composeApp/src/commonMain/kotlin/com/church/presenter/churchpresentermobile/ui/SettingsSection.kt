package com.church.presenter.churchpresentermobile.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.mode_section_title
import churchpresentermobile.composeapp.generated.resources.settings_about_section
import churchpresentermobile.composeapp.generated.resources.settings_appearance_section
import churchpresentermobile.composeapp.generated.resources.settings_computer_section
import churchpresentermobile.composeapp.generated.resources.settings_connection_checking
import churchpresentermobile.composeapp.generated.resources.settings_connection_connected
import churchpresentermobile.composeapp.generated.resources.settings_connection_limited
import churchpresentermobile.composeapp.generated.resources.settings_connection_rejected
import churchpresentermobile.composeapp.generated.resources.settings_connection_unreachable
import churchpresentermobile.composeapp.generated.resources.settings_connection_wrong_server
import churchpresentermobile.composeapp.generated.resources.settings_device_section
import churchpresentermobile.composeapp.generated.resources.settings_diagnostics_section
import churchpresentermobile.composeapp.generated.resources.settings_section_about_subtitle
import churchpresentermobile.composeapp.generated.resources.settings_section_appearance_subtitle
import churchpresentermobile.composeapp.generated.resources.settings_section_computer_subtitle
import churchpresentermobile.composeapp.generated.resources.settings_section_device_subtitle
import churchpresentermobile.composeapp.generated.resources.settings_section_diagnostics_subtitle
import churchpresentermobile.composeapp.generated.resources.settings_section_mode_subtitle
import churchpresentermobile.composeapp.generated.resources.settings_section_server_subtitle
import churchpresentermobile.composeapp.generated.resources.settings_server_section
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import org.jetbrains.compose.resources.StringResource

/**
 * The pages of the settings sheet, in the order the menu lists them.
 *
 * Each owns only its own controls — the design's rule, and the reason the
 * old single scroll became a menu: a volunteer sent to "fix the server
 * address" should land on the address, not on a theme toggle. The same entries
 * are the phone's menu and the tablet's list beside the form, so they are named
 * here, once, where both can agree on them.
 */
enum class SettingsSection(
    val title: StringResource,
    val subtitle: StringResource,
    val icon: ImageVector,
) {
    MODE(
        Res.string.mode_section_title,
        Res.string.settings_section_mode_subtitle,
        Icons.Outlined.SwapHoriz,
    ),
    SERVER(
        Res.string.settings_server_section,
        Res.string.settings_section_server_subtitle,
        Icons.Outlined.Dns,
    ),
    COMPUTER(
        Res.string.settings_computer_section,
        Res.string.settings_section_computer_subtitle,
        Icons.Outlined.Computer,
    ),
    DEVICE(
        Res.string.settings_device_section,
        Res.string.settings_section_device_subtitle,
        Icons.Outlined.Smartphone,
    ),
    APPEARANCE(
        Res.string.settings_appearance_section,
        Res.string.settings_section_appearance_subtitle,
        Icons.Outlined.Palette,
    ),
    DIAGNOSTICS(
        Res.string.settings_diagnostics_section,
        Res.string.settings_section_diagnostics_subtitle,
        Icons.Outlined.MonitorHeart,
    ),
    ABOUT(
        Res.string.settings_about_section,
        Res.string.settings_section_about_subtitle,
        Icons.Outlined.Info,
    ),
}

/**
 * Which sections the sheet has, given what this build and mode offer.
 *
 * The same switches the pages themselves read, so the menu can never name a
 * page that draws nothing: the mode page only exists where standalone is
 * possible, a desktop's address is a *server* in remote mode and a *computer*
 * to copy from in standalone, and the device names only mean something to a
 * desktop that asks for them.
 */
internal fun settingsSections(
    hasDesktop: Boolean,
    supportsStandalone: Boolean,
    /** True where the app is in a mode other than remote, which always has a way back out. */
    canLeaveMode: Boolean = false,
): List<SettingsSection> =
    buildList {
        if (supportsStandalone || canLeaveMode) add(SettingsSection.MODE)
        add(if (hasDesktop) SettingsSection.SERVER else SettingsSection.COMPUTER)
        if (hasDesktop) add(SettingsSection.DEVICE)
        add(SettingsSection.APPEARANCE)
        add(SettingsSection.DIAGNOSTICS)
        add(SettingsSection.ABOUT)
    }

/** How the one-word connection summary at the foot of the menu is coloured. */
internal enum class ConnectionTone { GOOD, WARNING, BAD, PENDING }

/** The one-word summary of a desktop connection, and its colour. */
internal class ConnectionSummary(val label: StringResource, val tone: ConnectionTone)

/**
 * The connection as one word, for the foot of the menu and the Diagnostics
 * page — the same reading of a status the startup screen explains at length.
 */
internal fun connectionSummary(state: StatusUiState): ConnectionSummary = when (state) {
    is StatusUiState.Loading -> ConnectionSummary(Res.string.settings_connection_checking, ConnectionTone.PENDING)
    is StatusUiState.Error -> ConnectionSummary(Res.string.settings_connection_unreachable, ConnectionTone.BAD)
    is StatusUiState.Unauthorized -> ConnectionSummary(Res.string.settings_connection_rejected, ConnectionTone.BAD)
    is StatusUiState.NotChurchPresenter ->
        ConnectionSummary(Res.string.settings_connection_wrong_server, ConnectionTone.BAD)
    is StatusUiState.Success ->
        if (state.warnings.isEmpty()) ConnectionSummary(Res.string.settings_connection_connected, ConnectionTone.GOOD)
        else ConnectionSummary(Res.string.settings_connection_limited, ConnectionTone.WARNING)
}
