package id.kelana.feature.paspor.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.paspor.R
import id.kelana.feature.paspor.domain.StampFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Ambil cap screens 6.1 to 6.5 plus the provisional "menunggu sinyal" state, with fixed states. */
@RunWith(AndroidJUnit4::class)
class AmbilCapContentTest {
    @get:Rule val rule = createComposeRule()

    private val place = testPlace("museum-sonobudoyo", "Museum Sonobudoyo")
    private val calls = mutableListOf<String>()

    private val actions = AmbilCapActions(
        onBack = { calls += "back" },
        onAllowLocation = { calls += "allow" },
        onCapNow = { calls += "cap" },
        onRetry = { calls += "retry" },
        onOpenSettings = { calls += "settings" },
        onCancelPending = { calls += "cancel" },
        onPendingExpired = { calls += "expired" },
        onDirections = { calls += "directions" },
        onLogin = { calls += "login" },
    )

    private fun show(step: CapStep, status: PlaceStatus = PlaceStatus.Loaded, withPlace: Boolean = true) {
        rule.setContent {
            KelanaTheme {
                AmbilCapContent(
                    state = AmbilCapUiState(status, if (withPlace) place else null, step),
                    actions = actions,
                )
            }
        }
    }

    @Test
    fun izinLokasi_6_1_showsTitleAndBothActions() {
        show(CapStep.Explain)
        rule.onNodeWithText(str(R.string.paspor_izin_title)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_izin_allow)).assertIsDisplayed().performClick()
        rule.onNodeWithText(str(R.string.paspor_izin_later)).assertIsDisplayed().performClick()
        assertEquals(listOf("allow", "back"), calls)
    }

    @Test
    fun ambilCap_6_2_showsPlaceBannerAndCapButton() {
        show(CapStep.Ready)
        rule.onNodeWithText(str(R.string.paspor_ambil_banner_title, place.name)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_ambil_privacy)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_ambil_button)).assertIsDisplayed().assertIsEnabled().performClick()
        assertEquals(listOf("cap"), calls)
    }

    @Test
    fun locating_showsLocatingLabel_andDisablesButton() {
        show(CapStep.Working(accuracyM = null))
        rule.onNodeWithText(str(R.string.paspor_ambil_locating)).assertIsDisplayed().assertIsNotEnabled()
        rule.onNodeWithText(str(R.string.paspor_ambil_button)).assertDoesNotExist()
    }

    @Test
    fun sending_showsServerCheckLabel_andAccuracy() {
        show(CapStep.Working(accuracyM = 18))
        rule.onNodeWithText(str(R.string.paspor_ambil_sending)).assertIsDisplayed().assertIsNotEnabled()
        rule.onNodeWithText(str(R.string.paspor_ambil_banner_accuracy, 18)).assertExists()
    }

    @Test
    fun diLuarArea_6_4_showsDistanceAndBothActions() {
        show(CapStep.OutOfArea(distanceM = 420))
        rule.onNodeWithText(str(R.string.paspor_diluar_title)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_diluar_body_distance, "420", place.name)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_diluar_directions)).assertIsDisplayed().performClick()
        rule.onNodeWithText(str(R.string.paspor_retry)).assertIsDisplayed().performClick()
        assertEquals(listOf("directions", "retry"), calls)
    }

    @Test
    fun diLuarArea_withoutDistance_usesBodyWithoutNumber() {
        show(CapStep.OutOfArea(distanceM = null))
        rule.onNodeWithText(str(R.string.paspor_diluar_body_unknown, place.name)).assertExists()
    }

    @Test
    fun ditolak_6_5_canAskAgain_showsSettingsAndRetry() {
        show(CapStep.Denied(canAskAgain = true))
        rule.onNodeWithText(str(R.string.paspor_ditolak_title)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_open_settings)).assertIsDisplayed().performClick()
        rule.onNodeWithText(str(R.string.paspor_retry)).assertIsDisplayed().performClick()
        assertEquals(listOf("settings", "allow"), calls)
    }

    @Test
    fun ditolak_6_5_neverAskAgain_showsOnlySettings() {
        show(CapStep.Denied(canAskAgain = false))
        rule.onNodeWithText(str(R.string.paspor_open_settings)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.paspor_retry)).assertDoesNotExist()
    }

    @Test
    fun menungguSinyal_showsTitleRemainingTimeAndCancel() {
        show(CapStep.WaitingSignal(capturedAt = Instant.now().minusSeconds(5 * 60)))
        rule.onNodeWithText(str(R.string.paspor_pending_title)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_pending_body)).assertExists()
        rule.onNodeWithText(plural(R.plurals.paspor_pending_remaining, 25, 25)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_pending_cancel)).assertIsDisplayed().performClick()
        assertEquals(listOf("cancel"), calls)
    }

    @Test
    fun menungguSinyal_expiredRequest_notifiesExpiry() {
        show(CapStep.WaitingSignal(capturedAt = Instant.now().minusSeconds(31 * 60)))
        rule.waitForIdle()
        assertTrue(calls.contains("expired"))
    }

    @Test
    fun failure_needsLogin_offersLogin() {
        show(CapStep.Failed(StampFailure.NeedsLogin))
        rule.onNodeWithText(str(R.string.paspor_err_login_title)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_login)).assertIsDisplayed().performClick()
        assertEquals(listOf("login"), calls)
    }

    @Test
    fun failure_lowAccuracy_offersRetry() {
        show(CapStep.Failed(StampFailure.LowAccuracy))
        rule.onNodeWithText(str(R.string.paspor_err_accuracy_title)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_retry)).assertIsDisplayed().performClick()
        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun failure_notConfigured_hasNoPrimaryAction() {
        show(CapStep.Failed(StampFailure.NotConfigured))
        rule.onNodeWithText(str(R.string.paspor_err_config_title)).assertExists()
        rule.onNodeWithText(str(R.string.paspor_retry)).assertDoesNotExist()
    }

    @Test
    fun placeMissing_showsNotFoundMessage() {
        show(CapStep.Ready, status = PlaceStatus.NotFound, withPlace = false)
        rule.onNodeWithText(str(R.string.paspor_place_missing_title)).assertExists()
    }
}
