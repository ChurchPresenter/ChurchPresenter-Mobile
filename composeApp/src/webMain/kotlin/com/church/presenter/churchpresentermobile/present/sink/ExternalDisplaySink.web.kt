package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.present.OutputSink

// A browser tab cannot address a second physical display, so there is nothing
// to register. Declared once on the shared webMain source set — it satisfies
// the expect for both js and wasmJs.
actual fun createExternalDisplaySink(): OutputSink? = null
