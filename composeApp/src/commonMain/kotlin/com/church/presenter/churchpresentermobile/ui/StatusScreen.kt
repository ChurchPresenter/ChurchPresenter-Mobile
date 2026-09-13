package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.util.appVersion
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.status_connected
import churchpresentermobile.composeapp.generated.resources.status_connecting
import churchpresentermobile.composeapp.generated.resources.status_version_line
import churchpresentermobile.composeapp.generated.resources.status_continue
import churchpresentermobile.composeapp.generated.resources.status_continue_anyway
import churchpresentermobile.composeapp.generated.resources.status_endpoint_unavailable
import churchpresentermobile.composeapp.generated.resources.status_error_title
import churchpresentermobile.composeapp.generated.resources.status_info_bibles
import churchpresentermobile.composeapp.generated.resources.status_info_features
import churchpresentermobile.composeapp.generated.resources.status_info_songbooks
import churchpresentermobile.composeapp.generated.resources.status_issues_detected
import churchpresentermobile.composeapp.generated.resources.status_limited_functionality
import churchpresentermobile.composeapp.generated.resources.status_not_churchpresenter_body
import churchpresentermobile.composeapp.generated.resources.status_not_churchpresenter_title
import churchpresentermobile.composeapp.generated.resources.status_open_settings
import churchpresentermobile.composeapp.generated.resources.status_permission_present
import churchpresentermobile.composeapp.generated.resources.status_permission_schedule
import churchpresentermobile.composeapp.generated.resources.status_permission_upload
import churchpresentermobile.composeapp.generated.resources.status_permissions_title
import churchpresentermobile.composeapp.generated.resources.status_retry
import churchpresentermobile.composeapp.generated.resources.status_server_version
import churchpresentermobile.composeapp.generated.resources.status_unauthorized_body
import churchpresentermobile.composeapp.generated.resources.status_unauthorized_title
import churchpresentermobile.composeapp.generated.resources.status_warn_missing_endpoint_body
import churchpresentermobile.composeapp.generated.resources.status_warn_missing_endpoint_title
import churchpresentermobile.composeapp.generated.resources.status_warn_no_api_key_body
import churchpresentermobile.composeapp.generated.resources.status_warn_no_api_key_title
import churchpresentermobile.composeapp.generated.resources.status_warn_no_bibles_body
import churchpresentermobile.composeapp.generated.resources.status_warn_no_bibles_title
import churchpresentermobile.composeapp.generated.resources.status_warn_no_songbooks_body
import churchpresentermobile.composeapp.generated.resources.status_warn_no_songbooks_title
import churchpresentermobile.composeapp.generated.resources.status_warn_present_blocked_body
import churchpresentermobile.composeapp.generated.resources.status_warn_present_blocked_title
import churchpresentermobile.composeapp.generated.resources.status_warn_schedule_blocked_body
import churchpresentermobile.composeapp.generated.resources.status_warn_schedule_blocked_title
import churchpresentermobile.composeapp.generated.resources.status_warn_unknown_version_body
import churchpresentermobile.composeapp.generated.resources.status_warn_unknown_version_title
import churchpresentermobile.composeapp.generated.resources.status_warn_upload_blocked_body
import churchpresentermobile.composeapp.generated.resources.status_warn_upload_blocked_title
import com.church.presenter.churchpresentermobile.model.DevicePermissions
import com.church.presenter.churchpresentermobile.model.StatusWarning
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import com.church.presenter.churchpresentermobile.viewmodel.StatusViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Startup status / compatibility screen.
 *
 * Shown once after the splash screen. If the server responds with no warnings it
 * auto-advances after 3 seconds. If there are warnings or an error the user
 * must explicitly tap Continue (or Open Settings).
 *
 * @param viewModel      The [StatusViewModel] that owns the network call.
 * @param onContinue     Called when the user (or the auto-advance timer) dismisses the screen.
 * @param onOpenSettings Called when the user taps "Open Settings" from the error state.
 * @param twoPane        Lay the cards out side by side rather than stacked — the
 *   tablet arrangement, see [usesTwoPaneLayout]. The content is the same either
 *   way; only where it sits changes.
 */
