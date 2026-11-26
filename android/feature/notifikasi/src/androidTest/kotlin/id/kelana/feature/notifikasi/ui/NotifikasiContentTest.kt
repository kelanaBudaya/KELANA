package id.kelana.feature.notifikasi.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.NotificationCategory
import id.kelana.feature.notifikasi.R
import id.kelana.feature.notifikasi.domain.NotificationSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

private fun str(id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

/** Izin notifikasi 4.9, Notifikasi 5.1 and Pengaturan notifikasi, with fixed states (no Hilt, no DataStore). */
@RunWith(AndroidJUnit4::class)
class NotifikasiContentTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    // ---- 4.9 Izin notifikasi ------------------------------------------------------------------------------------

    @Test
    fun izin_showsTitleBody_andThreeActions() {
        rule.setContent {
            KelanaTheme {
                IzinNotifContent(
                    onClose = { calls += "close" },
                    onLater = { calls += "later" },
                    onAllow = { calls += "allow" },
                )
            }
        }
        rule.onNodeWithText(str(R.string.izin_notif_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.izin_notif_body)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.izin_notif_allow)).assertIsDisplayed().performClick()
        rule.onNodeWithText(str(R.string.izin_notif_later)).assertIsDisplayed().performClick()
        rule.onNodeWithContentDescription(str(R.string.izin_notif_close)).performClick()
        assertEquals(listOf("allow", "later", "close"), calls)
    }

    // ---- 5.1 Notifikasi -----------------------------------------------------------------------------------------

    private val stamp = NotificationItem(
        id = "n1",
        category = NotificationCategory.Paspor,
        title = "Cap Museum Sonobudoyo masuk paspormu",
        body = "Kamu sudah berkelana ke Museum Sonobudoyo!",
        time = LocalTime.of(9, 30),
        isUnread = true,
        placeId = "museum-sonobudoyo",
        eventId = null,
    )
    private val reminder = NotificationItem(
        id = "n2",
        category = NotificationCategory.Rencana,
        title = "Waktunya berangkat ke Candi Prambanan",
        body = "Jadwal resminya sedang dicek.",
        time = LocalTime.of(17, 0),
        isUnread = false,
        placeId = null,
        eventId = "ramayana",
    )
    private val groups = listOf(
        NotificationGroup(LocalDate.of(2026, 11, 23), RelativeDay.Today, listOf(stamp)),
        NotificationGroup(LocalDate.of(2026, 11, 22), RelativeDay.Yesterday, listOf(reminder)),
    )

    private fun showList(state: NotifikasiUiState) {
        rule.setContent {
            KelanaTheme {
                NotifikasiContent(
                    state = state,
                    onBack = { calls += "back" },
                    onSettings = { calls += "settings" },
                    onFilter = { calls += "filter:${it.name}" },
                    onOpen = { calls += "open:${it.id}" },
                )
            }
        }
    }

    @Test
    fun list_showsDayGroupsAndEntries_withUnreadMarker() {
        showList(NotifikasiUiState(groups = groups, isLoading = false))
        rule.onNodeWithText(str(R.string.notifikasi_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.notifikasi_group_today)).assertIsDisplayed()
        rule.onNodeWithText(stamp.title).assertIsDisplayed()
        rule.onNodeWithText(stamp.body).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.notifikasi_group_yesterday)).assertIsDisplayed()
        rule.onNodeWithText(reminder.title).assertIsDisplayed()
        rule.onNodeWithContentDescription(str(R.string.notifikasi_unread_cd)).assertIsDisplayed()
    }

    @Test
    fun list_entryOpens_withItsItem() {
        showList(NotifikasiUiState(groups = groups, isLoading = false))
        rule.onNodeWithText(stamp.title).performClick()
        assertEquals(listOf("open:n1"), calls)
    }

    @Test
    fun list_filterChipsAndSettingsAndBack_callBack() {
        showList(NotifikasiUiState(groups = groups, isLoading = false))
        rule.onNodeWithText(str(R.string.notifikasi_chip_paspor)).performClick()
        rule.onNodeWithText(str(R.string.notifikasi_chip_rencana)).performClick()
        rule.onNodeWithText(str(R.string.notifikasi_chip_cerita)).performClick()
        rule.onNodeWithText(str(R.string.notifikasi_chip_all)).performClick()
        rule.onNodeWithContentDescription(str(R.string.notifikasi_settings_cd)).performClick()
        rule.onNodeWithContentDescription(str(R.string.notifikasi_back)).performClick()
        assertEquals(
            listOf("filter:Paspor", "filter:Rencana", "filter:Cerita", "filter:All", "settings", "back"),
            calls,
        )
    }

    @Test
    fun empty_showsEmptyState_andKeepsFilters() {
        showList(NotifikasiUiState(isLoading = false))
        rule.onNodeWithText(str(R.string.notifikasi_empty_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.notifikasi_empty_body)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.notifikasi_chip_all)).assertIsDisplayed()
    }

    @Test
    fun withEntries_doesNotShowEmptyState() {
        showList(NotifikasiUiState(groups = groups, isLoading = false))
        rule.onNodeWithText(str(R.string.notifikasi_empty_title)).assertDoesNotExist()
    }

    // ---- Pengaturan notifikasi ----------------------------------------------------------------------------------

    /** Keeps the switches in state, like the ViewModel's saved settings do. */
    private fun showSettings(initial: NotificationSettings) {
        rule.setContent {
            KelanaTheme {
                var settings by remember { mutableStateOf(initial) }
                PengaturanNotifContent(
                    state = PengaturanNotifUiState(settings = settings, isLoaded = true),
                    onBack = { calls += "back" },
                    onShowReminders = { settings = settings.copy(showReminders = it); calls += "reminders:$it" },
                    onStampConfirmations = { settings = settings.copy(stampConfirmations = it); calls += "stamp:$it" },
                    onQuietHours = { settings = settings.copy(quietHours = it); calls += "quiet:$it" },
                )
            }
        }
    }

    @Test
    fun settings_showsThreeSwitchesWithTheirState() {
        showSettings(NotificationSettings(showReminders = true, stampConfirmations = false, quietHours = true))
        rule.onNodeWithText(str(R.string.pengaturan_notif_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.pengaturan_notif_leave_title)).assertIsOn()
        rule.onNodeWithText(str(R.string.pengaturan_notif_stamp_title)).assertIsOff()
        rule.onNodeWithText(str(R.string.pengaturan_notif_quiet_title)).assertIsOn()
    }

    @Test
    fun settings_toggling_reportsNewValueAndFlipsSwitch() {
        showSettings(NotificationSettings(showReminders = true, stampConfirmations = true, quietHours = true))
        rule.onNodeWithText(str(R.string.pengaturan_notif_leave_title)).performClick().assertIsOff()
        rule.onNodeWithText(str(R.string.pengaturan_notif_stamp_title)).performClick().assertIsOff()
        rule.onNodeWithText(str(R.string.pengaturan_notif_quiet_title)).performClick().assertIsOff()
        rule.onNodeWithText(str(R.string.pengaturan_notif_quiet_title)).performClick().assertIsOn()
        assertEquals(listOf("reminders:false", "stamp:false", "quiet:false", "quiet:true"), calls)
    }

    @Test
    fun settings_backCallsBack() {
        showSettings(NotificationSettings())
        rule.onNodeWithContentDescription(str(R.string.notifikasi_back)).performClick()
        assertEquals(listOf("back"), calls)
    }
}
