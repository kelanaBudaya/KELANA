package id.kelana.feature.rencana.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.kelana.core.designsystem.component.formatCheckedDate
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.DayPlan
import id.kelana.core.model.SkipReason
import id.kelana.core.model.SkippedPlace
import id.kelana.core.model.TravelGroup
import id.kelana.core.ui.formatRupiah
import id.kelana.core.ui.formatTimeRange
import id.kelana.feature.rencana.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Stateless Rencana and Susun content with fixed states: no Hilt, no network. */
@RunWith(AndroidJUnit4::class)
class RencanaContentTest {
    @get:Rule val rule = createComposeRule()

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private val today = LocalDate.of(2026, 11, 20)
    private val checked = LocalDate.of(2026, 11, 20)

    private fun setRencana(
        state: RencanaUiState,
        onEdit: () -> Unit = {},
        onOpenRoute: () -> Unit = {},
    ) {
        rule.setContent {
            KelanaTheme {
                RencanaContent(
                    state, onEdit = onEdit, onOpenRoute = onOpenRoute, onOpenClosed = {}, onTakeStamp = {},
                    onRemind = {}, onOpenEvent = {},
                )
            }
        }
    }

    // ---- Day picker ----

    private fun multiDayState(selected: Int, empty: Boolean = false): RencanaUiState {
        val dates = (0..2).map { today.plusDays(it.toLong()) }
        val stops = PreviewRencana.stops()
        return PreviewRencana.filled().copy(
            plan = DayPlan(dates[selected], if (empty) emptyList() else stops),
            days = dates.mapIndexed { i, d -> DayOption(d, if (i == 2) 0 else stops.size) },
            isDefaultDay = selected == 0,
        )
    }

    @Test
    fun dayPicker_showsChipsAndReportsTheTappedDay() {
        var picked: LocalDate? = null
        rule.setContent {
            KelanaTheme {
                RencanaContent(
                    multiDayState(0), onEdit = {}, onOpenRoute = {}, onOpenClosed = {}, onTakeStamp = {},
                    onRemind = {}, onOpenEvent = {}, onSelectDay = { picked = it },
                )
            }
        }
        val desc = str(R.string.rencana_day_chip_desc, 2, formatDayNumberMonth(today.plusDays(1)))
        rule.onNodeWithContentDescription(desc).assertIsDisplayed().performClick()
        assertEquals(today.plusDays(1), picked)
    }

    @Test
    fun singleDay_hasNoPicker() {
        setRencana(PreviewRencana.filled())
        rule.onNodeWithText(str(R.string.rencana_day_chip, 1)).assertDoesNotExist()
    }

    @Test
    fun emptyDay_showsTheEmptyDayCard() {
        rule.setContent {
            KelanaTheme {
                RencanaContent(
                    multiDayState(2, empty = true), onEdit = {}, onOpenRoute = {}, onOpenClosed = {}, onTakeStamp = {},
                    onRemind = {}, onOpenEvent = {},
                )
            }
        }
        rule.onNodeWithText(str(R.string.rencana_day_empty_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.rencana_day_empty_body)).assertIsDisplayed()
    }

    // ---- Rencana kosong ----

    @Test
    fun rencanaKosong_showsEmptyStateAndCtaCallsBack() {
        var started = 0
        setRencana(PreviewRencana.empty(), onEdit = { started++ })
        rule.onNodeWithText(str(R.string.rencana_empty_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.rencana_empty_cta)).performClick()
        assertEquals(1, started)
    }

    // ---- Rencana hari ini ----

    @Test
    fun rencanaFilled_showsStopsWithOfficialFactsAndNoCheckDates() {
        setRencana(PreviewRencana.filled())
        rule.onNodeWithText(str(R.string.rencana_title_today)).assertIsDisplayed()
        rule.onNodeWithText("Keraton Yogyakarta").assertExists()
        val hours = str(
            R.string.rencana_hours_value,
            formatTimeRange(LocalTime.of(9, 0), LocalTime.of(15, 0)),
        )
        rule.onNodeWithText(hours).assertExists()
        rule.onNodeWithText(str(R.string.rencana_price_value, formatRupiah(25_000))).assertExists()
        rule.onNodeWithText("Diperiksa", substring = true).assertDoesNotExist()
        rule.onNodeWithText(formatCheckedDate(checked), substring = true).assertDoesNotExist()
    }

    @Test
    fun rencanaFilled_priceWithoutCheckDate_showsHargaSedangDicek() {
        setRencana(PreviewRencana.filled())
        // Museum Sonobudoyo has a price fact with no value and no check date.
        rule.onNodeWithText(str(R.string.rencana_price_checking)).assertExists()
    }

    @Test
    fun rencanaFilled_editAndRouteCallBack() {
        var edited = 0
        var routed = 0
        setRencana(PreviewRencana.filled(), onEdit = { edited++ }, onOpenRoute = { routed++ })
        rule.onNodeWithContentDescription(str(R.string.rencana_edit)).performClick()
        rule.onNodeWithText(str(R.string.rencana_route_open_map)).performClick()
        assertEquals(1, edited)
        assertEquals(1, routed)
    }

    // ---- Tanpa internet ----

