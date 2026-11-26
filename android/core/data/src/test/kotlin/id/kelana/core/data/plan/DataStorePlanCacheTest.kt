package id.kelana.core.data.plan

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import id.kelana.core.model.DayPlan
import id.kelana.core.model.EventSchedule
import id.kelana.core.model.Interest
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Pace
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlanChoices
import id.kelana.core.model.PlanStop
import id.kelana.core.model.Recurrence
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.ReminderTiming
import id.kelana.core.model.SkipReason
import id.kelana.core.model.SkippedPlace
import id.kelana.core.model.SpecialNeed
import id.kelana.core.model.Travel
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.TravelMode
import id.kelana.core.model.TripPlan
import id.kelana.core.model.Vehicle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Logic runs on an in-memory DataStore (the real file store cannot replace its file twice on Windows). One real-file
 * test with a single write covers the process restart.
 */
class DataStorePlanCacheTest {
    @get:Rule
    val folder = TemporaryFolder()

    private class MemoryStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            state.value = transform(state.value)
            return state.value
        }
    }

    private val d1 = LocalDate.of(2026, 12, 1)
    private val d2 = LocalDate.of(2026, 12, 2)

    private fun richTrip(): TripPlan {
        val checked = LocalDate.of(2026, 11, 20)
        val visit = PlanStop(
            placeId = "p1", placeName = "Prambanan", capCode = "PRB", category = PlaceCategory.Candi,
            lat = -7.752, lng = 110.491, start = LocalTime.of(9, 0), end = LocalTime.of(10, 30),
            hours = OfficialFact(OpeningHours(DayOfWeek.TUESDAY, false, LocalTime.of(8, 0), LocalTime.of(17, 0), "last entry 16:00"), "https://x.id", checked),
            price = OfficialFact(50_000L, "https://x.id/p", checked),
            travel = Travel(TravelMode.Walk, 5),
            arrivalAdviceMin = 10, arrivalAdviceMax = 20, accessUnverified = true,
        )
        val unchecked = visit.copy(
            placeId = "p2", placeName = "Museum", category = PlaceCategory.Museum,
            start = LocalTime.of(11, 0), end = LocalTime.of(12, 0),
            hours = OfficialFact(OpeningHours(DayOfWeek.SUNDAY, true, null, null), null, null),
            price = OfficialFact<Long?>(null, null, null),
            travel = Travel(TravelMode.RideHailing, 12),
        )
        val show = PlanStop(
            placeId = "p1", placeName = "Prambanan", capCode = "PRB", category = PlaceCategory.Tari,
            lat = -7.752, lng = 110.491, start = LocalTime.of(19, 0), end = LocalTime.of(21, 0),
            eventId = "e1", eventTitle = "Ramayana",
            schedule = OfficialFact(EventSchedule(Recurrence.Weekly(DayOfWeek.TUESDAY), LocalTime.of(19, 30), LocalTime.of(21, 0)), "https://x.id/e", checked),
            price = OfficialFact(150_000L, null, checked),
            travel = Travel(TravelMode.Car, 20),
        )
        val showOnce = show.copy(
            eventId = "e2",
            schedule = OfficialFact(EventSchedule(Recurrence.Once(d2), LocalTime.of(19, 0), null), null, null),
            travel = Travel(TravelMode.Transit, 30),
        )
        val showDaily = show.copy(
            eventId = "e3",
            schedule = OfficialFact(EventSchedule(Recurrence.Daily, LocalTime.of(10, 0), null), null, checked),
            travel = null,
        )
        return TripPlan(
            choices = PlanChoices(
                d1, d2, TravelGroup.Family, Interest.entries.toSet(), LocalTime.of(7, 30), LocalTime.of(22, 0),
                Pace.Packed, Vehicle.RideHailing, SpecialNeed.entries.toSet(),
            ),
            days = listOf(
                DayPlan(
                    d1, listOf(visit, unchecked, show),
                    skipped = listOf(
                        SkippedPlace("s1", "Keraton", SkipReason.TemporarilyClosed, "Renovasi"),
                        SkippedPlace("s2", "Taman", SkipReason.NoRoomInDay),
                    ),
                ),
                DayPlan(d2, listOf(showOnce, showDaily), noShowFound = true),
            ),
            builtOn = d1.minusDays(3),
        )
    }

    @Test
    fun startsEmpty() = runBlocking {
        val cache = DataStorePlanCache(MemoryStore())
        assertNull(cache.trip.first())
        assertEquals(emptyMap<String, ReminderChoice>(), cache.reminders.first())
    }

    @Test
    fun richTripRoundTripsThroughANewInstance() = runBlocking {
        val store = MemoryStore()
        val trip = richTrip()
        DataStorePlanCache(store).save(trip)
        assertEquals(trip, DataStorePlanCache(store).trip.first())
    }

    @Test
    fun everyEnumValueRoundTrips() = runBlocking {
        val store = MemoryStore()
        val base = richTrip()
        for (mode in TravelMode.entries) for (vehicle in Vehicle.entries) for (pace in Pace.entries) {
            val stop = base.days[0].stops[0].copy(travel = Travel(mode, 1))
            val trip = base.copy(
                choices = base.choices.copy(vehicle = vehicle, pace = pace, interests = Interest.entries.toSet()),
                days = listOf(base.days[0].copy(stops = listOf(stop))),
            )
            val cache = DataStorePlanCache(store)
            cache.save(trip)
            assertEquals(trip, cache.trip.first())
        }
    }

    @Test
    fun tripWhoseDaysAreAllPastIsStillReturned() = runBlocking {
        val cache = DataStorePlanCache(MemoryStore())
        val past = richTrip().let { it.copy(choices = it.choices.copy(startDate = LocalDate.of(2020, 1, 1), endDate = LocalDate.of(2020, 1, 1)), days = listOf(it.days[0].copy(date = LocalDate.of(2020, 1, 1)))) }
        cache.save(past)
        assertEquals(past, cache.trip.first())
    }

    @Test
    fun corruptJsonReadsAsNull() = runBlocking {
        val store = MemoryStore()
        store.edit { it[DataStorePlanCache.TRIP_KEY] = "{not json" }
        assertNull(DataStorePlanCache(store).trip.first())
    }

    @Test
    fun otherVersionReadsAsNull() = runBlocking {
        val store = MemoryStore()
        val cache = DataStorePlanCache(store)
        cache.save(richTrip())
        store.edit { it[DataStorePlanCache.TRIP_KEY] = it[DataStorePlanCache.TRIP_KEY]!!.replaceFirst("\"version\":1", "\"version\":99") }
        assertNull(cache.trip.first())
    }

    @Test
    fun unknownEnumNameReadsAsNullWithoutCrash() = runBlocking {
        val store = MemoryStore()
        val cache = DataStorePlanCache(store)
        cache.save(richTrip())
        store.edit { it[DataStorePlanCache.TRIP_KEY] = it[DataStorePlanCache.TRIP_KEY]!!.replace("\"RideHailing\"", "\"Teleport\"") }
        assertNull(cache.trip.first())
    }

    @Test
    fun missingFieldReadsAsNull() = runBlocking {
        val store = MemoryStore()
        store.edit { it[DataStorePlanCache.TRIP_KEY] = "{\"version\":1,\"trip\":{}}" }
        assertNull(DataStorePlanCache(store).trip.first())
    }

    @Test
    fun remindersPersistAndAreReplacedPerEvent() = runBlocking {
        val store = MemoryStore()
        val cache = DataStorePlanCache(store)
        cache.saveReminder("e1", ReminderChoice(ReminderTiming.OneHourBefore, true))
        cache.saveReminder("e2", ReminderChoice(ReminderTiming.WhenToLeave, false))
        cache.saveReminder("e1", ReminderChoice(ReminderTiming.ThirtyMinutesBefore, false))
        assertEquals(
            mapOf(
                "e1" to ReminderChoice(ReminderTiming.ThirtyMinutesBefore, false),
                "e2" to ReminderChoice(ReminderTiming.WhenToLeave, false),
            ),
            DataStorePlanCache(store).reminders.first(),
        )
    }

    @Test
    fun unknownReminderTimingIsDroppedOthersKept() = runBlocking {
        val store = MemoryStore()
        val cache = DataStorePlanCache(store)
        cache.saveReminder("e1", ReminderChoice(ReminderTiming.WhenToLeave, true))
        cache.saveReminder("e2", ReminderChoice(ReminderTiming.OneHourBefore, true))
        store.edit { it[DataStorePlanCache.REMINDERS_KEY] = it[DataStorePlanCache.REMINDERS_KEY]!!.replace("OneHourBefore", "Never") }
        assertEquals(mapOf("e1" to ReminderChoice(ReminderTiming.WhenToLeave, true)), cache.reminders.first())
    }

    @Test
    fun clearWipesPlanAndReminders() = runBlocking {
        val cache = DataStorePlanCache(MemoryStore())
        cache.save(richTrip())
        cache.saveReminder("e1", ReminderChoice(ReminderTiming.WhenToLeave, true))
        cache.clear()
        assertNull(cache.trip.first())
        assertEquals(emptyMap<String, ReminderChoice>(), cache.reminders.first())
    }

    @Test
    fun savedTripSurvivesARealFileRestart() = runBlocking {
        val file = File(folder.root, "plan.preferences_pb")
        val scope = CoroutineScope(Dispatchers.IO)
        val trip = richTrip()
        DataStorePlanCache(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })).save(trip)
        // DataStore refuses two active stores on one file: release the first before reopening.
        scope.coroutineContext[Job]!!.cancelAndJoin()
        val scope2 = CoroutineScope(Dispatchers.IO)
        try {
            val reopened = DataStorePlanCache(PreferenceDataStoreFactory.create(scope = scope2, produceFile = { file }))
            assertEquals(trip, reopened.trip.first())
        } finally {
            scope2.coroutineContext[Job]!!.cancelAndJoin()
        }
    }
}
