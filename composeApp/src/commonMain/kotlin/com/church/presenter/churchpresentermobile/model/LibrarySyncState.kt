package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/**
 * What happened the last time the library was pulled from a desktop.
 *
 * Persisted so the Library screen can answer "is this current?" without
 * touching the network — the question an operator asks on a Sunday morning.
 *
 * @param sourceHost The desktop this came from. A different host means the
 *   staleness figure is about someone else's catalogue.
 * @param keptLocalCount How many of the user's own edits were preserved rather
 *   than overwritten. Reported so the merge is visible rather than silent.
 */
@Serializable
data class LibrarySyncState(
    val lastSyncEpochMs: Long = 0L,
    val sourceHost: String = "",
    val songCount: Int = 0,
    val failedCount: Int = 0,
    val keptLocalCount: Int = 0,
) {
    val hasEverSynced: Boolean get() = lastSyncEpochMs > 0L

    /** True when the last sync did not manage to fetch everything it found. */
    val wasPartial: Boolean get() = failedCount > 0

    companion object {
        val NEVER: LibrarySyncState = LibrarySyncState()
    }
}

/** Live progress of a sync in flight. */
data class SyncProgress(
    val done: Int = 0,
    val total: Int = 0,
    val currentTitle: String = "",
    val isRunning: Boolean = false,
) {
    /** 0f..1f, or 0f before the total is known. */
    val fraction: Float get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)

    /**
     * Running, but the catalogue request hasn't answered yet so there is no
     * total to count towards. The UI must show an indeterminate indicator here:
     * rendering the usual "Copying 0 of 0…" at 0% makes a working sync look
     * frozen, which is exactly how it was reported.
     */
    val isPreparing: Boolean get() = isRunning && total <= 0

    companion object {
        val IDLE: SyncProgress = SyncProgress()
    }
}

/** How a finished sync turned out. */
sealed interface SyncOutcome {

    /** @param keptLocal Items skipped because the user had edited them. */
    data class Success(
        val songCount: Int,
        val failedCount: Int,
        val keptLocal: Int,
    ) : SyncOutcome

    /** The desktop could not be reached, or refused. Nothing was changed. */
    data class Failed(val message: String) : SyncOutcome

    /** The user stopped it. Whatever had been written is kept. */
    data class Cancelled(val songCount: Int) : SyncOutcome
}
