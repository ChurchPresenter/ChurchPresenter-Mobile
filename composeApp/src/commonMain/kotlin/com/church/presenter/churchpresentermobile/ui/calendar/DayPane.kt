package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_add_service
import churchpresentermobile.composeapp.generated.resources.calendar_copy_last
import churchpresentermobile.composeapp.generated.resources.calendar_empty_hint
import churchpresentermobile.composeapp.generated.resources.calendar_nothing_planned
import churchpresentermobile.composeapp.generated.resources.calendar_service_count_one
import churchpresentermobile.composeapp.generated.resources.calendar_service_count_other
import com.church.presenter.churchpresentermobile.calendar.clockText
import com.church.presenter.churchpresentermobile.calendar.dayName
import com.church.presenter.churchpresentermobile.calendar.dayOverline
import com.church.presenter.churchpresentermobile.calendar.minutesText
import com.church.presenter.churchpresentermobile.calendar.shortDate
import com.church.presenter.churchpresentermobile.calendar.totalSeconds
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.ServiceKind
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/** The selected day's services as cards, or the empty day with its two ways out. */
@Composable
internal fun DayServices(
    date: LocalDate,
    services: List<PlannedService>,
    selectedId: String?,
    onOpen: (PlannedService) -> Unit,
    onAdd: () -> Unit,
    onCopyLast: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (services.isEmpty()) {
            EmptyDay(date, onAdd, onCopyLast)
        } else {
            CalendarOverline(dayOverline(date), trailing = serviceCountText(services.size))
            services.forEach { service ->
                ServiceCard(service, selected = service.id == selectedId, onClick = { onOpen(service) })
            }
        }
    }
}

/** A service in the day's list: a color bar for its kind, its name, and when and how long. */
@Composable
internal fun ServiceCard(
    service: PlannedService,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    CalendarCard(modifier = modifier, selected = selected, onClick = onClick) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colorOf(ServiceKind.byId(service.kind).colorHex)),
        )
        Column(modifier = Modifier.weight(1f)) {
            TitleText(service.name)
            MutedText(serviceSummary(service))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (selected) colors.accent else colors.dim,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** `10:00 AM · 12 items · 69 min`. */
@Composable
internal fun serviceSummary(service: PlannedService): String {
    val parts = mutableListOf(clockText(service.startTime), itemCountText(service.rows))
    val seconds = totalSeconds(service)
    if (seconds > 0) parts += minutesText(seconds)
    return parts.joinToString(" · ")
}

@Composable
internal fun serviceCountText(count: Int): String =
    if (count == 1) {
        stringResource(Res.string.calendar_service_count_one)
    } else {
        stringResource(Res.string.calendar_service_count_other, count)
    }

@Composable
private fun EmptyDay(date: LocalDate, onAdd: () -> Unit, onCopyLast: (() -> Unit)?) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceStrong),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "${stringResource(Res.string.calendar_nothing_planned)}\n${shortDate(date)}",
            color = colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(Res.string.calendar_empty_hint),
            color = colors.muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        CalendarPrimaryButton(
            stringResource(Res.string.calendar_add_service),
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
        )
        if (onCopyLast != null) {
            Spacer(Modifier.height(8.dp))
            CalendarSecondaryButton(
                label = stringResource(Res.string.calendar_copy_last, dayName(date.dayOfWeek)),
                onClick = onCopyLast,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** The bottom "Add service" bar of the phone's month screen. */
@Composable
internal fun AddServiceBar(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    CalendarPrimaryButton(
        label = stringResource(Res.string.calendar_add_service),
        icon = Icons.Filled.Add,
        onClick = onAdd,
        modifier = modifier.fillMaxWidth(),
    )
}
