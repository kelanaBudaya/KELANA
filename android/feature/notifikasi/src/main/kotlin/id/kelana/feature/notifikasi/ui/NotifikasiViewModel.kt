package id.kelana.feature.notifikasi.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.AppNotification
import id.kelana.core.model.NotificationCategory
import id.kelana.core.model.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/** Chips of the center: Semua / Rencana / Paspor / Cerita. Info entries show under Semua only. */
enum class NotificationFilter(val category: NotificationCategory?) {
    All(null),
    Rencana(NotificationCategory.Rencana),
    Paspor(NotificationCategory.Paspor),
    Cerita(NotificationCategory.Cerita),
}

/** How a day group is titled: "HARI INI", "KEMARIN", or the date in id-ID. */
enum class RelativeDay { Today, Yesterday, Other }

@Immutable
data class NotificationItem(
    val id: String,
    val category: NotificationCategory,
    val title: String,
    val body: String,
    val time: LocalTime,
    /** True when the entry was unread when this screen opened (the dot stays while the screen is open). */
    val isUnread: Boolean,
    val placeId: String?,
    val eventId: String?,
)

@Immutable
data class NotificationGroup(val date: LocalDate, val relative: RelativeDay, val items: List<NotificationItem>)

@Immutable
data class NotifikasiUiState(
    val filter: NotificationFilter = NotificationFilter.All,
    val groups: List<NotificationGroup> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()
}

/**
 * Notification center (5.1). Opening the screen marks everything read, but the entries that were unread at that
 * moment keep their dot while the screen stays open, so the user can see what is new.
 */
@HiltViewModel
class NotifikasiViewModel(
    private val repository: NotificationRepository,
    private val clock: Clock,
    private val zone: () -> ZoneId,
) : ViewModel() {

    @Inject
    constructor(repository: NotificationRepository, clock: Clock) :
        this(repository, clock, { ZoneId.systemDefault() })

    private val filter = MutableStateFlow(NotificationFilter.All)
    private var unreadAtOpen: Set<String> = emptySet()
    private val _state = MutableStateFlow(NotifikasiUiState())
    val uiState: StateFlow<NotifikasiUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.notifications(), filter) { all, f -> all to f }.collect { (all, f) ->
                val unread = all.filter { !it.isRead }.map { it.id }
                unreadAtOpen = unreadAtOpen + unread
                _state.value = NotifikasiUiState(filter = f, groups = group(all, f, unreadAtOpen), isLoading = false)
                if (unread.isNotEmpty()) repository.markAllRead()
            }
        }
    }

    fun selectFilter(value: NotificationFilter) {
        filter.value = value
    }

    private fun group(all: List<AppNotification>, f: NotificationFilter, unread: Set<String>): List<NotificationGroup> {
        val zoneId = zone()
        val today = LocalDate.now(clock.withZone(zoneId))
        return all
            .filter { f.category == null || it.category == f.category }
            .sortedByDescending { it.createdAt }
            .groupBy { it.createdAt.atZone(zoneId).toLocalDate() }
            .map { (date, entries) ->
                NotificationGroup(
                    date = date,
                    relative = when (date) {
                        today -> RelativeDay.Today
                        today.minusDays(1) -> RelativeDay.Yesterday
                        else -> RelativeDay.Other
                    },
                    items = entries.map {
                        NotificationItem(
                            id = it.id,
                            category = it.category,
                            title = it.title,
                            body = it.body,
                            time = it.createdAt.atZone(zoneId).toLocalTime(),
                            isUnread = it.id in unread,
                            placeId = it.placeId,
                            eventId = it.eventId,
                        )
                    },
                )
            }
    }
}
