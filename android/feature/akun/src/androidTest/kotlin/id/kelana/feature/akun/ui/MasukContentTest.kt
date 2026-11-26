package id.kelana.feature.akun.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.akun.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MasukContentTest {
    @get:Rule val rule = createComposeRule()

    private var google = 0
    private var guest = 0
    private var email = 0
    private var daftar = 0

    private fun show(state: MasukUiState = MasukUiState()) {
        rule.setContent {
            KelanaTheme {
                MasukContent(
                    state = state,
                    onGoogleClick = { google++ },
                    onGuestClick = { guest++ },
                    onEmailClick = { email++ },
                    onDaftarClick = { daftar++ },
                )
            }
        }
    }

    @Test
    fun guestCanSeeAndOpenLanguageControl() {
        show()
        rule.onNodeWithText("Bahasa Indonesia").assertIsDisplayed().performClick()
        rule.onNodeWithText("English").assertIsDisplayed()
    }

    @Test
    fun showsTitleGoogleEmailAndGuestOptions() {
        show()
        rule.onNodeWithText(str(R.string.masuk_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.masuk_google)).assertIsDisplayed().assertHasClickAction()
        rule.onNodeWithText(str(R.string.masuk_email)).assertIsDisplayed().assertHasClickAction()
        rule.onNodeWithText(str(R.string.masuk_guest)).assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun optionsCallTheirCallbacks() {
        show()
        rule.onNodeWithText(str(R.string.masuk_google)).performClick()
        rule.onNodeWithText(str(R.string.masuk_email)).performClick()
        rule.onNodeWithText(str(R.string.masuk_daftar_link)).performClick()
        rule.onNodeWithText(str(R.string.masuk_guest)).performClick()
        assertEquals(listOf(1, 1, 1, 1), listOf(google, email, daftar, guest))
    }

    @Test
    fun loading_disablesGoogleAndEmailButHidesGoogleLabel() {
        show(MasukUiState(isLoading = true))
        rule.onNodeWithText(str(R.string.masuk_email)).assertIsNotEnabled()
        rule.onNodeWithText(str(R.string.masuk_google)).assertDoesNotExist()
    }

    @Test
    fun error_showsItsMessage() {
        show(MasukUiState(error = MasukError.Network))
        rule.onNodeWithText(str(R.string.masuk_error_network)).assertIsDisplayed()
    }
}
