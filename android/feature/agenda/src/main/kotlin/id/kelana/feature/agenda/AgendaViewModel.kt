package id.kelana.feature.agenda

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventRepository
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.RegionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** One cell of the date strip. [hasEvents] draws the dot under the day. */
@Immutable
data class AgendaDay(val date: LocalDate, val hasEvents: Boolean)

/** Events of one later day, shown under a heading such as "Sabtu, 21 November". */
@Immutable
data class AgendaSection(val date: LocalDate, val items: List<EventOccurrence>)

@Immutable
data class AgendaUiState(
    val isLoading: Boolean = true,
    val today: LocalDate,
    val selectedDate: LocalDate,
    val regionName: String? = null,
    val days: List<AgendaDay> = emptyList(),
    /** Categories that have events in the strip window, in enum order. "Semua" is `selectedFilter == null`. */
    val filters: List<PlaceCategory> = emptyList(),
    val selectedFilter: PlaceCategory? = null,
    /** First evening event of [selectedDate] ("Malam ini"). */
    val tonight: EventOccurrence? = null,
    /** Other events of [selectedDate] ("Juga hari ini"). */
    val alsoToday: List<EventOccurrence> = emptyList(),
    /** Events on the days after [selectedDate] within the strip window. */
    val upcoming: List<AgendaSection> = emptyList(),
) {
    val isSelectedDayEmpty: Boolean get() = tonight == null && alsoToday.isEmpty()
    val isEmpty: Boolean get() = isSelectedDayEmpty && upcoming.isEmpty()
}

/**
 * Agenda budaya. [clock] makes "today" injectable for tests and previews; Hilt uses the secondary constructor
 * (Asia/Jakarta). An event is listed only when its schedule has a check date: the date and time are official facts
 * and must not be shown without one (the detail screen shows "sedang dicek" for those).
 */
@HiltViewModel
class AgendaViewModel(
    private val events: EventRepository,
    private val regions: RegionRepository,
    clock: Clock,
) : ViewModel() {

    @Inject
    constructor(events: EventRepository, regions: RegionRepository) :
        this(events, regions, Clock.system(ZoneId.of("Asia/Jakarta")))

    private val today: LocalDate = LocalDate.now(clock)
    private val selectedDate = MutableStateFlow(today)
    private val selectedFilter = MutableStateFlow<PlaceCategory?>(null)

    private val windowEnd = today.plusDays(WINDOW_DAYS - 1L)

    private val regionName: Flow<String?> =
        combine(regions.regions(), regions.selectedRegionId) { list, id ->
            (list.firstOrNull { it.id == id } ?: list.firstOrNull { it.isAvailable })?.name
        }

    val uiState: StateFlow<AgendaUiState> =
        combine(
            events.eventsBetween(today, windowEnd).map { list -> list.filter { it.event.schedule.checkedAt != null } },
            selectedDate,
            selectedFilter,
            regionName,
        ) { occurrences, date, filter, region ->
            buildState(today, occurrences, date, filter, region)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AgendaUiState(today = today, selectedDate = today),
        )

    fun selectDate(date: LocalDate) {
        if (date in today..windowEnd) selectedDate.value = date
    }

    /** `null` means "Semua". */
    fun selectFilter(category: PlaceCategory?) {
        selectedFilter.update { category }
    }

    companion object {
        /** The date strip covers today and the following 13 days. */
        const val WINDOW_DAYS = 14
    }
}

internal fun buildState(
    today: LocalDate,
    occurrences: List<EventOccurrence>,
    selected: LocalDate,
    filter: PlaceCategory?,
    regionName: String?,
): AgendaUiState {
    val days = (0 until AgendaViewModel.WINDOW_DAYS).map { offset ->
        val date = today.plusDays(offset.toLong())
        AgendaDay(date, hasEvents = occurrences.any { it.date == date })
    }
    val filters = PlaceCategory.entries.filter { category -> occurrences.any { it.event.category == category } }
    val filtered = occurrences.filter { filter == null || it.event.category == filter }
    val startOf = { o: EventOccurrence -> o.event.schedule.value.start }
    val selectedDay = filtered.filter { it.date == selected }.sortedBy(startOf)
    val tonight = selectedDay.firstOrNull { startOf(it) >= EveningStart }
    val upcoming = filtered
        .filter { it.date.isAfter(selected) }
        .groupBy { it.date }
        .toSortedMap()
        .map { (date, items) -> AgendaSection(date, items.sortedBy(startOf)) }
    return AgendaUiState(
        isLoading = false,
        today = today,
        selectedDate = selected,
        regionName = regionName,
        days = days,
        filters = filters,
        selectedFilter = filter,
        tonight = tonight,
        alsoToday = selectedDay.filter { it != tonight },
        upcoming = upcoming,
    )
}
