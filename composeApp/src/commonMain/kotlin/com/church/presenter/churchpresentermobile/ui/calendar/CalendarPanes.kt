package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.calendar.YearMonthRef
import com.church.presenter.churchpresentermobile.calendar.monthTitle
import com.church.presenter.churchpresentermobile.calendar.storedDate
import com.church.presenter.churchpresentermobile.calendar.today
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.ui.verticalScrollbar
import com.church.presenter.churchpresentermobile.viewmodel.CalendarViewModel
import kotlinx.datetime.LocalDate

/** What both panes draw from: the month on show, the day chosen inside it, and what each holds. */
internal class CalendarMonthState(
    val month: YearMonthRef,
    val selectedDate: LocalDate,
    val monthServices: List<PlannedService>,
    val dayServices: List<PlannedService>,
    val canCopyLast: Boolean,
)

/** The month header's three controls, which the two panes place differently but drive the same. */
internal class MonthHeaderActions(
    val onToday: () -> Unit,
    val onSync: () -> Unit,
    val synced: Boolean,
    val onSettings: (() -> Unit)?,
)

/** The tablet's shape: the month down the left, the chosen day's run of show filling the rest. */
@Composable
internal fun CalendarTwoPane(
    state: CalendarMonthState,
    header: MonthHeaderActions,
    viewModel: CalendarViewModel,
    sources: PickerSources,
    openService: PlannedService?,
    runActions: (PlannedService) -> RunOfShowActions,
    onAddService: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize().background(LocalAppColors.current.background)) {
        val monthScroll = rememberScrollState()
        Column(
            modifier = Modifier.width(MonthPaneWidth).fillMaxHeight()
                .verticalScrollbar(monthScroll)
                .verticalScroll(monthScroll),
        ) {
            MonthHeader(
                serviceCount = state.monthServices.size,
                monthName = monthTitle(state.month),
                onToday = header.onToday,
                onBack = null,
                compact = true,
                onSync = header.onSync,
                synced = header.synced,
                onSettings = header.onSettings,
            )
            Column(
                modifier = Modifier.padding(horizontal = PagePadding),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                MonthGridFor(state, viewModel)
                ServiceTypesLegend(state.monthServices)
                Spacer(Modifier.height(8.dp))
            }
        }
        VerticalDivider(color = LocalAppColors.current.borderSubtle)
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            DayHeader(state.selectedDate, state.dayServices, onAdd = onAddService)
            val shown = openService?.takeIf { it.date == storedDate(state.selectedDate) }
                ?: state.dayServices.firstOrNull()
            if (state.dayServices.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(horizontal = PagePadding).padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.dayServices.forEach { service ->
                        ServiceCard(
                            service = service,
                            selected = service.id == shown?.id,
                            onClick = { viewModel.openService(service.id) },
                            modifier = Modifier.width(ServiceCardWidth),
                        )
                    }
                }
            }
            HorizontalDivider(color = LocalAppColors.current.borderSubtle)
            if (shown != null) {
                RunOfShowScreen(
                    shown,
                    sources,
                    viewModel::newRowId,
                    runActions(shown),
                    modifier = Modifier.weight(1f),
                    inline = true,
                )
            } else {
                Box(modifier = Modifier.weight(1f).padding(PagePadding)) {
                    DayServices(
                        date = state.selectedDate,
                        services = emptyList(),
                        selectedId = null,
                        onOpen = {},
                        onAdd = onAddService,
                        onCopyLast = copyLast(state, viewModel),
                    )
                }
            }
        }
    }
}

/** The phone's shape: the month, the chosen day under it, and Add service pinned at the foot. */
@Composable
internal fun CalendarMonthPane(
    state: CalendarMonthState,
    header: MonthHeaderActions,
    viewModel: CalendarViewModel,
    onBack: (() -> Unit)?,
    onAddService: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(LocalAppColors.current.background)) {
        MonthHeader(
            serviceCount = state.monthServices.size,
            monthName = monthTitle(state.month),
            onToday = header.onToday,
            onBack = onBack,
            compact = false,
            onSync = header.onSync,
            synced = header.synced,
            onSettings = header.onSettings,
        )
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier.weight(1f)
                .verticalScrollbar(scroll)
                .verticalScroll(scroll)
                .padding(horizontal = PagePadding),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MonthGridFor(state, viewModel)
            DayServices(
                date = state.selectedDate,
                services = state.dayServices,
                selectedId = null,
                onOpen = { viewModel.openService(it.id) },
                onAdd = onAddService,
                onCopyLast = copyLast(state, viewModel),
            )
            ServiceTypesLegend(state.monthServices)
            Spacer(Modifier.height(8.dp))
        }
        // An empty day already offers Add service in its body; a second one under it is noise.
        if (state.dayServices.isNotEmpty()) {
            HorizontalDivider(color = LocalAppColors.current.borderSubtle)
            // The tab bar under this already clears the system navigation bar.
            AddServiceBar(
                onAdd = onAddService,
                modifier = Modifier.padding(horizontal = PagePadding, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun MonthGridFor(state: CalendarMonthState, viewModel: CalendarViewModel) {
    MonthGrid(
        state.month,
        state.selectedDate,
        today(),
        state.monthServices,
        viewModel::select,
        viewModel::showPreviousMonth,
        viewModel::showNextMonth,
    )
}

/** "Copy last Sunday", or nothing when there is no service to copy. */
private fun copyLast(state: CalendarMonthState, viewModel: CalendarViewModel): (() -> Unit)? =
    if (state.canCopyLast) ({ viewModel.copyLastInto(state.selectedDate) }) else null

private val ServiceCardWidth = 220.dp