@Composable
fun StatusScreen(
    viewModel: StatusViewModel,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
    twoPane: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsState()

    // Auto-advance when there are no warnings — hold for 3 seconds so users
    // can read the permissions summary before being moved on automatically.
    LaunchedEffect(uiState) {
        if (uiState is StatusUiState.Success) {
            val warnings = (uiState as StatusUiState.Success).warnings
            if (warnings.isEmpty()) {
                delay(3000)
                onContinue()
            }
        }
    }

    val metrics = if (twoPane) StatusMetrics.Tablet else StatusMetrics.Phone
    CompositionLocalProvider(LocalStatusMetrics provides metrics) {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                StatusContent(
                    state = uiState,
                    onRetry = { viewModel.recheck() },
                    onContinue = onContinue,
                    onOpenSettings = onOpenSettings,
                    sideBySide = twoPane,
                )
            }
        }
    }
}

/** One of the five states, as its own content. */
@Composable
private fun StatusContent(
    state: StatusUiState,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
    sideBySide: Boolean,
) {
    when (state) {
        is StatusUiState.Loading -> LoadingContent(onOpenSettings = onOpenSettings)
        is StatusUiState.Error   -> ErrorContent(
            title          = stringResource(Res.string.status_error_title),
            message        = state.message,
            onRetry        = onRetry,
            onContinue     = onContinue,
            onOpenSettings = onOpenSettings,
            sideBySide     = sideBySide,
        )
        is StatusUiState.Unauthorized -> ErrorContent(
            title          = stringResource(Res.string.status_unauthorized_title),
            message        = stringResource(Res.string.status_unauthorized_body),
            onRetry        = onRetry,
            onContinue     = onContinue,
            onOpenSettings = onOpenSettings,
            sideBySide     = sideBySide,
        )
        is StatusUiState.NotChurchPresenter -> ErrorContent(
            title          = stringResource(Res.string.status_not_churchpresenter_title),
            message        = stringResource(Res.string.status_not_churchpresenter_body),
            onRetry        = onRetry,
            onContinue     = onContinue,
            onOpenSettings = onOpenSettings,
            sideBySide     = sideBySide,
        )
        is StatusUiState.Success -> {
            if (state.warnings.isEmpty()) {
                AllGoodContent(
                    permissions       = state.status.permissions,
                    serverVersion     = state.status.appVersion,
                    bibles            = state.status.bibles,
                    songbooks         = state.status.songbooks,
                    endpointAvailable = state.status.endpointAvailable,
                    onContinue        = onContinue,
                    sideBySide        = sideBySide,
                )
            } else {
                WarningsContent(
                    appVersion     = state.status.appVersion,
                    warnings       = state.warnings,
                    permissions    = state.status.permissions,
                    bibles         = state.status.bibles,
                    songbooks      = state.status.songbooks,
                    features       = state.status.features,
                    onContinue     = onContinue,
                    onOpenSettings = onOpenSettings,
                    sideBySide     = sideBySide,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent(onOpenSettings: () -> Unit) {
    // If the check is still loading after a few seconds (e.g. an unreachable
    // host that hangs rather than failing fast), reveal a way out instead of
    // leaving the user stuck on a bare spinner with no escape.
    var showEscapeHatch by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(5000)
        showEscapeHatch = true
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(UiTags.STATUS_LOADING),
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.status_connecting), style = MaterialTheme.typography.bodyLarge)
        if (showEscapeHatch) {
            Spacer(Modifier.height(24.dp))
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag(UiTags.STATUS_OPEN_SETTINGS),
            ) {
                Text(stringResource(Res.string.status_open_settings))
            }
        }
    }
}

/**
 * The hero at the top of a connected or limited screen: an icon in a tinted
 * circle, the title, and a line of small print under it.
 */
@Composable
private fun StatusHeading(
    icon: ImageVector,
    circle: Color,
    tint: Color,
    title: String,
    subtitle: String?,
) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(m.hero.circle)
                .clip(CircleShape)
                .background(circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(m.hero.glyph),
            )
        }
        Spacer(Modifier.height(m.hero.gapAfterIcon))
        Text(
            text = title,
            fontSize = m.hero.title,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.025).em,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = m.hero.subtitle,
                color = colors.muted,
            )
        }
    }
}

