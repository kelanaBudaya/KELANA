package id.kelana.core.model

/**
 * Clears what one part of the app keeps on the device for the signed-in user. Each owner registers its own cleaner
 * with a Hilt multibinding (`@IntoSet`), so "Hapus akun" can clear everything without knowing the features.
 * Runs after the server confirmed the deletion. [clear] may throw; the caller still runs the other cleaners.
 */
fun interface UserDataCleaner {
    suspend fun clear()
}
