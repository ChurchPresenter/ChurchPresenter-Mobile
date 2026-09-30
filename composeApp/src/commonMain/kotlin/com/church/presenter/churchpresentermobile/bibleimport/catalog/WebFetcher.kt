package com.church.presenter.churchpresentermobile.bibleimport.catalog

import com.church.presenter.churchpresentermobile.network.createImageHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes

/** One HTTP response, reduced to what the catalogues and the installer read. */
class WebResponse(
    val status: Int,
    /** Header names lower-cased. */
    val headers: Map<String, String>,
    val body: ByteArray,
) {
    val isSuccess: Boolean get() = status in HTTP_OK

    private companion object {
        val HTTP_OK = 200..299
    }
}

/**
 * Fetches a URL off the public internet.
 *
 * The seam the catalogue and the installer are tested through — the same shape as the
 * `serviceFactory` seams elsewhere in the app — so no test ever reaches eBible or GitHub.
 */
fun interface WebFetcher {
    /**
     * @param onProgress Bytes received so far, and the total when the server said.
     * @throws Exception a network failure. A response with an error status is not one.
     */
    suspend fun get(url: String, headers: Map<String, String>, onProgress: (Long, Long?) -> Unit): WebResponse
}

/**
 * The real fetcher, over its own client.
 *
 * Not the app's JSON client: that one gives up after twelve seconds, which a 7 MB Bible on church
 * Wi-Fi can easily outlast. Here the whole request may take as long as it needs, and only a
 * connection that stops delivering for [STALL_TIMEOUT_MS] is abandoned.
 */
class KtorWebFetcher(private val client: HttpClient = defaultClient) : WebFetcher {

    override suspend fun get(
        url: String,
        headers: Map<String, String>,
        onProgress: (Long, Long?) -> Unit,
    ): WebResponse {
        val response = client.get(url) {
            header("User-Agent", USER_AGENT)
            headers.forEach { (name, value) -> header(name, value) }
            onDownload { received, total -> onProgress(received, total) }
        }
        return WebResponse(
            status = response.status.value,
            headers = response.headers.entries().associate { (name, values) -> name.lowercase() to values.first() },
            body = response.readRawBytes(),
        )
    }

    private companion object {
        const val USER_AGENT = "ChurchPresenter-Mobile"
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val STALL_TIMEOUT_MS = 30_000L

        val defaultClient: HttpClient by lazy {
            createImageHttpClient().config {
                install(HttpTimeout) {
                    requestTimeoutMillis = null
                    connectTimeoutMillis = CONNECT_TIMEOUT_MS
                    socketTimeoutMillis = STALL_TIMEOUT_MS
                }
            }
        }
    }
}
