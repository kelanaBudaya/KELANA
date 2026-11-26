package id.kelana.feature.paspor.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.kelana.core.data.repository.FakePlaceRepository
import id.kelana.core.data.repository.FakeRegionRepository
import id.kelana.core.model.Stamp
import id.kelana.feature.paspor.FakeRegions
import id.kelana.feature.paspor.ListPlaces
import id.kelana.feature.paspor.ScriptedStampRepository
import id.kelana.feature.paspor.testPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CapBerhasilViewModelTest {
    private val t0 = Instant.parse("2026-11-20T03:00:00Z")

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(placeId: String, stamps: ScriptedStampRepository) =
        CapBerhasilViewModel(SavedStateHandle(mapOf("placeId" to placeId)), FakePlaceRepository(), stamps, FakeRegionRepository())

    @Test fun counts_come_from_the_confirmed_stamps_and_the_passport_places() = runTest {
        val stamps = ScriptedStampRepository(
            listOf(Stamp("keraton-yogyakarta", t0), Stamp("tamansari", t0.plusSeconds(60)), Stamp("museum-sonobudoyo", t0.plusSeconds(120))),
        )
        vm("museum-sonobudoyo", stamps).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals("Museum Sonobudoyo", s.place?.name)
            assertEquals(3, s.collected)
            assertEquals(10, s.total)
            assertEquals(7, s.remaining)
            assertEquals(t0.plusSeconds(120), s.stampedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun without_the_stamp_in_the_list_no_date_is_shown_and_counts_stay_real() = runTest {
        vm("museum-sonobudoyo", ScriptedStampRepository()).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals(0, s.collected)
            assertEquals(10, s.remaining)
            assertNull(s.stampedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun an_unknown_place_has_no_place_and_no_invented_numbers() = runTest {
        vm("nowhere", ScriptedStampRepository(listOf(Stamp("tamansari", t0)))).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertNull(s.place)
            assertNull(s.stampedAt)
            assertEquals(1, s.collected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun all_stamps_collected_leaves_nothing_remaining() = runTest {
        val ids = listOf(
            "keraton-yogyakarta", "museum-sonobudoyo", "tamansari", "museum-kereta-wahanarata", "museum-benteng-vredeburg",
            "puro-pakualaman", "museum-batik-yogyakarta", "tugu-yogyakarta", "kotagede", "candi-prambanan",
        )
        vm("candi-prambanan", ScriptedStampRepository(ids.map { Stamp(it, t0) })).uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals(10, s.collected)
            assertEquals(0, s.remaining)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun region_texts_follow_the_stamped_place_not_the_selected_region() = runTest {
        val jtm = testPlace("bromo", "JT-01").copy(regionId = "JTM")
        val diy = testPlace("sono", "YK-02").copy(regionId = "DIY")
        fun vmFor(placeId: String, selected: String) = CapBerhasilViewModel(
            SavedStateHandle(mapOf("placeId" to placeId)), ListPlaces(listOf(jtm, diy)), ScriptedStampRepository(), FakeRegions(selected),
        )
        vmFor("bromo", "DIY").uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals("JTM", s.regionCode)
            assertEquals("Jatim", s.regionShortName)
            cancelAndIgnoreRemainingEvents()
        }
        vmFor("sono", "JTM").uiState.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals("DIY", s.regionCode)
            assertEquals("Jogja", s.regionShortName)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
