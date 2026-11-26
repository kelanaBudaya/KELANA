package id.kelana.feature.jelajah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.RegionRepository
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
    /** Casual name of the selected region for the subtitle ("Jatim"); null when none is selected. */
    val regionShortName: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && places.isEmpty()
}

@HiltViewModel
class SemuaTempatViewModel internal constructor(
    placeRepository: PlaceRepository,
    regions: RegionRepository,
    today: () -> LocalDate,
) : ViewModel() {

    @Inject
    constructor(placeRepository: PlaceRepository, regions: RegionRepository) : this(placeRepository, regions, { todayInJakarta() })

    private val date = today()
    private val category = MutableStateFlow<PlaceCategory?>(null)
    private val openTodayOnly = MutableStateFlow(false)

    val state: StateFlow<SemuaTempatUiState> = combine(
        placeRepository.places(),
        category,
        openTodayOnly,
        regions.selectedRegion(),
    ) { all, selected, openOnly, region ->
        SemuaTempatUiState(
            isLoading = false,
            category = selected,
            openTodayOnly = openOnly,
            totalCount = all.size,
            places = all
                .filter { selected == null || it.category == selected }
                .filter { !openOnly || it.isOpenOn(date) },
            today = date,
            regionShortName = region?.shortName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SemuaTempatUiState())

    fun onCategoryChange(value: PlaceCategory?) {
        category.value = value
    }

    fun onOpenTodayChange(enabled: Boolean) {
        openTodayOnly.value = enabled
    }
}
