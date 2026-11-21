package id.kelana.feature.akun.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import id.kelana.feature.akun.domain.GoogleTokenResult

/** Gets a Google ID token through AndroidX Credential Manager. Needs an Activity context. */
class GoogleCredentialHelper {

    suspend fun getIdToken(context: Context, webClientId: String, hashedNonce: String): GoogleTokenResult {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleTokenResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleTokenResult.Failure
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleTokenResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleTokenResult.NoAccount
        } catch (_: GetCredentialException) {
            GoogleTokenResult.Failure
        } catch (_: GoogleIdTokenParsingException) {
            GoogleTokenResult.Failure
        }
    }
}
