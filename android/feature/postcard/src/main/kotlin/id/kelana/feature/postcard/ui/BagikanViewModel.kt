package id.kelana.feature.postcard.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.feature.postcard.domain.CardContent
import id.kelana.feature.postcard.domain.CardExporter
import id.kelana.feature.postcard.domain.PostcardShareSession
import id.kelana.feature.postcard.domain.ShareFormat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ShareTarget { InstagramStory, WhatsApp, Other }

/** A short message for a snackbar, shown once. */
enum class BagikanNotice { Saved, SaveFailed, ShareFailed }

@Immutable
data class BagikanUiState(
    /** Null when the card is gone (process death): the screen offers to write a new one. */
    val card: CardContent? = null,
    val format: ShareFormat = ShareFormat.Story,
    val busy: Boolean = false,
    val canSave: Boolean = false,
    val notice: BagikanNotice? = null,
)

/** Open another app with the rendered picture. [uri] is a content Uri (FileProvider) as text. */
data class ShareEvent(val target: ShareTarget, val uri: String)

/**
 * Bagikan postcard (7.2). Instagram Stories always gets the 9:16 picture; WhatsApp, the share sheet and Simpan
 * use the format picked with the chips.
 */
@HiltViewModel
class BagikanViewModel @Inject constructor(
    session: PostcardShareSession,
    private val exporter: CardExporter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        BagikanUiState(card = session.card, canSave = exporter.canSaveToGallery),
    )
    val uiState: StateFlow<BagikanUiState> = _uiState.asStateFlow()

    private val _events = Channel<ShareEvent>(Channel.BUFFERED)
    val events: Flow<ShareEvent> = _events.receiveAsFlow()

    fun onFormat(format: ShareFormat) = _uiState.update { it.copy(format = format) }

    fun onShare(target: ShareTarget) {
        val state = _uiState.value
        val card = state.card ?: return
        if (state.busy) return
        val format = if (target == ShareTarget.InstagramStory) ShareFormat.Story else state.format
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            val uri = exporter.renderToCache(card, format)
            if (uri == null) {
                _uiState.update { it.copy(busy = false, notice = BagikanNotice.ShareFailed) }
            } else {
                _uiState.update { it.copy(busy = false) }
                _events.send(ShareEvent(target, uri))
            }
        }
    }

    fun onSave() {
        val state = _uiState.value
        val card = state.card ?: return
        if (state.busy || !state.canSave) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            val saved = exporter.saveToGallery(card, state.format)
            _uiState.update {
                it.copy(busy = false, notice = if (saved) BagikanNotice.Saved else BagikanNotice.SaveFailed)
            }
        }
    }

    fun onNoticeShown() = _uiState.update { it.copy(notice = null) }

    /** The other app could not be opened. */
    fun onShareFailed() = _uiState.update { it.copy(notice = BagikanNotice.ShareFailed) }
}
