package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.app_title
import churchpresentermobile.composeapp.generated.resources.contact_us_title
import churchpresentermobile.composeapp.generated.resources.mode_remote_body
import churchpresentermobile.composeapp.generated.resources.mode_remote_title
import churchpresentermobile.composeapp.generated.resources.mode_standalone_body
import churchpresentermobile.composeapp.generated.resources.mode_standalone_title
import churchpresentermobile.composeapp.generated.resources.settings_about_copyright
import churchpresentermobile.composeapp.generated.resources.settings_app_version
import churchpresentermobile.composeapp.generated.resources.settings_computer_explain
import churchpresentermobile.composeapp.generated.resources.settings_connection_status_label
import churchpresentermobile.composeapp.generated.resources.settings_developer_section
import churchpresentermobile.composeapp.generated.resources.settings_device_name_hint
import churchpresentermobile.composeapp.generated.resources.settings_device_name_label
import churchpresentermobile.composeapp.generated.resources.settings_device_name_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_display_name_hint
import churchpresentermobile.composeapp.generated.resources.settings_display_name_label
import churchpresentermobile.composeapp.generated.resources.settings_display_name_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_privacy_section
import churchpresentermobile.composeapp.generated.resources.settings_send_test_error
import churchpresentermobile.composeapp.generated.resources.settings_server_section
import churchpresentermobile.composeapp.generated.resources.settings_server_version
import churchpresentermobile.composeapp.generated.resources.settings_telemetry_description
import churchpresentermobile.composeapp.generated.resources.settings_telemetry_label
import churchpresentermobile.composeapp.generated.resources.settings_test_error_sent
import churchpresentermobile.composeapp.generated.resources.settings_theme_dark
import churchpresentermobile.composeapp.generated.resources.settings_theme_light
import churchpresentermobile.composeapp.generated.resources.settings_theme_system
import churchpresentermobile.composeapp.generated.resources.settings_theme_title
import churchpresentermobile.composeapp.generated.resources.settings_this_device
import com.church.presenter.churchpresentermobile.deviceName
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.util.CrashReporting
import com.church.presenter.churchpresentermobile.util.appVersion
import com.church.presenter.churchpresentermobile.util.isDebugBuild
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import org.jetbrains.compose.resources.stringResource

/**
 * One page of the settings sheet: the controls of the section chosen in the
 * menu, in a scrolling column.
 *
 * The same page on both shapes — beside the list on a tablet, in the menu's
 * place on a phone. A tablet only pairs the server fields up two to a row
 * ([twoPane]).
 *
 * @param status The desktop connection, or null in a mode that has no desktop;
 *   Diagnostics and About read it.
 * @param draftUrl The base URL the server fields currently spell, or null when
 *   it is the one already saved — the preview only appears while they differ.
 */
@Composable
internal fun SettingsPage(
    section: SettingsSection,
    appSettings: AppSettings,
    appMode: AppMode,
    activeUrl: String,
    draft: ServerDraft,
    edits: ServerDraftEdits,
    draftUrl: String?,
    themeMode: ThemeMode,
    telemetryEnabled: Boolean,
    status: StatusUiState?,
    onModeTapped: (AppMode) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onTelemetry: (Boolean) -> Unit,
    onCheckStatus: () -> Unit,
    onContact: () -> Unit,
    twoPane: Boolean,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollbar(scroll)
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (section) {
            SettingsSection.MODE -> ModeSection(appMode = appMode, onModeTapped = onModeTapped)
            SettingsSection.SERVER -> ServerSection(
                appSettings = appSettings,
                activeUrl = activeUrl,
                draft = draft,
                edits = edits,
                draftUrl = draftUrl,
                onCheckStatus = onCheckStatus,
                twoPane = twoPane,
            )
            SettingsSection.COMPUTER -> ComputerSection(appSettings = appSettings)
            SettingsSection.DEVICE -> DeviceSection(draft = draft, edits = edits)
            SettingsSection.APPEARANCE -> AppearanceSection(themeMode = themeMode, onThemeMode = onThemeMode)
            SettingsSection.DIAGNOSTICS -> DiagnosticsSection(
                status = status,
                telemetryEnabled = telemetryEnabled,
                onTelemetry = onTelemetry,
            )
            SettingsSection.ABOUT -> AboutSection(status = status, onContact = onContact)
        }
    }
}

