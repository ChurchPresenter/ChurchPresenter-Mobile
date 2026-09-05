package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.library.Chords
import com.church.presenter.churchpresentermobile.model.SlideTextAlign
import com.church.presenter.churchpresentermobile.model.showsReference
import com.church.presenter.churchpresentermobile.model.displayBody
import com.church.presenter.churchpresentermobile.model.SlideVerticalAlign
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.SlideFont
import com.church.presenter.churchpresentermobile.model.SlideTextSize

/**
 * Renders one [Slide] full-bleed — the audience view.
 *
 * Deliberately stateless and data-only. The same composable is used by the
 * in-app preview, the Android `Presentation` window and the iOS external
 * display scene; the latter two live outside `App()`'s Compose tree and cannot
 * be handed a ViewModel, so everything they need arrives in [slide].
 *
 * Type scales with the available width rather than using fixed sp, so a 16:9
 * preview strip on a phone and a 65" TV both look like the same design — the
 * web equivalent of the mockup's `clamp(15px, 3.5vw, 42px)`.
 */
@Composable
fun StandaloneOutputScreen(
    slide: Slide,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val boxWidth = maxWidth
        val boxHeight = maxHeight
        val widthPx = boxWidth.value
        val bodySize = scaledSp(widthPx, slide.textSize)
        val referenceSize = (bodySize.value * REFERENCE_RATIO).coerceIn(8f, 22f).sp
        val cornerSize = (bodySize.value * CORNER_RATIO).coerceIn(7f, 15f).sp

        // Fade rather than cut, matching the mockup's 450ms opacity transition —
        // a hard cut to black reads as a crash on a projector.
        val contentAlpha by animateFloatAsState(
            targetValue = if (slide.isHidden) 0f else 1f,
            animationSpec = tween(durationMillis = HIDE_FADE_MS),
            label = "slideAlpha",
        )

        Backdrop(slide)

        // A page or a video *is* the slide, so it replaces the text layer rather
        // than sitting behind it — and stands down while blanked, like everything
        // else the audience should not be seeing.
        val mediaUrl = slide.mediaUrl?.takeIf { it.isNotBlank() && !slide.isHidden }
        if (mediaUrl != null) {
            when (slide.kind) {
                SlideKind.WEB -> OutputWebView(mediaUrl, Modifier.fillMaxSize())
                SlideKind.VIDEO -> OutputVideoView(mediaUrl, Modifier.fillMaxSize())
                else -> Unit
            }
        }
        val showsText = mediaUrl == null || slide.kind !in setOf(SlideKind.WEB, SlideKind.VIDEO)

        val textColor = parseHexColor(slide.theme.textColor, Color.White)
        val accentColor = parseHexColor(slide.theme.accentColor, Color.White)

        // Both axes come from the theme so the phone, an attached screen and a
        // watching browser lay the words out the same way.
        val bodyAlign = slide.theme.textAlign.toTextAlign()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (showsText) contentAlpha else 0f)
                .padding(
                    horizontal = boxWidth * slide.theme.margin.horizontal,
                    vertical = boxHeight * slide.theme.margin.vertical,
                ),
            horizontalAlignment = slide.theme.textAlign.toHorizontalAlignment(),
            verticalArrangement = slide.theme.verticalAlign.toVerticalArrangement(),
        ) {
            val chordText = slide.chordBody?.takeIf { slide.theme.showChords }
            if (chordText != null) {
                OutputChordChart(
                    text = chordText,
                    bodySize = bodySize,
                    textColor = textColor,
                    chordColor = accentColor,
                    fontFamily = slide.theme.font.toFontFamily(),
                    horizontalAlignment = slide.theme.textAlign.toHorizontalAlignment(),
                )
            } else if (slide.body.isNotBlank()) {
                val bodyText = slide.displayBody()
                val bodyStyle = TextStyle(
                    fontSize = bodySize,
                    lineHeight = bodySize * BODY_LINE_HEIGHT,
                    fontFamily = slide.theme.font.toFontFamily(),
                    color = textColor,
                    textAlign = bodyAlign,
                )
                if (slide.theme.autoFitText) {
                    // weight(fill = false) hands the text the height that is left
                    // as a limit without making it fill that height, so the words
                    // shrink to fit while the chosen vertical alignment still
                    // decides where the block sits.
                    BasicText(
                        text = bodyText,
                        style = bodyStyle,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = bodySize * AUTO_FIT_FLOOR,
                            maxFontSize = bodySize,
                        ),
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    )
                } else {
                    Text(
                        text = bodyText,
                        style = bodyStyle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            slide.reference?.takeIf { it.isNotBlank() && slide.showsReference() }?.let { reference ->
                Text(
                    text = reference.uppercase(),
                    style = TextStyle(
                        fontSize = referenceSize,
                        letterSpacing = referenceSize * REFERENCE_TRACKING,
                        fontFamily = FontFamily.SansSerif,
                        color = accentColor.copy(alpha = REFERENCE_ALPHA),
                        textAlign = bodyAlign,
                    ),
                    modifier = Modifier.padding(top = boxHeight * REFERENCE_GAP),
                )
            }
            slide.footer?.takeIf { it.isNotBlank() }?.let { footer ->
                Text(
                    text = footer,
                    style = TextStyle(
                        fontSize = cornerSize,
                        fontFamily = FontFamily.SansSerif,
                        color = textColor.copy(alpha = FOOTER_ALPHA),
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.padding(top = boxHeight * FOOTER_GAP),
                )
            }
        }

        // Corner furniture stays visible while the slide is blanked: a black
        // screen with the church name still reads as "we meant to do that".
        slide.theme.brandLine?.takeIf { it.isNotBlank() }?.let { brand ->
            Text(
                text = brand.uppercase(),
                style = TextStyle(
                    fontSize = cornerSize,
                    letterSpacing = cornerSize * BRAND_TRACKING,
                    fontFamily = FontFamily.SansSerif,
                    color = textColor.copy(alpha = CORNER_ALPHA),
                ),
                modifier = Modifier.align(Alignment.TopStart).padding(boxWidth * CORNER_PAD),
            )
        }
    }
}

/** The layer behind the text: a gradient wash, a photo, or plain black. */
@Composable
private fun Backdrop(slide: Slide) {
    when (slide.backdrop) {
        SlideBackdrop.BLACK -> Box(Modifier.fillMaxSize().background(Color.Black))

        SlideBackdrop.GRADIENT -> Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(
                        parseHexColor(slide.theme.gradientTop, GRADIENT_TOP),
                        parseHexColor(slide.theme.gradientBottom, GRADIENT_BOTTOM),
                    )
                )
            )
        )

        SlideBackdrop.IMAGE -> {
            // Paint black underneath so a slow or failed image load shows a
            // clean screen rather than whatever was behind it.
            Box(Modifier.fillMaxSize().background(Color.Black))
            slide.backdropUrl?.takeIf { it.isNotBlank() }?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
                // Scrim: unmodified photos almost never carry legible white text.
                Box(Modifier.fillMaxSize().background(IMAGE_SCRIM))
            }
        }
    }
}

