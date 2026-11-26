package id.kelana.feature.postcard.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.POSTCARD_MESSAGE_MAX
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceRepository
import id.kelana.core.model.PostcardDraft
import id.kelana.core.model.PostcardPhoto
import id.kelana.core.model.PostcardPublishResult
import id.kelana.core.model.PostcardRepository
import id.kelana.core.model.PostcardStyle
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.Stamp
import id.kelana.core.model.StampRepository
import id.kelana.feature.postcard.domain.CardContent
import id.kelana.feature.postcard.domain.PhotoProcessor
import id.kelana.feature.postcard.domain.PostcardShareSession
import id.kelana.feature.postcard.domain.StampedPlace
import id.kelana.feature.postcard.domain.isMessageValid
import id.kelana.feature.postcard.domain.stampedPlaces
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What Tulis postcard shows (Figma 7.1 and its guard states). */
sealed interface TulisMode {
    /** The session or the stamp list is still being read. */
    data object Loading : TulisMode

    /** Guest: posting needs an account. */
    data object Guest : TulisMode

    /** Signed in, no stamp anywhere: leads to Paspor. */
    data object NoStamps : TulisMode

    /** No place given: the user picks one of their stamped places. */
    data class PickPlace(val places: List<StampedPlace>) : TulisMode

    /** A place without a stamp (the client check is a hint) or the server refused it: a stamp is needed first. */
    data class NeedsStamp(val place: Place) : TulisMode

    /** The form for [place]. */
    data class Form(val place: Place) : TulisMode
}

enum class PublishError { Network, Invalid, NotConfigured }

sealed interface PublishState {
    data object Idle : PublishState
    data object Sending : PublishState
    data class Failed(val error: PublishError) : PublishState
}

@Immutable
data class TulisUiState(
    val mode: TulisMode = TulisMode.Loading,
    val photo: PostcardPhoto? = null,
    val photoBusy: Boolean = false,
    val photoFailed: Boolean = false,
    /** 1 to 5, or null: never preselected. */
    val rating: Int? = null,
    val message: String = "",
    val style: PostcardStyle = PostcardStyle.Jurnal,
    val isPublic: Boolean = true,
    val publish: PublishState = PublishState.Idle,
) {
    val messageLength: Int get() = message.length
    val canPublish: Boolean
        get() = mode is TulisMode.Form &&
            isMessageValid(message) &&
            !photoBusy &&
            publish !is PublishState.Sending
}

sealed interface TulisEvent {
    /** The postcard is stored: open Bagikan. */
    data object OpenBagikan : TulisEvent

    /** The server says the session is gone: send the user to Masuk. */
    data object OpenLogin : TulisEvent
}

private data class Form(
    val photo: PostcardPhoto? = null,
    val photoBusy: Boolean = false,
    val photoFailed: Boolean = false,
    val rating: Int? = null,
    val message: String = "",
    val style: PostcardStyle = PostcardStyle.Jurnal,
    val isPublic: Boolean = true,
    val publish: PublishState = PublishState.Idle,
    val pickedPlaceId: String? = null,
    val serverNeedsStamp: Boolean = false,
)

/**
 * Tulis postcard (7.1). The place comes from the route ([placeId]) or the user picks one of their stamped
 * places. The stamp check here is only a hint: the server (RLS) decides, and a refusal comes back as NeedsStamp.
 */
