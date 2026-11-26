package id.kelana.feature.cerita.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.cerita.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun str(id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

/**
 * Cerita feed with fixed states. The stories are the labelled sample objects from [CeritaPreviewData]
 * (not real visitors); the test only checks which state and controls the screen shows.
 */
@RunWith(AndroidJUnit4::class)
class CeritaFeedContentTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(state: CeritaUiState) {
        rule.setContent {
            KelanaTheme {
                CeritaFeedContent(
                    state = state,
                    onSelectFilter = { calls += "filter:${it.name}" },
                    onRetry = { calls += "retry" },
                    onOpen = { calls += "open:${it.id}" },
                    onReport = { calls += "report:${it.id}" },
                    onWritePostcard = { calls += "write" },
                    onSignIn = { calls += "signIn" },
                )
            }
        }
    }

    private val content = CeritaUiState(
        status = FeedStatus.Content,
        items = CeritaPreviewData.stories,
        postcardCount = 2,
        videoCount = 2,
    )

    @Test
    fun guest_seesLoginGuard_andNoStoriesOrFilters() {
        show(CeritaUiState(status = FeedStatus.Guest, items = CeritaPreviewData.stories, postcardCount = 2, videoCount = 2))
        rule.onNodeWithText(str(R.string.cerita_guest_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cerita_guest_body)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cerita_filter_all)).assertDoesNotExist()
        rule.onNodeWithText(str(R.string.cerita_write_postcard)).assertDoesNotExist()
        rule.onNodeWithContentDescription(
            str(R.string.cerita_open_postcard, CeritaPreviewData.postcard.authorName, CeritaPreviewData.postcard.placeName),
        ).assertDoesNotExist()
        rule.onNodeWithText(str(R.string.cerita_guest_action)).performClick()
        assertEquals(listOf("signIn"), calls)
    }

    @Test
    fun empty_showsEmptyState_withFiltersAndWriteButton() {
        show(CeritaUiState(status = FeedStatus.Empty))
        rule.onNodeWithText(str(R.string.cerita_empty_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cerita_empty_body)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cerita_filter_all)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cerita_write_postcard)).assertIsDisplayed().performClick()
        assertEquals(listOf("write"), calls)
    }

    @Test
    fun emptyForFilter_showsFilterSpecificMessage() {
        show(CeritaUiState(status = FeedStatus.Empty, filter = StoryFilter.Video, postcardCount = 2, videoCount = 0))
        rule.onNodeWithText(str(R.string.cerita_empty_filter_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cerita_empty_title)).assertDoesNotExist()
    }

    @Test
    fun filterChips_reportEachFilter() {
        show(content)
        rule.onNodeWithText(str(R.string.cerita_filter_postcard)).performClick()
        rule.onNodeWithText(str(R.string.cerita_filter_video)).performClick()
        rule.onNodeWithText(str(R.string.cerita_filter_all)).performClick()
        assertEquals(listOf("filter:Postcard", "filter:Video", "filter:All"), calls)
    }

    @Test
    fun content_showsStoryTiles_andOpensThem() {
        show(content)
        val postcard = CeritaPreviewData.postcard
        // The sample feed has two postcards by the same author at the same place: take the first tile.
        rule.onAllNodesWithContentDescription(str(R.string.cerita_open_postcard, postcard.authorName, postcard.placeName))
            .onFirst()
            .assertIsDisplayed()
            .performClick()
        assertEquals(listOf("open:${postcard.id}"), calls)
    }

    @Test
    fun content_showsCountsSummary() {
        show(content)
        rule.onNodeWithText(str(R.string.cerita_summary_caption)).assertIsDisplayed()
    }

    @Test
    fun error_offersRetry() {
        show(CeritaUiState(status = FeedStatus.Error))
        rule.onNodeWithText(str(R.string.cerita_error_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cerita_retry)).performClick()
        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun loading_showsLoadingIndicator() {
        show(CeritaUiState(status = FeedStatus.Loading))
        rule.onNodeWithContentDescription(str(R.string.cerita_loading)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cerita_write_postcard)).assertDoesNotExist()
    }
}
