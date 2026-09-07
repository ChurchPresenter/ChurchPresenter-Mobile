package com.church.presenter.churchpresentermobile.ui.library

/**
 * Semantics tags for the library screens, so a UI test can name what it is
 * reaching for.
 *
 * The alternative — and what these replaced — is selecting by position in the
 * semantics tree, which turns a layout change into a test failure somewhere
 * unrelated, and cannot reach a lazy item that has not been composed yet.
 * Labels are no use either: compose-resources does not resolve in the wasmJs
 * test runtime, so every `stringResource` renders empty there.
 *
 * `internal`, and referenced from both sides, so a tag cannot be renamed on one
 * side alone. They carry no behaviour and are stripped from nothing — a tag is
 * a semantics property, not a view.
 */
internal object LibraryTags {

    // ── Library list ─────────────────────────────────────────────────────
    const val SEARCH = "library:search"

    /** The row for one song or notice, by its library id. */
    fun row(id: String) = "library:row:$id"

    fun rowEdit(id: String) = "library:row:$id:edit"

    fun rowDelete(id: String) = "library:row:$id:delete"

    const val DELETE_CONFIRM = "library:delete:confirm"
    const val DELETE_DISMISS = "library:delete:dismiss"

    // ── Editors ──────────────────────────────────────────────────────────
    const val FIELD_TITLE = "editor:title"
    const val FIELD_NUMBER = "editor:number"
    const val FIELD_BOOK = "editor:book"
    const val FIELD_AUTHOR = "editor:author"
    const val FIELD_COPYRIGHT = "editor:copyright"
    const val FIELD_BODY = "editor:body"

    /** The words of one verse, by its position in the song. */
    fun verse(index: Int) = "editor:verse:$index"

    const val ADD_VERSE = "editor:addVerse"
    const val SAVE = "editor:save"
    const val CANCEL = "editor:cancel"
    const val DISCARD = "editor:discard"
    const val KEEP_EDITING = "editor:keepEditing"

    // ── Copying songs from the computer ──────────────────────────────────
    const val SYNC_PROGRESS = "sync:progress"
    const val SYNC_PROGRESS_LABEL = "sync:progress:label"
    const val SYNC_CURRENT_TITLE = "sync:progress:title"
    const val SYNC_OUTCOME = "sync:outcome"
    const val SYNC_BUTTON = "sync:button"
    const val SYNC_BOOK_COUNT = "sync:books:count"
    const val SYNC_BOOKS_TOGGLE_ALL = "sync:books:toggleAll"
    const val SYNC_BOOKS_FINDING = "sync:books:finding"
    const val SYNC_BOOKS_MISSING = "sync:books:missing"
    const val SYNC_BOOKS_NONE = "sync:books:none"

    /** The All / Choose books control. */
    fun syncScope(index: Int) = "sync:scope:$index"

    /** One songbook's tick box. */
    fun syncBook(name: String) = "sync:book:$name"

    // ── Copying a Bible ──────────────────────────────────────────────────
    const val BIBLE_SYNC_INSTALLED = "bibleSync:installed"
    const val BIBLE_SYNC_CHOOSE_HINT = "bibleSync:chooseHint"
    const val BIBLE_SYNC_FINDING = "bibleSync:finding"
    const val BIBLE_SYNC_FIND = "bibleSync:find"
    const val BIBLE_SYNC_DOWNLOAD = "bibleSync:download"
    const val BIBLE_SYNC_STOP = "bibleSync:stop"
    const val BIBLE_SYNC_PROGRESS = "bibleSync:progress"
    const val BIBLE_SYNC_OUTCOME = "bibleSync:outcome"
    const val BIBLE_SYNC_LOAD_ERROR = "bibleSync:loadError"
    const val BIBLE_SYNC_REMOVE_CONFIRM = "bibleSync:remove:confirm"
    const val BIBLE_SYNC_REMOVE_DISMISS = "bibleSync:remove:dismiss"

    /** One translation already on the phone. */
    fun bibleInstalled(id: String) = "bibleSync:installed:$id"

    fun bibleRemove(id: String) = "bibleSync:installed:$id:remove"

    /** One translation the computer is offering. */
    fun bibleChoice(fileName: String) = "bibleSync:choice:$fileName"

    // ── Sharing a library ────────────────────────────────────────────────
    const val SHARE_EXPORT = "share:export"
    const val SHARE_IMPORT = "share:import"
    const val SHARE_RESULT = "share:result"
    const val SHARE_PREVIEW = "share:preview"
    const val SHARE_PREVIEW_CANCEL = "share:preview:cancel"

    /** One of the ways an overlapping item can be resolved. */
    fun shareResolve(mode: String) = "share:preview:$mode"

    // ── The library list's own controls ──────────────────────────────────
    const val EMPTY_LIBRARY = "library:empty"
    const val EMPTY_COPY_FROM_COMPUTER = "library:empty:copy"
    const val EMPTY_WRITE_SONG = "library:empty:write"
    const val NO_RESULTS = "library:noResults"
    const val MESSAGE = "library:message"
    const val SYNC_CHIP = "library:chip:sync"
    const val BIBLE_CHIP = "library:chip:bible"
    const val SHARE_CHIP = "library:chip:share"
    const val ADD = "library:add"
    const val ADD_SONG = "library:add:song"
    const val ADD_NOTICE = "library:add:notice"
    const val ADD_DISMISS = "library:add:dismiss"
    const val SONGS_HEADING = "library:heading:songs"
    const val NOTICES_HEADING = "library:heading:notices"

    /** One of the All / Songs / Notices filters. */
    fun filter(index: Int) = "library:filter:$index"

    /** One half of the copy sheet — songs or Bible. */
    fun syncSection(index: Int) = "sync:section:$index"
}
