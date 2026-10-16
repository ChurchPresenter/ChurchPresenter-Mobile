package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.getAppContext
import com.church.presenter.churchpresentermobile.util.Logger
import java.io.File

private const val TAG = "FileStorage"
private const val DIRECTORY = "library"

/** Backed by the app's private files directory — no permissions, cleared on uninstall. */
class AndroidFileStore : FileStore {

    private val root: File?
        get() = getAppContext()?.filesDir?.let { File(it, DIRECTORY).apply { mkdirs() } }

    override fun read(name: String): String? = runCatching {
        val file = File(root ?: return null, name)
        if (file.exists()) file.readText() else null
    }.onFailure { Logger.e(TAG, "read '$name' failed: ${it.message}") }.getOrNull()

    override fun write(name: String, text: String) {
        runCatching {
            val dir = root ?: return
            // Write-then-rename: a process killed mid-write leaves the previous
            // good file intact rather than a truncated one.
            val temp = File(dir, "$name.tmp")
            temp.writeText(text)
            val target = File(dir, name)
            if (!temp.renameTo(target)) {
                target.writeText(text)
                temp.delete()
            }
        }.onFailure { Logger.e(TAG, "write '$name' failed: ${it.message}") }
    }

    override fun delete(name: String) {
        runCatching { File(root ?: return, name).delete() }
    }

    override fun list(): List<String> =
        root?.listFiles()?.filter { it.isFile }?.map { it.name }.orEmpty()

    override fun sizeBytes(name: String): Long = runCatching {
        File(root ?: return 0L, name).takeIf { it.exists() }?.length() ?: 0L
    }.getOrDefault(0L)
}

actual fun createFileStore(): FileStore = AndroidFileStore()
