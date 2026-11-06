package com.church.presenter.churchpresentermobile.signage

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.churchpresenter.signage.core.Manifest
import org.churchpresenter.signage.core.PROTOCOL_VERSION
import org.churchpresenter.signage.core.SignageJson
import java.io.IOException
import java.io.InputStream

/** Where manifests and files come from: ChurchPresenter over HTTP, or a fake in tests. */
internal interface ContentSource {
    /** This player's manifest and its version. */
    suspend fun manifest(): Pair<String, Manifest>

    /** Streams the file named [sha256] to [consume]; throws [IOException] when it cannot. */
    suspend fun <T> file(sha256: String, consume: (InputStream) -> T): T
}

/** How a sync ended. */
internal sealed interface SyncResult {
    data class Updated(val version: String, val downloaded: Int) : SyncResult

    data object UpToDate : SyncResult

    /** Nothing changed on disk; the TV goes on playing what it has. */
    data class Failed(val reason: String) : SyncResult
}

/**
 * The safe swap, as the Pi player does it: fetch the manifest, download only the
 * files missing here (each checked against its hash), and switch only when all
 * are present. Cut off at any step, the old manifest plays on.
 *
 * A manifest from any owner but this screen's is refused, so a second
 * ChurchPresenter on the network cannot take the screen over.
 */
internal class ManifestSync(private val store: SignageStore, private val source: ContentSource) {

    suspend fun sync(): SyncResult {
        val (version, manifest) = try {
            source.manifest()
        } catch (e: IOException) {
            return SyncResult.Failed("manifest: ${e.message}")
        }
        val owner = store.identity().ownerId
        return when {
            manifest.ownerId != owner -> SyncResult.Failed("manifest from ${manifest.ownerId}, owner is $owner")
            manifest.protocol > PROTOCOL_VERSION -> SyncResult.Failed("manifest protocol ${manifest.protocol}")
            store.manifest()?.version == version -> SyncResult.UpToDate
            else -> download(version, manifest)
        }
    }

    private suspend fun download(version: String, manifest: Manifest): SyncResult {
        val missing = manifest.fileHashes().filterNot(store.files::has)
        val failed = missing.firstNotNullOfOrNull { sha -> fetch(sha) }
        return when {
            failed != null -> SyncResult.Failed(failed)
            !store.commit(StoredManifest(version, manifest)) -> SyncResult.Failed("files missing after download")
            else -> SyncResult.Updated(version, missing.size)
        }
    }

    /** Downloads one file; why it could not, or null when it is here. */
    private suspend fun fetch(sha: String): String? = try {
        if (source.file(sha) { store.files.put(sha, it) }) null else "file $sha did not match its hash"
    } catch (e: IOException) {
        "file $sha: ${e.message}"
    }
}

/** Manifests and files from ChurchPresenter's companion server at [baseUrl] (`http://host:port`). */
internal class HttpContentSource(
    private val client: HttpClient,
    private val baseUrl: String,
    private val token: String,
) : ContentSource {

    override suspend fun manifest(): Pair<String, Manifest> {
        val response = client.get("$baseUrl/api/signage/manifest") { bearerAuth(token) }
        response.requireOk()
        val version = response.headers[HttpHeaders.ETag]?.trim('"') ?: throw IOException("manifest has no version")
        val manifest = try {
            SignageJson.decodeFromString(Manifest.serializer(), response.bodyAsText())
        } catch (e: IllegalArgumentException) {
            throw IOException("unreadable manifest", e)
        }
        return version to manifest
    }

    override suspend fun <T> file(sha256: String, consume: (InputStream) -> T): T =
        client.prepareGet("$baseUrl/api/signage/files/$sha256") { bearerAuth(token) }.execute { response ->
            response.requireOk()
            response.bodyAsChannel().toInputStream().use(consume)
        }

    private fun HttpResponse.requireOk() {
        if (!status.isSuccess()) throw IOException("HTTP ${status.value}")
    }
}
