package id.kelana.feature.jelajah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventRepository
import id.kelana.core.model.FactDisplay
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.RegionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** PROVISIONAL: real stamp data arrives in Phase 3. The Paspor card and the weekly challenge show these numbers. */
internal const val PLACEHOLDER_STAMP_COUNT = 3
internal const val PASSPORT_TOTAL = 10
internal const val PLACEHOLDER_CHALLENGE_DONE = 1
internal const val PLACEHOLDER_CHALLENGE_GOAL = 3

data class JelajahUiState(
    val isLoading: Boolean = true,
    val date: LocalDate? = null,
    val regionName: String? = null,
    /** Next checked event today that has not started yet; null hides the banner. */
    val tonightEvent: EventOccurrence? = null,
    val featuredPlaces: List<Place> = emptyList(),
    val stampCount: Int = PLACEHOLDER_STAMP_COUNT,
    val stampTotal: Int = PASSPORT_TOTAL,
)

@HiltViewModel
class JelajahViewModel internal constructor(
    places: PlaceRepository,
    events: EventRepository,
    regions: RegionRepository,
    now: () -> LocalDateTime,
) : ViewModel() {

    @Inject
    constructor(places: PlaceRepository, events: EventRepository, regions: RegionRepository) :
        this(places, events, regions, { LocalDateTime.now() })

    private val start = now()

    val state: StateFlow<JelajahUiState> = combine(
        places.places(),
        events.eventsOn(start.toLocalDate()),
        regions.regions(),
        regions.selectedRegionId,
    ) { allPlaces, todays, allRegions, selectedId ->
        JelajahUiState(
            isLoading = false,
            date = start.toLocalDate(),
            regionName = allRegions.firstOrNull { it.id == selectedId }?.name,
            tonightEvent = todays.firstOrNull { it.startsAfter(start) },
            featuredPlaces = allPlaces,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JelajahUiState())
}

/** Only an event whose schedule has a check date may show its time; it must not have started yet. */
private fun EventOccurrence.startsAfter(now: LocalDateTime): Boolean {
    val shown = event.schedule.display() as? FactDisplay.Checked ?: return false
    return !shown.value.start.isBefore(now.toLocalTime())
}
