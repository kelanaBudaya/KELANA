package id.kelana.feature.onboarding

import app.cash.turbine.test
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.Region
import id.kelana.core.model.RegionRepository
import id.kelana.core.model.RegionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

private class TestRegions : RegionRepository {
    val all = listOf(
        Region("DIY", "DI Yogyakarta", RegionStatus.Available, 1),
        Region("JTG", "Jawa Tengah", RegionStatus.Upcoming, 2),
        Region("BALI", "Bali", RegionStatus.Upcoming, 3),
        Region("PAPUA", "Papua", RegionStatus.NotOpen, 4),
    )
    private val selected = MutableStateFlow<String?>(null)
    override fun regions(): Flow<List<Region>> = flowOf(all)
    override val selectedRegionId: Flow<String?> = selected
    override suspend fun select(regionId: String) {
        if (all.any { it.id == regionId && it.isAvailable }) selected.value = regionId
    }
}

private class TestPlaces : PlaceRepository {
    private val list = (1..6).map {
        Place(
            id = "p$it", slug = "p$it", name = "Tempat $it", category = PlaceCategory.Museum, capCode = "YK-0$it",
            lat = 0.0, lng = 0.0, address = "", description = "", coverKey = "",
        )
    }
    override fun places(): Flow<List<Place>> = flowOf(list)
    override fun place(id: String): Flow<Place?> = flowOf(list.firstOrNull { it.id == id })
    override fun placesByCategory(category: PlaceCategory): Flow<List<Place>> = flowOf(list)
    override fun search(query: String): Flow<List<Place>> = flowOf(emptyList())
}

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingTest {
    private lateinit var regions: TestRegions

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        regions = TestRegions()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    // ----- Intro page state -----

    @Test
    fun mulaiPageIsNotAnIntroPage() {
        val state = IntroUiState.forPage(MulaiPage)
        assertTrue(state.isMulai)
        assertNull(state.intro)
    }

    @Test
    fun introPagesAdvanceOneToThreeThenStop() {
        assertEquals(2, IntroUiState.forPage(1).nextPage)
        assertEquals(3, IntroUiState.forPage(2).nextPage)
        assertNull(IntroUiState.forPage(3).nextPage)
        assertTrue(IntroUiState.forPage(3).intro!!.isLast)
    }

    @Test
    fun indicatorStepMatchesPageAndHasThreeSteps() {
        val states = (1..3).map { IntroUiState.forPage(it) }
        assertEquals(listOf(0, 1, 2), states.map { it.intro!!.step })
        assertTrue(states.all { it.stepCount == 3 })
        assertFalse(IntroUiState.forPage(1).isMulai)
    }

    @Test
    fun outOfRangePageHasNoIntro() {
        assertNull(IntroUiState.forPage(9).intro)
    }

    // ----- "Mulai dari Jogja" / Lewati / completing -----

    @Test
    fun startFromJogjaSelectsDiyAndFinishes() = runTest {
        val vm = IntroViewModel(regions)
        vm.finished.test {
            assertFalse(awaitItem())
            vm.startFromJogja()
            assertTrue(awaitItem())
        }
        assertEquals("DIY", regions.selectedRegionId.let { (it as MutableStateFlow).value })
    }

    // ----- Pilih daerah -----

    @Test
    fun loadsRegionsAndOpensFirstAvailableCallout() = runTest {
        val vm = PilihDaerahViewModel(regions, TestPlaces())
        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals(4, s.regions.size)
        assertEquals("DIY", s.selectedRegionId)
        assertEquals(2, s.upcomingCount)
        assertEquals(1, s.notOpenCount)
        assertEquals(4, s.placeNames.size)
        assertEquals(2, s.moreCount)
    }

    @Test
    fun pinTapShowsThatRegionsCallout() = runTest {
        val vm = PilihDaerahViewModel(regions, TestPlaces())
        vm.onPinTapped("JTG")
        assertEquals("JTG", vm.state.value.selectedRegion?.id)
        vm.onPinTapped("tidak-ada")
        assertEquals("JTG", vm.state.value.selectedRegionId)
    }

    @Test
    fun availableRegionCanBeChosenAndFinishes() = runTest {
        val vm = PilihDaerahViewModel(regions, TestPlaces())
        vm.choose("DIY")
        assertTrue(vm.state.value.finished)
        assertEquals("DIY", (regions.selectedRegionId as MutableStateFlow).value)
        vm.onFinishedHandled()
        assertFalse(vm.state.value.finished)
    }

    @Test
    fun upcomingAndNotOpenRegionsCannotBeChosen() = runTest {
        val vm = PilihDaerahViewModel(regions, TestPlaces())
        vm.choose("JTG")
        vm.choose("PAPUA")
        vm.choose("tidak-ada")
        assertFalse(vm.state.value.finished)
        assertNull((regions.selectedRegionId as MutableStateFlow).value)
    }
}
