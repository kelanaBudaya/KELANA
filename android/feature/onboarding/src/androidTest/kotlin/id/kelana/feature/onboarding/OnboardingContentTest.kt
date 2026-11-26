package id.kelana.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.Region
import id.kelana.core.model.RegionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Stateless onboarding content with fixed states: no Hilt, no network. Strings come from resources. */
@RunWith(AndroidJUnit4::class)
class OnboardingContentTest {
    @get:Rule val rule = createComposeRule()

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private val regions = listOf(
        Region("DIY", "DI Yogyakarta", RegionStatus.Available, 1),
        Region("JTG", "Jawa Tengah", RegionStatus.Upcoming, 2),
        Region("JTM", "Jawa Timur", RegionStatus.NotOpen, 3),
    )

    private fun pilihState(selected: String?) = PilihDaerahUiState(
        loading = false,
        regions = regions,
        selectedRegionId = selected,
        placesByRegion = mapOf("DIY" to RegionPlaces(listOf("Keraton Yogyakarta", "Candi Prambanan"), 2)),
    )

    /** Hosts IntroContent the way the nav graph does: onNext(page) moves to that page. */
    private fun setIntroPager(onFinishedJogja: () -> Unit = {}) {
        rule.setContent {
            var page by mutableIntStateOf(MulaiPage)
            KelanaTheme {
                IntroContent(
                    state = IntroUiState.forPage(page),
                    onNext = { page = it },
                    onSkip = {},
                    onStartFromJogja = onFinishedJogja,
                    onOtherRegion = {},
                    onHaveAccount = {},
                )
            }
        }
    }

    @Test
    fun mulai_showsTaglineAndStartButton() {
        setIntroPager()
        rule.onNodeWithText(str(R.string.mulai_tagline)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.mulai_start)).assertIsDisplayed()
    }

    @Test
    fun mulai_start_opensLanguageStep_thenNextOpensFirstKenalan() {
        setIntroPager()
        rule.onNodeWithText(str(R.string.mulai_start)).performClick()
        rule.onNodeWithText(str(R.string.bahasa_title)).assertIsDisplayed()
        rule.onNodeWithText("English").assertIsDisplayed()
        rule.onNodeWithText("Bahasa Indonesia").assertIsDisplayed()
        rule.onNodeWithText(str(R.string.intro_next)).performClick()
        rule.onNodeWithText(str(R.string.intro1_title)).assertIsDisplayed()
    }

    @Test
    fun kenalan_next_advancesThroughAllPagesToStartFromJogja() {
        var started = 0
        setIntroPager { started++ }
        rule.onNodeWithText(str(R.string.mulai_start)).performClick()
        rule.onNodeWithText(str(R.string.intro_next)).performClick() // language step
        rule.onNodeWithText(str(R.string.intro_next)).performClick()
        rule.onNodeWithText(str(R.string.intro2_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.intro_next)).performClick()
        rule.onNodeWithText(str(R.string.intro3_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.intro3_start_jogja)).performClick()
        assertEquals(1, started)
    }

    @Test
    fun kenalan_skip_callsOnSkip() {
        var skipped = 0
        rule.setContent {
            KelanaTheme {
                IntroContent(
                    state = IntroUiState.forPage(IntroPage.OfficialInfo.page),
                    onNext = {}, onSkip = { skipped++ }, onStartFromJogja = {}, onOtherRegion = {}, onHaveAccount = {},
                )
            }
        }
        rule.onNodeWithText(str(R.string.intro_skip)).performClick()
        assertEquals(1, skipped)
    }

    @Test
    fun pilihDaerah_availableRegionIsSelectable() {
        val chosen = mutableListOf<String>()
        rule.setContent {
            KelanaTheme { PilihDaerahContent(pilihState("DIY"), onBack = {}, onPinTapped = {}, onChoose = { chosen += it }) }
        }
        rule.onNodeWithText(str(R.string.pilih_choose_region, "DI Yogyakarta")).performClick()
        assertEquals(listOf("DIY"), chosen)
        rule.onNodeWithText(str(R.string.pilih_choose_here)).performClick()
        assertEquals(listOf("DIY", "DIY"), chosen)
    }

    @Test
    fun pilihDaerah_upcomingRegionHasNoChooseButton() {
        val chosen = mutableListOf<String>()
        rule.setContent {
            KelanaTheme { PilihDaerahContent(pilihState("JTG"), onBack = {}, onPinTapped = {}, onChoose = { chosen += it }) }
        }
        rule.onNodeWithText(str(R.string.status_upcoming_text)).assertExists()
        rule.onNodeWithText(str(R.string.pilih_choose_here)).assertDoesNotExist()
        assertTrue(chosen.isEmpty())
    }

    @Test
    fun pilihDaerah_notOpenRegionHasNoChooseButton() {
        rule.setContent {
            KelanaTheme { PilihDaerahContent(pilihState("JTM"), onBack = {}, onPinTapped = {}, onChoose = {}) }
        }
        rule.onNodeWithText(str(R.string.status_not_open_text)).assertExists()
        rule.onNodeWithText(str(R.string.pilih_choose_here)).assertDoesNotExist()
    }

    @Test
    fun pilihDaerah_pinTap_reportsRegionId() {
        val tapped = mutableListOf<String>()
        rule.setContent {
            KelanaTheme { PilihDaerahContent(pilihState("DIY"), onBack = {}, onPinTapped = { tapped += it }, onChoose = {}) }
        }
        rule.onNodeWithContentDescription(str(R.string.pin_description, "Jawa Tengah", str(R.string.status_upcoming)))
            .performClick()
        assertEquals(listOf("JTG"), tapped)
    }

    @Test
    fun pilihDaerah_emptyAndLoadingStates() {
        rule.setContent {
            KelanaTheme { PilihDaerahContent(PilihDaerahUiState(loading = true), onBack = {}, onPinTapped = {}, onChoose = {}) }
        }
        rule.onNodeWithText(str(R.string.pilih_loading)).assertIsDisplayed()
    }
}
