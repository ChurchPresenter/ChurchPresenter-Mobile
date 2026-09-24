package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.calendar.CalendarRepository
import com.church.presenter.churchpresentermobile.calendar.nowIso
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.io.IOException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "CalendarSync"
private const val NAME_CHARS = 120

/** 2,000 rows a page: far beyond what the relay holds, so this only stops a runaway. */
private const val MAX_PULL_PAGES = 50

/** Where this phone stands with the relay. */
sealed class SyncStatus {
    data object NotEnrolled : SyncStatus()
    data object Syncing : SyncStatus()
    data class Synced(val at: String, val pulled: Int, val pushed: Int) : SyncStatus()
    data class Failed(val message: String) : SyncStatus()
    /** The relay no longer accepts this phone: revoked, or the desktop started over with a new key. */
    data object Unauthorized : SyncStatus()
}

/**
 * One round of keeping this phone's `calendar.json` and the relay in step: replay what was edited
 * here, then take what changed there. A write the relay refuses as stale gives way to the other
 * copy, which the next pull brings.
 */
class CalendarSyncEngine(
    private val repository: CalendarRepository,
    private val state: () -> CalendarSyncState,
    private val saveState: (CalendarSyncState) -> Unit,
    private val clientKeys: ClientKeySource,
    private val pushToken: () -> String = { "" },
    /** What this phone calls itself, for the desktop's list of who is enrolled. */
    private val deviceName: () -> String = { "" },
    private val clientFor: (CalendarSyncState, suspend () -> String) -> RelayClient = { s, k -> RelayClient(s, k) },
    /** Where the desktop's songbooks land when they arrive through the relay. */
    private val catalogStore: SongCatalogStore? = null,
) {
    private val _status = MutableStateFlow<SyncStatus>(
        if (state().isEnrolled) SyncStatus.Synced("", 0, 0) else SyncStatus.NotEnrolled,
    )
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val lock = Mutex()

    val isEnrolled: Boolean get() = state().isEnrolled

    /** Forgets the enrollment on this phone; the desktop's Revoke does the same at the relay. */
    fun leave() {
        saveState(CalendarSyncState())
        _status.value = SyncStatus.NotEnrolled
    }

    suspend fun sync(): Boolean = lock.withLock {
        val current = state()
        if (!current.isEnrolled) {
            _status.value = SyncStatus.NotEnrolled
            return@withLock false
        }
        val sealing = Sealing.fromEncodedKey(current.instanceKey, current.instanceId)
        if (sealing == null) {
            _status.value = SyncStatus.Unauthorized
            return@withLock false
        }
        _status.value = SyncStatus.Syncing
        try {
            val client = clientFor(current, clientKeys::current)
            try {
                round(client, sealing, current)
            } catch (rotated: RelayFailure.ClientKey) {
                // The key rotated under us: fetch the new one and go once more.
                if (clientKeys.refresh() == null) throw rotated
                round(client, sealing, state())
            }
            true
        } catch (refused: RelayFailure.Unauthorized) {
            Logger.e(TAG, "the relay no longer accepts this phone: ${refused.message}")
            _status.value = SyncStatus.Unauthorized
            false
        } catch (e: RelayFailure) {
            Logger.e(TAG, "sync failed: ${e.message}")
            _status.value = SyncStatus.Failed(e.message.orEmpty())
            false
        } catch (e: IllegalStateException) {
            // Whatever the wire handed us could not be made sense of. A round is background work:
            // it reports and waits for the next one rather than taking the app down.
            Logger.e(TAG, "sync failed: ${e.message}", e)
            _status.value = SyncStatus.Failed(e.message.orEmpty())
            false
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG, "sync failed: ${e.message}", e)
            _status.value = SyncStatus.Failed(e.message.orEmpty())
            false
        } catch (e: IOException) {
            // The network itself: no route, a dropped WiFi, a captive portal, a certificate the
            // device's clock says is not valid yet. Uncaught, one took the app down.
            Logger.e(TAG, "sync failed: ${e.message}", e)
            _status.value = SyncStatus.Failed(e.message.orEmpty())
            false
        }
    }

    private suspend fun round(client: RelayClient, sealing: Sealing, current: CalendarSyncState) {
        registerPushIfChanged(client, current)
        registerNameIfChanged(client, sealing, current)
        val pushed = push(client, sealing)
        val pulled = pull(client, sealing, state(), pushed)
        _status.value = SyncStatus.Synced(nowIso(), pulled, pushed.size)
    }

    private suspend fun registerPushIfChanged(client: RelayClient, current: CalendarSyncState) {
        val token = pushToken()
        if (token.isBlank() || token == current.registeredPushToken) return
        runCatching { client.registerPushToken(token) }
            .onSuccess { saveState(state().copy(registeredPushToken = token)) }
            .onFailure { Logger.e(TAG, "push token not registered: ${it.message}") }
    }

    /**
     * A phone enrolled from an invite has no name at the relay until it says one. Sent once, and
     * again only when the name changes; a phone enrolled the old way has no device id to seal
     * under and keeps the name the desktop gave it.
     */
    private suspend fun registerNameIfChanged(client: RelayClient, sealing: Sealing, current: CalendarSyncState) {
        val name = deviceName().trim().take(NAME_CHARS)
        if (current.deviceId.isEmpty() || name.isEmpty() || name == current.registeredName) return
        runCatching { client.registerName(sealing.sealText(name, current.deviceId)) }
            .onSuccess { saveState(state().copy(registeredName = name)) }
            .onFailure { Logger.e(TAG, "name not registered: ${it.message}") }
    }

    /** Replays local edits and deletes. A stale write is dropped here and the pull below brings the winner. */
    private suspend fun push(client: RelayClient, sealing: Sealing): Set<String> {
        val doc = repository.document.value
        val done = HashSet<String>()
        for (id in doc.pendingPush) {
            val service = doc.serviceById(id) ?: run { done += id; continue }
            try {
                client.putRecord(sealing.seal(service), ifRev = service.rev)
            } catch (_: RelayFailure.Conflict) {
                Logger.d(TAG, "push $id — relay has a newer copy, taking theirs")
            }
            done += id
        }
        val deleted = HashSet<String>()
        for (id in doc.pendingDeletes) {
            runCatching { client.deleteRecord(id) }
                .onFailure { if (it !is RelayFailure.Conflict) throw it }
            deleted += id
        }
        repository.applySync(
            accepted = emptyMap(),
            removed = emptySet(),
            presets = null,
            pushedIds = done,
            deletedIds = deleted,
        )
        return done
    }

    private suspend fun pull(
        client: RelayClient,
        sealing: Sealing,
        current: CalendarSyncState,
        pushed: Set<String>,
        page: Int = 1,
    ): Int {
        val changes = client.changes(current.cursor)
        if (changes.rev < current.cursor) throw RelayFailure.Rejected(0, "revision went backwards")
        val accepted = HashMap<String, PlannedService>()
        val catalog = HashMap<String, CatalogRecord>()
        var unreadable = 0
        var rejected = 0
        for (record in changes.records.take(Sanitize.RECORDS_PER_PULL)) {
            when {
                record.id.startsWith(CATALOG_PREFIX) ->
                    sealing.openCatalog(record)?.let { catalog[record.id] = it } ?: unreadable++
                else -> {
                    val opened = sealing.open(record)
                    val service = opened?.let(Sanitize::service)
                    when {
                        opened == null -> unreadable++
                        service == null -> rejected++
                        else -> accepted[service.id] = service
                    }
                }
            }
        }
        if (unreadable > 0) Logger.e(TAG, "pull — $unreadable records did not open under this key")
        if (rejected > 0) Logger.e(TAG, "pull — $rejected records were not services this phone keeps")
        val gone = changes.tombstones.map { it.id }.filter(Sanitize::isId).toSet()
        val removed = gone.filterNot { it.startsWith(CATALOG_PREFIX) }.toSet()
        catalogStore?.merge(catalog, gone.filter { it.startsWith(CATALOG_PREFIX) }.toSet())
        val presets = changes.presetsBox.takeIf { it.isNotEmpty() }
            ?.let { box -> sealing.openPresets(box)?.presets?.let(Sanitize::presets) }
        // An edit made here while this round ran stays pending; a record we just pushed comes
        // back stamped by the relay and replaces our unstamped copy.
        val stillPending = repository.document.value.pendingPush - pushed
        val applied = accepted.filterKeys { it !in stillPending }
        repository.applySync(
            accepted = applied,
            removed = removed,
            presets = presets,
            pushedIds = emptySet(),
            deletedIds = emptySet(),
        )
        val next = current.copy(cursor = changes.rev, lastSyncAt = nowIso())
        saveState(next)
        val count = applied.size + removed.size
        if (!changes.more) return count
        // A full page: the rest is behind it. Stopping here would leave the cursor short, which is safe,
        // but the phone would look up to date until the next round.
        if (page >= MAX_PULL_PAGES) throw RelayFailure.Rejected(0, "relay has more changes than one round will read")
        return count + pull(client, sealing, next, pushed, page + 1)
    }
}
