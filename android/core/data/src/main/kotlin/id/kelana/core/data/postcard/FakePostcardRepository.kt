package id.kelana.core.data.postcard

import id.kelana.core.model.PostcardDraft
import id.kelana.core.model.PostcardPublishResult
import id.kelana.core.model.PostcardRepository

/** In-memory stand-in for previews and ViewModel tests. [nextResult] is the answer of every [publish]; not bound by Hilt. */
class FakePostcardRepository(
    var nextResult: PostcardPublishResult = PostcardPublishResult.Published("fake-postcard"),
) : PostcardRepository {
    val published = mutableListOf<PostcardDraft>()

    override suspend fun publish(draft: PostcardDraft): PostcardPublishResult {
        published += draft
        return nextResult
    }
}
