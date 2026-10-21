package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.church.presenter.churchpresentermobile.library.Chords

/**
 * The words with their chords, for an output.
 *
 * Each chord sits above the words it was written against, using the same
 * splitter the phone and the desktop use, so the three agree on where a chord
 * belongs. Falls back to nothing special when a line has no chords: that line is
 * a single segment with an empty chord, which draws as ordinary words.
 */
@Composable
internal fun OutputChordChart(
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
                                lineHeight = bodySize * CHART_LINE_HEIGHT,
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

/** The words' line height, as the body's: a chart is the same words with chords over them. */
private const val CHART_LINE_HEIGHT = 1.32f
