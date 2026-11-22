package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.app_title
import churchpresentermobile.composeapp.generated.resources.contact_us_title
import churchpresentermobile.composeapp.generated.resources.mode_calendar_body
import churchpresentermobile.composeapp.generated.resources.mode_remote_body
import churchpresentermobile.composeapp.generated.resources.mode_standalone_body
import churchpresentermobile.composeapp.generated.resources.settings_about_copyright
import churchpresentermobile.composeapp.generated.resources.settings_app_version
import churchpresentermobile.composeapp.generated.resources.settings_computer_explain
import churchpresentermobile.composeapp.generated.resources.settings_connection_status_label
import churchpresentermobile.composeapp.generated.resources.settings_copy_from
import churchpresentermobile.composeapp.generated.resources.settings_developer_section
import churchpresentermobile.composeapp.generated.resources.settings_mode_footnote
import churchpresentermobile.composeapp.generated.resources.settings_mode_heading
import churchpresentermobile.composeapp.generated.resources.settings_privacy_section
import churchpresentermobile.composeapp.generated.resources.settings_server_section
import churchpresentermobile.composeapp.generated.resources.settings_server_version
import churchpresentermobile.composeapp.generated.resources.settings_status_address_label
import churchpresentermobile.composeapp.generated.resources.settings_telemetry_description
import churchpresentermobile.composeapp.generated.resources.settings_telemetry_label
import churchpresentermobile.composeapp.generated.resources.settings_test_error_body
import churchpresentermobile.composeapp.generated.resources.settings_test_error_send
import churchpresentermobile.composeapp.generated.resources.settings_test_error_sent
import churchpresentermobile.composeapp.generated.resources.settings_test_error_title
import churchpresentermobile.composeapp.generated.resources.settings_tvs_on_wifi
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.tv.SourceKind
import com.church.presenter.churchpresentermobile.tv.TvSource
import com.church.presenter.churchpresentermobile.ui.standalone.TvPicker
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
 * @param status The desktop connection, or null in a mode that has no desktop;
 *   Server, Diagnostics and About read it.
 * @param draftUrl The base URL the server fields currently spell, or null when
 *   it is the one already saved — the preview only appears while they differ.
 * @param onCheckStatus Runs the server check again; its answer shows on the Server page.
 * @param onOpenServer Opens the Server page, from the status line on Diagnostics.
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
    displayPage: @Composable () -> Unit = {},
    onOpenServer: () -> Unit = {},
) {
    val scroll = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollbar(scroll)
            .verticalScroll(scroll)
            .padding(
                if (twoPane) PaddingValues(start = 40.dp, end = 40.dp, top = 28.dp, bottom = 40.dp)
                else PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 30.dp),
            ),
    ) {
        Column(
            modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            when (section) {
                SettingsSection.MODE -> ModeSection(appMode = appMode, onModeTapped = onModeTapped)
                SettingsSection.DISPLAY -> Column(modifier = Modifier.testTag(UiTags.SETTINGS_DISPLAY_SECTION)) {
                    displayPage()
                }
                SettingsSection.SERVER -> ServerSection(
                    appSettings = appSettings,
                    activeUrl = activeUrl,
                    draft = draft,
                    edits = edits,
                    draftUrl = draftUrl,
                    status = status,
                    onCheckStatus = onCheckStatus,
                    twoPane = twoPane,
                )
                SettingsSection.COMPUTER -> ComputerSection(appSettings = appSettings)
                SettingsSection.TVS -> Column {
                    GroupLabel(
                        stringResource(Res.string.settings_tvs_on_wifi),
                        modifier = Modifier.testTag(UiTags.SETTINGS_TVS_SECTION),
                    )
                    TvPicker(source = desktopSource(appSettings))
                }
                SettingsSection.DEVICE -> DeviceSection(draft = draft, edits = edits)
                SettingsSection.APPEARANCE -> AppearanceSection(themeMode = themeMode, onThemeMode = onThemeMode)
                SettingsSection.DIAGNOSTICS -> DiagnosticsSection(
                    status = status,
                    address = "${appSettings.host}:${appSettings.port}",
                    telemetryEnabled = telemetryEnabled,
                    onTelemetry = onTelemetry,
                    onOpenServer = onOpenServer,
                )
                SettingsSection.ABOUT -> AboutSection(status = status, onContact = onContact)
            }
        }
    }
}

/** The computer this phone is connected to, as a TV reaches it. */
internal fun desktopSource(settings: AppSettings): TvSource = TvSource(
    name = settings.host,
    host = settings.host,
    port = settings.port,
    kind = SourceKind.DESKTOP,
    apiKey = settings.apiKey,
)

private val settingsModes = listOf(AppMode.REMOTE, AppMode.STANDALONE, AppMode.CALENDAR)

@Composable
private fun ModeSection(appMode: AppMode, onModeTapped: (AppMode) -> Unit) {
    val colors = LocalAppColors.current
    Column {
        GroupLabel(
            stringResource(Res.string.settings_mode_heading),
            modifier = Modifier.testTag(UiTags.SETTINGS_MODE_SECTION),
        )
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            settingsModes.forEachIndexed { index, mode ->
                val selected = mode == appMode
                val tint = colors.modeTint(mode)
                val shape = RoundedCornerShape(18.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (selected) Modifier.halo(tint.copy(alpha = 0.12f), 18.dp) else Modifier)
                        .clip(shape)
                        .background(if (selected) tint.copy(alpha = 0.09f) else colors.surface)
                        .border(1.5.dp, if (selected) tint else colors.border, shape)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { if (!selected) onModeTapped(mode) },
                        )
                        .testTag(UiTags.settingsMode(index))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    IconTile(mode.icon(), tint, size = 44.dp, radius = 13.dp, iconSize = 22.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(mode.titleRes()),
                            color = colors.text,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.02).em,
                        )
                        Text(
                            stringResource(modeBody(mode)),
                            color = colors.muted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Box(Modifier.padding(top = 2.dp)) { RadioMark(selected = selected, tint = tint) }
                }
            }
        }
        Footnote(stringResource(Res.string.settings_mode_footnote), modifier = Modifier.padding(top = 12.dp))
    }
}

