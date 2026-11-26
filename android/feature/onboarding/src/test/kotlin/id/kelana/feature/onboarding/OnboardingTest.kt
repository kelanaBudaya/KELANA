package id.kelana.feature.onboarding

import app.cash.turbine.test
import id.kelana.core.model.AppLanguage
import id.kelana.core.model.AppLanguageRepository
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
        Region("JTM", "Jawa Timur", RegionStatus.Available, 5),
    )
    private val selected = MutableStateFlow<String?>(null)
    override fun regions(): Flow<List<Region>> = flowOf(all)
    override val selectedRegionId: Flow<String?> = selected
    override suspend fun select(regionId: String) {
        if (all.any { it.id == regionId && it.isAvailable }) selected.value = regionId
    }
}

private class TestLanguages(initial: AppLanguage = AppLanguage.ID) : AppLanguageRepository {
    val current = MutableStateFlow(initial)
    override val language: Flow<AppLanguage> = current
    override fun set(language: AppLanguage) {
        current.value = language
    }
}

private class TestPlaces : PlaceRepository {
    /** Six DIY places, three JTM places with other names. */
    private val list = (1..6).map { place("p$it", "Tempat $it", "DIY") } +
        (1..3).map { place("j$it", "Jatim $it", "JTM") }

    private companion object {
        fun place(id: String, name: String, regionId: String) = Place(
            id = id, slug = id, name = name, category = PlaceCategory.Museum, capCode = "X-$id",
            lat = 0.0, lng = 0.0, address = "", description = "", coverKey = "", regionId = regionId,
        )
    }
    override fun places(): Flow<List<Place>> = flowOf(list)
    override fun placesIn(regionId: String): Flow<List<Place>> = flowOf(list.filter { it.regionId == regionId })
    override fun place(id: String): Flow<Place?> = flowOf(list.firstOrNull { it.id == id })
    override fun placesByCategory(category: PlaceCategory): Flow<List<Place>> = flowOf(list)
    override fun search(query: String): Flow<List<Place>> = flowOf(emptyList())
}

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingTest {
    private lateinit var regions: TestRegions
    private lateinit var languages: TestLanguages

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        regions = TestRegions()
        languages = TestLanguages()
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
    fun languageStepHasItsOwnPageAndIsNotMulaiOrKenalan() {
        val state = IntroUiState.forPage(LanguagePage)
        assertTrue(state.isLanguage)
        assertFalse(state.isMulai)
        assertNull(state.intro)
        assertNull(state.nextPage)
        assertFalse(IntroUiState.forPage(MulaiPage).isLanguage)
        assertTrue((1..3).none { IntroUiState.forPage(it).isLanguage })
    }

    @Test
    fun stepOrderIsMulaiThenLanguageThenKenalan() {
        // The screen navigates Mulai -> LanguagePage -> OfficialInfo; Kenalan keeps pages 1..3 and indicator 1..3.
        assertEquals(listOf(0, 4, 1, 2, 3), listOf(MulaiPage, LanguagePage, IntroPage.OfficialInfo.page, 2, 3))
        assertEquals(3, IntroUiState.forPage(LanguagePage).stepCount)
        assertEquals(0, IntroPage.OfficialInfo.step)
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
        val vm = IntroViewModel(regions, languages)
        vm.finished.test {
            assertFalse(awaitItem())
            vm.startFromJogja()
            assertTrue(awaitItem())
        }
        assertEquals("DIY", regions.selectedRegionId.let { (it as MutableStateFlow).value })
    }

    @Test
    fun languageStartsFromShownLanguageAndFollowsSelection() = runTest {
        val vm = IntroViewModel(regions, languages)
        assertEquals(AppLanguage.ID, vm.language.value)
        vm.setLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, vm.language.value)
        assertEquals(AppLanguage.EN, languages.current.value)
        vm.setLanguage(AppLanguage.ID)
        assertEquals(AppLanguage.ID, vm.language.value)
    }

    @Test
    fun languageStartsFromTheRememberedChoice() {
        val vm = IntroViewModel(regions, TestLanguages(AppLanguage.EN))
        assertEquals(AppLanguage.EN, vm.language.value)
    }

    // ----- Pilih daerah -----

    @Test
    fun loadsRegionsAndOpensFirstAvailableCallout() = runTest {
        val vm = PilihDaerahViewModel(regions, TestPlaces())
        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals(5, s.regions.size)
        assertEquals("DIY", s.selectedRegionId)
        assertEquals(2, s.upcomingCount)
        assertEquals(1, s.notOpenCount)
    }

    @Test
    fun eachAvailableRegionCardGetsItsOwnCountAndNames() = runTest {
        val s = PilihDaerahViewModel(regions, TestPlaces()).state.value
        val diy = s.placesOf("DIY")
        assertEquals(6, diy.count)
        assertEquals(listOf("Tempat 1", "Tempat 2", "Tempat 3", "Tempat 4"), diy.names)
        assertEquals(2, diy.moreCount)
        val jtm = s.placesOf("JTM")
        assertEquals(3, jtm.count)
        assertEquals(listOf("Jatim 1", "Jatim 2", "Jatim 3"), jtm.names)
        assertEquals(0, jtm.moreCount)
    }

    @Test
    fun unavailableRegionsGetNoPlaces() = runTest {
        val s = PilihDaerahViewModel(regions, TestPlaces()).state.value
        assertEquals(setOf("DIY", "JTM"), s.placesByRegion.keys)
        for (id in listOf("JTG", "BALI", "PAPUA", "TIDAK-ADA")) {
            assertEquals(id, RegionPlaces(), s.placesOf(id))
        }
    }

    @Test
    fun placeCountsDoNotFollowTheSelectedRegion() = runTest {
        val vm = PilihDaerahViewModel(regions, TestPlaces())
        vm.state.test {
            val before = awaitItem().placesByRegion
            vm.onPinTapped("JTM")
            assertEquals(before, awaitItem().placesByRegion)
        }
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

    // ----- Map spots (artifact positions) -----

    @Test
    fun everySampleRegionHasAMapSpotInsideTheMapAndAnIsland() {
        regions.all.forEachIndexed { index, region ->
            val pos = pinPositionFor(region.id, index)
            assertTrue(region.id, pos.x in 0f..MapUnitsWidth && pos.y in 0f..MapUnitsHeight)
            assertTrue(region.id, islandNameRes(region.id) != null)
        }
    }

    @Test
    fun islandCodesCoverAllJavaRegionsAndBantenHasADot() {
        listOf("DIY", "JTG", "JTM", "JBR", "JKT", "BTN").forEachIndexed { i, code ->
            assertEquals(code, R.string.island_jawa, islandNameRes(code))
            val pos = pinPositionFor(code, i)
            assertTrue(code, pos.x in 0f..MapUnitsWidth && pos.y in 0f..MapUnitsHeight)
        }
        assertNull(islandNameRes("BDG"))
        // Banten has its own spot, not the fallback row and not on top of a neighbour.
        val btn = pinPositionFor("BTN", 0)
        for (other in listOf("JKT", "JBR", "DIY")) {
            val o = pinPositionFor(other, 0)
            assertTrue(other, kotlin.math.hypot(btn.x - o.x, btn.y - o.y) >= 10f)
        }
        assertTrue(btn.y != 175f)
    }

    @Test
    fun diyPinSitsWhereTheArtifactPutsIt() {
        val pos = pinPositionFor("DIY", 0)
        assertEquals(118f, pos.x, 0.01f)
        assertEquals(144.1f, pos.y, 0.01f)
        assertTrue(pos.below)
    }

    @Test
    fun unknownRegionGetsAFallbackSpotInsideTheMap() {
        val pos = pinPositionFor("TIDAK-ADA", 3)
        assertTrue(pos.x in 0f..MapUnitsWidth && pos.y in 0f..MapUnitsHeight)
        assertNull(islandNameRes("TIDAK-ADA"))
    }
}
