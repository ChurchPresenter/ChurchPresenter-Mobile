package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.calendar.parseStoredDate
import com.church.presenter.churchpresentermobile.calendar.parseStoredTime
import com.church.presenter.churchpresentermobile.calendar.today
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.PresetSummary
import com.church.presenter.churchpresentermobile.model.RowEnd
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.model.SectionPalette
import com.church.presenter.churchpresentermobile.model.ServiceKind
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * What a service has to pass before it is kept on this phone, whether it was typed here, read
 * from disk or pulled from the relay: strings cleaned and capped, numbers clamped, dates and
 * times checked, rows that make no sense dropped.
 */
object Sanitize {
    private const val NAME_CHARS = 120
    private const val TITLE_CHARS = 200
    private const val DETAIL_CHARS = 200
    private const val ID_CHARS = 64
    private const val ROWS_PER_SERVICE = 200
    private const val MAX_SECONDS = 24 * 60 * 60
    private const val MAX_REPEATS = 99
    private const val RETENTION_DAYS = 90
    private const val HORIZON_DAYS = 2 * 366
    private const val PRESETS_MAX = 500
    private const val BOOKS_IN_BIBLE = 66

    private val SPACES = Regex("\\s+")
    private val HEX_COLOR = Regex("^#[0-9A-Fa-f]{6}$")
    private val ID = Regex("^[A-Za-z0-9_.:-]{1,$ID_CHARS}$")
    private val ROW_ENDS = setOf(RowEnd.HOLD, RowEnd.NEXT, RowEnd.BLANK)
    private val INSTANT = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?Z$")
    private val RELAY_URL = Regex("^https://[A-Za-z0-9.-]+(:\\d{1,5})?$")
    private val SECRET = Regex("^[A-Za-z0-9_-]{16,128}$")
    /** How many records one pull is allowed to hand this phone. */
    const val RECORDS_PER_PULL = 2_000

    /** Control characters and the invisible ones that disguise text removed, whitespace collapsed, capped. */
    fun text(value: String, max: Int): String =
        stripDisguising(value).replace(SPACES, " ").trim().take(max)

    fun isId(value: String): Boolean = ID.matches(value)

