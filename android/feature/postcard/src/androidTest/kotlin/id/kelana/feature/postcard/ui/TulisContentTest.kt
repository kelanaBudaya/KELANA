package id.kelana.feature.postcard.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PostcardStyle
import id.kelana.feature.postcard.R
import id.kelana.feature.postcard.domain.StampedPlace
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Tulis postcard 7.1 and its guard states, with fixed states (no Hilt, no network). */
@RunWith(AndroidJUnit4::class)
class TulisContentTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun actions(onMessage: (String) -> Unit = {}) = TulisActions(
        onBack = { calls += "back" },
        onMessage = onMessage,
        onStyle = { calls += "style:${it.name}" },
        onPublish = { calls += "publish" },
        onPlace = { calls += "place:$it" },
        onLogin = { calls += "login" },
        onOpenPaspor = { calls += "paspor" },
        onTakeStamp = { calls += "takeStamp:$it" },
        onVideo = { calls += "video:$it" },
    )

    private fun show(state: TulisUiState) {
        rule.setContent { KelanaTheme { TulisContent(state, actions()) } }
    }

    /** Keeps the typed message in state, like the ViewModel does. */
    private fun showStateful(initial: TulisUiState) {
        rule.setContent {
            KelanaTheme {
                var state by remember { mutableStateOf(initial) }
                TulisContent(state, actions(onMessage = { state = state.copy(message = it) }))
            }
        }
    }

    private val form = TulisUiState(mode = TulisMode.Form(TestPlace))

    @Test
    fun guest_showsLoginGuard() {
        show(TulisUiState(mode = TulisMode.Guest))
        rule.onNodeWithText(str(R.string.postcard_guest_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.postcard_guest_action)).performClick()
        assertEquals(listOf("login"), calls)
    }

    @Test
    fun noStamps_leadsToPaspor() {
        show(TulisUiState(mode = TulisMode.NoStamps))
        rule.onNodeWithText(str(R.string.postcard_nostamps_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.postcard_open_paspor)).performClick()
        assertEquals(listOf("paspor"), calls)
    }

    @Test
    fun needsStamp_offersTakeStampAndPaspor() {
        show(TulisUiState(mode = TulisMode.NeedsStamp(TestPlace)))
        rule.onNodeWithText(str(R.string.postcard_needsstamp_title, TestPlace.name)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.postcard_needsstamp_action)).performClick()
        rule.onNodeWithText(str(R.string.postcard_open_paspor)).performClick()
        assertEquals(listOf("takeStamp:${TestPlace.id}", "paspor"), calls)
    }

    @Test
    fun pickPlace_listsStampedPlaces_andReportsChoice() {
        show(
            TulisUiState(
                mode = TulisMode.PickPlace(listOf(StampedPlace(TestPlace, Instant.parse("2026-11-20T03:00:00Z")))),
            ),
        )
        rule.onNodeWithText(str(R.string.postcard_pick_title)).assertIsDisplayed()
        rule.onNodeWithText(TestPlace.name).performClick()
        assertEquals(listOf("place:${TestPlace.id}"), calls)
    }

    @Test
    fun form_publishDisabledWithoutMessage_enabledAfterTyping() {
        showStateful(form)
        rule.onNodeWithText(str(R.string.postcard_publish)).assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("Koleksi wayangnya lengkap.")
        rule.onNodeWithText(str(R.string.postcard_publish)).assertIsEnabled().performClick()
        assertEquals(listOf("publish"), calls)
    }

    @Test
    fun form_whitespaceOnlyMessage_keepsPublishDisabled() {
        showStateful(form)
        rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("   ")
        rule.onNodeWithText(str(R.string.postcard_publish)).assertIsNotEnabled()
    }

    @Test
    fun form_videoButton_callsBackWithPlaceId() {
        show(form)
        rule.onNodeWithText(str(R.string.postcard_video_button)).assertIsDisplayed().performClick()
        assertEquals(listOf("video:${TestPlace.id}"), calls)
    }

    @Test
    fun form_styleChip_reportsStyle() {
        show(form)
        rule.onNodeWithText(str(R.string.postcard_style_peta)).performScrollTo().performClick()
        assertEquals(listOf("style:${PostcardStyle.Peta.name}"), calls)
    }

    @Test
    fun form_sending_showsProgressLabel_andDisablesPublish() {
        show(form.copy(message = "Halo", publish = PublishState.Sending))
        rule.onNodeWithText(str(R.string.postcard_publishing)).assertIsNotEnabled()
    }

    @Test
    fun form_networkError_showsBanner_andOffersRetry() {
        show(form.copy(message = "Halo", publish = PublishState.Failed(PublishError.Network)))
        rule.onNodeWithText(str(R.string.postcard_error_network)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.postcard_retry)).assertIsEnabled().performClick()
        assertEquals(listOf("publish"), calls)
    }
}
