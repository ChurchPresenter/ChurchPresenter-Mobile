package com.church.presenter.churchpresentermobile.calendar

import com.church.presenter.churchpresentermobile.calendar.sync.Sanitize
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

private const val TAG = "CalendarRepository"
internal const val CALENDAR_FILE = "calendar.json"

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
        commit { it.withService(clean.copy(updatedAt = now())).copy(pendingPush = it.pendingPush + clean.id) }
    }

    fun saveServices(services: List<PlannedService>) {
        val stamp = now()
        val clean = services.mapNotNull { Sanitize.service(it, fromRelay = false) }
        if (clean.isEmpty()) return
        commit {
            it.withServices(clean.map { service -> service.copy(updatedAt = stamp) })
                .copy(pendingPush = it.pendingPush + clean.map { s -> s.id })
        }
    }

    /**
     * What a sync round decided: services as the relay now holds them (written without marking
     * them pending), ids the relay no longer has, the desktop's preset index, and which pending
     * marks were accepted.
     */
    fun applySync(
        accepted: Map<String, PlannedService>,
        removed: Set<String>,
        presets: List<PresetSummary>?,
        pushedIds: Set<String>,
        deletedIds: Set<String>,
    ) {
        commit { doc ->
            val kept = doc.services.filterNot { it.id in removed }.map { accepted[it.id] ?: it }
            val added = accepted.values.filter { s -> kept.none { it.id == s.id } && s.id !in doc.pendingDeletes }
            doc.copy(
                services = kept + added,
                presets = presets ?: doc.presets,
                pendingPush = doc.pendingPush - pushedIds - removed,
                pendingDeletes = doc.pendingDeletes - deletedIds - removed,
                deletedServices = doc.deletedServices.filterKeys { it !in deletedIds },
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
