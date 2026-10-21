package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.display_close
import churchpresentermobile.composeapp.generated.resources.display_full_screen
import churchpresentermobile.composeapp.generated.resources.display_full_screen_bible
import churchpresentermobile.composeapp.generated.resources.display_full_screen_hint
import churchpresentermobile.composeapp.generated.resources.display_full_screen_spec
import churchpresentermobile.composeapp.generated.resources.display_preview_song
import churchpresentermobile.composeapp.generated.resources.display_sample_book
import churchpresentermobile.composeapp.generated.resources.display_sample_song_lyrics
import churchpresentermobile.composeapp.generated.resources.display_sample_song_second
import churchpresentermobile.composeapp.generated.resources.display_sample_song_title
import churchpresentermobile.composeapp.generated.resources.display_sample_verse
import churchpresentermobile.composeapp.generated.resources.display_sample_version
import com.church.presenter.churchpresentermobile.model.DEFAULT_GRADIENT_BOTTOM
import com.church.presenter.churchpresentermobile.model.DEFAULT_GRADIENT_TOP
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.model.TextElement
import com.church.presenter.churchpresentermobile.model.VerseCredit
import com.church.presenter.churchpresentermobile.model.formattedReference
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/** The shape the preview is drawn in. */
internal enum class PreviewAspect(val ratio: Float, val label: String) {
    WIDE(16f / 9f, "16:9"),
    STANDARD(4f / 3f, "4:3"),
}

/** What the preview's words sit on. */
internal enum class PreviewBackground { OUTPUT, DARK, LIGHT }

/**
 * How the operator has asked to see the preview.
 *
 * @param useLive Preview the slide that is on the output now, when it is of the kind being
 *   styled, rather than the sample.
 */
internal data class PreviewOptions(
    val useLive: Boolean = false,
    val aspect: PreviewAspect = PreviewAspect.WIDE,
    val background: PreviewBackground = PreviewBackground.OUTPUT,
)

/** A Bible verse and a two-language song to style against when nothing suitable is live. */
internal class SampleSlides(val bible: Slide, val song: Slide) {
    fun of(kind: SlideKind): Slide = if (kind == SlideKind.SONG) song else bible
}

@Composable
internal fun rememberSampleSlides(): SampleSlides {
    val book = stringResource(Res.string.display_sample_book)
    val version = stringResource(Res.string.display_sample_version)
    val bible = Slide(
        kind = SlideKind.BIBLE,
        body = stringResource(Res.string.display_sample_verse),
        reference = "$book $SAMPLE_CHAPTER:$SAMPLE_VERSE",
        verseCredit = VerseCredit(bibleName = version, bookName = book, chapter = SAMPLE_CHAPTER, verse = SAMPLE_VERSE),
    )
    val song = Slide(
        kind = SlideKind.SONG,
        body = stringResource(Res.string.display_sample_song_lyrics),
        songNumber = SAMPLE_SONG_NUMBER,
        songTitle = stringResource(Res.string.display_sample_song_title),
        secondaryBody = stringResource(Res.string.display_sample_song_second),
    )
    return SampleSlides(bible, song)
}

/** What each reference format writes for the sample verse, for the format menu. */
internal fun referenceSamples(samples: SampleSlides): List<String> =
    REFERENCE_FORMATS.map { samples.bible.formattedReference(it).orEmpty() }

/**
 * The slide the preview draws: the sample or the live slide, in [theme] — the output's look
 * carrying the draft styling — on the chosen background.
 *
 * Never blank and always live, whatever the output is doing — the preview exists to show the
 * words, and a blacked-out output would otherwise make it a black box.
 */
