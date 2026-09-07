package com.church.presenter.churchpresentermobile.screenshot

import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.model.SongVerse

/**
 * Content for the store and website images.
 *
 * Deliberately separate from the test fixtures. A golden wants the smallest
 * input that proves the layout — `chapterOne` is three clipped verses, which is
 * exactly right for a diff and looks, on a listing page, like an app with
 * almost nothing in it. Half of that screenshot was empty background.
 *
 * So this is what a real phone would be showing on a Sunday: a whole chapter
 * that fills the screen and scrolls past the bottom, a hymn with all its verses,
 * a library somebody has actually used. All public-domain text (KJV, and hymns
 * long out of copyright), so nothing here needs clearing to publish.
 */
internal object MarketingContent {

    /** Genesis 1 — enough verses to fill a phone and run off the bottom. */
    val genesisOne: List<BibleVerse> = listOf(
        "In the beginning God created the heaven and the earth.",
        "And the earth was without form, and void; and darkness was upon the face of the deep. " +
            "And the Spirit of God moved upon the face of the waters.",
        "And God said, Let there be light: and there was light.",
        "And God saw the light, that it was good: and God divided the light from the darkness.",
        "And God called the light Day, and the darkness he called Night. " +
            "And the evening and the morning were the first day.",
        "And God said, Let there be a firmament in the midst of the waters, " +
            "and let it divide the waters from the waters.",
        "And God made the firmament, and divided the waters which were under the firmament " +
            "from the waters which were above the firmament: and it was so.",
        "And God called the firmament Heaven. And the evening and the morning were the second day.",
        "And God said, Let the waters under the heaven be gathered together unto one place, " +
            "and let the dry land appear: and it was so.",
        "And God called the dry land Earth; and the gathering together of the waters called he Seas: " +
            "and God saw that it was good.",
        "And God said, Let the earth bring forth grass, the herb yielding seed, " +
            "and the fruit tree yielding fruit after his kind, whose seed is in itself, upon the earth: " +
            "and it was so.",
        "And the earth brought forth grass, and herb yielding seed after his kind, " +
            "and the tree yielding fruit, whose seed was in itself, after his kind: " +
            "and God saw that it was good.",
    ).mapIndexed { index, text -> BibleVerse(verse = index + 1, text = text) }

    /** Amazing Grace, all of it — the shape a hymn actually has on screen. */
    val amazingGrace: SongDetail = SongDetail(
        number = "1",
        title = "Amazing Grace",
        author = "John Newton",
        tune = "New Britain",
        songbook = "Hymns",
        verses = listOf(
            verse(
                "Verse 1",
                "Amazing grace! how sweet the sound",
                "That saved a wretch like me!",
                "I once was lost, but now am found,",
                "Was blind, but now I see.",
            ),
            verse(
                "Verse 2",
                "'Twas grace that taught my heart to fear,",
                "And grace my fears relieved;",
                "How precious did that grace appear",
                "The hour I first believed!",
            ),
            verse(
                "Verse 3",
                "Through many dangers, toils and snares,",
                "I have already come;",
                "'Tis grace hath brought me safe thus far,",
                "And grace will lead me home.",
            ),
            verse(
                "Verse 4",
                "When we've been there ten thousand years,",
                "Bright shining as the sun,",
                "We've no less days to sing God's praise",
                "Than when we'd first begun.",
            ),
        ),
    )

    /** A library somebody has been using for a while. */
    val librarySongs: List<LocalSong> = listOf(
        localSong("s1", "1", "Amazing Grace", "Amazing grace! how sweet the sound"),
        localSong("s2", "23", "Be Thou My Vision", "Be thou my vision, O Lord of my heart"),
        localSong("s3", "104", "How Great Thou Art", "O Lord my God, when I in awesome wonder"),
        localSong("s4", "88", "Great Is Thy Faithfulness", "Great is thy faithfulness, O God my Father"),
        localSong("s5", "7", "In Christ Alone", "In Christ alone my hope is found"),
        localSong("s6", "56", "It Is Well With My Soul", "When peace like a river attendeth my way"),
        localSong("s7", "12", "Holy, Holy, Holy", "Holy, holy, holy! Lord God Almighty!"),
        localSong("s8", "77", "Come Thou Fount", "Come thou fount of every blessing"),
    )

    val notices: List<LocalAnnouncement> = listOf(
        LocalAnnouncement(
            id = "a1",
            title = "Welcome",
            body = "Tea and coffee in the hall after the service — please stay and say hello.",
        ),
        LocalAnnouncement(
            id = "a2",
            title = "Working bee",
            body = "Saturday from 9am. Bring gloves and a plate for morning tea.",
        ),
        LocalAnnouncement(
            id = "a3",
            title = "Youth group",
            body = "Friday 7pm in the hall. Friends welcome.",
        ),
    )

    /** The same hymn as a deck, for the standalone controller's preview. */
    val amazingGraceDeck: SlideDeck = SlideDeck(
        kind = SlideKind.SONG,
        title = "Amazing Grace",
        slides = amazingGrace.verses.orEmpty().map { verse ->
            Slide(
                kind = SlideKind.SONG,
                body = verse.lines.orEmpty().joinToString("\n"),
                reference = "Amazing Grace",
                footer = verse.label,
            )
        },
    )

    private fun verse(label: String, vararg lines: String) =
        SongVerse(label = label, lines = lines.toList())

    private fun localSong(id: String, number: String, title: String, firstLine: String) =
        LocalSong(
            id = id,
            number = number,
            title = title,
            sections = listOf(LocalSongSection(SectionType.VERSE, firstLine)),
        )
}
