package id.kelana.feature.jelajah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventRepository
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

enum class CariFilter { Semua, Tempat, Pentas }

data class CariUiState(
    val query: String = "",
    val filter: CariFilter = CariFilter.Semua,
    val isLoading: Boolean = false,
    val places: List<Place> = emptyList(),
    val events: List<EventOccurrence> = emptyList(),
    /** Jakarta date for the "Kunjungi Hari Ini" badge; null shows no badge. */
    val today: LocalDate? = null,
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val isEmptyResult: Boolean get() = hasQuery && !isLoading && places.isEmpty() && events.isEmpty()
}

/** Events are searched within this many days ahead (PROVISIONAL: the repository has no text search for events). */
internal const val EVENT_SEARCH_DAYS = 14L

@HiltViewModel
class CariViewModel internal constructor(
    private val places: PlaceRepository,
    private val events: EventRepository,
    private val today: () -> LocalDate,
) : ViewModel() {

    @Inject
    constructor(places: PlaceRepository, events: EventRepository) :
        this(places, events, { todayInJakarta() })

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(CariFilter.Semua)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<CariUiState> = combine(query, filter) { q, f -> q to f }
        .flatMapLatest { (q, f) ->
            if (q.isBlank()) {
                flowOf(CariUiState(query = q, filter = f))
            } else {
                combine(placeResults(q, f), eventResults(q, f)) { p, e ->
                    CariUiState(query = q, filter = f, places = p, events = e, today = today())
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CariUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: CariFilter) {
        filter.value = value
    }

    private fun placeResults(q: String, f: CariFilter) =
        if (f == CariFilter.Pentas) flowOf(emptyList()) else places.search(q)

    private fun eventResults(q: String, f: CariFilter) =
        if (f == CariFilter.Tempat) {
            flowOf(emptyList())
        } else {
            val from = today()
            events.eventsBetween(from, from.plusDays(EVENT_SEARCH_DAYS)).map { all ->
                all.filter {
                    it.event.title.contains(q.trim(), ignoreCase = true) ||
                        it.event.placeName.contains(q.trim(), ignoreCase = true)
                }.distinctBy { it.event.id }
            }
        }
}
