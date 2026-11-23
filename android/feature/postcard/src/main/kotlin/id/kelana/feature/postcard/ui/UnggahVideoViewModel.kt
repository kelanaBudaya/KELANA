package id.kelana.feature.postcard.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.Stamp
import id.kelana.core.model.StampRepository
import id.kelana.core.model.VIDEO_CAPTION_MAX
import id.kelana.core.model.VIDEO_MAX_SECONDS_PROVISIONAL
import id.kelana.core.model.VideoUploadFailure
import id.kelana.core.model.VideoUploadRepository
import id.kelana.core.model.VideoUploadRequest
import id.kelana.core.model.VideoUploadUpdate
import id.kelana.feature.postcard.domain.VideoMetadataReader
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What Unggah video (7.3) shows. */
sealed interface UnggahMode {
    data object Loading : UnggahMode

    /** Guest: uploading needs an account. */
    data object Guest : UnggahMode

    /** The place id is not a known place. */
    data object PlaceUnknown : UnggahMode

    /** No stamp for [place] (client hint, or the server refused): "ambil cap dulu". */
    data class NeedsStamp(val place: Place) : UnggahMode

    data class Form(val place: Place) : UnggahMode
}

/** A video picked with the Photo Picker. [durationSeconds] is read on the device. */
data class PickedVideo(val uri: String, val durationSeconds: Int) {
    val tooLong: Boolean get() = durationSeconds > VIDEO_MAX_SECONDS_PROVISIONAL
}

sealed interface UploadState {
    data object Idle : UploadState

    /** [fraction] 0.0 to 1.0. */
    data class Uploading(val fraction: Float) : UploadState

    data class Failed(val reason: VideoUploadFailure) : UploadState

    data object Done : UploadState
}

@Immutable
data class UnggahVideoUiState(
    val mode: UnggahMode = UnggahMode.Loading,
    val video: PickedVideo? = null,
    /** The picked file could not be read (no duration). */
    val videoUnreadable: Boolean = false,
    val readingVideo: Boolean = false,
    val caption: String = "",
    val consentArea: Boolean = false,
    val consentPeople: Boolean = false,
    val consentCeremony: Boolean = false,
    val upload: UploadState = UploadState.Idle,
) {
    val allConsented: Boolean get() = consentArea && consentPeople && consentCeremony
    val tooLong: Boolean get() = video?.tooLong == true
    val canUpload: Boolean
        get() = mode is UnggahMode.Form &&
            video != null && !tooLong &&
            allConsented &&
            !readingVideo &&
            upload !is UploadState.Uploading && upload !is UploadState.Done
}

sealed interface UnggahEvent {
    /** The video is stored and live: go to Cerita. */
    data object Uploaded : UnggahEvent

    /** The server says the session is gone: send the user to Masuk. */
    data object OpenLogin : UnggahEvent
}

private data class VideoForm(
    val video: PickedVideo? = null,
    val unreadable: Boolean = false,
    val reading: Boolean = false,
    val caption: String = "",
    val consentArea: Boolean = false,
    val consentPeople: Boolean = false,
    val consentCeremony: Boolean = false,
    val upload: UploadState = UploadState.Idle,
    val serverNeedsStamp: Boolean = false,
)

/**
 * Unggah video (7.3). The place comes from the route. The stamp check here is only a hint: the server (RLS) decides,
 * and a refusal comes back as [UnggahMode.NeedsStamp]. The duration limit is PROVISIONAL (see VIDEO_MAX_SECONDS_PROVISIONAL).
 */
