package com.church.presenter.churchpresentermobile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/** Returns a new random UUID string (e.g. "550e8400-e29b-41d4-a716-446655440000"). */
expect fun generateUUID(): String

