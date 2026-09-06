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

    // ── The announcement composer ────────────────────────────────────────
    const val ANNOUNCE_TEXT = "announce:text"
    const val ANNOUNCE_GO_LIVE = "announce:goLive"
    const val ANNOUNCE_CLEAR = "announce:clear"
    const val ANNOUNCE_ADD_TO_SCHEDULE = "announce:addToSchedule"
    const val ANNOUNCE_SAVE = "announce:save"
    const val ANNOUNCE_NO_SAVED = "announce:noSaved"
    const val ANNOUNCE_COUNTDOWN_FIELDS = "announce:countdown"
    const val ANNOUNCE_UNTIL_FIELDS = "announce:until"

    /** One of the kinds of announcement (text, countdown, clock…). */
    fun announceType(name: String) = "announce:type:$name"

    /** One saved announcement, by its id. */
    fun savedAnnouncement(id: String) = "announce:saved:$id"

    fun savedAnnouncementDelete(id: String) = "announce:saved:$id:delete"

    // ── The media player ─────────────────────────────────────────────────
    const val MEDIA_URL = "media:url"
    const val MEDIA_GO_LIVE = "media:goLive"
    const val MEDIA_CLEAR = "media:clear"
    const val MEDIA_ADD_TO_SCHEDULE = "media:addToSchedule"
    const val MEDIA_PLAY_PAUSE = "media:playPause"
    const val MEDIA_STOP = "media:stop"
    const val MEDIA_BACK_10 = "media:back10"
    const val MEDIA_FORWARD_10 = "media:forward10"
    const val MEDIA_MUTE = "media:mute"
    const val MEDIA_VOLUME = "media:volume"
    const val MEDIA_UPLOAD = "media:upload"

    // ── The desktop's address ────────────────────────────────────────────
    const val ADDRESS_HOST = "address:host"
    const val ADDRESS_PORT = "address:port"
    const val ADDRESS_API_KEY = "address:apiKey"
    const val ADDRESS_REVEAL_KEY = "address:revealKey"
    const val ADDRESS_HINT = "address:hint"

    // ── The Strong's dictionary ──────────────────────────────────────────
    const val DICT_SEARCH = "dict:search"
    const val DICT_LOADING = "dict:loading"
    const val DICT_ERROR = "dict:error"
    const val DICT_EMPTY = "dict:empty"

    /** One segment of the All / Hebrew / Greek filter, by its position. */
    fun dictFilter(index: Int) = "dict:filter:$index"

    /** One entry row, by the Strong's number that identifies it. */
    fun dictEntry(number: String) = "dict:entry:$number"

    /** The occurrence count on an entry row — absent when the desktop sent none. */
    fun dictEntryUses(number: String) = "dict:entry:$number:uses"

    // ── The Book → Chapter → Verse reference filter ──────────────────────
    const val DICT_REF_BOOK = "dict:ref:book"
    const val DICT_REF_CHAPTER = "dict:ref:chapter"
    const val DICT_REF_VERSE = "dict:ref:verse"
    const val DICT_REF_CLEAR = "dict:ref:clear"

    /** One entry of a reference dropdown's menu, by the dropdown's own tag. */
    fun refOption(dropdown: String, index: Int) = "$dropdown:option:$index"

    // ── The dictionary entry sheet ───────────────────────────────────────
    const val DICT_SHEET = "dict:sheet"
    const val DICT_SHEET_PROJECT = "dict:sheet:project"
    const val DICT_SHEET_ADD_TO_SCHEDULE = "dict:sheet:addToSchedule"
    const val DICT_SHEET_LANGUAGE = "dict:sheet:language"
    const val DICT_SHEET_TRANSLITERATION = "dict:sheet:transliteration"
    const val DICT_SHEET_PRONUNCIATION = "dict:sheet:pronunciation"
    const val DICT_SHEET_OCCURRENCES = "dict:sheet:occurrences"
    const val DICT_SHEET_DEFINITION = "dict:sheet:definition"
    const val DICT_SHEET_KJV_USAGE = "dict:sheet:kjvUsage"
    const val DICT_SHEET_APPEARS_IN = "dict:sheet:appearsIn"
    const val DICT_SHEET_APPEARS_LOADING = "dict:sheet:appearsIn:loading"
    const val DICT_SHEET_APPEARS_COUNT = "dict:sheet:appearsIn:count"

    /** One verse of the sheet's "appears in" list, by the reference it names. */
    fun dictAppearsVerse(reference: String) = "dict:appears:$reference"
}
