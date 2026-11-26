package id.kelana.feature.postcard.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.kelana.core.data.postcard.FakePostcardRepository
import id.kelana.core.data.repository.FakePlaceRepository
import id.kelana.core.data.stamp.FakeStampRepository
import id.kelana.core.model.PostcardPhoto
import id.kelana.core.model.PostcardPublishResult
import id.kelana.core.model.PostcardStyle
import id.kelana.core.model.SessionState
import id.kelana.core.model.Stamp
import id.kelana.feature.postcard.FakePhotoProcessor
import id.kelana.feature.postcard.FakeRegions
import id.kelana.feature.postcard.FakeSessionRepository
import id.kelana.feature.postcard.ListPlaces
import id.kelana.feature.postcard.placeIn
import id.kelana.feature.postcard.domain.PostcardShareSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TulisViewModelTest {
    private val t0 = Instant.parse("2026-11-20T03:00:00Z")
    private val sonobudoyo = "museum-sonobudoyo"
    private val tamansari = "tamansari"

    private val postcards = FakePostcardRepository()
    private val photos = FakePhotoProcessor()
    private val share = PostcardShareSession()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.vm(
        placeId: String? = sonobudoyo,
        stamps: List<Stamp> = listOf(Stamp(sonobudoyo, t0), Stamp(tamansari, t0.plusSeconds(60))),
        session: SessionState = id.kelana.feature.postcard.SignedInState,
    ): TulisViewModel {
        val handle = SavedStateHandle(if (placeId == null) emptyMap() else mapOf("placeId" to placeId))
        return TulisViewModel(
            handle, FakePlaceRepository(), FakeStampRepository(stamps), FakeSessionRepository(session),
            postcards, photos, share, id.kelana.feature.postcard.FakeRegions(),
        ).keepActive(this)
    }

    /** Keeps the WhileSubscribed state flow running for the whole test so `.value` is always current. */
    private fun TulisViewModel.keepActive(scope: TestScope): TulisViewModel {
        scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { uiState.collect {} }
        return this
    }

    private suspend fun TulisViewModel.ready(): TulisUiState {
        return uiState.first { it.mode != TulisMode.Loading }
    }

    @Test fun guest_sees_the_login_prompt_state() = runTest {
        val m = vm(session = SessionState.Guest)
        m.uiState.test {
            var s = awaitItem()
            while (s.mode == TulisMode.Loading) s = awaitItem()
            assertEquals(TulisMode.Guest, s.mode)
            assertFalse(s.canPublish)
        }
    }

    @Test fun no_stamps_and_no_place_leads_to_the_passport() = runTest {
        val s = vm(placeId = null, stamps = emptyList()).ready()
        assertEquals(TulisMode.NoStamps, s.mode)
    }

    @Test fun place_without_a_stamp_explains_that_a_stamp_is_needed() = runTest {
        val s = vm(placeId = "kotagede", stamps = listOf(Stamp(sonobudoyo, t0))).ready()
        val mode = s.mode as TulisMode.NeedsStamp
        assertEquals("kotagede", mode.place.id)
        assertFalse(s.canPublish)
    }

    @Test fun place_with_a_stamp_opens_the_form() = runTest {
        val s = vm().ready()
        val mode = s.mode as TulisMode.Form
        assertEquals(sonobudoyo, mode.place.id)
        assertEquals(PostcardStyle.Jurnal, s.style)
        assertTrue(s.isPublic)
        assertNull(s.rating)
    }

    @Test fun without_a_place_the_user_picks_one_of_the_stamped_places() = runTest {
        val m = vm(placeId = null)
        val pick = m.ready().mode as TulisMode.PickPlace
        assertEquals(listOf(sonobudoyo, tamansari).toSet(), pick.places.map { it.place.id }.toSet())
        m.onPlacePicked(tamansari)
        val form = m.uiState.value.mode as TulisMode.Form
        assertEquals(tamansari, form.place.id)
    }

    @Test fun message_must_have_one_to_500_characters_after_trimming() = runTest {
        val m = vm()
        m.ready()
        assertFalse(m.uiState.value.canPublish)
        m.onMessageChanged("   ")
        assertFalse(m.uiState.value.canPublish)
        m.onMessageChanged(" halo ")
        assertTrue(m.uiState.value.canPublish)
        m.onMessageChanged("x".repeat(600))
        assertEquals(500, m.uiState.value.message.length)
        assertTrue(m.uiState.value.canPublish)
    }

    @Test fun rating_is_optional_never_preselected_and_a_second_tap_clears_it() = runTest {
        val m = vm()
        assertNull(m.ready().rating)
        m.onRating(4)
        assertEquals(4, m.uiState.value.rating)
        m.onRating(2)
        assertEquals(2, m.uiState.value.rating)
        m.onRating(2)
        assertNull(m.uiState.value.rating)
        m.onRating(9)
        assertNull(m.uiState.value.rating)
    }

    @Test fun published_result_stores_the_card_and_opens_bagikan() = runTest {
        val m = vm()
        m.ready()
        m.onMessageChanged("  Keris dan wayangnya lengkap  ")
        m.onRating(4)
        m.onStyle(PostcardStyle.Cap)
        m.onPublicChanged(false)
        m.onPhotoChosen("content://picked/1")
        m.events.test {
            m.onPublish()
            assertEquals(TulisEvent.OpenBagikan, awaitItem())
        }
        val draft = postcards.published.single()
        assertEquals(sonobudoyo, draft.placeId)
        assertEquals("Keris dan wayangnya lengkap", draft.message)
        assertEquals(4, draft.rating)
        assertEquals(PostcardStyle.Cap, draft.style)
        assertFalse(draft.isPublic)
        assertNotNull(draft.photo)

        val card = share.card!!
        assertEquals("Museum Sonobudoyo", card.placeName)
        assertEquals(t0, card.stampedAt) // the stamp date, not today's date
        assertEquals("Keris dan wayangnya lengkap", card.message)
        assertEquals(PostcardStyle.Cap, card.style)
        assertEquals(PublishState.Idle, m.uiState.value.publish)
    }

    @Test fun network_error_keeps_the_form_and_allows_a_retry() = runTest {
        postcards.nextResult = PostcardPublishResult.NetworkError
        val m = vm()
        m.ready()
        m.onMessageChanged("halo")
        m.onPublish()
        assertEquals(PublishState.Failed(PublishError.Network), m.uiState.value.publish)
        assertEquals("halo", m.uiState.value.message)
        assertTrue(m.uiState.value.canPublish)
        assertNull(share.card)

        postcards.nextResult = PostcardPublishResult.Published("id-1")
        m.events.test {
            m.onPublish()
            assertEquals(TulisEvent.OpenBagikan, awaitItem())
        }
        assertEquals(2, postcards.published.size)
    }

    @Test fun invalid_and_not_configured_results_become_errors() = runTest {
        val m = vm()
        m.ready()
        m.onMessageChanged("halo")
        postcards.nextResult = PostcardPublishResult.Invalid
        m.onPublish()
        assertEquals(PublishState.Failed(PublishError.Invalid), m.uiState.value.publish)
        postcards.nextResult = PostcardPublishResult.NotConfigured
        m.onPublish()
        assertEquals(PublishState.Failed(PublishError.NotConfigured), m.uiState.value.publish)
        assertNull(share.card)
    }

    @Test fun server_needs_stamp_switches_to_the_explanation() = runTest {
        postcards.nextResult = PostcardPublishResult.NeedsStamp
        val m = vm()
        m.ready()
        m.onMessageChanged("halo")
        m.onPublish()
        val mode = m.uiState.value.mode as TulisMode.NeedsStamp
        assertEquals(sonobudoyo, mode.place.id)
        assertNull(share.card)
    }

    @Test fun server_needs_login_sends_the_user_to_masuk() = runTest {
        postcards.nextResult = PostcardPublishResult.NeedsLogin
        val m = vm()
        m.ready()
        m.onMessageChanged("halo")
        m.events.test {
            m.onPublish()
            assertEquals(TulisEvent.OpenLogin, awaitItem())
        }
    }

    @Test fun a_second_tap_while_sending_is_ignored() = runTest {
        val gate = CompletableDeferred<Unit>()
        val slow = object : id.kelana.core.model.PostcardRepository {
            var calls = 0
            override suspend fun publish(draft: id.kelana.core.model.PostcardDraft): PostcardPublishResult {
                calls++
                gate.await()
                return PostcardPublishResult.NetworkError
            }
        }
        val handle = SavedStateHandle(mapOf("placeId" to sonobudoyo))
        val m = TulisViewModel(
            handle, FakePlaceRepository(), FakeStampRepository(listOf(Stamp(sonobudoyo, t0))),
            FakeSessionRepository(), slow, photos, share, id.kelana.feature.postcard.FakeRegions(),
        ).keepActive(this)
        m.ready()
        m.onMessageChanged("halo")
        m.onPublish()
        assertEquals(PublishState.Sending, m.uiState.value.publish)
        assertFalse(m.uiState.value.canPublish)
        m.onPublish()
        gate.complete(Unit)
        assertEquals(1, slow.calls)
    }

    @Test fun photo_is_processed_then_kept_and_can_be_removed() = runTest {
        val m = vm()
        m.ready()
        m.onPhotoChosen("content://picked/1")
        assertEquals(listOf("content://picked/1"), photos.sources)
        assertNotNull(m.uiState.value.photo)
        assertFalse(m.uiState.value.photoBusy)
        m.onPhotoRemoved()
        assertNull(m.uiState.value.photo)
    }

    @Test fun unreadable_photo_shows_a_failure_and_keeps_no_photo() = runTest {
        photos.result = null
        val m = vm()
        m.ready()
        m.onPhotoChosen("content://picked/bad")
        assertTrue(m.uiState.value.photoFailed)
        assertNull(m.uiState.value.photo)
        photos.result = PostcardPhoto(byteArrayOf(9), "image/jpeg")
        m.onPhotoChosen("content://picked/good")
        assertFalse(m.uiState.value.photoFailed)
        assertSame(photos.result, m.uiState.value.photo)
    }

    @Test fun publishing_waits_for_the_photo_to_be_ready() = runTest {
        photos.gate = CompletableDeferred()
        val m = vm()
        m.ready()
        m.onMessageChanged("halo")
        m.onPhotoChosen("content://picked/1")
        assertTrue(m.uiState.value.photoBusy)
        assertFalse(m.uiState.value.canPublish)
        photos.gate!!.complete(Unit)
        assertTrue(m.uiState.value.canPublish)
    }

    @Test fun card_region_follows_the_region_of_the_stamped_place() = runTest {
        for ((regionId, name, short) in listOf(
            Triple("DIY", "DI Yogyakarta", "Jogja"),
            Triple("JTG", "Jawa Tengah", "Jateng"),
            Triple("JTM", "Jawa Timur", "Jatim"),
        )) {
            val place = placeIn(regionId)
            val session = PostcardShareSession()
            val m = TulisViewModel(
                SavedStateHandle(mapOf("placeId" to place.id)), ListPlaces(listOf(place)),
                FakeStampRepository(listOf(Stamp(place.id, t0))), FakeSessionRepository(), postcards, photos, session,
                // The selected region is deliberately another one: the place decides.
                FakeRegions(selected = "DIY".takeIf { regionId != "DIY" } ?: "JTM"),
            ).keepActive(this)
            m.ready()
            m.onMessageChanged("halo")
            m.onPublish()
            val card = session.card!!
            assertEquals(name, card.regionName)
            assertEquals(name.uppercase(), card.regionLabel)
            assertEquals(regionId, card.regionCode)
            assertEquals(short, card.regionShortName)
        }
    }
}
