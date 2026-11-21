package id.kelana.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.OnboardingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StartDestination { Onboarding, Jelajah }

@HiltViewModel
class AppViewModel @Inject constructor(
    private val onboarding: OnboardingRepository,
) : ViewModel() {

    /** Null until the stored flag is read, so the NavHost starts at the right place. */
    val startDestination: StateFlow<StartDestination?> = onboarding.completed
        .map { if (it) StartDestination.Jelajah else StartDestination.Onboarding }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun completeOnboarding() {
        viewModelScope.launch { onboarding.markCompleted() }
    }
}
