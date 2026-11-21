package id.kelana.feature.cerita.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.PostcardStory
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.Story
import id.kelana.core.model.StoryRepository
import id.kelana.core.model.VideoStory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface StoryDetailUiState {
    data object Loading : StoryDetailUiState

    /** Guest: the story is never requested. */
    data object Guest : StoryDetailUiState
    data object NotFound : StoryDetailUiState
    data object Error : StoryDetailUiState
    data class Postcard(val story: PostcardStory) : StoryDetailUiState
    data class Video(val story: VideoStory) : StoryDetailUiState
}

/** Shared by the postcard and the video detail screens. The story id comes from the route argument `storyId`. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: SessionRepository,
    private val storyRepository: StoryRepository,
) : ViewModel() {
    private val storyId: String = savedStateHandle.get<String>(ARG_STORY_ID).orEmpty()
    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<StoryDetailUiState> = sessionRepository.sessionState.flatMapLatest { session ->
        when (session) {
            SessionState.Loading -> flowOf(StoryDetailUiState.Loading)
            SessionState.Guest -> flowOf(StoryDetailUiState.Guest)
            is SessionState.SignedIn -> retryTick.flatMapLatest {
                storyRepository.story(storyId)
                    .map<Story?, StoryDetailUiState> { story ->
                        when (story) {
                            null -> StoryDetailUiState.NotFound
                            is PostcardStory -> StoryDetailUiState.Postcard(story)
                            is VideoStory -> StoryDetailUiState.Video(story)
                        }
                    }
                    .onStart { emit(StoryDetailUiState.Loading) }
                    .catch { emit(StoryDetailUiState.Error) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoryDetailUiState.Loading)

    fun retry() {
        retryTick.update { it + 1 }
    }

    companion object {
        /** Same name as the property in `CeritaPostcardRoute` and `CeritaVideoRoute`. */
        const val ARG_STORY_ID = "storyId"
    }
}
