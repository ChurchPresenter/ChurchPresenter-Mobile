package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.RowEnd
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.type
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A tapped row: what can be changed about it, and what cannot.
 *
 * A row the desktop authored keeps its content read-only — a song is the desktop's song — while
 * where and how long it runs is this device's to change. The rows written on the phone (a section,
 * a ministry item, a passage) can be renamed here too.
 */
@OptIn(ExperimentalTestApi::class)
class RowEditorContentTest {

    private var saved: RowEdit? = null
    private val moves = mutableListOf<Int>()
    private var removed = 0
    private var dismissed = 0

    private fun ComposeUiTest.show(row: PlanRow) = showScreen {
        RowEditorContent(
            service = CalendarFixtures.service,
            row = row,
            onSave = { saved = it },
            onMove = { moves += it },
            onRemove = { removed++ },
            onDismiss = { dismissed++ },
        )
    }

    private fun ComposeUiTest.save() = click(CalendarTags.ROW_SAVE)

    private val song = CalendarFixtures.service.rows[1]
    private val section = CalendarFixtures.service.rows[0]
    private val ministry = CalendarFixtures.service.rows[4]

    // ── What each kind offers ────────────────────────────────────────────

    @Test
    fun theRowBeingEditedIsNamedAtTheTop() = runComposeUiTest {
        show(song)

        assertTrue(isShowing(CalendarFixtures.FIRST_SONG))
    }

    @Test
    fun aSongsContentCannotBeRenamedHere() = runComposeUiTest {
        show(song)

        // The title belongs to the desktop's library; only how long it runs is editable here.
        assertTrue(!exists(CalendarTags.ROW_NAME))
        assertTrue(exists(CalendarTags.ROW_DURATION))
    }

    @Test
    fun aMinistryItemCanBeRenamedAndReassigned() = runComposeUiTest {
        show(ministry)

        assertTrue(exists(CalendarTags.ROW_NAME))
        assertTrue(exists(CalendarTags.ROW_DETAIL))
        assertTrue(exists(CalendarTags.ROW_DURATION))
    }

    @Test
    fun aSectionHasNoTimingAtAll() = runComposeUiTest {
        show(section)

        // A divider does not go on screen, so there is nothing to time.
        assertTrue(exists(CalendarTags.ROW_NAME))
        assertTrue(!exists(CalendarTags.ROW_DURATION))
    }

    @Test
    fun aRowsLengthIsShownAsItWasPlanned() = runComposeUiTest {
        show(song)

        assertTrue(isShowing("4:30"), "270 seconds, as the operator typed it")
    }

    // ── Saving ───────────────────────────────────────────────────────────

    @Test
    fun savingASongKeepsItsTitleAndReturnsItsLength() = runComposeUiTest {
        show(song)

        save()

        val edit = saved
        assertIs<PlanRow.Song>(edit?.row)
        assertEquals(CalendarFixtures.FIRST_SONG, edit?.row?.title)
        assertEquals(270, edit?.seconds)
    }

    @Test
    fun aRetypedLengthIsWhatIsSaved() = runComposeUiTest {
        show(song)

        type(CalendarTags.ROW_DURATION, "6:00")
        waitForIdle()
        save()

        assertEquals(360, saved?.seconds)
    }

    @Test
    fun aLengthThatIsNotATimeSavesNoLength() = runComposeUiTest {
        show(song)

        type(CalendarTags.ROW_DURATION, "soon")
        waitForIdle()
        save()

        assertNull(saved?.seconds, "the item's own length, rather than a number nobody meant")
    }

    @Test
    fun renamingASectionIsSaved() = runComposeUiTest {
        show(section)

        type(CalendarTags.ROW_NAME, "Communion")
        waitForIdle()
        save()

        assertEquals("Communion", saved?.row?.title)
    }

    @Test
    fun aSectionRenamedToNothingKeepsTheNameItHad() = runComposeUiTest {
        show(section)

        type(CalendarTags.ROW_NAME, "   ")
        waitForIdle()
        save()

        assertEquals(CalendarFixtures.SECTION, saved?.row?.title)
    }

    @Test
    fun aSectionSavesNoLengthAndDefaultTiming() = runComposeUiTest {
        show(section)

        save()

        assertNull(saved?.seconds)
        assertEquals("", saved?.timing?.startAt)
        assertEquals(RowEnd.HOLD, saved?.timing?.atEnd)
    }

    @Test
    fun renamingAMinistryItemAndItsPersonIsSaved() = runComposeUiTest {
        show(ministry)

        type(CalendarTags.ROW_NAME, "Notices")
        type(CalendarTags.ROW_DETAIL, "Peter")
        waitForIdle()
        save()

        val row = assertIs<PlanRow.Ministry>(saved?.row)
        assertEquals("Notices", row.title)
        assertEquals("Peter", row.detail)
    }

    @Test
    fun aPinnedRowSavesTheClockTimeItWasPinnedTo() = runComposeUiTest {
        show(song)

        save()

        assertEquals("10:05", saved?.timing?.startAt, "as the fixture pinned it")
    }

    // ── Moving and removing ──────────────────────────────────────────────

    @Test
    fun aRowInTheMiddleCanBeMovedBothWays() = runComposeUiTest {
        show(song)

        click(CalendarTags.ROW_UP)
        click(CalendarTags.ROW_DOWN)

        assertEquals(listOf(-1, 1), moves)
    }

    @Test
    fun theFirstRowCannotBeMovedUpOutOfTheService() = runComposeUiTest {
        show(section)

        click(CalendarTags.ROW_UP)

        assertTrue(moves.isEmpty())
    }

    @Test
    fun aRowCanBeRemoved() = runComposeUiTest {
        show(song)

        click(CalendarTags.ROW_REMOVE)

        assertEquals(1, removed)
    }

    @Test
    fun theEditorCanBeClosedWithoutSaving() = runComposeUiTest {
        show(song)

        click(CalendarTags.SHEET_CLOSE)

        assertEquals(1, dismissed)
        assertNull(saved)
    }
}
