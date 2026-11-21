package id.kelana.core.data.auth

import id.kelana.core.model.AuthValidation
import id.kelana.core.model.EmailAuthFailure
import id.kelana.core.model.EmailAuthRepository
import id.kelana.core.model.EmailAuthResult
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseEmailAuthRepository @Inject constructor(
    private val api: EmailAuthApi,
) : EmailAuthRepository {

    override suspend fun signUp(email: String, password: String): EmailAuthResult {
        val mail = email.trim()
        if (!AuthValidation.isValidEmail(mail)) return failure(EmailAuthFailure.InvalidEmail)
        if (!AuthValidation.isValidPassword(password)) return failure(EmailAuthFailure.WeakPassword)
        return try {
            if (api.signUp(mail, password)) EmailAuthResult.Success else failure(EmailAuthFailure.ConfirmationRequired)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure(mapEmailAuthError(e.toRawAuthError(), EmailAuthOperation.SignUp))
        }
    }

    override suspend fun signIn(email: String, password: String): EmailAuthResult {
        val mail = email.trim()
        if (!AuthValidation.isValidEmail(mail)) return failure(EmailAuthFailure.InvalidEmail)
        // No password-rule check: a password set under other rules must still be able to sign in.
        if (password.isEmpty()) return failure(EmailAuthFailure.WrongCredentials)
        return try {
            api.signIn(mail, password)
            EmailAuthResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure(mapEmailAuthError(e.toRawAuthError(), EmailAuthOperation.SignIn))
        }
    }

    private fun failure(reason: EmailAuthFailure) = EmailAuthResult.Failure(reason)
}