/**
 * @param sideBySide The permissions and the content summary as two cards in a
 *   row rather than one under the other. Both cards are the same composables
 *   either way; the row only appears where there is room for it.
 */
@Composable
private fun AllGoodContent(
    permissions: DevicePermissions,
    serverVersion: String?,
    bibles: List<String>,
    songbooks: List<String>,
    endpointAvailable: Boolean,
    onContinue: () -> Unit,
    sideBySide: Boolean,
) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    val hasContent = bibles.isNotEmpty() || songbooks.isNotEmpty()
    val scroll = rememberScrollState()
    // The scroll — and its thumb — span the window; only the content is capped,
    // so the thumb sits at the screen's edge rather than over the cards.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTags.STATUS_ALL_GOOD)
            .verticalScrollbar(scroll)
            .verticalScroll(scroll),
    ) {
        CappedColumn {
            Spacer(Modifier.height(24.dp))
            StatusHeading(
                icon = Icons.Filled.Check,
                circle = colors.accentTint,
                tint = colors.accent,
                title = stringResource(Res.string.status_connected),
                subtitle = stringResource(Res.string.status_version_line, serverVersion ?: "—", appVersion),
            )
            if (!endpointAvailable) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.status_endpoint_unavailable),
                    fontSize = m.hero.subtitle,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(m.hero.gapAfter))
            if (sideBySide) {
                Row(
                    modifier = Modifier.widthIn(max = StatusSummaryMaxWidth).height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    PermissionsSummaryCard(permissions, modifier = Modifier.weight(1f).fillMaxHeight())
                    if (hasContent) {
                        ContentSummaryCard(
                            bibles = bibles,
                            songbooks = songbooks,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                // The design's tablet Continue is the accent-filled bar the warnings
                // screen already uses, rather than the phone's outlined one.
                PrimaryButton(
                    label = stringResource(Res.string.status_continue),
                    icon = Icons.Filled.CheckCircle,
                    onClick = onContinue,
                    modifier = Modifier.widthIn(max = StatusSummaryMaxWidth).testTag(UiTags.STATUS_CONTINUE),
                )
            } else {
                PermissionsSummaryCard(permissions)
                if (hasContent) {
                    Spacer(Modifier.height(12.dp))
                    ContentSummaryCard(bibles = bibles, songbooks = songbooks)
                }
                Spacer(Modifier.height(20.dp))
                OutlineActionButton(
                    modifier = Modifier.testTag(UiTags.STATUS_CONTINUE),
                    label = stringResource(Res.string.status_continue),
                    icon = Icons.Filled.CheckCircle,
                    onClick = onContinue,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * The readable column every state's content sits in, centred in whatever is
 * wider. Capped on every window, not only a tablet's: a phone is narrower than
 * the cap, so nothing changes there, and a wide browser window gets a column
 * instead of cards stretched across it.
 */
@Composable
private fun CappedColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = StatusContentMaxWidth),
        content = content,
    )
}

@Composable
private fun ContentSummaryCard(
    bibles: List<String>,
    songbooks: List<String>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    SummaryCard(modifier) {
        if (bibles.isNotEmpty()) {
            Text(
                stringResource(Res.string.status_info_bibles),
                fontSize = m.card.title, fontWeight = FontWeight.SemiBold, color = colors.text,
            )
            Spacer(Modifier.height(m.card.titleGap))
            bibles.forEach { Text(it, fontSize = m.row.text, color = colors.muted, lineHeight = m.row.lineHeight) }
        }
        if (songbooks.isNotEmpty()) {
            if (bibles.isNotEmpty()) Spacer(Modifier.height(m.card.groupGap))
            Text(
                stringResource(Res.string.status_info_songbooks),
                fontSize = m.card.title, fontWeight = FontWeight.SemiBold, color = colors.text,
            )
            Spacer(Modifier.height(m.card.titleGap))
            songbooks.forEach { Text(it, fontSize = m.row.text, color = colors.muted, lineHeight = m.row.lineHeight) }
        }
    }
}

/** The bordered surface both summary cards sit on. */
@Composable
private fun SummaryCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    val shape = RoundedCornerShape(m.card.radius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, shape)
            .padding(horizontal = m.card.paddingHorizontal, vertical = m.card.paddingVertical),
        content = content,
    )
}

