package com.church.presenter.churchpresentermobile.present

// A browser is not told its own LAN address, and has no server to advertise.
actual fun localIpAddress(): String? = null
