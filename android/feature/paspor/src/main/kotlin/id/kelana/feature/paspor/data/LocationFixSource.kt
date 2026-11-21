package id.kelana.feature.paspor.data

/** The outcome of one location read. Coordinates exist only inside [Fix] and are never logged or stored here. */
sealed interface LocationFix {
    class Fix(
        val lat: Double,
        val lng: Double,
        val accuracyM: Double,
        val isMock: Boolean,
    ) : LocationFix {
        // Keep coordinates out of logs and crash reports.
        override fun toString(): String = "Fix(accuracyM=$accuracyM, isMock=$isMock)"
    }

    /** No fix could be read (GPS off, timeout, no accuracy). */
    data object NoFix : LocationFix

    /** The location permission is not granted (anymore). */
    data object NoPermission : LocationFix
}

/**
 * Reads the device location exactly once per call. The only caller is the "Cap sekarang" handler.
 * Implementations must not subscribe to updates and must not cache the result.
 */
interface LocationFixSource {
    suspend fun currentFix(): LocationFix
}
