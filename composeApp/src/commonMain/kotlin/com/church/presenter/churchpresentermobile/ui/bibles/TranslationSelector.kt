package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.bibles_cd_choose_translation
import churchpresentermobile.composeapp.generated.resources.bibles_empty_body
import churchpresentermobile.composeapp.generated.resources.bibles_empty_title
import churchpresentermobile.composeapp.generated.resources.bibles_get
import churchpresentermobile.composeapp.generated.resources.bibles_get_more
import churchpresentermobile.composeapp.generated.resources.bibles_manage
import churchpresentermobile.composeapp.generated.resources.bibles_on_this_device
import churchpresentermobile.composeapp.generated.resources.bibles_selector_desktop
import churchpresentermobile.composeapp.generated.resources.bibles_selector_desktop_detail
import churchpresentermobile.composeapp.generated.resources.bibles_selector_installed
import churchpresentermobile.composeapp.generated.resources.bibles_sheet_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * The translation selector above the books list — design 1a / 1g.
 *
 * Shows the translation being read and how many are on the device; tapping it opens the list of
 * them (a sheet on a phone, a popover on a tablet). With nothing installed — a phone reading its
 * desktop's Bible in remote mode — it says so, and still opens, since "Get Bibles" is inside.
 *
 * @param open True while the sheet or popover is up, which is when the design draws the outline.
 */
