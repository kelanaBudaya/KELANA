package id.kelana.feature.tiket

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.kelana.core.designsystem.component.formatCheckedDate
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PlaceCategory
import id.kelana.core.ui.formatRupiah
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Stateless Tiket resmi content with fixed states: no Hilt, no network. */
@RunWith(AndroidJUnit4::class)
class TiketContentTest {
    @get:Rule val rule = createComposeRule()

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private val checked = LocalDate.of(2026, 11, 20)

    private val withDate = TiketItem(
        "1", TiketKind.Pentas, "Sendratari Prambanan", PlaceCategory.Tari, "Candi Prambanan",
        "https://tiket.example.org/x", 150_000, checked,
    )
    private val withoutDate = TiketItem(
        "2", TiketKind.Tempat, "Keraton Yogyakarta", PlaceCategory.Candi, null,
        "https://tiket.example.org/y", null, null,
    )

    private fun setContent(
        state: TiketUiState,
        onRetry: () -> Unit = {},
        onOpenSeller: (String, String) -> Unit = { _, _ -> },
        onSelectFilter: (TiketKind?) -> Unit = {},
    ) {
        rule.setContent {
            KelanaTheme {
                TiketContent(state, onBack = {}, onSelectFilter = onSelectFilter, onRetry = onRetry, onOpenSeller = onOpenSeller)
            }
        }
    }

    @Test
    fun priceWithCheckDate_showsPriceAndDiperiksaDate() {
        setContent(TiketUiState(isLoading = false, items = listOf(withDate)))
        rule.onNodeWithText(str(R.string.tiket_checked, formatCheckedDate(checked))).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.tiket_price_from, formatRupiah(150_000)), substring = true).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.tiket_price_pending)).assertDoesNotExist()
    }

    @Test
    fun priceWithoutCheckDate_showsHargaSedangDicekAndNoPrice() {
        setContent(TiketUiState(isLoading = false, items = listOf(withoutDate)))
        rule.onNodeWithText(str(R.string.tiket_price_pending)).assertIsDisplayed()
        rule.onNodeWithText("Rp", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Diperiksa", substring = true).assertDoesNotExist()
    }

    @Test
    fun priceWithoutCheckDate_isNeverShownEvenWhenAmountIsSet() {
        val inconsistent = withDate.copy(checkedAt = null)
        setContent(TiketUiState(isLoading = false, items = listOf(inconsistent)))
        rule.onNodeWithText(str(R.string.tiket_price_pending)).assertIsDisplayed()
        rule.onNodeWithText(formatRupiah(150_000), substring = true).assertDoesNotExist()
    }

    @Test
    fun mixedRows_showOneBadgeOfEachKind() {
        setContent(TiketUiState(isLoading = false, items = listOf(withDate, withoutDate)))
        rule.onAllNodesWithText(str(R.string.tiket_price_pending)).assertCountEquals(1)
        rule.onAllNodesWithText(str(R.string.tiket_checked, formatCheckedDate(checked))).assertCountEquals(1)
    }

    @Test
    fun rowTap_opensOfficialSellerWithTitle() {
        val calls = mutableListOf<Pair<String, String>>()
        setContent(
            TiketUiState(isLoading = false, items = listOf(withDate)),
            onOpenSeller = { url, label -> calls += url to label },
        )
        rule.onNodeWithText("Sendratari Prambanan").performClick()
        assertEquals(listOf("https://tiket.example.org/x" to "Sendratari Prambanan"), calls)
    }

    @Test
    fun emptyState_showsMessageWithoutRetry() {
        setContent(TiketUiState(isLoading = false))
        rule.onNodeWithText(str(R.string.tiket_empty_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.tiket_retry)).assertDoesNotExist()
    }

    @Test
    fun errorState_retryCallsBack() {
        var retried = 0
        setContent(TiketUiState(isLoading = false, isError = true), onRetry = { retried++ })
        rule.onNodeWithText(str(R.string.tiket_error_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.tiket_retry)).performClick()
        assertEquals(1, retried)
    }

    @Test
    fun bannerStatesNoPayment_andFilterReportsKind() {
        val kinds = mutableListOf<TiketKind?>()
        setContent(TiketUiState(isLoading = false, items = listOf(withDate)), onSelectFilter = { kinds += it })
        rule.onNodeWithText(str(R.string.tiket_banner)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.tiket_filter_show)).performClick()
        assertEquals(listOf<TiketKind?>(TiketKind.Pentas), kinds)
    }
}