    /**
     * The service as this phone will keep it, or null when its header is not something to keep.
     * [fromRelay] adds the relay's retention window; a service typed here may be planned further out.
     */
    fun service(service: PlannedService, fromRelay: Boolean = true, now: LocalDate = today()): PlannedService? {
        if (!isId(service.id)) return null
        val date = parseStoredDate(service.date) ?: return null
        if (fromRelay) {
            val earliest = now.minus(RETENTION_DAYS, DateTimeUnit.DAY)
            val latest = now.plus(HORIZON_DAYS, DateTimeUnit.DAY)
            if (date < earliest || date > latest) return null
        }
        val time = parseStoredTime(service.startTime) ?: return null

        val seen = HashSet<String>()
        val rows = service.rows.take(ROWS_PER_SERVICE).mapNotNull { row ->
            if (!isId(row.id) || !seen.add(row.id)) null else row(row)
        }
        val ids = rows.mapTo(HashSet()) { it.id }
        return service.copy(
            date = date.toString(),
            startTime = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}",
            name = text(service.name, NAME_CHARS).ifEmpty { DEFAULT_NAME },
            kind = ServiceKind.byId(service.kind).id,
            rows = rows,
            plannedSeconds = service.plannedSeconds.filterKeys { it in ids }.mapValues { (
                _,
                s,
            ) -> s.coerceIn(0, MAX_SECONDS) },
            timing = service.timing.filterKeys { it in ids }.mapValues { (
                _,
                t,
            ) -> timing(t) }.filterValues { !it.isDefault() },
            seriesId = service.seriesId.takeIf(::isId).orEmpty(),
            updatedAt = instant(service.updatedAt),
            editedAt = instant(service.editedAt),
            version = service.version.coerceAtLeast(0L),
            rev = service.rev.coerceAtLeast(0L),
        )
    }

    /** An ISO-8601 instant as the relay stamps them, or blank. */
    fun instant(value: String): String = if (INSTANT.matches(value)) value else ""

    /** A relay URL this phone will talk to: TLS, a host, nothing else. */
    fun relayUrl(value: String): String? {
        val trimmed = value.trim().trimEnd('/')
        return trimmed.takeIf { RELAY_URL.matches(it) }
    }

    /** A token or key as the QR carries them: URL-safe base64, bounded. */
    fun secret(value: String): String? = value.takeIf { SECRET.matches(it) }

    fun presets(presets: List<PresetSummary>): List<PresetSummary> = presets
        .filter { isId(it.id) }
        .take(PRESETS_MAX)
        .map { it.copy(
            name = text(it.name, TITLE_CHARS).ifEmpty { DEFAULT_PRESET },
            kind = text(it.kind, ID_CHARS),
            detail = text(it.detail, DETAIL_CHARS),
        ) }

    private fun row(row: PlanRow): PlanRow = when (row) {
        is PlanRow.Section -> row.copy(
            title = text(row.title, TITLE_CHARS).ifEmpty { DEFAULT_SECTION },
            color = if (HEX_COLOR.matches(row.color)) row.color else SectionPalette.DEFAULT,
        )
        is PlanRow.Song -> row.copy(
            title = text(row.title, TITLE_CHARS).ifEmpty { DEFAULT_SONG },
            songId = text(row.songId, TITLE_CHARS),
            songbook = text(row.songbook, TITLE_CHARS),
            number = text(row.number, ID_CHARS),
        )
        is PlanRow.Bible -> row.copy(
            title = text(row.title, TITLE_CHARS).ifEmpty { DEFAULT_REFERENCE },
            preview = text(row.preview, DETAIL_CHARS),
            bookId = if (row.bookId in 1..BOOKS_IN_BIBLE) row.bookId else 0,
        )
        is PlanRow.Ministry -> row.copy(
            title = text(row.title, TITLE_CHARS).ifEmpty { DEFAULT_MINISTRY },
            detail = text(row.detail, DETAIL_CHARS),
        )
        is PlanRow.Preset -> row.copy(
            title = text(row.title, TITLE_CHARS).ifEmpty { DEFAULT_PRESET },
            presetId = text(row.presetId, ID_CHARS),
            kind = text(row.kind, ID_CHARS),
        )
        is PlanRow.Ref -> row.copy(
            title = text(row.title, TITLE_CHARS),
            kind = text(row.kind, ID_CHARS),
            subtitle = text(row.subtitle, DETAIL_CHARS),
        )
    }

    private fun timing(timing: RowTiming): RowTiming = RowTiming(
        startAt = timing.startAt.takeIf { it.isEmpty() || parseStoredTime(it) != null }.orEmpty(),
        followsPrevious = timing.followsPrevious && timing.startAt.isEmpty(),
        repeats = timing.repeats.coerceIn(0, MAX_REPEATS),
        atEnd = if (timing.atEnd in ROW_ENDS) timing.atEnd else RowEnd.HOLD,
    )

    private const val DEFAULT_NAME = "Service"
    private const val DEFAULT_SECTION = "Section"
    private const val DEFAULT_SONG = "Song"
    private const val DEFAULT_REFERENCE = "Reference"
    private const val DEFAULT_MINISTRY = "Ministry"
    private const val DEFAULT_PRESET = "Preset"
}

/**
 * Control characters plus bidi marks/overrides/isolates, zero-width space, word joiner, BOM,
 * invisible operators and tag characters. Deliberately NOT every format character: the
 * zero-width joiner and non-joiner (U+200D, U+200C) are part of how Persian, Hindi, Nepali and
 * other scripts, and emoji sequences, are spelled. Written as a scan, not a regex, so the same
 * rule holds on every target (the desktop applies the same one).
 */
private fun stripDisguising(value: String): String = buildString(value.length) {
    var i = 0
    while (i < value.length) {
        val c = value[i]
        // U+E0000..E007F, the tag block, is the surrogate pair DB 40 / DC 00..7F.
        if (c == '\uDB40' && i + 1 < value.length && value[i + 1] in '\uDC00'..'\uDC7F') {
            i += 2
            continue
        }
        if (!isDisguising(c)) append(c)
        i++
    }
}

private fun isDisguising(c: Char): Boolean =
    c.category == CharCategory.CONTROL ||
        c == '\u200B' || c == '\u200E' || c == '\u200F' || c == '\u061C' || c == '\u180E' || c == '\uFEFF' ||
        c in '\u202A'..'\u202E' || c in '\u2060'..'\u2069' || c in '\uFFF9'..'\uFFFB'
