package id.kelana.feature.tempat.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceRepository
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

enum class PlaceLoadStatus { Loading, Loaded, NotFound, Error }

/** Single immutable state of Detail tempat. [place] is set only when [status] is [PlaceLoadStatus.Loaded]. */
data class DetailTempatUiState(
    val status: PlaceLoadStatus = PlaceLoadStatus.Loading,
    val place: Place? = null,
)

/** Loads one place by the `placeId` route argument. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailTempatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val places: PlaceRepository,
) : ViewModel() {
    private val placeId: String = savedStateHandle.get<String>("placeId").orEmpty()
    private val reload = MutableStateFlow(0)

    val uiState: StateFlow<DetailTempatUiState> = reload
        .flatMapLatest {
            places.place(placeId)
                .map { place ->
                    if (place == null) {
                        DetailTempatUiState(PlaceLoadStatus.NotFound)
                    } else {
                        DetailTempatUiState(PlaceLoadStatus.Loaded, place)
                    }
                }
                .onStart { emit(DetailTempatUiState(PlaceLoadStatus.Loading)) }
                .catch { e ->
                    if (e is CancellationException) throw e
                    emit(DetailTempatUiState(PlaceLoadStatus.Error))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailTempatUiState())

    fun retry() {
        reload.value += 1
    }
}
