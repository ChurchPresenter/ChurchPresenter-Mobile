package com.church.presenter.churchpresentermobile.calendar

import com.church.presenter.churchpresentermobile.calendar.sync.Sanitize
import com.church.presenter.churchpresentermobile.calendar.sync.edited
import com.church.presenter.churchpresentermobile.calendar.sync.receiving
import com.church.presenter.churchpresentermobile.calendar.sync.receivingDeletion
import com.church.presenter.churchpresentermobile.library.FileStore
import com.church.presenter.churchpresentermobile.library.createFileStore
import com.church.presenter.churchpresentermobile.model.CalendarDocument
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.PresetSummary
import com.church.presenter.churchpresentermobile.model.SavedTemplate
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private const val TAG = "CalendarRepository"
internal const val CALENDAR_FILE = "calendar.json"

/** How long a deletion is remembered, as on the desktop: long enough to refuse an old copy of it. */
private val DELETION_MEMORY = 90.days

/**
 * `calendar.json` on this device: every planned service, saved templates and the desktop's
 * preset index. The same shape as the desktop's own file and as what will travel to it, so
 * syncing is a merge of two documents rather than a conversion.
 *
 * Every mutation goes through [commit], which stamps the service and writes the file, so
 * "saved immediately" holds by construction.
 */
class CalendarRepository(
    private val storage: FileStore = createFileStore(),
    private val now: () -> String = ::nowIso,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        classDiscriminator = "type"
    }

    private val _document = MutableStateFlow(CalendarDocument.EMPTY)
    val document: StateFlow<CalendarDocument> = _document.asStateFlow()

    fun load(): CalendarDocument {
        val text = storage.read(CALENDAR_FILE)
        val loaded = if (text.isNullOrBlank()) {
            CalendarDocument.EMPTY
        } else {
            runCatching { json.decodeFromString(CalendarDocument.serializer(), text) }
                .onFailure { Logger.e(TAG, "calendar.json unreadable, starting empty: ${it.message}") }
                .getOrDefault(CalendarDocument.EMPTY)
                .let { doc ->
                    doc.copy(
                        services = doc.services.mapNotNull { Sanitize.service(it, fromRelay = false) },
                        presets = Sanitize.presets(doc.presets),
                    )
                }
        }
        _document.value = loaded
        return loaded
    }

    fun service(id: String): PlannedService? = _document.value.serviceById(id)

    /** Every save passes [Sanitize] first; a service the rules refuse outright is not saved. */
    fun saveService(service: PlannedService) {
        val clean = Sanitize.service(service, fromRelay = false) ?: return
        val before = _document.value.serviceById(clean.id)
        if (before == clean) return
        val stamp = now()
        commit { it.withService(it.edited(clean, stamp)).copy(pendingPush = it.pendingPush + clean.id) }
    }

    fun saveServices(services: List<PlannedService>) {
        val stamp = now()
        val clean = services.mapNotNull { Sanitize.service(it, fromRelay = false) }
        if (clean.isEmpty()) return
        commit {
            it.withServices(clean.map { service -> it.edited(service, stamp) })
                .copy(pendingPush = it.pendingPush + clean.map { s -> s.id })
        }
    }

    /**
     * What a sync round brought and what it sent. [services] and [deletions] from the relay are
     * merged by *Which copy wins* (SYNC.md): each replaces what is here only when it outranks it,
     * so the relay's own stamps and plaintext tombstones decide nothing. [pushedIds] and
     * [deletedIds] are the writes the relay accepted, no longer pending. A sent deletion is still
     * remembered, for 90 days: it is what refuses an old copy of the service handed back later.
     */
    fun applySync(
        services: List<PlannedService> = emptyList(),
        deletions: List<PlannedService> = emptyList(),
        presets: List<PresetSummary>? = null,
        pushedIds: Set<String> = emptySet(),
        deletedIds: Set<String> = emptySet(),
    ) {
        commit { start ->
            var doc = start.copy(
                pendingPush = start.pendingPush - pushedIds,
                pendingDeletes = start.pendingDeletes - deletedIds,
            )
            for (remote in services) doc = doc.receiving(remote)
            for (deletion in deletions) doc = doc.receivingDeletion(deletion)
            val cutoff = runCatching { (Instant.parse(now()) - DELETION_MEMORY).toString() }.getOrNull()
            val remembered = doc.deletedServices.filter { (id, at) ->
                cutoff == null || id in doc.pendingDeletes || at >= cutoff
            }
            doc.copy(
                presets = presets ?: doc.presets,
                deletedServices = remembered,
                deletedVersions = doc.deletedVersions.filterKeys { it in remembered },
                deletedRevs = doc.deletedRevs.filterKeys { it in remembered },
            )
        }
    }

    fun deleteService(id: String) {
        commit { it.withoutService(id, now()) }
    }

    fun saveTemplate(template: SavedTemplate) {
        commit { it.withTemplate(template) }
    }

    fun deleteTemplate(id: String) {
        commit { it.copy(templates = it.templates.filterNot { template -> template.id == id }) }
    }

    /** Replaces the whole document — what a sync or an import does. */
    fun replaceAll(document: CalendarDocument) {
        commit { document }
    }

    private fun commit(transform: (CalendarDocument) -> CalendarDocument) {
        val updated = transform(_document.value)
        _document.value = updated
        runCatching { storage.write(CALENDAR_FILE, json.encodeToString(CalendarDocument.serializer(), updated)) }
            .onFailure { Logger.e(TAG, "calendar.json write failed: ${it.message}") }
    }
}
