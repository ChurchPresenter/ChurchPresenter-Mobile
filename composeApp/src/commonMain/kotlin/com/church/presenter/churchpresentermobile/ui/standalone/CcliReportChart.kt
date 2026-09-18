package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.report_legend_songs
import churchpresentermobile.composeapp.generated.resources.report_legend_verses
import churchpresentermobile.composeapp.generated.resources.report_over_time
import com.church.presenter.churchpresentermobile.model.ActivityPoint
import com.church.presenter.churchpresentermobile.ui.OverlineRow
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/** The chart never shows more tick labels than this, whatever the bucket count. */
private const val MAX_TICKS = 6

/** Width of the gap between bucket groups, as a share of one group's slot. */
private const val GROUP_GAP = 0.3f

/** How much of the group's bar space each of the two bars takes. */
private const val BAR_SHARE = 0.5f

/**
 * Songs and verses per bucket, as paired bars.
 *
 * Drawn on a Canvas rather than laid out from boxes: fifty-two weekly buckets
 * on a phone are a few pixels each, and a hundred composed boxes for that is
 * both slower and no clearer.
 */
@Composable
internal fun ActivityChart(points: List<ActivityPoint>, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val songColor = colors.accent
    val verseColor = colors.verseTint
    val baseline = colors.borderSubtle
    val most = points.maxOfOrNull { maxOf(it.songCount, it.verseCount) }?.coerceAtLeast(1) ?: 1
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OverlineRow(
            label = stringResource(Res.string.report_over_time),
            trailing = points.firstOrNull()?.let { first -> "${first.label} – ${points.last().label}" },
        )
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val slot = size.width / points.size.coerceAtLeast(1)
            val groupWidth = slot * (1 - GROUP_GAP)
            val barWidth = groupWidth * BAR_SHARE
            drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            points.forEachIndexed { index, point ->
                val left = index * slot + (slot - groupWidth) / 2
                drawBar(left, point.songCount, most, barWidth, songColor)
                drawBar(left + barWidth, point.verseCount, most, barWidth, verseColor)
            }
        }
        TickLabels(points.map { it.label })
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(songColor, stringResource(Res.string.report_legend_songs))
            LegendDot(verseColor, stringResource(Res.string.report_legend_verses))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBar(
    left: Float,
    count: Int,
    most: Int,
    width: Float,
    color: Color,
) {
    if (count == 0) return
    val height = size.height * count / most
    drawRoundRect(
        color = color,
        topLeft = Offset(left, size.height - height),
        size = Size(width, height),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
}

/** Up to [MAX_TICKS] labels, spread across the width; the rest are left unlabelled. */
@Composable
private fun TickLabels(labels: List<String>) {
    val colors = LocalAppColors.current
    if (labels.isEmpty()) return
    val every = (labels.size + MAX_TICKS - 1) / MAX_TICKS
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Text(
                text = if (index % every == 0) label else "",
                color = colors.muted,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, color = colors.muted, fontSize = 11.sp)
    }
}
