package id.kelana.core.data.stamp

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface IntegrityTokenResult {
    /** A fresh token. Never stored: it is used for one request and dropped. */
    data class Token(val value: String) : IntegrityTokenResult {
        override fun toString(): String = "Token(redacted)"
    }

    /** No Google Cloud project number in this build, so the real provider cannot work. */
    data object NotConfigured : IntegrityTokenResult

    /** Play Integrity failed for now (Play Services missing, network, quota). Retry later. */
    data object Unavailable : IntegrityTokenResult
}

/** Requests a Play Integrity token at SEND time, bound to the nonce. Implementations must not cache tokens. */
interface IntegrityTokenProvider {
    suspend fun requestToken(nonce: String): IntegrityTokenResult
}

/**
 * Real provider, using the classic `IntegrityManager`: its request takes our nonce directly, and the server
 * (verify-stamp) compares the decoded `requestDetails.nonce` with `base64url(sha256(canonical))`. The Standard
 * API needs a warm-up and binds a `requestHash` instead, which does not match the server's nonce check.
 * The classic request quota is far above stamp traffic.
 * A null [cloudProjectNumber] gives [IntegrityTokenResult.NotConfigured].
 */
class PlayIntegrityTokenProvider(
    private val context: Context,
    private val cloudProjectNumber: Long?,
) : IntegrityTokenProvider {

    override suspend fun requestToken(nonce: String): IntegrityTokenResult {
        val project = cloudProjectNumber ?: return IntegrityTokenResult.NotConfigured
        return try {
            val manager = IntegrityManagerFactory.create(context.applicationContext)
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .setCloudProjectNumber(project)
                .build()
            suspendCancellableCoroutine { cont ->
                manager.requestIntegrityToken(request)
                    .addOnSuccessListener { if (cont.isActive) cont.resume(IntegrityTokenResult.Token(it.token())) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(IntegrityTokenResult.Unavailable) }
                    .addOnCanceledListener { if (cont.isActive) cont.resume(IntegrityTokenResult.Unavailable) }
            }
        } catch (_: Exception) {
            IntegrityTokenResult.Unavailable
        }
    }
}

/**
 * Dev stand-in: returns `dev-token`, which only the server's `KELANA_ENV=dev` stub accepts.
 * Selected only in debug builds with `integrity.devToken=true` (see core/data/build.gradle.kts).
 */
class DevIntegrityTokenProvider : IntegrityTokenProvider {
    override suspend fun requestToken(nonce: String): IntegrityTokenResult = IntegrityTokenResult.Token(DEV_TOKEN)

    companion object {
        const val DEV_TOKEN = "dev-token"
    }
}