@Composable
private fun ModeSection(appMode: AppMode, onModeTapped: (AppMode) -> Unit) {
    val colors = LocalAppColors.current
    SectionTitle(SettingsSection.MODE.title, tag = UiTags.SETTINGS_MODE_SECTION)
    val modeOptions = listOf(AppMode.REMOTE, AppMode.STANDALONE)
    SegmentedControl(
        options = listOf(
            stringResource(Res.string.mode_remote_title),
            stringResource(Res.string.mode_standalone_title),
        ),
        selectedIndex = modeOptions.indexOf(appMode).coerceAtLeast(0),
        onSelect = { index ->
            val target = modeOptions[index]
            if (target != appMode) onModeTapped(target)
        },
        optionTag = { UiTags.settingsMode(it) },
    )
    Text(
        text = if (appMode == AppMode.STANDALONE) {
            stringResource(Res.string.mode_standalone_body)
        } else {
            stringResource(Res.string.mode_remote_body)
        },
        fontSize = 12.sp,
        color = colors.muted,
    )
}

/**
 * Standalone's one address.
 *
 * Commit e8e35ae removed the whole server block from standalone, correctly: an
 * address that names a machine doing the presenting means nothing when this
 * phone is the presenter. But content still has to come from somewhere, and the
 * Library tab's "copy from computer" was silently aiming at the default host
 * with no way to correct it. So the address comes back — and only the address,
 * framed as where content is copied from rather than as a server. No status
 * check, no active-server card: neither has anything to report in a mode that
 * never connects.
 */
@Composable
private fun ComputerSection(appSettings: AppSettings) {
    val colors = LocalAppColors.current
    SectionTitle(SettingsSection.COMPUTER.title, tag = UiTags.SETTINGS_COMPUTER_SECTION)
    Text(
        text = stringResource(Res.string.settings_computer_explain),
        fontSize = 11.sp,
        color = colors.muted,
    )
    DesktopAddressFields(settings = appSettings, showHint = false)
}

/** The two names this device gives the desktop, each with a line saying where it shows up. */
@Composable
private fun DeviceSection(draft: ServerDraft, edits: ServerDraftEdits) {
    val colors = LocalAppColors.current
    SectionTitle(Res.string.settings_this_device)
    // The placeholder is what the desktop will be told if this is left blank,
    // so the operator can see the OS name and decide whether it is good enough
    // — "iPhone" usually isn't when there are three in the building.
    SettingsField(
        label = stringResource(Res.string.settings_device_name_label),
        value = draft.customDeviceName,
        onValueChange = edits.onCustomDeviceName,
        modifier = Modifier.testTag(UiTags.SETTINGS_DEVICE_NAME),
        placeholder = deviceName().ifBlank { stringResource(Res.string.settings_device_name_placeholder) },
    )
    Text(text = stringResource(Res.string.settings_device_name_hint), fontSize = 11.sp, color = colors.muted)
    // A separate field because the desktop shows a question's author and the
    // device it came from on separate lines: "Sound desk" answers one of those
    // and not the other.
    SettingsField(
        label = stringResource(Res.string.settings_display_name_label),
        value = draft.displayName,
        onValueChange = edits.onDisplayName,
        placeholder = stringResource(Res.string.settings_display_name_placeholder),
        modifier = Modifier.testTag(UiTags.SETTINGS_DISPLAY_NAME),
    )
    Text(text = stringResource(Res.string.settings_display_name_hint), fontSize = 11.sp, color = colors.muted)
}

/** The theme control — it drives the theme live, so there is nothing to save here. */
@Composable
private fun AppearanceSection(themeMode: ThemeMode, onThemeMode: (ThemeMode) -> Unit) {
    SectionTitle(Res.string.settings_theme_title)
    val themeOptions = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
    SegmentedControl(
        options = listOf(
            stringResource(Res.string.settings_theme_system),
            stringResource(Res.string.settings_theme_light),
            stringResource(Res.string.settings_theme_dark),
        ),
        selectedIndex = themeOptions.indexOf(themeMode).coerceAtLeast(0),
        onSelect = { onThemeMode(themeOptions[it]) },
        optionTag = { UiTags.settingsTheme(it) },
    )
}

