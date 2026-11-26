package id.kelana.feature.jelajah

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.PlaceCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Stateless Jelajah content with fixed states: no Hilt, no network. Strings come from resources. */
@RunWith(AndroidJUnit4::class)
class JelajahContentTest {
    @get:Rule val rule = createComposeRule()

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun callbacks(
        onTile: (HomeTile) -> Unit = {},
        onPassport: () -> Unit = {},
        onQuickAction: (QuickAction) -> Unit = {},
    ) = HomeCallbacks(
        onSearch = {}, onBell = {}, onAvatar = {}, onPassport = onPassport, onQuickAction = onQuickAction,
        onTile = onTile, onEvent = {}, onPlace = {}, onSeeAll = {},
    )

    @Test
    fun home_showsCategoryTilesAndPassportCard() {
        rule.setContent { KelanaTheme { HomeContent(PreviewData.homeState(), "", callbacks()) } }
        rule.onNodeWithText(str(R.string.home_passport_title)).assertIsDisplayed()
        HomeTile.entries.forEach { rule.onNodeWithText(str(it.label)).assertExists() }
    }

    @Test
    fun home_guestWithoutStamps_showsPassportEmptyText() {
        val guest = PreviewData.homeState().copy(stampCount = 0, stampTotal = 0)
        rule.setContent { KelanaTheme { HomeContent(guest, "", callbacks()) } }
        rule.onNodeWithText(str(R.string.home_passport_empty)).assertIsDisplayed()
    }

    @Test
    fun home_passportWithStamps_showsRealCounts() {
        val state = PreviewData.homeState().copy(stampCount = 2, stampTotal = 3)
        rule.setContent { KelanaTheme { HomeContent(state, "AB", callbacks()) } }
        rule.onNodeWithText(str(R.string.home_passport_count, 2, 3)).assertIsDisplayed()
    }

    @Test
    fun home_tileTap_reportsTile() {
        val tapped = mutableListOf<HomeTile>()
        rule.setContent { KelanaTheme { HomeContent(PreviewData.homeState(), "", callbacks(onTile = { tapped += it })) } }
        rule.onNodeWithText(str(HomeTile.Pentas.label)).performClick()
        assertEquals(listOf(HomeTile.Pentas), tapped)
    }

    @Test
    fun home_passportCard_opensPassport() {
        var opened = 0
        rule.setContent { KelanaTheme { HomeContent(PreviewData.homeState(), "", callbacks(onPassport = { opened++ })) } }
        rule.onNodeWithText(str(R.string.home_passport_title)).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun home_quickActions_listedForGuestToo() {
        val actions = mutableListOf<QuickAction>()
        rule.setContent { KelanaTheme { HomeContent(PreviewData.homeState(), "", callbacks(onQuickAction = { actions += it })) } }
        rule.onNodeWithText(str(R.string.action_plan)).performClick()
        assertEquals(listOf(QuickAction.Rencana), actions)
    }

    @Test
    fun cari_noQuery_showsPrompt() {
        rule.setContent {
            KelanaTheme { CariContent(CariUiState(), onBack = {}, onQueryChange = {}, onFilterChange = {}, onPlace = {}, onEvent = {}) }
        }
        rule.onNodeWithText(str(R.string.cari_prompt)).assertIsDisplayed()
    }

    @Test
    fun cari_queryWithoutResults_showsEmptyState() {
        rule.setContent {
            KelanaTheme {
                CariContent(CariUiState(query = "zzz"), onBack = {}, onQueryChange = {}, onFilterChange = {}, onPlace = {}, onEvent = {})
            }
        }
        rule.onNodeWithText(str(R.string.cari_empty_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cari_empty_body)).assertIsDisplayed()
    }

    @Test
    fun cari_results_showPlaceAndFilterCallback() {
        val filters = mutableListOf<CariFilter>()
        rule.setContent {
            KelanaTheme {
                CariContent(
                    CariUiState(query = "candi", places = PreviewData.places.take(2)),
                    onBack = {}, onQueryChange = {}, onFilterChange = { filters += it }, onPlace = {}, onEvent = {},
                )
            }
        }
        rule.onNodeWithText("Candi Prambanan").assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cari_filter_places)).performClick()
        assertEquals(listOf(CariFilter.Tempat), filters)
    }

    @Test
    fun kategoriPentas_empty_showsKabariAkuState() {
        var notified = 0
        val state = KategoriUiState(category = PlaceCategory.Pentas, isLoading = false, totalCount = 0)
        rule.setContent {
            KelanaTheme {
                KategoriContent(state, onBack = {}, onOpenTodayChange = {}, onPlace = {}, onNotifyMe = { notified++ })
            }
        }
        rule.onNodeWithText(str(R.string.kategori_empty_title, str(id.kelana.core.ui.R.string.category_pentas).lowercase())).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.kategori_empty_body)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.kabari_aku)).performClick()
        assertEquals(1, notified)
    }

    @Test
    fun kategoriMuseum_listsPlaces() {
        val state = KategoriUiState(PlaceCategory.Museum, false, false, 1, PreviewData.places.take(1))
        rule.setContent {
            KelanaTheme { KategoriContent(state, onBack = {}, onOpenTodayChange = {}, onPlace = {}, onNotifyMe = {}) }
        }
        rule.onNodeWithText("Museum Sonobudoyo").assertIsDisplayed()
    }

    @Test
    fun kategori_filteredEmpty_showsFilterEmptyState() {
        val state = KategoriUiState(PlaceCategory.Museum, false, true, 2, emptyList())
        rule.setContent {
            KelanaTheme { KategoriContent(state, onBack = {}, onOpenTodayChange = {}, onPlace = {}, onNotifyMe = {}) }
        }
        rule.onNodeWithText(str(R.string.filter_empty_title)).assertIsDisplayed()
    }

    @Test
    fun semuaTempat_emptyState() {
        rule.setContent {
            KelanaTheme {
                SemuaTempatContent(
                    SemuaTempatUiState(isLoading = false), onBack = {}, onCategoryChange = {}, onOpenTodayChange = {}, onPlace = {},
                )
            }
        }
        rule.onNodeWithText(str(R.string.filter_empty_title)).assertIsDisplayed()
    }
}
