package com.church.presenter.churchpresentermobile.ui

/**
 * Semantics tags for the app's screens, so a UI test can name what it is
 * reaching for.
 *
 * See `ui/library/LibraryTags.kt` for the same idea in that package, and the
 * "tag it, don't count it" section of AGENT.md for why: labels come from
 * compose-resources, which does not resolve in the wasmJs test runtime, and an
 * index into the semantics tree turns any layout change into a failure
 * somewhere unrelated.
 */
internal object UiTags {

    // ── Songs list ───────────────────────────────────────────────────────
    const val SONGS_SEARCH = "songs:search"
    const val SONGS_ERROR = "songs:error"
    const val SONGS_RETRY = "songs:retry"
    const val SONGS_BOOK_FILTER = "songs:bookFilter"
    const val SONGS_COUNT = "songs:count"
    /**
     * The three ways the list can be empty, tagged apart.
     *
     * They are different problems with different fixes — clear the filter, get
     * songs onto the phone, check the desktop — so a test has to be able to
     * tell them apart. One shared tag could not, and the messages that do
     * differ are `stringResource`s, which render empty in the wasmJs runtime.
     */
    const val SONGS_EMPTY_NO_MATCH = "songs:empty:noMatch"
    const val SONGS_EMPTY_NO_SONGS = "songs:empty:noSongs"
    const val SONGS_EMPTY_LOCAL_LIBRARY = "songs:empty:localLibrary"

    /** One song row, by the number and songbook that identify it. */
    fun songCard(number: String, book: String?) = "songs:card:$number:${book.orEmpty()}"

    /** An entry in the songbook filter menu; null is the "all books" entry. */
    fun bookOption(book: String?) = "songs:book:${book ?: "all"}"

    // ── The action stack every content screen shares ─────────────────────
    const val FAB_CAST = "fab:cast"
    const val FAB_ADD_TO_SCHEDULE = "fab:addToSchedule"
    const val FAB_SELECT = "fab:select"
    const val FAB_CAST_BADGE = "fab:castBadge"
    const val FAB_CLEAR_DISPLAY = "fab:clearDisplay"
    const val FAB_HOLD = "fab:hold"

    // ── Song detail ──────────────────────────────────────────────────────
    const val SONG_DETAIL_LOADING = "songDetail:loading"
    const val SONG_DETAIL_ERROR = "songDetail:error"
    const val SONG_DETAIL_NO_LYRICS = "songDetail:noLyrics"
    const val SONG_DETAIL_PLAIN_TEXT = "songDetail:plainText"

    /** One verse card, by its position in the song. */
    fun verseCard(index: Int) = "songDetail:verse:$index"

    /** The "live" pill on the verse currently projected. */
    fun verseLivePill(index: Int) = "songDetail:verse:$index:live"

    // ── Bible: books ─────────────────────────────────────────────────────
    const val BIBLE_SEARCH = "bible:search"
    const val BIBLE_NO_MATCH = "bible:noMatch"
    const val BIBLE_NO_BOOKS = "bible:noBooks"

    /** One book row, by the name the desktop gave it. */
    fun bibleBook(name: String) = "bible:book:$name"

    // ── Bible: chapters and verses ───────────────────────────────────────
    const val BIBLE_CHAPTERS_GRID = "bible:chapters"
    const val BIBLE_NO_VERSES = "bible:noVerses"
    const val BIBLE_MULTI_SELECT_COUNT = "bible:multiSelectCount"

    fun bibleChapter(number: Int) = "bible:chapter:$number"

    /** One verse row, by its position in the chapter. */
    fun bibleVerse(index: Int) = "bible:verse:$index"

    // ── Web page ─────────────────────────────────────────────────────────
    const val WEB_URL = "web:url"
    const val WEB_GO_LIVE = "web:goLive"
    const val WEB_CLEAR = "web:clear"
    const val WEB_ADD_TO_SCHEDULE = "web:addToSchedule"
    const val WEB_ADD_BOOKMARK = "web:addBookmark"
    const val WEB_NO_BOOKMARKS = "web:noBookmarks"

    /** One saved page, by its id. */
    fun bookmark(id: String) = "web:bookmark:$id"

    fun bookmarkDelete(id: String) = "web:bookmark:$id:delete"

    /** The "live" marker on the page currently projected. */
    fun bookmarkLive(id: String) = "web:bookmark:$id:live"

    // ── The running-order drawer ─────────────────────────────────────────
    const val DRAWER_CLOSE = "drawer:close"
    const val DRAWER_COUNT = "drawer:count"
    const val DRAWER_EMPTY = "drawer:empty"
    const val DRAWER_ERROR = "drawer:error"
    const val DRAWER_CLEAR = "drawer:clear"

    /** One row of the standalone running order, by its position. */
    fun orderRow(index: Int) = "order:row:$index"

    fun orderMoveUp(index: Int) = "order:row:$index:up"

    fun orderMoveDown(index: Int) = "order:row:$index:down"

    fun orderRemove(index: Int) = "order:row:$index:remove"

    /** One row of the desktop's schedule, by its position. */
    fun scheduleRow(index: Int) = "schedule:row:$index"

    /** The "live" marker on the schedule item the desktop is showing. */
    fun scheduleRowLive(index: Int) = "schedule:row:$index:live"

    // ── Certificate setup ────────────────────────────────────────────────
    const val CERT_DONE = "cert:done"
    const val CERT_SKIP = "cert:skip"
    const val CERT_SKIP_TOP = "cert:skipTop"
    const val CERT_FINGERPRINT = "cert:fingerprint"
    const val CERT_NO_FINGERPRINT = "cert:noFingerprint"

    // ── First-run connection setup ───────────────────────────────────────
    const val CONNECT_HOST = "connect:host"
    const val CONNECT_PORT = "connect:port"
    const val CONNECT_API_KEY = "connect:apiKey"
    const val CONNECT_APPLY = "connect:apply"
    const val CONNECT_DONE = "connect:done"
    const val CONNECT_SKIP = "connect:skip"
    const val CONNECT_SKIP_TOP = "connect:skipTop"
    const val CONNECT_NO_CAMERA = "connect:noCamera"
    const val CONNECT_CONNECTED = "connect:connected"

    // ── The contact form ─────────────────────────────────────────────────
    const val CONTACT_NAME = "contact:name"
    const val CONTACT_EMAIL = "contact:email"
    const val CONTACT_MESSAGE = "contact:message"
    const val CONTACT_SEND = "contact:send"
    const val CONTACT_OPEN_BROWSER = "contact:openBrowser"
    const val CONTACT_SENT = "contact:sent"
    const val CONTACT_ERROR = "contact:error"

    /** One of the four kinds of message the server accepts. */
    fun contactType(key: String) = "contact:type:$key"

    // ── The startup status check ─────────────────────────────────────────
    const val STATUS_LOADING = "status:loading"
    const val STATUS_ERROR = "status:error"
    const val STATUS_ALL_GOOD = "status:allGood"
    const val STATUS_WARNINGS = "status:warnings"
    const val STATUS_RETRY = "status:retry"
    const val STATUS_CONTINUE = "status:continue"
    const val STATUS_OPEN_SETTINGS = "status:openSettings"

    // ── The desktop's address ────────────────────────────────────────────
    const val ADDRESS_HOST = "address:host"
    const val ADDRESS_PORT = "address:port"
    const val ADDRESS_API_KEY = "address:apiKey"
    const val ADDRESS_REVEAL_KEY = "address:revealKey"
    const val ADDRESS_HINT = "address:hint"
}
