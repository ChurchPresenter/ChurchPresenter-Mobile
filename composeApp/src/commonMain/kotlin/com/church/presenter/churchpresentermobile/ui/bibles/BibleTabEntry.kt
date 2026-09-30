package com.church.presenter.churchpresentermobile.ui.bibles

import com.church.presenter.churchpresentermobile.ui.OutlineActionButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.viewmodel.BibleChoiceViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.SyncRequestHandler
import com.church.presenter.churchpresentermobile.TabNavigationHandler
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.ui.library.SyncSection
import androidx.compose.material.icons.filled.CloudDownload
import churchpresentermobile.composeapp.generated.resources.empty_action_get_bible
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

/**
 * The web build's way to a first Bible, which cannot download one: copy it from the desktop.
 * The Library tab owns that sheet, so ask it to open on the Bible half.
 */
@Composable
internal fun CopyFromDesktopButton() {
    OutlineActionButton(
        label = stringResource(Res.string.empty_action_get_bible),
        icon = Icons.Filled.CloudDownload,
        onClick = {
            SyncRequestHandler.request(SyncSection.BIBLE)
            TabNavigationHandler.navigateTo(AppTab.LIBRARY)
        },
    )
}

/**
 * The translation selector and what it opens — design 1a/1b on a phone, 1g on a tablet.
 *
 * Owns the [BibleChoiceViewModel] it reads, as every composable here owns its own.
 */
@Composable
internal fun BibleTranslationPicker(
    bibles: LocalBibleRepository,
    twoPane: Boolean,
    onOpenBibles: ((BiblesPage) -> Unit)?,
) {
    val choice: BibleChoiceViewModel = viewModel(key = "bible_tab_choice") { BibleChoiceViewModel(bibles) }
    val installed by choice.installed.collectAsState()
    val active by choice.active.collectAsState()
    val activeId by choice.activeId.collectAsState()
    // With nothing installed and nowhere to get one, the selector would offer nothing.
    if (installed.isEmpty() && onOpenBibles == null) return
    var open by remember { mutableStateOf(false) }
    val choose: (String) -> Unit = { id -> choice.setActive(id); open = false }
    val manage = onOpenBibles?.let { go -> { open = false; go(BiblesPage.INSTALLED) } }
    val getMore = onOpenBibles?.let { go -> { open = false; go(BiblesPage.GET) } }
    Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
        TranslationSelector(active, installed.size, onClick = { open = true }, open = open)
        if (open && twoPane) {
            TranslationPopover(installed, activeId, choose, manage, getMore, onDismiss = { open = false })
        }
    }
    if (open && !twoPane) {
        TranslationSheet(installed, activeId, choose, manage, getMore, onDismiss = { open = false })
    }
}

/**
 * The Bible tab with nothing to read — design 1c / 1h. Where Bibles can be downloaded that is the
 * way offered; on the web, which cannot download one, it is copying one from the desktop.
 */
@Composable
internal fun BibleTabEmpty(onOpenBibles: ((BiblesPage) -> Unit)?, modifier: Modifier = Modifier) {
    NoBibleInstalled(
        onGetBibles = onOpenBibles?.let { open -> { open(BiblesPage.GET) } },
        modifier = modifier,
        fallbackAction = { CopyFromDesktopButton() },
    )
}
