package id.kelana.feature.rencana.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.PlanStop
import id.kelana.feature.rencana.domain.PlanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@Immutable
data class PetaRuteUiState(
    val isLoading: Boolean = true,
    /** Stops of the shown day in order; empty when there is no plan. */
    val stops: List<PlanStop> = emptyList(),
    /** The stop to go to next ("BERIKUTNYA"): the one going on now, else the first that has not started. */
    val next: PlanStop? = null,
)

/** Route view of the plan of today (or the next planned day). Reads the cached plan, so it works offline. */
@HiltViewModel
class PetaRuteViewModel(
    plans: PlanRepository,
    private val clock: Clock,
) : ViewModel() {

    @Inject
    constructor(plans: PlanRepository) : this(plans, Clock.system(ZoneId.of("Asia/Jakarta")))

    val uiState: StateFlow<PetaRuteUiState> = plans.trip.map { trip ->
        val today = LocalDate.now(clock)
        val now = LocalTime.now(clock)
        val day = trip?.days?.firstOrNull { it.date == today } ?: trip?.days?.firstOrNull { it.date.isAfter(today) }
        val stops = day?.stops.orEmpty()
        val next = if (day?.date == today) stops.firstOrNull { it.end > now } else stops.firstOrNull()
        PetaRuteUiState(isLoading = false, stops = stops, next = next)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetaRuteUiState())
}
