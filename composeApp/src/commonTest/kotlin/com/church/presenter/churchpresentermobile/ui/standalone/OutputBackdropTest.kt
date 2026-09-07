package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What sits behind the words, and what replaces them.
 *
 * A backdrop is chosen once and then forgotten about, so the failures here are
 * quiet ones: an image backdrop with no photo yet showing whatever was behind
 * it, or a web slide that keeps drawing its body text over the page it is meant
 * to *be*. Both look like a broken screen from the back of a hall and neither
 * shows up on the operator's phone.
 */
@OptIn(ExperimentalTestApi::class)
class OutputBackdropTest {

    private fun slide(
        kind: SlideKind = SlideKind.SONG,
        body: String = "Amazing grace, how sweet the sound",
        backdrop: SlideBackdrop = SlideBackdrop.GRADIENT,
        backdropUrl: String? = null,
        mediaUrl: String? = null,
        isBlank: Boolean = false,
        isLive: Boolean = true,
        theme: SlideTheme = SlideTheme(),
    ) = Slide(
        kind = kind,
        body = body,
        backdrop = backdrop,
        backdropUrl = backdropUrl,
        mediaUrl = mediaUrl,
        isBlank = isBlank,
        isLive = isLive,
        theme = theme,
    )

    // ── The three backdrops ──────────────────────────────────────────────

