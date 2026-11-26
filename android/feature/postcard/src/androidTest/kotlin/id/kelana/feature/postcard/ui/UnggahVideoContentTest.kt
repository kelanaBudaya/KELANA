package id.kelana.feature.postcard.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.VIDEO_MAX_SECONDS_PROVISIONAL
import id.kelana.core.model.VideoUploadFailure
import id.kelana.feature.postcard.R
import id.kelana.feature.postcard.domain.formatVideoDuration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unggah video 7.3 and its guard states, with fixed states (no Hilt, no network). */
@RunWith(AndroidJUnit4::class)
class UnggahVideoContentTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val form = UnggahVideoUiState(mode = UnggahMode.Form(TestPlace))
    private val shortVideo = PickedVideo("content://video/1", 42)

    private fun actionsFor(update: ((UnggahVideoUiState) -> UnggahVideoUiState) -> Unit = {}) = UnggahVideoActions(
        onBack = { calls += "back" },
        onPick = { calls += "pick" },
        onRemove = { calls += "remove" },
        onConsentArea = { v -> update { it.copy(consentArea = v) } },
        onConsentPeople = { v -> update { it.copy(consentPeople = v) } },
        onConsentCeremony = { v -> update { it.copy(consentCeremony = v) } },
        onUpload = { calls += "upload" },
        onLogin = { calls += "login" },
        onOpenPaspor = { calls += "paspor" },
        onTakeStamp = { calls += "takeStamp:$it" },
    )

    private fun show(state: UnggahVideoUiState) {
        rule.setContent { KelanaTheme { UnggahVideoContent(state, actionsFor()) } }
    }

    /** Keeps the consent checkboxes in state, like the ViewModel does. */
    private fun showStateful(initial: UnggahVideoUiState) {
        rule.setContent {
            KelanaTheme {
                var state by remember { mutableStateOf(initial) }
                UnggahVideoContent(state, actionsFor { change -> state = change(state) })
            }
        }
    }

    private fun tickAllConsents() {
        listOf(
            R.string.video_consent_area,
            R.string.video_consent_people,
            R.string.video_consent_ceremony,
        ).forEach { rule.onNodeWithText(str(it)).performScrollTo().performClick() }
    }

    @Test
    fun guest_showsLoginGuard() {
        show(UnggahVideoUiState(mode = UnggahMode.Guest))
        rule.onNodeWithText(str(R.string.video_guest_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.postcard_guest_action)).performClick()
        assertEquals(listOf("login"), calls)
    }

    @Test
    fun needsStamp_showsAmbilCapDuluState() {
        show(UnggahVideoUiState(mode = UnggahMode.NeedsStamp(TestPlace)))
        rule.onNodeWithText(str(R.string.video_needsstamp_title, TestPlace.name)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.postcard_needsstamp_action)).performClick()
        rule.onNodeWithText(str(R.string.postcard_open_paspor)).performClick()
        assertEquals(listOf("takeStamp:${TestPlace.id}", "paspor"), calls)
    }

    @Test
    fun placeUnknown_offersBack() {
        show(UnggahVideoUiState(mode = UnggahMode.PlaceUnknown))
        rule.onNodeWithText(str(R.string.video_unknown_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.video_unknown_action)).performClick()
        assertEquals(listOf("back"), calls)
    }

    @Test
    fun noVideo_uploadDisabled_andPickOpensPicker() {
        show(form)
        rule.onNodeWithText(str(R.string.video_upload)).assertIsNotEnabled()
        rule.onNodeWithText(str(R.string.video_pick_title)).performClick()
        assertEquals(listOf("pick"), calls)
    }

    @Test
    fun videoWithoutConsents_uploadDisabled() {
        show(form.copy(video = shortVideo))
        rule.onNodeWithText(formatVideoDuration(shortVideo.durationSeconds)).assertExists()
        rule.onNodeWithText(str(R.string.video_upload)).assertIsNotEnabled()
    }

    @Test
    fun videoWithTwoOfThreeConsents_uploadDisabled() {
        show(form.copy(video = shortVideo, consentArea = true, consentPeople = true))
        rule.onNodeWithText(str(R.string.video_upload)).assertIsNotEnabled()
    }

    @Test
    fun consentsWithoutVideo_uploadDisabled() {
        show(form.copy(consentArea = true, consentPeople = true, consentCeremony = true))
        rule.onNodeWithText(str(R.string.video_upload)).assertIsNotEnabled()
    }

    @Test
    fun videoAndAllThreeConsents_uploadEnabled_andCallsBack() {
        showStateful(form.copy(video = shortVideo))
        rule.onNodeWithText(str(R.string.video_upload)).assertIsNotEnabled()
        tickAllConsents()
        rule.onNodeWithText(str(R.string.video_upload)).assertIsEnabled().performClick()
        assertEquals(listOf("upload"), calls)
    }

    @Test
    fun tooLongVideo_showsMessage_andKeepsUploadDisabledEvenWithConsents() {
        val longVideo = PickedVideo("content://video/2", VIDEO_MAX_SECONDS_PROVISIONAL + 15)
        show(form.copy(video = longVideo, consentArea = true, consentPeople = true, consentCeremony = true))
        rule.onNodeWithText(
            str(
                R.string.video_too_long,
                formatVideoDuration(longVideo.durationSeconds),
                VIDEO_MAX_SECONDS_PROVISIONAL,
            ),
        ).assertExists()
        rule.onNodeWithText(str(R.string.video_upload)).assertIsNotEnabled()
    }

    @Test
    fun uploading_showsPercentageAndDisablesButton() {
        show(
            form.copy(
                video = shortVideo,
                consentArea = true,
                consentPeople = true,
                consentCeremony = true,
                upload = UploadState.Uploading(0.4f),
            ),
        )
        rule.onNodeWithText(str(R.string.video_uploading, 40)).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun failedUpload_showsBanner_andOffersRetry() {
        show(
            form.copy(
                video = shortVideo,
                consentArea = true,
                consentPeople = true,
                consentCeremony = true,
                upload = UploadState.Failed(VideoUploadFailure.Network),
            ),
        )
        rule.onNodeWithText(str(R.string.video_error_network)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.video_retry)).assertIsEnabled().performClick()
        assertEquals(listOf("upload"), calls)
    }

    @Test
    fun unreadableVideo_showsError() {
        show(form.copy(videoUnreadable = true))
        rule.onNodeWithText(str(R.string.video_unreadable)).assertExists()
    }
}
