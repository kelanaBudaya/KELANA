package id.kelana.core.data.postcard

import id.kelana.core.data.stamp.FakeSession
import id.kelana.core.model.PostcardDraft
import id.kelana.core.model.PostcardPhoto
import id.kelana.core.model.PostcardPublishResult
import id.kelana.core.model.PostcardStyle
import id.kelana.core.model.SessionState
import id.kelana.core.model.SessionUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class FakePostcardRemote : PostcardRemote {
    override var isConfigured: Boolean = true
    var uploadFailure: Exception? = null
    var insertFailure: Exception? = null
    val uploads = mutableListOf<String>()
    val inserts = mutableListOf<PostcardInsertDto>()
    val deleted = mutableListOf<String>()

    override suspend fun upload(path: String, bytes: ByteArray, mimeType: String) {
        uploadFailure?.let { throw it }
        uploads += path
    }

    override suspend fun insert(row: PostcardInsertDto) {
        insertFailure?.let { throw it }
        inserts += row
    }

    override suspend fun deleteFile(path: String) {
        deleted += path
    }
}

class SupabasePostcardRepositoryTest {
    private val userId = "0000AAAA-0000-0000-0000-000000000001"
    private val placeId = "1111BBBB-0000-0000-0000-000000000002"
    private val signedIn = SessionState.SignedIn(SessionUser(userId, null, null))

    private val remote = FakePostcardRemote()

    private fun repo(state: SessionState = signedIn) = SupabasePostcardRepository(FakeSession(state), remote)

    private fun draft(
        message: String = "Tamansari indah",
        photo: PostcardPhoto? = PostcardPhoto(ByteArray(10), "image/jpeg"),
        rating: Int? = 4,
    ) = PostcardDraft(placeId, message, photo, rating, PostcardStyle.Cap, isPublic = false)

    @Test
    fun publishesPhotoThenRowWithBucketPathAndAllFields() = runTest {
        val result = repo().publish(draft(message = "  halo  "))
        val published = result as PostcardPublishResult.Published
        val path = remote.uploads.single()
        assertTrue(path, path.startsWith("${userId.lowercase()}/${placeId.lowercase()}/"))
        assertTrue(path.endsWith(".jpg"))
        val row = remote.inserts.single()
        assertEquals(published.id, row.id)
        assertEquals("halo", row.message)
        assertEquals(path, row.photoPath)
        assertEquals(4, row.rating)
        assertEquals("cap", row.cardStyle)
        assertEquals(false, row.isPublic)
    }

    @Test
    fun noPhotoMeansNoUploadAndNullPath() = runTest {
        assertTrue(repo().publish(draft(photo = null, rating = null)) is PostcardPublishResult.Published)
        assertTrue(remote.uploads.isEmpty())
        assertNull(remote.inserts.single().photoPath)
        assertNull(remote.inserts.single().rating)
    }

    @Test
    fun guestGetsNeedsLoginAndNothingIsSent() = runTest {
        assertEquals(PostcardPublishResult.NeedsLogin, repo(SessionState.Guest).publish(draft()))
        assertTrue(remote.uploads.isEmpty() && remote.inserts.isEmpty())
    }

    @Test
    fun noSupabaseClientGivesNotConfigured() = runTest {
        remote.isConfigured = false
        assertEquals(PostcardPublishResult.NotConfigured, repo().publish(draft()))
        assertTrue(remote.uploads.isEmpty() && remote.inserts.isEmpty())
    }

    @Test
    fun rlsRefusalOnTheRowBecomesNeedsStampAndTheUploadedFileIsRemoved() = runTest {
        remote.insertFailure = PostcardHttpException(403)
        assertEquals(PostcardPublishResult.NeedsStamp, repo().publish(draft()))
        assertEquals(remote.uploads, remote.deleted)
    }

    @Test
    fun rlsRefusalOnTheUploadBecomesNeedsStamp() = runTest {
        remote.uploadFailure = PostcardHttpException(403)
        assertEquals(PostcardPublishResult.NeedsStamp, repo().publish(draft()))
        assertTrue(remote.inserts.isEmpty())
    }

    @Test
    fun networkFailureBecomesNetworkError() = runTest {
        remote.uploadFailure = IOException("offline")
        assertEquals(PostcardPublishResult.NetworkError, repo().publish(draft()))
        remote.uploadFailure = null
        remote.insertFailure = IOException("timeout")
        assertEquals(PostcardPublishResult.NetworkError, repo().publish(draft()))
    }

    @Test
    fun statusCodesMapToResults() = runTest {
        remote.insertFailure = PostcardHttpException(500)
        assertEquals(PostcardPublishResult.NetworkError, repo().publish(draft()))
        remote.insertFailure = PostcardHttpException(400)
        assertEquals(PostcardPublishResult.Invalid, repo().publish(draft()))
        remote.insertFailure = PostcardHttpException(401)
        assertEquals(PostcardPublishResult.NeedsLogin, repo().publish(draft()))
    }

    @Test
    fun draftsThatBreakServerRulesAreRefusedBeforeAnyCall() = runTest {
        assertEquals(PostcardPublishResult.Invalid, repo().publish(draft(message = "   ")))
        assertEquals(PostcardPublishResult.Invalid, repo().publish(draft(message = "x".repeat(501))))
        assertEquals(PostcardPublishResult.Invalid, repo().publish(draft(rating = 6)))
        assertEquals(PostcardPublishResult.Invalid, repo().publish(draft(rating = 0)))
        val big = PostcardPhoto(ByteArray(SupabasePostcardRepository.MAX_PHOTO_BYTES + 1), "image/jpeg")
        assertEquals(PostcardPublishResult.Invalid, repo().publish(draft(photo = big)))
        assertTrue(remote.uploads.isEmpty() && remote.inserts.isEmpty())
    }
}
