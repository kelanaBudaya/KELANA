package id.kelana.feature.akun.domain

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Sign-in nonce helpers. Google receives the SHA-256 hash of the nonce (it ends up in the ID token),
 * Supabase receives the raw nonce and hashes it itself to compare.
 */
object Nonce {
    private const val RAW_BYTES = 32

    /** 32 random bytes as 64 lowercase hex characters. */
    fun generateRaw(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(RAW_BYTES).also(random::nextBytes)
        return bytes.toHex()
    }

    /** SHA-256 of [raw], as lowercase hex. */
    fun sha256Hex(raw: String): String =
        MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8)).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
