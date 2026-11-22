package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.ui.unit.em
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.church.presenter.churchpresentermobile.util.Analytics
import com.church.presenter.churchpresentermobile.util.AnalyticsScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.cd_back
import churchpresentermobile.composeapp.generated.resources.cd_close
import churchpresentermobile.composeapp.generated.resources.mode_switch_cancel
import churchpresentermobile.composeapp.generated.resources.mode_switch_confirm_action
import churchpresentermobile.composeapp.generated.resources.mode_switch_confirm_body
import churchpresentermobile.composeapp.generated.resources.mode_switch_confirm_title
import churchpresentermobile.composeapp.generated.resources.mode_switch_to_calendar_body
import churchpresentermobile.composeapp.generated.resources.mode_switch_to_calendar_title
import churchpresentermobile.composeapp.generated.resources.mode_switch_to_remote_body
import churchpresentermobile.composeapp.generated.resources.mode_switch_to_remote_title
import churchpresentermobile.composeapp.generated.resources.settings_check_status
import churchpresentermobile.composeapp.generated.resources.settings_host_empty
import churchpresentermobile.composeapp.generated.resources.settings_invalid_host
import churchpresentermobile.composeapp.generated.resources.settings_invalid_port
import churchpresentermobile.composeapp.generated.resources.settings_save
import churchpresentermobile.composeapp.generated.resources.settings_status_recheck
import churchpresentermobile.composeapp.generated.resources.settings_title
import churchpresentermobile.composeapp.generated.resources.status_connected
import churchpresentermobile.composeapp.generated.resources.status_connecting
import churchpresentermobile.composeapp.generated.resources.status_error_title
import churchpresentermobile.composeapp.generated.resources.status_limited_functionality
import churchpresentermobile.composeapp.generated.resources.status_not_churchpresenter_title
import churchpresentermobile.composeapp.generated.resources.status_unauthorized_title
import com.church.presenter.churchpresentermobile.DeepLinkHandler
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.model.AppModeHolder
import com.church.presenter.churchpresentermobile.model.supportsStandalone
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.SettingsViewModel
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import com.church.presenter.churchpresentermobile.viewmodel.StatusViewModel
import org.jetbrains.compose.resources.stringResource

