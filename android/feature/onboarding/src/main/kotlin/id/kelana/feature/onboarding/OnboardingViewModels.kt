package id.kelana.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.AppLanguage
import id.kelana.core.model.AppLanguageRepository
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.RegionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Region the "Mulai dari Jogja" button picks. */
internal const val DefaultRegionId = "DIY"

/**
 * Mulai and Kenalan pages. Navigation is done by the screen with route objects; this ViewModel only
 * owns the "Mulai dari Jogja" choice, which selects DIY and then reports [finished].
 * Onboarding completion itself is stored by `:app` (onFinished), never here.
 */
@HiltViewModel
internal class IntroViewModel @Inject constructor(
    private val regions: RegionRepository,
    private val appLanguage: AppLanguageRepository,
) : ViewModel() {
    /** Language choice shown on the Mulai page; picking one applies it at once (the screen is recreated). */
    val language: StateFlow<AppLanguage> =
        appLanguage.language.stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.SYSTEM)

    fun setLanguage(language: AppLanguage) = appLanguage.set(language)

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    fun startFromJogja() {
        viewModelScope.launch {
            regions.select(DefaultRegionId)
            _finished.value = true
        }
    }

    fun onFinishedHandled() {
        _finished.value = false
    }
}

@HiltViewModel
internal class PilihDaerahViewModel @Inject constructor(
    private val regionRepository: RegionRepository,
    private val placeRepository: PlaceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PilihDaerahUiState())
    val state: StateFlow<PilihDaerahUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            regionRepository.regions().collect { list ->
                _state.update { old ->
                    old.copy(
                        loading = false,
                        regions = list,
                        // The first available region is open in the callout, as in the frame.
                        selectedRegionId = old.selectedRegionId ?: list.firstOrNull { it.isAvailable }?.id,
                    )
                }
            }
        }
        viewModelScope.launch {
            // PROVISIONAL: PlaceRepository has no region filter; all places belong to the available region for now.
            placeRepository.places().collect { places ->
                _state.update { it.copy(placeNames = places.take(4).map { p -> p.name }, placeCount = places.size) }
            }
        }
    }

    /** Pin tap: shows the callout for that region. */
    fun onPinTapped(regionId: String) {
        if (_state.value.regions.any { it.id == regionId }) _state.update { it.copy(selectedRegionId = regionId) }
    }

    /** "Berkelana di sini" / "Berkelana di DI Yogyakarta". Only an available region is chosen. */
    fun choose(regionId: String) {
        val region = _state.value.regions.firstOrNull { it.id == regionId } ?: return
        if (!region.isAvailable) return
        viewModelScope.launch {
            regionRepository.select(region.id)
            _state.update { it.copy(finished = true) }
        }
    }

    fun onFinishedHandled() = _state.update { it.copy(finished = false) }
}
