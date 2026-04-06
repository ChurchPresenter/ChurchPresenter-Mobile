package com.church.presenter.churchpresentermobile.model

/**
 * Static demo content displayed when the `is_demo_mode` Remote Config flag is true.
 *
 * All data is hardcoded so the app works with zero network access, making it
 * suitable for App Store / Play Store review submissions, trade-show demos, and
 * offline presentations.
 */
object DemoData {

    // ─── Bible ───────────────────────────────────────────────────────────────

    val books: List<BibleBook> = listOf(
        BibleBook(id = "genesis",     name = "Genesis",     chapterTotal = 50,  testament = "Old Testament"),
        BibleBook(id = "psalms",      name = "Psalms",      chapterTotal = 150, testament = "Old Testament"),
        BibleBook(id = "proverbs",    name = "Proverbs",    chapterTotal = 31,  testament = "Old Testament"),
        BibleBook(id = "isaiah",      name = "Isaiah",      chapterTotal = 66,  testament = "Old Testament"),
        BibleBook(id = "matthew",     name = "Matthew",     chapterTotal = 28,  testament = "New Testament"),
        BibleBook(id = "john",        name = "John",        chapterTotal = 21,  testament = "New Testament"),
        BibleBook(id = "acts",        name = "Acts",        chapterTotal = 28,  testament = "New Testament"),
        BibleBook(id = "romans",      name = "Romans",      chapterTotal = 16,  testament = "New Testament"),
        BibleBook(id = "ephesians",   name = "Ephesians",   chapterTotal = 6,   testament = "New Testament"),
        BibleBook(id = "philippians", name = "Philippians", chapterTotal = 4,   testament = "New Testament"),
        BibleBook(id = "revelation",  name = "Revelation",  chapterTotal = 22,  testament = "New Testament"),
    )

