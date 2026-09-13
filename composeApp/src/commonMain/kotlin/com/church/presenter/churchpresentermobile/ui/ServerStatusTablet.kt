package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.settings_status_address_label
import churchpresentermobile.composeapp.generated.resources.settings_status_bibles
import churchpresentermobile.composeapp.generated.resources.settings_status_none
import churchpresentermobile.composeapp.generated.resources.settings_status_recheck
import churchpresentermobile.composeapp.generated.resources.settings_status_songbooks
import churchpresentermobile.composeapp.generated.resources.status_connected
import churchpresentermobile.composeapp.generated.resources.status_connecting
import churchpresentermobile.composeapp.generated.resources.status_error_title
import churchpresentermobile.composeapp.generated.resources.status_issues_detected
import churchpresentermobile.composeapp.generated.resources.status_limited_functionality
import churchpresentermobile.composeapp.generated.resources.status_not_churchpresenter_body
import churchpresentermobile.composeapp.generated.resources.status_not_churchpresenter_title
import churchpresentermobile.composeapp.generated.resources.status_permission_present
import churchpresentermobile.composeapp.generated.resources.status_permission_schedule
import churchpresentermobile.composeapp.generated.resources.status_permission_upload
import churchpresentermobile.composeapp.generated.resources.status_permissions_title
import churchpresentermobile.composeapp.generated.resources.status_unauthorized_body
import churchpresentermobile.composeapp.generated.resources.status_unauthorized_title
import churchpresentermobile.composeapp.generated.resources.status_version_line
import com.church.presenter.churchpresentermobile.model.DevicePermissions
import com.church.presenter.churchpresentermobile.model.StatusWarning
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.util.appVersion
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import org.jetbrains.compose.resources.stringResource

/** Fixed width of the permissions-and-content column beside the issue grid. */
private val StatusModalSideWidth = 420.dp

/** The permissions card is a little wider than the content card beside it, as the design draws them. */
private const val PERMISSIONS_WEIGHT = 1.15f

/** The modal's cards are rounder and roomier than a settings page's. */
private val MODAL_CARD_RADIUS = 20.dp
private val ModalCardPadding = PaddingValues(horizontal = 26.dp, vertical = 22.dp)

/**
 * The Check Server Status modal's body, laid out across a tablet.
 *
 * The phone stacks one card under the next; here a connected desktop's
 * permissions and content sit side by side, a limited one's issues fill a
 * two-column grid beside them, and a failure is one centred message with the
 * address it tried. Recheck stays at the foot whichever state is showing.
 *
 * Drawn at the startup status screen's tablet sizes ([StatusMetrics.Tablet]),
 * since the two screens show the same cards and the design draws them alike.
 */
@Composable
internal fun ServerStatusTablet(
    state: StatusUiState,
    address: String,
    onRecheck: () -> Unit,
) {
    CompositionLocalProvider(LocalStatusMetrics provides StatusMetrics.Tablet) {
        val scroll = rememberScrollState()
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 26.dp)) {
            // The verdict scrolls if it must; Recheck stays put at the foot.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScrollbar(scroll)
                    .verticalScroll(scroll)
                    // Room for the thumb beside the cards rather than over them.
                    .padding(end = 12.dp),
            ) {
                when (state) {
                    is StatusUiState.Loading -> Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 120.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.testTag(UiTags.STATUS_DIALOG_LOADING))
                        Text(
                            stringResource(Res.string.status_connecting),
                            color = LocalAppColors.current.muted, fontSize = 18.sp,
                        )
                    }
                    is StatusUiState.Error -> Failure(
                        icon = Icons.Filled.Warning,
                        title = stringResource(Res.string.status_error_title),
                        body = state.message,
                        address = address,
                        tag = UiTags.STATUS_DIALOG_ERROR,
                    )
                    is StatusUiState.Unauthorized -> Failure(
                        icon = Icons.Filled.Lock,
                        title = stringResource(Res.string.status_unauthorized_title),
                        body = stringResource(Res.string.status_unauthorized_body),
                        address = address,
                        tag = UiTags.STATUS_DIALOG_UNAUTHORIZED,
                    )
                    is StatusUiState.NotChurchPresenter -> Failure(
                        icon = Icons.Filled.Warning,
                        title = stringResource(Res.string.status_not_churchpresenter_title),
                        body = stringResource(Res.string.status_not_churchpresenter_body),
                        address = address,
                        tag = UiTags.STATUS_DIALOG_NOT_CHURCHPRESENTER,
                    )
                    is StatusUiState.Success -> Answered(state)
                }
            }
            Spacer(Modifier.height(18.dp))
            RecheckBar(onRecheck)
        }
    }
}

/** A desktop that answered: the banner, then permissions beside content, then the issues if any. */
@Composable
private fun Answered(state: StatusUiState.Success) {
    val colors = LocalAppColors.current
    val status = state.status
    val warnings = state.warnings
    val limited = warnings.isNotEmpty()
    Column {
        Banner(
            icon = if (limited) Icons.Filled.Warning else Icons.Filled.CheckCircle,
            tint = if (limited) colors.warning else colors.accent,
            title = stringResource(
                if (limited) Res.string.status_limited_functionality else Res.string.status_connected,
            ),
            subtitle = stringResource(Res.string.status_version_line, status.appVersion ?: "—", appVersion),
        )
        Spacer(Modifier.height(18.dp))
        if (limited) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.width(StatusModalSideWidth),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Permissions(status.permissions)
                    Content(status.bibles, status.songbooks)
                }
                Column(modifier = Modifier.weight(1f)) { Issues(warnings) }
            }
        } else {
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Permissions(status.permissions, modifier = Modifier.weight(PERMISSIONS_WEIGHT).fillMaxHeight())
                Content(status.bibles, status.songbooks, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

/** The tinted strip across the top: an icon, the verdict, and the two versions under it. */
@Composable
private fun Banner(icon: ImageVector, tint: Color, title: String, subtitle: String) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.24f), shape)
            .padding(horizontal = 26.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(40.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                color = colors.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.03).em,
                modifier = Modifier.testTag(UiTags.STATUS_DIALOG_CONNECTED),
            )
            Text(
                text = subtitle,
                color = colors.muted,
                fontSize = 16.sp,
                modifier = Modifier.testTag(UiTags.STATUS_DIALOG_SERVER_VERSION),
            )
        }
    }
}

