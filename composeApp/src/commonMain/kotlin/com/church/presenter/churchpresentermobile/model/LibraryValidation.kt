package com.church.presenter.churchpresentermobile.model

/** A field an editor can report a problem against. */
enum class LibraryField {
    TITLE,
    NUMBER,
    SECTIONS,
    BODY,
    NAME,
    ENTRIES,
}

/**
 * The outcome of validating an editor's contents.
 *
 * Errors block saving; warnings do not. The split matters: a duplicate song
 * number is usually a mistake but is legitimate across two songbooks, so
 * refusing to save would be wrong — telling the user and letting them decide is
 * not.
 */
data class ValidationResult(
    val errors: Map<LibraryField, String> = emptyMap(),
    val warnings: Map<LibraryField, String> = emptyMap(),
) {
    val isValid: Boolean get() = errors.isEmpty()

    companion object {
        val VALID: ValidationResult = ValidationResult()
    }
}

/**
 * Pure validation for the library editors.
 *
 * Kept out of the ViewModels so the rules are exhaustively testable and so the
 * same checks can be reused by the import path later.
 */
object LibraryValidation {

    /** Longest a single section may be before it stops fitting on a screen. */
    const val MAX_SECTION_CHARS = 2_000

    /** Line count past which a section is unlikely to be readable on one slide. */
    const val LONG_SECTION_LINES = 12

    /**
     * @param existing The rest of the library, for the duplicate-number check.
     */
    fun validateSong(song: LocalSong, existing: List<LocalSong> = emptyList()): ValidationResult {
        val errors = mutableMapOf<LibraryField, String>()
        val warnings = mutableMapOf<LibraryField, String>()

        if (song.title.isBlank()) {
            errors[LibraryField.TITLE] = "Give the song a title"
        }

        val usableSections = song.sections.filter { it.text.isNotBlank() }
        if (usableSections.isEmpty()) {
            errors[LibraryField.SECTIONS] = "Add at least one verse or chorus"
        }

        song.sections.firstOrNull { it.text.length > MAX_SECTION_CHARS }?.let {
            errors[LibraryField.SECTIONS] =
                "One section is too long — split it into two (max $MAX_SECTION_CHARS characters)"
        }

        // A duplicate number within the same book is almost always a slip; the
        // same number in a different songbook is normal, so scope the check.
        if (song.number.isNotBlank()) {
            val clash = existing.any {
                it.id != song.id &&
                    it.number.equals(song.number, ignoreCase = true) &&
                    it.bookName.orEmpty().equals(song.bookName.orEmpty(), ignoreCase = true)
            }
            if (clash) {
                warnings[LibraryField.NUMBER] = "Another song already uses number ${song.number}"
            }
        }

        usableSections.firstOrNull { it.text.lineSequence().count() > LONG_SECTION_LINES }?.let {
            warnings[LibraryField.SECTIONS] =
                "A section is longer than $LONG_SECTION_LINES lines and may not fit on one slide"
        }

        return ValidationResult(errors, warnings)
    }

    fun validateAnnouncement(announcement: LocalAnnouncement): ValidationResult {
        val errors = mutableMapOf<LibraryField, String>()
        val warnings = mutableMapOf<LibraryField, String>()

        if (announcement.body.isBlank()) {
            errors[LibraryField.BODY] = "Add some text to show"
        }
        if (announcement.body.length > MAX_SECTION_CHARS) {
            errors[LibraryField.BODY] = "Too long to fit on a screen (max $MAX_SECTION_CHARS characters)"
        }
        if (announcement.body.lineSequence().count() > LONG_SECTION_LINES) {
            warnings[LibraryField.BODY] = "This may not fit on one slide"
        }
        return ValidationResult(errors, warnings)
    }

    /**
     * @param library The library the setlist's entries point into, so a
     *   reference to a deleted song is caught before a service rather than during one.
     */
    fun validateSetlist(setlist: LocalSetlist, library: LibraryData = LibraryData.EMPTY): ValidationResult {
        val errors = mutableMapOf<LibraryField, String>()
        val warnings = mutableMapOf<LibraryField, String>()

        if (setlist.name.isBlank()) {
            errors[LibraryField.NAME] = "Give the set a name"
        }
        if (setlist.entries.isEmpty()) {
            errors[LibraryField.ENTRIES] = "Add at least one item"
        }

        val missing = setlist.entries.count { entry ->
            when (entry.type) {
                SetlistEntryType.SONG -> library.songs.none { it.id == entry.reference }
                SetlistEntryType.ANNOUNCEMENT -> library.announcements.none { it.id == entry.reference }
                // Bible references are passages, not library ids — nothing to resolve.
                SetlistEntryType.BIBLE -> false
            }
        }
        if (missing > 0) {
            warnings[LibraryField.ENTRIES] =
                if (missing == 1) "1 item is no longer in your library"
                else "$missing items are no longer in your library"
        }

        return ValidationResult(errors, warnings)
    }
}
