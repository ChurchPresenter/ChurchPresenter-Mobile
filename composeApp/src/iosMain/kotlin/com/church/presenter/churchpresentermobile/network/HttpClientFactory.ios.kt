package com.church.presenter.churchpresentermobile.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret
import kotlinx.serialization.json.Json
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.credentialForTrust

@OptIn(ExperimentalForeignApi::class)
actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        engine {
            // Trust all certificates for self-signed HTTPS on local dev server.
            handleChallenge { _, _, challenge, completionHandler ->
                val space = challenge.protectionSpace
                if (space.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
                    @Suppress("UNCHECKED_CAST")
                    val opaquePtr = space.performSelector(
                        platform.objc.sel_registerName("serverTrust")
                    ) as? COpaquePointer
                    val credential = opaquePtr?.let {
                        @Suppress("UNCHECKED_CAST")
                        NSURLCredential.credentialForTrust(it.reinterpret())
                    }
                    // 0 = NSURLSessionAuthChallengeUseCredential
                    completionHandler(0, credential)
                } else {
                    // 1 = NSURLSessionAuthChallengePerformDefaultHandling
                    completionHandler(1, null)
                }
            }
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
@OptIn(ExperimentalForeignApi::class)
actual fun createActionHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        engine {
            handleChallenge { _, _, challenge, completionHandler ->
                val space = challenge.protectionSpace
                if (space.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
                    @Suppress("UNCHECKED_CAST")
                    val opaquePtr = space.performSelector(
                        platform.objc.sel_registerName("serverTrust")
                    ) as? COpaquePointer
                    val credential = opaquePtr?.let {
                        @Suppress("UNCHECKED_CAST")
                        NSURLCredential.credentialForTrust(it.reinterpret())
                    }
                    completionHandler(0, credential)
                } else {
                    completionHandler(1, null)
                }
            }
        }
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

@OptIn(ExperimentalForeignApi::class)
actual fun createImageHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        engine {
            handleChallenge { _, _, challenge, completionHandler ->
                val space = challenge.protectionSpace
                if (space.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
                    @Suppress("UNCHECKED_CAST")
                    val opaquePtr = space.performSelector(
                        platform.objc.sel_registerName("serverTrust")
                    ) as? COpaquePointer
                    val credential = opaquePtr?.let {
                        @Suppress("UNCHECKED_CAST")
                        NSURLCredential.credentialForTrust(it.reinterpret())
                    }
                    completionHandler(0, credential)
                } else {
                    completionHandler(1, null)
                }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        // No ContentNegotiation — lets Coil read raw image bytes without interference
    }
}

