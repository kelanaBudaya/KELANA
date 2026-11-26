package id.kelana.feature.postcard.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PostcardStyle
import id.kelana.feature.postcard.R
import id.kelana.feature.postcard.domain.CardContent
import id.kelana.feature.postcard.domain.ShareFormat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Bagikan postcard 7.2: format chips and share targets. */
@RunWith(AndroidJUnit4::class)
class BagikanContentTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val card = CardContent(
        placeName = TestPlace.name,
        capCode = TestPlace.capCode,
        message = "Koleksi wayangnya lengkap.",
        rating = 4,
        style = PostcardStyle.Jurnal,
        stampedAt = Instant.parse("2026-11-20T03:00:00Z"),
        photoJpeg = null,
        regionName = "DI Yogyakarta",
        regionCode = "DIY",
        regionShortName = "Jogja",
    )

    private fun show(state: BagikanUiState) {
        rule.setContent {
            KelanaTheme {
                BagikanContent(
                    state = state,
                    actions = BagikanActions(
                        onBack = { calls += "back" },
                        onFormat = { calls += "format:${it.name}" },
                        onShare = { calls += "share:${it.name}" },
                        onSave = { calls += "save" },
                        onWriteAgain = { calls += "writeAgain" },
                    ),
                    snackbarHost = SnackbarHostState(),
                )
            }
        }
    }

    @Test
    fun shareTargets_callWithTheirTarget() {
        show(BagikanUiState(card = card, canSave = true))
        rule.onNodeWithText(str(R.string.postcard_share_instagram)).assertIsDisplayed().performClick()
        rule.onNodeWithText(str(R.string.postcard_share_whatsapp)).assertIsDisplayed().performClick()
        rule.onNodeWithText(str(R.string.postcard_share_other)).assertIsDisplayed().performClick()
        rule.onNodeWithText(str(R.string.postcard_share_save)).assertIsDisplayed().performClick()
        assertEquals(
            listOf("share:InstagramStory", "share:WhatsApp", "share:Other", "save"),
            calls,
        )
    }

    @Test
    fun saveTile_hiddenWhenGalleryCannotBeUsed() {
        show(BagikanUiState(card = card, canSave = false))
        rule.onNodeWithText(str(R.string.postcard_share_save)).assertDoesNotExist()
        rule.onNodeWithText(str(R.string.postcard_share_instagram)).assertIsEnabled()
    }

    @Test
    fun formatChips_reportTheirFormat() {
        show(BagikanUiState(card = card))
        rule.onNodeWithText(str(R.string.postcard_format_feed)).performClick()
        rule.onNodeWithText(str(R.string.postcard_format_story)).performClick()
        assertEquals(listOf("format:${ShareFormat.Feed.name}", "format:${ShareFormat.Story.name}"), calls)
    }

    @Test
    fun busy_disablesShareTargets() {
        show(BagikanUiState(card = card, busy = true, canSave = true))
        rule.onNodeWithText(str(R.string.postcard_share_instagram)).assertIsNotEnabled()
        rule.onNodeWithText(str(R.string.postcard_share_whatsapp)).assertIsNotEnabled()
        rule.onNodeWithText(str(R.string.postcard_share_other)).assertIsNotEnabled()
        rule.onNodeWithText(str(R.string.postcard_share_save)).assertIsNotEnabled()
    }

    @Test
    fun cardGone_offersToWriteAgain() {
        show(BagikanUiState(card = null))
        rule.onNodeWithText(str(R.string.postcard_gone_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.postcard_share_instagram)).assertDoesNotExist()
        rule.onNodeWithText(str(R.string.postcard_gone_action)).performClick()
        assertEquals(listOf("writeAgain"), calls)
    }
}
