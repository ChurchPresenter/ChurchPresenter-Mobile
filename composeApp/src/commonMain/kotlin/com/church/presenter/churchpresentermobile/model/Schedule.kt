package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single item in the service schedule returned by GET /api/schedule.
 */
@Serializable
data class ScheduleItem(
    val id: String? = null,
    val type: String? = null,
    val title: String? = null,
    val details: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("isActive") val isActiveCamel: Boolean? = null,
    val index: Int? = null,
    val notes: String? = null,
    // Generic display text — used by all item types (songs, images, etc.)
    @SerialName("displayText")   val displayTextCamel: String? = null,
    @SerialName("display_text")  val displayTextSnake: String? = null,
    @SerialName("display-text")  val displayTextKebab: String? = null,
    // Bible-specific fields — multiple name variants to handle different server styles
    @SerialName("bookName")   val bookNameCamel: String? = null,
    @SerialName("book_name")  val bookNameSnake: String? = null,
    @SerialName("book-name")  val bookNameKebab: String? = null,
    @SerialName("book")       val bookNameShort: String? = null,
    val chapter: Int? = null,
    @SerialName("verseNumber")  val verseNumberCamel: Int? = null,
    @SerialName("verse_number") val verseNumberSnake: Int? = null,
    @SerialName("verse")        val verseNumberShort: Int? = null,
    @SerialName("verseRange")   val verseRangeCamel: String? = null,
    @SerialName("verse_range")  val verseRangeSnake: String? = null,
    @SerialName("verse-range")  val verseRangeKebab: String? = null,
    @SerialName("verses")       val verseRangeVerses: String? = null,
    // Image/picture-specific fields
    @SerialName("folderId")     val folderIdCamel: String? = null,
    @SerialName("folder_id")    val folderIdSnake: String? = null,
    @SerialName("folder-id")    val folderIdKebab: String? = null,
    @SerialName("folderName")   val folderNameCamel: String? = null,
    @SerialName("folder_name")  val folderNameSnake: String? = null,
    @SerialName("folder-name")  val folderNameKebab: String? = null,
    @SerialName("imageIndex")   val imageIndexCamel: Int? = null,
    @SerialName("image_index")  val imageIndexSnake: Int? = null,
    @SerialName("image-index")  val imageIndexKebab: Int? = null,
    @SerialName("imageNumber")  val imageNumber: Int? = null,
    // Media
    @SerialName("mediaUrl")   val mediaUrl: String? = null,
    @SerialName("mediaType")  val mediaType: String? = null,
    // Website
    @SerialName("url")        val url: String? = null,
    // Announcement / dictionary text payload
    @SerialName("text")            val text: String? = null,
    @SerialName("textColor")       val textColor: String? = null,
    @SerialName("backgroundColor") val backgroundColor: String? = null,
    // Announcement / timer details (to reconstruct the composer)
    @SerialName("fontSize")          val fontSize: Int? = null,
    @SerialName("animationType")     val animationType: String? = null,
    @SerialName("animationDuration") val animationDuration: Int? = null,
    @SerialName("isTimer")           val isTimer: Boolean? = null,
    @SerialName("timerMode")         val timerMode: String? = null,
    @SerialName("timerHours")        val timerHours: Int? = null,
    @SerialName("timerMinutes")      val timerMinutes: Int? = null,
    @SerialName("timerSeconds")      val timerSeconds: Int? = null,
    @SerialName("timerExpiredText")  val timerExpiredText: String? = null,
    @SerialName("targetHour")        val targetHour: Int? = null,
    @SerialName("targetMinute")      val targetMinute: Int? = null,
    @SerialName("liveClockFormat")   val liveClockFormat: String? = null,
) {
    /** Best available Bible book name. */
    val bookName: String?
        get() = bookNameCamel ?: bookNameSnake ?: bookNameKebab ?: bookNameShort

    /** Best available starting verse number. */
    val verseNumber: Int?
        get() = verseNumberCamel ?: verseNumberSnake ?: verseNumberShort

    /** Best available verse range string (e.g. "16-18"). */
    val verseRange: String?
        get() = verseRangeCamel ?: verseRangeSnake ?: verseRangeKebab ?: verseRangeVerses

    /** Human-readable display text for any item type. */
    val displayText: String?
        get() = displayTextCamel ?: displayTextSnake ?: displayTextKebab

    /** Best available picture folder ID. */
    val folderId: String?
        get() = folderIdCamel ?: folderIdSnake ?: folderIdKebab

    /** Best available picture folder name. */
    val folderName: String?
        get() = folderNameCamel ?: folderNameSnake ?: folderNameKebab

    /** Best available image index within the folder. */
    val imageIndex: Int?
        get() = imageIndexCamel ?: imageIndexSnake ?: imageIndexKebab ?: imageNumber

    /** Display name for the item — falls back through several fields. */
    val displayTitle: String
        get() {
            val typeLower = type?.lowercase()
            // For image/picture items: prefer displayText + folderName
            if (typeLower == "image" || typeLower == "picture") {
                val dt = displayText?.takeIf { it.isNotBlank() }
                val fn = folderName?.takeIf { it.isNotBlank() }
                return when {
                    dt != null && fn != null -> "$fn / $dt"
                    dt != null               -> dt
                    fn != null               -> fn
                    title != null && title.isNotBlank() -> title
                    else                     -> id ?: "Untitled"
                }
            }
            // Prefer explicit displayText for other types
            displayText?.takeIf { it.isNotBlank() }?.let { return it }
            // Prefer an explicit title from the server
            title?.takeIf { it.isNotBlank() }?.let { return it }
            // Build a Bible reference label when the relevant fields are present
            if (bookName != null && chapter != null && verseNumber != null) {
                val ref = when {
                    !verseRange.isNullOrBlank() -> "$bookName $chapter:$verseRange"
                    else                        -> "$bookName $chapter:$verseNumber"
                }
                return ref
            }
            // Last resort: id (UUID) or placeholder
            return id ?: "Untitled"
        }

    /** Whether this item is currently the active one in the service. */
    val active: Boolean
        get() = isActive ?: isActiveCamel ?: false

    /** Icon emoji that represents the item type. */
    val typeIcon: String
        get() = when (type?.lowercase()) {
            "song"         -> "🎵"
            "bible"        -> "📖"
            "presentation" -> "📽"
            "image","picture" -> "🖼"
            "video"        -> "🎬"
            "announcement" -> "📢"
            else           -> "📄"
        }
}

/**
 * Top-level response from GET /api/schedule.
 * The server may return either a JSON array or an object wrapping the list.
 */
@Serializable
data class ScheduleResponse(
    val items: List<ScheduleItem>? = null,
    val schedule: List<ScheduleItem>? = null,
    val total: Int? = null,
) {
    val allItems: List<ScheduleItem>
        get() = items ?: schedule ?: emptyList()
}

