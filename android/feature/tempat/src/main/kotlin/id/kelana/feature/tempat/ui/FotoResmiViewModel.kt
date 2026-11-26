package id.kelana.feature.tempat.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.PlacePhoto
import id.kelana.core.model.PlacePhotoRepository
import id.kelana.core.model.PlaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Load status of the photo list itself (separate from the place status). */
enum class PhotoStatus { Loading, Loaded, Empty, Error }

/** Photo gallery of a place: the place header plus its open-licensed [photos]. */
data class FotoResmiUiState(
    val status: PlaceLoadStatus = PlaceLoadStatus.Loading,
    val placeName: String = "",
    val photos: List<PlacePhoto> = emptyList(),
    val photoStatus: PhotoStatus = PhotoStatus.Loading,
) {
    val photoCount: Int get() = photos.size
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FotoResmiViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val places: PlaceRepository,
    private val photos: PlacePhotoRepository,
) : ViewModel() {
    private val placeId: String = savedStateHandle.get<String>("placeId").orEmpty()
    private val reload = MutableStateFlow(0)

    val uiState: StateFlow<FotoResmiUiState> = reload
        .flatMapLatest {
            combine(places.place(placeId), photoState()) { place, (list, photoStatus) ->
                if (place == null) {
                    FotoResmiUiState(PlaceLoadStatus.NotFound)
                } else {
                    FotoResmiUiState(PlaceLoadStatus.Loaded, place.name, list, photoStatus)
                }
            }
                .catch { e ->
                    if (e is CancellationException) throw e
                    emit(FotoResmiUiState(PlaceLoadStatus.Error))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FotoResmiUiState())

    private fun photoState() = photos.photos(placeId)
        .map { list -> list to if (list.isEmpty()) PhotoStatus.Empty else PhotoStatus.Loaded }
        .catch { e ->
            if (e is CancellationException) throw e
            emit(emptyList<PlacePhoto>() to PhotoStatus.Error)
        }
        .onStart { emit(emptyList<PlacePhoto>() to PhotoStatus.Loading) }

    fun retry() {
        reload.value += 1
    }
}
