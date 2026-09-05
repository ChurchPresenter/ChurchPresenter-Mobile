package com.church.presenter.churchpresentermobile.present

/**
 * The phone's address on the local network, or `null` when it has none.
 *
 * This is the number the operator types into a TV browser, so it must be the
 * LAN address other devices can reach — not loopback, and not a mobile-data
 * address, which no TV in the room can route to.
 */
expect fun localIpAddress(): String?
