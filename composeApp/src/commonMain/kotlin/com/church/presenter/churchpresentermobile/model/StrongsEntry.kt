package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/** Item type discriminator the desktop server infers dictionary items from. */
private const val DICTIONARY_ITEM_TYPE = "dictionary"

/**
 * A Strong's dictionary entry, mirroring the desktop server's `StrongsEntry`
 * returned by `GET /api/dictionary` and `GET /api/dictionary/{number}`.
 */
@Serializable
data class StrongsEntry(
    val number: String,
    val word: String,
    val transliteration: String = "",
    val pronunciation: String = "",
    val definition: String = "",
    val kjvUsage: String = "",
    /** Total word-instance occurrences across scripture (from the interlinear index). */
    val occurrences: Int = 0,
    /** Root Strong's number parsed from the definition (e.g. "H433"), or blank. */
    val root: String = ""
) {
    val isHebrew: Boolean get() = number.startsWith("H")
    val isGreek: Boolean get() = number.startsWith("G")
    val numericValue: Int get() = number.drop(1).toIntOrNull() ?: 0
}

/**
 * One verse in which a Strong's number appears — a row in the entry sheet's
 * "Appears in" list. Mirrors the server's `DictionaryVerseDto`.
 */
@Serializable
data class DictionaryVerse(
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val reference: String,
    val text: String = ""
)

/** Response from `GET /api/dictionary/{number}/verses`. */
@Serializable
data class DictionaryVersesResponse(
    val number: String,
    /** Total number of verses the number appears in; [verses] is capped below this. */
    val total: Int = 0,
    val verses: List<DictionaryVerse> = emptyList()
)

/**
 * Flat item payload for `POST /api/project` and `POST /api/schedule/add`.
 * The desktop's `RemoteItemDto.toScheduleItem()` infers a dictionary item from
 * [strongsNumber]; the word is carried in [title].
 */
@Serializable
data class DictionaryItemPayload(
    val type: String = DICTIONARY_ITEM_TYPE,
    val id: String,
    val strongsNumber: String,
    val title: String,
    val transliteration: String = "",
    val definition: String = "",
    val displayText: String
)

/** Wrapper request body for both project and schedule-add endpoints. */
@Serializable
data class ProjectDictionaryRequest(val item: DictionaryItemPayload)
