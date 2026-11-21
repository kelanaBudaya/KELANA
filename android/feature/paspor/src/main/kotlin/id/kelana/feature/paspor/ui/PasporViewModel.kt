package id.kelana.feature.paspor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.RegionRepository
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.StampRepository
import id.kelana.feature.paspor.domain.PassportItem
import id.kelana.feature.paspor.domain.buildPassport
import id.kelana.feature.paspor.domain.collectedCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Figma 7.4. Counts are real: [collected] is 0 when the user has no confirmed stamp. */
data class PasporUiState(
    val isLoading: Boolean = true,
    val isGuest: Boolean = false,
    val initials: String = "",
    val regionName: String? = null,
    val regionCode: String? = null,
    val items: List<PassportItem> = emptyList(),
) {
    val total: Int get() = items.size
    val collected: Int get() = items.collectedCount()
    val remaining: Int get() = total - collected

    /** The collected place stamped last, used for the postcard prompt. */
    val latestStamped: PassportItem? get() = items.filter { it.isCollected }.maxByOrNull { it.stampedAt!! }
}

@HiltViewModel
class PasporViewModel @Inject constructor(
    places: PlaceRepository,
    stamps: StampRepository,
    regions: RegionRepository,
    session: SessionRepository,
) : ViewModel() {
    val uiState: StateFlow<PasporUiState> = combine(
        places.places().catch { emit(emptyList()) },
        stamps.stamps().catch { emit(emptyList()) },
        regions.regions().catch { emit(emptyList()) },
        regions.selectedRegionId,
        session.sessionState,
    ) { allPlaces, ownStamps, allRegions, regionId, sessionState ->
        val region = allRegions.firstOrNull { it.id == regionId }
        PasporUiState(
            isLoading = sessionState is SessionState.Loading,
            isGuest = sessionState is SessionState.Guest,
            initials = initialsOf(sessionState),
            regionName = region?.name,
            regionCode = region?.id,
            // A guest has no stamps: the repository already answers with an empty list.
            items = buildPassport(allPlaces, ownStamps),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PasporUiState())
}

internal fun initialsOf(state: SessionState): String = when (state) {
    is SessionState.SignedIn -> (state.user.displayName ?: state.user.email).orEmpty()
        .split(' ', '@').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    else -> ""
}
