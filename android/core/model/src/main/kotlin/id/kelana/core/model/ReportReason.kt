package id.kelana.core.model

/** Mirrors the Postgres enum `report_reason`. [key] is the contract value. */
enum class ReportReason(val key: String) {
    HoursOrPriceChanged("hours_or_price_changed"),
    PlaceClosed("place_closed"),
    CeremonyShownWithoutPermission("ceremony_shown_without_permission"),
    InappropriateMedia("inappropriate_media"),
    Other("other"),
    ;

    companion object {
        fun fromKey(key: String): ReportReason? = entries.firstOrNull { it.key == key }
    }
}

/** A report about a place. [details] is at most 1000 characters in the contract. */
data class ReportDraft(
    val placeId: String,
    val reason: ReportReason,
    val details: String? = null,
)

/** Outcome of submitting a report. */
sealed interface ReportResult {
    data object Success : ReportResult

    /** Guest: send the user to Masuk. */
    data object NeedsLogin : ReportResult
    data object Failed : ReportResult
}
