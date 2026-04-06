package com.church.presenter.churchpresentermobile

import platform.Foundation.NSUUID
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun generateUUID(): String = NSUUID().UUIDString()

