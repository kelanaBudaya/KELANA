package id.kelana.feature.postcard.ui

import app.cash.turbine.test
import id.kelana.core.model.PostcardStyle
import id.kelana.feature.postcard.FakeCardExporter
import id.kelana.feature.postcard.domain.CardContent
import id.kelana.feature.postcard.domain.PostcardShareSession
import id.kelana.feature.postcard.domain.ShareFormat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class BagikanViewModelTest {
    private val card = CardContent(
        placeName = "Museum Sonobudoyo",
        capCode = "YK-02",
        message = "Halo",
        rating = 4,
        style = PostcardStyle.Jurnal,
        stampedAt = Instant.parse("2026-11-20T03:00:00Z"),
        photoJpeg = null,
        regionName = "DI Yogyakarta",
        regionCode = "DIY",
        regionShortName = "Jogja",
    )
    private val exporter = FakeCardExporter()
    private val session = PostcardShareSession().also { it.put(card) }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(s: PostcardShareSession = session, e: FakeCardExporter = exporter) = BagikanViewModel(s, e)

    @Test fun starts_with_the_card_from_tulis_in_story_format() {
        val state = vm().uiState.value
        assertEquals(card, state.card)
        assertEquals(ShareFormat.Story, state.format)
        assertTrue(state.canSave)
    }

    @Test fun without_a_card_the_screen_gets_the_gone_state_and_actions_do_nothing() = runTest {
        val m = vm(PostcardShareSession())
        assertNull(m.uiState.value.card)
        m.onShare(ShareTarget.Other)
        m.onSave()
        assertTrue(exporter.rendered.isEmpty() && exporter.saved.isEmpty())
    }

    @Test fun format_chips_switch_between_story_and_feed() {
        val m = vm()
        m.onFormat(ShareFormat.Feed)
        assertEquals(ShareFormat.Feed, m.uiState.value.format)
        m.onFormat(ShareFormat.Story)
        assertEquals(ShareFormat.Story, m.uiState.value.format)
    }

    @Test fun whatsapp_and_the_share_sheet_use_the_picked_format() = runTest {
        val m = vm()
        m.onFormat(ShareFormat.Feed)
        m.events.test {
            m.onShare(ShareTarget.WhatsApp)
            assertEquals(ShareEvent(ShareTarget.WhatsApp, "content://fake/postcards/card.png"), awaitItem())
            m.onShare(ShareTarget.Other)
            assertEquals(ShareTarget.Other, awaitItem().target)
        }
        assertEquals(listOf(ShareFormat.Feed, ShareFormat.Feed), exporter.rendered)
    }

    @Test fun instagram_story_always_gets_the_9_16_picture() = runTest {
        val m = vm()
        m.onFormat(ShareFormat.Feed)
        m.events.test {
            m.onShare(ShareTarget.InstagramStory)
            assertEquals(ShareTarget.InstagramStory, awaitItem().target)
        }
        assertEquals(listOf(ShareFormat.Story), exporter.rendered)
        assertEquals(ShareFormat.Feed, m.uiState.value.format) // the chip choice itself is kept
    }

    @Test fun render_failure_shows_a_notice_and_opens_nothing() = runTest {
        exporter.renderResult = null
        val m = vm()
        m.events.test {
            m.onShare(ShareTarget.Other)
            expectNoEvents()
        }
        assertEquals(BagikanNotice.ShareFailed, m.uiState.value.notice)
        assertFalse(m.uiState.value.busy)
        m.onNoticeShown()
        assertNull(m.uiState.value.notice)
    }

    @Test fun save_reports_success_and_failure() = runTest {
        val m = vm()
        m.onFormat(ShareFormat.Feed)
        m.onSave()
        assertEquals(BagikanNotice.Saved, m.uiState.value.notice)
        assertEquals(listOf(ShareFormat.Feed), exporter.saved)
        exporter.saveResult = false
        m.onSave()
        assertEquals(BagikanNotice.SaveFailed, m.uiState.value.notice)
    }

    @Test fun save_is_unavailable_on_old_android() = runTest {
        val old = FakeCardExporter(canSaveToGallery = false)
        val m = vm(e = old)
        assertFalse(m.uiState.value.canSave)
        m.onSave()
        assertTrue(old.saved.isEmpty())
    }

    @Test fun a_second_tap_while_rendering_is_ignored() = runTest {
        val gate = CompletableDeferred<Unit>()
        exporter.gate = gate
        val m = vm()
        m.onShare(ShareTarget.Other)
        assertTrue(m.uiState.value.busy)
        m.onShare(ShareTarget.WhatsApp)
        m.onSave()
        gate.complete(Unit)
        assertEquals(1, exporter.rendered.size)
        assertTrue(exporter.saved.isEmpty())
        assertFalse(m.uiState.value.busy)
    }

    @Test fun share_failure_from_the_screen_becomes_a_notice() {
        val m = vm()
        m.onShareFailed()
        assertEquals(BagikanNotice.ShareFailed, m.uiState.value.notice)
    }
}
