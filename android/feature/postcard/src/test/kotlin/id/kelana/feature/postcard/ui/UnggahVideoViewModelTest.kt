package id.kelana.feature.postcard.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.kelana.core.data.repository.FakePlaceRepository
import id.kelana.core.data.stamp.FakeStampRepository
import id.kelana.core.model.SessionState
import id.kelana.core.model.Stamp
import id.kelana.core.model.VIDEO_CAPTION_MAX
import id.kelana.core.model.VIDEO_MAX_SECONDS_PROVISIONAL
import id.kelana.core.model.VideoUploadFailure
import id.kelana.core.model.VideoUploadRepository
import id.kelana.core.model.VideoUploadRequest
import id.kelana.core.model.VideoUploadUpdate
import id.kelana.feature.postcard.FakeSessionRepository
import id.kelana.feature.postcard.SignedInState
import id.kelana.feature.postcard.domain.VideoMetadataReader
import id.kelana.feature.postcard.domain.formatVideoDuration
import id.kelana.feature.postcard.domain.secondsFromMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** Upload driven by the test: each call hands the test a [MutableSharedFlow] to push updates into. */
private class ScriptedUploads : VideoUploadRepository {
    val requests = mutableListOf<VideoUploadRequest>()
    var current = MutableSharedFlow<VideoUploadUpdate>(extraBufferCapacity = 16)

    override fun upload(request: VideoUploadRequest): Flow<VideoUploadUpdate> {
        requests += request
        val source = MutableSharedFlow<VideoUploadUpdate>(extraBufferCapacity = 16).also { current = it }
        // Ends the flow after a terminal update so the collector finishes like a real upload.
        return source.transformWhile { emit(it); it !is VideoUploadUpdate.Done && it !is VideoUploadUpdate.Failed }
    }
}

