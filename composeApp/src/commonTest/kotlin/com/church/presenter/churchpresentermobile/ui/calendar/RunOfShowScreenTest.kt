package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The run of show: every row of a service in order, with the clock time each one is projected to
 * start at, and the controls that change them.
 *
 * The inline form is the tablet's — beside the month rather than over it — and differs in more
 * than width: it carries per-row move and remove controls, because there is no room to open a row
 * editor over the top of it.
 */
@OptIn(ExperimentalTestApi::class)
class RunOfShowScreenTest {

    private var armed: Boolean? = null
    private val moves = mutableListOf<Pair<Int, Int>>()
    private val removed = mutableListOf<String>()
    private var deleted = 0
    private var loaded = 0

    private fun actions(loadable: Boolean = false) = RunOfShowActions(
        rows = RowListActions(
            onAdd = { _, _, _ -> },
            onUpdate = {},
            onRemove = { removed += it },
            onMove = { from, to -> moves += from to to },
        ),
        onArmed = { armed = it },
        onCopy = {},
        onUpdateService = {},
        onDelete = { deleted++ },
        onLoadIntoSchedule = if (loadable) ({ loaded++ }) else null,
    )

    private fun ComposeUiTest.show(
        service: PlannedService = CalendarFixtures.service,
        inline: Boolean = false,
        loadable: Boolean = false,
        onBack: (() -> Unit)? = null,
    ) = showScreen {
        RunOfShowScreen(
            service,
            CalendarFixtures.sources(),
            { "new-row" },
            actions(loadable),
            onBack = onBack,
            inline = inline,
        )
    }

    // ── What it draws ────────────────────────────────────────────────────

    @Test
    fun theServiceIsNamedAtTheTop() = runComposeUiTest {
        show()

        assertTrue(isShowing(CalendarFixtures.SERVICE_NAME))
    }

    @Test
    fun everyRowIsListed() = runComposeUiTest {
        show()

        // A section heading is drawn in small caps; the rest read as they were typed.
        assertTrue(isShowing(CalendarFixtures.SECTION.uppercase()))
        assertTrue(isShowing(CalendarFixtures.FIRST_SONG))
        assertTrue(isShowing(CalendarFixtures.SECOND_SONG))
        assertTrue(isShowing(CalendarFixtures.PASSAGE))
        assertTrue(exists(CalendarTags.row("r5")), "the ministry item")
        assertTrue(exists(CalendarTags.row("r6")), "the cue")
    }

    @Test
    fun aRowCarriesWhatItIsUnderItsTitle() = runComposeUiTest {
        show()

        assertTrue(isShowing("Hymnal"), "a song's book")
        assertTrue(isShowing("Anna"), "who the ministry item is")
        assertTrue(isShowing("For God so loved"), "the verse's first words")
    }

    @Test
    fun eachRowShowsWhenItIsProjectedToStart() = runComposeUiTest {
        show()

        // 10:00 start, the first song pinned to 10:05, then each length added on.
        assertTrue(isShowing("10:05"))
        assertTrue(isShowing("10:09"))
    }

    @Test
    fun anEmptyRunOfShowSaysSoInsteadOfDrawingNothing() = runComposeUiTest {
        show(CalendarFixtures.empty)

        assertFalse(isShowing(CalendarFixtures.FIRST_SONG))
        assertTrue(isShowing(CalendarFixtures.SERVICE_NAME), "the service is still named")
    }

    @Test
    fun aServiceWithNoReadableStartTimeStillListsItsRows() = runComposeUiTest {
        show(CalendarFixtures.service.copy(startTime = "nonsense"))

        assertTrue(isShowing(CalendarFixtures.FIRST_SONG), "no clock is not no rows")
    }

    // ── The controls ─────────────────────────────────────────────────────

    @Test
    fun theArmedSwitchReportsBothWays() = runComposeUiTest {
        show()

        click(CalendarTags.RUN_ARMED)

        assertEquals(false, armed, "the fixture is armed, so the first tap disarms it")
    }

    @Test
    fun aDisarmedServiceCanBeArmedAgain() = runComposeUiTest {
        show(CalendarFixtures.service.copy(armed = false))

        click(CalendarTags.RUN_ARMED)

        assertEquals(true, armed)
    }

    @Test
    fun theBackArrowIsThereOnlyWhenThereIsSomewhereToGoBackTo() = runComposeUiTest {
        var back = 0
        show(onBack = { back++ })

        click(CalendarTags.RUN_BACK)

        assertEquals(1, back)
    }

    @Test
    fun theInlineFormHasNoBackArrow() = runComposeUiTest {
        show(inline = true)

        // Beside the month, there is nothing behind this pane to return to.
        assertTrue(!exists(CalendarTags.RUN_BACK))
        assertTrue(isShowing(CalendarFixtures.SERVICE_NAME))
    }

    @Test
    fun aRowCanBeRemovedInPlaceOnATablet() = runComposeUiTest {
        show(inline = true)

        assertTrue(exists(CalendarTags.row("r2")), "every row is reachable by its id")
    }

    @Test
    fun loadIntoScheduleIsOfferedOnlyWithADesktopToLoadInto() = runComposeUiTest {
        show(loadable = true)

        click(CalendarTags.RUN_LOAD)

        assertEquals(1, loaded)
    }

    @Test
    fun withNoDesktopTheLoadButtonDoesNothing() = runComposeUiTest {
        show(loadable = false)

        click(CalendarTags.RUN_LOAD)

        assertEquals(0, loaded, "it is drawn but disabled, so the row still reads as loadable later")
    }

    @Test
    fun theAddButtonOpensThePicker() = runComposeUiTest {
        show()

        click(CalendarTags.RUN_ADD)
        waitForIdle()

        assertTrue(isShowing(CalendarFixtures.SERVICE_NAME))
    }

    @Test
    fun aCueRowIsListedLikeAnyOther() = runComposeUiTest {
        val onlyCue = CalendarFixtures.service.copy(
            rows = listOf(PlanRow.Ref(id = "c1", title = "Go live", kind = "cue", subtitle = "At 10:00")),
        )
        show(onlyCue)

        assertTrue(isShowing("Go live"))
        assertTrue(isShowing("At 10:00"))
    }

    @Test
    fun aSectionRowDrawsWithoutAClockTime() = runComposeUiTest {
        val onlySection = CalendarFixtures.service.copy(
            rows = listOf(PlanRow.Section(id = "s", title = "Communion")),
            plannedSeconds = emptyMap(),
            timing = emptyMap(),
        )
        show(onlySection)

        assertTrue(isShowing("COMMUNION"), "a heading is drawn in small caps")
    }

    @Test
    fun openingARowShowsItsEditor() = runComposeUiTest {
        show()

        click(CalendarTags.row("r2"))
        waitForIdle()

        // The editor opens over the list; the row it is editing is named in it.
        assertTrue(isShowing(CalendarFixtures.FIRST_SONG))
    }
}