/**
 * @param sideBySide Retry and Open Settings as a pair on one row, capped at
 *   [StatusActionsMaxWidth], rather than stacked full-width. Two full-width
 *   buttons across a tablet are a metre of green with a word in the middle.
 */
@Composable
private fun ErrorContent(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
    sideBySide: Boolean,
) {
    val m = LocalStatusMetrics.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = StatusContentMaxWidth).testTag(UiTags.STATUS_ERROR),
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(m.hero.errorGlyph),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = m.hero.title,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = m.hero.subtitle,
            lineHeight = m.row.lineHeight,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = StatusActionsMaxWidth),
        )
        Spacer(Modifier.height(24.dp))
        val retry: @Composable (Modifier) -> Unit = { modifier ->
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(m.button.radius),
                modifier = modifier.height(m.button.height).testTag(UiTags.STATUS_RETRY),
            ) {
                Text(stringResource(Res.string.status_retry), fontSize = m.button.text)
            }
        }
        val openSettings: @Composable (Modifier) -> Unit = { modifier ->
            OutlinedButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(m.button.radius),
                modifier = modifier.height(m.button.height).testTag(UiTags.STATUS_OPEN_SETTINGS),
            ) {
                Text(stringResource(Res.string.status_open_settings), fontSize = m.button.text)
            }
        }
        if (sideBySide) {
            Row(
                modifier = Modifier.widthIn(max = StatusActionsMaxWidth),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                retry(Modifier.weight(1f))
                openSettings(Modifier.weight(1f))
            }
        } else {
            retry(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            openSettings(Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onContinue,
            modifier = Modifier.testTag(UiTags.STATUS_CONTINUE),
        ) {
            Text(stringResource(Res.string.status_continue_anyway), fontSize = m.button.text)
        }
    }
}

/**
 * @param sideBySide The permissions card and the buttons in a fixed-width
 *   column on the left, the issues in a two-column grid filling the right —
 *   rather than everything in one stack. The same cards; only the arrangement
 *   and the number of columns change.
 */
