package com.church.presenter.churchpresentermobile.library

/**
 * Minimal per-platform file store for the on-device library.
 *
 * Deliberately five methods over whole documents rather than a database. The
 * library is one JSON document read at launch and written on edit; SQLDelight
 * was evaluated and rejected because it adds a Gradle plugin and codegen to a
 * build already juggling five targets, its generated API exists only in the
 * android/iOS source sets (forcing another expect layer), and the full-text
 * search it buys is illusory — the app already filters the whole song list in
 * memory. Keeping the document whole also makes the export format free.
 *
 * An interface plus a factory, matching
 * [com.church.presenter.churchpresentermobile.model.SettingsStorage] — an
 * `expect class` could not be faked in commonTest, and the recovery paths here
 * are exactly what needs testing.
 *
 * If a library ever outgrows this, swap [LibraryRepository]'s internals; callers
 * see no change.
 */
interface FileStore {

    /** Returns the file's contents, or null when it does not exist or cannot be read. */
    fun read(name: String): String?

    /**
     * Writes [text] to [name].
     *
     * Implementations must write atomically — a phone killed mid-write during a
     * service must not leave a half-written library behind.
     */
    fun write(name: String, text: String)

    /** Deletes [name] if it exists. */
    fun delete(name: String)

    /** Names of every file in the store. */
    fun list(): List<String>

    /** Size of [name] in bytes, or 0 when it does not exist. */
    fun sizeBytes(name: String): Long
}

/** Creates the platform-specific [FileStore] implementation. */
expect fun createFileStore(): FileStore
