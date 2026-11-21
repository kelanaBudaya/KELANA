package id.kelana.feature.jelajah

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class KategoriUiState(
    /** Null when the route carried an unknown category key. */
    val category: PlaceCategory? = null,
    val isLoading: Boolean = true,
    val openTodayOnly: Boolean = false,
    /** All places of the category, before the "Buka hari ini" filter. */
    val totalCount: Int = 0,
    val places: List<Place> = emptyList(),
    val today: LocalDate? = null,
) {
    /** The category has no place at all (for example Topeng): show the "Kabari aku" state. */
    val isCategoryEmpty: Boolean get() = !isLoading && totalCount == 0

    /** The category has places but the filter hides all of them. */
    val isFilteredEmpty: Boolean get() = !isLoading && totalCount > 0 && places.isEmpty()
}

@HiltViewModel
class KategoriViewModel internal constructor(
    placeRepository: PlaceRepository,
    private val category: PlaceCategory?,
    today: () -> LocalDate,
) : ViewModel() {

    /** Type-safe navigation stores the route argument under its property name, "category". */
    @Inject
    constructor(placeRepository: PlaceRepository, handle: SavedStateHandle) :
        this(placeRepository, handle.get<String>("category")?.let(PlaceCategory::fromKey), { LocalDate.now() })

    private val date = today()
    private val openTodayOnly = MutableStateFlow(false)

    val state: StateFlow<KategoriUiState> = combine(
        category?.let(placeRepository::placesByCategory) ?: flowOf(emptyList()),
        openTodayOnly,
    ) { all, openOnly ->
        KategoriUiState(
            category = category,
            isLoading = false,
            openTodayOnly = openOnly,
            totalCount = all.size,
            places = if (openOnly) all.filter { it.isOpenOn(date) } else all,
            today = date,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KategoriUiState(category = category))

    fun onOpenTodayChange(enabled: Boolean) {
        openTodayOnly.value = enabled
    }
}