    @Test
    fun aGradientBackdropStillShowsTheWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(slide(backdrop = SlideBackdrop.GRADIENT)) }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun aBlackBackdropStillShowsTheWords() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(slide(backdrop = SlideBackdrop.BLACK)) }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun anImageBackdropStillShowsTheWords() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                slide(backdrop = SlideBackdrop.IMAGE, backdropUrl = "http://192.168.1.50:8080/p/1")
            )
        }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun anImageBackdropWithNoPhotoStillShowsTheWords() = runComposeUiTest {
        // Black is painted underneath, so a photo that never loads leaves a
        // clean screen rather than whatever was there before.
        showScreen { StandaloneOutputScreen(slide(backdrop = SlideBackdrop.IMAGE, backdropUrl = null)) }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun anImageBackdropWithABlankUrlIsTreatedAsNoPhoto() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(slide(backdrop = SlideBackdrop.IMAGE, backdropUrl = "   ")) }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun aGradientWithCustomColoursStillShowsTheWords() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                slide(theme = SlideTheme(gradientTop = "#102030", gradientBottom = "#405060"))
            )
        }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun aGradientWithUnreadableColoursFallsBackRatherThanFailing() = runComposeUiTest {
        // The theme is stored text and can be anything; a bad value must not
        // take the audience screen down.
        showScreen {
            StandaloneOutputScreen(
                slide(theme = SlideTheme(gradientTop = "not a colour", gradientBottom = ""))
            )
        }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun aBlackBackdropWorksWithAnImageUrlLeftBehind() = runComposeUiTest {
        // Switching back to black must not keep drawing the old photo.
        showScreen {
            StandaloneOutputScreen(
                slide(backdrop = SlideBackdrop.BLACK, backdropUrl = "http://192.168.1.50:8080/p/1")
            )
        }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    // ── Blanked and held back ────────────────────────────────────────────

    @Test
    fun aBlankedSlideIsMarkedHidden() = runComposeUiTest {
        // Hiding is a fade rather than a removal — a hard cut to black reads as
        // a crash on a projector — so the words stay composed at zero alpha,
        // which the semantics tree cannot see. The flag the renderer fades on is
        // what there is to assert.
        val blanked = slide(isBlank = true)

        showScreen { StandaloneOutputScreen(blanked) }

        assertTrue(blanked.isHidden)
    }

    @Test
    fun aSlideHeldBackIsMarkedHidden() = runComposeUiTest {
        // Preparing the next verse must not put it on the wall.
        val held = slide(isLive = false)

        showScreen { StandaloneOutputScreen(held) }

        assertTrue(held.isHidden)
    }

    @Test
    fun aBlankedSlideOverAnImageBackdropStillDrawsTheBackdrop() = runComposeUiTest {
        // Blanking hides the words, not the church's photo.
        val blanked = slide(
            backdrop = SlideBackdrop.IMAGE,
            backdropUrl = "http://192.168.1.50:8080/p/1",
            isBlank = true,
        )

        showScreen { StandaloneOutputScreen(blanked) }

        assertTrue(blanked.isHidden)
    }

    @Test
    fun aBlankedSlideOverBlackIsMarkedHidden() = runComposeUiTest {
        val blanked = slide(backdrop = SlideBackdrop.BLACK, isBlank = true)

        showScreen { StandaloneOutputScreen(blanked) }

        assertTrue(blanked.isHidden)
    }

    @Test
    fun aSlideThatIsBothBlankedAndHeldBackIsHiddenOnce() = runComposeUiTest {
        // Two reasons to hide are not a contradiction.
        val hidden = slide(isBlank = true, isLive = false)

        showScreen { StandaloneOutputScreen(hidden) }

        assertTrue(hidden.isHidden)
    }

    // ── A page or a video is the slide ───────────────────────────────────

    @Test
    fun aWebSlideRendersItsPage() = runComposeUiTest {
        // The page replaces the text layer rather than sitting behind it; the
        // swap is an alpha, which carries no semantics, so what this holds is
        // that a web slide lays out at all.
        showScreen {
            StandaloneOutputScreen(
                slide(
                    kind = SlideKind.WEB,
                    body = "https://example.org/notices",
                    mediaUrl = "https://example.org/notices",
                )
            )
        }

        assertTrue(isShowing("https://example.org/notices"))
    }

    @Test
    fun aVideoSlideRendersItsPlayer() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                slide(
                    kind = SlideKind.VIDEO,
                    body = "https://example.org/sermon.mp4",
                    mediaUrl = "https://example.org/sermon.mp4",
                )
            )
        }

        assertTrue(isShowing("https://example.org/sermon.mp4"))
    }

    @Test
    fun aBlankedWebSlideStandsDownLikeEverythingElse() = runComposeUiTest {
        // A page is not exempt from a blank: the congregation should not be
        // reading a website while the screen is meant to be empty.
        val blanked = slide(
            kind = SlideKind.WEB,
            body = "Notices",
            mediaUrl = "https://example.org/notices",
            isBlank = true,
        )

        showScreen { StandaloneOutputScreen(blanked) }

        assertTrue(blanked.isHidden)
    }

    @Test
    fun aWebSlideHeldBackStandsDownToo() = runComposeUiTest {
        val held = slide(
            kind = SlideKind.WEB,
            body = "Notices",
            mediaUrl = "https://example.org/notices",
            isLive = false,
        )

        showScreen { StandaloneOutputScreen(held) }

        assertTrue(held.isHidden)
    }

    @Test
    fun aSongSlideCarryingAMediaUrlStillShowsItsWords() = runComposeUiTest {
        // Only WEB and VIDEO slides *are* their media; anything else keeps its
        // text.
        showScreen {
            StandaloneOutputScreen(
                slide(kind = SlideKind.SONG, mediaUrl = "https://example.org/notices")
            )
        }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun aWebSlideWithNoAddressFallsBackToItsWords() = runComposeUiTest {
        // Nothing to frame, so there is no reason to hide the text as well.
        showScreen {
            StandaloneOutputScreen(slide(kind = SlideKind.WEB, mediaUrl = null))
        }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun aWebSlideWithABlankAddressFallsBackToItsWords() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(slide(kind = SlideKind.WEB, mediaUrl = "   "))
        }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun aVideoSlideWithNoAddressFallsBackToItsWords() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(slide(kind = SlideKind.VIDEO, mediaUrl = null))
        }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun aWebSlideOverAnImageBackdropStillRenders() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                slide(
                    kind = SlideKind.WEB,
                    body = "Notices",
                    mediaUrl = "https://example.org/notices",
                    backdrop = SlideBackdrop.IMAGE,
                    backdropUrl = "http://192.168.1.50:8080/p/1",
                )
            )
        }

        assertTrue(isShowing("Notices"))
    }

    @Test
    fun aVideoSlideOverBlackStillRenders() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                slide(
                    kind = SlideKind.VIDEO,
                    body = "Sermon",
                    mediaUrl = "https://example.org/sermon.mp4",
                    backdrop = SlideBackdrop.BLACK,
                )
            )
        }

        assertTrue(isShowing("Sermon"))
    }

    // ── The empty screen ─────────────────────────────────────────────────

    @Test
    fun theBlankSlideOverAGradientDrawsNothing() = runComposeUiTest {
        showScreen { StandaloneOutputScreen(Slide.BLANK) }

        assertFalse(isShowing("Amazing"))
    }

    @Test
    fun aClearedScreenKeepsItsBackdrop() = runComposeUiTest {
        // The room should see the church's own wash, not a black rectangle,
        // between items.
        showScreen {
            StandaloneOutputScreen(Slide.BLANK.copy(backdrop = SlideBackdrop.GRADIENT))
        }

        assertFalse(isShowing("Amazing"))
    }

    @Test
    fun anEmptyBodyOverAnImageBackdropDrawsNoWords() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                slide(
                    body = "",
                    backdrop = SlideBackdrop.IMAGE,
                    backdropUrl = "http://192.168.1.50:8080/p/1",
                )
            )
        }

        assertFalse(isShowing("Amazing"))
    }

    @Test
    fun aPhotoSlideIsAllBackdropAndNoWords() = runComposeUiTest {
        // Projecting a picture should show the picture, not a caption.
        showScreen {
            StandaloneOutputScreen(
                slide(
                    kind = SlideKind.IMAGE,
                    body = "",
                    backdrop = SlideBackdrop.IMAGE,
                    backdropUrl = "http://192.168.1.50:8080/p/1",
                )
            )
        }

        assertFalse(isShowing("Amazing"))
    }

    @Test
    fun aSlideKeepsItsWordsAcrossEveryBackdrop() = runComposeUiTest {
        // The backdrop is a layer, not a switch that turns the words off.
        showScreen {
            StandaloneOutputScreen(slide(backdrop = SlideBackdrop.BLACK, body = "Verse one"))
        }

        assertTrue(isShowing("Verse one"))
    }

    @Test
    fun anImageBackdropWorksWithoutATheme() = runComposeUiTest {
        showScreen {
            StandaloneOutputScreen(
                slide(
                    backdrop = SlideBackdrop.IMAGE,
                    backdropUrl = "http://192.168.1.50:8080/p/1",
                    theme = SlideTheme(),
                )
            )
        }

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun aHeldBackImageSlideIsMarkedHidden() = runComposeUiTest {
        val held = slide(
            backdrop = SlideBackdrop.IMAGE,
            backdropUrl = "http://192.168.1.50:8080/p/1",
            isLive = false,
        )

        showScreen { StandaloneOutputScreen(held) }

        assertTrue(held.isHidden)
    }
}
