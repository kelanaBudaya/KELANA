package id.kelana.feature.tempat.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.Event
import id.kelana.core.model.EventRepository
import id.kelana.core.model.Place
import id.kelana.core.model.PlacePhotoRepository
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.Recurrence
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class PlaceLoadStatus { Loading, Loaded, NotFound, Error }

/** Single immutable state of Detail tempat. [place] is set only when [status] is [PlaceLoadStatus.Loaded]. */
data class DetailTempatUiState(
    val status: PlaceLoadStatus = PlaceLoadStatus.Loading,
    val place: Place? = null,
    /** Events hosted at [place], ready to show. Empty while loading, on error, or when there are none. */
    val events: List<Event> = emptyList(),
    /** Published open-licensed photos of [place]; 0 while loading, on error, or when there are none (chip hidden). */
    val photoCount: Int = 0,
    /** Full-size URL of the first gallery photo (sort order 0) for the header cover; null = show the illustration. */
    val coverUrl: String? = null,
)

/**
 * Loads one place by the `placeId` route argument, plus the events hosted there. Events are optional content: a
 * failure to load them only hides that section. [clock] makes "today" injectable; Hilt uses the secondary
 * constructor (Asia/Jakarta).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailTempatViewModel(
    savedStateHandle: SavedStateHandle,
    private val places: PlaceRepository,
    private val events: EventRepository,
    private val photos: PlacePhotoRepository,
    private val clock: Clock,
) : ViewModel() {
    @Inject
    constructor(savedStateHandle: SavedStateHandle, places: PlaceRepository, events: EventRepository, photos: PlacePhotoRepository) :
        this(savedStateHandle, places, events, photos, Clock.system(ZoneId.of("Asia/Jakarta")))

    private val placeId: String = savedStateHandle.get<String>("placeId").orEmpty()
    private val reload = MutableStateFlow(0)

    val uiState: StateFlow<DetailTempatUiState> = reload
        .flatMapLatest {
            combine(places.place(placeId), hostedEvents(), photoSummary()) { place, hosted, photo ->
                    if (place == null) {
                        DetailTempatUiState(PlaceLoadStatus.NotFound)
                    } else {
                        DetailTempatUiState(PlaceLoadStatus.Loaded, place, hosted, photo.first, photo.second)
                    }
                }
                .onStart { emit(DetailTempatUiState(PlaceLoadStatus.Loading)) }
                .catch { e ->
                    if (e is CancellationException) throw e
                    emit(DetailTempatUiState(PlaceLoadStatus.Error))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailTempatUiState())

    /** Shown events: schedule checked (an official fact), no one-time event in the past, soonest first. */
    private fun hostedEvents(): Flow<List<Event>> {
        val today = LocalDate.now(clock)
        return events.eventsAt(placeId)
            .map { list ->
                list.filter { it.schedule.checkedAt != null }
                    .mapNotNull { e -> nextDate(e, today)?.let { e to it } }
                    .sortedWith(compareBy({ it.second }, { it.first.schedule.value.start }))
                    .map { it.first }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }
            // Do not hold the place back while events load.
            .onStart { emit(emptyList()) }
    }

    /** Optional content: (count, cover url). Any failure counts as (0, null): chip hidden, illustration cover. */
    private fun photoSummary(): Flow<Pair<Int, String?>> = photos.photos(placeId)
        .map { list -> list.size to list.firstOrNull()?.url }
        .catch { e ->
            if (e is CancellationException) throw e
            emit(0 to null)
        }
        .onStart { emit(0 to null) }

    private fun nextDate(event: Event, today: LocalDate): LocalDate? =
        when (val r = event.schedule.value.recurrence) {
            is Recurrence.Once -> r.date.takeIf { !it.isBefore(today) }
            else -> event.nextOccurrences(today, 1).firstOrNull()
        }

    fun retry() {
        reload.value += 1
    }
}
