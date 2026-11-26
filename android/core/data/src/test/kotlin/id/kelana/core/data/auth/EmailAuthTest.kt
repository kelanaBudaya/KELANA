package id.kelana.core.data.auth

import id.kelana.core.model.EmailAuthFailure
import id.kelana.core.model.EmailAuthResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

private class FakeEmailAuthApi : EmailAuthApi {
    var error: Throwable? = null
    var signUpLeavesSession = true
    val calls = mutableListOf<String>()

    var lastDisplayName: String? = null

    override suspend fun signUp(email: String, password: String, displayName: String?): Boolean {
        lastDisplayName = displayName
        calls += "signUp"
        error?.let { throw it }
        return signUpLeavesSession
    }

    override suspend fun signIn(email: String, password: String) {
        calls += "signIn"
        error?.let { throw it }
    }
}

/** Stands in for a Supabase error response. */
private fun fakeRest(code: String?, status: Int = 400, retry: Int? = null) =
    AuthApiException(RawAuthError.Rest(code, status, retry))

private fun rest(code: String?, status: Int = 400, retry: Int? = null) = RawAuthError.Rest(code, status, retry)

class EmailAuthErrorMapperTest {
    private val signUp = EmailAuthOperation.SignUp
    private val signIn = EmailAuthOperation.SignIn

    @Test
    fun infrastructureErrors() {
        assertEquals(EmailAuthFailure.NotConfigured, mapEmailAuthError(RawAuthError.NotConfigured, signIn))
        assertEquals(EmailAuthFailure.NetworkError, mapEmailAuthError(RawAuthError.Network, signUp))
        assertEquals(EmailAuthFailure.Unknown, mapEmailAuthError(RawAuthError.Other, signUp))
    }

    @Test
    fun rateLimitCarriesRetryAfter() {
        assertEquals(EmailAuthFailure.RateLimited(42), mapEmailAuthError(rest("over_email_send_rate_limit", 429, 42), signUp))
        assertEquals(EmailAuthFailure.RateLimited(null), mapEmailAuthError(rest("over_request_rate_limit", 429), signIn))
        assertEquals(EmailAuthFailure.RateLimited(null), mapEmailAuthError(rest(null, 429), signUp))
    }

    @Test
    fun signUpFailures() {
        assertEquals(EmailAuthFailure.WeakPassword, mapEmailAuthError(rest("weak_password", 422), signUp))
        assertEquals(EmailAuthFailure.EmailAlreadyRegistered, mapEmailAuthError(rest("email_exists", 422), signUp))
        assertEquals(EmailAuthFailure.EmailAlreadyRegistered, mapEmailAuthError(rest("user_already_exists", 422), signUp))
        assertEquals(EmailAuthFailure.InvalidEmail, mapEmailAuthError(rest("validation_failed"), signUp))
    }

    @Test
    fun signInFailures() {
        assertEquals(EmailAuthFailure.WrongCredentials, mapEmailAuthError(rest("invalid_credentials"), signIn))
        assertEquals(EmailAuthFailure.WrongCredentials, mapEmailAuthError(rest("email_not_confirmed"), signIn))
        // Sign-up-only codes do not leak into sign-in.
        assertEquals(EmailAuthFailure.Unknown, mapEmailAuthError(rest("email_exists", 422), signIn))
    }

    @Test
    fun unknownCodeIsUnknown() {
        assertEquals(EmailAuthFailure.Unknown, mapEmailAuthError(rest("something_new", 500), signUp))
        assertEquals(EmailAuthFailure.Unknown, mapEmailAuthError(rest(null, 500), signIn))
    }

    @Test
    fun exceptionsAreClassified() {
        assertEquals(RawAuthError.NotConfigured, AuthNotConfiguredException().toRawAuthError())
        assertEquals(RawAuthError.Network, IOException("down").toRawAuthError())
        assertEquals(RawAuthError.Other, IllegalStateException().toRawAuthError())
    }
}