internal fun previewSlide(
    kind: SlideKind,
    theme: SlideTheme,
    live: Slide,
    samples: SampleSlides,
    options: PreviewOptions,
): Slide {
    val liveFits = options.useLive && live.kind == kind && live.body.isNotBlank()
    val base = if (liveFits) live else samples.of(kind)
    val backdrop = when (options.background) {
        PreviewBackground.OUTPUT -> live.backdrop to live.backdropUrl
        PreviewBackground.DARK, PreviewBackground.LIGHT -> SlideBackdrop.GRADIENT to null
    }
    return base.copy(
        theme = when (options.background) {
            PreviewBackground.OUTPUT -> theme
            PreviewBackground.DARK -> theme.copy(
                gradientTop = DEFAULT_GRADIENT_TOP,
                gradientBottom = DEFAULT_GRADIENT_BOTTOM,
            )
            PreviewBackground.LIGHT -> theme.copy(gradientTop = LIGHT_TOP, gradientBottom = LIGHT_BOTTOM)
        },
        backdrop = backdrop.first,
        backdropUrl = backdrop.second,
        isBlank = false,
        isLive = true,
    )
}

/**
 * The output in miniature, drawn by the very renderer the output uses, with the element being
 * edited outlined and every element tappable.
 */
@Composable
internal fun PreviewCard(
    slide: Slide,
    element: TextElement,
    onElementTap: (TextElement) -> Unit,
    aspect: PreviewAspect,
    modifier: Modifier = Modifier,
    onExpand: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect.ratio)
            .clip(shape)
            .border(1.dp, colors.borderSubtle, shape)
            .testTag(DisplayTextTags.PREVIEW),
    ) {
        StandaloneOutputScreen(slide = slide, highlight = element, onElementTap = onElementTap)
        if (onExpand != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = onExpand)
                    .testTag(DisplayTextTags.EXPAND),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Fullscreen,
                    contentDescription = stringResource(Res.string.display_full_screen),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * The preview filling the screen, as the hall will see it.
 *
 * Always on black, in either theme: what it shows is the output, which has no light mode. Tapping
 * an element closes it on that element's settings.
 */
@Composable
internal fun FullScreenPreview(
    slideFor: (SlideKind) -> Slide,
    kind: SlideKind,
    onKindChange: (SlideKind) -> Unit,
    element: TextElement,
    onElementTap: (TextElement) -> Unit,
    aspect: PreviewAspect,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag(DisplayTextTags.FULL_SCREEN),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                KindChip(stringResource(Res.string.display_full_screen_bible), kind == SlideKind.BIBLE) {
                    onKindChange(SlideKind.BIBLE)
                }
                Spacer(Modifier.width(8.dp))
                KindChip(stringResource(Res.string.display_preview_song), kind == SlideKind.SONG) {
                    onKindChange(SlideKind.SONG)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(Res.string.display_full_screen_spec, aspect.label),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.display_close),
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onClose)
                        .testTag(DisplayTextTags.FULL_SCREEN_CLOSE),
                )
            }
            BoxWithConstraints(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                // As large as fits either way round, so a phone turned on its side fills the width.
                val fitsByWidth = maxWidth / maxHeight < aspect.ratio
                Box(
                    if (fitsByWidth) Modifier.fillMaxWidth().aspectRatio(aspect.ratio)
                    else Modifier.fillMaxSize().aspectRatio(aspect.ratio, matchHeightConstraintsFirst = true),
                ) {
                    StandaloneOutputScreen(slide = slideFor(kind), highlight = element, onElementTap = onElementTap)
                }
            }
            Text(
                stringResource(Res.string.display_full_screen_hint),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun KindChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Text(
        text = label,
        color = if (selected) colors.onAccent else Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Arrangement used by both preview option rows. */
internal val PreviewOptionSpacing = Arrangement.spacedBy(8.dp)

private const val SAMPLE_CHAPTER = 3
private const val SAMPLE_VERSE = 16
private const val SAMPLE_SONG_NUMBER = "104"
private const val LIGHT_TOP = "#F5F5F4"
private const val LIGHT_BOTTOM = "#D6D3D1"
