package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideFont
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.SlideMargin
import com.church.presenter.churchpresentermobile.model.SlideTextAlign
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.model.SlideVerticalAlign
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The audience screen — what the room actually sees.
 *
 * This is the last composable in the chain and the only one nobody is watching
 * while it draws: the operator is looking at the phone. So the tests are about
 * the words — which lines appear, which are suppressed, and whether a reference
 * a church asked to hide stays hidden for the kind of slide they asked about.
 *
 * Nothing here asserts on colour, size or position. The same composable draws
 * the phone's preview strip and a 65-inch screen, and every dimension it uses
 * is derived from the width it is handed.
 */
@OptIn(ExperimentalTestApi::class)
class OutputSlideTest {

    private fun verse(
        kind: SlideKind = SlideKind.SONG,
        body: String = "Amazing grace, how sweet the sound",
        reference: String? = null,
        footer: String? = null,
        chordBody: String? = null,
        theme: SlideTheme = SlideTheme(),
        backdrop: SlideBackdrop = SlideBackdrop.GRADIENT,
        backdropUrl: String? = null,
        mediaUrl: String? = null,
        isBlank: Boolean = false,
        isLive: Boolean = true,
        textSize: SlideTextSize = SlideTextSize.MEDIUM,
    ) = Slide(
        kind = kind,
        body = body,
        chordBody = chordBody,
        reference = reference,
        footer = footer,
        textSize = textSize,
        backdrop = backdrop,
        backdropUrl = backdropUrl,
        mediaUrl = mediaUrl,
        isBlank = isBlank,
        isLive = isLive,
        theme = theme,
    )

    // ── The words ────────────────────────────────────────────────────────

