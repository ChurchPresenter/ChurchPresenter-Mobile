package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.network.WsSender
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme

/**
 * Shared setup for the library screens' UI tests.
 *
 * Controls are reached by their [LibraryTags], not by position: the labels come
 * from compose-resources, which does not resolve in the wasmJs test runtime, and
 * an index into the semantics tree turns any layout change into a failure
 * somewhere unrelated. Content the operator typed — song titles, notice bodies —
 * is plain data and is matched by its text.
 */
internal fun libraryOf(
    songs: List<LocalSong> = emptyList(),
    notices: List<LocalAnnouncement> = emptyList(),
) = LibraryRepository(InMemoryFileStorage()).apply {
    songs.forEach { upsertSong(it) }
    notices.forEach { upsertAnnouncement(it) }
}

internal fun song(id: String, number: String, title: String) =
    LocalSong(id = id, number = number, title = title)

internal fun amazingGrace() = LocalSong(
    id = "s1",
    number = "42",
    title = "Amazing Grace",
    sections = listOf(LocalSongSection(SectionType.VERSE, "Amazing grace, how sweet the sound")),
)

/**
 * A translation already on the phone, for the cases where the Bible chip only
 * exists because one is.
 */
internal fun biblesWith(vararg titles: String): LocalBibleRepository {
    val repository = LocalBibleRepository(InMemoryFileStorage(), now = { 0L })
    titles.forEachIndexed { i, title ->
        repository.install(
            fileName = "bible_$i.spb",
            text = """
                ##Title: $title
                1 Genesis 50
                -----
                B001C001V001 1 1 1 In the beginning.
            """.trimIndent(),
        )
    }
    return repository
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showLibrary(
    repository: LibraryRepository,
    onEditSong: (String?) -> Unit = {},
    onEditAnnouncement: (String?) -> Unit = {},
    sender: WsSender = FakeWsSender(),
    bibles: LocalBibleRepository = LocalBibleRepository(InMemoryFileStorage()),
    settings: AppSettings = AppSettings(InMemorySettingsStorage()),
) {
    setContent {
        AppTheme(themeMode = ThemeMode.DARK) {
            LibraryScreen(
                repository = repository,
                bibles = bibles,
                settings = settings,
                sender = sender,
                onEditSong = onEditSong,
                onEditAnnouncement = onEditAnnouncement,
            )
        }
    }
}

/** The node carrying [tag], scrolling a lazy list until it exists if need be. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.tagged(tag: String): SemanticsNodeInteraction {
    if (onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isEmpty()) {
        onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
    }
    return onNodeWithTag(tag)
}

/**
 * The editable node behind [tag].
 *
 * A tag passed to `EditorField` or `SearchField` lands on the wrapper those
 * composables put their modifier on, not on the text field inside it — and only
 * the text field carries the focus and set-text actions. So look for a
 * descendant that is editable first, and fall back to the tagged node itself,
 * which is what a tag placed straight on a text field gives.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.textField(tag: String): SemanticsNodeInteraction {
    tagged(tag)
    val inside = onAllNodes(
        hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),
        useUnmergedTree = true,
    )
    return if (inside.fetchSemanticsNodes().isNotEmpty()) inside[0] else onNodeWithTag(tag)
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.type(tag: String, text: String) =
    textField(tag).performTextReplacement(text)

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.isShowing(text: String): Boolean =
    onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.exists(tag: String): Boolean =
    onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