    @Test
    fun offline_cachedPlanStaysReadableWithOfflineBanner() {
        setRencana(PreviewRencana.filled().copy(isOffline = true))
        rule.onNodeWithText(str(R.string.rencana_offline_banner)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.rencana_title_today)).assertIsDisplayed()
        rule.onNodeWithText("Keraton Yogyakarta").assertExists()
        rule.onNodeWithText("Museum Sonobudoyo").assertExists()
    }

    @Test
    fun offline_emptyStateAlsoShowsTheOfflineBanner() {
        setRencana(PreviewRencana.empty().copy(isOffline = true))
        rule.onNodeWithText(str(R.string.rencana_offline_banner)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.rencana_empty_title)).assertIsDisplayed()
    }

    @Test
    fun statusContent_showsTitleAndRetryCallsBack() {
        var retried = 0
        var back = 0
        rule.setContent {
            KelanaTheme {
                RencanaStatusContent(
                    title = str(R.string.rencana_loaderror_title),
                    body = str(R.string.rencana_loaderror_body),
                    primaryText = str(R.string.rencana_retry),
                    onPrimary = { retried++ },
                    secondaryText = str(R.string.rencana_status_back),
                    onSecondary = { back++ },
                )
            }
        }
        rule.onNodeWithText(str(R.string.rencana_loaderror_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.rencana_loaderror_body)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.rencana_retry)).performClick()
        rule.onNodeWithText(str(R.string.rencana_status_back)).performClick()
        assertEquals(1, retried)
        assertEquals(1, back)
    }

    @Test
    fun statusContent_withoutSecondaryHidesBack() {
        rule.setContent {
            KelanaTheme {
                RencanaStatusContent(
                    title = str(R.string.rencana_buildfail_title),
                    body = str(R.string.rencana_buildfail_body),
                    primaryText = str(R.string.rencana_retry),
                    onPrimary = {},
                )
            }
        }
        rule.onNodeWithText(str(R.string.rencana_buildfail_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.rencana_status_back)).assertDoesNotExist()
    }

    @Test
    fun rencanaLoadError_showsErrorWithRetry() {
        var retried = 0
        rule.setContent {
            KelanaTheme {
                RencanaContent(
                    PreviewRencana.empty().copy(loadError = true), onEdit = {}, onOpenRoute = {}, onOpenClosed = {},
                    onTakeStamp = {}, onRemind = {}, onOpenEvent = {}, onRetry = { retried++ },
                )
            }
        }
        rule.onNodeWithText(str(R.string.rencana_loaderror_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.rencana_retry)).performClick()
        assertEquals(1, retried)
    }

    @Test
    fun filledPlan_doesNotListSkippedPlaces() {
        val filled = PreviewRencana.filled()
        val skipped = listOf(SkippedPlace("gone", "Museum Tidak Muat", SkipReason.NoRoomInDay))
        setRencana(filled.copy(plan = filled.plan!!.copy(skipped = skipped)))
        rule.onNodeWithText("Keraton Yogyakarta").assertExists()
        rule.onNodeWithText("Tidak masuk rencana").assertDoesNotExist()
        rule.onNodeWithText("Museum Tidak Muat", substring = true).assertDoesNotExist()
    }

    // ---- Susun 1/6 Tanggal ----

    private fun susunState(startDate: LocalDate, companion: TravelGroup? = null) = SusunUiState(
        today = today,
        visibleMonth = YearMonth.of(2026, 11),
        startDate = startDate,
        companion = companion,
    )

    @Test
    fun susunTanggal_nextDisabledForPastDate_enabledOnceDatePicked() {
        val past = LocalDate.of(2026, 11, 10)
        val pick = LocalDate.of(2026, 11, 25)
        rule.setContent {
            var state by remember { mutableStateOf(susunState(past)) }
            KelanaTheme {
                SusunTanggalContent(
                    state = state,
                    onPick = { state = state.copy(startDate = it, endDate = null) },
                    onPreviousMonth = {}, onNextMonth = {}, onBack = {}, onContinue = {},
                )
            }
        }
        rule.onNodeWithText(str(R.string.rencana_next)).assertIsNotEnabled()
        rule.onNodeWithContentDescription(formatDayNumberMonth(pick)).performClick()
        rule.onNodeWithText(str(R.string.rencana_next)).assertIsEnabled()
    }

    @Test
    fun susunTanggal_pastDayCellIsNotClickable() {
        val picked = mutableListOf<LocalDate>()
        rule.setContent {
            KelanaTheme {
                SusunTanggalContent(
                    state = susunState(today), onPick = { picked += it },
                    onPreviousMonth = {}, onNextMonth = {}, onBack = {}, onContinue = {},
                )
            }
        }
        rule.onNodeWithContentDescription(formatDayNumberMonth(LocalDate.of(2026, 11, 10))).assertIsNotEnabled()
        rule.onNodeWithContentDescription(formatDayNumberMonth(LocalDate.of(2026, 11, 22))).performClick()
        assertEquals(listOf(LocalDate.of(2026, 11, 22)), picked)
    }

    @Test
    fun susunTanggal_nextCallsContinueWhenDateValid() {
        var continued = 0
        rule.setContent {
            KelanaTheme {
                SusunTanggalContent(
                    state = susunState(today), onPick = {}, onPreviousMonth = {}, onNextMonth = {},
                    onBack = {}, onContinue = { continued++ },
                )
            }
        }
        rule.onNodeWithText(str(R.string.rencana_next)).assertIsEnabled().performClick()
        assertEquals(1, continued)
    }

    // ---- Susun 2/6 Teman ----

    @Test
    fun susunTeman_nextDisabledUntilCompanionChosen() {
        rule.setContent {
            var state by remember { mutableStateOf(susunState(today)) }
            KelanaTheme {
                SusunTemanContent(
                    state = state, onSelect = { state = state.copy(companion = it) }, onBack = {}, onContinue = {},
                )
            }
        }
        rule.onNodeWithText(str(R.string.rencana_next)).assertIsNotEnabled()
        rule.onNodeWithText(str(R.string.rencana_teman_family)).performClick()
        rule.onNodeWithText(str(R.string.rencana_next)).assertIsEnabled()
        rule.onNodeWithText(str(R.string.rencana_teman_family_tip)).assertExists()
    }
}
