package com.church.presenter.churchpresentermobile.ui.tv

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.model.AppSettings

/** Whether this device can be a signage player at all. */
expect val signageSupported: Boolean

/** The TV as a signage player. Draws nothing where [signageSupported] is false. */
@Composable
expect fun SignageMode(settings: AppSettings)
