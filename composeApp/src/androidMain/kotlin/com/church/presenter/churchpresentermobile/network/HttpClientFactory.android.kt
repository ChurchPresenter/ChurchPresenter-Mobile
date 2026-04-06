package com.church.presenter.churchpresentermobile.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

actual fun createHttpClient(): HttpClient {
    // Trust all certificates for self-signed HTTPS (local dev server only).
    // OkHttp engine is used instead of the Android engine so this custom
    // SSLContext is honoured — the Android engine uses HttpURLConnection which
    // is intercepted by NetworkSecurityConfig before our trust manager runs.
    val trustAllCerts = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(trustAllCerts), SecureRandom())
    }

    val okHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts)
        .hostnameVerifier { _, _ -> true }
        .build()

    return HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
}

/**
 * Action client: connect timeout only — no request or socket timeout.
 * Used for approval-required POSTs (/api/schedule/add, /api/project) that
 * hold the connection open until the desktop user clicks Allow/Deny.
 */
actual fun createActionHttpClient(): HttpClient {
    val trustAllCerts = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(trustAllCerts), SecureRandom())
    }
    val okHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts)
        .hostnameVerifier { _, _ -> true }
        .build()

    return HttpClient(OkHttp) {
        engine { preconfigured = okHttpClient }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = null  // no timeout — waits for user Allow/Deny
            socketTimeoutMillis  = null  // no socket inactivity timeout
        }
        install(ContentNegotiation) {
            json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
        }
    }
}

/** Image-only client: same trust-all SSL as [createHttpClient] but no ContentNegotiation. */
actual fun createImageHttpClient(): HttpClient {
    val trustAllCerts = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(trustAllCerts), SecureRandom())
    }
    val okHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts)
        .hostnameVerifier { _, _ -> true }
        .build()

    return HttpClient(OkHttp) {
        engine { preconfigured = okHttpClient }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        // No ContentNegotiation — lets Coil read raw JPEG bytes without interference
    }
}

