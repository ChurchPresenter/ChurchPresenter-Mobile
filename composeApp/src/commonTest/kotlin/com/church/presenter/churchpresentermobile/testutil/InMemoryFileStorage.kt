package com.church.presenter.churchpresentermobile.testutil

import com.church.presenter.churchpresentermobile.library.FileStore

/**
 * In-memory [FileStore] for tests, mirroring [InMemorySettingsStorage].
 *
 * [failWrites] and [corrupt] exist so the repository's recovery paths — a phone
 * killed mid-write, a truncated file — are exercised rather than assumed.
 */
class InMemoryFileStorage(initial: Map<String, String> = emptyMap()) : FileStore {
    private val files = initial.toMutableMap()

    /** Simulates a full disk or a permission failure. */
    var failWrites: Boolean = false

    val writeCount: Int get() = writes

    private var writes = 0

    override fun read(name: String): String? = files[name]

    override fun write(name: String, text: String) {
        if (failWrites) return
        writes++
        files[name] = text
    }

    override fun delete(name: String) {
        files.remove(name)
    }

    override fun list(): List<String> = files.keys.toList()

    override fun sizeBytes(name: String): Long = files[name]?.length?.toLong() ?: 0L

    /** Replaces a file's contents with something unparseable. */
    fun corrupt(name: String) {
        files[name] = "{ this is not json"
    }

    fun contains(name: String): Boolean = name in files
}
