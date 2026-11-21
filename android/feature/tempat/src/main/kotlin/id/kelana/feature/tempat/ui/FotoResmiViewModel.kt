package id.kelana.feature.tempat.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.PlaceRepository
import id.kelana.feature.tempat.domain.OFFICIAL_PHOTO_COUNT
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Official photos of a place. PROVISIONAL: [photoCount] placeholders drawn from [coverKey] until real photos exist. */
data class FotoResmiUiState(
    val status: PlaceLoadStatus = PlaceLoadStatus.Loading,
    val placeName: String = "",
    val coverKey: String = "",
    val photoCount: Int = 0,
)

@HiltViewModel
class FotoResmiViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    places: PlaceRepository,
) : ViewModel() {
    private val placeId: String = savedStateHandle.get<String>("placeId").orEmpty()

    val uiState: StateFlow<FotoResmiUiState> = places.place(placeId)
        .map { place ->
            if (place == null) {
                FotoResmiUiState(PlaceLoadStatus.NotFound)
            } else {
                FotoResmiUiState(PlaceLoadStatus.Loaded, place.name, place.coverKey, OFFICIAL_PHOTO_COUNT)
            }
        }
        .catch { e ->
            if (e is CancellationException) throw e
            emit(FotoResmiUiState(PlaceLoadStatus.Error))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FotoResmiUiState())
}
