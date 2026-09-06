package com.church.presenter.churchpresentermobile.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Which words in a definition become tappable Strong's links.
 *
 * A Strong's definition is written as prose with cross-references embedded in
 * it ("from H1262"), and those references are the only way to walk from one
 * entry to a related one. Two things can go wrong and neither shows up in a
 * screenshot: a reference that is not linked is a dead end, and the entry's own
 * number linked back to itself is a loop that reloads the sheet the operator is
 * already reading.
 *
 * Asserted against [definitionRuns] rather than the rendered `AnnotatedString`,
 * which is where the decision actually lives.
 */
class DictionaryDefinitionLinksTest {

    private fun links(definition: String, own: String = "H1254") =
        definitionRuns(definition, own).mapNotNull { it.link }

    private fun rendered(definition: String, own: String = "H1254") =
        definitionRuns(definition, own).joinToString("") { it.text }

    // ── What becomes a link ──────────────────────────────────────────────

    @Test
    fun aHebrewReferenceBecomesALink() {
        assertEquals(listOf("H1262"), links("from H1262"))
    }

    @Test
    fun aGreekReferenceBecomesALink() {
        assertEquals(listOf("G26"), links("compare G26"))
    }

    @Test
    fun everyReferenceInADefinitionBecomesALink() {
        assertEquals(listOf("H1262", "G26"), links("from H1262 and G26"))
    }

    @Test
    fun theSameReferenceTwiceIsLinkedTwice() {
        // Both are tappable — the operator may reach for either one.
        assertEquals(listOf("H1262", "H1262"), links("H1262 or H1262"))
    }

    @Test
    fun aDefinitionWithNoReferencesHasNoLinks() {
        assertTrue(links("to shape, fashion, create").isEmpty())
    }

    @Test
    fun anEntryDoesNotLinkToItself() {
        assertTrue(links("a form of H1254", own = "H1254").isEmpty())
    }

    @Test
    fun anEntrysOwnNumberIsStillShown() {
        // Not a link, but the words must survive: dropping it would change the
        // definition the operator reads.
        assertEquals("a form of H1254", rendered("a form of H1254"))
    }

    @Test
    fun otherReferencesSurviveTheEntrysOwnNumber() {
        assertEquals(listOf("H1262"), links("H1254 from H1262", own = "H1254"))
    }

    @Test
    fun aGreekEntryDoesNotLinkToItself() {
        assertTrue(links("see G26", own = "G26").isEmpty())
    }

    // ── The words either side ────────────────────────────────────────────

    @Test
    fun theWholeDefinitionSurvivesLinking() {
        val definition = "to shape, create; from H1262 and G26, compare H430"
        assertEquals(definition, rendered(definition))
    }

    @Test
    fun theWordsBeforeAReferenceAreKept() {
        val runs = definitionRuns("from H1262", "H1254")
        assertEquals("from ", runs.first().text)
    }

    @Test
    fun theWordsBeforeAReferenceAreNotALink() {
        val runs = definitionRuns("from H1262", "H1254")
        assertNull(runs.first().link)
    }

    @Test
    fun theWordsAfterAReferenceAreKept() {
        val runs = definitionRuns("from H1262 in the sense of cutting", "H1254")
        assertEquals(" in the sense of cutting", runs.last().text)
    }

    @Test
    fun aDefinitionStartingWithAReferenceHasNoEmptyLeadingRun() {
        val runs = definitionRuns("H1262 is the root", "H1254")
        assertEquals("H1262", runs.first().text)
    }

    @Test
    fun aDefinitionEndingWithAReferenceHasNoEmptyTrailingRun() {
        val runs = definitionRuns("the root is H1262", "H1254")
        assertEquals("H1262", runs.last().text)
    }

    @Test
    fun anEmptyDefinitionProducesNoRuns() {
        assertTrue(definitionRuns("", "H1254").isEmpty())
    }

    @Test
    fun aDefinitionOfOnlyAReferenceIsOneLink() {
        assertEquals(listOf(DefinitionRun("H1262", "H1262")), definitionRuns("H1262", "H1254"))
    }

    @Test
    fun aLinkCarriesTheNumberItShows() {
        val run = definitionRuns("from H1262", "H1254").last()
        assertEquals(run.text, run.link)
    }

    // ── What is not a reference ──────────────────────────────────────────

    @Test
    fun aBareLetterIsNotAReference() {
        assertTrue(links("the letter H stands alone").isEmpty())
    }

    @Test
    fun aBareNumberIsNotAReference() {
        assertTrue(links("occurs 1262 times").isEmpty())
    }

    @Test
    fun aLowercaseLetterIsNotAReference() {
        assertTrue(links("h1262 in lower case").isEmpty())
    }

    @Test
    fun anotherLetterIsNotAReference() {
        assertTrue(links("see A1262").isEmpty())
    }

    @Test
    fun aFiveDigitReferenceIsStillAReference() {
        assertEquals(listOf("H12345"), links("see H12345"))
    }

    @Test
    fun aSixDigitRunIsClippedToFiveDigits() {
        // Strong's numbers stop at five digits; the sixth is not part of the
        // reference and must stay as text.
        val runs = definitionRuns("see H123456", "H1254")
        assertEquals(listOf("H12345"), runs.mapNotNull { it.link })
        assertEquals("see H123456", runs.joinToString("") { it.text })
    }

    @Test
    fun aReferenceInBracketsIsStillLinked() {
        assertEquals(listOf("H1262"), links("a root (H1262) of uncertain derivation"))
    }

    @Test
    fun aReferenceFollowedByAFullStopIsLinkedWithoutIt() {
        val runs = definitionRuns("compare H1262.", "H1254")
        assertEquals(listOf("H1262"), runs.mapNotNull { it.link })
        assertEquals("compare H1262.", runs.joinToString("") { it.text })
    }

    @Test
    fun aReferenceRunTogetherWithAWordIsStillLinked() {
        assertEquals(listOf("H1262"), links("seeH1262"))
    }

    @Test
    fun linkingIsCaseSensitiveOnTheLanguageLetter() {
        assertFalse(links("g26 and G26").contains("g26"))
    }
}
