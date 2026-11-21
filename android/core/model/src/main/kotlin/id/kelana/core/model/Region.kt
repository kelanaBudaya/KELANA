package id.kelana.core.model

/** A region (province) of Indonesia. Only regions with [isAvailable] set are open to users. */
data class Region(
    val id: String,
    val name: String,
    val isAvailable: Boolean,
)
