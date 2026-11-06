package com.church.presenter.churchpresentermobile.tv

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

/** The mDNS service a phone advertises while it serves the display page. */
const val PHONE_SERVICE_TYPE = "_cpmobile._tcp"

/** The mDNS service ChurchPresenter on a computer advertises. */
const val DESKTOP_SERVICE_TYPE = "_churchpresenter._tcp"

/** The mDNS service a TV advertises, so a phone can send something to it. */
const val TV_SERVICE_TYPE = "_cptv._tcp"

/** Something advertising on the network. */
data class FoundService(val name: String, val host: String, val port: Int)

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
 * Finds what advertises [serviceType] on this network: phones and desktops for
 * a TV's picker, TVs for a phone's.
 *
 * [found] lists what is advertising now; whatever stops (locked, closed, left
 * the Wi-Fi) drops off the list.
 */
expect class ServiceFinder(serviceType: String) {
    val found: StateFlow<List<FoundService>>

    fun start()

    fun stop()
}

/**
 * The TV's end of "show this on the TV": a small listener a phone asks, and the
 * advert that lets the phone find it.
 *
 * [requested] holds what a phone last asked for, until [consume] takes it.
 * [home] counts requests to stop showing.
 */
expect class TvControlServer() {
    val requested: StateFlow<TvSource?>
    val home: StateFlow<Int>

    fun start()

    fun stop()

    fun consume()
}

/** Keeps the display awake while it is composed — a TV showing slides must never dim. */
@Composable
expect fun KeepScreenOn()
