package com.church.presenter.churchpresentermobile.ui.standalone

/**
 * Names the standalone screens' controls for a UI test.
 *
 * Every label on these screens is a `stringResource`, which renders empty in
 * the wasmJs test runtime — Prev, Next, Blank, Clear and Live would all be
 * zero-width buttons that a positional tap misses. See the "tag it, don't count
 * it" section of AGENT.md.
 */
internal object StandaloneTags {

    // ── The live bar, and the chip it folds into ─────────────────────────
    const val LIVE_BAR = "liveBar"
    const val LIVE_BAR_PREVIEW = "liveBar:preview"
    const val LIVE_BAR_PREV = "liveBar:prev"
    const val LIVE_BAR_NEXT = "liveBar:next"
    const val LIVE_BAR_BLANK = "liveBar:blank"
    const val LIVE_BAR_STATUS = "liveBar:status"
    const val LIVE_BAR_COLLAPSE = "liveBar:collapse"
    const val LIVE_CHIP = "liveChip"

    // ── The full live view, and the pieces it shares with the bar ────────
    const val LIVE_VIEW = "liveView"
    const val PREVIEW = "controller:preview"
    const val EMPTY_DECK = "controller:emptyDeck"
    const val PREV = "controller:prev"
    const val NEXT = "controller:next"
    const val BLANK = "controller:blank"
    const val CLEAR = "controller:clear"
    const val LIVE = "controller:live"
    const val LOOK = "controller:look"
    const val BACKDROP_PHOTOS = "controller:backdrop:photos"
    const val BACKDROP_NO_PHOTOS = "controller:backdrop:noPhotos"
    const val BACKDROP_NEEDS_SERVER = "controller:backdrop:needsServer"

    /** One section of the open deck, by its position. */
    fun section(index: Int) = "controller:section:$index"

    /** One of the three backdrop choices. */
    fun backdrop(index: Int) = "controller:backdrop:$index"

    /** One photo offered as a backdrop. */
    fun backdropPhoto(id: String) = "controller:backdrop:photo:$id"

    // ── Settings → Display ───────────────────────────────────────────────
    const val DISPLAY_OUTPUTS = "display:outputs"
    const val OUTPUTS_EMPTY = "outputs:empty"
    const val OUTPUTS_GUIDANCE = "outputs:guidance"

    /** One screen the phone can project onto. */
    fun sink(id: String) = "outputs:sink:$id"

    fun sinkUrl(id: String) = "outputs:sink:$id:url"

    // ── Notices held on this device ──────────────────────────────────────
    const val NOTICES_EMPTY = "localNotices:empty"
    const val NO_OUTPUT = "localNotices:noOutput"

    fun notice(id: String) = "localNotices:row:$id"

    fun noticeProject(id: String) = "localNotices:row:$id:project"

    fun noticeClear(id: String) = "localNotices:row:$id:clear"

    // ── Photos held on this device ───────────────────────────────────────
    const val PHOTOS_EMPTY = "localPhotos:empty"
    const val PHOTOS_PICK = "localPhotos:pick"
    const val PHOTOS_CLEAR = "localPhotos:clear"
    const val PHOTOS_NO_SERVER = "localPhotos:noServer"

    /** One picked photo, by its id. */
    fun photo(id: String) = "localPhotos:photo:$id"

    fun photoRemove(id: String) = "localPhotos:photo:$id:remove"

    fun photoLive(id: String) = "localPhotos:photo:$id:live"
}
