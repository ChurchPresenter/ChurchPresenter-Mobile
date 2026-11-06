package com.church.presenter.churchpresentermobile.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.church.presenter.churchpresentermobile.MainActivity
import com.church.presenter.churchpresentermobile.model.AppSettings

/**
 * Opens the app when the TV starts, so a lobby TV comes back from a power cut with
 * nobody touching it. Does nothing unless someone turned it on, on the TV's home
 * screen, and the TV was showing something; a phone never turns it on.
 */
class TvBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!AppSettings().tvResumesOnBoot) return
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
