package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.SavedTemplate
import com.church.presenter.churchpresentermobile.model.ServiceKind
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.type
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The form behind both "New service" and "Edit service": a name, a start time, a kind, and — for
 * a new one — what to start from.
 *
 * The start time is typed, not picked, so it accepts what a person types: `10:00`, `6:30 pm`,
 * `1830`. What is stored is always 24-hour, whatever was typed.
 */
@OptIn(ExperimentalTestApi::class)
class ServiceFormTest {

    private var confirmed: ServiceDraft? = null
    private var dismissed = 0
    private var deleted = 0

    private val templates = listOf(
        SavedTemplate(id = "t1", name = "Standard Sunday", startTime = "10:00"),
        SavedTemplate(id = "t2", name = "Carol Service", startTime = "18:30"),
    )

    private fun ComposeUiTest.show(
        initialName: String = "Sunday Morning",
        initialStart: String = "10:00",
        initialKind: String = ServiceKind.SUNDAY.id,
        templates: List<SavedTemplate> = emptyList(),
        deletable: Boolean = false,
    ) = showScreen {
        ServiceForm(
            title = "New service",
            subtitle = "Sunday, September 20, 2026",
            initialName = initialName,
            initialStart = initialStart,
            initialKind = initialKind,
            templates = templates,
            confirmLabel = "Add",
            onConfirm = { confirmed = it },
            onDismiss = { dismissed++ },
            onDelete = if (deletable) ({ deleted++ }) else null,
        )
    }

    private fun ComposeUiTest.confirm() = click(CalendarTags.SERVICE_CONFIRM)

    @Test
    fun theFormOpensOnWhatItWasGiven() = runComposeUiTest {
        show()

        assertTrue(isShowing("Sunday Morning"))
        assertTrue(isShowing("Sunday, September 20, 2026"))
    }

    @Test
    fun aStoredTimeIsShownAsTheClockFormatReadsIt() = runComposeUiTest {
        show(initialStart = "18:30")

        assertTrue(isShowing("6:30"), "typed back as a person would say it")
    }

    @Test
    fun confirmingHandsBackWhatWasTyped() = runComposeUiTest {
        show()

        type(CalendarTags.SERVICE_NAME, "Carol Service")
        waitForIdle()
        confirm()

        assertEquals("Carol Service", confirmed?.name)
        assertEquals("10:00", confirmed?.startTime)
    }

    @Test
    fun aTypedTwelveHourTimeIsStoredAsTwentyFour() = runComposeUiTest {
        show()

        type(CalendarTags.SERVICE_START, "6:30 pm")
        waitForIdle()
        confirm()

        assertEquals("18:30", confirmed?.startTime)
    }

    @Test
    fun aFourDigitTimeIsAccepted() = runComposeUiTest {
        show()

        type(CalendarTags.SERVICE_START, "1830")
        waitForIdle()
        confirm()

        assertEquals("18:30", confirmed?.startTime)
    }

    @Test
    fun aNameIsTrimmedBeforeItIsUsed() = runComposeUiTest {
        show()

        type(CalendarTags.SERVICE_NAME, "  Evening Prayer  ")
        waitForIdle()
        confirm()

        assertEquals("Evening Prayer", confirmed?.name)
    }

    @Test
    fun aServiceWithNoNameCannotBeCreated() = runComposeUiTest {
        show()

        type(CalendarTags.SERVICE_NAME, " ")
        waitForIdle()

        onNodeWithTag(CalendarTags.SERVICE_CONFIRM).assertIsNotEnabled()
        assertNull(confirmed, "there is nothing to create until it has a name")
    }

    @Test
    fun aStartTimeNobodyCanReadStopsTheForm() = runComposeUiTest {
        show()

        type(CalendarTags.SERVICE_START, "whenever")
        waitForIdle()

        onNodeWithTag(CalendarTags.SERVICE_CONFIRM).assertIsNotEnabled()
        assertNull(confirmed)
    }

    @Test
    fun theKindCanBeChanged() = runComposeUiTest {
        show()

        click(CalendarTags.serviceKind(1))
        waitForIdle()
        confirm()

        assertEquals(ServiceKind.MIDWEEK.id, confirmed?.kind)
    }

    @Test
    fun aServiceOpensOnTheKindItAlreadyIs() = runComposeUiTest {
        show(initialKind = ServiceKind.SPECIAL.id, deletable = true)

        confirm()

        assertEquals(ServiceKind.SPECIAL.id, confirmed?.kind)
    }

    @Test
    fun aNewServiceStartsBlankUnlessATemplateIsChosen() = runComposeUiTest {
        show(templates = templates)

        confirm()

        assertNull(confirmed?.templateId)
    }

    @Test
    fun theSavedTemplatesAreOffered() = runComposeUiTest {
        show(templates = templates)

        assertTrue(isShowing("Standard Sunday"))
        assertTrue(isShowing("Carol Service"))
    }

    @Test
    fun choosingATemplateIsCarriedThrough() = runComposeUiTest {
        show(templates = templates)

        click(CalendarTags.template("t1"))
        waitForIdle()
        confirm()

        assertEquals("t1", confirmed?.templateId)
    }

    @Test
    fun editingAnExistingServiceOffersDeleteAndNoTemplates() = runComposeUiTest {
        show(deletable = true, templates = emptyList())

        assertTrue(!isShowing("Standard Sunday"))
        click(CalendarTags.SERVICE_DELETE)

        assertEquals(1, deleted)
    }

    @Test
    fun theFormCanBeClosedWithoutCreatingAnything() = runComposeUiTest {
        show()

        click(CalendarTags.SERVICE_CANCEL)

        assertEquals(1, dismissed)
        assertNull(confirmed)
    }
}
