package id.kelana.feature.peta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.EventRepository
import id.kelana.core.model.PlaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/** One immutable state for the map screen. */
internal data class PetaUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    /** Every place from the repository, before the filter. */
    val allPlaces: List<PetaPlace> = emptyList(),
    val filter: PetaFilter = PetaFilter.All,
    val selectedId: String? = null,
    val today: LocalDate? = null,
) {
    /** Places the pins show: the filter applied to [allPlaces]. */
    val pins: List<PetaPlace> get() = allPlaces.filter { it.matches(filter) }

    /** The selected place; null when nothing is selected or the filter hides it. */
    val selected: PetaPlace? get() = pins.firstOrNull { it.place.id == selectedId }

    /** No places at all (not the same as a filter that matches nothing). */
    val isEmpty: Boolean get() = !isLoading && !hasError && allPlaces.isEmpty()
}

@HiltViewModel
internal class PetaViewModel internal constructor(
    placeRepository: PlaceRepository,
    eventRepository: EventRepository,
    today: () -> LocalDate,
) : ViewModel() {

    @Inject
    constructor(placeRepository: PlaceRepository, eventRepository: EventRepository) :
        this(placeRepository, eventRepository, { LocalDate.now() })

    private val date = today()
    private val filter = MutableStateFlow(PetaFilter.All)
    private val selectedId = MutableStateFlow<String?>(null)
    private val attempt = MutableStateFlow(0)

    private data class Loaded(val isLoading: Boolean, val hasError: Boolean, val places: List<PetaPlace>)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val loaded = attempt.flatMapLatest {
        // Collected, not first(): the repositories emit the cache first and again after a network refresh,
        // and the refreshed facts (hours, check dates) must reach the map.
        // A failing event lookup only loses the "Pentas malam ini" badge; the map itself still works.
        val showIds = eventRepository.eventsOn(date)
            .map { placeIdsWithShow(it) }
            .onStart { emit(emptySet()) }
            .catch { emit(emptySet()) }
        combine(placeRepository.places(), showIds) { places, shows ->
            Loaded(
                isLoading = false,
                hasError = false,
                places = places.map { PetaPlace(it, openToday = it.isOpenOn(date), showToday = it.id in shows) },
            )
        }
            .onStart { emit(Loaded(isLoading = true, hasError = false, places = emptyList())) }
            .catch { emit(Loaded(isLoading = false, hasError = true, places = emptyList())) }
    }

    val state: StateFlow<PetaUiState> = combine(loaded, filter, selectedId) { l, f, s ->
        PetaUiState(
            isLoading = l.isLoading,
            hasError = l.hasError,
            allPlaces = l.places,
            filter = f,
            selectedId = s,
            today = date,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetaUiState())

    fun onFilterChange(value: PetaFilter) {
        filter.value = value
    }

    /** Tapping the selected pin again clears the selection. */
    fun onPinTap(placeId: String) {
        selectedId.value = if (selectedId.value == placeId) null else placeId
    }

    fun onRetry() {
        attempt.value += 1
    }
}
