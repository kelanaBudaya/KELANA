package id.kelana.feature.tiket

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kelana.core.model.EventRepository
import id.kelana.core.model.Place
import id.kelana.core.model.PlaceCategory
import id.kelana.core.model.PlaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** What a ticket row stands for. [Kelas] is a craft place (kerajinan) that sells workshops. */
enum class TiketKind { Tempat, Pentas, Kelas }

/**
 * One row. [priceFromIdr] and [checkedAt] are both set or both null: a price is carried only when its fact has a
 * check date (AGENTS.md); otherwise the row shows "Harga sedang dicek". [sellerUrl] is https only.
 */
@Immutable
data class TiketItem(
    val id: String,
    val kind: TiketKind,
    val title: String,
    val category: PlaceCategory,
    /** Place name, set for shows only. */
    val placeName: String?,
    val sellerUrl: String?,
    val priceFromIdr: Long?,
    val checkedAt: LocalDate?,
    /** True when the ticket can be bought at the venue ticket counter. */
    val onSite: Boolean = false,
)

/** The extra line under the price of a row. */
enum class TiketNote { AlsoAtCounter, BuyAtCounter }

/** What a row shows besides title and price: an optional [note] and whether it leads to the official site. */
internal data class RowPresentation(val note: TiketNote?, val opensSeller: Boolean)

/**
 * Pure display rule: on-site with a seller -> "also sold at the counter" (site action stays); on-site without a
 * seller -> "buy at the counter" and no action; seller only -> action; neither -> price only.
 * Free entry ([shownPrice] == 0) is not bought at a counter, so it never gets a counter note. An unknown or
 * unchecked price (null) is not free.
 */
internal fun rowPresentation(onSite: Boolean, sellerUrl: String?, shownPrice: Long?): RowPresentation {
    val hasSeller = sellerUrl != null
    val note = when {
        shownPrice == 0L -> null
        onSite && hasSeller -> TiketNote.AlsoAtCounter
        onSite -> TiketNote.BuyAtCounter
        else -> null
    }
    return RowPresentation(note, opensSeller = hasSeller)
}

/** The price a row may show: only with a check date (AGENTS.md), otherwise null ("Harga sedang dicek"). */
internal fun visiblePrice(item: TiketItem): Long? = item.priceFromIdr?.takeIf { item.checkedAt != null }

/** "Semua" is `filter == null`. */
@Immutable
data class TiketUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val filter: TiketKind? = null,
    val items: List<TiketItem> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && !isError && items.isEmpty()
}

private sealed interface Source {
    data object Loading : Source
    data class Loaded(val items: List<TiketItem>) : Source
    data object Failed : Source
}

/**
 * Tiket resmi (9.5). Rows come from the existing repositories: places with ticket tiers and the shows of the next
 * [WINDOW_DAYS] days. No payment: the screen only hands the official seller URL to the confirm sheet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TiketViewModel(
    private val places: PlaceRepository,
    private val events: EventRepository,
    clock: Clock,
) : ViewModel() {

    @Inject
    constructor(places: PlaceRepository, events: EventRepository) :
        this(places, events, Clock.system(ZoneId.of("Asia/Jakarta")))

    private val today: LocalDate = LocalDate.now(clock)
    private val filter = MutableStateFlow<TiketKind?>(null)
    private val reload = MutableStateFlow(0)

    private val source: Flow<Source> = reload.flatMapLatest {
        flow {
            emit(Source.Loading)
            emitAll(
                combine(
                    places.places(),
                    events.eventsBetween(today, today.plusDays(WINDOW_DAYS - 1L)),
                ) { placeList, occurrences ->
                    val shows = occurrences.map { it.event }.distinctBy { it.id }.map { e ->
                        val fact = e.ticket
                        val priced = fact.checkedAt != null && fact.value.priceFromIdr != null
                        TiketItem(
                            id = "pentas-${e.id}",
                            kind = TiketKind.Pentas,
                            title = e.title,
                            category = e.category,
                            placeName = e.placeName,
                            sellerUrl = httpsOnly(fact.value.sellerUrl),
                            priceFromIdr = if (priced) fact.value.priceFromIdr else null,
                            checkedAt = if (priced) fact.checkedAt else null,
                            onSite = fact.value.onSiteAvailable,
                        )
                    }
                    Source.Loaded(placeList.mapNotNull(::placeItem) + shows) as Source
                }.catch { emit(Source.Failed) },
            )
        }
    }

    val uiState: StateFlow<TiketUiState> = combine(source, filter) { src, kind ->
        when (src) {
            Source.Loading -> TiketUiState(filter = kind)
            Source.Failed -> TiketUiState(isLoading = false, isError = true, filter = kind)
            is Source.Loaded -> TiketUiState(
                isLoading = false,
                filter = kind,
                items = src.items.filter { kind == null || it.kind == kind },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TiketUiState())

    /** `null` means "Semua". */
    fun selectFilter(kind: TiketKind?) {
        filter.update { kind }
    }

    fun retry() {
        reload.update { it + 1 }
    }

    companion object {
        /** Shows are taken from today and the following 13 days, like the Agenda date strip. */
        const val WINDOW_DAYS = 14
    }
}

/**
 * A place row, or null when the place lists no ticket tier at all. The price is the lowest tier that has both a
 * price and a check date; with none, the row has no price and shows "sedang dicek".
 */
internal fun placeItem(place: Place): TiketItem? {
    if (place.tickets.isEmpty()) return null
    val best = place.tickets
        .filter { it.checkedAt != null && it.value.priceIdr != null }
        .minByOrNull { it.value.priceIdr!! }
    val seller = httpsOnly(best?.value?.sellerUrl)
        ?: place.tickets.firstNotNullOfOrNull { httpsOnly(it.value.sellerUrl) }
    return TiketItem(
        id = "tempat-${place.id}",
        kind = if (place.category == PlaceCategory.Kerajinan) TiketKind.Kelas else TiketKind.Tempat,
        title = place.name,
        category = place.category,
        placeName = null,
        sellerUrl = seller,
        priceFromIdr = best?.value?.priceIdr,
        checkedAt = best?.checkedAt,
        onSite = place.tickets.any { it.value.onSiteAvailable },
    )
}

/** Only https links are opened (AGENTS.md). */
internal fun httpsOnly(url: String?): String? = url?.trim()?.takeIf { httpsHost(it) != null }

/** The host of an `https` URL, or null for anything else. */
internal fun httpsHost(url: String?): String? {
    if (url == null) return null
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
    if (!"https".equals(uri.scheme, ignoreCase = true)) return null
    return uri.host?.takeIf { it.isNotBlank() }
}
