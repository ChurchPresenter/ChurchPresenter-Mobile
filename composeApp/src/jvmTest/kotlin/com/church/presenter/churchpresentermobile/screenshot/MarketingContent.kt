package com.church.presenter.churchpresentermobile.screenshot

import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.model.SongVerse
import com.church.presenter.churchpresentermobile.model.StrongsEntry

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

    /**
     * The books pane of the tablet's Bible tab. Enough to run past the bottom
     * of a landscape tablet — a pane that ends at Ruth with room to spare
     * reads as a Bible with twelve books in it.
     */
    val bibleBooks: List<BibleBook> = listOf(
        "Genesis" to 50, "Exodus" to 40, "Leviticus" to 27, "Numbers" to 36,
        "Deuteronomy" to 34, "Joshua" to 24, "Judges" to 21, "Ruth" to 4,
        "1 Samuel" to 31, "2 Samuel" to 24, "1 Kings" to 22, "2 Kings" to 25,
        "1 Chronicles" to 29, "2 Chronicles" to 36, "Ezra" to 10, "Nehemiah" to 13,
        "Esther" to 10, "Job" to 42, "Psalms" to 150, "Proverbs" to 31,
    ).map { (name, chapters) -> BibleBook(name = name, chapterTotal = chapters) }

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
        "And the evening and the morning were the third day.",
        "And God said, Let there be lights in the firmament of the heaven to divide the day " +
            "from the night; and let them be for signs, and for seasons, and for days, and years:",
        "And let them be for lights in the firmament of the heaven to give light upon the earth: " +
            "and it was so.",
        "And God made two great lights; the greater light to rule the day, and the lesser light " +
            "to rule the night: he made the stars also.",
        "And God set them in the firmament of the heaven to give light upon the earth,",
        "And to rule over the day and over the night, and to divide the light from the darkness: " +
            "and God saw that it was good.",
        "And the evening and the morning were the fourth day.",
        "And God said, Let the waters bring forth abundantly the moving creature that hath life, " +
            "and fowl that may fly above the earth in the open firmament of heaven.",
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

    /** The desktop's catalogue, as the tablet's Songs list pane shows it: the same hymns as [librarySongs]. */
    val songs: List<Song> = listOf(
        song(1, "1", "Amazing Grace", "John Newton", "Hymns"),
        song(2, "23", "Be Thou My Vision", "Mary E. Byrne", "Hymns"),
        song(3, "104", "How Great Thou Art", "Stuart K. Hine", "Hymns"),
        song(4, "88", "Great Is Thy Faithfulness", "Thomas O. Chisholm", "Hymns"),
        song(5, "7", "In Christ Alone", "Keith Getty", "Modern"),
        song(6, "56", "It Is Well With My Soul", "Horatio Spafford", "Hymns"),
        song(7, "12", "Holy, Holy, Holy", "Reginald Heber", "Hymns"),
        song(8, "77", "Come Thou Fount", "Robert Robinson", "Hymns"),
        song(9, "31", "Be Still, My Soul", "Katharina von Schlegel", "Hymns"),
        song(10, "45", "Rock of Ages", "Augustus Toplady", "Hymns"),
        song(11, "19", "10,000 Reasons", "Matt Redman", "Modern"),
        song(12, "63", "Crown Him with Many Crowns", "Matthew Bridges", "Hymns"),
    )

    /**
     * A library somebody has been using for a while.
     *
     * The first song is complete — every verse, its writer, its songbook and
     * its copyright line — because it is the one the tablet image opens in
     * the editor, where every one of those is a field and an empty field
     * reads as a feature nobody filled in.
     */
    val librarySongs: List<LocalSong> = listOf(
        LocalSong(
            id = "s1",
            number = "1",
            title = "Amazing Grace",
            author = "John Newton",
            bookName = "Hymns",
            copyright = "Public domain",
            sections = amazingGrace.verses.orEmpty().map { verse ->
                LocalSongSection(SectionType.VERSE, verse.lines.orEmpty().joinToString("\n"))
            },
        ),
        localSong("s2", "23", "Be Thou My Vision", "Mary E. Byrne", "Be thou my vision, O Lord of my heart"),
        localSong("s3", "104", "How Great Thou Art", "Stuart K. Hine", "O Lord my God, when I in awesome wonder"),
        localSong(
            "s4", "88", "Great Is Thy Faithfulness", "Thomas O. Chisholm", "Great is thy faithfulness, O God my Father",
        ),
        localSong("s5", "7", "In Christ Alone", "Keith Getty", "In Christ alone my hope is found"),
        localSong(
            "s6", "56", "It Is Well With My Soul", "Horatio Spafford", "When peace like a river attendeth my way",
        ),
        localSong("s7", "12", "Holy, Holy, Holy", "Reginald Heber", "Holy, holy, holy! Lord God Almighty!"),
        localSong("s8", "77", "Come Thou Fount", "Robert Robinson", "Come thou fount of every blessing"),
    )

    /**
     * Strong's entries for the tablet's More tab, which opens the dictionary
     * in its second pane. Enough of them, in both languages, that the pane
     * reads as a lexicon rather than as a search that found two words.
     */
    val dictionary: List<StrongsEntry> = listOf(
        strongs("H1254", "bara", "baw-raw", "to shape, create; compare H1262 and G26", 54),
        strongs("H430", "elohim", "el-o-heem", "gods in the ordinary sense; God, the supreme God", 2606),
        strongs("H7965", "shalom", "shaw-lome", "safe; well, happy, friendly; welfare, health, prosperity, peace", 237),
        strongs("H2617", "chesed", "kheh-sed", "kindness; by implication piety, favour, mercy, loving-kindness", 248),
        strongs("H8085", "shama", "shaw-mah", "to hear intelligently; often with implication of attention", 1159),
        strongs("G26", "agape", "ag-ah-pay", "love, affection, benevolence", 116),
        strongs("G3056", "logos", "log-os", "something said; a word, a topic, reasoning, the Divine Expression", 330),
        strongs("G5485", "charis", "khar-ece", "graciousness; the divine influence upon the heart; gratitude", 156),
        strongs("G4102", "pistis", "pis-tis", "persuasion, credence; moral conviction; reliance upon Christ", 244),
        strongs("G1515", "eirene", "i-ray-nay", "peace; by implication prosperity; quietness, rest", 92),
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

    private fun song(id: Int, number: String, title: String, author: String, book: String) =
        Song(id = id, number = number, title = title, author = author, bookName = book)

    private fun localSong(id: String, number: String, title: String, author: String, firstLine: String) =
        LocalSong(
            id = id,
            number = number,
            title = title,
            author = author,
            bookName = "Hymns",
            sections = listOf(LocalSongSection(SectionType.VERSE, firstLine)),
        )

    private fun strongs(number: String, word: String, said: String, definition: String, uses: Int) =
        StrongsEntry(
            number = number,
            word = word,
            transliteration = said,
            pronunciation = "$said'",
            definition = definition,
            occurrences = uses,
        )
}
