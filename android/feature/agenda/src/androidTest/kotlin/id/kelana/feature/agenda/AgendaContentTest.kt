package id.kelana.feature.agenda

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.kelana.core.designsystem.component.formatCheckedDate
import id.kelana.core.designsystem.theme.KelanaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Stateless Agenda and Detail pentas content with fixed states: no Hilt, no network. */
@RunWith(AndroidJUnit4::class)
class AgendaContentTest {
    @get:Rule val rule = createComposeRule()

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private val today = PreviewData.today
    private val tomorrow = today.plusDays(1)
    private val dayAfter = today.plusDays(2)

    @Test
    fun agenda_dayTiles_switchSelection() {
        rule.setContent {
            var selected by remember { mutableStateOf(today) }
            KelanaTheme {
                AgendaContent(
                    state = PreviewData.agendaState.copy(selectedDate = selected),
                    onSelectDate = { selected = it },
                    onSelectFilter = {}, onOpenEvent = {}, onOpenMap = {},
                )
            }
        }
        // Day 0 and day 2 have events (", ada acara" in the description), day 1 has none.
        val todayTile = rule.onNodeWithContentDescription(str(R.string.agenda_day_has_events_cd, formatDayLong(today)))
        val tomorrowTile = rule.onNodeWithContentDescription(formatDayLong(tomorrow))
        todayTile.assertIsSelected()
        tomorrowTile.assertIsNotSelected()
        tomorrowTile.performClick()
        tomorrowTile.assertIsSelected()
        todayTile.assertIsNotSelected()
    }

    @Test
    fun agenda_dayTileTap_reportsDate() {
        val picked = mutableListOf<LocalDate>()
        rule.setContent {
            KelanaTheme {
                AgendaContent(PreviewData.agendaState, onSelectDate = { picked += it }, onSelectFilter = {}, onOpenEvent = {}, onOpenMap = {})
            }
        }
        rule.onNodeWithContentDescription(str(R.string.agenda_day_has_events_cd, formatDayLong(dayAfter))).performClick()
        assertEquals(listOf(dayAfter), picked)
    }

    @Test
    fun agenda_tonightCard_opensEvent() {
        val opened = mutableListOf<String>()
        rule.setContent {
            KelanaTheme {
                AgendaContent(PreviewData.agendaState, onSelectDate = {}, onSelectFilter = {}, onOpenEvent = { opened += it }, onOpenMap = {})
            }
        }
        rule.onNodeWithText(str(R.string.agenda_tonight), substring = true).assertIsDisplayed()
        rule.onNodeWithText(PreviewData.roro.title).performClick()
        assertEquals(listOf(PreviewData.roro.id), opened)
    }

    @Test
    fun agenda_emptyDay_showsEmptyState() {
        val empty = PreviewData.agendaState.copy(tonight = null, alsoToday = emptyList(), upcoming = emptyList())
        rule.setContent {
            KelanaTheme {
                AgendaContent(empty, onSelectDate = {}, onSelectFilter = {}, onOpenEvent = {}, onOpenMap = {})
            }
        }
        rule.onNodeWithText(str(R.string.agenda_empty_title)).assertIsDisplayed()
    }

    @Test
    fun agenda_filterAll_reportsNull() {
        val filters = mutableListOf<Any?>()
        rule.setContent {
            KelanaTheme {
                AgendaContent(
                    PreviewData.agendaState.copy(selectedFilter = id.kelana.core.model.EventTag.Tari),
                    onSelectDate = {}, onSelectFilter = { filters += it }, onOpenEvent = {}, onOpenMap = {},
                )
            }
        }
        rule.onNodeWithText(str(R.string.agenda_filter_all)).performClick()
        assertEquals(listOf<Any?>(null), filters)
    }

    @Test
    fun detailPentas_checkedTicket_buyButtonCallsBackWithOfficialUrl() {
        val calls = mutableListOf<Pair<String, String>>()
        val found = toFound(PreviewData.roro)
        rule.setContent {
            KelanaTheme { DetailPentasContent(found, onBack = {}, onBuyTicket = { url, label -> calls += url to label }) }
        }
        rule.onNodeWithText(str(R.string.detail_buy_ticket)).performClick()
        assertEquals(listOf("https://example.org/tiket" to PreviewData.roro.placeName), calls)
    }

    @Test
    fun detailPentas_checkedSchedule_showsCheckDateInBadge() {
        rule.setContent {
            KelanaTheme { DetailPentasContent(toFound(PreviewData.roro), onBack = {}, onBuyTicket = { _, _ -> }) }
        }
        rule.onNodeWithText(PreviewData.roro.title).assertExists()
        rule.onNodeWithText(formatCheckedDate(PreviewData.today), substring = true).assertExists()
    }

    @Test
    fun detailPentas_uncheckedFacts_showSedangDicekNoBuyButton() {
        val roro = PreviewData.roro
        val unchecked = roro.copy(
            schedule = roro.schedule.copy(checkedAt = null),
            ticket = roro.ticket.copy(checkedAt = null),
        )
        rule.setContent {
            KelanaTheme { DetailPentasContent(toFound(unchecked), onBack = {}, onBuyTicket = { _, _ -> }) }
        }
        rule.onNodeWithText(str(R.string.detail_buy_ticket)).assertDoesNotExist()
        rule.onNodeWithText("19.30", substring = true).assertDoesNotExist()
        rule.onNodeWithText("150", substring = true).assertDoesNotExist()
        rule.onNodeWithText(formatCheckedDate(PreviewData.today), substring = true).assertDoesNotExist()
        rule.onAllNodesWithText(str(R.string.detail_checking)).assertCountEquals(2)
    }

    @Test
    fun detailPentas_notFound_showsMessage() {
        rule.setContent {
            KelanaTheme { DetailPentasContent(DetailPentasUiState.NotFound, onBack = {}, onBuyTicket = { _, _ -> }) }
        }
        rule.onNodeWithText(str(R.string.detail_not_found_title)).assertIsDisplayed()
    }
}
