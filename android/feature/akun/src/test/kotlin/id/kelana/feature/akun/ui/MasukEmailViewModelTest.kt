package id.kelana.feature.akun.ui

import app.cash.turbine.test
import id.kelana.core.model.EmailAuthFailure
import id.kelana.core.model.EmailAuthRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class FakeEmailAuthRepository(var result: EmailAuthResult = EmailAuthResult.Success) : EmailAuthRepository {
    var signInCalls = 0
    var signUpCalls = 0
    var lastEmail: String? = null
    var lastPassword: String? = null

    override suspend fun signUp(email: String, password: String): EmailAuthResult {
        signUpCalls++
        lastEmail = email
        lastPassword = password
        return result
    }

    override suspend fun signIn(email: String, password: String): EmailAuthResult {
        signInCalls++
        lastEmail = email
        lastPassword = password
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MasukEmailViewModelTest {
    private val repo = FakeEmailAuthRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun filled(email: String = " nama@contoh.id ", password: String = "rahasia123") =
        MasukEmailViewModel(repo).also {
            it.onEmailChange(email)
            it.onPasswordChange(password)
        }

    @Test
    fun emptyForm_cannotSubmit() {
        assertFalse(MasukEmailViewModel(repo).state.value.canSubmit)
    }

    @Test
    fun invalidEmail_showsFieldErrorWithoutCallingRepository() {
        val vm = filled(email = "bukan-email")
        vm.onSubmit()
        assertEquals(EmailAuthError.InvalidEmail, vm.state.value.emailError)
        assertEquals(0, repo.signInCalls)
    }

    @Test
    fun success_sendsTrimmedEmailAndEmitsSignedIn() = runTest {
        val vm = filled()
        vm.events.test {
            vm.onSubmit()
            assertEquals(MasukEmailEvent.SignedIn, awaitItem())
        }
        assertEquals("nama@contoh.id", repo.lastEmail)
        assertEquals("rahasia123", repo.lastPassword)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun wrongCredentials_isFormErrorAndDoesNotNameEmailOrPasswordField() {
        repo.result = EmailAuthResult.Failure(EmailAuthFailure.WrongCredentials)
        val vm = filled()
        vm.onSubmit()
        assertEquals(EmailAuthError.WrongCredentials, vm.state.value.formError)
        assertNull(vm.state.value.emailError)
        assertNull(vm.state.value.passwordError)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun otherFailures_mapToFormErrors() {
        val failures = listOf(
            EmailAuthFailure.NetworkError to EmailAuthError.Network,
            EmailAuthFailure.NotConfigured to EmailAuthError.NotConfigured,
            EmailAuthFailure.RateLimited(30) to EmailAuthError.RateLimited,
            EmailAuthFailure.Unknown to EmailAuthError.Unknown,
            EmailAuthFailure.ConfirmationRequired to EmailAuthError.ConfirmationRequired,
        )
        failures.forEach { (failure, expected) ->
            repo.result = EmailAuthResult.Failure(failure)
            val vm = filled()
            vm.onSubmit()
            assertEquals(expected, vm.state.value.formError)
        }
    }

    @Test
    fun editingClearsErrors() {
        repo.result = EmailAuthResult.Failure(EmailAuthFailure.WrongCredentials)
        val vm = filled()
        vm.onSubmit()
        vm.onPasswordChange("lain12345")
        assertNull(vm.state.value.formError)
    }

    @Test
    fun togglePasswordVisible_flips() {
        val vm = MasukEmailViewModel(repo)
        vm.onTogglePasswordVisible()
        assertTrue(vm.state.value.passwordVisible)
        vm.onTogglePasswordVisible()
        assertFalse(vm.state.value.passwordVisible)
    }

    @Test
    fun emptyPassword_showsPasswordRequired() {
        val vm = MasukEmailViewModel(repo)
        vm.onEmailChange("nama@contoh.id")
        vm.onSubmit()
        assertEquals(EmailAuthError.PasswordRequired, vm.state.value.passwordError)
        assertEquals(0, repo.signInCalls)
    }
}
