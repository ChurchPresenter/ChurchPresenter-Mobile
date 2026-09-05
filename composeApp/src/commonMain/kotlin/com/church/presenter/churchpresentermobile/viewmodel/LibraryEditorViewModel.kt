package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import com.church.presenter.churchpresentermobile.generateUUID
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.LibraryValidation
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.model.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Backs the song and announcement editors.
 *
 * Holds a working copy and validates it on every change, so the save button can
 * be honest about whether it will work before the user presses it. Nothing
 * reaches the repository until [save].
 */
class LibraryEditorViewModel(
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _song = MutableStateFlow(blankSong())
    val song: StateFlow<LocalSong> = _song.asStateFlow()

    private val _announcement = MutableStateFlow(blankAnnouncement())
    val announcement: StateFlow<LocalAnnouncement> = _announcement.asStateFlow()

    private val _validation = MutableStateFlow(ValidationResult.VALID)
    val validation: StateFlow<ValidationResult> = _validation.asStateFlow()

    /** True once the user has changed something — used to warn before discarding. */
    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    /** Live preview of the section being edited, rendered exactly as the screen will. */
    private val _previewSlide = MutableStateFlow(Slide.BLANK)
    val previewSlide: StateFlow<Slide> = _previewSlide.asStateFlow()

    // ── Song editing ─────────────────────────────────────────────────────

    /** Loads [id] for editing, or starts a new song when it is null or unknown. */
    fun editSong(id: String?) {
        _song.value = id?.let(repository::song) ?: blankSong()
        _isDirty.value = false
        revalidateSong()
    }

    fun setSongTitle(value: String) = updateSong { it.copy(title = value) }

    fun setSongNumber(value: String) = updateSong { it.copy(number = value) }

    fun setSongAuthor(value: String) = updateSong { it.copy(author = value.ifBlank { null }) }

    fun setSongBook(value: String) = updateSong { it.copy(bookName = value.ifBlank { null }) }

    fun setSongCopyright(value: String) = updateSong { it.copy(copyright = value.ifBlank { null }) }

    fun addSection(type: SectionType = SectionType.VERSE) = updateSong {
        it.copy(sections = it.sections + LocalSongSection(type = type))
    }

    fun setSectionText(index: Int, text: String) = updateSection(index) { it.copy(text = text) }

    fun setSectionType(index: Int, type: SectionType) = updateSection(index) { it.copy(type = type) }

    fun removeSection(index: Int) = updateSong { song ->
        song.copy(sections = song.sections.filterIndexed { i, _ -> i != index })
    }

    /** Moves the section at [index] one place towards the start. */
    fun moveSectionUp(index: Int) = moveSection(index, index - 1)

    /** Moves the section at [index] one place towards the end. */
    fun moveSectionDown(index: Int) = moveSection(index, index + 1)

    private fun moveSection(from: Int, to: Int) = updateSong { song ->
        if (from !in song.sections.indices || to !in song.sections.indices) return@updateSong song
        val reordered = song.sections.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        song.copy(sections = reordered)
    }

    /**
     * Splits the section at [index] on its blank lines.
     *
     * A stanza pasted from a hymnal often arrives as one block that will not fit
     * on a slide; this turns it into the slides it should have been without the
     * user retyping it.
     */
    fun splitSection(index: Int) = updateSong { song ->
        val section = song.sections.getOrNull(index) ?: return@updateSong song
        val parts = section.text.split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.size < 2) return@updateSong song

        val replacements = parts.map { section.copy(text = it, label = null) }
        song.copy(
            sections = song.sections.toMutableList().apply {
                removeAt(index)
                addAll(index, replacements)
            }
        )
    }

    /** Shows the section at [index] in the preview pane. */
    fun previewSection(index: Int) {
        val deck = SlideDeckBuilder.fromLocalSong(_song.value)
        _previewSlide.value = deck.slideAt(index) ?: Slide.BLANK
    }

    // ── Announcement editing ─────────────────────────────────────────────

    fun editAnnouncement(id: String?) {
        _announcement.value = id?.let(repository::announcement) ?: blankAnnouncement()
        _isDirty.value = false
        revalidateAnnouncement()
    }

    fun setAnnouncementTitle(value: String) = updateAnnouncement { it.copy(title = value) }

    fun setAnnouncementBody(value: String) = updateAnnouncement { it.copy(body = value) }

    // ── Saving ───────────────────────────────────────────────────────────

    /** Persists the song if it validates. Returns true when it was saved. */
    fun saveSong(): Boolean {
        revalidateSong()
        if (!_validation.value.isValid) return false
        // Drop empty sections rather than projecting blank slides from them.
        val cleaned = _song.value.copy(
            sections = _song.value.sections.filter { it.text.isNotBlank() }
        )
        repository.upsertSong(cleaned)
        _isDirty.value = false
        return true
    }

    /** Persists the announcement if it validates. Returns true when it was saved. */
    fun saveAnnouncement(): Boolean {
        revalidateAnnouncement()
        if (!_validation.value.isValid) return false
        repository.upsertAnnouncement(_announcement.value)
        _isDirty.value = false
        return true
    }

    // ── Internals ────────────────────────────────────────────────────────

    private inline fun updateSong(transform: (LocalSong) -> LocalSong) {
        _song.value = transform(_song.value)
        _isDirty.value = true
        revalidateSong()
    }

    private inline fun updateSection(index: Int, transform: (LocalSongSection) -> LocalSongSection) {
        updateSong { song ->
            if (index !in song.sections.indices) return@updateSong song
            song.copy(
                sections = song.sections.mapIndexed { i, section ->
                    if (i == index) transform(section) else section
                }
            )
        }
    }

    private inline fun updateAnnouncement(transform: (LocalAnnouncement) -> LocalAnnouncement) {
        _announcement.value = transform(_announcement.value)
        _isDirty.value = true
        revalidateAnnouncement()
    }

    private fun revalidateSong() {
        _validation.value = LibraryValidation.validateSong(_song.value, repository.songs)
    }

    private fun revalidateAnnouncement() {
        _validation.value = LibraryValidation.validateAnnouncement(_announcement.value)
    }

    private fun blankSong() = LocalSong(
        id = generateUUID(),
        sections = listOf(LocalSongSection(SectionType.VERSE)),
    )

    private fun blankAnnouncement() = LocalAnnouncement(id = generateUUID())
}
