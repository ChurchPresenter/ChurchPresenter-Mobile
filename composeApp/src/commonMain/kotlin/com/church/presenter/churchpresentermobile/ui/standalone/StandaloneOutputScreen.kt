package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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

        val textColor = parseHexColor(slide.theme.textColor, Color.White)
        val accentColor = parseHexColor(slide.theme.accentColor, Color.White)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentAlpha)
                .padding(horizontal = boxWidth * HORIZONTAL_PAD, vertical = boxHeight * VERTICAL_PAD),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (slide.body.isNotBlank()) {
                Text(
                    text = slide.body,
                    style = TextStyle(
                        fontSize = bodySize,
                        lineHeight = bodySize * BODY_LINE_HEIGHT,
                        fontFamily = slide.theme.font.toFontFamily(),
                        color = textColor,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            slide.reference?.takeIf { it.isNotBlank() }?.let { reference ->
                Text(
                    text = reference.uppercase(),
                    style = TextStyle(
                        fontSize = referenceSize,
                        letterSpacing = referenceSize * REFERENCE_TRACKING,
                        fontFamily = FontFamily.SansSerif,
                        color = accentColor.copy(alpha = REFERENCE_ALPHA),
                        textAlign = TextAlign.Center,
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
                Brush.linearGradient(listOf(GRADIENT_TOP, GRADIENT_MID, GRADIENT_BOTTOM))
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
private const val BODY_LINE_HEIGHT = 1.32f
private const val REFERENCE_RATIO = 0.46f
private const val REFERENCE_TRACKING = 0.14f
private const val REFERENCE_ALPHA = 0.72f
private const val CORNER_RATIO = 0.30f
private const val CORNER_ALPHA = 0.45f
private const val BRAND_TRACKING = 0.22f
private const val FOOTER_ALPHA = 0.55f
private const val HORIZONTAL_PAD = 0.08f
private const val VERTICAL_PAD = 0.06f
private const val REFERENCE_GAP = 0.045f
private const val FOOTER_GAP = 0.02f
private const val CORNER_PAD = 0.035f

private val GRADIENT_TOP = Color(0xFF2A1D5E)
private val GRADIENT_MID = Color(0xFF0B0D1A)
private val GRADIENT_BOTTOM = Color(0xFF05060D)
private val IMAGE_SCRIM = Color(0x99000000)