@Composable
private fun Permissions(permissions: DevicePermissions, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    SettingsCard(
        modifier = modifier.testTag(UiTags.STATUS_DIALOG_PERMISSIONS),
        radius = MODAL_CARD_RADIUS,
        padding = ModalCardPadding,
    ) {
        Text(
            stringResource(Res.string.status_permissions_title),
            color = colors.text, fontSize = m.card.title, fontWeight = FontWeight.Bold,
        )
        PermissionLine(stringResource(Res.string.status_permission_present), permissions.canPresent)
        HorizontalDivider(color = colors.borderSubtle)
        PermissionLine(stringResource(Res.string.status_permission_schedule), permissions.canAddToSchedule)
        HorizontalDivider(color = colors.borderSubtle)
        PermissionLine(stringResource(Res.string.status_permission_upload), permissions.canUploadFiles, last = true)
    }
}

@Composable
private fun PermissionLine(label: String, granted: Boolean, last: Boolean = false) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 15.dp, bottom = if (last) 0.dp else 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            fontWeight = FontWeight.Bold,
            color = if (granted) colors.accent else colors.danger,
        )
    }
}

@Composable
private fun Content(bibles: List<String>, songbooks: List<String>, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val m = LocalStatusMetrics.current
    val none = stringResource(Res.string.settings_status_none)
    SettingsCard(modifier = modifier, radius = MODAL_CARD_RADIUS, padding = ModalCardPadding) {
        Text(
            stringResource(Res.string.settings_status_bibles),
            color = colors.text, fontSize = m.card.title, fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (bibles.isEmpty()) none else bibles.joinToString("\n") { "• $it" },
            color = if (bibles.isEmpty()) colors.muted else colors.text,
            fontSize = m.row.text,
            lineHeight = m.row.lineHeight,
            modifier = Modifier.padding(top = 8.dp).testTag(UiTags.STATUS_DIALOG_BIBLES),
        )
        Text(
            stringResource(Res.string.settings_status_songbooks),
            color = colors.text, fontSize = m.card.title, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = if (songbooks.isEmpty()) none else songbooks.joinToString("\n") { "• $it" },
            color = if (songbooks.isEmpty()) colors.muted else colors.text,
            fontSize = m.row.text,
            lineHeight = m.row.lineHeight,
            modifier = Modifier.padding(top = 8.dp).testTag(UiTags.STATUS_DIALOG_SONGBOOKS),
        )
    }
}

/** "Issues detected", the count, and the warnings two to a row. */
@Composable
private fun Issues(warnings: List<StatusWarning>) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.testTag(UiTags.STATUS_DIALOG_WARNINGS)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.status_issues_detected),
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = warnings.size.toString(),
                color = colors.danger,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        warnings.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // A lone last card takes the whole row, like the cards above it.
                row.forEach { warning -> IssueCard(warning, modifier = Modifier.weight(1f).fillMaxHeight()) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** One warning: its icon and title, its code in small monospace under them, and the explanation. */
@Composable
private fun IssueCard(warning: StatusWarning, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val (icon, title, body) = warningDetails(warning)
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.danger.copy(alpha = 0.07f))
            .border(1.dp, colors.danger.copy(alpha = 0.26f), shape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = colors.danger, modifier = Modifier.size(19.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    title,
                    color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp, letterSpacing = (-0.02).em,
                )
                Text(
                    warning::class.simpleName ?: warning.toString(),
                    color = colors.danger, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace, letterSpacing = 0.04.em,
                )
            }
        }
        Text(
            body,
            color = colors.muted, fontSize = 14.sp, lineHeight = 21.sp,
            modifier = Modifier.padding(top = 11.dp),
        )
    }
}

/** A failure: a big icon, the verdict, the explanation, and the address that was tried. */
@Composable
private fun Failure(icon: ImageVector, title: String, body: String, address: String, tag: String) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = colors.danger, modifier = Modifier.size(78.dp))
        Text(
            text = title,
            color = colors.danger,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.035).em,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 26.dp).testTag(tag),
        )
        Text(
            text = body,
            color = colors.muted,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp).widthIn(max = 620.dp),
        )
        Row(
            modifier = Modifier
                .padding(top = 28.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(Res.string.settings_status_address_label).uppercase(),
                color = colors.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.09.em,
            )
            Text(text = address, color = colors.text, fontSize = 17.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun RecheckBar(onRecheck: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, shape)
            .clickable(onClick = onRecheck)
            .testTag(UiTags.STATUS_DIALOG_RECHECK),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Refresh, contentDescription = null, tint = colors.text, modifier = Modifier.size(21.dp))
        Text(
            stringResource(Res.string.settings_status_recheck),
            color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
        )
    }
}
