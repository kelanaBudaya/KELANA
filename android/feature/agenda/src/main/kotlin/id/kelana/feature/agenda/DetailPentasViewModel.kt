package id.kelana.feature.agenda

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.Event
import id.kelana.core.model.EventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/** "Sumber: example.org, dibuka 20 Nov 2026". Only built when the schedule has a check date and an https source. */
@Immutable
data class SourceNote(val host: String, val openedAt: LocalDate)

sealed interface DetailPentasUiState {
    data object Loading : DetailPentasUiState

    data object NotFound : DetailPentasUiState

    @Immutable
    data class Found(
        val event: Event,
        val sourceNote: SourceNote?,
        /** Official ticket seller, set only when the ticket fact is checked and the URL is https. */
        val ticketUrl: String?,
    ) : DetailPentasUiState
}

@HiltViewModel
class DetailPentasViewModel(
    events: EventRepository,
    eventId: String,
) : ViewModel() {

    /** The route argument is stored in the handle under the field name of `DetailPentasRoute`. */
    @Inject
    constructor(events: EventRepository, savedStateHandle: SavedStateHandle) :
        this(events, savedStateHandle.get<String>("eventId").orEmpty())

    val uiState: StateFlow<DetailPentasUiState> = events.event(eventId)
        .map { it?.let(::toFound) ?: DetailPentasUiState.NotFound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailPentasUiState.Loading)
}

internal fun toFound(event: Event): DetailPentasUiState.Found {
    val schedule = event.schedule
    val host = httpsHost(schedule.sourceUrl)
    val openedAt = schedule.checkedAt
    val note = if (host != null && openedAt != null) SourceNote(host, openedAt) else null
    val ticket = event.ticket
    val sellerUrl = ticket.value.sellerUrl
    val ticketUrl = if (ticket.checkedAt != null && httpsHost(sellerUrl) != null) sellerUrl else null
    return DetailPentasUiState.Found(event, note, ticketUrl)
}