/**
 * The desktop connection in one row, the usage-data switch, and in a debug
 * build the crash-report probe — everything about finding out what is wrong.
 */
@Composable
private fun DiagnosticsSection(
    status: StatusUiState?,
    telemetryEnabled: Boolean,
    onTelemetry: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    if (status != null) {
        SectionTitle(Res.string.settings_server_section)
        val summary = connectionSummary(status)
        val tone = when (summary.tone) {
            ConnectionTone.GOOD -> colors.accent
            ConnectionTone.WARNING -> colors.warning
            ConnectionTone.BAD -> colors.danger
            ConnectionTone.PENDING -> colors.muted
        }
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(tone))
                    Text(
                        stringResource(Res.string.settings_connection_status_label),
                        color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    )
                }
                Text(
                    stringResource(summary.label),
                    color = tone, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag(UiTags.SETTINGS_CONNECTION),
                )
            }
        }
        HorizontalDivider(color = colors.borderSubtle)
    }
    SectionTitle(Res.string.settings_privacy_section)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.settings_telemetry_label), color = colors.text, fontSize = 14.sp)
            Text(
                text = stringResource(Res.string.settings_telemetry_description),
                fontSize = 12.sp,
                color = colors.muted,
            )
        }
        Switch(
            checked = telemetryEnabled,
            onCheckedChange = onTelemetry,
            modifier = Modifier.testTag(UiTags.SETTINGS_TELEMETRY),
        )
    }
    if (isDebugBuild) {
        HorizontalDivider(color = colors.borderSubtle)
        DeveloperTools()
    }
}

/** Debug builds only. */
@Composable
private fun DeveloperTools() {
    val colors = LocalAppColors.current
    var testErrorSent by remember { mutableStateOf(false) }
    SectionTitle(Res.string.settings_developer_section)
    OutlineActionButton(
        label = stringResource(Res.string.settings_send_test_error),
        icon = Icons.Filled.Warning,
        modifier = Modifier.testTag(UiTags.SETTINGS_TEST_ERROR),
        onClick = {
            CrashReporting.recordException(
                RuntimeException("Test error — ChurchPresenter Mobile v$appVersion")
            )
            testErrorSent = true
        },
    )
    if (testErrorSent) {
        Text(
            text = stringResource(Res.string.settings_test_error_sent),
            fontSize = 12.sp,
            color = colors.accent,
            modifier = Modifier.testTag(UiTags.SETTINGS_TEST_ERROR_SENT),
        )
    }
}

/** The app's name and version, the desktop's version when it has answered, and the way to reach us. */
@Composable
private fun AboutSection(status: StatusUiState?, onContact: () -> Unit) {
    val colors = LocalAppColors.current
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            CrossIcon(brush = colors.crossBrush, modifier = Modifier.size(width = 24.dp, height = 48.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(Res.string.app_title),
                    color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.03).em,
                )
                Text(stringResource(Res.string.settings_about_copyright), color = colors.muted, fontSize = 13.sp)
            }
        }
    }
    SettingsCard {
        ValueRow(stringResource(Res.string.settings_app_version), appVersion, mono = true)
        val serverVersion = (status as? StatusUiState.Success)?.status?.appVersion
        if (serverVersion != null) {
            HorizontalDivider(color = colors.borderSubtle)
            ValueRow(stringResource(Res.string.settings_server_version), serverVersion, mono = true)
        }
    }
    // Contact — in every build and both modes: the endpoint is a public one on
    // the internet, so it needs no desktop.
    OutlineActionButton(
        label = stringResource(Res.string.contact_us_title),
        icon = Icons.Filled.MailOutline,
        onClick = onContact,
        modifier = Modifier.testTag(UiTags.SETTINGS_CONTACT),
    )
}
