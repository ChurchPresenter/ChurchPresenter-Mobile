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
}
