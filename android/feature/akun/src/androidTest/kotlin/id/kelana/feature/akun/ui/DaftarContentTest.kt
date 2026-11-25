package id.kelana.feature.akun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.core.model.EmailAuthRepository
import id.kelana.core.model.EmailAuthResult
import id.kelana.feature.akun.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private class FakeEmailAuth : EmailAuthRepository {
    var signUps = 0
    var signIns = 0

    override suspend fun signUp(email: String, password: String): EmailAuthResult {
        signUps++
        return EmailAuthResult.Success
    }

    override suspend fun signIn(email: String, password: String): EmailAuthResult {
        signIns++
        return EmailAuthResult.Success
    }
}

/** Email sign-up and sign-in forms. The real ViewModels run on a fake repository (no Hilt, no network). */
@RunWith(AndroidJUnit4::class)
class DaftarContentTest {
    @get:Rule val rule = createComposeRule()

    private fun showDaftar(vm: DaftarViewModel, onMasuk: () -> Unit = {}) {
        rule.setContent {
            KelanaTheme {
                val state by vm.state.collectAsState()
                DaftarContent(
                    state = state,
                    onEmailChange = vm::onEmailChange,
                    onPasswordChange = vm::onPasswordChange,
                    onConfirmChange = vm::onConfirmChange,
                    onTogglePasswordVisible = vm::onTogglePasswordVisible,
                    onSubmit = vm::onSubmit,
                    onBack = {},
                    onMasuk = onMasuk,
                )
            }
        }
    }

    private fun fill(vararg values: String) {
        values.forEachIndexed { i, v -> rule.onAllNodes(hasSetTextAction())[i].performTextInput(v) }
    }

    @Test
    fun emptyForm_submitIsDisabled_andPasswordRuleIsShown() {
        showDaftar(DaftarViewModel(FakeEmailAuth()))
        rule.onNodeWithText(str(R.string.email_daftar_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.email_password_hint)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.email_daftar_button)).assertIsNotEnabled()
    }

    @Test
    fun weakPassword_blocksSubmit_andShowsRuleError() {
        val repo = FakeEmailAuth()
        showDaftar(DaftarViewModel(repo))
        fill("a@b.id", "abc", "abc")
        rule.onNodeWithText(str(R.string.email_daftar_button)).assertIsEnabled().performClick()
        rule.waitForIdle()
        rule.onNodeWithText(str(R.string.email_error_weak_password)).assertIsDisplayed()
        assertEquals(0, repo.signUps)
    }

    @Test
    fun lettersOnlyPassword_isRejected() {
        val repo = FakeEmailAuth()
        showDaftar(DaftarViewModel(repo))
        fill("a@b.id", "hanyahuruf", "hanyahuruf")
        rule.onNodeWithText(str(R.string.email_daftar_button)).performClick()
        rule.waitForIdle()
        rule.onNodeWithText(str(R.string.email_error_weak_password)).assertIsDisplayed()
        assertEquals(0, repo.signUps)
    }

    @Test
    fun mismatchedConfirm_showsMismatchAndDoesNotSubmit() {
        val repo = FakeEmailAuth()
        showDaftar(DaftarViewModel(repo))
        fill("a@b.id", "rahasia123", "rahasia124")
        rule.onNodeWithText(str(R.string.email_daftar_button)).performClick()
        rule.waitForIdle()
        rule.onNodeWithText(str(R.string.email_error_password_mismatch)).assertIsDisplayed()
        assertEquals(0, repo.signUps)
    }

    @Test
    fun validForm_submitsOnce() {
        val repo = FakeEmailAuth()
        showDaftar(DaftarViewModel(repo))
        fill("a@b.id", "rahasia123", "rahasia123")
        rule.onNodeWithText(str(R.string.email_daftar_button)).performClick()
        rule.waitForIdle()
        assertEquals(1, repo.signUps)
    }

    @Test
    fun switchLink_callsOnMasuk() {
        var masuk = 0
        showDaftar(DaftarViewModel(FakeEmailAuth()), onMasuk = { masuk++ })
        rule.onNodeWithText(str(R.string.email_daftar_to_masuk_link)).performClick()
        assertEquals(1, masuk)
    }

    @Test
    fun masukEmail_submitDisabledUntilBothFieldsFilled() {
        val vm = MasukEmailViewModel(FakeEmailAuth())
        rule.setContent {
            KelanaTheme {
                val state by vm.state.collectAsState()
                MasukEmailContent(
                    state = state,
                    onEmailChange = vm::onEmailChange,
                    onPasswordChange = vm::onPasswordChange,
                    onTogglePasswordVisible = vm::onTogglePasswordVisible,
                    onSubmit = vm::onSubmit,
                    onBack = {},
                    onDaftar = {},
                )
            }
        }
        rule.onNodeWithText(str(R.string.email_masuk_title)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.email_masuk_button)).assertIsNotEnabled()
        fill("a@b.id", "rahasia123")
        rule.onNodeWithText(str(R.string.email_masuk_button)).assertIsEnabled()
    }
}
