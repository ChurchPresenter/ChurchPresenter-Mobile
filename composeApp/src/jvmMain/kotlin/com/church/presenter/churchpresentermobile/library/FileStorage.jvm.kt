package com.church.presenter.churchpresentermobile.library

/**
 * In-memory library store — a test host has nothing to persist between runs.
 *
 * An object expression rather than a named class: the factory below is the only
 * thing that ever builds one, and this file is named for the `expect` it
 * completes rather than for what is inside it.
 */
actual fun createFileStore(): FileStore = object : FileStore {
    private val files = mutableMapOf<String, String>()

    override fun read(name: String): String? = files[name]
    override fun write(name: String, text: String) { files[name] = text }
    override fun delete(name: String) { files.remove(name) }
    override fun list(): List<String> = files.keys.toList()
    override fun sizeBytes(name: String): Long = files[name]?.length?.toLong() ?: 0L
}