@HiltViewModel
class TulisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    places: PlaceRepository,
    private val stamps: StampRepository,
    session: SessionRepository,
    private val postcards: PostcardRepository,
    private val photos: PhotoProcessor,
    private val shareSession: PostcardShareSession,
) : ViewModel() {
    private val routePlaceId: String? = savedStateHandle.get<String>("placeId")

    private val form = MutableStateFlow(Form())
    private val allPlaces: Flow<List<Place>> = places.places().catch { emit(emptyList()) }
    private val ownStamps: Flow<List<Stamp>> = stamps.stamps().catch { emit(emptyList()) }

    private val _events = Channel<TulisEvent>(Channel.BUFFERED)
    val events: Flow<TulisEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<TulisUiState> = combine(
        form, allPlaces, ownStamps, session.sessionState,
    ) { f, placeList, stampList, sessionState ->
        val stamped = stampedPlaces(placeList, stampList)
        val chosenId = f.pickedPlaceId ?: routePlaceId
        val chosen = chosenId?.let { id -> placeList.firstOrNull { it.id.equals(id, ignoreCase = true) } }
        val hasStamp = chosen != null && stamped.any { it.place.id == chosen.id }
        val mode = when {
            sessionState is SessionState.Loading -> TulisMode.Loading
            sessionState is SessionState.Guest -> TulisMode.Guest
            chosen != null && (!hasStamp || f.serverNeedsStamp) -> TulisMode.NeedsStamp(chosen)
            chosen != null -> TulisMode.Form(chosen)
            stamped.isEmpty() -> TulisMode.NoStamps
            else -> TulisMode.PickPlace(stamped)
        }
        TulisUiState(
            mode = mode,
            photo = f.photo,
            photoBusy = f.photoBusy,
            photoFailed = f.photoFailed,
            rating = f.rating,
            message = f.message,
            style = f.style,
            isPublic = f.isPublic,
            publish = f.publish,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TulisUiState())

    fun onPlacePicked(placeId: String) = form.update { it.copy(pickedPlaceId = placeId, serverNeedsStamp = false) }

    /** Tapping the selected star again clears the rating (it is optional). */
    fun onRating(stars: Int) = form.update {
        if (stars !in 1..5) it else it.copy(rating = if (it.rating == stars) null else stars)
    }

    fun onMessageChanged(text: String) = form.update { it.copy(message = text.take(POSTCARD_MESSAGE_MAX)) }

    fun onStyle(style: PostcardStyle) = form.update { it.copy(style = style) }

    fun onPublicChanged(isPublic: Boolean) = form.update { it.copy(isPublic = isPublic) }

    /** [source] is the content Uri of the picked or captured photo, as text. */
    fun onPhotoChosen(source: String) {
        form.update { it.copy(photoBusy = true, photoFailed = false) }
        viewModelScope.launch {
            val result = photos.process(source)
            form.update {
                if (result == null) it.copy(photoBusy = false, photoFailed = true)
                else it.copy(photo = result, photoBusy = false, photoFailed = false)
            }
        }
    }

    /** The camera or picker could not be opened. */
    fun onPhotoUnavailable() = form.update { it.copy(photoFailed = true) }

    fun onPhotoRemoved() = form.update { it.copy(photo = null, photoFailed = false) }

    fun onPublish() {
        val state = uiState.value
        val mode = state.mode as? TulisMode.Form ?: return
        if (!state.canPublish) return
        val draft = PostcardDraft(
            placeId = mode.place.id,
            message = state.message.trim(),
            photo = state.photo,
            rating = state.rating,
            style = state.style,
            isPublic = state.isPublic,
        )
        form.update { it.copy(publish = PublishState.Sending) }
        viewModelScope.launch {
            when (val result = postcards.publish(draft)) {
                is PostcardPublishResult.Published -> {
                    shareSession.put(cardOf(mode.place, draft))
                    form.update { it.copy(publish = PublishState.Idle) }
                    _events.send(TulisEvent.OpenBagikan)
                }
                PostcardPublishResult.NeedsLogin -> {
                    form.update { it.copy(publish = PublishState.Idle) }
                    _events.send(TulisEvent.OpenLogin)
                }
                PostcardPublishResult.NeedsStamp ->
                    form.update { it.copy(publish = PublishState.Idle, serverNeedsStamp = true) }
                PostcardPublishResult.Invalid -> fail(PublishError.Invalid)
                PostcardPublishResult.NetworkError -> fail(PublishError.Network)
                PostcardPublishResult.NotConfigured -> fail(PublishError.NotConfigured)
            }
        }
    }

    private fun fail(error: PublishError) = form.update { it.copy(publish = PublishState.Failed(error)) }

    private suspend fun cardOf(place: Place, draft: PostcardDraft): CardContent {
        val stampedAt = stampedPlaces(listOf(place), ownStamps.first()).firstOrNull()?.stampedAt
        return CardContent(
            placeName = place.name,
            capCode = place.capCode,
            message = draft.message,
            rating = draft.rating,
            style = draft.style,
            stampedAt = stampedAt,
            photoJpeg = draft.photo?.bytes,
        )
    }
}
