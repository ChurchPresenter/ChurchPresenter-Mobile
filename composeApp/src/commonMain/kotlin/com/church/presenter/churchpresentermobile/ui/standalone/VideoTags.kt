package com.church.presenter.churchpresentermobile.ui.standalone

/**
 * Test tags for the standalone Media tab, named once and referenced from both
 * the screen and its tests so a rename cannot drift.
 */
internal object VideoTags {
    const val PREVIEW = "localMedia:preview"
    const val TIME = "localMedia:time"
    const val TITLE = "localMedia:title"
    const val STATUS = "localMedia:status"
    const val LOOP = "localMedia:loop"
    const val BLANK = "localMedia:blank"
    const val CLEAR = "localMedia:clear"
    const val LIVE = "localMedia:live"
    const val END_ACTION = "localMedia:endAction"
    const val SOUND = "localMedia:sound"
    const val SOUND_HINT = "localMedia:soundHint"
    const val ADD = "localMedia:add"
    const val ADD_FROM_PHOTOS = "localMedia:add:photos"
    const val ADD_FROM_FILES = "localMedia:add:files"
    const val EMPTY = "localMedia:empty"
    const val DELETE_CONFIRM = "localMedia:deleteConfirm"

    /** One stored video's row, by its id. */
    fun row(id: String) = "localMedia:video:$id"

    fun delete(id: String) = "localMedia:video:$id:delete"
}
