package id.kelana.feature.tempat.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.OfficialFact
import id.kelana.core.model.OpeningHours
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.Recurrence
import id.kelana.core.model.ReportReason
import id.kelana.core.model.TicketPrice
import id.kelana.core.ui.formatRupiah
import id.kelana.feature.tempat.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Stateless Detail tempat and Laporkan content with fixed states: no Hilt, no network. */
@RunWith(AndroidJUnit4::class)
class TempatContentTest {
    @get:Rule val rule = createComposeRule()

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private val checkedDate = LocalDate.of(2026, 11, 20)
    private val sellerUrl = "https://example.org/tiket"

    private fun place(checked: Boolean) = Place(
        id = "museum-sonobudoyo", slug = "museum-sonobudoyo", name = "Museum Sonobudoyo",
        category = PlaceCategory.Museum, capCode = "YK-02", lat = 0.0, lng = 0.0,
        address = "Jl. Pangurakan No. 6", description = "Museum budaya Jawa.", coverKey = "museum-sonobudoyo",
        hours = DayOfWeek.entries.map {
            OfficialFact(
                OpeningHours(
                    it, it == DayOfWeek.MONDAY,
                    if (it == DayOfWeek.MONDAY) null else LocalTime.of(8, 0),
                    if (it == DayOfWeek.MONDAY) null else LocalTime.of(21, 0),
                ),
                "https://example.org/sample", if (checked) checkedDate else null,
            )
        },
        tickets = listOf(
            OfficialFact(TicketPrice("Dewasa", 10_000, sellerUrl), "https://example.org/sample", if (checked) checkedDate else null),
            OfficialFact(TicketPrice("Anak-anak", 5_000, null), "https://example.org/sample", if (checked) checkedDate else null),
        ),
    )

    private fun setDetail(
        place: Place,
        onStamp: () -> Unit = {},
        onTicketSite: (String) -> Unit = {},
        onReport: () -> Unit = {},
    ) {
        rule.setContent {
            KelanaTheme {
                DetailTempatContent(
                    place = place, today = DayOfWeek.TUESDAY, onBack = {}, onPhotos = {}, onStamp = onStamp,
                    onTicketSite = onTicketSite, onReport = onReport, onAllStories = {},
                )
            }
        }
    }

