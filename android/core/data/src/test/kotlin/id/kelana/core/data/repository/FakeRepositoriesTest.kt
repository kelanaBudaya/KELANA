package id.kelana.core.data.repository

import id.kelana.core.model.AuthResult
import id.kelana.core.model.FactDisplay
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.ReportDraft
import id.kelana.core.model.ReportReason
import id.kelana.core.model.ReportResult
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.SessionUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class FakeRepositoriesTest {
    private val places = FakePlaceRepository()
    private val events = FakeEventRepository()

    @Test
    fun hasTenPlacesInPassportOrder() = runTest {
        val all = places.places().first()
        assertEquals(10, all.size)
        assertEquals((1..10).map { "YK-%02d".format(it) }, all.map { it.capCode })
    }

    @Test
    fun placeById_returnsPlaceOrNull() = runTest {
        assertEquals("Candi Prambanan", places.place("candi-prambanan").first()?.name)
        assertNull(places.place("tidak-ada").first())
    }

    @Test
    fun placesByCategory_filters() = runTest {
        assertEquals(
            setOf("Museum Sonobudoyo", "Museum Kereta Wahanarata", "Museum Benteng Vredeburg", "Museum Batik Yogyakarta"),
            places.placesByCategory(PlaceCategory.Museum).first().map { it.name }.toSet(),
        )
        assertEquals(listOf("candi-prambanan"), places.placesByCategory(PlaceCategory.Candi).first().map { it.id })
        assertTrue(places.placesByCategory(PlaceCategory.Pentas).first().isEmpty())
    }

    @Test
    fun search_isCaseInsensitiveAndMatchesNameOrDescription() = runTest {
        assertEquals(listOf("museum-sonobudoyo"), places.search("SONOBUDOYO").first().map { it.id })
        // "wayang" appears only in the description of Sonobudoyo.
        assertEquals(listOf("museum-sonobudoyo"), places.search("wayang").first().map { it.id })
        assertTrue(places.search("   ").first().isEmpty())
        assertTrue(places.search("zzz").first().isEmpty())
    }

    @Test
    fun everyOfficialFactHasASource_exceptTheUnknownPrice() = runTest {
        places.places().first().forEach { place ->
            (place.hours + place.tickets).forEach { fact ->
                if (fact.checkedAt != null) assertNotNull("${place.id} fact without source", fact.sourceUrl)
            }
        }
    }

    @Test
    fun pakualamanPrice_hasNoCheckDateAndDisplaysAsChecking() = runTest {
        val price = places.place("puro-pakualaman").first()!!.tickets.single()
        assertNull(price.checkedAt)
        assertEquals(FactDisplay.Checking, price.display())
    }

    @Test
    fun sonobudoyoHours_closedOnMondayOnly() = runTest {
        val hours = places.place("museum-sonobudoyo").first()!!.hours.map { it.value }
        assertEquals(7, hours.size)
        assertEquals(listOf(DayOfWeek.MONDAY), hours.filter { it.isClosed }.map { it.weekday })
    }

    @Test
    fun eventsOnFriday_isRoroJonggrang() = runTest {
        // 2026-11-20 is a Friday.
        val result = events.eventsOn(LocalDate.of(2026, 11, 20)).first()
        assertEquals(listOf("Sendratari Roro Jonggrang"), result.map { it.event.title })
    }

    @Test
    fun eventsBetween_coversAWeekSortedByDate() = runTest {
        val result = events.eventsBetween(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 22)).first()
        assertEquals(5, result.size)
        assertEquals(result.map { it.date }.sorted(), result.map { it.date })
    }

    @Test
    fun eventsAt_filtersByPlace() = runTest {
        val result = events.eventsAt("candi-prambanan").first()
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.placeId == "candi-prambanan" })
    }

    @Test
    fun eventsOnMonday_isEmpty() = runTest {
        assertTrue(events.eventsOn(LocalDate.of(2026, 11, 23)).first().isEmpty())
    }

    @Test
    fun eventById() = runTest {
        assertNotNull(events.event("ramayana-selasa").first())
        assertNull(events.event("x").first())
    }

    @Test
    fun regions_onlyDiyIsAvailable_andOnlyAvailableCanBeSelected() = runTest {
        val repo = FakeRegionRepository()
        val regions = repo.regions().first()
        assertEquals(10, regions.size)
        assertEquals(listOf("DIY"), regions.filter { it.isAvailable }.map { it.id })
        repo.select("BALI")
        assertNull(repo.selectedRegionId.first())
        repo.select("DIY")
        assertEquals("DIY", repo.selectedRegionId.first())
    }

    @Test
    fun selectedRegion_followsSelection() = runTest {
        val repo = FakeRegionRepository()
        assertNull(repo.selectedRegion().first())
        repo.select("DIY")
        assertEquals("Jogja", repo.selectedRegion().first()?.shortName)
    }

    private class FakeSession(initial: SessionState) : SessionRepository {
        val state = MutableStateFlow(initial)
        override val sessionState: Flow<SessionState> = state
        override suspend fun signInWithGoogle(idToken: String, nonce: String) = AuthResult.Success
        override suspend fun signOut() = Unit
        override suspend fun updateDisplayName(name: String) = AuthResult.Success
    }

    private val draft = ReportDraft("candi-prambanan", ReportReason.PlaceClosed)

    @Test
    fun report_guestNeedsLogin() = runTest {
        assertEquals(ReportResult.NeedsLogin, FakeReportRepository(FakeSession(SessionState.Guest)).submit(draft))
    }

    @Test
    fun report_signedInSucceeds() = runTest {
        val user = SessionUser("u1", null, null)
        assertEquals(ReportResult.Success, FakeReportRepository(FakeSession(SessionState.SignedIn(user))).submit(draft))
    }
}
