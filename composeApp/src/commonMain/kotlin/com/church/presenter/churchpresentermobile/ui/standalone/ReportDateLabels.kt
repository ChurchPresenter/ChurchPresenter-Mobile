package com.church.presenter.churchpresentermobile.ui.standalone

import com.church.presenter.churchpresentermobile.model.ReportDates
import kotlinx.datetime.TimeZone

/** "Jan 4, 2026" for a timestamp, in the device's zone. */
internal fun reportDate(epochMs: Long): String =
    ReportDates.dateLabel(ReportDates.dayOf(epochMs, TimeZone.currentSystemDefault()))
