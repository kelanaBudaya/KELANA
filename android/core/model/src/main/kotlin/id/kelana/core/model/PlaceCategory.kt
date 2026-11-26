package id.kelana.core.model

/** Mirrors the Postgres enum `place_category`. [key] is the contract value. */
enum class PlaceCategory(val key: String) {
    Museum("museum"),
    Candi("candi"),
    Tari("tari"),
    Pentas("pentas"),
    SeniMusik("seni_musik"),
    Kerajinan("kerajinan"),
    Lainnya("lainnya"),
    ;

    companion object {
        /**
         * Legacy keys written by older app versions into the Room cache; they map to the renamed categories so a
         * cached place never falls back to [Lainnya] after an update.
         */
        private val legacyKeys = mapOf("topeng" to Pentas, "gamelan" to SeniMusik)

        fun fromKey(key: String): PlaceCategory? = entries.firstOrNull { it.key == key } ?: legacyKeys[key]
    }
}