private class FakeMetadata(var seconds: Int? = 30) : VideoMetadataReader {
    val sources = mutableListOf<String>()
    override suspend fun durationSeconds(source: String): Int? {
        sources += source
        return seconds
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class UnggahVideoViewModelTest {
    private val t0 = Instant.parse("2026-11-20T03:00:00Z")
    private val sonobudoyo = "museum-sonobudoyo"
    private val uploads = ScriptedUploads()
    private val metadata = FakeMetadata()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.vm(
        placeId: String = sonobudoyo,
        stamps: List<Stamp> = listOf(Stamp(sonobudoyo, t0)),
        session: SessionState = SignedInState,
    ): UnggahVideoViewModel {
        val vm = UnggahVideoViewModel(
            SavedStateHandle(mapOf("placeId" to placeId)),
            FakePlaceRepository(), FakeStampRepository(stamps), FakeSessionRepository(session), uploads, metadata,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    private suspend fun UnggahVideoViewModel.ready(): UnggahVideoUiState = uiState.first { it.mode != UnggahMode.Loading }

    private fun UnggahVideoViewModel.consentAll() {
        onConsentArea(true)
        onConsentPeople(true)
        onConsentCeremony(true)
    }

    @Test fun `upload stays disabled until a video and all three checks`() = runTest {
        val vm = vm()
        assertTrue(vm.ready().mode is UnggahMode.Form)
        assertFalse(vm.uiState.value.canUpload)

        vm.onVideoPicked("content://v/1")
        assertFalse("video but no checks", vm.uiState.value.canUpload)

        vm.onConsentArea(true)
        vm.onConsentPeople(true)
        assertFalse("two of three checks", vm.uiState.value.canUpload)

        vm.onConsentCeremony(true)
        assertTrue(vm.uiState.value.canUpload)

        vm.onConsentPeople(false)
        assertFalse("a check was removed", vm.uiState.value.canUpload)

        vm.onConsentPeople(true)
        vm.onVideoRemoved()
        assertFalse("checks but no video", vm.uiState.value.canUpload)
    }

    @Test fun `all checks start unticked`() = runTest {
        val s = vm().ready()
        assertFalse(s.consentArea || s.consentPeople || s.consentCeremony)
    }

    @Test fun `video over the limit is flagged and cannot be uploaded`() = runTest {
        val vm = vm()
        vm.ready()
        vm.consentAll()
        metadata.seconds = VIDEO_MAX_SECONDS_PROVISIONAL + 1
        vm.onVideoPicked("content://v/long")
        assertTrue(vm.uiState.value.tooLong)
        assertFalse(vm.uiState.value.canUpload)
        vm.onUpload()
        assertTrue(uploads.requests.isEmpty())

        metadata.seconds = VIDEO_MAX_SECONDS_PROVISIONAL
        vm.onVideoPicked("content://v/exact")
        assertFalse(vm.uiState.value.tooLong)
        assertTrue(vm.uiState.value.canUpload)
    }

    @Test fun `unreadable video gives no video and no upload`() = runTest {
        val vm = vm()
        vm.ready()
        vm.consentAll()
        metadata.seconds = null
        vm.onVideoPicked("content://v/bad")
        assertTrue(vm.uiState.value.videoUnreadable)
        assertNull(vm.uiState.value.video)
        assertFalse(vm.uiState.value.canUpload)
    }

    @Test fun `progress then success sends the request and opens the story feed`() = runTest {
        val vm = vm()
        vm.ready()
        vm.consentAll()
        vm.onVideoPicked("content://v/1")
        vm.onCaption("  Ruang wayang  ")
        vm.events.test {
            vm.onUpload()
            assertEquals(UploadState.Uploading(0f), vm.uiState.value.upload)
            assertEquals(
                VideoUploadRequest("content://v/1", sonobudoyo, "Ruang wayang", 30),
                uploads.requests.single(),
            )
            assertFalse("no second upload while one runs", vm.uiState.value.canUpload)

            uploads.current.emit(VideoUploadUpdate.Progress(0.5f))
            assertEquals(UploadState.Uploading(0.5f), vm.uiState.value.upload)

            uploads.current.emit(VideoUploadUpdate.Done("v-1"))
            assertEquals(UploadState.Done, vm.uiState.value.upload)
            assertEquals(UnggahEvent.Uploaded, awaitItem())
        }
    }

    @Test fun `failure shows retry and a second try can succeed`() = runTest {
        val vm = vm()
        vm.ready()
        vm.consentAll()
        vm.onVideoPicked("content://v/1")
        vm.events.test {
            vm.onUpload()
            uploads.current.emit(VideoUploadUpdate.Failed(VideoUploadFailure.Network))
            assertEquals(UploadState.Failed(VideoUploadFailure.Network), vm.uiState.value.upload)
            assertTrue("retry is possible", vm.uiState.value.canUpload)

            vm.onUpload()
            assertEquals(2, uploads.requests.size)
            assertEquals(UploadState.Uploading(0f), vm.uiState.value.upload)
            uploads.current.emit(VideoUploadUpdate.Done("v-2"))
            assertEquals(UnggahEvent.Uploaded, awaitItem())
        }
    }

    @Test fun `a flow that ends without a result counts as a network failure`() = runTest {
        val empty = UnggahVideoViewModel(
            SavedStateHandle(mapOf("placeId" to sonobudoyo)),
            FakePlaceRepository(), FakeStampRepository(listOf(Stamp(sonobudoyo, t0))), FakeSessionRepository(),
            object : VideoUploadRepository {
                override fun upload(request: VideoUploadRequest): Flow<VideoUploadUpdate> = flow { }
            },
            metadata,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { empty.uiState.collect {} }
        empty.ready()
        empty.consentAll()
        empty.onVideoPicked("content://v/1")
        empty.onUpload()
        assertEquals(UploadState.Failed(VideoUploadFailure.Network), empty.uiState.value.upload)
    }

    @Test fun `no stamp for the place shows ambil cap dulu and blocks upload`() = runTest {
        val vm = vm(stamps = emptyList())
        val s = vm.ready()
        assertTrue(s.mode is UnggahMode.NeedsStamp)
        assertFalse(s.canUpload)
        vm.consentAll()
        vm.onVideoPicked("content://v/1")
        vm.onUpload()
        assertTrue(uploads.requests.isEmpty())
    }

    @Test fun `server refusing for a missing stamp switches to ambil cap dulu`() = runTest {
        val vm = vm()
        vm.ready()
        vm.consentAll()
        vm.onVideoPicked("content://v/1")
        vm.onUpload()
        uploads.current.emit(VideoUploadUpdate.Failed(VideoUploadFailure.NotStamped))
        assertTrue(vm.uiState.value.mode is UnggahMode.NeedsStamp)
        assertEquals(UploadState.Idle, vm.uiState.value.upload)
    }

    @Test fun `server saying the session is gone opens login`() = runTest {
        val vm = vm()
        vm.ready()
        vm.consentAll()
        vm.onVideoPicked("content://v/1")
        vm.events.test {
            vm.onUpload()
            uploads.current.emit(VideoUploadUpdate.Failed(VideoUploadFailure.NeedsLogin))
            assertEquals(UnggahEvent.OpenLogin, awaitItem())
        }
    }

    @Test fun `guest sees the login guard and unknown place is reported`() = runTest {
        assertEquals(UnggahMode.Guest, vm(session = SessionState.Guest).ready().mode)
        assertEquals(UnggahMode.PlaceUnknown, vm(placeId = "tidak-ada").ready().mode)
    }

    @Test fun `caption is capped`() = runTest {
        val vm = vm()
        vm.ready()
        vm.onCaption("a".repeat(VIDEO_CAPTION_MAX + 20))
        assertEquals(VIDEO_CAPTION_MAX, vm.uiState.value.caption.length)
    }

    @Test fun `duration helpers`() {
        assertEquals("0:42", formatVideoDuration(42))
        assertEquals("1:05", formatVideoDuration(65))
        assertEquals(1, secondsFromMillis(1L))
        assertEquals(31, secondsFromMillis(30_001L))
        assertNull(secondsFromMillis(0L))
        assertNull(secondsFromMillis(null))
    }
}
