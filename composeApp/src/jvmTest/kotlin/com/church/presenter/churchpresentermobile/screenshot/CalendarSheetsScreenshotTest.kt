package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.RowEnd
import com.church.presenter.churchpresentermobile.model.RowKind
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.model.SavedTemplate
import com.church.presenter.churchpresentermobile.model.SectionPalette
import com.church.presenter.churchpresentermobile.model.ServiceKind
import com.church.presenter.churchpresentermobile.ui.calendar.CopyServiceContent
import com.church.presenter.churchpresentermobile.ui.calendar.RowEditorContent
import com.church.presenter.churchpresentermobile.ui.calendar.ServiceForm
import com.church.presenter.churchpresentermobile.ui.calendar.TimingDraft
import com.church.presenter.churchpresentermobile.ui.calendar.TimingPanel
import kotlin.test.Test

/**
 * The calendar's sheets: the row editor, the service form, the copy dialog and the timing panel.
 *
 * Each is photographed as its content rather than through the modal sheet it normally opens in —
 * a sheet is its own window, and a capture of the screen behind it shows nothing of what it covers.
 */
class CalendarSheetsScreenshotTest {

    private val service = PlannedService(
        id = "s1",
        date = "2026-09-20",
        name = "Sunday Morning",
        startTime = "10:00",
        rows = listOf(
            PlanRow.Section(id = "r1", title = "Worship", color = SectionPalette.VIOLET),
            PlanRow.Song(id = "r2", title = "Amazing Grace", songbook = "Hymnal", number = "42"),
            PlanRow.Bible(id = "r3", title = "John 3:16-17", preview = "For God so loved the world"),
            PlanRow.Ministry(id = "r4", title = "Welcome & notices", detail = "Anna"),
            PlanRow.Ref(id = "r5", title = "Countdown", kind = RowKind.CUE, subtitle = "Blank the screens"),
        ),
        plannedSeconds = mapOf("r2" to 270, "r3" to 120, "r4" to 300),
        timing = mapOf("r2" to RowTiming(startAt = "10:05", atEnd = RowEnd.NEXT)),
    )

    // ── The row editor, one per row kind ─────────────────────────────────

    private fun rowEditor(name: String, index: Int) = screenshot(name) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            RowEditorContent(
                service = service,
                row = service.rows[index],
                onSave = {},
                onMove = {},
                onRemove = {},
                onDismiss = {},
            )
        }
    }

    /** A song: its title is the desktop's, so only its length and timing can be changed. */
    @Test
    fun rowEditorSong() = rowEditor("calendar-row__song", 1)

    /** A heading: a name and a colour, and no timing at all. */
    @Test
    fun rowEditorSection() = rowEditor("calendar-row__section", 0)

    /** A passage, which the phone may rename because the phone typed it. */
    @Test
    fun rowEditorBible() = rowEditor("calendar-row__bible", 2)

    /** A ministry item: what happens, who it is, and how long it runs. */
    @Test
    fun rowEditorMinistry() = rowEditor("calendar-row__ministry", 3)

    /** A cue: nothing of its own to show, so only where it sits in the order. */
    @Test
    fun rowEditorCue() = rowEditor("calendar-row__cue", 4)

    // ── The timing panel on its own ──────────────────────────────────────

    private fun timing(name: String, draft: TimingDraft) = screenshot(name) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            TimingPanel(draft = draft, onChange = {}, serviceStart = "10:00")
        }
    }

    @Test
    fun timingCued() = timing("calendar-timing__cued", TimingDraft())

    @Test
    fun timingPinned() = timing(
        "calendar-timing__pinned",
        TimingDraft(startOffsetMinutes = -5, runSeconds = 300, atEnd = RowEnd.NEXT),
    )

    @Test
    fun timingLooping() = timing(
        "calendar-timing__looping",
        TimingDraft(followsPrevious = true, repeats = 0, atEnd = RowEnd.BLANK),
    )

    @Test
    fun timingDisabled() = screenshot("calendar-timing__disabled") {
        // A section's panel: drawn, so the shape of a row is the same everywhere, but inert.
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            TimingPanel(draft = TimingDraft(), onChange = {}, serviceStart = "10:00", enabled = false)
        }
    }

    // ── The service form ─────────────────────────────────────────────────

    private val templates = listOf(
        SavedTemplate(id = "t1", name = "Standard Sunday", startTime = "10:00"),
        SavedTemplate(id = "t2", name = "Carol Service", startTime = "18:30"),
    )

    @Test
    fun newService() = screenshot("calendar-service__new") {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            ServiceForm(
                title = "New service",
                subtitle = "Sunday, September 20, 2026",
                initialName = "Sunday Morning",
                initialStart = "10:00",
                initialKind = ServiceKind.SUNDAY.id,
                templates = templates,
                confirmLabel = "Add service",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }

    @Test
    fun newServiceWithNoTemplatesSaved() = screenshot("calendar-service__new-no-templates") {
        // A first-ever service: nothing to start from but a blank one.
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            ServiceForm(
                title = "New service",
                subtitle = "Sunday, September 20, 2026",
                initialName = "Sunday Morning",
                initialStart = "10:00",
                initialKind = ServiceKind.SUNDAY.id,
                templates = emptyList(),
                confirmLabel = "Add service",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }

    @Test
    fun editService() = screenshot("calendar-service__edit") {
        // The same form over a service that exists: Delete instead of a template list.
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            ServiceForm(
                title = "Edit service",
                subtitle = "Sunday, September 20, 2026",
                initialName = "Sunday Morning",
                initialStart = "18:30",
                initialKind = ServiceKind.SPECIAL.id,
                templates = emptyList(),
                confirmLabel = "Save",
                onConfirm = {},
                onDismiss = {},
                onDelete = {},
            )
        }
    }

    // ── Copying a service ────────────────────────────────────────────────

    @Test
    fun copyService() = screenshot("calendar-copy__weekly") {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            CopyServiceContent(service = service, onCopy = {}, onDismiss = {})
        }
    }

    @Test
    fun copyServiceWithNoAutomation() = screenshot("calendar-copy__no-cues") {
        // No cue rows, so the automation checkbox is not offered at all.
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            CopyServiceContent(
                service = service.copy(rows = service.rows.filterNot { it.id == "r5" }),
                onCopy = {},
                onDismiss = {},
            )
        }
    }
}