    private val verseMap: Map<Pair<String, Int>, List<BibleVerse>> = mapOf(
        ("genesis" to 1) to listOf(
            BibleVerse(verse = 1, text = "In the beginning God created the heavens and the earth."),
            BibleVerse(verse = 2, text = "Now the earth was formless and empty, darkness was over the surface of the deep, and the Spirit of God was hovering over the waters."),
            BibleVerse(verse = 3, text = "And God said, \"Let there be light,\" and there was light."),
            BibleVerse(verse = 4, text = "God saw that the light was good, and he separated the light from the darkness."),
            BibleVerse(verse = 5, text = "God called the light \"day,\" and the darkness he called \"night.\" And there was evening, and there was morning—the first day."),
        ),
        ("psalms" to 23) to listOf(
            BibleVerse(verse = 1, text = "The Lord is my shepherd; I shall not want."),
            BibleVerse(verse = 2, text = "He makes me lie down in green pastures. He leads me beside still waters."),
            BibleVerse(verse = 3, text = "He restores my soul. He leads me in paths of righteousness for his name's sake."),
            BibleVerse(verse = 4, text = "Even though I walk through the valley of the shadow of death, I will fear no evil, for you are with me; your rod and your staff, they comfort me."),
            BibleVerse(verse = 5, text = "You prepare a table before me in the presence of my enemies; you anoint my head with oil; my cup overflows."),
            BibleVerse(verse = 6, text = "Surely goodness and mercy shall follow me all the days of my life, and I shall dwell in the house of the Lord forever."),
        ),
        ("proverbs" to 3) to listOf(
            BibleVerse(verse = 5, text = "Trust in the Lord with all your heart and lean not on your own understanding;"),
            BibleVerse(verse = 6, text = "in all your ways submit to him, and he will make your paths straight."),
        ),
        ("isaiah" to 40) to listOf(
            BibleVerse(verse = 28, text = "Do you not know? Have you not heard? The Lord is the everlasting God, the Creator of the ends of the earth."),
            BibleVerse(verse = 29, text = "He gives strength to the weary and increases the power of the weak."),
            BibleVerse(verse = 30, text = "Even youths grow tired and weary, and young men stumble and fall;"),
            BibleVerse(verse = 31, text = "but those who hope in the Lord will renew their strength. They will soar on wings like eagles; they will run and not grow weary, they will walk and not be faint."),
        ),
        ("matthew" to 5) to listOf(
            BibleVerse(verse = 3,  text = "Blessed are the poor in spirit, for theirs is the kingdom of heaven."),
            BibleVerse(verse = 4,  text = "Blessed are those who mourn, for they will be comforted."),
            BibleVerse(verse = 5,  text = "Blessed are the meek, for they will inherit the earth."),
            BibleVerse(verse = 6,  text = "Blessed are those who hunger and thirst for righteousness, for they will be filled."),
            BibleVerse(verse = 7,  text = "Blessed are the merciful, for they will be shown mercy."),
            BibleVerse(verse = 8,  text = "Blessed are the pure in heart, for they will see God."),
            BibleVerse(verse = 9,  text = "Blessed are the peacemakers, for they will be called children of God."),
        ),
        ("john" to 3) to listOf(
            BibleVerse(verse = 1,  text = "Now there was a Pharisee, a man named Nicodemus who was a member of the Jewish ruling council."),
            BibleVerse(verse = 2,  text = "He came to Jesus at night and said, \"Rabbi, we know that you are a teacher who has come from God, for no one could perform the signs you are doing if God were not with him.\""),
            BibleVerse(verse = 3,  text = "Jesus replied, \"Very truly I tell you, no one can see the kingdom of God unless they are born again.\""),
            BibleVerse(verse = 14, text = "Just as Moses lifted up the snake in the wilderness, so the Son of Man must be lifted up,"),
            BibleVerse(verse = 15, text = "that everyone who believes may have eternal life in him."),
            BibleVerse(verse = 16, text = "For God so loved the world that he gave his one and only Son, that whoever believes in him shall not perish but have eternal life."),
            BibleVerse(verse = 17, text = "For God did not send his Son into the world to condemn the world, but to save the world through him."),
        ),
        ("romans" to 8) to listOf(
            BibleVerse(verse = 1,  text = "Therefore, there is now no condemnation for those who are in Christ Jesus."),
            BibleVerse(verse = 28, text = "And we know that in all things God works for the good of those who love him, who have been called according to his purpose."),
            BibleVerse(verse = 38, text = "For I am convinced that neither death nor life, neither angels nor demons, neither the present nor the future, nor any powers,"),
            BibleVerse(verse = 39, text = "neither height nor depth, nor anything else in all creation, will be able to separate us from the love of God that is in Christ Jesus our Lord."),
        ),
        ("ephesians" to 2) to listOf(
            BibleVerse(verse = 8, text = "For it is by grace you have been saved, through faith—and this is not from yourselves, it is the gift of God—"),
            BibleVerse(verse = 9, text = "not by works, so that no one can boast."),
            BibleVerse(verse = 10, text = "For we are God's handiwork, created in Christ Jesus to do good works, which God prepared in advance for us to do."),
        ),
        ("philippians" to 4) to listOf(
            BibleVerse(verse = 4,  text = "Rejoice in the Lord always. I will say it again: Rejoice!"),
            BibleVerse(verse = 6,  text = "Do not be anxious about anything, but in every situation, by prayer and petition, with thanksgiving, present your requests to God."),
            BibleVerse(verse = 7,  text = "And the peace of God, which transcends all understanding, will guard your hearts and your minds in Christ Jesus."),
            BibleVerse(verse = 13, text = "I can do all this through him who gives me strength."),
        ),
    )

    /**
     * Returns demo verses for the given book name and chapter.
     * Falls back to generic demo verses if no specific data exists.
     */
    fun getVerses(bookDisplayName: String, chapter: Int): List<BibleVerse> {
        val key = bookDisplayName.trim().lowercase() to chapter
        return verseMap[key]
            ?: verseMap.entries.firstOrNull { (k, _) -> k.first == bookDisplayName.trim().lowercase() }?.value
            ?: listOf(
                BibleVerse(verse = 1, text = "Demo — $bookDisplayName $chapter:1 — \"The Lord is good.\""),
                BibleVerse(verse = 2, text = "Demo — $bookDisplayName $chapter:2 — \"His mercy endures forever.\""),
                BibleVerse(verse = 3, text = "Demo — $bookDisplayName $chapter:3 — \"Give thanks to the Lord.\""),
                BibleVerse(verse = 4, text = "Demo — $bookDisplayName $chapter:4 — \"He is faithful in all things.\""),
                BibleVerse(verse = 5, text = "Demo — $bookDisplayName $chapter:5 — \"Blessed are all who trust in Him.\""),
            )
    }

