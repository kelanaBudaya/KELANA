package id.kelana.feature.paspor.ui

import app.cash.turbine.test
import id.kelana.core.data.repository.FakeRegionRepository
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.SessionUser
import id.kelana.core.model.Stamp
import id.kelana.core.model.StampRejectReason
import id.kelana.core.model.StampResult
import id.kelana.feature.paspor.FakeSession
import id.kelana.feature.paspor.ScriptedStampRepository
import id.kelana.feature.paspor.domain.StampFailure
import id.kelana.feature.paspor.domain.StampOutcome
import id.kelana.feature.paspor.domain.buildPassport
import id.kelana.feature.paspor.domain.collectedCount
import id.kelana.feature.paspor.domain.toOutcome
import id.kelana.feature.paspor.testPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PasporViewModelTest {
    private val t0 = Instant.parse("2026-11-20T03:00:00Z")
    private val places = listOf(
        testPlace("keraton", "YK-01"),
        testPlace("sono", "YK-02"),
        testPlace("tamansari", "YK-03"),
        testPlace("prambanan", "YK-04", PlaceCategory.Candi),
    )

    private class Places(private val list: List<id.kelana.core.model.Place>) : PlaceRepository {
        override fun places(): Flow<List<id.kelana.core.model.Place>> = flowOf(list)
        override fun place(id: String): Flow<id.kelana.core.model.Place?> = flowOf(list.firstOrNull { it.id == id })
        override fun placesByCategory(category: PlaceCategory): Flow<List<id.kelana.core.model.Place>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<id.kelana.core.model.Place>> = flowOf(emptyList())
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private val signedIn = SessionState.SignedIn(SessionUser("u1", "mira.hartono@example.com", "Mira Hartono"))

    private fun vm(stamps: ScriptedStampRepository, session: SessionState = signedIn) =
        PasporViewModel(Places(places), stamps, FakeRegionRepository(), FakeSession(session))

    @Test fun counts_are_zero_when_there_is_no_stamp() = runTest {
        vm(ScriptedStampRepository()).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals(0, s.collected)
            assertEquals(4, s.total)
            assertEquals(4, s.remaining)
            assertTrue(s.items.none { it.isCollected })
            assertNull(s.latestStamped)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun items_follow_passport_order_and_collected_ones_have_a_date() = runTest {
        val stamps = ScriptedStampRepository(listOf(Stamp("sono", t0), Stamp("prambanan", t0.plusSeconds(60))))
        vm(stamps).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals(listOf("keraton", "sono", "tamansari", "prambanan"), s.items.map { it.place.id })
            assertEquals(listOf(false, true, false, true), s.items.map { it.isCollected })
            assertEquals(t0, s.items[1].stampedAt)
            assertEquals(2, s.collected)
            assertEquals(2, s.remaining)
            assertEquals("prambanan", s.latestStamped?.place?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun a_stamp_for_an_unknown_place_is_not_counted_and_ids_compare_ignoring_case() {
        val passport = buildPassport(places, listOf(Stamp("SONO", t0), Stamp("somewhere-else", t0)))
        assertEquals(1, passport.collectedCount())
        assertTrue(passport.first { it.place.id == "sono" }.isCollected)
    }

    @Test fun a_place_stamped_twice_is_counted_once_with_the_first_date() {
        val passport = buildPassport(places, listOf(Stamp("sono", t0.plusSeconds(90)), Stamp("sono", t0)))
        assertEquals(1, passport.collectedCount())
        assertEquals(t0, passport.first { it.place.id == "sono" }.stampedAt)
    }

    @Test fun a_guest_sees_zero_stamps_and_is_flagged_as_guest() = runTest {
        vm(ScriptedStampRepository(), SessionState.Guest).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertTrue(s.isGuest)
            assertEquals(0, s.collected)
            assertEquals("", s.initials)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun a_signed_in_user_has_initials_and_is_not_a_guest() = runTest {
        vm(ScriptedStampRepository()).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertFalse(s.isGuest)
            assertEquals("MH", s.initials)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun the_session_loading_state_is_not_shown_as_a_guest() = runTest {
        vm(ScriptedStampRepository(), SessionState.Loading).uiState.test {
            val s = awaitItem()
            assertTrue(s.isLoading)
            assertFalse(s.isGuest)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun a_queued_request_is_not_a_stamp_in_the_passport() = runTest {
        val stamps = ScriptedStampRepository(nextResult = StampResult.Queued)
        stamps.requestStamp(
            id.kelana.core.model.StampRequest("sono", -7.8, 110.3, 10.0, false, t0),
        )
        vm(stamps).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals(0, s.collected)
            assertTrue(s.items.none { it.isCollected })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun the_passport_updates_only_when_the_server_confirmed_a_stamp() = runTest {
        val stamps = ScriptedStampRepository()
        vm(stamps).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals(0, s.collected)
            stamps.stampsState.value = listOf(Stamp("keraton", t0))
            assertEquals(1, awaitItem().collected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ----- outcome mapping

    @Test fun outcome_mapping_covers_every_result() {
        assertEquals(StampOutcome.Confirmed, StampResult.Stamped.toOutcome())
        assertEquals(StampOutcome.Confirmed, StampResult.AlreadyStamped.toOutcome())
        assertEquals(StampOutcome.Waiting, StampResult.Queued.toOutcome())
        assertEquals(StampOutcome.Waiting, StampResult.NetworkError.toOutcome())
        assertEquals(StampOutcome.TooFar(250), StampResult.Rejected(StampRejectReason.TooFar, 250).toOutcome())
        assertEquals(StampOutcome.Failed(StampFailure.NeedsLogin), StampResult.NeedsLogin.toOutcome())
        assertEquals(StampOutcome.Failed(StampFailure.NotConfigured), StampResult.NotConfigured.toOutcome())
        assertEquals(StampOutcome.Failed(StampFailure.Expired), StampResult.Expired.toOutcome())
    }
}
