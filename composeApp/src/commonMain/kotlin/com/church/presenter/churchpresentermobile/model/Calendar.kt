package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val CURRENT_CALENDAR_VERSION = 1

/**
 * Everything the planner holds — the one thing `calendar.json` contains on this device, and the
 * shape exchanged with a ChurchPresenter desktop. A phone can name a song, a passage, a section,
 * a ministry item or a preset; a row the desktop authored comes back as a [PlanRow.Ref].
 */
@Serializable
data class CalendarDocument(
    val version: Int = CURRENT_CALENDAR_VERSION,
    val services: List<PlannedService> = emptyList(),
    val templates: List<SavedTemplate> = emptyList(),
    /** Services deleted here, as id → ISO instant, so a delete survives a merge with the desktop. */
    val deletedServices: Map<String, String> = emptyMap(),
    /** The desktop's presets, by name and kind only — the payload stays on the desktop. */
    val presets: List<PresetSummary> = emptyList(),
    /** Services edited here and not yet accepted by the relay; replayed by the next sync. */
    val pendingPush: Set<String> = emptySet(),
    /** Services deleted here and not yet told to the relay. */
    val pendingDeletes: Set<String> = emptySet(),
) {
    fun servicesOn(date: String): List<PlannedService> =
        services.filter { it.date == date }.sortedBy { it.startTime }

    fun plannedDates(): Set<String> = services.mapTo(mutableSetOf()) { it.date }

    fun serviceById(id: String): PlannedService? = services.firstOrNull { it.id == id }

    fun withService(service: PlannedService): CalendarDocument {
        val index = services.indexOfFirst { it.id == service.id }
        val updated = if (index >= 0) services.toMutableList().also { it[index] = service } else services + service
        return copy(services = updated)
    }

    fun withServices(added: List<PlannedService>): CalendarDocument =
        added.fold(this) { document, service -> document.withService(service) }

    fun withoutService(id: String, at: String): CalendarDocument = copy(
        services = services.filterNot { it.id == id },
        deletedServices = deletedServices + (id to at),
        pendingPush = pendingPush - id,
        pendingDeletes = pendingDeletes + id,
    )

    fun withTemplate(template: SavedTemplate): CalendarDocument {
        val index = templates.indexOfFirst { it.id == template.id || it.name.equals(template.name, ignoreCase = true) }
        val updated = if (index >= 0) templates.toMutableList().also { it[index] = template } else templates + template
        return copy(templates = updated)
    }

    companion object {
        val EMPTY = CalendarDocument()
    }
}

@Serializable
data class PlannedService(
    val id: String,
    /** `YYYY-MM-DD`. */
    val date: String,
    val name: String,
    /** `HH:mm`, 24-hour, whatever the clock format on screen. */
    val startTime: String,
    val kind: String = ServiceKind.SUNDAY.id,
    val rows: List<PlanRow> = emptyList(),
    /** How long each row is planned to run, in seconds, keyed by row id. */
    val plannedSeconds: Map<String, Int> = emptyMap(),
    /** When each row starts and what it does at the end, keyed by row id; missing means cued by hand. */
    val timing: Map<String, RowTiming> = emptyMap(),
    /** Whether the automation may fire when this service is loaded on the desktop. */
    val armed: Boolean = true,
    /** Shared by every copy made together, so a series can be found again. */
    val seriesId: String = "",
    /** ISO instant of the last edit; what a merge compares. Stamped by the relay once synced. */
    val updatedAt: String = "",
    /** The relay revision this copy came from; 0 until it has been there. What a write is conditioned on. */
    val rev: Long = 0L,
) {
    fun timingFor(rowId: String): RowTiming = timing[rowId] ?: RowTiming.DEFAULT
    fun plannedSecondsFor(rowId: String): Int? = plannedSeconds[rowId]

    fun withRow(row: PlanRow, seconds: Int?, rowTiming: RowTiming, at: Int = rows.size): PlannedService {
        val existing = rows.indexOfFirst { it.id == row.id }
        val list = rows.toMutableList()
        if (existing >= 0) list[existing] = row else list.add(at.coerceIn(0, list.size), row)
        return copy(
            rows = list,
            plannedSeconds = if (seconds == null) plannedSeconds - row.id else plannedSeconds + (row.id to seconds),
            timing = if (rowTiming.isDefault()) timing - row.id else timing + (row.id to rowTiming),
        )
    }

    fun withoutRow(rowId: String): PlannedService = copy(
        rows = rows.filterNot { it.id == rowId },
        plannedSeconds = plannedSeconds - rowId,
        timing = timing - rowId,
    )

    fun withRowMoved(from: Int, to: Int): PlannedService {
        if (from !in rows.indices || to !in rows.indices || from == to) return this
        val list = rows.toMutableList()
        val row = list.removeAt(from)
        list.add(to, row)
        return copy(rows = list)
    }

    /** Rows that go on screen or take time up front — everything but section headings. */
    val itemCount: Int get() = rows.count { it !is PlanRow.Section }
}

/**
 * One row of a run of show. The `type` discriminator is what the file and the wire carry.
 */
@Serializable
sealed class PlanRow {
    abstract val id: String
    abstract val title: String

