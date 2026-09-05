package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.present.OutputSink

/** Stable id of the external-display sink within a registry. */
const val EXTERNAL_DISPLAY_SINK_ID = "external_display"

/**
 * Creates the platform's external-display sink, or `null` where there is none.
 *
 * "External display" is broader than a cable. On Android this one sink covers
 * wired HDMI/USB-C, Chromecast "Cast screen", and Miracast — the system surfaces
 * all of them as presentation displays. On iOS it covers AirPlay and wired
 * adapters, both of which arrive as an external window scene. None of it costs
 * anything or needs an SDK, which is why it ships before the phone-hosted web
 * page and long before the paid Cast integration.
 *
 * A factory rather than an `expect class` so the web target can simply return
 * `null` instead of carrying a no-op implementation of the whole interface.
 */
expect fun createExternalDisplaySink(): OutputSink?