    @Test
    fun detail_checkedFact_showsValuesAndNoCheckDate() {
        setDetail(place(checked = true))
        rule.onNodeWithText("Diperiksa", substring = true).assertDoesNotExist()
        rule.onNodeWithText("example.org", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Dewasa").assertExists()
        rule.onNodeWithText(formatRupiah(10_000)).assertExists()
        rule.onNodeWithText("Anak-anak").assertExists()
        rule.onNodeWithText(formatRupiah(5_000)).assertExists()
        rule.onNodeWithText(str(R.string.tempat_ticket_pending)).assertDoesNotExist()
        rule.onNodeWithText("08.00", substring = true).assertExists()
    }

    @Test
    fun detail_uncheckedFact_showsSedangDicekAndNoValue() {
        setDetail(place(checked = false))
        // Opening hours row and closed-days row.
        rule.onAllNodesWithText(str(R.string.tempat_info_checking), ignoreCase = true).assertCountEquals(2)
        rule.onNodeWithText(str(R.string.tempat_ticket_pending)).assertExists()
        rule.onNodeWithText(formatRupiah(10_000), substring = true).assertDoesNotExist()
        rule.onNodeWithText(formatRupiah(5_000), substring = true).assertDoesNotExist()
        rule.onNodeWithText("08.00", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Diperiksa", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Checked", substring = true).assertDoesNotExist()
    }

    @Test
    fun detail_uncheckedFact_hidesTicketSiteButton() {
        setDetail(place(checked = false))
        rule.onNodeWithText(str(R.string.tempat_ticket_site)).assertDoesNotExist()
    }

    @Test
    fun detail_checkedFact_ticketSiteOpensOfficialUrl() {
        val opened = mutableListOf<String>()
        setDetail(place(checked = true), onTicketSite = { opened += it })
        rule.onNodeWithText(str(R.string.tempat_ticket_site)).performClick()
        assertEquals(listOf(sellerUrl), opened)
    }

    @Test
    fun detail_stampBannerAndReportRow_callBack() {
        var stamped = 0
        var reported = 0
        setDetail(place(checked = true), onStamp = { stamped++ }, onReport = { reported++ })
        rule.onNodeWithText(str(R.string.tempat_cap_headline)).performScrollTo().performClick()
        rule.onNodeWithText(str(R.string.tempat_report_action)).performScrollTo().performClick()
        assertEquals(1, stamped)
        assertEquals(1, reported)
    }

    @Test
    fun detail_eventsSection_showsRowsAndOpensEvent() {
        val opened = mutableListOf<String>()
        rule.setContent {
            KelanaTheme {
                DetailTempatContent(
                    place = place(checked = true), today = DayOfWeek.TUESDAY, onBack = {}, onPhotos = {}, onStamp = {},
                    onTicketSite = {}, onReport = {}, onAllStories = {},
                    events = listOf(sampleHostedEvent("e1", "Wayang kulit semalam", Recurrence.Weekly(DayOfWeek.SATURDAY), 60_000)),
                    onOpenEvent = { opened += it },
                )
            }
        }
        rule.onNodeWithText(str(R.string.tempat_events_title)).performScrollTo().assertExists()
        rule.onNodeWithText(str(R.string.tempat_events_price_from, formatRupiah(60_000))).assertExists()
        rule.onNodeWithText("Wayang kulit semalam").performScrollTo().performClick()
        assertEquals(listOf("e1"), opened)
    }

    @Test
    fun detail_noEvents_hidesEventsSection() {
        setDetail(place(checked = true))
        rule.onNodeWithText(str(R.string.tempat_events_title)).assertDoesNotExist()
    }

    @Test
    fun detail_storiesSectionShowsEmptyStateNotInventedStories() {
        setDetail(place(checked = true))
        rule.onNodeWithText(str(R.string.tempat_stories_empty_title)).assertExists()
    }

    @Test
    fun detail_errorState_retryCallsBack() {
        var retried = 0
        rule.setContent {
            KelanaTheme {
                MessageScreen(
                    title = str(R.string.tempat_error_title), body = str(R.string.tempat_error_body),
                    onBack = {}, actionLabel = str(R.string.tempat_retry), onAction = { retried++ },
                )
            }
        }
        rule.onNodeWithText(str(R.string.tempat_error_title)).assertExists()
        rule.onNodeWithText(str(R.string.tempat_retry)).performClick()
        assertEquals(1, retried)
    }

    @Test
    fun laporkan_submitDisabledUntilReasonChosen() {
        rule.setContent {
            KelanaTheme {
                LaporkanContent(LaporkanUiState(placeName = "Museum Sonobudoyo"), onBack = {}, onReason = {}, onDetails = {}, onSubmit = {})
            }
        }
        rule.onNodeWithText(str(R.string.laporkan_submit)).assertIsNotEnabled()
    }

    @Test
    fun laporkan_withReason_submitEnabledAndCallsBack() {
        var submitted = 0
        rule.setContent {
            KelanaTheme {
                LaporkanContent(
                    LaporkanUiState(reason = ReportReason.PlaceClosed, placeName = "Museum Sonobudoyo"),
                    onBack = {}, onReason = {}, onDetails = {}, onSubmit = { submitted++ },
                )
            }
        }
        rule.onNodeWithText(str(R.string.laporkan_submit)).assertIsEnabled().performClick()
        assertEquals(1, submitted)
    }

    @Test
    fun laporkan_reasonTap_reportsReason() {
        val reasons = mutableListOf<ReportReason>()
        rule.setContent {
            KelanaTheme {
                LaporkanContent(LaporkanUiState(), onBack = {}, onReason = { reasons += it }, onDetails = {}, onSubmit = {})
            }
        }
        rule.onNodeWithText(str(R.string.laporkan_reason_closed)).performClick()
        assertEquals(listOf(ReportReason.PlaceClosed), reasons)
    }

    @Test
    fun laporkan_failed_showsErrorBanner() {
        rule.setContent {
            KelanaTheme {
                LaporkanContent(
                    LaporkanUiState(reason = ReportReason.PlaceClosed, failed = true),
                    onBack = {}, onReason = {}, onDetails = {}, onSubmit = {},
                )
            }
        }
        rule.onNodeWithText(str(R.string.laporkan_error)).assertExists()
    }

    @Test
    fun laporTerkirim_showsThanksAndBackToPlace() {
        var back = 0
        rule.setContent { KelanaTheme { LaporTerkirimContent(placeName = "Museum Sonobudoyo", onBack = { back++ }) } }
        rule.onNodeWithText(str(R.string.laporkan_sent_title)).assertExists()
        rule.onNodeWithText(str(R.string.laporkan_sent_back, "Museum Sonobudoyo")).performClick()
        assertEquals(1, back)
    }
}
