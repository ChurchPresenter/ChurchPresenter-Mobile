package com.church.presenter.churchpresentermobile.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.PowerManager
import com.church.presenter.churchpresentermobile.testutil.RecordingContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The two locks the presentation service exists to hold.
 *
 * This is the most damaging thing in the app that can fail quietly. Without the
 * partial wake lock the CPU sleeps and the embedded server stops answering;
 * without the high-performance Wi-Fi lock a locked phone drops LAN sockets
 * after a minute or two and the TV falls back to its standby screen mid-song.
 * Neither shows up as an error anywhere — the screen simply goes.
 *
 * `PowerManager` and `WifiManager` are final and inert in the unit-test
 * `android.jar`, so they are mocked. The service is real: only
 * `buildNotification`, which needs a working `Notification.Builder`, is stubbed
 * so the start can get as far as taking the locks.
 */
class PresentationLocksTest {

    /** A started service, with the locks it took. */
    private class Started {
        val wakeLock = mockk<PowerManager.WakeLock>(relaxed = true)
        val wifiLock = mockk<WifiManager.WifiLock>(relaxed = true)
        val service = spyk(PresentationForegroundService(), recordPrivateCalls = true)

        /** The level the wake lock was asked for. */
        val wakeLockLevel = slot<Int>()

        /** The mode the Wi-Fi lock was asked for. */
        val wifiLockMode = slot<Int>()

        /** How long the wake lock was taken for. */
        val holdMillis = slot<Long>()

        init {
            val power = mockk<PowerManager> {
                every { newWakeLock(capture(wakeLockLevel), any()) } returns wakeLock
            }
            val wifi = mockk<WifiManager> {
                every { createWifiLock(capture(wifiLockMode), any()) } returns wifiLock
            }
            every { wakeLock.acquire(capture(holdMillis)) } returns Unit
            every { service["buildNotification"](any<String>()) } returns mockk<Notification>()
            every { service.getSystemService(Context.POWER_SERVICE) } returns power
            every { service.getApplicationContext() } returns mockk<Context> {
                every { getSystemService(Context.WIFI_SERVICE) } returns wifi
            }
        }

        fun start() = service.onStartCommand(Intent(), 0, 1)
    }

    @Test
    fun `starting takes a wake lock so the CPU cannot sleep under the server`() {
        val started = Started()

        started.start()

        verify { started.wakeLock.acquire(any()) }
    }

    @Test
    fun `the wake lock is partial, not one that keeps the screen on`() {
        // A full wake lock would hold the operator's display awake for the whole
        // service and drain the battery this is meant to protect.
        val started = Started()

        started.start()

        assertEquals(PowerManager.PARTIAL_WAKE_LOCK, started.wakeLockLevel.captured)
    }

    @Test
    fun `the wake lock is taken for six hours, not indefinitely`() {
        // A timeout is the safety net: a service killed without onDestroy would
        // otherwise hold the CPU awake until the phone was restarted. Six hours
        // comfortably outlasts any service.
        val started = Started()

        started.start()

        assertEquals(6 * 60 * 60 * 1000L, started.holdMillis.captured)
    }

    @Test
    fun `the Wi-Fi lock is the high-performance one`() {
        // The ordinary full lock still permits power-saving, which is what was
        // dropping the sockets in the first place.
        val started = Started()

        started.start()

        assertEquals(WifiManager.WIFI_MODE_FULL_HIGH_PERF, started.wifiLockMode.captured)
    }

    @Test
    fun `starting takes a Wi-Fi lock so LAN sockets survive a locked phone`() {
        // Wi-Fi power-saving is what drops the TV; this is the only thing that
        // stops it.
        val started = Started()

        started.start()

        verify { started.wifiLock.acquire() }
    }

    @Test
    fun `both locks ignore reference counting so one release is enough`() {
        // The service takes each lock once and drops it once. Reference counting
        // would leave a lock held after a restart took it a second time.
        val started = Started()

        started.start()

        verify { started.wakeLock.setReferenceCounted(false) }
        verify { started.wifiLock.setReferenceCounted(false) }
    }

    @Test
    fun `starting twice does not take a second pair of locks`() {
        // onStartCommand runs again whenever the Browser screen comes back into
        // view; stacking locks would leak them.
        val started = Started()

        started.start()
        started.start()

        verify(exactly = 1) { started.wakeLock.acquire(any()) }
        verify(exactly = 1) { started.wifiLock.acquire() }
    }

    @Test
    fun `stopping releases a held wake lock`() {
        val started = Started()
        every { started.wakeLock.isHeld } returns true
        started.start()

        started.service.onDestroy()

        verify { started.wakeLock.release() }
    }

    @Test
    fun `stopping releases a held Wi-Fi lock`() {
        val started = Started()
        every { started.wifiLock.isHeld } returns true
        started.start()

        started.service.onDestroy()

        verify { started.wifiLock.release() }
    }

    @Test
    fun `a lock that is no longer held is not released again`() {
        // Releasing an unheld lock throws; the isHeld check is what keeps
        // onDestroy from crashing after the system reclaimed one.
        val started = Started()
        every { started.wakeLock.isHeld } returns false
        every { started.wifiLock.isHeld } returns false
        started.start()

        started.service.onDestroy()

        verify(exactly = 0) { started.wakeLock.release() }
        verify(exactly = 0) { started.wifiLock.release() }
    }

    @Test
    fun `stopping twice does not release twice`() {
        // onDestroy can follow a stopSelf that already unwound the locks.
        val started = Started()
        every { started.wakeLock.isHeld } returns true
        every { started.wifiLock.isHeld } returns true
        started.start()

        started.service.onDestroy()
        started.service.onDestroy()

        verify(exactly = 1) { started.wakeLock.release() }
        verify(exactly = 1) { started.wifiLock.release() }
    }

    @Test
    fun `a device with no power manager still starts rather than crashing`() {
        // getSystemService can answer null on an unusual device; losing the lock
        // degrades the feature, throwing here would kill the app.
        val service = spyk(PresentationForegroundService(), recordPrivateCalls = true)
        every { service["buildNotification"](any<String>()) } returns mockk<Notification>()
        every { service.getSystemService(Context.POWER_SERVICE) } returns null
        every { service.getApplicationContext() } returns RecordingContext()

        service.onStartCommand(Intent(), 0, 1)
        service.onDestroy()
    }
}