// ─────────────────────────────────────────────────────────────────────────────
// Settings screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The settings sheet.
 *
 * @param twoPane The tablet arrangement, see [usesTwoPaneLayout]: the section
 *   list down the left, the same form beside it with the server fields paired
 *   up. On a phone the form fills the sheet and the list is not drawn.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    /** Opens the contact form. Settings is where people look for a way to reach support. */
    onContact: () -> Unit,
    /**
     * Supplied by tests only, matching the seam [SongsScreen] and [BibleScreen]
     * already use. The app leaves both null and gets its own instances; a test
     * cannot, because this screen is a `Dialog` with no ViewModelStoreOwner of
     * its own to scope them to.
     */
    providedViewModel: SettingsViewModel? = null,
    providedStatusViewModel: StatusViewModel? = null,
    twoPane: Boolean = false,
    /**
     * The section to open with, or null for the usual start: a tablet opens on
     * the first section, a phone on the menu. The live bar's screens dot opens
     * on Display; a test uses it to photograph one page without tapping through.
     */
    initialSection: SettingsSection? = null,
    /**
     * Settings → Display, built by the shell: it owns the engine and the screens
     * that page drives, which this dialog has no business constructing.
     */
    displayPage: @Composable () -> Unit = {},
    /** The theme picked on Appearance, applied at once rather than on Save. */
    onThemeChange: (ThemeMode) -> Unit = {},
) {
    val viewModel: SettingsViewModel = providedViewModel
        ?: viewModel { SettingsViewModel(appSettings) }
    val host         by viewModel.host.collectAsState()
    val port         by viewModel.port.collectAsState()
    val apiKey       by viewModel.apiKey.collectAsState()
    val displayName  by viewModel.displayName.collectAsState()
    val customDeviceName by viewModel.customDeviceName.collectAsState()
    val hostError    by viewModel.hostError.collectAsState()
    val portError    by viewModel.portError.collectAsState()
    val activeUrl    by viewModel.activeUrl.collectAsState()
    val draftBaseUrl by viewModel.draftBaseUrl.collectAsState()
    val urlChanged   by viewModel.urlChanged.collectAsState()
    val themeMode    by viewModel.themeMode.collectAsState()
    val telemetryEnabled by viewModel.telemetryEnabled.collectAsState()

    // Mode the user has tapped but not yet confirmed. Switching mode redirects
    // where everything projects, so it asks first.
    var pendingMode      by remember { mutableStateOf<AppMode?>(null) }

    // Inline server-status check
    val statusViewModel: StatusViewModel = providedStatusViewModel
        ?: viewModel(key = "settings_status") { StatusViewModel(appSettings) }

    // Directly observe the global deep-link counter.
    // This fires reliably even when the dialog is open — no token-passing required.
    val deepLinkCount by DeepLinkHandler.appliedCount.collectAsState()
    LaunchedEffect(deepLinkCount) { if (deepLinkCount > 0) viewModel.reloadFromStorage() }

    val emptyHostError   = stringResource(Res.string.settings_host_empty)
    val invalidPortError = stringResource(Res.string.settings_invalid_port)
    val invalidHostError = stringResource(Res.string.settings_invalid_host)

    val appMode by AppModeHolder.mode.collectAsState()
    pendingMode?.let { target ->
        ModeSwitchDialog(
            target = target,
            onConfirm = { AppModeHolder.set(appSettings, target); pendingMode = null },
            onDismiss = { pendingMode = null },
        )
    }

    val dismiss = { viewModel.cancel(); onDismiss() }
    val save = {
        viewModel.save(
            // The status shown beside the form describes the address it was checked against;
            // after a new one is saved, "Connected" would be a claim about the old server.
            onSuccess        = { statusViewModel.recheck(); onSaved(); onDismiss() },
            emptyHostError   = emptyHostError,
            invalidPortError = invalidPortError,
            invalidHostError = invalidHostError,
        )
    }
    // The desktop connection, for the menu's foot and the Diagnostics page.
    // Standalone has no desktop, so it shows nothing rather than "unreachable".
    val hasDesktop = appMode == AppMode.REMOTE
    val statusState by statusViewModel.uiState.collectAsState()
    val page: @Composable (SettingsSection, () -> Unit) -> Unit = { section, openServer ->
        SettingsPage(
            section = section,
            appSettings = appSettings,
            appMode = appMode,
            activeUrl = activeUrl,
            draft = ServerDraft(host, port, apiKey, customDeviceName, displayName, hostError, portError),
            edits = ServerDraftEdits(
                onHost = viewModel::setHost,
                onPort = viewModel::setPort,
                onApiKey = viewModel::setApiKey,
                onCustomDeviceName = viewModel::setCustomDeviceName,
                onDisplayName = viewModel::setDisplayName,
                onReset = viewModel::resetToDefaults,
            ),
            draftUrl = draftBaseUrl.takeIf { urlChanged },
            themeMode = themeMode,
            telemetryEnabled = telemetryEnabled,
            status = statusState.takeIf { hasDesktop },
            onModeTapped = { pendingMode = it },
            onThemeMode = { mode ->
                viewModel.setThemeMode(mode)
                appSettings.themeMode = mode
                onThemeChange(mode)
            },
            onTelemetry = viewModel::setTelemetryEnabled,
            // What the fields say, not what was saved: a wrong address typed in must not still read "Connected".
            onCheckStatus = {
                if (host.isBlank()) {
                    statusViewModel.recheck()
                } else {
                    statusViewModel.recheckAt(draftBaseUrl, apiKey.trim())
                }
            },
            onContact = onContact,
            twoPane = twoPane,
            displayPage = displayPage,
            onOpenServer = openServer,
        )
    }
    SettingsSheet(
        sections = settingsSections(
            hasDesktop = hasDesktop,
            supportsStandalone = supportsStandalone,
            canLeaveMode = appMode != AppMode.REMOTE,
            usesComputer = appMode != AppMode.CALENDAR,
            hasDisplay = appMode == AppMode.STANDALONE,
        ),
        status = statusState.takeIf { hasDesktop },
        address = ServerDraft(host, port, apiKey, customDeviceName, displayName).address(saved = appSettings),
        appMode = appMode,
        twoPane = twoPane,
        initialSection = initialSection,
        onDismiss = dismiss,
        onSave = save,
        page = page,
    )
}