    // ─── Songs ────────────────────────────────────────────────────────────────

    val songs: List<Song> = listOf(
        Song(number = "1",  title = "Amazing Grace",              tune = "New Britain",  author = "John Newton",    bookName = "Hymnal"),
        Song(number = "2",  title = "How Great Thou Art",         tune = "O Store Gud",  author = "Carl Boberg",    bookName = "Hymnal"),
        Song(number = "3",  title = "Great Is Thy Faithfulness",  tune = "Faithfulness", author = "Thomas Chisholm",bookName = "Hymnal"),
        Song(number = "4",  title = "Holy, Holy, Holy",           tune = "Nicaea",       author = "Reginald Heber", bookName = "Hymnal"),
        Song(number = "5",  title = "It Is Well With My Soul",    tune = "Ville du Havre",author = "Horatio Spafford",bookName = "Hymnal"),
        Song(number = "6",  title = "Be Thou My Vision",          tune = "Slane",        author = "Dallan Forgaill",bookName = "Hymnal"),
        Song(number = "7",  title = "10,000 Reasons",             author = "Matt Redman",                           bookName = "Contemporary"),
        Song(number = "8",  title = "Oceans (Where Feet May Fail)",author = "Hillsong United",                      bookName = "Contemporary"),
        Song(number = "9",  title = "Way Maker",                  author = "Sinach",                                bookName = "Contemporary"),
        Song(number = "10", title = "Good Good Father",           author = "Chris Tomlin",                          bookName = "Contemporary"),
    )