    @Test
    fun theSlidesWordsAreShown() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse()) }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun anEmptySlideShowsNoWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(body = "")) }

        assertFalse(isShowing("Amazing"))
    }

    @Test
    fun aSlideOfOnlyWhitespaceShowsNoWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(body = "   \n  ")) }

        assertFalse(isShowing("Amazing"))
    }

    @Test
    fun theBlankSlideDrawsNothing() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(Slide.BLANK) }

        assertFalse(isShowing("Amazing"))
    }

    @Test
    fun lineBreaksAreKeptByDefault() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(body = "First line\nSecond line")) }

        assertTrue(isShowing("First line\nSecond line"))
    }

    @Test
    fun lineBreaksBecomeSpacesWhenTheChurchAsksForIt() = runComposeUiTest {
        // A hymnbook's lines are set for a narrow page and run off a projector.
        showScreen {
            StandaloneOutputScreen(
                verse(body = "First line\nSecond line", theme = SlideTheme(ignoreLineBreaks = true))
            )
        }

        assertTrue(isShowing("First line Second line"))
    }

    @Test
    fun theWordsSurviveAutoFit() = runComposeUiTest {
        // Auto-fit swaps in a text composable that shrinks to fit; the words
        // must not change with it.
        showScreen { StandaloneOutputScreen(verse(theme = SlideTheme(autoFitText = true))) }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun theLargestTextStillShowsTheWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(textSize = SlideTextSize.LARGE)) }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun theSmallestTextStillShowsTheWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(textSize = SlideTextSize.SMALL)) }

        assertTrue(isShowing("Amazing grace"))
    }

    // ── The reference line, asked per kind ───────────────────────────────

    @Test
    fun aSongsReferenceIsShownWhenTheChurchWantsIt() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(
                    kind = SlideKind.SONG,
                    reference = "Amazing Grace · Verse 2",
                    theme = SlideTheme(showSongReference = true),
                )
            )
        }

        assertTrue(isShowing("AMAZING GRACE · VERSE 2"))
    }

    @Test
    fun aSongsReferenceIsHiddenWhenTheChurchDoesNot() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(
                    kind = SlideKind.SONG,
                    reference = "Amazing Grace · Verse 2",
                    theme = SlideTheme(showSongReference = false),
                )
            )
        }

        assertFalse(isShowing("AMAZING GRACE"))
    }

    @Test
    fun hidingSongReferencesDoesNotHideBibleOnes() = runComposeUiTest {
        // Most churches want no heading over a hymn and still want chapter and
        // verse over scripture — which is why they are two settings.
        showScreen {
            StandaloneOutputScreen(
                verse(
                    kind = SlideKind.BIBLE,
                    reference = "John 3:16",
                    theme = SlideTheme(showSongReference = false, showBibleReference = true),
                )
            )
        }

        assertTrue(isShowing("JOHN 3:16"))
    }

    @Test
    fun aBibleReferenceIsHiddenWhenTheChurchDoesNotWantIt() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(
                    kind = SlideKind.BIBLE,
                    reference = "John 3:16",
                    theme = SlideTheme(showBibleReference = false),
                )
            )
        }

        assertFalse(isShowing("JOHN 3:16"))
    }

    @Test
    fun hidingBibleReferencesDoesNotHideSongOnes() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(
                    kind = SlideKind.SONG,
                    reference = "Amazing Grace",
                    theme = SlideTheme(showBibleReference = false, showSongReference = true),
                )
            )
        }

        assertTrue(isShowing("AMAZING GRACE"))
    }

    @Test
    fun aNoticesReferenceFollowsTheOtherSetting() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(
                    kind = SlideKind.ANNOUNCEMENT,
                    reference = "Notices",
                    theme = SlideTheme(showOtherReference = true),
                )
            )
        }

        assertTrue(isShowing("NOTICES"))
    }

    @Test
    fun aNoticesReferenceIsHiddenWhenTheOtherSettingIsOff() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(
                    kind = SlideKind.ANNOUNCEMENT,
                    reference = "Notices",
                    theme = SlideTheme(showOtherReference = false),
                )
            )
        }

        assertFalse(isShowing("NOTICES"))
    }

    @Test
    fun anImageSlideFollowsTheOtherSetting() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(
                    kind = SlideKind.IMAGE,
                    reference = "Sunset",
                    theme = SlideTheme(showOtherReference = false),
                )
            )
        }

        assertFalse(isShowing("SUNSET"))
    }

    @Test
    fun aReferenceIsShownInCapitals() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(reference = "john 3:16")) }

        assertTrue(isShowing("JOHN 3:16"))
    }

    @Test
    fun aBlankReferenceIsNotAnEmptyLine() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(reference = "   ")) }

        assertTrue(isShowing("Amazing grace"))
    }

    // ── The footer ───────────────────────────────────────────────────────

    @Test
    fun theFooterIsShown() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(footer = "CCLI 1234")) }

        assertTrue(isShowing("CCLI 1234"))
    }

    @Test
    fun theFooterIsQuotedExactly() = runComposeUiTest {
        // It is usually a licence number, and licence numbers are quoted as written.
        showScreen { StandaloneOutputScreen(verse(footer = "CCLI Licence 1234")) }

        assertTrue(isShowing("CCLI Licence 1234"))
    }

    @Test
    fun aSlideWithNoFooterShowsNone() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(footer = null)) }

        assertFalse(isShowing("CCLI"))
    }

    @Test
    fun aBlankFooterIsNotAnEmptyLine() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(footer = "  ")) }

        assertFalse(isShowing("CCLI"))
    }

    @Test
    fun theFooterAndTheReferenceCanBothBeShown() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(reference = "John 3:16", footer = "CCLI 1234")) }

        assertTrue(isShowing("JOHN 3:16"))
        assertTrue(isShowing("CCLI 1234"))
    }

    // ── The church's name in the corner ──────────────────────────────────

    @Test
    fun theBrandLineIsShown() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(theme = SlideTheme(brandLine = "St Mary's"))) }

        assertTrue(isShowing("ST MARY'S"))
    }

    @Test
    fun theBrandLineIsShownInCapitals() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(theme = SlideTheme(brandLine = "st mary's"))) }

        assertTrue(isShowing("ST MARY'S"))
    }

    @Test
    fun noBrandLineIsShownWhenTheChurchHasNotSetOne() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(theme = SlideTheme(brandLine = null))) }

        assertFalse(isShowing("ST MARY"))
    }

    @Test
    fun aBlankBrandLineIsNotAnEmptyCorner() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(theme = SlideTheme(brandLine = "   "))) }

        assertFalse(isShowing("ST MARY"))
    }

    @Test
    fun theBrandLineStaysWhileTheScreenIsBlanked() = runComposeUiTest {
        // A black screen carrying the church's name still reads as deliberate.
        showScreen {
            StandaloneOutputScreen(verse(isBlank = true, theme = SlideTheme(brandLine = "St Mary's")))
        }

        assertTrue(isShowing("ST MARY'S"))
    }

    // ── Chords ───────────────────────────────────────────────────────────

    @Test
    fun chordsAreDrawnWhenTheChurchAsksForThem() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(
                    body = "Amazing grace",
                    chordBody = "[G]Amazing [C]grace",
                    theme = SlideTheme(showChords = true),
                )
            )
        }

        assertTrue(isShowing("G"))
    }

    @Test
    fun theWordsAreStillThereUnderTheChords() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(
                    body = "Amazing grace",
                    chordBody = "[G]Amazing [C]grace",
                    theme = SlideTheme(showChords = true),
                )
            )
        }

        assertTrue(isShowing("Amazing"))
    }

    @Test
    fun chordMarkupNeverReachesTheScreen() = runComposeUiTest {
        // The brackets are markup; the congregation must never see them.
        showScreen {
            StandaloneOutputScreen(
                verse(
                    body = "Amazing grace",
                    chordBody = "[G]Amazing [C]grace",
                    theme = SlideTheme(showChords = true),
                )
            )
        }

        assertFalse(isShowing("[G]"))
    }

    @Test
    fun theCleanWordsAreShownWhenChordsAreOff() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(
                    body = "Amazing grace",
                    chordBody = "[G]Amazing [C]grace",
                    theme = SlideTheme(showChords = false),
                )
            )
        }

        assertTrue(isShowing("Amazing grace"))
        assertFalse(isShowing("[G]"))
    }

    @Test
    fun aSongWithNoChordsIsUnaffectedByTheSetting() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(body = "Amazing grace", chordBody = null, theme = SlideTheme(showChords = true))
            )
        }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun aChordLineWithoutChordsStillShowsItsWords() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(
                    body = "How sweet the sound",
                    chordBody = "How sweet the sound",
                    theme = SlideTheme(showChords = true),
                )
            )
        }

        assertTrue(isShowing("How sweet the sound"))
    }

    @Test
    fun chordsAreDrawnOnEveryLine() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(
                    body = "Amazing grace\nHow sweet",
                    chordBody = "[G]Amazing grace\n[D]How sweet",
                    theme = SlideTheme(showChords = true),
                )
            )
        }

        assertTrue(isShowing("D"))
    }

    // ── The layer behind the words ───────────────────────────────────────

    @Test
    fun aGradientBackdropStillShowsTheWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(backdrop = SlideBackdrop.GRADIENT)) }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun aBlackBackdropStillShowsTheWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(backdrop = SlideBackdrop.BLACK)) }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun anImageBackdropWithNoPhotoStillShowsTheWords() = runComposeUiTest {
        // The renderer paints black underneath, so a failed load is a clean
        // screen rather than whatever was there before.
        showScreen {
            StandaloneOutputScreen(verse(backdrop = SlideBackdrop.IMAGE, backdropUrl = null))
        }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun anImageBackdropWithABlankUrlStillShowsTheWords() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(verse(backdrop = SlideBackdrop.IMAGE, backdropUrl = "  "))
        }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun aCustomGradientStillShowsTheWords() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(theme = SlideTheme(gradientTop = "#123456", gradientBottom = "#654321"))
            )
        }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun aMalformedColourFallsBackRatherThanFailing() = runComposeUiTest {
        // Themes are persisted as JSON and hand-edited; a bad colour must not
        // take the audience screen down mid-service.
        showScreen {
            StandaloneOutputScreen(
                verse(theme = SlideTheme(textColor = "not-a-colour", gradientTop = "??"))
            )
        }

        assertTrue(isShowing("Amazing grace"))
    }

    // ── Layout choices the theme carries ─────────────────────────────────

    @Test
    fun leftAlignedTextIsStillShown() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(verse(theme = SlideTheme(textAlign = SlideTextAlign.LEFT)))
        }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun rightAlignedTextIsStillShown() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(verse(theme = SlideTheme(textAlign = SlideTextAlign.RIGHT)))
        }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun topAlignedTextIsStillShown() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(theme = SlideTheme(verticalAlign = SlideVerticalAlign.TOP))
            )
        }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun bottomAlignedTextIsStillShown() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                verse(theme = SlideTheme(verticalAlign = SlideVerticalAlign.BOTTOM))
            )
        }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun thinMarginsStillShowTheWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(theme = SlideTheme(margin = SlideMargin.THIN))) }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun thickMarginsStillShowTheWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(theme = SlideTheme(margin = SlideMargin.THICK))) }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun aSansSerifSlideStillShowsTheWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(theme = SlideTheme(font = SlideFont.SANS))) }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun aSerifSlideStillShowsTheWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(theme = SlideTheme(font = SlideFont.SERIF))) }

        assertTrue(isShowing("Amazing grace"))
    }

    // ── A page or a video, which replaces the words ──────────────────────

    @Test
    fun aWebSlideWithNoUrlFallsBackToItsWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(kind = SlideKind.WEB, mediaUrl = null)) }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun aWebSlideWithABlankUrlFallsBackToItsWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(kind = SlideKind.WEB, mediaUrl = "   ")) }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun aSongSlideIgnoresAMediaUrlItShouldNotHave() = runComposeUiTest {
        // Only WEB and VIDEO replace the text layer; anything else keeps its words.
        showScreen {
            StandaloneOutputScreen(verse(kind = SlideKind.SONG, mediaUrl = "https://example.org"))
        }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun aHeldBackSlideStillDraws() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(isLive = false)) }

        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun aLogoSlideDraws() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(verse(kind = SlideKind.LOGO, body = "St Mary's")) }

        assertTrue(isShowing("St Mary's"))
    }
}
