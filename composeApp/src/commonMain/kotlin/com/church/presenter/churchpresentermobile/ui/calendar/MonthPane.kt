package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_kind_midweek
import churchpresentermobile.composeapp.generated.resources.calendar_kind_special
import churchpresentermobile.composeapp.generated.resources.calendar_kind_sunday
import churchpresentermobile.composeapp.generated.resources.calendar_next_month
import churchpresentermobile.composeapp.generated.resources.calendar_prev_month
import churchpresentermobile.composeapp.generated.resources.calendar_service_types
import com.church.presenter.churchpresentermobile.calendar.YearMonthRef
import com.church.presenter.churchpresentermobile.calendar.dayShortName
import com.church.presenter.churchpresentermobile.calendar.monthGridDates
import com.church.presenter.churchpresentermobile.calendar.monthTitle
import com.church.presenter.churchpresentermobile.calendar.storedDate
import com.church.presenter.churchpresentermobile.calendar.weekDays
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.ServiceKind
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

private const val DAYS_PER_WEEK = 7
private const val MAX_DOTS = 3

/** The month grid: a dot under each planned day, the selected day lit, today ringed. */
@Composable
internal fun MonthGrid(
    month: YearMonthRef,
    selected: LocalDate,
    today: LocalDate,
    services: List<PlannedService>,
    onSelect: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val byDate = services.groupBy { it.date }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NavButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(Res.string.calendar_prev_month), onPrevious)
            Text(
                text = monthTitle(month),
                color = colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            NavButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(Res.string.calendar_next_month), onNext)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays().forEach { day ->
                Text(
                    text = dayShortName(day),
                    color = colors.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.06.em,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        monthGridDates(month).chunked(DAYS_PER_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        inMonth = month.contains(date),
                        selected = date == selected,
                        isToday = date == today,
                        dots = byDate[storedDate(date)].orEmpty().map { ServiceKind.byId(it.kind).colorHex }.take(MAX_DOTS),
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = colors.secondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    isToday: Boolean,
    dots: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.accent else colors.background.copy(alpha = 0f))
            .then(if (isToday && !selected) Modifier.border(1.dp, colors.accent, RoundedCornerShape(10.dp)) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.day.toString(),
            color = when {
                selected -> colors.onAccent
                !inMonth -> colors.dim
                else -> colors.text
            },
            fontSize = 14.sp,
            fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(6.dp)) {
            dots.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (selected) colors.onAccent else colorOf(hex)),
                )
            }
        }
    }
}

/** How many of each kind the month holds, under the grid. */
@Composable
internal fun ServiceTypesLegend(services: List<PlannedService>, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CalendarOverline(stringResource(Res.string.calendar_service_types))
        ServiceKind.entries.forEach { kind ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorDot(kind.colorHex)
                Text(kindName(kind), color = colors.secondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(services.count { it.kind == kind.id }.toString(), color = colors.muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
internal fun kindName(kind: ServiceKind): String = stringResource(
    when (kind) {
        ServiceKind.SUNDAY -> Res.string.calendar_kind_sunday
        ServiceKind.MIDWEEK -> Res.string.calendar_kind_midweek
        ServiceKind.SPECIAL -> Res.string.calendar_kind_special
    },
)
