package id.kelana.feature.akun.ui

import app.cash.turbine.test
import id.kelana.core.model.EmailAuthFailure
import id.kelana.core.model.EmailAuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DaftarViewModelTest {
    private val repo = FakeEmailAuthRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun filled(
        email: String = "nama@contoh.id",
        password: String = "rahasia123",
        confirm: String = password,
    ) = DaftarViewModel(repo).also {
        it.onEmailChange(email)
        it.onPasswordChange(password)
        it.onConfirmChange(confirm)
    }

    @Test
    fun invalidEmail_andWeakPassword_showFieldErrorsWithoutCallingRepository() {
        val vm = filled(email = "x", password = "abc")
        vm.onSubmit()
        assertEquals(EmailAuthError.InvalidEmail, vm.state.value.emailError)
        assertEquals(EmailAuthError.WeakPassword, vm.state.value.passwordError)
        assertEquals(0, repo.signUpCalls)
    }

    @Test
    fun mismatchedConfirm_showsMismatch() {
        val vm = filled(confirm = "rahasia124")
        vm.onSubmit()
        assertEquals(EmailAuthError.PasswordMismatch, vm.state.value.confirmError)
        assertEquals(0, repo.signUpCalls)
    }

    @Test
    fun success_emitsSignedIn() = runTest {
        val vm = filled()
        vm.events.test {
            vm.onSubmit()
            assertEquals(DaftarEvent.SignedIn, awaitItem())
        }
        assertEquals(1, repo.signUpCalls)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun emailAlreadyRegistered_isEmailFieldError() {
        repo.result = EmailAuthResult.Failure(EmailAuthFailure.EmailAlreadyRegistered)
        val vm = filled()
        vm.onSubmit()
        assertEquals(EmailAuthError.EmailAlreadyRegistered, vm.state.value.emailError)
        assertNull(vm.state.value.formError)
    }

    @Test
    fun serverWeakPassword_isPasswordFieldError() {
        repo.result = EmailAuthResult.Failure(EmailAuthFailure.WeakPassword)
        val vm = filled()
        vm.onSubmit()
        assertEquals(EmailAuthError.WeakPassword, vm.state.value.passwordError)
    }

    @Test
    fun confirmationRequired_showsClearMessageNotSuccess() = runTest {
        repo.result = EmailAuthResult.Failure(EmailAuthFailure.ConfirmationRequired)
        val vm = filled()
        vm.events.test {
            vm.onSubmit()
            expectNoEvents()
        }
        assertEquals(EmailAuthError.ConfirmationRequired, vm.state.value.formError)
    }

    @Test
    fun networkError_isFormError() {
        repo.result = EmailAuthResult.Failure(EmailAuthFailure.NetworkError)
        val vm = filled()
        vm.onSubmit()
        assertEquals(EmailAuthError.Network, vm.state.value.formError)
        assertFalse(vm.state.value.isLoading)
    }
}
