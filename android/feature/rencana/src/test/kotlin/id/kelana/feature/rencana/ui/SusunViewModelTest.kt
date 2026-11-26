package id.kelana.feature.rencana.ui

import id.kelana.core.model.Interest
import id.kelana.core.model.Pace
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlanChoices
import id.kelana.core.model.SpecialNeed
import id.kelana.core.model.TravelGroup
import id.kelana.core.model.Vehicle
import id.kelana.feature.rencana.domain.BuildResult
import id.kelana.feature.rencana.domain.PlanBuilder
import id.kelana.feature.rencana.domain.PlanRepository
import id.kelana.feature.rencana.domain.hoursFacts
import id.kelana.feature.rencana.domain.place
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class SusunViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val places = FakePlaces(listOf(place("m1", hours = hoursFacts(LocalTime.of(8, 0), LocalTime.of(21, 0)))))
    private val cache = FakeCache()
    private val connectivity = FakeConnectivity()

    private fun viewModel() = SusunViewModel(repo(places, FakeEvents(emptyList()), cache), connectivity, TestClock, stageDelayMs = 0)

    private fun SusunViewModel.fillAnswers() {
        selectCompanion(TravelGroup.Solo)
        toggleInterest(Interest.Museum)
    }

    @Test
    fun startsOnTodayWithSensibleDefaultsAndNothingToContinueWith() {
        val s = viewModel().uiState.value
        assertEquals(TestToday, s.startDate)
        assertNull(s.endDate)
        assertEquals(1, s.dayCount)
        assertEquals(YearMonth.of(2026, 11), s.visibleMonth)
        assertTrue(s.canContinueTanggal)
        assertFalse(s.canContinueTeman)
        assertFalse(s.canContinueMinat)
        assertTrue(s.canContinueWaktu)
        assertFalse(s.canSubmit)
        assertEquals(LocalTime.of(8, 0), s.windowStart)
        assertEquals(LocalTime.of(21, 0), s.windowEnd)
        assertEquals(Pace.Relaxed, s.pace)
        assertEquals(Vehicle.TransitAndWalk, s.vehicle)
        assertTrue(s.needs.isEmpty())
    }

    @Test
    fun companionAndInterestsGateTheirSteps() {
        val vm = viewModel()
        vm.selectCompanion(TravelGroup.Family)
        assertTrue(vm.uiState.value.canContinueTeman)
        vm.toggleInterest(Interest.Candi)
        assertTrue(vm.uiState.value.canContinueMinat)
        vm.toggleInterest(Interest.Candi)
        assertFalse(vm.uiState.value.canContinueMinat)
    }

    @Test
    fun aLaterTapSetsTheEndAndFurtherTapsRestart() {
        val vm = viewModel()
        // The start is preselected as today, so the first tap on a later date completes the range.
        vm.pickDate(TestToday.plusDays(2))
        assertEquals(TestToday, vm.uiState.value.startDate)
        assertEquals(TestToday.plusDays(2), vm.uiState.value.endDate)
        assertEquals(3, vm.uiState.value.dayCount)
        // a tap after a complete range starts over
        vm.pickDate(TestToday.plusDays(4))
        assertEquals(TestToday.plusDays(4), vm.uiState.value.startDate)
        assertNull(vm.uiState.value.endDate)
        // a tap before the start also starts over
        vm.pickDate(TestToday.plusDays(1))
        assertEquals(TestToday.plusDays(1), vm.uiState.value.startDate)
        assertNull(vm.uiState.value.endDate)
    }

    @Test
    fun tapOnTheStartAgainKeepsASingleDay() {
        val vm = viewModel()
        vm.pickDate(TestToday)
        assertNull(vm.uiState.value.endDate)
        assertEquals(1, vm.uiState.value.dayCount)
    }

    @Test
    fun pastDatesAreIgnored() {
        val vm = viewModel()
        vm.pickDate(TestToday.minusDays(1))
        assertEquals(TestToday, vm.uiState.value.startDate)
    }

    @Test
    fun aRangeLongerThanTheLimitRestartsFromTheTappedDate() {
        val vm = viewModel()
        val far = TestToday.plusDays(PlanBuilder.MAX_DAYS.toLong())
        vm.pickDate(far)
        assertEquals(far, vm.uiState.value.startDate)
        assertNull(vm.uiState.value.endDate)
        val ok = TestToday.plusDays(PlanBuilder.MAX_DAYS - 1L)
        val vm2 = viewModel()
        vm2.pickDate(TestToday)
        vm2.pickDate(ok)
        assertEquals(ok, vm2.uiState.value.endDate)
        assertEquals(PlanBuilder.MAX_DAYS, vm2.uiState.value.dayCount)
    }

    @Test
    fun monthNavigationCannotGoBeforeThisMonth() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.canGoPreviousMonth)
        vm.showPreviousMonth()
        assertEquals(YearMonth.of(2026, 11), vm.uiState.value.visibleMonth)
        vm.showNextMonth()
        assertEquals(YearMonth.of(2026, 12), vm.uiState.value.visibleMonth)
        assertTrue(vm.uiState.value.canGoPreviousMonth)
        vm.showPreviousMonth()
        assertEquals(YearMonth.of(2026, 11), vm.uiState.value.visibleMonth)
    }

    @Test
    fun setStartAndEndRoundToFiveMinutesAndClamp() {
        val vm = viewModel()
        vm.setStart(LocalTime.of(9, 17))
        assertEquals(LocalTime.of(9, 15), vm.uiState.value.windowStart)
        vm.setEnd(LocalTime.of(18, 59))
        assertEquals(LocalTime.of(18, 55), vm.uiState.value.windowEnd)
        vm.setStart(LocalTime.of(3, 0))
        assertEquals(LocalTime.of(5, 0), vm.uiState.value.windowStart)
        vm.setEnd(LocalTime.of(23, 59))
        assertEquals(LocalTime.of(23, 55), vm.uiState.value.windowEnd)
    }

    @Test
    fun windowUnderTwoHoursBlocksContinueButKeepsValue() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.windowTooShort)
        vm.setStart(LocalTime.of(10, 0))
        vm.setEnd(LocalTime.of(11, 30))
        assertEquals(LocalTime.of(11, 30), vm.uiState.value.windowEnd)
        assertTrue(vm.uiState.value.windowTooShort)
        assertFalse(vm.uiState.value.canContinueWaktu)
        vm.setEnd(LocalTime.of(12, 0))
        assertFalse(vm.uiState.value.windowTooShort)
        assertTrue(vm.uiState.value.canContinueWaktu)
    }

    @Test
    fun semuaIsExclusive() {
        val vm = viewModel()
        vm.toggleInterest(Interest.Museum)
        vm.toggleInterest(Interest.Candi)
        vm.toggleInterest(Interest.Semua)
        assertEquals(setOf(Interest.Semua), vm.uiState.value.interests)
        vm.toggleInterest(Interest.Candi)
        assertEquals(setOf(Interest.Candi), vm.uiState.value.interests)
        vm.toggleInterest(Interest.Semua)
        vm.toggleInterest(Interest.Semua)
        assertTrue(vm.uiState.value.interests.isEmpty())
    }

    @Test
    fun semuaShowsTheShowStage() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.showsShowStage)
        vm.toggleInterest(Interest.Semua)
        assertTrue(vm.uiState.value.showsShowStage)
        assertTrue(vm.uiState.value.canContinueMinat)
    }

    @Test
    fun noneClearsEveryNeedAndNeedsToggle() {
        val vm = viewModel()
        vm.toggleNeed(SpecialNeed.Wheelchair)
        vm.toggleNeed(SpecialNeed.MoreRests)
        assertEquals(setOf(SpecialNeed.Wheelchair, SpecialNeed.MoreRests), vm.uiState.value.needs)
        vm.toggleNeed(SpecialNeed.Wheelchair)
        assertEquals(setOf(SpecialNeed.MoreRests), vm.uiState.value.needs)
        vm.clearNeeds()
        assertTrue(vm.uiState.value.needs.isEmpty())
    }

    @Test
    fun checklistLengthFollowsTheAnswers() {
        val vm = viewModel()
        vm.fillAnswers()
        assertEquals(2, vm.stageCountNow())
        vm.selectCompanion(TravelGroup.Family)
        assertEquals(3, vm.stageCountNow())
        vm.toggleNeed(SpecialNeed.Wheelchair)
        assertEquals(4, vm.stageCountNow())
    }

    @Test
    fun buildWithMissingAnswersDoesNothing() {
        val vm = viewModel()
        vm.startBuild()
        assertFalse(vm.uiState.value.isBuilding)
        assertNull(vm.uiState.value.outcome)
        assertNull(cache.tripState.value)
    }

    @Test
    fun buildSavesThePlanAndReportsBuilt() = runTest {
        val vm = viewModel()
        vm.fillAnswers()
        vm.startBuild()
        val s = vm.uiState.value
        assertEquals(BuildOutcome.Built, s.outcome)
        assertFalse(s.isBuilding)
        val trip = cache.tripState.value
        assertNotNull(trip)
        assertEquals(listOf("m1"), trip!!.days.single().stops.map { it.placeId })
        assertEquals(TravelGroup.Solo, trip.choices.companion)
        vm.consumeOutcome()
        assertNull(vm.uiState.value.outcome)
    }

    @Test
    fun buildOfflineBuildsFromCache() = runTest {
        val vm = viewModel()
        vm.fillAnswers()
        connectivity.state.value = false
        vm.startBuild()
        assertEquals(BuildOutcome.Built, vm.uiState.value.outcome)
        assertNotNull(cache.tripState.value)
    }

    @Test
    fun buildFailureReportsFailed() = runTest {
        val throwing = object : PlanRepository by repo(places, FakeEvents(emptyList()), cache) {
            override suspend fun build(choices: PlanChoices): BuildResult = throw IllegalStateException("boom")
        }
        val vm = SusunViewModel(throwing, connectivity, TestClock, stageDelayMs = 0)
        vm.fillAnswers()
        vm.startBuild()
        assertEquals(BuildOutcome.Failed, vm.uiState.value.outcome)
        assertFalse(vm.uiState.value.isBuilding)
        assertNull(cache.tripState.value)
    }

    @Test
    fun noDataRemembersOfflineForCopy() = runTest {
        val empty = FakePlaces(emptyList())
        val vm = SusunViewModel(repo(empty, FakeEvents(emptyList()), cache), connectivity, TestClock, stageDelayMs = 0)
        vm.fillAnswers()
        vm.startBuild()
        assertEquals(BuildOutcome.NoData, vm.uiState.value.outcome)
        assertFalse(vm.uiState.value.outcomeOffline)
        vm.consumeOutcome()
        connectivity.state.value = false
        vm.startBuild()
        assertEquals(BuildOutcome.NoData, vm.uiState.value.outcome)
        assertTrue(vm.uiState.value.outcomeOffline)
    }

    @Test
    fun retryBuildRunsAgain() = runTest {
        val late = FakePlaces(emptyList())
        val vm = SusunViewModel(repo(late, FakeEvents(emptyList()), cache), connectivity, TestClock, stageDelayMs = 0)
        vm.fillAnswers()
        vm.startBuild()
        assertEquals(BuildOutcome.NoData, vm.uiState.value.outcome)
        late.all = places.all
        vm.retryBuild()
        assertEquals(BuildOutcome.Built, vm.uiState.value.outcome)
        assertNotNull(cache.tripState.value)
    }

    @Test
    fun buildThatFindsNothingKeepsTheOldPlan() = runTest {
        val vm = viewModel()
        vm.fillAnswers()
        vm.startBuild()
        val before = cache.tripState.value
        val again = viewModel()
        again.selectCompanion(TravelGroup.Solo)
        again.toggleInterest(Interest.Topeng) // no topeng place exists
        again.startBuild()
        assertEquals(BuildOutcome.NothingFits, again.uiState.value.outcome)
        assertEquals(before, cache.tripState.value)
        assertTrue(places.all.none { it.category == PlaceCategory.Topeng })
    }

    @Test
    fun aRangeBuildsOnePlanPerDay() = runTest {
        val vm = viewModel()
        vm.fillAnswers()
        vm.pickDate(TestToday)
        vm.pickDate(TestToday.plusDays(2))
        vm.startBuild()
        assertEquals(3, cache.tripState.value!!.days.size)
        assertEquals(LocalDate.of(2026, 11, 22), cache.tripState.value!!.choices.endDate)
    }
}
