package com.church.presenter.churchpresentermobile.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What counts as a chord, and what the audience is left with.
 *
 * The rule is shared with the desktop's ChordTransposer on purpose: the two apps
 * read the same songs, and a token one of them calls a chord and the other calls
 * a word is a word that goes missing off a screen.
 */
class ChordsTest {

    @Test
    fun ordinaryChordsAreRecognised() {
        listOf("G", "Am", "C#", "Bb", "F#m7", "Dsus4", "Gmaj7", "A/C#").forEach {
            assertTrue(Chords.isChord(it), "$it should be a chord")
        }
    }

    @Test
    fun sectionNamesAreNotChords() {
        // The whole reason for a strict rule: these are words the author wrote,
        // and stripping every bracketed token would delete them off the slide.
        listOf("Verse 1", "Bridge", "Repeat", "Chorus", "x2", "Instrumental").forEach {
            assertFalse(Chords.isChord(it), "$it should not be a chord")
        }
    }

    @Test
    fun chordsComeOutOfTheProjectedWords() {
        assertEquals(
            "Amazing grace how sweet the sound",
            Chords.stripChords("[G]Amazing grace how [C]sweet the [G]sound"),
        )
    }

    @Test
    fun bracketedWordsThatAreNotChordsSurvive() {
        assertEquals(
            "Praise him [Repeat]",
            Chords.stripChords("[D]Praise him [Repeat]"),
        )
    }

    @Test
    fun aSongWithNoChordsIsUntouched() {
        val plain = "Amazing grace how sweet the sound\nThat saved a wretch like me"
        assertEquals(plain, Chords.stripChords(plain))
    }

    @Test
    fun theGapLeftByAChordIsClosedUp() {
        // "[G] Amazing" would otherwise project with a leading space, and a chord
        // between words would leave a double one.
        assertEquals("Amazing grace", Chords.stripChords("[G] Amazing  [C]  grace"))
    }

    @Test
    fun everyLineOfAVerseIsCleaned() {
        assertEquals(
            "Amazing grace\nhow sweet the sound",
            Chords.stripChords("[G]Amazing grace\n[C]how sweet the [G]sound"),
        )
    }

    @Test
    fun hasChordsIgnoresBracketedProse() {
        assertTrue(Chords.hasChords("[Am]Hello"))
        assertFalse(Chords.hasChords("[Chorus]"))
        assertFalse(Chords.hasChords("No brackets at all"))
    }

    // ── Drawing a chart ──────────────────────────────────────────────────────

    @Test
    fun aChordOwnsTheWordsUntilTheNextOne() {
        val segments = Chords.parseLine("[G]Amazing grace how [C]sweet the sound")

        assertEquals(listOf("G", "C"), segments.map { it.chord })
        assertEquals("Amazing grace how ", segments[0].text)
        assertEquals("sweet the sound", segments[1].text)
    }

    @Test
    fun wordsBeforeTheFirstChordKeepTheirPlace() {
        val segments = Chords.parseLine("Amazing [C]grace")

        assertEquals(listOf("", "C"), segments.map { it.chord })
        assertEquals("Amazing ", segments[0].text)
    }

    @Test
    fun aLineWithNoChordsIsOneSegment() {
        val segments = Chords.parseLine("Amazing grace how sweet the sound")

        assertEquals(1, segments.size)
        assertEquals("", segments.single().chord)
        assertEquals("Amazing grace how sweet the sound", segments.single().text)
    }

    @Test
    fun withChordsOffTheLineComesBackClean() {
        // One renderer draws both views; this is the "off" half of that.
        val segments = Chords.parseLine("[G]Amazing grace", showChords = false)

        assertEquals(1, segments.size)
        assertEquals("", segments.single().chord)
        assertEquals("Amazing grace", segments.single().text)
    }

    @Test
    fun bracketedWordsAreNotTreatedAsChordsWhenSplitting() {
        val segments = Chords.parseLine("[D]Praise him [Repeat]")

        assertEquals(listOf("D"), segments.map { it.chord })
        assertEquals("Praise him [Repeat]", segments.single().text)
    }
}