/**
 * The full-screen dialog around the pages: the menu, and the page itself.
 *
 * On a tablet the menu sits beside the page and the pane on the right shows
 * the section chosen there. On a phone the menu *is* the sheet: tapping a
 * section pushes that section's page, under a header that names it and leads
 * back. Save acts on the whole draft, so it is on every page that edits it.
 *
 * @param status The desktop connection, or null in a mode that has no desktop.
 * @param page The page for one section, given the way to open the Server page.
 */
@Composable
internal fun SettingsSheet(
    sections: List<SettingsSection>,
    status: StatusUiState?,
    address: String,
    appMode: AppMode,
    twoPane: Boolean,
    initialSection: SettingsSection?,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    page: @Composable (SettingsSection, () -> Unit) -> Unit,
) {
    val colors = LocalAppColors.current
    // The tablet always has a section open; the phone starts on the menu.
    // Keyed on the sections so a mode switch, which swaps Server for Computer,
    // cannot leave a section selected that the menu no longer offers.
    var selected by remember(sections, twoPane) {
        mutableStateOf(initialSection?.takeIf { it in sections } ?: if (twoPane) sections.first() else null)
    }
    val open = selected
    // The sheet's pages are its own, so it reports them itself: App reports
    // nothing while Settings is open (see currentScreenName).
    LaunchedEffect(open) {
        Analytics.logScreenView(AnalyticsScreen.settingsSection(open?.analyticsName))
    }
    val openServer = { if (SettingsSection.SERVER in sections) selected = SettingsSection.SERVER }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // On a phone's page, back belongs to the handler below, which returns to the menu.
            dismissOnBackPress      = twoPane || open == null,
            dismissOnClickOutside   = false,
        )
    ) {
        // Inside the dialog, so it hears back before the dialog does.
        AppBackHandler(enabled = !twoPane && open != null) { selected = null }
        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (twoPane && open != null) {
                    SettingsSectionList(
                        sections = sections,
                        current = open,
                        appMode = appMode,
                        onSelect = { selected = it },
                        onDone = onDismiss,
                        status = status,
                        address = address,
                        modifier = Modifier.width(SettingsSectionPaneWidth),
                    )
                    VerticalDivider(color = colors.border)
                }
                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (open != null) {
                        SettingsHeader(
                            title = stringResource(open.title),
                            onSave = onSave,
                            onBack = if (!twoPane) ({ selected = null }) else null,
                            twoPane = twoPane,
                        )
                        page(open, openServer)
                    } else {
                        SettingsMenu(
                            sections = sections,
                            appMode = appMode,
                            onSelect = { selected = it },
                            onDone = onDismiss,
                            status = status,
                            address = address,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * A page's header: on a phone the way back to the menu, the page's name and
 * Save; on a tablet the name on the left and Save on the right.
 */
@Composable
private fun SettingsHeader(
    title: String,
    onSave: (() -> Unit)?,
    onBack: (() -> Unit)?,
    twoPane: Boolean,
) {
    val colors = LocalAppColors.current
    val save: @Composable () -> Unit = {
        if (onSave != null) {
            Text(
                text = stringResource(Res.string.settings_save),
                color = colors.onAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.accent)
                    .testTag(UiTags.SETTINGS_SAVE)
                    .clickable(role = Role.Button, onClick = onSave)
                    .padding(horizontal = if (twoPane) 20.dp else 16.dp, vertical = if (twoPane) 11.dp else 9.dp),
            )
        }
    }
    if (twoPane) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    color = colors.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.025).em,
                    modifier = Modifier.weight(1f),
                )
                save()
            }
            HorizontalDivider(color = colors.border)
        }
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(52.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.width(96.dp)) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .testTag(UiTags.SETTINGS_BACK)
                        .clickable(role = Role.Button, onClick = onBack)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(Res.string.cd_back),
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        stringResource(Res.string.settings_title),
                        color = colors.accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Text(
            text = title,
            color = colors.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Row(modifier = Modifier.width(96.dp), horizontalArrangement = Arrangement.End) { save() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Server Status dialog
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerStatusDialog(
    statusViewModel: StatusViewModel,
    onDismiss: () -> Unit,
    /** The tablet's arrangement, see [ServerStatusTablet]; [address] is the URL it names on a failure. */
    twoPane: Boolean = false,
    address: String = "",
) {
    val uiState by statusViewModel.uiState.collectAsState()

    val subtitle = when (val s = uiState) {
        is StatusUiState.Loading           -> stringResource(Res.string.status_connecting)
        is StatusUiState.Error             -> stringResource(Res.string.status_error_title)
        is StatusUiState.Unauthorized      -> stringResource(Res.string.status_unauthorized_title)
        is StatusUiState.NotChurchPresenter -> stringResource(Res.string.status_not_churchpresenter_title)
        is StatusUiState.Success -> if (s.warnings.isEmpty())
            stringResource(Res.string.status_connected)
        else
            stringResource(Res.string.status_limited_functionality)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = true,
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(stringResource(Res.string.settings_check_status),
                                    style = MaterialTheme.typography.titleMedium)
                                Text(subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag(UiTags.STATUS_DIALOG_CLOSE)) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.cd_close))
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { statusViewModel.recheck() },
                                modifier = Modifier.testTag(UiTags.STATUS_DIALOG_RECHECK),
                            ) {
                                Icon(Icons.Filled.Refresh,
                                    contentDescription = stringResource(Res.string.settings_status_recheck))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor             = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor          = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionIconContentColor     = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    )
                }
            ) { innerPadding ->
                if (twoPane) {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        ServerStatusTablet(
                            state = uiState,
                            address = address,
                            onRecheck = { statusViewModel.recheck() },
                        )
                    }
                    return@Scaffold
                }
                ServerStatusPhone(
                    state = uiState,
                    onRecheck = { statusViewModel.recheck() },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
internal fun ModeSwitchDialog(
    target: AppMode,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (target) {
                    AppMode.STANDALONE -> stringResource(Res.string.mode_switch_confirm_title)
                    AppMode.REMOTE -> stringResource(Res.string.mode_switch_to_remote_title)
                    AppMode.CALENDAR -> stringResource(Res.string.mode_switch_to_calendar_title)
                }
            )
        },
        text = {
            ScrollingDialogBody {
                Text(
                    when (target) {
                        AppMode.STANDALONE -> stringResource(Res.string.mode_switch_confirm_body)
                        AppMode.REMOTE -> stringResource(Res.string.mode_switch_to_remote_body)
                        AppMode.CALENDAR -> stringResource(Res.string.mode_switch_to_calendar_body)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(UiTags.MODE_SWITCH_CONFIRM)) {
                Text(stringResource(Res.string.mode_switch_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(UiTags.MODE_SWITCH_CANCEL)) {
                Text(stringResource(Res.string.mode_switch_cancel))
            }
        },
    )
}
