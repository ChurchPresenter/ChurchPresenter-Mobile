package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextReplacement
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.network.WsSender
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.math.abs

/**
 * Shared setup for the Library screen's tests.
 *
 * Note for anyone extending these: compose-resources does not resolve in the
 * wasmJs test runtime, so every `stringResource` renders empty. Song and notice
 * titles are plain data and do render, which is what the assertions key on; the
 * buttons beside them cannot be selected by their labels, only by where they
 * sit.
 */
internal fun song(id: String, number: String, title: String) =
    LocalSong(id = id, number = number, title = title)

internal fun repositoryWith(
    songs: List<LocalSong> = emptyList(),
    announcements: List<LocalAnnouncement> = emptyList(),
) = LibraryRepository(InMemoryFileStorage()).apply {
    songs.forEach { upsertSong(it) }
    announcements.forEach { upsertAnnouncement(it) }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showLibrary(
    repository: LibraryRepository,
    onEditSong: (String?) -> Unit = {},
    onEditAnnouncement: (String?) -> Unit = {},
    sender: WsSender = FakeWsSender(),
) {
    setContent {
        AppTheme(themeMode = ThemeMode.DARK) {
            LibraryScreen(
                repository = repository,
                bibles = LocalBibleRepository(InMemoryFileStorage()),
                settings = AppSettings(InMemorySettingsStorage()),
                sender = sender,
                onEditSong = onEditSong,
                onEditAnnouncement = onEditAnnouncement,
            )
        }
    }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.isShowing(text: String): Boolean =
    onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.clickableCount(): Int =
    onAllNodes(hasClickAction()).fetchSemanticsNodes().size

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.search(query: String) {
    onNode(hasSetTextAction()).performTextReplacement(query)
}

/**
 * The buttons on the row showing [title] — Delete then Edit, left to right.
 *
 * Found by where they are rather than by what they say: their labels come from
 * compose-resources, which does not resolve in this runtime, so the row is
 * located by its title and the clickables on the same line are its actions.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.actionsOnRow(title: String): List<SemanticsNodeInteraction> {
    val row = onAllNodes(hasText(title, substring = true)).fetchSemanticsNodes().first().boundsInRoot
    val rowCentre = (row.top + row.bottom) / 2f
    val clickables = onAllNodes(hasClickAction())
    return clickables.fetchSemanticsNodes()
        .mapIndexedNotNull { index, node ->
            val centre = (node.boundsInRoot.top + node.boundsInRoot.bottom) / 2f
            index.takeIf { abs(centre - rowCentre) < SAME_ROW_TOLERANCE_PX }
        }
        .map { clickables[it] }
}

/** Half a row's height in pixels — enough to tell one row from the next. */
private const val SAME_ROW_TOLERANCE_PX = 30f
