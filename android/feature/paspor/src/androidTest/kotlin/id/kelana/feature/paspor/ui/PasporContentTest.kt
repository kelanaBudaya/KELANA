package id.kelana.feature.paspor.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.feature.paspor.R
import id.kelana.feature.paspor.domain.PassportItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PasporContentTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()
    private var tappedPlace: Place? = null

    private val sono = testPlace("sonobudoyo", "Museum Sonobudoyo", PlaceCategory.Museum, "YK-02")
    private val prambanan = testPlace("prambanan", "Candi Prambanan", PlaceCategory.Candi, "YK-01")
    private val tamanSari = testPlace("taman-sari", "Taman Sari", PlaceCategory.Tari, "YK-03")

    private fun show(state: PasporUiState) {
        rule.setContent {
            KelanaTheme {
                PasporContent(
                    state = state,
                    actions = PasporActions(
                        onBack = { calls += "back" },
                        onProfile = { calls += "profile" },
                        onPlace = { tappedPlace = it },
                        onPostcard = { calls += "postcard" },
                        onLogin = { calls += "login" },
                    ),
                )
            }
        }
    }

    private fun partial() = PasporUiState(
        isLoading = false,
        initials = "SD",
        regionName = "DI Yogyakarta",
        regionCode = "DIY",
        regionShortName = "Jogja",
        items = listOf(
            PassportItem(sono, Instant.parse("2026-11-20T03:00:00Z")),
            PassportItem(prambanan, null),
            PassportItem(tamanSari, null),
        ),
    )

    @Test
    fun grid_showsEarnedAndEmptyStamps() {
        show(partial())
        rule.onNodeWithText(str(R.string.paspor_count_title, 1, 3)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_remaining, 2, "Jogja")).assertExists()
        rule.onNodeWithContentDescription(str(R.string.paspor_stamp_collected_cd, sono.name))
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription(str(R.string.paspor_stamp_missing_cd, prambanan.name))
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription(str(R.string.paspor_stamp_missing_cd, tamanSari.name)).assertExists()
    }

    @Test
    fun tappingAStamp_reportsItsPlace() {
        show(partial())
        rule.onNodeWithContentDescription(str(R.string.paspor_stamp_missing_cd, prambanan.name))
            .performScrollTo().performClick()
        assertEquals(prambanan, tappedPlace)
    }

    @Test
    fun postcardPrompt_showsForLatestStamp_andCallsBack() {
        show(partial())
        rule.onNodeWithText(str(R.string.paspor_postcard_title)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(str(R.string.paspor_postcard_subtitle, sono.name)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_postcard_title)).performClick()
        assertEquals(listOf("postcard"), calls)
    }

    @Test
    fun noStamps_showsEmptyHint_andNoPostcardPrompt() {
        show(partial().copy(items = listOf(PassportItem(sono, null), PassportItem(prambanan, null))))
        rule.onNodeWithText(str(R.string.paspor_section_empty)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_postcard_title)).assertDoesNotExist()
    }

    @Test
    fun allStamped_showsCompleteMessage() {
        show(partial().copy(items = listOf(PassportItem(sono, Instant.parse("2026-11-20T03:00:00Z")))))
        rule.onNodeWithText(str(R.string.paspor_complete, "Jogja")).assertExists()
    }

    @Test
    fun guest_showsLoginPrompt_andCallsLogin() {
        show(PasporUiState(isLoading = false, isGuest = true))
        rule.onNodeWithText(str(R.string.paspor_guest_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.paspor_login)).performClick()
        assertEquals(listOf("login"), calls)
    }

    @Test
    fun noPlaces_showsUnavailableMessage() {
        show(PasporUiState(isLoading = false))
        rule.onNodeWithText(str(R.string.paspor_no_places_title)).assertIsDisplayed()
    }
}
