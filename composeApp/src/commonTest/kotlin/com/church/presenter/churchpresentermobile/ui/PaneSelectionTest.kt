package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.MoreDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What "the open one" looks like when the list stays on screen beside it.
 *
 * On a phone none of this existed: opening something replaced the list it came
 * from, so there was never a list to mark. With both halves visible the mark is
 * the only thing telling the operator which of the rows in front of them is the
 * one currently filling the other pane.
 */
@OptIn(ExperimentalTestApi::class)
class PaneSelectionTest {

    // ── Bible: the chapter grid beside the verses ────────────────────────

    @Test
    fun theChapterPaneMarksTheOpenChapter() = runComposeUiTest {
        showScreen {
            ChaptersGrid(
                book = genesis,
                onChapterSelect = {},
                selectedChapter = 3,
                columns = 2,
                modifier = Modifier.fillMaxSize(),
            )
        }

        assertTrue(exists(UiTags.bibleChapter(3)))
        assertTrue(exists(UiTags.bibleChapter(1)), "the other chapters are still offered")
    }

    @Test
    fun everyChapterStaysTappableWhileOneIsOpen() = runComposeUiTest {
        // The open chapter is drawn differently; it must not become inert, or the
        // operator cannot return to it after wandering off.
        var picked: Int? = null
        showScreen {
            ChaptersGrid(
                book = genesis,
                onChapterSelect = { picked = it },
                selectedChapter = 3,
                columns = 2,
                modifier = Modifier.fillMaxSize(),
            )
        }

        click(UiTags.bibleChapter(3))
        assertEquals(3, picked)

        click(UiTags.bibleChapter(5))
        assertEquals(5, picked)
    }

    @Test
    fun theChapterGridDrawsTheColumnCountItIsGiven() = runComposeUiTest {
        // Two in the tablet's narrow pane, four on a phone. Both counts have to
        // render every chapter — a grid that dropped some would strand a reading.
        listOf(2, 4).forEach { columns ->
            runComposeUiTest {
                showScreen {
                    ChaptersGrid(
                        book = genesis,
                        onChapterSelect = {},
                        columns = columns,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                (1..6).forEach {
                    assertTrue(exists(UiTags.bibleChapter(it)), "chapter $it missing at $columns columns")
                }
            }
        }
    }

    // ── More: the tile grid beside the open tool ─────────────────────────

    @Test
    fun theLauncherMarksTheOpenTool() = runComposeUiTest {
        showScreen {
            MoreScreen(mode = AppMode.REMOTE, onSelect = {}, selected = MoreDestination.DICTIONARY)
        }

        // Every tile is still offered; the open one is simply drawn as open.
        MoreDestination.forMode(AppMode.REMOTE).forEach {
            assertTrue(exists(UiTags.moreRow(it)), "$it fell out of the launcher")
        }
    }

    @Test
    fun theOpenToolIsStillTappable() = runComposeUiTest {
        var picked: MoreDestination? = null
        showScreen {
            MoreScreen(
                mode = AppMode.REMOTE,
                onSelect = { picked = it },
                selected = MoreDestination.DICTIONARY,
            )
        }

        click(UiTags.moreRow(MoreDestination.DICTIONARY))

        assertEquals(MoreDestination.DICTIONARY, picked)
    }

    @Test
    fun nothingIsMarkedOnAPhone() = runComposeUiTest {
        // `selected` defaults to null, because on a phone opening a tool replaces
        // this screen and there is never a marked tile to see.
        var picked: MoreDestination? = null
        showScreen { MoreScreen(mode = AppMode.REMOTE, onSelect = { picked = it }) }

        click(UiTags.moreRow(MoreDestination.WEB))

        assertEquals(MoreDestination.WEB, picked)
    }

    @Test
    fun standaloneStillHidesTheDesktopOnlyTools() = runComposeUiTest {
        // The `selected` parameter must not have quietly changed which tiles the
        // mode filter lets through.
        showScreen {
            MoreScreen(mode = AppMode.STANDALONE, onSelect = {}, selected = MoreDestination.WEB)
        }

        assertFalse(exists(UiTags.moreRow(MoreDestination.QA)))
        assertFalse(exists(UiTags.moreRow(MoreDestination.DICTIONARY)))
        assertTrue(exists(UiTags.moreRow(MoreDestination.WEB)))
    }

    private val genesis = BibleBook(name = "Genesis", chapterTotal = 50)
}