@Composable
private fun WarningsContent(
    appVersion: String?,
    warnings: List<StatusWarning>,
    permissions: DevicePermissions,
    bibles: List<String>,
    songbooks: List<String>,
    features: List<String>,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
    sideBySide: Boolean,
) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTags.STATUS_WARNINGS)
            .verticalScrollbar(scroll)
            .verticalScroll(scroll),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CappedColumn {
            Spacer(Modifier.height(16.dp))
            StatusHeading(
                icon = Icons.Filled.Warning,
                circle = colors.warningTint,
                tint = colors.warning,
                title = stringResource(Res.string.status_limited_functionality),
                subtitle = appVersion?.let { stringResource(Res.string.status_server_version, it) },
            )
            Spacer(Modifier.height(m.hero.gapAfter))
            if (sideBySide) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    Column(modifier = Modifier.width(StatusPermissionsPaneWidth)) {
                        PermissionsSummaryCard(permissions)
                        Spacer(Modifier.height(20.dp))
                        WarningsActions(onContinue = onContinue, onOpenSettings = onOpenSettings)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        WarningsList(warnings, bibles, songbooks, features, columns = 2)
                    }
                }
            } else {
                PermissionsSummaryCard(permissions)
                Spacer(Modifier.height(20.dp))
                WarningsList(warnings, bibles, songbooks, features, columns = 1)
                Spacer(Modifier.height(16.dp))
                WarningsActions(onContinue = onContinue, onOpenSettings = onOpenSettings)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * The "issues detected" line, one card per warning, and the informational
 * cards after them.
 *
 * @param columns How many warning cards share a row. One on a phone; two in the
 *   tablet's right-hand pane, where the cards in a row are stretched to the
 *   same height so the grid reads as a grid.
 */
@Composable
private fun WarningsList(
    warnings: List<StatusWarning>,
    bibles: List<String>,
    songbooks: List<String>,
    features: List<String>,
    columns: Int,
) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    Text(
        text = stringResource(Res.string.status_issues_detected),
        fontSize = m.row.text,
        textAlign = if (columns > 1) TextAlign.Start else TextAlign.Center,
        color = colors.muted,
        lineHeight = m.row.lineHeight,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    warnings.chunked(columns).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(m.noticeGap),
        ) {
            // A lone last card takes the whole row, like the cards above it.
            row.forEach { warning -> WarningCard(warning, modifier = Modifier.weight(1f).fillMaxHeight()) }
        }
        Spacer(Modifier.height(m.noticeGap))
    }
    if (bibles.isNotEmpty()) {
        InfoCard(
            icon  = Icons.Filled.Info,
            title = stringResource(Res.string.status_info_bibles),
            body  = bibles.joinToString(", "),
        )
        Spacer(Modifier.height(m.noticeGap))
    }
    if (songbooks.isNotEmpty()) {
        InfoCard(
            icon  = Icons.Filled.Info,
            title = stringResource(Res.string.status_info_songbooks),
            body  = songbooks.joinToString(", "),
        )
        Spacer(Modifier.height(m.noticeGap))
    }
    if (features.isNotEmpty()) {
        InfoCard(
            icon  = Icons.Filled.Info,
            title = stringResource(Res.string.status_info_features),
            body  = features.joinToString(", "),
        )
        Spacer(Modifier.height(m.noticeGap))
    }
}

/** Primary Continue + subtle Open Settings, in the redesign palette. */
@Composable
private fun WarningsActions(onContinue: () -> Unit, onOpenSettings: () -> Unit) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    PrimaryButton(
        label = stringResource(Res.string.status_continue),
        onClick = onContinue,
        modifier = Modifier.testTag(UiTags.STATUS_CONTINUE),
    )
    Spacer(Modifier.height(if (m.button.outlineQuiet) 12.dp else 6.dp))
    val shape = RoundedCornerShape(m.button.radius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(m.button.height)
            .testTag(UiTags.STATUS_OPEN_SETTINGS)
            .clip(shape)
            .then(if (m.button.outlineQuiet) Modifier.border(1.dp, colors.borderSubtle, shape) else Modifier)
            .clickable(onClick = onOpenSettings),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(Res.string.status_open_settings),
            color = if (m.button.outlineQuiet) colors.text else colors.muted,
            fontSize = m.button.text,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** The accent-filled bar that is this screen's primary action. */
@Composable
private fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(m.button.height)
            .clip(RoundedCornerShape(m.button.radius))
            .background(colors.accent)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(m.row.icon))
        }
        Text(label, color = colors.onAccent, fontSize = m.button.text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WarningCard(warning: StatusWarning, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val (icon, title, body) = warningDetails(warning)
    NoticeCard(icon = icon, title = title, body = body, tint = colors.danger, modifier = modifier)
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String) {
    val colors = LocalAppColors.current
    NoticeCard(icon = icon, title = title, body = body, tint = colors.amber)
}

/** Line height of a notice's body, as a multiple of its type size — the design's 1.55. */
private const val NOTICE_LINE_HEIGHT = 1.55f

/** A tinted, bordered notice: an icon, a bold title, and a line or two under it. */
@Composable
private fun NoticeCard(
    icon: ImageVector,
    title: String,
    body: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    val shape = RoundedCornerShape(m.notice.radius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.30f), shape)
            .padding(m.notice.padding),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(m.notice.icon),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = colors.text, fontSize = m.notice.title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(m.notice.titleGap))
            Text(body, color = colors.muted, fontSize = m.notice.body, lineHeight = m.notice.body * NOTICE_LINE_HEIGHT)
        }
    }
}

