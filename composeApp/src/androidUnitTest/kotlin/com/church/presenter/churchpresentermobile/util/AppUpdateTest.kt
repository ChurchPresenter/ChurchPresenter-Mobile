package com.church.presenter.churchpresentermobile.util

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.UpdateAvailability
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Whether, and how, the app offers a Play Store update.
 *
 * Two decisions matter. A *flexible* update downloads in the background and
 * lets the operator keep working; an *immediate* one takes the whole screen, so
 * it is only right when Play refuses the flexible kind or when a previous
 * immediate update was interrupted mid-download. Getting that wrong means a
 * full-screen updater in front of someone about to start a service.
 *
 * The other is the failure path. Most installs of this app are sideloaded APKs
 * (see TESTING.md) and emulators, where there is no Play Store to ask. Those
 * answers are the app's normal state, not defects, and must not be reported as
 * non-fatals — that is how the Crashlytics dashboard fills with noise.
 */
class AppUpdateTest {

    @AfterTest
    fun unmock() = unmockkAll()

    private val activity = mockk<ComponentActivity>(relaxed = true)
    private val launcher = mockk<ActivityResultLauncher<IntentSenderRequest>>(relaxed = true)
    private val manager = mockk<AppUpdateManager>(relaxed = true)

    /** Play answers with an update in the given state. */
    private fun playAnswers(availability: Int, flexibleAllowed: Boolean = true) {
        val info = mockk<AppUpdateInfo> {
            every { updateAvailability() } returns availability
            every { isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) } returns flexibleAllowed
            every { isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) } returns true
        }
        val task = mockk<Task<AppUpdateInfo>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<AppUpdateInfo>>()) } answers {
            firstArg<OnSuccessListener<AppUpdateInfo>>().onSuccess(info)
            task
        }
        every { task.addOnFailureListener(any()) } returns task
        install(task)
    }

    /** Play cannot be reached at all. */
    private fun playFails(error: Exception) {
        val task = mockk<Task<AppUpdateInfo>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<AppUpdateInfo>>()) } returns task
        every { task.addOnFailureListener(any()) } answers {
            firstArg<OnFailureListener>().onFailure(error)
            task
        }
        install(task)
    }

    private fun install(task: Task<AppUpdateInfo>) {
        every { manager.appUpdateInfo } returns task
        mockkStatic(AppUpdateManagerFactory::class)
        every { AppUpdateManagerFactory.create(any()) } returns manager
    }

    private fun check() = AppUpdate.checkAndPrompt(activity, launcher)

    /** The update type the flow was actually launched with. */
    private fun launchedType(): Int {
        val options = slot<AppUpdateOptions>()
        verify { manager.startUpdateFlowForResult(any(), launcher, capture(options)) }
        return options.captured.appUpdateType()
    }

    // ── Which flow is offered ────────────────────────────────────────────

    @Test
    fun `an available update is offered in the background`() {
        // Flexible: the download runs while the operator carries on.
        playAnswers(UpdateAvailability.UPDATE_AVAILABLE)

        check()

        assertEquals(AppUpdateType.FLEXIBLE, launchedType())
    }

    @Test
    fun `an update Play will not allow in the background takes the screen`() {
        // Play refuses flexible for some staleness/priority combinations; the
        // alternative to a full-screen updater is no update at all.
        playAnswers(UpdateAvailability.UPDATE_AVAILABLE, flexibleAllowed = false)

        check()

        assertEquals(AppUpdateType.IMMEDIATE, launchedType())
    }

    @Test
    fun `an interrupted immediate update is resumed as immediate`() {
        // The app was killed mid-download; leaving it half-applied is what makes
        // this the one case that has to reopen full-screen without asking.
        playAnswers(UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)

        check()

        assertEquals(AppUpdateType.IMMEDIATE, launchedType())
    }

    @Test
    fun `no update available prompts nothing`() {
        playAnswers(UpdateAvailability.UPDATE_NOT_AVAILABLE)

        check()

        verify(exactly = 0) { manager.startUpdateFlowForResult(any(), launcher, any<AppUpdateOptions>()) }
    }

    @Test
    fun `an unknown availability prompts nothing`() {
        // Play has added values before; anything unrecognised must be quiet.
        playAnswers(UpdateAvailability.UNKNOWN)

        check()

        verify(exactly = 0) { manager.startUpdateFlowForResult(any(), launcher, any<AppUpdateOptions>()) }
    }

    // ── When there is no Play Store to ask ───────────────────────────────

    @Test
    fun `a build with no Play Store is not reported as a crash`() {
        // Every sideloaded APK from the release workflow lands here.
        mockkObject(CrashReporting)
        playFails(installError(InstallErrorCode.ERROR_PLAY_STORE_NOT_FOUND))

        check()

        verify(exactly = 0) { CrashReporting.recordException(any()) }
    }

    @Test
    fun `an app the account does not own is not reported as a crash`() {
        // An emulator, or a device signed in as someone who never installed it.
        mockkObject(CrashReporting)
        playFails(installError(InstallErrorCode.ERROR_APP_NOT_OWNED))

        check()

        verify(exactly = 0) { CrashReporting.recordException(any()) }
    }

    @Test
    fun `any other install error is reported`() {
        // A genuine failure inside Play — worth seeing on the dashboard.
        mockkObject(CrashReporting)
        playFails(installError(InstallErrorCode.ERROR_INTERNAL_ERROR))

        check()

        verify { CrashReporting.recordException(any()) }
    }

    @Test
    fun `a failure that is not an install error at all is reported`() {
        mockkObject(CrashReporting)
        playFails(RuntimeException("something else went wrong"))

        check()

        verify { CrashReporting.recordException(any()) }
    }

    @Test
    fun `a failure never prompts for an update`() {
        playFails(installError(InstallErrorCode.ERROR_PLAY_STORE_NOT_FOUND))

        check()

        verify(exactly = 0) { manager.startUpdateFlowForResult(any(), launcher, any<AppUpdateOptions>()) }
    }

    private fun installError(code: Int): InstallException =
        mockk<InstallException>(relaxed = true) {
            every { errorCode } returns code
        }
}
