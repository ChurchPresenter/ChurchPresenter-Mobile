package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import kotlin.test.Test

/**
 * A section far longer than any verse — a whole file pasted or imported as one.
 *
 * A lyrics field that grows to fit its text cannot be that tall: past a couple of hundred
 * thousand pixels Compose's `Constraints` cannot represent the height, and measuring the
 * row throws, taking the app down as soon as the editor opens.
 */
@OptIn(ExperimentalTestApi::class)
class EditorLongSectionTest {

    @Test
    fun aSongWithAnEnormousSectionStillOpensAndScrollsPastIt() = runComposeUiTest {
        val enormous = List(12_000) { "line $it" }.joinToString("\n")
        val song = amazingGrace().copy(
            sections = listOf(
                LocalSongSection(SectionType.VERSE, enormous),
                LocalSongSection(SectionType.CHORUS, "Was blind but now I see"),
            ),
        )
        showSongEditor(libraryOf(songs = listOf(song)), songId = song.id)

        // The capped field scrolls too, so the list is named rather than found as "the" scroller.
        val list = onNodeWithTag(LibraryTags.EDITOR_LIST)
        list.performScrollToNode(hasTestTag(LibraryTags.verse(0)))
        onNodeWithTag(LibraryTags.verse(0)).assertIsDisplayed()
        list.performScrollToNode(hasTestTag(LibraryTags.verse(1)))
        onNodeWithTag(LibraryTags.verse(1)).assertIsDisplayed()
    }
}
