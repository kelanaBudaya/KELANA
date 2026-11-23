package id.kelana.feature.postcard

import id.kelana.core.model.AuthResult
import id.kelana.core.model.PostcardPhoto
import id.kelana.core.model.SessionRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.SessionUser
import id.kelana.feature.postcard.domain.CardContent
import id.kelana.feature.postcard.domain.CardExporter
import id.kelana.feature.postcard.domain.PhotoProcessor
import id.kelana.feature.postcard.domain.ShareFormat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

val SignedInState = SessionState.SignedIn(SessionUser("user-1", "a@test.invalid", "Ayu"))

class FakeSessionRepository(initial: SessionState = SignedInState) : SessionRepository {
    val state = MutableStateFlow(initial)
    override val sessionState: Flow<SessionState> = state
    override suspend fun signInWithGoogle(idToken: String, nonce: String): AuthResult = AuthResult.Success
    override suspend fun signOut() {
        state.value = SessionState.Guest
    }
}

/** Answers [result] and records the sources it was asked to process. [gate] holds the answer back. */
class FakePhotoProcessor(var result: PostcardPhoto? = PostcardPhoto(byteArrayOf(1, 2, 3), "image/jpeg")) : PhotoProcessor {
    val sources = mutableListOf<String>()
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun process(source: String): PostcardPhoto? {
        sources += source
        gate?.await()
        return result
    }
}

class FakeCardExporter(
    override val canSaveToGallery: Boolean = true,
    var renderResult: String? = "content://fake/postcards/card.png",
    var saveResult: Boolean = true,
) : CardExporter {
    val rendered = mutableListOf<ShareFormat>()
    val saved = mutableListOf<ShareFormat>()
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun renderToCache(card: CardContent, format: ShareFormat): String? {
        rendered += format
        gate?.await()
        return renderResult
    }

    override suspend fun saveToGallery(card: CardContent, format: ShareFormat): Boolean {
        saved += format
        gate?.await()
        return saveResult
    }
}
