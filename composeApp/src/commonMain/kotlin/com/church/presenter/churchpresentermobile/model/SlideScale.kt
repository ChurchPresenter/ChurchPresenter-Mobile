package com.church.presenter.churchpresentermobile.model

/**
 * Sizes on a slide, as fractions of its width. Fractions and nothing else, with no
 * floor or ceiling, so a thumbnail, a notification and a TV are the same picture at
 * different sizes. The browser page's stylesheet uses the same numbers, in `vw`.
 */
val SlideTextSize.widthFraction: Float
    get() = when (this) {
        SlideTextSize.SMALL -> 0.052f
        SlideTextSize.MEDIUM -> 0.066f
        SlideTextSize.LARGE -> 0.084f
    }

/** The reference line. */
const val REFERENCE_WIDTH_FRACTION: Float = 0.017f

/** The footer. */
const val FOOTER_WIDTH_FRACTION: Float = 0.011f

/** The church's name and the clock. */
const val CORNER_WIDTH_FRACTION: Float = 0.012f

/** Line height of the words, as a multiple of their size. */
const val BODY_LINE_HEIGHT: Float = 1.32f

/** The space above the reference and above the footer, as fractions of the slide's height. */
const val REFERENCE_GAP: Float = 0.045f
const val FOOTER_GAP: Float = 0.02f

/** Letter spacing of the reference, as a multiple of its size. */
const val REFERENCE_TRACKING: Float = 0.14f

/** How far auto-fit may shrink the words, as a fraction of their size. */
const val AUTO_FIT_FLOOR: Float = 0.45f
