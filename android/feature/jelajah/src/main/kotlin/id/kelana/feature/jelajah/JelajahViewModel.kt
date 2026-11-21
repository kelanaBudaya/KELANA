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
import id.kelana.core.model.StampRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** PROVISIONAL: the weekly challenge still shows these numbers (the Paspor card shows real counts). */
internal const val PLACEHOLDER_CHALLENGE_DONE = 1
internal const val PLACEHOLDER_CHALLENGE_GOAL = 3

data class JelajahUiState(
    val isLoading: Boolean = true,
    val date: LocalDate? = null,
    val regionName: String? = null,
    /** Next checked event today that has not started yet; null hides the banner. */
    val tonightEvent: EventOccurrence? = null,
    val featuredPlaces: List<Place> = emptyList(),
    /** The user's confirmed stamps at places of the passport: 0 for a guest or without stamps. */
    val stampCount: Int = 0,
    /** Places in the passport; 0 until the places are loaded (the card then shows no number). */
    val stampTotal: Int = 0,
)

@HiltViewModel
class JelajahViewModel internal constructor(
    places: PlaceRepository,
    events: EventRepository,
    regions: RegionRepository,
    stamps: StampRepository,
    now: () -> LocalDateTime,
) : ViewModel() {

    @Inject
    constructor(places: PlaceRepository, events: EventRepository, regions: RegionRepository, stamps: StampRepository) :
        this(places, events, regions, stamps, { LocalDateTime.now() })

    private val start = now()

    val state: StateFlow<JelajahUiState> = combine(
        places.places(),
        events.eventsOn(start.toLocalDate()),
        regions.regions(),
        regions.selectedRegionId,
        stamps.stamps(),
    ) { allPlaces, todays, allRegions, selectedId, ownStamps ->
        val placeIds = allPlaces.map { it.id.lowercase() }.toSet()
        JelajahUiState(
            isLoading = false,
            date = start.toLocalDate(),
            regionName = allRegions.firstOrNull { it.id == selectedId }?.name,
            tonightEvent = todays.firstOrNull { it.startsAfter(start) },
            featuredPlaces = allPlaces,
            stampCount = ownStamps.map { it.placeId.lowercase() }.distinct().count { it in placeIds },
            stampTotal = allPlaces.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JelajahUiState())
}

/** Only an event whose schedule has a check date may show its time; it must not have started yet. */
private fun EventOccurrence.startsAfter(now: LocalDateTime): Boolean {
    val shown = event.schedule.display() as? FactDisplay.Checked ?: return false
    return !shown.value.start.isBefore(now.toLocalTime())
}
