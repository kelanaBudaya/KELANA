package id.kelana.app

import id.kelana.core.model.OnboardingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private class FakeOnboardingRepository(initial: Boolean) : OnboardingRepository {
        val flag = MutableStateFlow(initial)
        override val completed = flag
        override suspend fun markCompleted() {
            flag.value = true
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun firstLaunch_startsAtOnboarding() = runTest {
        val vm = AppViewModel(FakeOnboardingRepository(initial = false))
        assertEquals(StartDestination.Onboarding, vm.startDestination.filterNotNull().first())
    }

    @Test
    fun returningUser_startsAtJelajah() = runTest {
        val vm = AppViewModel(FakeOnboardingRepository(initial = true))
        assertEquals(StartDestination.Jelajah, vm.startDestination.filterNotNull().first())
    }

    @Test
    fun completeOnboarding_persistsTheFlag() = runTest {
        val repo = FakeOnboardingRepository(initial = false)
        val vm = AppViewModel(repo)
        vm.completeOnboarding()
        assertEquals(true, repo.flag.value)
    }
}
