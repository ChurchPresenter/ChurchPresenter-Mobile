package com.church.presenter.churchpresentermobile.ui.canvas

import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.ImageSource
import com.church.presenter.churchpresentermobile.viewmodel.SelectionAction

/**
 * Test tags for the canvas, named once and referenced from both the screen and its
 * tests so a rename cannot drift. Labels come from compose-resources, which render
 * empty on the wasmJs test runtime — see AGENT.md, "tag it, don't count it".
 */
internal object CanvasTags {
    const val SCREEN = "canvas:screen"
    const val BACK = "canvas:back"
    const val UNDO = "canvas:undo"
    const val ROTATE_KNOB = "canvas:rotateKnob"
    const val REDO = "canvas:redo"
    const val ZOOM_OUT = "canvas:zoomOut"
    const val ZOOM_IN = "canvas:zoomIn"
    const val ZOOM_VALUE = "canvas:zoomValue"
    const val LAYERS = "canvas:layers"
    const val MORE = "canvas:more"
    const val GO_LIVE = "canvas:goLive"
    const val LIVE_NOTE = "canvas:liveNote"
    const val BOARD = "canvas:board"
    const val DOCK = "canvas:dock"
    const val ADD_PAGE = "canvas:addPage"
    const val OPTIONS = "canvas:options"
    const val LAYERS_PANEL = "canvas:layersPanel"
    const val LAYERS_DONE = "canvas:layersDone"
    const val INSERT_PANEL = "canvas:insertPanel"
    const val SELECTION_BAR = "canvas:selectionBar"
    const val MENU_RENAME = "canvas:menu:rename"
    const val MENU_DUPLICATE_PAGE = "canvas:menu:duplicatePage"
    const val MENU_DELETE_PAGE = "canvas:menu:deletePage"
    const val MENU_CLEAR_PAGE = "canvas:menu:clearPage"
    const val MENU_CLEAR_CANVAS = "canvas:menu:clearCanvas"
    const val CLEAR_CANVAS_CONFIRM = "canvas:clearCanvas:confirm"
    const val RENAME_FIELD = "canvas:rename:field"
    const val RENAME_SAVE = "canvas:rename:save"

    fun tool(tool: CanvasTool) = "canvas:tool:${tool.name.lowercase()}"
    fun page(index: Int) = "canvas:page:$index"
    fun element(id: String) = "canvas:element:$id"

    /** A named color in a palette row — an ink or highlighter color by its enum name. */
    fun preset(name: String) = "canvas:preset:${name.lowercase()}"

    /** A color the operator mixed, in a palette row. */
    fun mix(hex: String) = "canvas:mix:${hex.lowercase()}"
    fun layer(id: String) = "canvas:layer:$id"
    fun layerLock(id: String) = "canvas:layerLock:$id"
    fun layerVisibility(id: String) = "canvas:layerVisibility:$id"
    fun selection(action: SelectionAction) = "canvas:selection:${action.name.lowercase()}"
    fun insertSource(source: ImageSource) = "canvas:insert:${source.name.lowercase()}"
}
