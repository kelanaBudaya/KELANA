package id.kelana.feature.notifikasi

import id.kelana.core.model.AppNotification
import id.kelana.core.model.Event
import id.kelana.core.model.EventOccurrence
import id.kelana.core.model.EventRepository
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.EventTicket
import id.kelana.core.model.NotificationCategory
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.Recurrence
import id.kelana.feature.notifikasi.data.AppForeground
import id.kelana.feature.notifikasi.data.NotificationStorage
import id.kelana.feature.notifikasi.data.SystemNotifier
import id.kelana.feature.notifikasi.domain.NotificationSettings
import id.kelana.feature.notifikasi.domain.NotificationSettingsRepository
import id.kelana.feature.notifikasi.domain.TextSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Friday 20 November 2026, 10.40 in Jakarta (03.40 UTC). */
internal val Jakarta: ZoneId = ZoneId.of("Asia/Jakarta")
internal val Now: Instant = Instant.parse("2026-11-20T03:40:00Z")

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())

    override fun finished(description: Description) = Dispatchers.resetMain()
}

internal class FakeStorage(initial: List<AppNotification> = emptyList()) : NotificationStorage {
    val state = MutableStateFlow(initial)
    override val entries: Flow<List<AppNotification>> = state
    override suspend fun update(transform: (List<AppNotification>) -> List<AppNotification>) {
        state.update(transform)
    }
}

internal class FakeSettings(initial: NotificationSettings = NotificationSettings()) : NotificationSettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<NotificationSettings> = state
    override suspend fun update(transform: (NotificationSettings) -> NotificationSettings) {
        state.update(transform)
    }
}

internal class FakeNotifier(var allowed: Boolean = true) : SystemNotifier {
    val shown = mutableListOf<AppNotification>()
    override fun canShow() = allowed
    override fun show(notification: AppNotification) {
        shown += notification
    }
}

internal class FakeForeground(var foreground: Boolean = false) : AppForeground {
    override fun isForeground() = foreground
}

/** Text as "resource id|args", so tests can check which string and which names were used without Android resources. */
internal val RecordingText = TextSource { id, args -> "$id|${args.joinToString("|")}" }

internal fun place(id: String = "sonobudoyo", name: String = "Museum Sonobudoyo", cap: String = "YK-02") = Place(
    id = id,
    slug = id,
    name = name,
    category = PlaceCategory.Museum,
    capCode = cap,
    lat = -7.8,
    lng = 110.36,
    address = "",
    description = "",
    coverKey = "",
)

internal class FakePlaces(private val all: List<Place>) : PlaceRepository {
    override fun places(): Flow<List<Place>> = flowOf(all)
    override fun place(id: String): Flow<Place?> = flowOf(all.firstOrNull { it.id == id })
    override fun placesByCategory(category: PlaceCategory): Flow<List<Place>> = flowOf(all.filter { it.category == category })
    override fun search(query: String): Flow<List<Place>> = flowOf(emptyList())
}

internal fun show(
    id: String = "roro",
    day: LocalDate? = LocalDate.of(2026, 11, 20),
    start: LocalTime = LocalTime.of(19, 30),
    recurrence: Recurrence? = null,
    checkedAt: LocalDate? = LocalDate.of(2026, 11, 20),
) = Event(
    id = id,
    placeId = "prambanan",
    placeName = "Prambanan",
    title = "Sendratari Roro Jonggrang",
    description = "",
    category = PlaceCategory.Tari,
    schedule = OfficialFact(
        EventSchedule(recurrence ?: Recurrence.Once(requireNotNull(day)), start, null),
        "https://example.org",
        checkedAt,
    ),
    ticket = OfficialFact(EventTicket(null, null), null, checkedAt),
)

internal class FakeEvents(private val all: List<Event>) : EventRepository {
    override fun eventsOn(date: LocalDate): Flow<List<EventOccurrence>> = eventsBetween(date, date)
    override fun eventsBetween(from: LocalDate, to: LocalDate): Flow<List<EventOccurrence>> =
        flowOf(all.flatMap { e -> e.occurrencesBetween(from, to).map { EventOccurrence(e, it) } })

    override fun event(id: String): Flow<Event?> = flowOf(all.firstOrNull { it.id == id })
}

internal fun entry(
    id: String,
    at: Instant = Now,
    category: NotificationCategory = NotificationCategory.Rencana,
    read: Boolean = false,
    placeId: String? = null,
    eventId: String? = null,
) = AppNotification(id, category, "title $id", "body $id", at, read, placeId, eventId)

