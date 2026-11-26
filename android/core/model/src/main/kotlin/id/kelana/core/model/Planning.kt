package id.kelana.core.model

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/*
 * Models of the Rencana planner (answers of the 6-step wizard, the produced plan, and the two small ports the
 * planner needs). Pure Kotlin. The plan is derived from official facts (hours, closures, show times) of [Place]
 * and [Event]; it never stores a fact that has no check date.
 */

/** Wizard step 2 "Kamu berkelana bersama siapa?". */
enum class TravelGroup { Solo, Couple, Family, Group }

/**
 * Wizard step 3 "Apa yang paling ingin kamu lihat?". [categories] are the place categories the interest selects;
 * [includesShows] means the interest also selects shows (events). [Semua] ("Semua jenis") selects every category and shows.
 */
enum class Interest(val categories: Set<PlaceCategory>, val includesShows: Boolean = false) {
    Museum(setOf(PlaceCategory.Museum)),
    Candi(setOf(PlaceCategory.Candi)),

    /** Shows (events), plus the venues that host them: tari and gamelan places. */
    Pentas(setOf(PlaceCategory.Tari, PlaceCategory.Gamelan), includesShows = true),
    Kerajinan(setOf(PlaceCategory.Kerajinan)),

    /**
     * Label "Situs & lainnya" (the constant name is kept for stability). There is no keraton category in the data;
     * keraton, puro, taman and every other site are filed under [PlaceCategory.Lainnya].
     */
    Keraton(setOf(PlaceCategory.Lainnya)),
    Topeng(setOf(PlaceCategory.Topeng)),

    /** "Semua jenis": every place category and shows. Must stay the last constant. */
    Semua(PlaceCategory.entries.toSet(), includesShows = true),
}

/** Wizard step 4 tempo. */
enum class Pace(val maxStops: Int, val breakMinutes: Int) {
    Relaxed(maxStops = 3, breakMinutes = 30),
    Moderate(maxStops = 4, breakMinutes = 15),
    Packed(maxStops = 5, breakMinutes = 10),
}

/** Wizard step 5 "Kamu berkeliling naik apa?". */
enum class Vehicle { TransitAndWalk, PrivateVehicle, WalkOnly, RideHailing }

/** Wizard step 6. "Tidak ada" is the empty set. */
enum class SpecialNeed { Wheelchair, AvoidStairs, MoreRests }

/** How the stretch between two stops is travelled (an estimate, never an official fact). */
enum class TravelMode { Walk, Transit, Car, RideHailing }

/** Everything the wizard collects. [endDate] equals [startDate] for a single day. */
data class PlanChoices(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val companion: TravelGroup,
    val interests: Set<Interest>,
    val windowStart: LocalTime = LocalTime.of(8, 0),
    val windowEnd: LocalTime = LocalTime.of(21, 0),
    val pace: Pace = Pace.Relaxed,
    val vehicle: Vehicle = Vehicle.TransitAndWalk,
    val needs: Set<SpecialNeed> = emptySet(),
) {
    val dates: List<LocalDate>
        get() = generateSequence(startDate) { it.plusDays(1) }.takeWhile { !it.isAfter(endDate) }.toList()
}

/** Estimated travel from the previous stop. [minutes] is a ceiling estimate from straight-line distance. */
data class Travel(val mode: TravelMode, val minutes: Int)

/**
 * One block of a day plan: a place visit, or a show ([eventId] != null).
 * [start] and [end] are planned times derived from [hours] (place) or [schedule] (show); both facts always carry a
 * check date when the plan is built. [price] is the first ticket tier (place) or the show ticket; a null value
 * inside the fact means "price not known", and a fact without check date must be shown as "sedang dicek".
 */
data class PlanStop(
    val placeId: String,
    val placeName: String,
    val capCode: String,
    val category: PlaceCategory,
    /** Catchy stamp name; null falls back to [capCode]. */
    val stampName: String? = null,
    val lat: Double,
    val lng: Double,
    val start: LocalTime,
    val end: LocalTime,
    val eventId: String? = null,
    val eventTitle: String? = null,
    val hours: OfficialFact<OpeningHours>? = null,
    val schedule: OfficialFact<EventSchedule>? = null,
    val price: OfficialFact<Long?>? = null,
    val travel: Travel? = null,
    val arrivalAdviceMin: Int? = null,
    val arrivalAdviceMax: Int? = null,
    val accessUnverified: Boolean = false,
) {
    val isEvent: Boolean get() = eventId != null

    /** Stamp identity to show: [stampName] when present, otherwise [capCode]. */
    val stampLabel: String get() = stampLabelOf(stampName, capCode)

    /** Unique within a plan: a place can appear once as a visit and once as the show held there. */
    val key: String get() = eventId ?: placeId
}

enum class SkipReason { ClosedThatDay, TemporarilyClosed, HoursUnknown, NoRoomInDay }

/** A place that matched the interests but is not in the plan. [detail] is the closure reason when known. */
data class SkippedPlace(val placeId: String, val placeName: String, val reason: SkipReason, val detail: String? = null)

/** The plan of one day. [stops] are in time order and never overlap. */
data class DayPlan(
    val date: LocalDate,
    val stops: List<PlanStop>,
    val skipped: List<SkippedPlace> = emptyList(),
    /** Pentas was chosen but no official show with a check date takes place on [date]. */
    val noShowFound: Boolean = false,
)

/** What the wizard produced: [days] has one entry per date of [choices]. */
data class TripPlan(val choices: PlanChoices, val days: List<DayPlan>, val builtOn: LocalDate)

/** When to remind about a show (Pengingat sheet). */
enum class ReminderTiming { WhenToLeave, OneHourBefore, ThirtyMinutesBefore }

data class ReminderChoice(val timing: ReminderTiming, val addToCalendar: Boolean)

/** Local store of the last plan so today's plan stays readable offline. */
interface PlanCache {
    val trip: Flow<TripPlan?>
    suspend fun save(trip: TripPlan)
    suspend fun clear()

    /** Reminder choice per event id. */
    val reminders: Flow<Map<String, ReminderChoice>>
    suspend fun saveReminder(eventId: String, choice: ReminderChoice)
}

/** Whether the device has a validated network. Injected so tests can fake it. */
interface ConnectivityMonitor {
    val isOnline: Flow<Boolean>
    fun isOnlineNow(): Boolean
}