@Composable
internal fun TranslationSelector(
    active: InstalledBible?,
    installedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    open: Boolean = false,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    val description = stringResource(Res.string.bibles_cd_choose_translation)
    Row(
        modifier = modifier
            .testTag(BiblesTags.SELECTOR)
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(colors.cardFill)
            .border(1.dp, if (open) colors.accent else colors.cardEdge, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(18.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = active?.title ?: stringResource(Res.string.bibles_selector_desktop),
                color = colors.text,
                fontSize = 14.sp,
                // The design's 1.1 line height: Compose's default leaves two lines too tall for 44.
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (active != null) {
                    stringResource(
                        Res.string.bibles_selector_installed,
                        active.abbreviation.ifBlank { active.coverage },
                        installedCount,
                    )
                } else {
                    stringResource(Res.string.bibles_selector_desktop_detail)
                },
                color = colors.selectorSubtle,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MonoFamily,
                maxLines = 1,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.selectorSubtle,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * The installed translations and the way to get more — design 1b as a sheet on a phone.
 *
 * @param onManage Null hides "Manage" — on the web, where nothing can be installed or removed.
 * @param onGetMore Null hides "Get more Bibles" for the same reason.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TranslationSheet(
    installed: List<InstalledBible>,
    activeId: String,
    onChoose: (String) -> Unit,
    onManage: (() -> Unit)?,
    onGetMore: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.sheetBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        TranslationList(
            installed = installed,
            activeId = activeId,
            onChoose = onChoose,
            onManage = onManage,
            onGetMore = onGetMore,
            showHeader = true,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 26.dp).navigationBarsPadding(),
        )
    }
}

/**
 * The same list as [TranslationSheet], floating under the selector — design 1g, on a tablet.
 * 360 wide, anchored below the selector at the top of the books pane.
 */
@Composable
internal fun TranslationPopover(
    installed: List<InstalledBible>,
    activeId: String,
    onChoose: (String) -> Unit,
    onManage: (() -> Unit)?,
    onGetMore: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            modifier = Modifier
                .padding(top = POPOVER_TOP)
                .width(360.dp)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(colors.sheetBackground)
                .border(1.dp, colors.cardEdge, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            TranslationList(
                installed = installed,
                activeId = activeId,
                onChoose = onChoose,
                onManage = onManage,
                onGetMore = onGetMore,
                showHeader = false,
                compact = true,
            )
        }
    }
}

/** Just below the 44-tall selector the popover is anchored to, with design 1g's 6 gap. */
private val POPOVER_TOP = 50.dp

/**
 * The sheet's and the popover's content. `internal` rather than private so a UI test can compose
 * it without the modal sheet around it, which is a window of its own.
 */
@Composable
internal fun TranslationList(
    installed: List<InstalledBible>,
    activeId: String,
    onChoose: (String) -> Unit,
    onManage: (() -> Unit)?,
    onGetMore: (() -> Unit)?,
    showHeader: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = LocalAppColors.current
    Column(modifier.testTag(BiblesTags.SHEET)) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.bibles_sheet_title),
                    color = colors.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.025).em,
                )
                if (onManage != null) {
                    Text(
                        text = stringResource(Res.string.bibles_manage),
                        color = colors.selectorSubtle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag(BiblesTags.MANAGE).clickable(onClick = onManage),
                    )
                }
            }
        }
        if (installed.isNotEmpty()) {
            Overline(
                stringResource(Res.string.bibles_on_this_device),
                color = colors.selectorSubtle,
                modifier = Modifier.padding(
                    start = 4.dp,
                    end = 4.dp,
                    top = if (compact) 2.dp else 4.dp,
                    bottom = if (compact) 10.dp else 8.dp,
                ),
            )
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
            ) {
                installed.forEach { bible ->
                    TranslationRow(bible, bible.id == activeId, compact) { onChoose(bible.id) }
                }
            }
            Box(
                Modifier.padding(
                    vertical = if (compact) 11.dp else 16.dp,
                ).fillMaxWidth().height(1.dp).background(colors.borderSubtle),
            )
        }
        if (onGetMore != null) {
            BiblesButton(
                label = stringResource(Res.string.bibles_get_more),
                onClick = onGetMore,
                icon = Icons.Filled.Download,
                height = if (compact) 46.dp else 48.dp,
                tag = BiblesTags.GET_MORE,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TranslationRow(bible: InstalledBible, chosen: Boolean, compact: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(if (compact) 12.dp else 13.dp)
    val fill = when {
        chosen -> colors.chosenFill
        compact -> androidx.compose.ui.graphics.Color.Transparent
        else -> colors.cardFill
    }
    val edge = when {
        compact -> androidx.compose.ui.graphics.Color.Transparent
        chosen -> colors.chosenEdge
        else -> colors.cardEdge
    }
    // The tablet popover drops the language: it is narrower, and the book list beside it says it.
    val detail = if (compact) {
        listOf(bible.abbreviation, bible.coverage)
    } else {
        listOf(bible.abbreviation, bible.languageName, bible.coverage)
    }.filter { it.isNotBlank() }.joinToString(" · ")
    Row(
        modifier = Modifier
            .testTag(BiblesTags.translation(bible.id))
            .semantics { selected = chosen }
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 12.dp else 14.dp, vertical = if (compact) 12.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = if (chosen) colors.accent else colors.muted,
            modifier = Modifier.size(18.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                bible.title,
                color = colors.text,
                fontSize = 15.sp,
                fontWeight = if (chosen) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isNotEmpty()) {
                Text(
                    detail,
                    color = colors.selectorSubtle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MonoFamily,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
        if (chosen) Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * Nothing to read, and a way to fix it — design 1c / 1h.
 *
 * @param onGetBibles Null on the web, where the app cannot download; [fallbackAction] is offered
 *   instead (copying a Bible from the desktop, as the tab offered before).
 */
@Composable
internal fun NoBibleInstalled(
    onGetBibles: (() -> Unit)?,
    modifier: Modifier = Modifier,
    fallbackAction: (@Composable () -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier.testTag(BiblesTags.EMPTY).fillMaxSize().background(colors.background).padding(
            horizontal = 32.dp,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.chosenFill)
                .border(1.dp, colors.chosenEdge, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(Res.string.bibles_empty_title),
            color = colors.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.03).em,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.bibles_empty_body),
            color = colors.secondary,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp).widthIn360(),
        )
        Spacer(Modifier.height(28.dp))
        if (onGetBibles != null) {
            BiblesButton(
                label = stringResource(Res.string.bibles_get),
                onClick = onGetBibles,
                icon = Icons.Filled.Download,
                height = 52.dp,
                tag = BiblesTags.EMPTY_GET,
                modifier = Modifier.fillMaxWidth().widthIn360(),
            )
        } else {
            fallbackAction?.invoke()
        }
    }
}

/** The empty state's text and button stop at the width the tablet design gives them (1h). */
private fun Modifier.widthIn360(): Modifier = this.widthIn(max = EMPTY_MAX_WIDTH)

private val EMPTY_MAX_WIDTH = 306.dp
