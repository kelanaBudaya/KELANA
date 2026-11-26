package id.kelana.feature.jelajah

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import id.kelana.core.data.repository.FakeEventRepository
import id.kelana.core.data.repository.FakePlaceRepository
import id.kelana.core.data.repository.FakeRegionRepository
import id.kelana.core.data.stamp.FakeStampRepository
import id.kelana.core.model.Stamp
import id.kelana.core.model.PlaceCategory
import id.kelana.core.navigation.KategoriRoute
import id.kelana.core.navigation.PasporRoute
import id.kelana.core.navigation.PetaRoute
import id.kelana.core.navigation.SemuaTempatRoute
import id.kelana.core.navigation.TiketRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** Sample data: places checked 2026-11-20; 2026-11-20 is a Friday (Roro Jonggrang 19.30), 2026-11-23 a Monday. */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTests {
    private val friday = LocalDate.of(2026, 11, 20)
    private val monday = LocalDate.of(2026, 11, 23)
    private val places = FakePlaceRepository()
    private val events = FakeEventRepository()
    private val regions = FakeRegionRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    // Home

    @Test fun homeShowsFeaturedPlacesAndTonightEvent() = runTest {
        val vm = JelajahViewModel(places, events, regions, FakeStampRepository()) { LocalDateTime.of(friday, java.time.LocalTime.of(18, 0)) }
        vm.state.test {
            val s = awaitUntil { !it.isLoading }
            assertEquals(10, s.featuredPlaces.size)
            assertEquals("roro-jonggrang-jumat", s.tonightEvent?.event?.id)
            assertEquals(friday, s.date)
            // No stamp: the passport card shows a real 0 of the passport places, never an invented number.
            assertEquals(0, s.stampCount)
            assertEquals(10, s.stampTotal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun homeCountsOnlyConfirmedStampsAtPassportPlaces() = runTest {
        val at = java.time.Instant.parse("2026-11-20T03:00:00Z")
        val stamps = FakeStampRepository(
            initialStamps = listOf(
                Stamp("museum-sonobudoyo", at),
                Stamp("MUSEUM-SONOBUDOYO", at), // same place, counted once
                Stamp("tamansari", at),
                Stamp("somewhere-else", at), // not a passport place
            ),
        )
        val vm = JelajahViewModel(places, events, regions, stamps) { LocalDateTime.of(friday, java.time.LocalTime.of(18, 0)) }
        vm.state.test {
            val s = awaitUntil { !it.isLoading }
            assertEquals(2, s.stampCount)
            assertEquals(10, s.stampTotal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun weeklyProgressCountsOnlyStampsOfThisWeek() = runTest {
        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-11-20T11:00:00Z"), java.time.ZoneOffset.UTC)
        val stamps = FakeStampRepository(
            initialStamps = listOf(
                Stamp("museum-sonobudoyo", java.time.Instant.parse("2026-11-20T03:00:00Z")), // Friday: this week
                Stamp("tamansari", java.time.Instant.parse("2026-11-22T17:00:00Z")), // next Monday 00:00 Jakarta: not yet
                Stamp("kotagede", java.time.Instant.parse("2026-11-15T03:00:00Z")), // last week
            ),
        )
        val vm = JelajahViewModel(places, events, regions, stamps, clock) { LocalDateTime.of(friday, java.time.LocalTime.of(18, 0)) }
        vm.state.test {
            val s = awaitUntil { !it.isLoading }
            assertEquals(WeeklyProgress(done = 1, target = WEEKLY_STAMP_TARGET), s.weekly)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun weeklyProgressIsZeroForGuestWithoutStamps() = runTest {
        val vm = JelajahViewModel(places, events, regions, FakeStampRepository()) { LocalDateTime.of(friday, java.time.LocalTime.of(18, 0)) }
        vm.state.test {
            assertEquals(WeeklyProgress(0, WEEKLY_STAMP_TARGET), awaitUntil { !it.isLoading }.weekly)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun homeHidesBannerWhenEventAlreadyStarted() = runTest {
        val vm = JelajahViewModel(places, events, regions, FakeStampRepository()) { LocalDateTime.of(friday, java.time.LocalTime.of(21, 0)) }
        vm.state.test {
            assertNull(awaitUntil { !it.isLoading }.tonightEvent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun homeHidesBannerWhenNoEventToday() = runTest {
        // Wednesday has no sample event.
        val wednesday = LocalDate.of(2026, 11, 25)
        val vm = JelajahViewModel(places, events, regions, FakeStampRepository()) { LocalDateTime.of(wednesday, java.time.LocalTime.of(8, 0)) }
        vm.state.test {
            val s = awaitUntil { !it.isLoading }
            assertNull(s.tonightEvent)
            assertTrue(s.featuredPlaces.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun tilesRouteByRouteObject() {
        assertEquals(KategoriRoute("museum"), tileRoute(HomeTile.Museum))
        assertEquals(KategoriRoute("pentas"), tileRoute(HomeTile.Pentas))
        assertEquals(SemuaTempatRoute, tileRoute(HomeTile.Semua))
        assertEquals(PetaRoute, tileRoute(HomeTile.Peta))
        assertEquals(TiketRoute, tileRoute(HomeTile.Tiket))
    }

    @Test fun everyTileHasADestinationAndPassportGoesToPaspor() {
        HomeTile.entries.forEach { assertNotNull(it.name, tileRoute(it)) }
        assertEquals(PasporRoute, passportRoute)
    }

    // Cari

    @Test fun searchFindsPlacesAndEvents() = runTest {
        val vm = CariViewModel(places, events) { friday }
        vm.state.test {
            assertFalse(awaitItem().hasQuery)
            vm.onQueryChange("wayang")
            val s = awaitUntil { it.hasQuery && it.places.isNotEmpty() }
            assertTrue(s.places.any { it.id == "museum-sonobudoyo" })
            assertTrue(s.events.any { it.event.id == "wayang-wong-minggu" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun searchWithoutMatchIsEmptyResult() = runTest {
        val vm = CariViewModel(places, events) { friday }
        vm.state.test {
            awaitItem()
            vm.onQueryChange("zzzxxx")
            val s = awaitUntil { it.hasQuery }
            assertTrue(s.isEmptyResult)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun searchFilterTempatDropsEvents() = runTest {
        val vm = CariViewModel(places, events) { friday }
        vm.state.test {
            awaitItem()
            vm.onQueryChange("wayang")
            vm.onFilterChange(CariFilter.Tempat)
            val s = awaitUntil { it.filter == CariFilter.Tempat && it.hasQuery }
            assertTrue(s.events.isEmpty())
            assertFalse(s.places.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun searchFilterPentasDropsPlaces() = runTest {
        val vm = CariViewModel(places, events) { friday }
        vm.state.test {
            awaitItem()
            vm.onQueryChange("wayang")
            vm.onFilterChange(CariFilter.Pentas)
            val s = awaitUntil { it.filter == CariFilter.Pentas && it.hasQuery }
            assertTrue(s.places.isEmpty())
            assertFalse(s.events.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Kategori

    @Test fun kategoriListsOnlyItsCategory() = runTest {
        val vm = KategoriViewModel(places, PlaceCategory.Museum) { friday }
        vm.state.test {
            val s = awaitUntil { !it.isLoading }
            assertEquals(4, s.places.size)
            assertTrue(s.places.all { it.category == PlaceCategory.Museum })
            assertFalse(s.isCategoryEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun kategoriWithoutPlacesIsEmpty() = runTest {
        val vm = KategoriViewModel(places, PlaceCategory.Pentas) { friday }
        vm.state.test {
            val s = awaitUntil { !it.isLoading }
            assertTrue(s.isCategoryEmpty)
            assertTrue(s.places.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun kategoriOpenTodayFilterUsesCheckedHours() = runTest {
        // Every sample museum is closed on Monday and open on Friday.
        val onFriday = KategoriViewModel(places, PlaceCategory.Museum) { friday }
        onFriday.onOpenTodayChange(true)
        onFriday.state.test {
            assertEquals(4, awaitUntil { it.openTodayOnly && !it.isLoading }.places.size)
            cancelAndIgnoreRemainingEvents()
        }
        val onMonday = KategoriViewModel(places, PlaceCategory.Museum) { monday }
        onMonday.state.test {
            awaitUntil { !it.isLoading }
            onMonday.onOpenTodayChange(true)
            val s = awaitUntil { it.openTodayOnly }
            assertTrue(s.isFilteredEmpty)
            assertEquals(4, s.totalCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun kategoriWithUnknownKeyIsEmpty() = runTest {
        val vm = KategoriViewModel(places, null) { friday }
        vm.state.test {
            val s = awaitUntil { !it.isLoading }
            assertNull(s.category)
            assertTrue(s.isCategoryEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Semua tempat

    @Test fun semuaListsEveryPlace() = runTest {
        val vm = SemuaTempatViewModel(places) { friday }
        vm.state.test {
            val s = awaitUntil { !it.isLoading }
            assertEquals(10, s.places.size)
            assertFalse(s.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun semuaFiltersByCategory() = runTest {
        val vm = SemuaTempatViewModel(places) { friday }
        vm.state.test {
            awaitUntil { !it.isLoading }
            vm.onCategoryChange(PlaceCategory.Candi)
            val s = awaitUntil { it.category == PlaceCategory.Candi }
            assertEquals(listOf("candi-prambanan"), s.places.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun semuaEmptyWhenCategoryHasNoPlace() = runTest {
        val vm = SemuaTempatViewModel(places) { friday }
        vm.state.test {
            awaitUntil { !it.isLoading }
            vm.onCategoryChange(PlaceCategory.Pentas)
            assertTrue(awaitUntil { it.category == PlaceCategory.Pentas }.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun semuaOpenTodayExcludesClosedPlaces() = runTest {
        val vm = SemuaTempatViewModel(places) { monday }
        vm.state.test {
            awaitUntil { !it.isLoading }
            vm.onOpenTodayChange(true)
            val s = awaitUntil { it.openTodayOnly }
            assertTrue(s.places.isNotEmpty())
            // Keraton, museums and others are closed on Monday; Prambanan and Tugu are open.
            assertTrue(s.places.map { it.id }.containsAll(listOf("candi-prambanan", "tugu-yogyakarta")))
            assertFalse(s.places.any { it.id == "museum-sonobudoyo" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Place filters

    @Test fun placeWithoutCheckedHoursIsNeverOpenAndHasNoBadgeDate() {
        val place = PreviewData.place("X", PlaceCategory.Museum, withCheck = false)
        assertFalse(place.isOpenOn(friday))
        assertNull(place.latestCheckedAt())
        assertNotNull(PreviewData.place("Y", PlaceCategory.Museum).latestCheckedAt())
    }
}
