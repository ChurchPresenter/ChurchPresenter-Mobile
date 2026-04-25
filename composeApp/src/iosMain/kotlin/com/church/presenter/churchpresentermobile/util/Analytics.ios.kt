package com.church.presenter.churchpresentermobile.util

actual object Analytics {

    actual fun init() {
        // Analytics auto-starts after FirebaseApp.configure() in iOSApp.swift
    }

    actual fun logEvent(name: String, params: Map<String, String>) {
        val sanitised = params.mapValues { (_, v) -> v.take(100) }
        IosAnalyticsBridge.reporter?.logEvent(name, sanitised)
            ?: println("[Analytics] $name $sanitised")
        Logger.d("Analytics", "logEvent: $name $sanitised")
    }
}

