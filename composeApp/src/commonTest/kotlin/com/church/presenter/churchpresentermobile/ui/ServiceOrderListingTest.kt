package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalSetlistEntry
import com.church.presenter.churchpresentermobile.model.SetlistEntryType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What the standalone running order lists.
 *
 * Titles are content the operator typed, so they are matched by their text;
 * everything else is a `stringResource`, which renders empty in these runtimes.
 */
@OptIn(ExperimentalTestApi::class)
class ServiceOrderListingTest {

    @Test
    fun everyEntryIsListed() = runComposeUiTest {
        showServiceOrder()

        assertTrue(isShowing("Amazing Grace"))
        assertTrue(isShowing("John 3:16"))
        assertTrue(isShowing("Welcome notice"))
    }

    @Test
    fun anEntryIsListedByItsTitle() = runComposeUiTest {
        showServiceOrder(entries = listOf(entry("Be Thou My Vision")))

        assertTrue(isShowing("Be Thou My Vision"))
    }

    @Test
    fun everyEntryGetsARowOfItsOwn() = runComposeUiTest {
        showServiceOrder()

        assertTrue(exists(UiTags.orderRow(0)))
        assertTrue(exists(UiTags.orderRow(1)))
        assertTrue(exists(UiTags.orderRow(2)))
    }

    @Test
    fun thereIsNoRowBeyondTheLastEntry() = runComposeUiTest {
        showServiceOrder()

        assertFalse(exists(UiTags.orderRow(3)))
    }

    @Test
    fun theCountIsShown() = runComposeUiTest {
        showServiceOrder()

        assertTrue(exists(UiTags.DRAWER_COUNT))
    }

    @Test
    fun allThreeEntryTypesAreListedTogether() = runComposeUiTest {
        // Songs, passages and notices share one running order — the drawer must
        // not quietly drop a type it has no icon for.
        showServiceOrder()

        assertTrue(exists(UiTags.orderRow(0)))
        assertTrue(exists(UiTags.orderRow(1)))
        assertTrue(exists(UiTags.orderRow(2)))
    }

    @Test
    fun anEntryWithAnEmptyTitleStillGetsARow() = runComposeUiTest {
        // A half-saved entry has to stay visible, or it cannot be removed.
        showServiceOrder(entries = listOf(LocalSetlistEntry(SetlistEntryType.SONG, "ref", "")))

        assertTrue(exists(UiTags.orderRow(0)))
    }

    @Test
    fun aLongTitleStillListsTheEntry() = runComposeUiTest {
        showServiceOrder(entries = listOf(entry("A ".repeat(80) + "hymn")))

        assertTrue(exists(UiTags.orderRow(0)))
    }

    // ── Empty ────────────────────────────────────────────────────────────

    @Test
    fun anEmptyOrderSaysSo() = runComposeUiTest {
        showServiceOrder(entries = emptyList())

        assertTrue(exists(UiTags.DRAWER_EMPTY))
    }

    @Test
    fun theEmptyMessageIsGoneOnceThereIsAnEntry() = runComposeUiTest {
        showServiceOrder()

        assertFalse(exists(UiTags.DRAWER_EMPTY))
    }

    @Test
    fun anEmptyOrderListsNoRows() = runComposeUiTest {
        showServiceOrder(entries = emptyList())

        assertFalse(exists(UiTags.orderRow(0)))
    }

    @Test
    fun theCountIsStillShownWhenTheOrderIsEmpty() = runComposeUiTest {
        // "0 items" is information; a missing line reads as a broken drawer.
        showServiceOrder(entries = emptyList())

        assertTrue(exists(UiTags.DRAWER_COUNT))
    }
}