class SupabaseEmailAuthRepositoryTest {
    private val api = FakeEmailAuthApi()
    private val repo = SupabaseEmailAuthRepository(api)

    private fun failure(r: EmailAuthResult) = (r as EmailAuthResult.Failure).reason

    @Test
    fun happyPathsSucceed() = runTest {
        assertEquals(EmailAuthResult.Success, repo.signUp(" a@b.co ", "abcdefg1"))
        assertEquals(EmailAuthResult.Success, repo.signIn("a@b.co", "abcdefg1"))
        assertEquals(listOf("signUp", "signIn"), api.calls)
    }

    @Test
    fun signUpPassesTrimmedNicknameAndNullWhenBlank() = runTest {
        repo.signUp("a@b.co", "abcdefg1", "  Sari  ")
        assertEquals("Sari", api.lastDisplayName)
        repo.signUp("a@b.co", "abcdefg1", "   ")
        assertEquals(null, api.lastDisplayName)
        repo.signUp("a@b.co", "abcdefg1")
        assertEquals(null, api.lastDisplayName)
    }

    @Test
    fun signUpWithoutSessionIsConfirmationRequired() = runTest {
        api.signUpLeavesSession = false
        assertEquals(EmailAuthFailure.ConfirmationRequired, failure(repo.signUp("a@b.co", "abcdefg1")))
    }

    @Test
    fun invalidInputNeverReachesTheServer() = runTest {
        assertEquals(EmailAuthFailure.InvalidEmail, failure(repo.signUp("nope", "abcdefg1")))
        assertEquals(EmailAuthFailure.WeakPassword, failure(repo.signUp("a@b.co", "short1")))
        assertEquals(EmailAuthFailure.WeakPassword, failure(repo.signUp("a@b.co", "abcdefgh")))
        assertEquals(EmailAuthFailure.InvalidEmail, failure(repo.signIn("nope", "abcdefg1")))
        assertEquals(EmailAuthFailure.WrongCredentials, failure(repo.signIn("a@b.co", "")))
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun signInDoesNotDistinguishUnknownEmailFromWrongPassword() = runTest {
        // Supabase returns the same invalid_credentials for both cases, so the results are identical.
        api.error = fakeRest("invalid_credentials")
        assertEquals(EmailAuthFailure.WrongCredentials, failure(repo.signIn("known@b.co", "abcdefg1")))
        assertEquals(EmailAuthFailure.WrongCredentials, failure(repo.signIn("unknown@b.co", "abcdefg1")))
    }

    @Test
    fun signUpFailuresAreTyped() = runTest {
        api.error = fakeRest("user_already_exists", 422)
        assertEquals(EmailAuthFailure.EmailAlreadyRegistered, failure(repo.signUp("a@b.co", "abcdefg1")))
        api.error = fakeRest("weak_password", 422)
        assertEquals(EmailAuthFailure.WeakPassword, failure(repo.signUp("a@b.co", "abcdefg1")))
        api.error = fakeRest("over_email_send_rate_limit", 429, retry = 30)
        assertEquals(EmailAuthFailure.RateLimited(30), failure(repo.signUp("a@b.co", "abcdefg1")))
        api.error = IOException("down")
        assertEquals(EmailAuthFailure.NetworkError, failure(repo.signUp("a@b.co", "abcdefg1")))
        api.error = AuthNotConfiguredException()
        assertEquals(EmailAuthFailure.NotConfigured, failure(repo.signUp("a@b.co", "abcdefg1")))
        api.error = IllegalStateException()
        assertEquals(EmailAuthFailure.Unknown, failure(repo.signIn("a@b.co", "abcdefg1")))
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        api.error = CancellationException("cancelled")
        try {
            repo.signIn("a@b.co", "abcdefg1")
            throw AssertionError("expected cancellation")
        } catch (_: CancellationException) {
            // expected
        }
    }
}
