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
}
