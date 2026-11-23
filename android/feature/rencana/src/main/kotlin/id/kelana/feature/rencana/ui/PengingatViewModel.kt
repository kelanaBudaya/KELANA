package id.kelana.feature.rencana.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.Event
import id.kelana.core.model.EventRepository
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.ReminderScheduler
import id.kelana.core.model.ReminderTiming
import id.kelana.core.navigation.IZIN_NOTIF_RESULT_KEY
import id.kelana.feature.rencana.domain.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/** What the phone calendar's "new event" screen is filled with (no calendar permission is needed for that). */
data class CalendarInsert(val title: String, val location: String, val startMillis: Long, val endMillis: Long)

@Immutable
data class PengingatUiState(
    val timing: ReminderTiming = ReminderTiming.WhenToLeave,
    val addToCalendar: Boolean = false,
    val saved: Boolean = false,
    /** Set together with [saved] when "Masukkan ke kalender" was on and the show time is a checked official fact. */
    val calendarInsert: CalendarInsert? = null,
)

/**
 * "Kapan kami ingatkan?" for one show. "Simpan" stores the choice in the plan cache and asks [ReminderScheduler]
 * (feature notifikasi, WorkManager) to remind at that time. When "Masukkan ke kalender" is on, the screen opens the
 * calendar's insert intent with [PengingatUiState.calendarInsert].
 *
 * The notification permission step (4.9) happens before saving: the screen sends the user to Izin notifikasi, which
 * sets [IZIN_NOTIF_RESULT_KEY] in this entry's [SavedStateHandle] when it closes, and the save carries on from there.
 */
@HiltViewModel
class PengingatViewModel(
    private val plans: PlanRepository,
    private val scheduler: ReminderScheduler,
    private val events: EventRepository,
    private val clock: Clock,
    private val eventId: String,
    private val handle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    /** The route argument is stored in the handle under the field name of `PengingatRoute`. */
    @Inject
    constructor(
        plans: PlanRepository,
        scheduler: ReminderScheduler,
        events: EventRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(
        plans,
        scheduler,
        events,
        Clock.system(ZoneId.of("Asia/Jakarta")),
        savedStateHandle.get<String>("eventId").orEmpty(),
        savedStateHandle,
    )

    private val _state = MutableStateFlow(PengingatUiState())
    val uiState: StateFlow<PengingatUiState> = _state.asStateFlow()

    init {
        // The Izin notifikasi sheet closed (whatever the user chose): now save.
        viewModelScope.launch {
            handle.getStateFlow(IZIN_NOTIF_RESULT_KEY, false).collect { done ->
                if (done) {
                    handle[IZIN_NOTIF_RESULT_KEY] = false
                    save()
                }
            }
        }
    }

    fun selectTiming(timing: ReminderTiming) = _state.update { it.copy(timing = timing) }

    fun setAddToCalendar(value: Boolean) = _state.update { it.copy(addToCalendar = value) }

    /** "Simpan". */
    fun save() {
        val s = _state.value
        if (eventId.isEmpty() || s.saved) return
        viewModelScope.launch {
            val choice = ReminderChoice(s.timing, s.addToCalendar)
            plans.saveReminder(eventId, choice)
            scheduler.scheduleShowReminder(eventId, choice)
            val insert = if (s.addToCalendar) calendarInsertFor(events.event(eventId).first()) else null
            _state.update { it.copy(saved = true, calendarInsert = insert) }
        }
    }

    /** The next occurrence of the show that has not started yet, or null when there is none or its time is not a checked fact. */
    private fun calendarInsertFor(event: Event?): CalendarInsert? {
        if (event == null || event.schedule.checkedAt == null) return null
        val schedule = event.schedule.value
        val now = clock.instant()
        val zone = clock.zone
        val today = clock.instant().atZone(zone).toLocalDate()
        for (offset in 0 until LOOKAHEAD_DAYS) {
            val date = today.plusDays(offset.toLong())
            if (!schedule.recurrence.occursOn(date)) continue
            val start = LocalDateTime.of(date, schedule.start).atZone(zone).toInstant()
            if (!start.isAfter(now)) continue
            val end = schedule.end?.let { LocalDateTime.of(date, it).atZone(zone).toInstant() }
                ?.takeIf { it.isAfter(start) }
                ?: start.plus(DEFAULT_SHOW_LENGTH)
            return CalendarInsert(event.title, event.placeName, start.toEpochMilli(), end.toEpochMilli())
        }
        return null
    }

    private companion object {
        const val LOOKAHEAD_DAYS = 366

        /** PROVISIONAL: calendar length of a show that has no official end time. */
        val DEFAULT_SHOW_LENGTH: Duration = Duration.ofHours(1)
    }
}
