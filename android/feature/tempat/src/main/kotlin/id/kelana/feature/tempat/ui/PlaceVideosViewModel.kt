package id.kelana.feature.tempat.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.PlaceVideo
import id.kelana.core.model.PlaceVideoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** State of the "Video pengunjung" section only. A failure here never affects the rest of Detail tempat. */
sealed interface PlaceVideosState {
    data object Loading : PlaceVideosState
    data object Empty : PlaceVideosState
    data object Error : PlaceVideosState
    data class Videos(val items: List<PlaceVideo>) : PlaceVideosState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaceVideosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PlaceVideoRepository,
) : ViewModel() {
    private val placeId: String = savedStateHandle.get<String>("placeId").orEmpty()
    private val reload = MutableStateFlow(0)

    val state: StateFlow<PlaceVideosState> = reload
        .flatMapLatest {
            repository.videos(placeId)
                .map<List<PlaceVideo>, PlaceVideosState> { list ->
                    if (list.isEmpty()) PlaceVideosState.Empty else PlaceVideosState.Videos(list)
                }
                .onStart { emit(PlaceVideosState.Loading) }
                .catch { e ->
                    if (e is CancellationException) throw e
                    emit(PlaceVideosState.Error)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaceVideosState.Loading)

    fun retry() {
        reload.value += 1
    }
}
