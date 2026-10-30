package com.church.presenter.churchpresentermobile.tv

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

/** The mDNS service a phone advertises while it serves the display page. */
const val PHONE_SERVICE_TYPE = "_cpmobile._tcp"

/**
 * Lets TVs on the network find this phone while it is presenting standalone.
 *
 * Without it the operator would have to read the phone's address off Settings
 * and type it with a TV remote — the one thing a lobby TV should never need.
 * A platform with no service discovery does nothing; the TV can still be given
 * the address by hand.
 */
expect object PhoneAdvert {
    /** Advertises the display server on [port]. Safe to call again with a new port. */
    fun start(port: Int)

    fun stop()
}

/**
 * Finds phones presenting standalone on this network, for the TV's picker.
 *
 * [found] lists what is advertising now; a phone that stops (locked, closed,
 * left the Wi-Fi) drops off the list.
 */
expect class PhoneFinder() {
    val found: StateFlow<List<PhoneSource>>

    fun start()

    fun stop()
}

/** Keeps the display awake while it is composed — a TV showing slides must never dim. */
@Composable
expect fun KeepScreenOn()
