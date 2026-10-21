package com.church.presenter.churchpresentermobile.ui.standalone

import com.church.presenter.churchpresentermobile.model.TextElement

/**
 * Names the Display text screen's controls for a UI test.
 *
 * Its labels are `stringResource`s, which render empty in the wasmJs test runtime, and the same
 * controls repeat for every element — so a test names what it means here, in one place both
 * sides read.
 */
internal object DisplayTextTags {
    const val SCREEN = "display:screen"
    const val BACK = "display:back"
    const val PREVIEW = "display:preview"
    const val EXPAND = "display:expand"
    const val FULL_SCREEN = "display:fullScreen"
    const val FULL_SCREEN_CLOSE = "display:fullScreen:close"
    const val SAVE = "display:save"
    const val RESET = "display:reset"
    const val SCREEN_OPTIONS = "display:screenOptions"
    const val FONT = "display:font"
    const val SIZE = "display:size"
    const val BOLD = "display:bold"
    const val ITALIC = "display:italic"
    const val UNDERLINE = "display:underline"
    const val COLOR_CUSTOM = "display:color:custom"
    const val OUTLINE = "display:outline"
    const val OUTLINE_COLOR = "display:outline:color"
    const val OUTLINE_WIDTH = "display:outline:width"
    const val BACKING = "display:backing"
    const val BACKING_COLOR = "display:backing:color"
    const val BACKING_OPACITY = "display:backing:opacity"
    const val SHADOW = "display:shadow"
    const val SHADOW_BLUR = "display:shadow:blur"
    const val SHADOW_OFFSET = "display:shadow:offset"
    const val LETTER_SPACING = "display:letterSpacing"
    const val WORD_SPACING = "display:wordSpacing"
    const val VISIBLE = "display:visible"
    const val REFERENCE_FORMAT = "display:reference:format"
    const val REFERENCE_POSITION = "display:reference:position"
    const val SECOND_SHOW = "display:second:show"
    const val SECOND_POSITION = "display:second:position"
    const val SECOND_SCALE = "display:second:scale"
    const val SAMPLE = "display:sample"
    const val ASPECT = "display:aspect"
    const val BACKGROUND = "display:background"
    const val PICKER = "display:picker"
    const val PICKER_DONE = "display:picker:done"
    const val PICKER_HEX = "display:picker:hex"
    const val PICKER_SQUARE = "display:picker:square"
    const val PICKER_HUE = "display:picker:hue"
    const val PICKER_ALPHA = "display:picker:alpha"

    /** The Bible / Songs switch: 0 is Bible, 1 is Songs. */
    fun group(index: Int) = "display:group:$index"

    /** An entry in the element list or the element tabs. */
    fun elementTab(element: TextElement) = "display:tab:${element.name}"

    /** An element as drawn on the preview. */
    fun element(element: TextElement) = "display:element:${element.name}"

    fun letterCase(index: Int) = "display:case:$index"

    fun align(index: Int) = "display:align:$index"

    fun verticalAlign(index: Int) = "display:valign:$index"

    fun swatch(index: Int) = "display:swatch:$index"

    fun pickerMode(index: Int) = "display:picker:mode:$index"

    fun pickerSwatch(hex: String) = "display:picker:swatch:$hex"

    fun option(menu: String, index: Int) = "$menu:option:$index"
}