    /** A heading that divides the run of show; never goes on screen. */
    @Serializable
    @SerialName("section")
    data class Section(override val id: String, override val title: String, val color: String = SectionPalette.DEFAULT) : PlanRow()

    @Serializable
    @SerialName("song")
    data class Song(
        override val id: String,
        override val title: String,
        val songId: String = "",
        val songbook: String = "",
        val number: String = "",
    ) : PlanRow()

    /** A passage, as typed or picked: `Psalms 100:1-5`. The desktop resolves the text. */
    @Serializable
    @SerialName("bible")
    data class Bible(
        override val id: String,
        override val title: String,
        val preview: String = "",
        /** The canonical book number, 1..66, the same in every translation; 0 when only the name is known. */
        val bookId: Int = 0,
    ) : PlanRow()

    /** Something that happens up front and not on screen — a solo, a testimony, a prayer. */
    @Serializable
    @SerialName("ministry")
    data class Ministry(override val id: String, override val title: String, val detail: String = "") : PlanRow()

    /** One of the desktop's saved presets, by id; what it contains stays on the desktop. */
    @Serializable
    @SerialName("preset")
    data class Preset(
        override val id: String,
        override val title: String,
        val presetId: String,
        val kind: String = RowKind.PRESET,
    ) : PlanRow()

    /** A row the desktop authored. Shown as it was projected; movable, removable, never edited here. */
    @Serializable
    @SerialName("ref")
    data class Ref(
        override val id: String,
        override val title: String,
        val kind: String = RowKind.OTHER,
        val subtitle: String = "",
    ) : PlanRow()

    /** What icon and color the row is drawn with. */
    val kindKey: String
        get() = when (this) {
            is Section -> RowKind.SECTION
            is Song -> RowKind.SONG
            is Bible -> RowKind.BIBLE
            is Ministry -> RowKind.MINISTRY
            is Preset -> kind
            is Ref -> kind
        }
}

/** The kinds a row can be drawn as; the desktop's projection uses the same words. */
object RowKind {
    const val SECTION = "section"
    const val SONG = "song"
    const val BIBLE = "bible"
    const val MINISTRY = "ministry"
    const val PRESET = "preset"
    const val PICTURES = "pictures"
    const val PRESENTATION = "presentation"
    const val MEDIA = "media"
    const val SCENE = "scene"
    const val TIMER = "timer"
    const val ANNOUNCEMENT = "announcement"
    const val LOWER_THIRD = "lowerThird"
    const val WEBSITE = "website"
    const val CUE = "cue"
    const val OTHER = "other"
}

/**
 * How one row runs: when it starts, how many times, and what happens when it ends.
 * The all-defaults value is a row the operator cues by hand.
 */
@Serializable
data class RowTiming(
    /** `HH:mm` on the wall clock to start on its own; empty to wait to be cued. */
    val startAt: String = "",
    /** Goes live as the row before it finishes. */
    val followsPrevious: Boolean = false,
    /** 1 once, 0 until something else goes live, N that many times. */
    val repeats: Int = 1,
    /** A [RowEnd] constant. */
    val atEnd: String = RowEnd.HOLD,
) {
    fun startsOnItsOwn(): Boolean = startAt.isNotEmpty()
    fun startsWithoutCue(): Boolean = startsOnItsOwn() || followsPrevious
    fun loops(): Boolean = repeats == 0
    fun isDefault(): Boolean = this == DEFAULT

    companion object {
        val DEFAULT = RowTiming()
    }
}

object RowEnd {
    const val HOLD = "hold"
    const val NEXT = "next"
    const val BLANK = "blank"
}

enum class ServiceKind(val id: String, val colorHex: String) {
    SUNDAY("sunday", "#5B9DF5"),
    MIDWEEK("midweek", "#A78BFA"),
    SPECIAL("special", "#E8A33D");

    companion object {
        fun byId(id: String): ServiceKind = entries.firstOrNull { it.id == id } ?: SUNDAY
    }
}

/** The colors a section heading can carry, as the desktop stores them: a hex string on the row. */
object SectionPalette {
    const val SKY = "#7DD3FC"
    const val BLUE = "#5B9DF5"
    const val AMBER = "#E8A33D"
    const val GREEN = "#86EFAC"
    const val VIOLET = "#A78BFA"
    const val ROSE = "#F87171"
    const val DEFAULT = SKY
    val ALL = listOf(SKY, BLUE, AMBER, GREEN, VIOLET, ROSE)
}

/** A saved run of show offered under "Start from" when a service is added. */
@Serializable
data class SavedTemplate(
    val id: String,
    val name: String,
    val startTime: String = "",
    val kind: String = ServiceKind.SUNDAY.id,
    val rows: List<PlanRow> = emptyList(),
    val plannedSeconds: Map<String, Int> = emptyMap(),
    val timing: Map<String, RowTiming> = emptyMap(),
)

/** A desktop preset as the phone sees it: enough to pick, nothing to open. */
@Serializable
data class PresetSummary(
    val id: String,
    val name: String,
    val kind: String = RowKind.PRESET,
    val detail: String = "",
)