    private val songDetailMap: Map<String, SongDetail> = mapOf(
        "1" to SongDetail(
            number = "1", title = "Amazing Grace",
            author = "John Newton", bookNameKebab = "Hymnal",
            verses = listOf(
                SongVerse(number = 1, label = "Verse 1", lines = listOf(
                    "Amazing grace, how sweet the sound",
                    "That saved a wretch like me!",
                    "I once was lost, but now am found,",
                    "Was blind, but now I see.",
                )),
                SongVerse(number = 2, label = "Verse 2", lines = listOf(
                    "'Twas grace that taught my heart to fear,",
                    "And grace my fears relieved;",
                    "How precious did that grace appear",
                    "The hour I first believed.",
                )),
                SongVerse(number = 3, label = "Verse 3", lines = listOf(
                    "Through many dangers, toils, and snares,",
                    "I have already come;",
                    "'Tis grace has brought me safe thus far,",
                    "And grace will lead me home.",
                )),
                SongVerse(number = 4, label = "Verse 4", lines = listOf(
                    "When we've been there ten thousand years,",
                    "Bright shining as the sun,",
                    "We've no less days to sing God's praise",
                    "Than when we'd first begun.",
                )),
            ),
        ),
        "2" to SongDetail(
            number = "2", title = "How Great Thou Art",
            author = "Carl Boberg", bookNameKebab = "Hymnal",
            verses = listOf(
                SongVerse(number = 1, label = "Verse 1", lines = listOf(
                    "O Lord my God, when I in awesome wonder",
                    "Consider all the worlds Thy hands have made,",
                    "I see the stars, I hear the rolling thunder,",
                    "Thy power throughout the universe displayed.",
                )),
                SongVerse(number = 0, label = "Chorus", lines = listOf(
                    "Then sings my soul, my Saviour God, to Thee:",
                    "How great Thou art, how great Thou art!",
                    "Then sings my soul, my Saviour God, to Thee:",
                    "How great Thou art, how great Thou art!",
                )),
                SongVerse(number = 2, label = "Verse 2", lines = listOf(
                    "When through the woods and forest glades I wander",
                    "And hear the birds sing sweetly in the trees,",
                    "When I look down from lofty mountain grandeur",
                    "And hear the brook and feel the gentle breeze:",
                )),
                SongVerse(number = 3, label = "Verse 3", lines = listOf(
                    "And when I think that God, His Son not sparing,",
                    "Sent Him to die, I scarce can take it in;",
                    "That on the cross, my burden gladly bearing,",
                    "He bled and died to take away my sin:",
                )),
            ),
        ),
        "3" to SongDetail(
            number = "3", title = "Great Is Thy Faithfulness",
            author = "Thomas Chisholm", bookNameKebab = "Hymnal",
            verses = listOf(
                SongVerse(number = 1, label = "Verse 1", lines = listOf(
                    "Great is Thy faithfulness, O God my Father,",
                    "There is no shadow of turning with Thee;",
                    "Thou changest not, Thy compassions they fail not;",
                    "As Thou hast been, Thou forever wilt be.",
                )),
                SongVerse(number = 0, label = "Chorus", lines = listOf(
                    "Great is Thy faithfulness! Great is Thy faithfulness!",
                    "Morning by morning new mercies I see;",
                    "All I have needed Thy hand hath provided,",
                    "Great is Thy faithfulness, Lord, unto me!",
                )),
                SongVerse(number = 2, label = "Verse 2", lines = listOf(
                    "Summer and winter and springtime and harvest,",
                    "Sun, moon and stars in their courses above",
                    "Join with all nature in manifold witness",
                    "To Thy great faithfulness, mercy and love.",
                )),
            ),
        ),
        "7" to SongDetail(
            number = "7", title = "10,000 Reasons",
            author = "Matt Redman", bookNameKebab = "Contemporary",
            verses = listOf(
                SongVerse(number = 0, label = "Chorus", lines = listOf(
                    "Bless the Lord, O my soul, O my soul,",
                    "Worship His holy name.",
                    "Sing like never before, O my soul,",
                    "I'll worship Your holy name.",
                )),
                SongVerse(number = 1, label = "Verse 1", lines = listOf(
                    "The sun comes up, it's a new day dawning,",
                    "It's time to sing Your song again.",
                    "Whatever may pass and whatever lies before me,",
                    "Let me be singing when the evening comes.",
                )),
                SongVerse(number = 2, label = "Verse 2", lines = listOf(
                    "You're rich in love and You're slow to anger,",
                    "Your name is great and Your heart is kind;",
                    "For all Your goodness I will keep on singing,",
                    "Ten thousand reasons for my heart to find.",
                )),
                SongVerse(number = 3, label = "Verse 3", lines = listOf(
                    "And on that day when my strength is failing,",
                    "The end draws near and my time has come;",
                    "Still, my soul will sing Your praise unending—",
                    "Ten thousand years and then forevermore!",
                )),
            ),
        ),
        "9" to SongDetail(
            number = "9", title = "Way Maker",
            author = "Sinach", bookNameKebab = "Contemporary",
            verses = listOf(
                SongVerse(number = 1, label = "Verse", lines = listOf(
                    "You are here, moving in our midst,",
                    "I worship You, I worship You.",
                    "You are here, working in this place,",
                    "I worship You, I worship You.",
                )),
                SongVerse(number = 0, label = "Chorus", lines = listOf(
                    "Way Maker, Miracle Worker, Promise Keeper,",
                    "Light in the darkness, my God,",
                    "That is who You are.",
                )),
                SongVerse(number = 2, label = "Bridge", lines = listOf(
                    "Even when I don't see it, You're working.",
                    "Even when I don't feel it, You're working.",
                    "You never stop, You never stop working.",
                    "You never stop, You never stop working.",
                )),
            ),
        ),
    )

    /**
     * Returns full song detail for [songNumber], falling back to a generic demo detail.
     */
    fun getSongDetail(songNumber: String): SongDetail =
        songDetailMap[songNumber]
            ?: songs.firstOrNull { it.number == songNumber }?.let { song ->
                SongDetail(
                    number        = song.number,
                    title         = song.title,
                    author        = song.author,
                    bookNameKebab = song.bookName,
                    verses = listOf(
                        SongVerse(number = 1, label = "Verse 1", lines = listOf(
                            "Demo verse — ${song.title}",
                            "Second line of verse 1",
                        )),
                        SongVerse(number = 0, label = "Chorus", lines = listOf(
                            "Demo chorus — ${song.title}",
                            "Second line of chorus",
                        )),
                        SongVerse(number = 2, label = "Verse 2", lines = listOf(
                            "Demo verse 2 — ${song.title}",
                            "Second line of verse 2",
                        )),
                    ),
                )
            }
            ?: SongDetail(
                number = songNumber, title = "Demo Song",
                verses = listOf(SongVerse(number = 1, label = "Verse 1", lines = listOf("Demo verse text"))),
            )

