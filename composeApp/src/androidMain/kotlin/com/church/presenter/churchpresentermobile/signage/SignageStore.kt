package com.church.presenter.churchpresentermobile.signage

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.churchpresenter.signage.core.Manifest
import org.churchpresenter.signage.core.ScreenOverride
import org.churchpresenter.signage.core.SignageJson
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

/** How much of a file is hashed at a time while it downloads. */
private const val COPY_BUFFER_BYTES = 64 * 1024

/**
 * Who this player is and who owns it. Made at first start; [token] and the owner
 * fields are filled by pairing and cleared by a release.
 */
@Serializable
internal data class PlayerIdentity(
    val playerId: String,
    val token: String? = null,
    val ownerId: String? = null,
    val ownerName: String? = null,
    /** The last address ChurchPresenter answered on, tried first on the next start. */
    val lastAddress: String? = null,
) {
    val isPaired: Boolean get() = token != null && ownerId != null

    fun released(): PlayerIdentity = copy(token = null, ownerId = null, ownerName = null)
}

/** A manifest with the version its `ETag` named. */
@Serializable
internal data class StoredManifest(val version: String, val manifest: Manifest)

/** Replaces [target] with [source] in one step: a power cut leaves one or the other, never half. */
private fun swapInto(source: File, target: File) {
    Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
}

/** The pictures and videos a TV holds, each named by its hash. */
internal class ContentFiles(private val dir: File) {

    init {
        dir.mkdirs()
    }

    fun has(sha256: String): Boolean = file(sha256).isFile

    fun file(sha256: String): File = File(dir, sha256)

    /**
     * Stores [input] as the file named [sha256], or nothing at all if it does not
     * hash to that. Hashed as it is written, so a video is never held in memory.
     */
    fun put(sha256: String, input: InputStream): Boolean {
        val temp = File(dir, "$sha256.part")
        val digest = MessageDigest.getInstance("SHA-256")
        temp.outputStream().use { out ->
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            var read = input.read(buffer)
            while (read >= 0) {
                digest.update(buffer, 0, read)
                out.write(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        val matches = digest.digest().joinToString("") { "%02x".format(it) } == sha256
        if (matches) swapInto(temp, file(sha256)) else temp.delete()
        return matches
    }

    /** Deletes everything but [wanted]. */
    fun retainOnly(wanted: Set<String>) {
        dir.listFiles().orEmpty().filter { it.name !in wanted }.forEach(File::delete)
    }
}

/**
 * What the TV keeps on disk, so it plays on with ChurchPresenter's computer
 * switched off — the whole point of signage.
 *
 * Plain files under [dir], each written whole and swapped into place.
 */
internal class SignageStore(private val dir: File) {

    val files = ContentFiles(File(dir, "files"))
    private val json = Json { ignoreUnknownKeys = true }

    /** Who this player is, made on first use. */
    fun identity(): PlayerIdentity =
        read("identity.json") { json.decodeFromString<PlayerIdentity>(it) }
            ?: PlayerIdentity(playerId = UUID.randomUUID().toString()).also(::saveIdentity)

    fun saveIdentity(identity: PlayerIdentity) =
        write("identity.json", json.encodeToString(PlayerIdentity.serializer(), identity))

    fun manifest(): StoredManifest? = read("manifest.json") {
        SignageJson.decodeFromString(StoredManifest.serializer(), it)
    }

    /** Switches to [stored], only once every file it names is here, and lets the rest go. */
    fun commit(stored: StoredManifest): Boolean {
        val wanted = stored.manifest.fileHashes()
        if (!wanted.all(files::has)) return false
        write("manifest.json", SignageJson.encodeToString(StoredManifest.serializer(), stored))
        files.retainOnly(wanted)
        return true
    }

    /** A manual switch from the Signage tab, kept so it holds with the computer off. */
    var override: ScreenOverride?
        get() = read("override.json") { SignageJson.decodeFromString(ScreenOverride.serializer(), it) }
        set(value) {
            if (value == null) {
                File(dir, "override.json").delete()
            } else {
                write("override.json", SignageJson.encodeToString(ScreenOverride.serializer(), value))
            }
        }

    /** Deletes the content, the manifest and any manual switch; stays paired. */
    fun wipe() {
        files.retainOnly(emptySet())
        File(dir, "manifest.json").delete()
        override = null
    }

    fun freeBytes(): Long = dir.usableSpace

    private fun <T> read(name: String, decode: (String) -> T): T? =
        runCatching { decode(File(dir, name).readText()) }.getOrNull()

    private fun write(name: String, text: String) {
        val temp = File(dir, "$name.part")
        temp.writeText(text)
        swapInto(temp, File(dir, name))
    }
}
