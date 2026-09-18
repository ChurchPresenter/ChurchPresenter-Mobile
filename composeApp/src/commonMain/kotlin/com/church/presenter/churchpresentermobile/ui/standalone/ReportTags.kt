package com.church.presenter.churchpresentermobile.ui.standalone

/**
 * Names the CCLI report's controls for a UI test.
 *
 * Its own object rather than more of [StandaloneTags], which is already past
 * the function-count gate; see that file for why tags rather than labels.
 */
internal object ReportTags {
    const val EMPTY = "report:empty"
    const val EMPTY_RANGE = "report:emptyRange"
    const val FROM = "report:from"
    const val TO = "report:to"
    const val YEAR = "report:year"
    const val FILTER = "report:filter"
    const val SONG_PLAYS = "report:songPlays"
    const val UNIQUE_SONGS = "report:uniqueSongs"
    const val VERSE_PLAYS = "report:versePlays"
    const val UNIQUE_VERSES = "report:uniqueVerses"
    const val BUSIEST = "report:busiest"
    const val EXPORT_CSV = "report:exportCsv"
    const val EXPORT_XLS = "report:exportXls"
    const val CLEAR = "report:clear"
    const val CLEAR_CONFIRM = "report:clear:confirm"
    const val CLEAR_CANCEL = "report:clear:cancel"

    /** One of the quick ranges, by its position: 3M, 6M, 12M, All. */
    fun preset(index: Int) = "report:preset:$index"

    /** One of the three views, by its position: Songs, Bible, Activity. */
    fun tab(index: Int) = "report:tab:$index"

    /** One year offered under the Year menu. */
    fun year(year: Int) = "report:year:$year"

    /** One songbook or translation offered by the filter; null is "all". */
    fun filterOption(name: String?) = "report:filter:${name ?: "all"}"

    /** One song in the phone's list or the tablet's table, by its position. */
    fun song(rank: Int) = "report:song:$rank"

    /** One song in the tablet's rankings pane, by its position. */
    fun rankedSong(rank: Int) = "report:ranked:$rank"

    /** One ranked verse, by its position. */
    fun verse(rank: Int) = "report:verse:$rank"

    /** One book in the top-books list. */
    fun book(name: String) = "report:book:$name"
}