@Composable
private fun PermissionsSummaryCard(permissions: DevicePermissions, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    SummaryCard(modifier) {
        Text(
            text = stringResource(Res.string.status_permissions_title),
            fontSize = m.card.title,
            fontWeight = FontWeight.SemiBold,
            color = colors.text,
        )
        Spacer(Modifier.height(m.card.titleGap))
        PermissionRow(label = stringResource(Res.string.status_permission_present),  granted = permissions.canPresent)
        Spacer(Modifier.height(m.row.gap))
        PermissionRow(
            label = stringResource(Res.string.status_permission_schedule),
            granted = permissions.canAddToSchedule,
        )
        Spacer(Modifier.height(m.row.gap))
        PermissionRow(
            label = stringResource(Res.string.status_permission_upload),
            granted = permissions.canUploadFiles,
        )
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (granted) colors.accent else colors.danger,
            modifier = Modifier.size(m.row.icon),
        )
        Spacer(Modifier.width(m.row.iconGap))
        Text(
            text = label,
            fontSize = m.row.text,
            color = if (granted) colors.text else colors.danger,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (granted) "true" else "false",
            fontSize = m.row.value,
            fontWeight = FontWeight.SemiBold,
            color = if (granted) colors.accent else colors.danger,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Warning metadata — resolved in composable scope so stringResource() works
// ─────────────────────────────────────────────────────────────────────────────

/** The words and icon one [StatusWarning] is shown with. */
internal data class WarningDetails(val icon: ImageVector, val title: String, val body: String)

/**
 * Resolves a [StatusWarning] to what the operator reads.
 *
 * `internal` because each warning sends someone somewhere different — to the
 * desktop's permissions, to its Bible import, to its version — and a warning
 * that resolves to the wrong words sends them to the wrong place. Eight
 * branches, none of them reachable from a screen test without eight different
 * server responses.
 */
@Composable
internal fun warningDetails(warning: StatusWarning): WarningDetails = when (warning) {
    is StatusWarning.NoApiKey -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_no_api_key_title),
        body  = stringResource(Res.string.status_warn_no_api_key_body),
    )
    is StatusWarning.NoBibles -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_no_bibles_title),
        body  = stringResource(Res.string.status_warn_no_bibles_body),
    )
    is StatusWarning.NoSongbooks -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_no_songbooks_title),
        body  = stringResource(Res.string.status_warn_no_songbooks_body),
    )
    is StatusWarning.PresentBlocked -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_present_blocked_title),
        body  = stringResource(Res.string.status_warn_present_blocked_body),
    )
    is StatusWarning.ScheduleBlocked -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_schedule_blocked_title),
        body  = stringResource(Res.string.status_warn_schedule_blocked_body),
    )
    is StatusWarning.UploadBlocked -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_upload_blocked_title),
        body  = stringResource(Res.string.status_warn_upload_blocked_body),
    )
    is StatusWarning.UnknownVersion -> WarningDetails(
        icon  = Icons.Filled.Info,
        title = stringResource(Res.string.status_warn_unknown_version_title),
        body  = stringResource(Res.string.status_warn_unknown_version_body, warning.version ?: "unknown"),
    )
    is StatusWarning.MissingEndpoint -> WarningDetails(
        icon  = Icons.Filled.Warning,
        title = stringResource(Res.string.status_warn_missing_endpoint_title, warning.endpoint),
        body  = stringResource(Res.string.status_warn_missing_endpoint_body, warning.endpoint),
    )
}
