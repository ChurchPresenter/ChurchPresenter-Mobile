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

    // ── The live controller ──────────────────────────────────────────────
    const val OUTPUT_CHIP = "controller:outputs"
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

    // ── The outputs sheet ────────────────────────────────────────────────
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

    // ── A link shown by this device ──────────────────────────────────────
    const val WEB_URL = "localWeb:url"
    const val WEB_GO_LIVE = "localWeb:goLive"
    const val WEB_CLEAR = "localWeb:clear"
    const val WEB_NOT_A_LINK = "localWeb:notALink"
    const val WEB_NO_OUTPUT = "localWeb:noOutput"
    const val WEB_REFUSES_FRAMING = "localWeb:refusesFraming"
    const val WEB_LIVE_URL = "localWeb:liveUrl"
}