@HiltViewModel
class UnggahVideoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    places: PlaceRepository,
    stamps: StampRepository,
    session: SessionRepository,
    private val uploads: VideoUploadRepository,
    private val metadata: VideoMetadataReader,
) : ViewModel() {
    private val routePlaceId: String? = savedStateHandle.get<String>("placeId")

    private val form = MutableStateFlow(VideoForm())
    private val allPlaces: Flow<List<Place>> = places.places().catch { emit(emptyList()) }
    private val ownStamps: Flow<List<Stamp>> = stamps.stamps().catch { emit(emptyList()) }
    private var uploadJob: Job? = null

    private val _events = Channel<UnggahEvent>(Channel.BUFFERED)
    val events: Flow<UnggahEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<UnggahVideoUiState> = combine(
        form, allPlaces, ownStamps, session.sessionState,
    ) { f, placeList, stampList, sessionState ->
        val place = routePlaceId?.let { id -> placeList.firstOrNull { it.id.equals(id, ignoreCase = true) } }
        val hasStamp = place != null && stampList.any { it.placeId.equals(place.id, ignoreCase = true) }
        val mode = when {
            sessionState is SessionState.Loading -> UnggahMode.Loading
            sessionState is SessionState.Guest -> UnggahMode.Guest
            place == null -> UnggahMode.PlaceUnknown
            !hasStamp || f.serverNeedsStamp -> UnggahMode.NeedsStamp(place)
            else -> UnggahMode.Form(place)
        }
        UnggahVideoUiState(
            mode = mode,
            video = f.video,
            videoUnreadable = f.unreadable,
            readingVideo = f.reading,
            caption = f.caption,
            consentArea = f.consentArea,
            consentPeople = f.consentPeople,
            consentCeremony = f.consentCeremony,
            upload = f.upload,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnggahVideoUiState())

    /** [uri] is the content Uri of the picked video, as text. Reads its duration before anything can be uploaded. */
    fun onVideoPicked(uri: String) {
        form.update { it.copy(reading = true, unreadable = false, upload = UploadState.Idle) }
        viewModelScope.launch {
            val seconds = metadata.durationSeconds(uri)
            form.update {
                if (seconds == null) it.copy(reading = false, unreadable = true, video = null)
                else it.copy(reading = false, unreadable = false, video = PickedVideo(uri, seconds))
            }
        }
    }

    /** The picker could not be opened. */
    fun onPickerUnavailable() = form.update { it.copy(unreadable = true) }

    fun onVideoRemoved() = form.update { it.copy(video = null, unreadable = false, upload = UploadState.Idle) }

    fun onCaption(text: String) = form.update { it.copy(caption = text.take(VIDEO_CAPTION_MAX)) }

    fun onConsentArea(checked: Boolean) = form.update { it.copy(consentArea = checked) }

    fun onConsentPeople(checked: Boolean) = form.update { it.copy(consentPeople = checked) }

    fun onConsentCeremony(checked: Boolean) = form.update { it.copy(consentCeremony = checked) }

    /** Starts the upload, or starts it again after a failure. */
    fun onUpload() {
        val state = uiState.value
        val mode = state.mode as? UnggahMode.Form ?: return
        val video = state.video ?: return
        if (!state.canUpload) return
        val request = VideoUploadRequest(
            uri = video.uri,
            placeId = mode.place.id,
            caption = state.caption.trim(),
            durationSeconds = video.durationSeconds,
        )
        form.update { it.copy(upload = UploadState.Uploading(0f)) }
        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            var finished = false
            uploads.upload(request)
                .catch {
                    finished = true
                    fail(VideoUploadFailure.Network)
                }
                .collect { update ->
                    when (update) {
                        is VideoUploadUpdate.Progress ->
                            form.update { it.copy(upload = UploadState.Uploading(update.fraction.coerceIn(0f, 1f))) }
                        is VideoUploadUpdate.Done -> {
                            finished = true
                            form.update { it.copy(upload = UploadState.Done) }
                            _events.send(UnggahEvent.Uploaded)
                        }
                        is VideoUploadUpdate.Failed -> {
                            finished = true
                            onFailed(update.reason)
                        }
                    }
                }
            // A flow that ends without Done or Failed did not store anything.
            if (!finished) fail(VideoUploadFailure.Network)
        }
    }

    private suspend fun onFailed(reason: VideoUploadFailure) {
        when (reason) {
            VideoUploadFailure.NotStamped ->
                form.update { it.copy(upload = UploadState.Idle, serverNeedsStamp = true) }
            VideoUploadFailure.NeedsLogin -> {
                form.update { it.copy(upload = UploadState.Idle) }
                _events.send(UnggahEvent.OpenLogin)
            }
            else -> fail(reason)
        }
    }

    private fun fail(reason: VideoUploadFailure) = form.update { it.copy(upload = UploadState.Failed(reason)) }
}