    // ─── Presentations ────────────────────────────────────────────────────────

    val presentations: List<Presentation> = listOf(
        Presentation(
            id        = "demo-pres-1",
            fileName  = "Sunday_Worship_Service.pptx",
            fileType  = "pptx",
            slideTotal = 5,
            slides    = (0 until 5).map { PresentationSlide(slideIndex = it) },
        ),
        Presentation(
            id        = "demo-pres-2",
            fileName  = "Weekly_Announcements.pptx",
            fileType  = "pptx",
            slideTotal = 3,
            slides    = (0 until 3).map { PresentationSlide(slideIndex = it) },
        ),
        Presentation(
            id        = "demo-pres-3",
            fileName  = "Welcome_Visitors.pdf",
            fileType  = "pdf",
            slideTotal = 2,
            slides    = (0 until 2).map { PresentationSlide(slideIndex = it) },
        ),
        Presentation(
            id        = "demo-pres-4",
            fileName  = "Sermon_Notes_John_3.pptx",
            fileType  = "pptx",
            slideTotal = 8,
            slides    = (0 until 8).map { PresentationSlide(slideIndex = it) },
        ),
    )

    // ─── Pictures ─────────────────────────────────────────────────────────────

    val picturesFolder: PicturesFolder = PicturesFolder(
        folderId   = "demo-folder-1",
        folderName = "Demo Pictures",
        folderPath = "/demo/pictures",
        imageTotal = 6,
        images = (0 until 6).map { i ->
            PictureImage(
                index    = i,
                fileName = "demo_image_${i + 1}.jpg",
            )
        },
    )

    // ─── Schedule ─────────────────────────────────────────────────────────────

    val scheduleItems: List<ScheduleItem> = listOf(
        // Opening song
        ScheduleItem(
            id               = "demo-sched-1",
            type             = "song",
            title            = "Amazing Grace",
            displayTextCamel = "1 - Amazing Grace",
            index            = 0,
        ),
        // Bible reading — Psalm 23
        ScheduleItem(
            id               = "demo-sched-2",
            type             = "bible",
            title            = "Psalms 23:1-6",
            bookNameCamel    = "Psalms",
            chapter          = 23,
            verseNumberCamel = 1,
            verseRangeCamel  = "1-6",
            displayTextCamel = "Psalms 23:1-6",
            index            = 1,
        ),
        // Second song
        ScheduleItem(
            id               = "demo-sched-3",
            type             = "song",
            title            = "How Great Thou Art",
            displayTextCamel = "2 - How Great Thou Art",
            index            = 2,
        ),
        // Sermon Bible reference — John 3:16
        ScheduleItem(
            id               = "demo-sched-4",
            type             = "bible",
            title            = "John 3:16",
            bookNameCamel    = "John",
            chapter          = 3,
            verseNumberCamel = 16,
            verseRangeCamel  = "16",
            displayTextCamel = "John 3:16",
            index            = 3,
        ),
        // Presentation — sermon slides
        ScheduleItem(
            id               = "demo-pres-4",
            type             = "presentation",
            title            = "Sermon Notes — John 3",
            displayTextCamel = "Sermon_Notes_John_3.pptx",
            index            = 4,
        ),
        // Picture
        ScheduleItem(
            id               = "demo-sched-6",
            type             = "picture",
            title            = "Demo Pictures",
            folderIdCamel    = "demo-folder-1",
            folderNameCamel  = "Demo Pictures",
            imageIndexCamel  = 0,
            displayTextCamel = "demo_image_1.jpg",
            index            = 5,
        ),
        // Closing song
        ScheduleItem(
            id               = "demo-sched-7",
            type             = "song",
            title            = "10,000 Reasons",
            displayTextCamel = "7 - 10,000 Reasons",
            index            = 6,
            isActive         = true,
        ),
    )
}

