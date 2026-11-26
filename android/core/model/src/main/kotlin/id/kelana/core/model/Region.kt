package id.kelana.core.model

/** Mirrors the Postgres enum `region_status`. */
enum class RegionStatus { Available, Upcoming, NotOpen }

/**
 * A region (province) of Indonesia. Only regions with [isAvailable] set are open to users.
 * [id] is the region code, for example `DIY`.
 */
data class Region(
    val id: String,
    val name: String,
    val status: RegionStatus,
    val sortOrder: Int = 0,
    /** Casual short name for running text ("stamps in Jogja"). Falls back to [name]. */
    val shortName: String = name,
) {
    val isAvailable: Boolean get() = status == RegionStatus.Available
}
