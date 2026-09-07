package com.church.presenter.churchpresentermobile.screenshot

import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.ui.standalone.ColorPickerDialog
import com.church.presenter.churchpresentermobile.ui.standalone.OutputTargetsContent
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneOutputScreen
import kotlin.test.Test

/**
 * What standalone mode puts on the screen in the room, and the sheets that
 * control it.
 *
 * [StandaloneOutputScreen] is the only composable in the app whose audience is
 * the congregation rather than the operator, so its states are the slide kinds:
 * words, a reading, a notice, a logo, and the blank a service starts and ends
 * on. Captured at a projector's shape rather than a phone's.
 *
 * [OutputTargetsContent] is the other half — every output the phone can drive,
 * in each state it can be in, including the ones that failed.
 */
class StandaloneOutputScreenshotTest {

    private val theme = com.church.presenter.churchpresentermobile.model.SlideTheme()

    private fun slide(
        kind: SlideKind,
        body: String,
        reference: String? = null,
        footer: String? = null,
        textSize: SlideTextSize = SlideTextSize.MEDIUM,
    ) = Slide(
        kind = kind,
        body = body,
        reference = reference,
        footer = footer,
        textSize = textSize,
        theme = theme,
    )

    // ── What the room sees ───────────────────────────────────────────────

    @Test
    fun songSlide() = screenshot(
        "output__song",
        width = Screenshots.TABLET_WIDTH,
        content = {
            StandaloneOutputScreen(
                slide = slide(
                    SlideKind.SONG,
                    "Amazing grace! how sweet the sound\nThat saved a wretch like me!",
                    reference = "Amazing Grace",
                ),
            )
        },
    )

    @Test
    fun bibleSlide() = screenshot(
        "output__bible",
        width = Screenshots.TABLET_WIDTH,
        content = {
            StandaloneOutputScreen(
                slide = slide(
                    SlideKind.BIBLE,
                    "For God so loved the world, that he gave his only begotten Son",
                    reference = "John 3:16",
                ),
            )
        },
    )

    @Test
    fun announcementSlide() = screenshot(
        "output__announcement",
        width = Screenshots.TABLET_WIDTH,
        content = {
            StandaloneOutputScreen(
                slide = slide(SlideKind.ANNOUNCEMENT, "Tea and coffee after the service", footer = "Welcome"),
            )
        },
    )

    @Test
    fun blankSlide() = screenshot(
        // How a service starts, ends, and pauses. Blank has to be genuinely
        // blank — a stray footer or clock here is on the wall for everyone.
        "output__blank",
        width = Screenshots.TABLET_WIDTH,
        content = { StandaloneOutputScreen(slide = slide(SlideKind.BLANK, "")) },
    )

    @Test
    fun logoSlide() = screenshot(
        "output__logo",
        width = Screenshots.TABLET_WIDTH,
        content = { StandaloneOutputScreen(slide = slide(SlideKind.LOGO, "")) },
    )

    @Test
    fun largeText() = screenshot(
        "output__large-text",
        width = Screenshots.TABLET_WIDTH,
        content = {
            StandaloneOutputScreen(
                slide = slide(
                    SlideKind.SONG,
                    "Amazing grace! how sweet the sound",
                    textSize = SlideTextSize.LARGE,
                ),
            )
        },
    )

    // ── The outputs it can be sent to ────────────────────────────────────

    @Test
    fun outputTargets() = screenshot("output-targets__mixed") {
        OutputTargetsContent(
            sinks = listOf(
                SinkStatus("web", "Web page", SinkState.ATTACHED, detail = "2 viewers", clientCount = 2),
                SinkStatus("cast", "Living room TV", SinkState.ATTACHING),
                SinkStatus("hdmi", "HDMI display", SinkState.DETACHED),
            ),
        )
    }

    @Test
    fun outputTargetsNoneAttached() = screenshot("output-targets__none-attached") {
        OutputTargetsContent(
            sinks = listOf(
                SinkStatus("web", "Web page", SinkState.DETACHED),
                SinkStatus("hdmi", "HDMI display", SinkState.DETACHED),
            ),
        )
    }

    @Test
    fun outputTargetsEmpty() = screenshot("output-targets__none-available") {
        OutputTargetsContent(sinks = emptyList())
    }

    // ── The colour picker ────────────────────────────────────────────────

    @Test
    fun colorPicker() = screenshot("color-picker__opened", dialog = true) {
        ColorPickerDialog(title = "Background", initial = "#101014", onPick = {}, onDismiss = {})
    }
}
