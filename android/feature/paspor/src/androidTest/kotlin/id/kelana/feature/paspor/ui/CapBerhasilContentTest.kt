package id.kelana.feature.paspor.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.paspor.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CapBerhasilContentTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(state: CapBerhasilUiState) {
        rule.setContent {
            KelanaTheme {
                CapBerhasilContent(
                    state = state,
                    onPostcard = { calls += "postcard" },
                    onPassport = { calls += "paspor" },
                )
            }
        }
    }

    @Test
    fun withPlace_showsPlaceTitleKickerAndActions() {
        val place = testPlace("museum-sonobudoyo", "Museum Sonobudoyo")
        show(
            CapBerhasilUiState(
                isLoading = false,
                place = place,
                collected = 3,
                total = 10,
                stampedAt = Instant.parse("2026-11-20T03:00:00Z"),
                regionCode = "DIY",
            ),
        )
        rule.onNodeWithText(str(R.string.paspor_berhasil_title, place.name)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_berhasil_kicker, 3, 7)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_berhasil_postcard)).assertIsDisplayed().performClick()
        rule.onNodeWithText(str(R.string.paspor_berhasil_passport)).assertIsDisplayed().performClick()
        assertEquals(listOf("postcard", "paspor"), calls)
    }

    @Test
    fun completePassport_showsCompleteKicker() {
        show(CapBerhasilUiState(isLoading = false, place = testPlace("a", "Candi Prambanan"), collected = 10, total = 10))
        rule.onNodeWithText(str(R.string.paspor_berhasil_kicker_complete, 10)).assertExists()
    }

    @Test
    fun withoutPlace_showsPlainCopy() {
        show(CapBerhasilUiState(isLoading = false))
        rule.onNodeWithText(str(R.string.paspor_berhasil_title_plain)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_berhasil_kicker_plain)).assertExists()
    }
}
