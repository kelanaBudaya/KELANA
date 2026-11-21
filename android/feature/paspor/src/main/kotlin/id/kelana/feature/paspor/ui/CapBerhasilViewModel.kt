package id.kelana.feature.paspor.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.RegionRepository
import id.kelana.core.model.StampRepository
import id.kelana.feature.paspor.domain.buildPassport
import id.kelana.feature.paspor.domain.collectedCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

/**
 * Figma 6.3. [collected] and [total] are real: the user's confirmed stamps and the places in the passport.
 * [stampedAt] is null when the stamp is not in the list (the screen then hides the date).
 */
data class CapBerhasilUiState(
    val isLoading: Boolean = true,
    val place: Place? = null,
    val collected: Int = 0,
    val total: Int = 0,
    val stampedAt: Instant? = null,
    val regionCode: String? = null,
) {
    val remaining: Int get() = (total - collected).coerceAtLeast(0)
}

@HiltViewModel
class CapBerhasilViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    places: PlaceRepository,
    stamps: StampRepository,
    regions: RegionRepository,
) : ViewModel() {
    private val placeId: String = savedStateHandle.get<String>("placeId").orEmpty()

    val uiState: StateFlow<CapBerhasilUiState> = combine(
        places.places().catch { emit(emptyList()) },
        stamps.stamps().catch { emit(emptyList()) },
        regions.selectedRegionId,
    ) { allPlaces, ownStamps, regionId ->
        val passport = buildPassport(allPlaces, ownStamps)
        val item = passport.firstOrNull { it.place.id.equals(placeId, ignoreCase = true) }
        CapBerhasilUiState(
            isLoading = false,
            place = item?.place,
            collected = passport.collectedCount(),
            total = passport.size,
            stampedAt = item?.stampedAt,
            regionCode = regionId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CapBerhasilUiState())
}
