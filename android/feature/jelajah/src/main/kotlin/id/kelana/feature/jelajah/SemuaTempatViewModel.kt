package id.kelana.feature.jelajah

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
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class SemuaTempatUiState(
    val isLoading: Boolean = true,
    /** Null means every category. */
    val category: PlaceCategory? = null,
    val openTodayOnly: Boolean = false,
    val totalCount: Int = 0,
    val places: List<Place> = emptyList(),
    val today: LocalDate? = null,
) {
    val isEmpty: Boolean get() = !isLoading && places.isEmpty()
}

@HiltViewModel
class SemuaTempatViewModel internal constructor(
    placeRepository: PlaceRepository,
    today: () -> LocalDate,
) : ViewModel() {

    @Inject
    constructor(placeRepository: PlaceRepository) : this(placeRepository, { LocalDate.now() })

    private val date = today()
    private val category = MutableStateFlow<PlaceCategory?>(null)
    private val openTodayOnly = MutableStateFlow(false)

    val state: StateFlow<SemuaTempatUiState> = combine(
        placeRepository.places(),
        category,
        openTodayOnly,
    ) { all, selected, openOnly ->
        SemuaTempatUiState(
            isLoading = false,
            category = selected,
            openTodayOnly = openOnly,
            totalCount = all.size,
            places = all
                .filter { selected == null || it.category == selected }
                .filter { !openOnly || it.isOpenOn(date) },
            today = date,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SemuaTempatUiState())

    fun onCategoryChange(value: PlaceCategory?) {
        category.value = value
    }

    fun onOpenTodayChange(enabled: Boolean) {
        openTodayOnly.value = enabled
    }
}
