package id.kelana.feature.cerita.ui

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class StoryFilter { All, Postcard, Video }

enum class FeedStatus {
    /** Session or stories still loading. */
    Loading,

    /** Guest: stories are never requested and never shown. */
    Guest,

    /** Loading the stories failed. */
    Error,

    /** Signed in, but there is nothing to show (no stories at all, or none for the chosen filter). */
    Empty,
    Content,
}

data class CeritaUiState(
    val status: FeedStatus = FeedStatus.Loading,
    val filter: StoryFilter = StoryFilter.All,
    /** Stories after the filter, newest first. */
    val items: List<Story> = emptyList(),
    /** Counts over all stories, before the filter. */
    val postcardCount: Int = 0,
    val videoCount: Int = 0,
) {
    /** True when there is no story at all (as opposed to none matching the filter). */
    val feedIsEmpty: Boolean get() = postcardCount == 0 && videoCount == 0
}

private sealed interface FeedSource {
    data object Loading : FeedSource
    data object Guest : FeedSource
    data object Error : FeedSource
    data class Loaded(val stories: List<Story>) : FeedSource
}

/**
 * Cerita tab feed. A guest never reaches [StoryRepository]: stories are read only while the session is signed in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CeritaViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    private val storyRepository: StoryRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(StoryFilter.All)
    private val retryTick = MutableStateFlow(0)

    private val source: Flow<FeedSource> = sessionRepository.sessionState.flatMapLatest { session ->
        when (session) {
            SessionState.Loading -> flowOf(FeedSource.Loading)
            SessionState.Guest -> flowOf(FeedSource.Guest)
            is SessionState.SignedIn -> retryTick.flatMapLatest {
                storyRepository.stories()
                    .map<List<Story>, FeedSource> { FeedSource.Loaded(it) }
                    .onStart { emit(FeedSource.Loading) }
                    .catch { emit(FeedSource.Error) }
            }
        }
    }

    val uiState: StateFlow<CeritaUiState> = combine(source, filter) { src, chosen ->
        when (src) {
            FeedSource.Loading -> CeritaUiState(FeedStatus.Loading, chosen)
            FeedSource.Guest -> CeritaUiState(FeedStatus.Guest, chosen)
            FeedSource.Error -> CeritaUiState(FeedStatus.Error, chosen)
            is FeedSource.Loaded -> {
                val newestFirst = src.stories.sortedByDescending { it.createdAt }
                val shown = when (chosen) {
                    StoryFilter.All -> newestFirst
                    StoryFilter.Postcard -> newestFirst.filterIsInstance<PostcardStory>()
                    StoryFilter.Video -> newestFirst.filterIsInstance<VideoStory>()
                }
                CeritaUiState(
                    status = if (shown.isEmpty()) FeedStatus.Empty else FeedStatus.Content,
                    filter = chosen,
                    items = shown,
                    postcardCount = newestFirst.count { it is PostcardStory },
                    videoCount = newestFirst.count { it is VideoStory },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CeritaUiState())

    fun select(newFilter: StoryFilter) {
        filter.value = newFilter
    }

    fun retry() {
        retryTick.update { it + 1 }
    }
}