private fun modeBody(mode: AppMode) = when (mode) {
    AppMode.REMOTE -> Res.string.mode_remote_body
    AppMode.STANDALONE -> Res.string.mode_standalone_body
    AppMode.CALENDAR -> Res.string.mode_calendar_body
}

/**
 * Standalone's one address: where songs and Bibles are copied from, never a
 * server it presents through.
 */
@Composable
private fun ComputerSection(appSettings: AppSettings) {
    val colors = LocalAppColors.current
    val tint = colors.greekAccent
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.25f), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            stringResource(Res.string.settings_computer_explain),
            color = colors.secondary,
            fontSize = 14.5.sp,
            lineHeight = 21.sp,
            modifier = Modifier.testTag(UiTags.SETTINGS_COMPUTER_SECTION),
        )
    }
    Column {
        GroupLabel(stringResource(Res.string.settings_copy_from))
        LookCard(padding = PaddingValues(18.dp)) {
            DesktopAddressFields(settings = appSettings, showHint = false)
        }
    }
}

@Composable
private fun DiagnosticsSection(
    status: StatusUiState?,
    address: String,
    telemetryEnabled: Boolean,
    onTelemetry: (Boolean) -> Unit,
    onOpenServer: () -> Unit,
) {
    val colors = LocalAppColors.current
    if (status != null) {
        Column {
            GroupLabel(stringResource(Res.string.settings_server_section))
            LookCard {
                val summary = connectionSummary(status)
                val tone = colors.toneColor(summary.tone)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpenServer)
                        .padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatusDot(tone)
                        Text(
                            stringResource(Res.string.settings_connection_status_label),
                            color = colors.secondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        stringResource(summary.label),
                        color = tone,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag(UiTags.SETTINGS_CONNECTION),
                    )
                }
                LookDivider()
                KeyValueRow(stringResource(Res.string.settings_status_address_label), address)
                val serverVersion = (status as? StatusUiState.Success)?.status?.appVersion
                if (serverVersion != null) {
                    LookDivider()
                    KeyValueRow(stringResource(Res.string.settings_server_version), serverVersion)
                }
            }
        }
    }
    Column {
        GroupLabel(stringResource(Res.string.settings_privacy_section))
        LookCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.settings_telemetry_label),
                        color = colors.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(Res.string.settings_telemetry_description),
                        color = colors.muted,
                        fontSize = 13.5.sp,
                        lineHeight = 19.5.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Switch(
                    checked = telemetryEnabled,
                    onCheckedChange = onTelemetry,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colors.accent,
                        checkedThumbColor = colors.onAccent,
                    ),
                    modifier = Modifier.testTag(UiTags.SETTINGS_TELEMETRY),
                )
            }
        }
    }
    if (isDebugBuild) DeveloperTools()
}

@Composable
private fun DeveloperTools() {
    val colors = LocalAppColors.current
    var testErrorSent by remember { mutableStateOf(false) }
    Column {
        GroupLabel(stringResource(Res.string.settings_developer_section))
        LookCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.settings_test_error_title),
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(Res.string.settings_test_error_body),
                        color = colors.muted,
                        fontSize = 13.5.sp,
                        lineHeight = 19.5.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (testErrorSent) {
                        Text(
                            stringResource(Res.string.settings_test_error_sent),
                            color = colors.accent,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp).testTag(UiTags.SETTINGS_TEST_ERROR_SENT),
                        )
                    }
                }
                PlainButton(
                    label = stringResource(Res.string.settings_test_error_send),
                    onClick = {
                        CrashReporting.recordException(
                            RuntimeException("Test error — ChurchPresenter Mobile v$appVersion"),
                        )
                        testErrorSent = true
                    },
                    modifier = Modifier.testTag(UiTags.SETTINGS_TEST_ERROR),
                )
            }
        }
    }
}

@Composable
private fun AboutSection(status: StatusUiState?, onContact: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .background(Brush.radialGradient(listOf(colors.accent.copy(alpha = 0.14f), Color.Transparent)))
            .border(1.dp, colors.border, shape)
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LogoTile(size = 72.dp, radius = 21.dp, crossHeight = 42.dp)
        Text(
            stringResource(Res.string.app_title),
            color = colors.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.03).em,
            modifier = Modifier.padding(top = 16.dp),
        )
        Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tagline("WORSHIP", colors.taglineWorship)
            Tagline("·", colors.muted)
            Tagline("PRESENT", colors.taglinePresent)
            Tagline("·", colors.muted)
            Tagline("CONNECT", colors.taglineConnect)
        }
    }
    LookCard {
        KeyValueRow(stringResource(Res.string.settings_app_version), appVersion)
        val serverVersion = (status as? StatusUiState.Success)?.status?.appVersion
        if (serverVersion != null) {
            LookDivider()
            KeyValueRow(stringResource(Res.string.settings_server_version), serverVersion)
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PlainButton(
            label = stringResource(Res.string.contact_us_title),
            onClick = onContact,
            leading = Icons.Filled.MailOutline,
            modifier = Modifier.testTag(UiTags.SETTINGS_CONTACT),
        )
        Text(stringResource(Res.string.settings_about_copyright), color = colors.muted, fontSize = 13.sp)
    }
}

@Composable
private fun Tagline(text: String, color: Color) {
    Text(text, color = color, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.16.em)
}
