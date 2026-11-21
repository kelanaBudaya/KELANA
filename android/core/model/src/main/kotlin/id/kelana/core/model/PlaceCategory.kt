package id.kelana.core.model

/** Mirrors the Postgres enum `place_category`. [key] is the contract value. */
enum class PlaceCategory(val key: String) {
    Museum("museum"),
    Candi("candi"),
    Tari("tari"),
    Topeng("topeng"),
    Gamelan("gamelan"),
    Kerajinan("kerajinan"),
    Lainnya("lainnya"),
    ;

    companion object {
        fun fromKey(key: String): PlaceCategory? = entries.firstOrNull { it.key == key }
    }
}
