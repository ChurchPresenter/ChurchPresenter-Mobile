package com.church.presenter.churchpresentermobile

import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val os: String = "ios"
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun generateUUID(): String = NSUUID().UUIDString()

/**
 * The device's name.
 *
 * iOS 16 and later returns the model ("iPhone") rather than the user's own name
 * unless the app holds an entitlement Apple grants sparingly. Still better than
 * a UUID in an approval prompt, and the operator can override it in Settings —
 * which is exactly the case the custom-name field exists for.
 */
actual fun deviceName(): String = UIDevice.currentDevice.name

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
