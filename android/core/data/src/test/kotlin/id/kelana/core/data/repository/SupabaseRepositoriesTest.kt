package id.kelana.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import id.kelana.core.data.remote.EventsPayload
import id.kelana.core.data.remote.PlacesPayload
import id.kelana.core.data.remote.toEntity
import id.kelana.core.data.remote.toEventClosureEntity
import id.kelana.core.data.remote.toFactEntities
import id.kelana.core.model.AuthResult
import id.kelana.core.model.FactDisplay
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.ReportDraft
import id.kelana.core.model.ReportReason
import id.kelana.core.model.ReportResult
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.SessionUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SupabaseRepositoriesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun payload(name: String = "Candi Prambanan") = PlacesPayload(
        places = listOf(placeDto("p1", name)),
        hours = listOf(hoursDto("p1", 1)),
        closures = emptyList(),
        tickets = listOf(ticketDto("p1")),
    )

    private fun placeRepo(remote: FakeRemote, dao: FakePlaceDao = FakePlaceDao(), regions: StubRegions = StubRegions()) =
        SupabasePlaceRepository(remote, dao, regions, testGate())

    // ---------- places ----------

    @Test
    fun places_emitCacheFirst_thenNetworkResult() = runTest {
        val dao = FakePlaceDao()
        val old = PlacesPayload(listOf(placeDto("p1", "Nama Lama")), emptyList(), emptyList(), emptyList())
        dao.replaceAll(old.places.mapNotNull { it.toEntity() }, old.toFactEntities())
        val remote = FakeRemote().apply { places = payload("Nama Baru") }

        placeRepo(remote, dao).places().test {
            assertEquals(listOf("Nama Lama"), awaitItem().map { it.name })
            assertEquals(listOf("Nama Baru"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, remote.fetchCount)
    }

    @Test
    fun places_coldStart_doesNotEmitEmptyBeforeNetwork() = runTest {
        val remote = FakeRemote().apply { places = payload() }
        placeRepo(remote).places().test {
            val first = awaitItem()
            assertEquals(1, first.size)
            assertEquals(-7.75, first[0].lat, 0.0)
            assertEquals(1, first[0].hours.size)
            assertEquals(1, first[0].tickets.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun places_networkFailure_keepsCacheAndCheckDate() = runTest {
        val dao = FakePlaceDao()
        val cached = payload()
        dao.replaceAll(cached.places.mapNotNull { it.toEntity() }, cached.toFactEntities())
        val remote = FakeRemote().apply { fail = true }

        placeRepo(remote, dao).places().test {
            val list = awaitItem()
            assertEquals("Candi Prambanan", list[0].name)
            assertEquals(LocalDate.of(2026, 11, 20), list[0].hours[0].checkedAt)
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, remote.fetchCount)
        assertEquals(1, dao.places.value.size)
        assertEquals(1 + 1, dao.facts.value.size)
    }

    @Test
    fun places_coldStartOffline_emitsEmptyList() = runTest {
        placeRepo(FakeRemote().apply { fail = true }).places().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun places_missingCheckDate_isCheckingState() = runTest {
        val remote = FakeRemote().apply {
            places = PlacesPayload(listOf(placeDto()), listOf(hoursDto(checkedAt = null)), emptyList(), listOf(ticketDto(checkedAt = null)))
        }
        val p = placeRepo(remote).places().first().single()
        assertEquals(FactDisplay.Checking, p.hours[0].display())
        assertEquals(FactDisplay.Checking, p.tickets[0].display())
    }

    @Test
    fun place_searchAndCategory() = runTest {
        val remote = FakeRemote().apply {
            places = PlacesPayload(
                listOf(placeDto("p1", "Candi Prambanan"), placeDto("p2", "Museum Batik").copy(category = "museum")),
                emptyList(), emptyList(), emptyList(),
            )
        }
        val repo = placeRepo(remote)
        assertEquals("p2", repo.place("p2").first()?.id)
        assertNull(repo.place("nope").first())
        assertEquals(listOf("p2"), repo.placesByCategory(PlaceCategory.Museum).first().map { it.id })
        assertEquals(listOf("p1"), repo.search("prambanan").first().map { it.id })
        assertTrue(repo.search("  ").first().isEmpty())
    }

    // ---------- events ----------

    private fun eventRepo(remote: FakeRemote, dao: FakeEventDao = FakeEventDao(), regions: StubRegions = StubRegions()) =
        SupabaseEventRepository(remote, dao, regions, testGate())

    @Test
    fun places_scopedToSelectedRegion_butPlaceByIdResolvesAcrossRegions() = runTest {
        val remote = FakeRemote().apply {
            places = PlacesPayload(
                listOf(
                    placeDto("diy1", "Candi Prambanan", 1, "DIY"),
                    placeDto("jkt1", "Museum Nasional", 2, "JKT"),
                    placeDto("none", "Tanpa Wilayah", 3, null),
                ),
                emptyList(), emptyList(), emptyList(),
            )
        }
        val regions = StubRegions(null)
        val repo = placeRepo(remote, regions = regions)
        // No selection: everything (Pilih daerah path).
        assertEquals(listOf("diy1", "jkt1", "none"), repo.places().first().map { it.id })

        regions.selected.value = "DIY"
        assertEquals(listOf("diy1"), repo.places().first().map { it.id })
        assertEquals("DIY", repo.places().first().single().regionId)
        assertEquals("jkt1", repo.place("jkt1").first()?.id)
        assertTrue(repo.search("museum").first().isEmpty())
        assertEquals(listOf("diy1"), repo.search("candi").first().map { it.id })
        assertTrue(repo.placesByCategory(PlaceCategory.Candi).first().all { it.regionId == "DIY" })
    }

    @Test
    fun places_regionSwitch_reEmits() = runTest {
        val remote = FakeRemote().apply {
            places = PlacesPayload(
                listOf(placeDto("diy1", "A", 1, "DIY"), placeDto("jkt1", "B", 2, "JKT")),
                emptyList(), emptyList(), emptyList(),
            )
        }
        val regions = StubRegions("DIY")
        placeRepo(remote, regions = regions).places().test {
            assertEquals(listOf("diy1"), awaitItem().map { it.id })
            regions.selected.value = "JKT"
            assertEquals(listOf("jkt1"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun events_scopedToHostPlaceRegion_andReEmitOnSwitch() = runTest {
        val remote = FakeRemote().apply {
            events = EventsPayload(
                listOf(
                    eventDto("e-diy", placeId = "diy1", region = "DIY"),
                    eventDto("e-jkt", placeId = "jkt1", region = "JKT"),
                ),
                emptyList(),
            )
        }
        val regions = StubRegions(null)
        val repo = eventRepo(remote, regions = regions)
        val from = LocalDate.of(2026, 11, 20)
        val to = from.plusDays(6)
        assertEquals(setOf("e-diy", "e-jkt"), repo.eventsBetween(from, to).first().map { it.event.id }.toSet())

        regions.selected.value = "DIY"
        repo.eventsBetween(from, to).test {
            assertEquals(setOf("e-diy"), awaitItem().map { it.event.id }.toSet())
            regions.selected.value = "JKT"
            assertEquals(setOf("e-jkt"), awaitItem().map { it.event.id }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
        // Lookups by id are not scoped.
        assertEquals("e-diy", repo.event("e-diy").first()?.id)
    }

    @Test
    fun events_weeklyExpandsOnIsoWeekday_andRespectsClosures() = runTest {
        val remote = FakeRemote().apply {
            events = EventsPayload(
                events = listOf(eventDto(weekday = 5)),
                closures = listOf(closureDto("p1", "2026-11-27", "2026-11-27")),
            )
        }
        val repo = eventRepo(remote)
        val friday = LocalDate.of(2026, 11, 20)
        assertEquals(listOf(friday), repo.eventsOn(friday).first().map { it.date })
        assertTrue(repo.eventsOn(LocalDate.of(2026, 11, 21)).first().isEmpty())
        // 27 Nov is cancelled by a closure of the place.
        assertEquals(listOf(friday), repo.eventsBetween(friday, LocalDate.of(2026, 11, 28)).first().map { it.date })
        assertEquals("e1", repo.event("e1").first()?.id)
        assertNull(repo.event("nope").first())
    }

    @Test
    fun eventsAt_returnsOnlyEventsOfThatPlace() = runTest {
        val remote = FakeRemote().apply {
            events = EventsPayload(events = listOf(eventDto(weekday = 5)), closures = emptyList())
        }
        val repo = eventRepo(remote)
        assertEquals(listOf("e1"), repo.eventsAt("p1").first().map { it.id })
        assertTrue(repo.eventsAt("other").first().isEmpty())
    }

    @Test
    fun events_onceAndDaily_orderedByDateThenStart() = runTest {
        val remote = FakeRemote().apply {
            events = EventsPayload(
                listOf(
                    eventDto("late", "daily", null, start = "20:00:00"),
                    eventDto("early", "daily", null, start = "10:00:00"),
                    eventDto("once", "once", null, date = "2026-11-21", start = "09:00:00"),
                ),
                emptyList(),
            )
        }
        val result = eventRepo(remote).eventsBetween(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 21)).first()
        assertEquals(
            listOf("20:early", "20:late", "21:once", "21:early", "21:late"),
            result.map { "${it.date.dayOfMonth}:${it.event.id}" },
        )
    }

    @Test
    fun events_cacheFirstThenNetwork_andOfflineKeepsCache() = runTest {
        val dao = FakeEventDao()
        dao.replaceAll(listOf(eventDto("old").toEntity()), emptyList())
        val remote = FakeRemote().apply { events = EventsPayload(listOf(eventDto("new")), emptyList()) }
        val friday = LocalDate.of(2026, 11, 20)

        eventRepo(remote, dao).eventsOn(friday).test {
            assertEquals(listOf("old"), awaitItem().map { it.event.id })
            assertEquals(listOf("new"), awaitItem().map { it.event.id })
            cancelAndIgnoreRemainingEvents()
        }

        val offline = FakeRemote().apply { fail = true }
        eventRepo(offline, dao).eventsOn(friday).test {
            assertEquals(listOf("new"), awaitItem().map { it.event.id })
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun events_cachedClosureRowsSurviveOffline() = runTest {
        val dao = FakeEventDao()
        dao.replaceAll(listOf(eventDto().toEntity()), listOf(closureDto("p1", "2026-11-27", "2026-11-27").toEventClosureEntity(0)))
        val repo = eventRepo(FakeRemote().apply { fail = true }, dao)
        assertTrue(repo.eventsOn(LocalDate.of(2026, 11, 27)).first().isEmpty())
    }

    // ---------- regions ----------

    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = CoroutineScope(backgroundScope.coroutineContext)) {
            File(tmp.newFolder(), "prefs.preferences_pb")
        }

    @Test
    fun regions_orderedAndOnlyAvailableSelectable() = runTest {
        val remote = FakeRemote().apply {
            regions = listOf(regionDto("BALI", "upcoming", 3), regionDto("DIY", "available", 1))
        }
        val repo = SupabaseRegionRepository(remote, FakeRegionDao(), dataStore(), testGate())
        repo.regions().test {
            val list = awaitItem()
            assertEquals(listOf("DIY", "BALI"), list.map { it.id }.sortedBy { if (it == "DIY") 0 else 1 })
            assertTrue(list.first { it.id == "DIY" }.isAvailable)
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(repo.selectedRegionId.first())
        repo.select("BALI")
        assertNull(repo.selectedRegionId.first())
        repo.select("tidak-ada")
        assertNull(repo.selectedRegionId.first())
        repo.select("DIY")
        assertEquals("DIY", repo.selectedRegionId.first())
    }

    @Test
    fun regions_selectionPersistsAcrossRepositoryInstances() = runTest {
        val store = dataStore()
        val remote = FakeRemote().apply { regions = listOf(regionDto("DIY", "available", 1)) }
        val dao = FakeRegionDao()
        SupabaseRegionRepository(remote, dao, store, testGate()).select("DIY")
        val second = SupabaseRegionRepository(FakeRemote().apply { fail = true }, dao, store, testGate())
        assertEquals("DIY", second.selectedRegionId.first())
    }

    @Test
    fun regions_offlineColdStart_cannotSelect_andEmitsEmpty() = runTest {
        val repo = SupabaseRegionRepository(FakeRemote().apply { fail = true }, FakeRegionDao(), dataStore(), testGate())
        assertTrue(repo.regions().first().isEmpty())
        repo.select("DIY")
        assertNull(repo.selectedRegionId.first())
    }

    // ---------- reports ----------

    private class StubSession(state: SessionState) : SessionRepository {
        override val sessionState: Flow<SessionState> = MutableStateFlow(state)
        override suspend fun signInWithGoogle(idToken: String, nonce: String) = AuthResult.Failed
        override suspend fun signOut() = Unit
        override suspend fun updateDisplayName(name: String) = AuthResult.Success
    }

    private val draft = ReportDraft("p1", ReportReason.PlaceClosed, "  Tutup  ")

    @Test
    fun report_guestNeedsLogin_andNothingSent() = runTest {
        val remote = FakeRemote()
        assertEquals(ReportResult.NeedsLogin, SupabaseReportRepository(StubSession(SessionState.Guest), remote).submit(draft))
        assertTrue(remote.reports.isEmpty())
    }

    @Test
    fun report_signedIn_insertsOwnRow() = runTest {
        val remote = FakeRemote()
        val session = StubSession(SessionState.SignedIn(SessionUser("u1", null, null)))
        assertEquals(ReportResult.Success, SupabaseReportRepository(session, remote).submit(draft))
        val row = remote.reports.single()
        assertEquals("u1", row.userId)
        assertEquals("p1", row.placeId)
        assertEquals("place_closed", row.reason)
        assertEquals("Tutup", row.details)
    }

    @Test
    fun report_failureAndLongDetails() = runTest {
        val session = StubSession(SessionState.SignedIn(SessionUser("u1", null, null)))
        val remote = FakeRemote()
        SupabaseReportRepository(session, remote).submit(draft.copy(details = "x".repeat(1500)))
        assertEquals(1000, remote.reports.single().details!!.length)
        remote.fail = true
        assertEquals(ReportResult.Failed, SupabaseReportRepository(session, remote).submit(draft))
        assertNotNull(remote.reports.single())
    }
}
