package id.kelana.feature.peta

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Peta screen with fixed states: no Hilt, no network. The map is the drawn illustration, not MapLibre tiles. */
@RunWith(AndroidJUnit4::class)
class PetaScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private val today = LocalDate.of(2026, 11, 20)

    private fun place(id: String, name: String, category: PlaceCategory, lat: Double, lng: Double, checked: Boolean) = Place(
        id = id, slug = id, name = name, category = category, capCode = "YK-00", lat = lat, lng = lng,
        address = "", description = "", coverKey = id,
        hours = DayOfWeek.entries.map {
            OfficialFact(
                OpeningHours(it, isClosed = false, opens = LocalTime.of(9, 0), closes = LocalTime.of(15, 0)),
                sourceUrl = null,
                checkedAt = if (checked) today else null,
            )
        },
    )

    private val places = listOf(
        PetaPlace(place("a", "Candi Prambanan", PlaceCategory.Candi, -7.752, 110.491, true), openToday = true, showToday = true),
        PetaPlace(place("c", "Keraton Yogyakarta", PlaceCategory.Lainnya, -7.8053, 110.3642, false), openToday = false, showToday = false),
    )

    private fun setScreen(
        state: PetaUiState,
        onFilterChange: (PetaFilter) -> Unit = {},
        onPinTap: (String) -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenDetail: (String) -> Unit = {},
    ) {
        rule.setContent {
            KelanaTheme { PetaScreen(state, onBack = {}, onFilterChange, onPinTap, onRetry, onOpenDetail) }
        }
    }

    @Test
    fun filterChips_areShownAndReportSelection() {
        val picked = mutableListOf<PetaFilter>()
        setScreen(PetaUiState(isLoading = false, allPlaces = places, today = today), onFilterChange = { picked += it })
        rule.onNodeWithText(str(R.string.peta_filter_all)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.peta_filter_open_today)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.peta_filter_has_cap)).performClick()
        assertEquals(listOf(PetaFilter.HasCap), picked)
    }

    @Test
    fun noSelection_showsHintAndNoPlaceCard() {
        setScreen(PetaUiState(isLoading = false, allPlaces = places, today = today))
        rule.onNodeWithText(str(R.string.peta_hint), substring = true).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.peta_open_detail)).assertDoesNotExist()
    }

    @Test
    fun pinSelect_callsBackAndOpensPlaceCard() {
        val tapped = mutableListOf<String>()
        rule.setContent {
            var selected by remember { mutableStateOf<String?>(null) }
            KelanaTheme {
                PetaScreen(
                    PetaUiState(isLoading = false, allPlaces = places, selectedId = selected, today = today),
                    onBack = {}, onFilterChange = {}, onPinTap = { tapped += it; selected = it },
                    onRetry = {}, onOpenDetail = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Candi Prambanan").performClick()
        assertEquals(listOf("a"), tapped)
        rule.onNodeWithContentDescription("Candi Prambanan").assertIsSelected()
        rule.onNodeWithText(str(R.string.peta_open_detail)).assertIsDisplayed()
    }

    @Test
    fun placeCard_checkedFacts_showDiperiksaDateAndBadge() {
        setScreen(PetaUiState(isLoading = false, allPlaces = places, selectedId = "a", today = today))
        rule.onNodeWithText(str(R.string.peta_card_checked, formatCheckDate(today))).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.peta_badge_show)).assertIsDisplayed()
    }

    @Test
    fun placeCard_uncheckedFacts_showSedangDicekWithoutDateOrHours() {
        setScreen(PetaUiState(isLoading = false, allPlaces = places, selectedId = "c", today = today))
        val line = str(
            R.string.peta_card_line,
            str(R.string.peta_card_hours_checking),
            str(R.string.peta_card_price_checking),
        )
        rule.onNodeWithText(line).assertIsDisplayed()
        rule.onNodeWithText("Diperiksa", substring = true).assertDoesNotExist()
        rule.onNodeWithText("09.00", substring = true).assertDoesNotExist()
    }

    @Test
    fun placeCard_openDetail_reportsPlaceId() {
        val opened = mutableListOf<String>()
        setScreen(PetaUiState(isLoading = false, allPlaces = places, selectedId = "a", today = today), onOpenDetail = { opened += it })
        rule.onNodeWithText(str(R.string.peta_open_detail)).performClick()
        assertEquals(listOf("a"), opened)
    }

    @Test
    fun filterWithoutMatch_showsNoMatchHint() {
        val none = places.map { it.copy(openToday = false) }
        setScreen(PetaUiState(isLoading = false, allPlaces = none, filter = PetaFilter.OpenToday, today = today))
        rule.onNodeWithText(str(R.string.peta_filter_no_match), substring = true).assertIsDisplayed()
    }

    @Test
    fun emptyState_showsMessage() {
        setScreen(PetaUiState(isLoading = false, today = today))
        rule.onNodeWithText(str(R.string.peta_empty_title)).assertIsDisplayed()
    }

    @Test
    fun errorState_retryCallsBack() {
        var retried = 0
        setScreen(PetaUiState(isLoading = false, hasError = true), onRetry = { retried++ })
        rule.onNodeWithText(str(R.string.peta_error_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.peta_retry)).performClick()
        assertEquals(1, retried)
    }
}
