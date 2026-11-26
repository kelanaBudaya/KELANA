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
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class JelajahUiState(
    val isLoading: Boolean = true,
    val date: LocalDate? = null,
    val regionName: String? = null,
    /** Casual name of the selected region for running text ("Jogja"); null until a region is selected. */
    val regionShortName: String? = null,
    /** Stamp-code prefix of the selected region's places ("YK", "JTM"); shown on the passport sticker. */
    val stampCode: String = "",
    /** Next checked event today that has not started yet; null hides the banner. */
    val tonightEvent: EventOccurrence? = null,
    val featuredPlaces: List<Place> = emptyList(),
    /** The user's confirmed stamps at places of the passport: 0 for a guest or without stamps. */
    val stampCount: Int = 0,
    /** Places in the passport; 0 until the places are loaded (the card then shows no number). */
    val stampTotal: Int = 0,
    /** Server-confirmed stamps this week (Jakarta) against the weekly target; 0 of the target for a guest. */
    val weekly: WeeklyProgress = WeeklyProgress(),
)

@HiltViewModel
class JelajahViewModel internal constructor(
    places: PlaceRepository,
    events: EventRepository,
    regions: RegionRepository,
    stamps: StampRepository,
    clock: Clock = Clock.system(ChallengeZone),
    now: () -> LocalDateTime,
) : ViewModel() {

    @Inject
    constructor(places: PlaceRepository, events: EventRepository, regions: RegionRepository, stamps: StampRepository) :
        this(places, events, regions, stamps, Clock.system(ChallengeZone), { LocalDateTime.now() })

    private val start = now()

    val state: StateFlow<JelajahUiState> = combine(
        places.places(),
        events.eventsOn(start.toLocalDate()),
        regions.selectedRegion(),
        stamps.stamps(),
    ) { allPlaces, todays, region, ownStamps ->
        val placeIds = allPlaces.map { it.id.lowercase() }.toSet()
        JelajahUiState(
            isLoading = false,
            date = start.toLocalDate(),
            regionName = region?.name,
            regionShortName = region?.shortName,
            stampCode = region?.let { stampCodePrefix(allPlaces, it.id) }.orEmpty(),
            tonightEvent = todays.firstOrNull { it.startsAfter(start) },
            featuredPlaces = allPlaces,
            stampCount = ownStamps.map { it.placeId.lowercase() }.distinct().count { it in placeIds },
            stampTotal = allPlaces.size,
            weekly = weeklyProgress(ownStamps, clock),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JelajahUiState())
}

/**
 * The stamp-code prefix shared by the places of one region ("YK-02" gives "YK"). Falls back to the
 * region code when there is no place or the first place's code has no usable prefix.
 */
internal fun stampCodePrefix(places: List<Place>, regionId: String): String =
    places.firstOrNull()?.capCode?.substringBefore('-', missingDelimiterValue = "")?.trim()?.takeIf { it.isNotEmpty() }
        ?: regionId

/** Only an event whose schedule has a check date may show its time; it must not have started yet. */
private fun EventOccurrence.startsAfter(now: LocalDateTime): Boolean {
    val shown = event.schedule.display() as? FactDisplay.Checked ?: return false
    return !shown.value.start.isBefore(now.toLocalTime())
}
