package id.kelana.core.ui.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.PlaceCoverRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Exposes place id to cover thumbnail URL for list cards, without depending on a feature. */
@HiltViewModel
class PlaceCoversViewModel @Inject constructor(
    repository: PlaceCoverRepository,
) : ViewModel() {
    val covers: StateFlow<Map<String, String>> = repository.covers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