/**
 * The words with their chords, for an output.
 *
 * Each chord sits above the words it was written against, using the same
 * splitter the phone and the desktop use, so the three agree on where a chord
 * belongs. Falls back to nothing special when a line has no chords: that line is
 * a single segment with an empty chord, which draws as ordinary words.
 */
@Composable
private fun OutputChordChart(
    text: String,
    bodySize: TextUnit,
    textColor: Color,
    chordColor: Color,
    fontFamily: FontFamily,
    horizontalAlignment: Alignment.Horizontal,
) {
    val chordSize = (bodySize.value * CHORD_RATIO).sp
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
    ) {
        text.lines().forEach { line ->
            Row(verticalAlignment = Alignment.Bottom) {
                Chords.parseLine(line).forEach { segment ->
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = segment.chord,
                            style = TextStyle(
                                fontSize = chordSize,
                                fontFamily = FontFamily.Monospace,
                                color = chordColor,
                            ),
                        )
                        Text(
                            text = segment.text,
                            style = TextStyle(
                                fontSize = bodySize,
                                lineHeight = bodySize * BODY_LINE_HEIGHT,
                                fontFamily = fontFamily,
                                color = textColor,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** Chords are drawn smaller than the words they sit over. */
private const val CHORD_RATIO = 0.55f

/** The desktop resolves the same three names to the same three alignments. */
private fun SlideTextAlign.toTextAlign(): TextAlign = when (this) {
    SlideTextAlign.LEFT -> TextAlign.Start
    SlideTextAlign.RIGHT -> TextAlign.End
    SlideTextAlign.CENTER -> TextAlign.Center
}

private fun SlideTextAlign.toHorizontalAlignment(): Alignment.Horizontal = when (this) {
    SlideTextAlign.LEFT -> Alignment.Start
    SlideTextAlign.RIGHT -> Alignment.End
    SlideTextAlign.CENTER -> Alignment.CenterHorizontally
}

private fun SlideVerticalAlign.toVerticalArrangement(): Arrangement.Vertical = when (this) {
    SlideVerticalAlign.TOP -> Arrangement.Top
    SlideVerticalAlign.BOTTOM -> Arrangement.Bottom
    SlideVerticalAlign.MIDDLE -> Arrangement.Center
}

private fun SlideFont.toFontFamily(): FontFamily = when (this) {
    SlideFont.SERIF -> FontFamily.Serif
    SlideFont.SANS -> FontFamily.SansSerif
}

/** Body type as a fraction of the rendered width, clamped like the mockup's `clamp()`. */
private fun scaledSp(widthDp: Float, size: SlideTextSize): TextUnit {
    val (ratio, min, max) = when (size) {
        SlideTextSize.SMALL -> Triple(0.052f, 12f, 34f)
        SlideTextSize.MEDIUM -> Triple(0.066f, 15f, 44f)
        SlideTextSize.LARGE -> Triple(0.084f, 18f, 56f)
    }
    return (widthDp * ratio).coerceIn(min, max).sp
}

/**
 * Parses `#RRGGBB` / `#AARRGGBB`, falling back to [fallback] on anything else.
 *
 * Colours arrive from a theme the user may have typed by hand, so a malformed
 * value must degrade to a readable slide rather than crash the projector.
 */
internal fun parseHexColor(hex: String, fallback: Color): Color {
    val cleaned = hex.trim().removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return fallback
    val value = cleaned.toLongOrNull(16) ?: return fallback
    return if (cleaned.length == 6) Color(value or 0xFF000000L) else Color(value)
}

private const val HIDE_FADE_MS = 450
/**
 * How far auto-fit may shrink the words before it stops.
 *
 * A floor rather than no limit: text small enough to fit anything is text nobody
 * at the back can read, and a verse that will not fit at this size is better
 * clipped than shown illegibly — the operator can split it or choose Small.
 */
private const val AUTO_FIT_FLOOR = 0.45f
private const val BODY_LINE_HEIGHT = 1.32f
private const val REFERENCE_RATIO = 0.46f
private const val REFERENCE_TRACKING = 0.14f
private const val REFERENCE_ALPHA = 0.72f
private const val CORNER_RATIO = 0.30f
private const val CORNER_ALPHA = 0.45f
private const val BRAND_TRACKING = 0.22f
private const val FOOTER_ALPHA = 0.55f
private const val REFERENCE_GAP = 0.045f
private const val FOOTER_GAP = 0.02f
private const val CORNER_PAD = 0.035f

/** Only reached when a theme carries an unreadable colour — the defaults live in SlideTheme. */
private val GRADIENT_TOP = Color(0xFF2A1D5E)
private val GRADIENT_BOTTOM = Color(0xFF05060D)
private val IMAGE_SCRIM = Color(0x99000000)
